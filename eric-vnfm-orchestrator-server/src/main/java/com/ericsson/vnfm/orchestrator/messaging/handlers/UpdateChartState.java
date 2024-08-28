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

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Update the state of the chart based on the result of it's individual lifecycle operation
 */
@Slf4j
@AllArgsConstructor
public final class UpdateChartState extends MessageHandler<HelmReleaseLifecycleMessage> {

    private LifecycleOperationState state;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        LOGGER.info("Handling update charts state with {} release name", message.getReleaseName());
        VnfInstance vnfInstance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        if (LifecycleOperationType.CHANGE_VNFPKG.equals(operation.getLifecycleOperationType())) {
            VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            updateTerminatedHelmCharts(tempInstance, message);
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        } else {
            updateHelmChartsWithState(vnfInstance, message);
        }
        passToSuccessor(getSuccessor(), context);
    }

    private void updateHelmChartsWithState(VnfInstance vnfInstance, HelmReleaseLifecycleMessage message) {
        vnfInstance.getHelmCharts().stream()
                .filter(chart -> !StringUtils.equals(LifecycleOperationState.FAILED.toString(), chart.getState()))
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), message.getReleaseName()))
                .findFirst()
                .ifPresent(chart -> {
                    chart.setState(state.toString());
                    chart.setRevisionNumber(message.getRevisionNumber());
                });
    }

    private void updateTerminatedHelmCharts(VnfInstance vnfInstance, HelmReleaseLifecycleMessage message) {
        vnfInstance.getTerminatedHelmCharts().stream()
                .filter(chart -> !StringUtils.equals(LifecycleOperationState.FAILED.toString(), chart.getState()))
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), message.getReleaseName()))
                .findFirst()
                .ifPresent(chart -> {
                    chart.setState(state.toString());
                    chart.setRevisionNumber(message.getRevisionNumber());
                });
    }
}
