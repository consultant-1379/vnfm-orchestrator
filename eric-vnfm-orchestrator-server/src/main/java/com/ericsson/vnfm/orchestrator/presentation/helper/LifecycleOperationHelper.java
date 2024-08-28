/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.vnfm.orchestrator.presentation.helper;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LifecycleOperationHelper {

    public static final List<String> CLUSTER_CONFIG_NOT_FOUND_ERROR_MESSAGE_PATTERNS = List.of(".+Cluster config file .+ not found.+",
            ".+cluster config not present.+");

    public boolean checkClusterConfigNotFoundError(LifecycleOperation operation) {
        String errorMessage = operation.getError();

        if (errorMessage != null && CLUSTER_CONFIG_NOT_FOUND_ERROR_MESSAGE_PATTERNS.stream().anyMatch(errorMessage::matches)) {
            LOGGER.error("Cluster Config not found, operation={}", operation.getOperationOccurrenceId());
            return true;
        }
        return false;
    }

    public boolean checkInstantiateFailedWithRollback(LifecycleOperation operation) {
        if (operation.getLifecycleOperationType().equals(LifecycleOperationType.INSTANTIATE) && operation
                .getOperationState().equals(LifecycleOperationState.ROLLED_BACK)) {
            LOGGER.info("Lifecycle operation {} has all ready been cleanup. Deleting vnf identifier...", operation.getOperationOccurrenceId());
            return true;
        }
        return false;
    }

    public Optional<LifecycleOperation> findLatestUpgradeOperation(List<LifecycleOperation> lifecycleOperations,
                                                                   String vnfInstanceId,
                                                                   List<ChangePackageOperationDetails> changePackageOperationDetails) {

        if (lifecycleOperations.isEmpty()) {
            LOGGER.debug("No life cycle operations found for VNF instance with ID: {}", vnfInstanceId);
            return Optional.empty();
        }

        final LifecycleOperation latestOperation = lifecycleOperations.get(0);
        if (LifecycleOperationState.FAILED == latestOperation.getOperationState()) {
            LOGGER.debug("Operation with id {} has Failed state for VNF instance with ID: {}",
                         latestOperation.getOperationOccurrenceId(), vnfInstanceId);
            return Optional.empty();
        }

        if (containsTerminate(lifecycleOperations)) {
            return Optional.empty();
        }

        final List<LifecycleOperation> changeVnfPkgOperations = getCompletedChangeVnfPkgOperations(lifecycleOperations);

        if (changeVnfPkgOperations.stream().anyMatch(operation -> operation.getSourceVnfdId() == null || operation.getTargetVnfdId() == null)) {
            return Optional.empty();
        }

        return changeVnfPkgOperations.stream()
                .filter(LifecycleOperationHelper::isNotSelfUpgrade)
                .findFirst()
                .filter(operation -> isUpgradeOperation(operation, changePackageOperationDetails));
    }

    private static boolean containsTerminate(final List<LifecycleOperation> lifecycleOperations) {
        return lifecycleOperations.stream().anyMatch(operation -> LifecycleOperationType.TERMINATE == operation.getLifecycleOperationType());
    }

    private static List<LifecycleOperation> getCompletedChangeVnfPkgOperations(final List<LifecycleOperation> lifecycleOperations) {
        return lifecycleOperations.stream()
                .filter(operation -> operation.getLifecycleOperationType() == LifecycleOperationType.CHANGE_VNFPKG)
                .filter(operation -> operation.getOperationState() == LifecycleOperationState.COMPLETED)
                .toList();
    }

    private static boolean isNotSelfUpgrade(final LifecycleOperation operation) {
        return !operation.getTargetVnfdId().equals(operation.getSourceVnfdId());
    }

    private static boolean isUpgradeOperation(final LifecycleOperation operation,
                                              final List<ChangePackageOperationDetails> changePackageOperationDetails) {

        return changePackageOperationDetails.stream()
                .filter(details -> details.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                .findFirst()
                .filter(details -> details.getChangePackageOperationSubtype() == ChangePackageOperationSubtype.UPGRADE)
                .isPresent();
    }
}
