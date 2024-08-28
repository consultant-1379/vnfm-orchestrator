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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getAdditionalParams;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getCompletedHelmCharts;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getFailedHelmCharts;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.google.common.annotations.VisibleForTesting;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TriggerTeardown extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final WorkflowRoutingService workflowRoutingService;
    private final LifeCycleManagementHelper lifeCycleManagementHelper;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking if teardown was specified");
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        if (vnfInstance.isCleanUpResources()) {
            Map<String, Object> additionalParams = getAdditionalParams(operation);
            HashMap<String, Object> terminateParameters = extractTerminateAdditionalParams(additionalParams);
            executeTeardown(operation, terminateParameters, vnfInstance);
            operation.setOperationState(LifecycleOperationState.ROLLING_BACK);
            passToSuccessor(getSuccessor(), context);
        } else {
            LOGGER.info("cleanUpResources is not set to true for {}, teardown will not be performed and operation {}"
                                + "marked as failed", operation.getVnfInstance().getVnfInstanceName(),
                        operation.getOperationOccurrenceId());
            updateOperationState(operation, FAILED);
            passToSuccessor(getAlternativeSuccessor(), context);
        }
    }

    @VisibleForTesting
    protected static HashMap<String, Object> extractTerminateAdditionalParams(final Map additionalParams) {
        HashMap<String, Object> additionalParameters = new HashMap<>();
        if (additionalParams.containsKey(SKIP_VERIFICATION)) {
            Boolean skipVerification = BooleanUtils.getBooleanValue(additionalParams.get(SKIP_VERIFICATION));
            additionalParameters.put(SKIP_VERIFICATION, skipVerification);
        }
        if (additionalParams.containsKey(APPLICATION_TIME_OUT)) {
            String appTimeout = String.valueOf(additionalParams.get(APPLICATION_TIME_OUT));
            additionalParameters.put(APPLICATION_TIME_OUT, appTimeout);
        }
        if (additionalParams.containsKey(SKIP_JOB_VERIFICATION)) {
            Boolean skipJobVerification = BooleanUtils.getBooleanValue(additionalParams.get(SKIP_JOB_VERIFICATION));
            additionalParameters.put(SKIP_JOB_VERIFICATION, skipJobVerification);
        }
        return additionalParameters;
    }

    private void executeTeardown(final LifecycleOperation operation, final HashMap<String, Object> terminateParameters,
                                 final VnfInstance vnfInstance) {
        String message = "Instantiate failed and cleanUpResources is true, now proceeding to terminate and delete all"
                + " pvcs. ";
        appendError(message, operation);
        List<HelmChart> helmCharts = getCompletedHelmCharts(vnfInstance);
        helmCharts.addAll(getFailedHelmCharts(vnfInstance));
        helmCharts.stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD && chart.isChartEnabled())
                .forEach(helmChart -> {
                    WorkflowRoutingResponse response = workflowRoutingService
                            .routeTerminateRequest(vnfInstance, operation, terminateParameters, helmChart.getReleaseName());
                    if (response.getHttpStatus().isError()) {
                        helmChart.setState(FAILED.toString());
                        String errorMessage = response.getErrorMessage();
                        appendError(errorMessage, operation);
                    } else {
                        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, operation.getApplicationTimeout());
                        helmChart.setState(PROCESSING.toString());
                    }
                });
    }
}

