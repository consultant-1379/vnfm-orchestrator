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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DefaultLcmOpErrorProcessor implements LcmOpErrorProcessor {

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    protected DatabaseInteractionService databaseInteractionService;

    @Override
    public void process(LifecycleOperation operation, HttpStatus status, String errorTitle, String errorDetail) {
        LOGGER.info("Started processing of occurred exception to LCM operation");
        VnfInstance vnfInstance = operation.getVnfInstance();

        transferOperationToRolledBackOrFailedStatus(operation);

        LOGGER.info("Remove lifecycle operation stage");
        operation.setLifecycleOperationStage(null);

        setErrorInformation(operation, status, errorTitle, errorDetail);

        persistLifecycleOperationAndVnfInstance(operation, vnfInstance);
        LOGGER.info("Completed processing of occurred exception to LCM operation");
    }

    @Override
    public LifecycleOperationType getType() {
        return null;
    }

    private void transferOperationToRolledBackOrFailedStatus(LifecycleOperation operation) {
        LOGGER.info("Started transferring LCM operation with state {} to FAILED or ROLLED_BACK", operation.getOperationState());
        VnfInstance vnfInstance = operation.getVnfInstance();

        if (LifecycleOperationState.STARTING.equals(operation.getOperationState())) {
            updateOperationState(operation, LifecycleOperationState.ROLLED_BACK);
        } else if (LifecycleOperationState.PROCESSING.equals(operation.getOperationState())) {
            LOGGER.warn("Updating chart status. Package ID: {}", vnfInstance.getVnfPackageId());
            setProcessingChartStateToFailed(vnfInstance);
            updateOperationState(operation, LifecycleOperationState.FAILED);
        }

        vnfInstance.setTempInstance(null);
        LOGGER.info("Completed transferring state of LCM operation. Current state: {}", operation.getOperationState());
    }

    private void persistLifecycleOperationAndVnfInstance(final LifecycleOperation operation, final VnfInstance vnfInstance) {
        LOGGER.info("Persisting error information for LCM operation");

        databaseInteractionService.persistLifecycleOperation(operation);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    private void setProcessingChartStateToFailed(VnfInstance vnfInstance) {
        vnfInstance.getHelmCharts().stream().filter(chart -> StringUtils
                        .equals(LifecycleOperationState.PROCESSING.toString(), chart.getState())).findFirst()
                .ifPresent(chart -> chart.setState(LifecycleOperationState.FAILED.toString()));
    }

    private void setErrorInformation(final LifecycleOperation operation, final HttpStatus httpStatus, String errorTitle, String errorDetail) {
        if (httpStatus.is4xxClientError()) {
            lifeCycleManagementHelper.setOperationErrorFor4xx(operation, errorDetail);
        } else {
            lifeCycleManagementHelper.setOperationError(operation, errorDetail, errorTitle, httpStatus);
        }
    }
}
