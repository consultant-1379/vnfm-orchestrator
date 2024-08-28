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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.SYNC_OPERATION_TIMED_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.sync.ActualScaleValues;
import com.ericsson.vnfm.orchestrator.model.sync.VnfInstanceContext;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncFailedException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SyncServiceImpl implements SyncService {

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private ActualScaleValuesRetriever actualScaleValuesRetriever;

    @Autowired
    private SyncActualValuesProcessor syncActualValuesProcessor;

    @Autowired
    private VnfInstanceService vnfInstanceService;

    @Async
    @Transactional
    @Override
    public void execute(final VnfInstance vnfInstance, final LifecycleOperation operation) {

        updateOperationState(operation, LifecycleOperationState.PROCESSING);
        databaseInteractionService.persistLifecycleOperation(operation);

        try {
            final var vnfInstanceWithDeps = getVnfInstanceContext(vnfInstance);

            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues =
                    actualScaleValuesRetriever.retrievePerTargetActualValuesByReleaseName(vnfInstanceWithDeps);

            final var syncResult = syncActualValuesProcessor.process(vnfInstanceWithDeps, releaseNameToPerTargetActualValues);

            checkTimeOutExpiration(operation);

            SyncResultApplier.apply(syncResult, vnfInstanceWithDeps);

            updateOperationState(operation, LifecycleOperationState.COMPLETED);
            operation.setTargetVnfdId(vnfInstance.getVnfDescriptorId());

            helmChartHistoryService.createAndPersistHistoryRecords(vnfInstance.getHelmCharts(), operation.getOperationOccurrenceId());
        } catch (SyncFailedException e) {
            LOGGER.error("Error was occurred during sync execution", e);
            LifeCycleManagementHelper.setOperationErrorAndStateFailed(operation, e.getMessage(), e.getStatus(), "");
        }

        databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
    }

    private VnfInstanceContext getVnfInstanceContext(final VnfInstance instance) {
        return new VnfInstanceContext(instance,
                                      replicaDetailsMapper.getReplicaDetailsForAllCharts(instance.getHelmCharts()),
                                      vnfInstanceService.getVnfControlledScalingExtension(instance));
    }

    private static void checkTimeOutExpiration(final LifecycleOperation operation) {
        if (!LocalDateTime.now().isBefore(operation.getExpiredApplicationTime())) {
            throw new SyncFailedException(SYNC_OPERATION_TIMED_OUT, HttpStatus.CONFLICT);
        }
    }
}
