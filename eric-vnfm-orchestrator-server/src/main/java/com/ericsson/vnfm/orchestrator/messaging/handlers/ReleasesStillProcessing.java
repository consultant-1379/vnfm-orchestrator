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

import static java.util.stream.Collectors.toList;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isAnyChartProcessing;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.extern.slf4j.Slf4j;

/**
 * Are any releases still processing their individual operation
 */
@Slf4j
public final class ReleasesStillProcessing extends MessageHandler<HelmReleaseLifecycleMessage> {

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling any releases still processing");
        HelmReleaseLifecycleMessage message = context.getMessage();
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        if (message.getState().equals(HelmReleaseState.FAILED)) {
            LOGGER.info("{} has failed with the following error message: {}", message.getReleaseName(),
                        message.getMessage());
            String errorMessage = message.getMessage() != null ? message.getMessage() :
                    "Failure event did not contain an error message.";
            String formattedErrorMessage = String.format("%s for %s failed with %s.", message.getOperationType(), message.getReleaseName(),
                                                         errorMessage);
            appendError(formattedErrorMessage, operation);
        }
        if (isAnyChartProcessing(instance)) {
            List<String> releasesStillProcessing = instance
                    .getHelmCharts()
                    .stream()
                    .filter(chart -> operation.getLifecycleOperationType().equals(LifecycleOperationType.TERMINATE)
                            && chart.getHelmChartType() != HelmChartType.CRD)
                    .filter(chart -> StringUtils.equals(LifecycleOperationState.PROCESSING.toString(),
                                                        chart.getState()))
                    .map(HelmChart::getReleaseName)
                    .collect(toList());
            LOGGER.info("Waiting for the HelmCharts : {} to be processed ", releasesStillProcessing);
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            LOGGER.info("All charts have been processed");
            passToSuccessor(getSuccessor(), context);
        }
    }
}
