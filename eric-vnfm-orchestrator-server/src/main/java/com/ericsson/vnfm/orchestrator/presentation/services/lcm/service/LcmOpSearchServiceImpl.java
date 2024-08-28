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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.service;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_PACKAGE_INFO;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.MODIFY_INFO;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SCALE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SYNC;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.NOT_FAILED_OPERATION_STATES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LcmOpSearchServiceImpl implements LcmOpSearchService {

    private final LifecycleOperationRepository lifecycleOperationRepository;

    private final LifecycleOperationHelper lifecycleOperationHelper;

    @Override
    public List<LifecycleOperation> searchAllNotFailedInstallOrUpgradeOperations(final VnfInstance vnfInstance) {
        List<LifecycleOperationType> types = List.of(INSTANTIATE, CHANGE_VNFPKG);

        return lifecycleOperationRepository.findByVnfInstanceAndTypesAndStates(vnfInstance, types, NOT_FAILED_OPERATION_STATES);
    }

    @Override
    public List<LifecycleOperation> searchAllNotFailedInstallOrUpgradeOrScaleOperations(final VnfInstance vnfInstance) {
        List<LifecycleOperationType> types = List.of(INSTANTIATE, CHANGE_VNFPKG, SCALE);

        return lifecycleOperationRepository.findByVnfInstanceAndTypesAndStates(vnfInstance, types, NOT_FAILED_OPERATION_STATES);
    }

    @Override
    public Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOrScaleOperation(final VnfInstance vnfInstance, final int skip) {
        List<LifecycleOperationType> types = List.of(INSTANTIATE, CHANGE_VNFPKG, SCALE);
        List<LifecycleOperationState> states = List.of(COMPLETED);

        List<LifecycleOperation> lifecycleOperations = lifecycleOperationRepository
                .findByVnfInstanceAndTypesAndStates(vnfInstance, types, states);

        return lifecycleOperations.stream()
                .skip(skip)
                .findFirst();
    }

    @Override
    public Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOperation(final VnfInstance vnfInstance, final int skip) {
        List<LifecycleOperationType> types = List.of(INSTANTIATE, CHANGE_VNFPKG);
        List<LifecycleOperationState> states = List.of(COMPLETED);

        List<LifecycleOperation> lifecycleOperations = lifecycleOperationRepository
                .findByVnfInstanceAndTypesAndStates(vnfInstance, types, states);

        return lifecycleOperations.stream()
                .skip(skip)
                .findFirst();
    }

    @Override
    public Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOperationForRollbackTo(final VnfInstance vnfInstance) {
        return searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 1);
    }

    @Override
    public List<LifecycleOperation> searchAllBefore(final LifecycleOperation lifecycleOperation) {
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        LocalDateTime stateEnteredTime = lifecycleOperation.getStateEnteredTime();
        return lifecycleOperationRepository.findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(vnfInstance,
                stateEnteredTime);
    }

    public Optional<LifecycleOperation> searchLastSuccessfulInstallOperation(final VnfInstance vnfInstance) {
        List<LifecycleOperationType> types = List.of(INSTANTIATE);
        List<LifecycleOperationState> states = List.of(COMPLETED);

        List<LifecycleOperation> lifecycleOperations = lifecycleOperationRepository
                .findByVnfInstanceAndTypesAndStates(vnfInstance, types, states);

        return lifecycleOperations.stream()
                .findFirst();
    }

    public Optional<LifecycleOperation> searchLastOperation(final VnfInstance vnfInstance) {
        List<LifecycleOperation> lifecycleOperations = lifecycleOperationRepository
                .findByVnfInstanceOrderByStateEnteredTimeDesc(vnfInstance);
        return lifecycleOperations.stream()
                .findFirst();
    }

    public Optional<LifecycleOperation> searchLastChangingOperation(final VnfInstance vnfInstance,
                                                                    final List<ChangePackageOperationDetails> changePackageOperationDetails) {
        final List<LifecycleOperation> lifecycleOperations = vnfInstance.getAllOperations();
        return Optional.ofNullable(lifecycleOperations).map(ArrayList::new)
                .map(ops -> {
                    ops.sort(Comparator.comparing(LifecycleOperation::getStartTime).reversed());
                    return ops;
                })
                .flatMap(operations -> lifecycleOperationHelper.findLatestUpgradeOperation(
                        operations, vnfInstance.getVnfInstanceId(), changePackageOperationDetails));
    }

    @Override
    public List<LifecycleOperation> searchAllCompleted(final VnfInstance vnfInstance) {
        List<LifecycleOperationState> states = List.of(COMPLETED);
        List<LifecycleOperationType> types = List.of(INSTANTIATE, SCALE, CHANGE_VNFPKG, TERMINATE,
                                                     SYNC, HEAL, MODIFY_INFO, CHANGE_PACKAGE_INFO);
        return lifecycleOperationRepository.findByVnfInstanceAndTypesAndStates(vnfInstance, types, states);
    }
}
