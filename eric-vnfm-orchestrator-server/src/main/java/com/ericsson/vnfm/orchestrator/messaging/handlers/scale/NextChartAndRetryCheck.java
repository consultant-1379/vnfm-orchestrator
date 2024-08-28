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
package com.ericsson.vnfm.orchestrator.messaging.handlers.scale;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getCurrentlyProcessingChart;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateChartStateIncreaseRetryCount;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class NextChartAndRetryCheck extends MessageHandler<HelmReleaseLifecycleMessage> {

    private WorkflowRoutingService workflowRoutingService;
    private MessageUtility utility;
    private ScaleService scaleService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if there is another processing chart and the No. of retry attempts");
        HelmReleaseLifecycleMessage message = context.getMessage();
        LifecycleOperation operation = context.getOperation();
        appendError(message.getMessage(), operation);
        VnfInstance instance = context.getVnfInstance();
        VnfInstance scaledInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        Optional<HelmChart> currentChart = getCurrentlyProcessingChart(scaledInstance);
        if (currentChart.isPresent() && currentChart.get().getRetryCount() < scaleService.getScaleRetryAttempts()) {
            HelmChart helmChart = currentChart.get();
            LOGGER.info("Current processing chart name is {}, retry attempt {}", helmChart.getHelmChartName(), helmChart.getRetryCount());
            scaledInstance = updateChartStateIncreaseRetryCount(scaledInstance,
                                                                message.getReleaseName(),
                                                                LifecycleOperationState.FAILED.toString(),
                                                                helmChart.getPriority());
            instance.setTempInstance(convertObjToJsonString(scaledInstance));
            WorkflowRoutingResponse response = workflowRoutingService
                    .routeScaleRequest(helmChart.getPriority(), context.getOperation(), instance);
            if (response.getHttpStatus().isError()) {
                utility.updateChartForRollback(instance, message, scaledInstance);
            }
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            LOGGER.info("All charts have been processed");
            instance.setTempInstance(convertObjToJsonString(scaledInstance));
            passToSuccessor(getSuccessor(), context);
        }
    }
}
