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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextCnfChart;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class NextChartPresent extends MessageHandler<HelmReleaseLifecycleMessage> {

    private WorkflowRoutingService workflowRoutingService;
    private MessageUtility utility;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if there is another not processed chart");
        VnfInstance instance = context.getVnfInstance();
        VnfInstance scaledInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        Optional<HelmChart> nextChart = getNextCnfChart(scaledInstance);
        if (nextChart.isPresent()) {
            HelmChart helmChart = nextChart.get();
            LOGGER.info("Next chart name is {}", helmChart.getHelmChartName());
            instance.setTempInstance(convertObjToJsonString(scaledInstance));
            WorkflowRoutingResponse response =
                    workflowRoutingService.routeScaleRequest(helmChart.getPriority(), context.getOperation(), instance);
            if (response.getHttpStatus().isError()) {
                utility.updateChartForRollback(instance, context.getMessage(), scaledInstance);
            }
            passToSuccessor(getAlternativeSuccessor(), context);

        } else {
            LOGGER.info("All charts have been processed");
            passToSuccessor(getSuccessor(), context);
        }
    }
}

