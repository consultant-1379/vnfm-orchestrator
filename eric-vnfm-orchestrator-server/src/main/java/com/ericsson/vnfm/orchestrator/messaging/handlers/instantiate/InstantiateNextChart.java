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
package com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextChart;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextCnfChart;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class InstantiateNextChart extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final WorkflowRoutingService workflowRoutingService;
    private final MessageUtility utility;
    private final DatabaseInteractionService databaseInteractionService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if there is another not processed chart");
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        Optional<HelmChart> nextChart = operation.getLifecycleOperationType().equals(LifecycleOperationType.HEAL) ?
                getNextCnfChart(vnfInstance) : getNextChart(vnfInstance);
        if (nextChart.isPresent()) {
            HelmChart helmChart = nextChart.get();
            LOGGER.info("Next chart name is {}", helmChart.getHelmChartName());
            WorkflowRoutingResponse response = workflowRoutingService
                    .routeInstantiateRequest(helmChart.getPriority(), operation, vnfInstance);
            if (response.getHttpStatus().isError()) {
                utility.updateChart(operation, helmChart.getReleaseName(), FAILED.toString(), null);
                appendError(response.getErrorMessage(), operation);
                passToSuccessor(getAlternativeSuccessor(), context);
            }
            databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
        } else {
            LOGGER.info("All charts have been processed");
            passToSuccessor(getSuccessor(), context);
        }
    }
}

