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
package com.ericsson.vnfm.orchestrator.messaging.handlers.terminate;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextCnfChart;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TerminateNextChart extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final WorkflowRoutingService workflowRoutingService;
    private final DatabaseInteractionService databaseInteractionService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if there is another chart to terminate");
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        Optional<HelmChart> nextChart = getNextCnfChart(vnfInstance);

        if (nextChart.isPresent()) {
            HelmChart nextHelmChartToTerminate = nextChart.get();
            String nextReleaseToTerminate = nextHelmChartToTerminate.getReleaseName();

            LOGGER.info("Next chart to terminate: chart name {}, release name {}",
                        nextHelmChartToTerminate.getHelmChartName(),
                        nextReleaseToTerminate);

            nextHelmChartToTerminate.setState(LifecycleOperationState.PROCESSING.toString());

            WorkflowRoutingResponse response = workflowRoutingService.routeTerminateRequest(vnfInstance, operation, nextReleaseToTerminate);

            if (response.getHttpStatus().isError()) {
                nextHelmChartToTerminate.setState(LifecycleOperationState.FAILED.toString());
                appendError(response.getErrorMessage(), operation);

                passToSuccessor(getSuccessor(), context);
            } else {
                databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
            }
        } else {
            LOGGER.info("All charts have been terminated");
            passToSuccessor(getSuccessor(), context);
        }
    }
}

