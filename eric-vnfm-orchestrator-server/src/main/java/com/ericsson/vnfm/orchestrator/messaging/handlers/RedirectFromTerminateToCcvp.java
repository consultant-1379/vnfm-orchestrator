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
package com.ericsson.vnfm.orchestrator.messaging.handlers;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.CHANGE_VNFPKG;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.operations.ChangeVnfPackageOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedirectFromTerminateToCcvp extends RedirectToAnotherOperation<ChangeVnfPackageOperation> {

    public RedirectFromTerminateToCcvp(final ChangeVnfPackageOperation routeToOperation,
                                       final DatabaseInteractionService databaseInteractionService) {
        super(routeToOperation, databaseInteractionService);
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    protected void onRedirect(final HelmReleaseLifecycleMessage message, final LifecycleOperation operation, final VnfInstance vnfInstance) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        List<TerminatedHelmChart> helmChartsToTerminate = tempInstance.getTerminatedHelmCharts();
        if (CollectionUtils.isNotEmpty(helmChartsToTerminate)) {
            updateTerminatedHelmChartsStates(helmChartsToTerminate, operation, message);
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
            databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
        }
    }

    @Override
    protected boolean shouldBeRedirected(final LifecycleOperation operation) {
        return operation.getLifecycleOperationType().equals(LifecycleOperationType.CHANGE_VNFPKG) &&
                operation.getOperationState().equals(LifecycleOperationState.PROCESSING);
    }

    @Override
    protected HelmReleaseOperationType getOperationType() {
        return CHANGE_VNFPKG;
    }

    private void updateTerminatedHelmChartsStates(List<TerminatedHelmChart> helmChartsToTerminate, LifecycleOperation operation,
                                                  HelmReleaseLifecycleMessage message) {
        HelmReleaseState state = message.getState();
        HelmReleaseOperationType operationType = message.getOperationType();
        helmChartsToTerminate.stream()
                .filter(chart -> !StringUtils.equals(LifecycleOperationState.FAILED.toString(), chart.getState())
                        && StringUtils.equals(chart.getReleaseName(), message.getReleaseName())
                        && StringUtils.equals(chart.getOperationOccurrenceId(), operation.getOperationOccurrenceId()))
                .findFirst()
                .ifPresent(chart -> {
                    if (HelmReleaseOperationType.DELETE_PVC.equals(operationType)) {
                        chart.setDeletePvcState(state.toString());
                    }
                    chart.setState(state.toString());
                    chart.setRevisionNumber(message.getRevisionNumber());
                });
    }
}
