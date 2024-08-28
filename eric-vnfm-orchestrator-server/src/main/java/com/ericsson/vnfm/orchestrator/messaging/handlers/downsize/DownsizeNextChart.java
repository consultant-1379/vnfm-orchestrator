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
package com.ericsson.vnfm.orchestrator.messaging.handlers.downsize;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getNextChartToDownsizeAfter;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Objects;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DownsizeNextChart extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final WorkflowRoutingService workflowRoutingService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if there is another chart to downsize");

        final LifecycleOperation operation = context.getOperation();
        final VnfInstance vnfInstance = context.getVnfInstance();
        final String currentReleaseName = context.getMessage().getReleaseName();

        final boolean downsizeDuringRollbackAfterCCVPFailure = isDownsizeDuringRollbackAfterCCVPFailure(operation);

        final VnfInstance targetInstance = downsizeDuringRollbackAfterCCVPFailure ?
                parseJson(vnfInstance.getTempInstance(), VnfInstance.class) :
                vnfInstance;

        final Optional<MessageHandler<HelmReleaseLifecycleMessage>> nextHandler =
                triggerDownsizeForNextChartIfPresent(targetInstance, operation, currentReleaseName);

        if (downsizeDuringRollbackAfterCCVPFailure) {
            vnfInstance.setTempInstance(convertObjToJsonString(targetInstance));
        }

        passToSuccessor(nextHandler, context);
    }

    private static boolean isDownsizeDuringRollbackAfterCCVPFailure(final LifecycleOperation operation) {
        return Objects.equals(operation.getOperationState(), LifecycleOperationState.ROLLING_BACK);
    }

    private Optional<MessageHandler<HelmReleaseLifecycleMessage>> triggerDownsizeForNextChartIfPresent(final VnfInstance targetInstance,
                                                                                                       final LifecycleOperation operation,
                                                                                                       final String currentReleaseName) {

        final Optional<HelmChart> nextChart = getNextChartToDownsizeAfter(currentReleaseName, targetInstance);

        if (nextChart.isPresent()) {
            final boolean downsizeRequestSucceeded = sendDownsizeRequest(nextChart.get(), targetInstance, operation);

            return downsizeRequestSucceeded ? getAlternativeSuccessor() : getSuccessor();
        }

        LOGGER.info("All charts have been downsized");

        return getSuccessor();
    }

    private boolean sendDownsizeRequest(final HelmChart nextHelmChartToDownsize, final VnfInstance vnfInstance, final LifecycleOperation operation) {
        final String nextReleaseToDownsize = nextHelmChartToDownsize.getReleaseName();

        LOGGER.info("Next chart to downsize: chart name {}, release name {}", nextHelmChartToDownsize.getHelmChartName(), nextReleaseToDownsize);

        nextHelmChartToDownsize.setDownsizeState(LifecycleOperationState.PROCESSING.toString());

        final ResponseEntity<Object> responseEntity = workflowRoutingService.routeDownsizeRequest(vnfInstance, operation, nextReleaseToDownsize);

        if (responseEntity.getStatusCode().isError()) {
            nextHelmChartToDownsize.setDownsizeState(LifecycleOperationState.FAILED.toString());

            String errorMessage = responseEntity.getBody() != null ? responseEntity.getBody().toString() : null; // NOSONAR
            appendError(errorMessage, operation);

            return false;
        }

        return true;
    }
}
