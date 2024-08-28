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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChangeVnfPackageServiceImpl implements ChangeVnfPackageService {

    @Autowired
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Override
    public boolean isDowngrade(String targetVnfd, LifecycleOperation currentOperation) {
        return getSuitableTargetDowngradeOperationFromOperationHelper(targetVnfd, currentOperation).isPresent();
    }

    @Override
    public boolean isSelfUpgrade(String targetVnfdId, LifecycleOperation operation) {
        return LifecycleOperationType.CHANGE_VNFPKG == operation.getLifecycleOperationType() &&
                targetVnfdId.equals(operation.getSourceVnfdId());
    }

    @Override
    public Optional<LifecycleOperation> getSuitableTargetDowngradeOperationFromVnfInstance(String targetVnfdId, VnfInstance vnfInstance) {
        String sourceVnfdId = vnfInstance.getVnfDescriptorId();
        if (sourceVnfdId.equals(targetVnfdId)) {
            return Optional.empty();
        }

        List<LifecycleOperation> lifecycleOperations = lcmOpSearchService.searchAllCompleted(vnfInstance);
        return getSuitableTargetDowngradeOperationFromOperations(targetVnfdId, sourceVnfdId, lifecycleOperations);
    }

    @Override
    public LifecycleOperation getSuitableTargetDowngradeOperationFromOperation(String targetVnfd, LifecycleOperation currentOperation) {
        return getSuitableTargetDowngradeOperationFromOperationHelper(targetVnfd, currentOperation)
                .orElseThrow(() -> new NoSuchElementException(String.format("No suitable operation for downgrade to %s$1 during operation %s$2",
                                                                            targetVnfd, currentOperation.getOperationOccurrenceId())));
    }

    private Optional<LifecycleOperation> getSuitableTargetDowngradeOperationFromOperationHelper(String targetVnfd,
                                                                                                LifecycleOperation currentOperation) {
        List<LifecycleOperation> operationsBeforeCurrentOperation = lcmOpSearchService.searchAllBefore(currentOperation);
        return getSuitableTargetDowngradeOperationFromOperations(targetVnfd, currentOperation.getSourceVnfdId(), operationsBeforeCurrentOperation);
    }

    @Override
    public Optional<LifecycleOperation> getSuitableTargetDowngradeOperation(VnfInstance vnfInstance,
                                                                            String currentOperationSourceVnfdId,
                                                                            String currentOperationTargetVnfdId) {
        LocalDateTime stateEnteredTime = LocalDateTime.now();
        List<LifecycleOperation> operationsBeforeCurrentOperation =
                lifecycleOperationRepository.findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(vnfInstance,
                                                                                                                            stateEnteredTime);
        return getSuitableTargetDowngradeOperationFromOperations(currentOperationTargetVnfdId,
                                                                 currentOperationSourceVnfdId,
                                                                 operationsBeforeCurrentOperation);
    }

    private Optional<LifecycleOperation> getSuitableTargetDowngradeOperationFromOperations(String targetVnfd, String sourceVnfd,
                                                                                           List<LifecycleOperation> operations) {
        if (isLatestPkgVersionChangeAnUpgradeFromVersion(operations, targetVnfd)) {
            return operations.stream()
                    .filter(operation -> isTargetVnfdIdNotEmpty(operation) && !sourceVnfd.equals(operation.getTargetVnfdId()))
                    .findFirst()
                    .filter(operation -> operation.getTargetVnfdId().equals(targetVnfd) &&
                            !operation.getLifecycleOperationType().equals(LifecycleOperationType.TERMINATE))
                    .filter(operation -> getRevisionCountFromOperation(operation) > 0);
        }
        return Optional.empty();
    }

    private int getRevisionCountFromOperation(LifecycleOperation operation) {
        final String operationOccurrenceId = operation.getOperationOccurrenceId();
        return helmChartHistoryRepository.findAllByLifecycleOperationIdOrderByPriorityAsc(operationOccurrenceId).size();
    }

    private boolean isLatestPkgVersionChangeAnUpgradeFromVersion(List<LifecycleOperation> lifecycleOperations, String targetVnfdId) {
        return lifecycleOperations.stream()
                .filter(this::isTargetAndSourceVnfdIdsAreDifferent)
                .findFirst()
                .filter(latestOperation -> latestOperation.getSourceVnfdId().equals(targetVnfdId))
                .map(latestOperation -> isChangePackageOperationSubtype(latestOperation.getOperationOccurrenceId()))
                .orElse(Boolean.FALSE);
    }

    private boolean isChangePackageOperationSubtype(String operationOccId) {
        Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository.findById(operationOccId);
        return changePackageOperationDetails.isPresent() && changePackageOperationDetails.get().getChangePackageOperationSubtype().equals(
                ChangePackageOperationSubtype.UPGRADE);
    }

    private boolean isTargetVnfdIdNotEmpty(LifecycleOperation operation) {
        return !StringUtils.isEmpty(operation.getTargetVnfdId());
    }

    private boolean isSourceVnfdIdNotEmpty(LifecycleOperation operation) {
        return !StringUtils.isEmpty(operation.getSourceVnfdId());
    }

    private boolean isTargetAndSourceVnfdIdsAreDifferent(LifecycleOperation operation) {
        return isTargetVnfdIdNotEmpty(operation) && isSourceVnfdIdNotEmpty(operation)
                && !operation.getTargetVnfdId().equals(operation.getSourceVnfdId());
    }
}
