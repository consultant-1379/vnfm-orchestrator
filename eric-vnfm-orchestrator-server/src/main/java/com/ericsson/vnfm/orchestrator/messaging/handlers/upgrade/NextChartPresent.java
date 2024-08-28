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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextChart;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextCnfChart;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getNextHelmChartToTerminate;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class NextChartPresent extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;
    private final WorkflowRoutingService workflowRoutingService;
    private final MessageUtility utility;
    private final DatabaseInteractionService databaseInteractionService;
    private final RollbackService rollbackService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Checking for the next chart to Upgrade");
        HelmReleaseLifecycleMessage message = context.getMessage();
        VnfInstance instance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        LifecycleOperation lifecycleOperation = context.getOperation();
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository
                .findById(lifecycleOperation.getOperationOccurrenceId());
        final boolean isDowngrade = isDowngradeOperation(changePackageOperationDetails);
        LOGGER.info("is Downgrade Operation: {}", isDowngrade);
        VnfInstance upgradedInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        utility.updateChartStateAndRevisionNumber(upgradedInstance,
                                                  message.getReleaseName(),
                                                  LifecycleOperationState.COMPLETED.toString(),
                                                  message.getRevisionNumber());
        instance.setTempInstance(convertObjToJsonString(upgradedInstance));
        databaseInteractionService.saveVnfInstanceToDB(instance);
        Optional<HelmChart> nextChart = isDowngrade ? getNextCnfChart(upgradedInstance) : getNextChart(upgradedInstance);
        if (nextChart.isPresent()) {
            HelmChart helmChart = nextChart.get();
            LOGGER.info("Next chart name is {}", helmChart.getHelmChartName());
            WorkflowRoutingResponse response = workflowRoutingService
                    .routeChangePackageInfoRequest(helmChart.getPriority(), operation, upgradedInstance);
            if (response.getHttpStatus().isError()) {
                handleErrorAndRollback(context, upgradedInstance, response, context.getMessage().getReleaseName());
            }
            passToSuccessor(getAlternativeSuccessor(), context);
            return;
        }

        LOGGER.info("All charts have been upgraded, checking if termination in necessary");
        Optional<TerminatedHelmChart> nextChartToTerminate = getNextHelmChartToTerminate(upgradedInstance, lifecycleOperation);
        if (nextChartToTerminate.isEmpty()) {
            LOGGER.info("All charts have been processed");
            passToSuccessor(getSuccessor(), context);
            return;
        }

        TerminatedHelmChart helmChartToTerminate = nextChartToTerminate.get();
        LOGGER.info("Next chart name to terminate is {}", helmChartToTerminate.getHelmChartName());
        WorkflowRoutingResponse response = workflowRoutingService
                .routeTerminateRequest(upgradedInstance, operation, helmChartToTerminate.getReleaseName());
        if (response.getHttpStatus().isError()) {
            handleErrorAndRollback(context, upgradedInstance, response, context.getMessage().getReleaseName());
        }
        passToSuccessor(getAlternativeSuccessor(), context);
    }

    private void handleErrorAndRollback(final MessageHandlingContext<HelmReleaseLifecycleMessage> context,
                                        final VnfInstance upgradedInstance,
                                        final WorkflowRoutingResponse wfsResponse,
                                        final String releaseName) {
        final VnfInstance sourceInstance = context.getVnfInstance();
        final LifecycleOperation operation = context.getOperation();
        setError(String.format("Upgrade failed on chart %s with message %s", releaseName, wfsResponse.getErrorMessage()), operation);
        updateOperationState(operation, LifecycleOperationState.ROLLING_BACK);
        HelmChartUtils.setCompletedChartsStateToProcessing(upgradedInstance, operation);

        final Optional<HelmChartType> helmChartType = emptyIfNull(sourceInstance.getHelmCharts())
                .stream()
                .filter(chart -> releaseName.equals(chart.getReleaseName()))
                .findFirst()
                .or(() -> emptyIfNull(upgradedInstance.getHelmCharts())
                        .stream()
                        .filter(chart -> releaseName.equals(chart.getReleaseName()))
                        .findFirst())
                .map(HelmChart::getHelmChartType);
        if (helmChartType.get() != HelmChartType.CNF) {
            rollbackService.rollbackNextChart(sourceInstance, upgradedInstance, operation);
        } else {
            rollbackService.rollbackChart(sourceInstance, upgradedInstance, operation, releaseName);
        }

        passToSuccessor(getAlternativeSuccessor(), context);
    }
}

