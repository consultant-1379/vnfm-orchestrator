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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.formatRollbackErrorMessage;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.messaging.ChartRollbackProcessingBag;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RollbackService {
    private static final String ERROR_MESSAGE = "Failure event did not contain an error message.";

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    public boolean rollbackNextChart(VnfInstance sourceInstance, VnfInstance upgradedInstance, LifecycleOperation operation) {
        final ChartRollbackProcessingBag rollbackBag = createRollbackBagForNextChart(sourceInstance, upgradedInstance, operation);
        return rollbackChart(rollbackBag, sourceInstance, upgradedInstance, operation);
    }

    public boolean rollbackChart(VnfInstance sourceInstance, VnfInstance upgradedInstance, LifecycleOperation operation, String releaseName) {
        final ChartRollbackProcessingBag rollbackBag = createRollbackBagForCurrentChart(sourceInstance, upgradedInstance, operation, releaseName);
        return rollbackChart(rollbackBag, sourceInstance, upgradedInstance, operation);
    }

    public boolean rollbackChart(ChartRollbackProcessingBag rollbackBag,
                                 VnfInstance sourceInstance,
                                 VnfInstance upgradedInstance,
                                 LifecycleOperation operation) {
        if (rollbackBag == null) {
            LOGGER.info("Skip rollback next chart, all chart have been rolled back");
            return false;
        }
        LOGGER.info("Start rolling back next helm chart");

        final ChartRollbackType type = rollbackBag.getType();
        final HelmChart helmChart = rollbackBag.getHelmChart();
        helmChart.setState(LifecycleOperationState.ROLLING_BACK.toString());
        sourceInstance.setTempInstance(convertObjToJsonString(upgradedInstance));

        if (type == ChartRollbackType.INSTANTIATE_CHART) {
            triggerInstantiateOperation(operation, sourceInstance, helmChart);
            return true;
        } else if (type == ChartRollbackType.ROLLBACK_CHART_VERSION) {
            triggerRollbackOperation(operation, sourceInstance, helmChart);
            return true;
        } else if (type == ChartRollbackType.TERMINATE_CHART) {
            triggerTerminateOperation(operation, sourceInstance, helmChart);
            return true;
        } else {
            return false;
        }
    }

    public ChartRollbackProcessingBag createRollbackBagForCurrentChart(VnfInstance sourceInstance,
                                                                       VnfInstance targetInstance,
                                                                       final LifecycleOperation operation,
                                                                       String releaseName) {
        Optional<HelmChart> terminatedHelmChart =
                emptyIfNull(targetInstance.getTerminatedHelmCharts())
                        .stream()
                        .filter(chart -> operation.getOperationOccurrenceId().equals(chart.getOperationOccurrenceId()))
                        .filter(chart -> releaseName.equals(chart.getReleaseName()))
                        .findFirst()
                        .map(HelmChartUtils::toHelmChart);
        Optional<HelmChart> sourceHelmChart =
                emptyIfNull(sourceInstance.getHelmCharts())
                        .stream()
                        .filter(chart -> releaseName.equals(chart.getReleaseName()))
                        .findFirst();
        Optional<HelmChart> targetHelmChart =
                emptyIfNull(targetInstance.getHelmCharts())
                        .stream()
                        .filter(chart -> releaseName.equals(chart.getReleaseName()))
                        .findFirst();

        return createBagBasedOfCharts(terminatedHelmChart, sourceHelmChart, targetHelmChart);
    }

    public ChartRollbackProcessingBag createRollbackBagForNextChart(VnfInstance sourceInstance,
                                                                    VnfInstance upgradedInstance,
                                                                    LifecycleOperation operation) {
        final List<HelmChart> terminatedHelmCharts = emptyIfNull(upgradedInstance.getTerminatedHelmCharts())
                .stream()
                .map(HelmChartUtils::toHelmChart)
                .collect(Collectors.toList());
        final Optional<HelmChart> maxPriorityChart = Stream.of(terminatedHelmCharts, emptyIfNull(upgradedInstance.getHelmCharts()))
                .flatMap(Collection::stream)
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.PROCESSING.name()))
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .max(Comparator.comparingInt(HelmChart::getPriority));

        if (maxPriorityChart.isEmpty()) {
            return null; // No charts left
        }

        final HelmChart chart = maxPriorityChart.get();
        Optional<HelmChart> terminatedHelmChart = HelmChartUtils.getTerminatedCnfChartWithName(
                upgradedInstance, operation, chart.getReleaseName(), chart.getHelmChartName());
        Optional<HelmChart> sourceHelmChart = HelmChartUtils.getCnfChartWithName(
                sourceInstance, chart.getReleaseName(), chart.getHelmChartName());
        Optional<HelmChart> targetHelmChart = HelmChartUtils.getCnfChartWithName(
                upgradedInstance, chart.getReleaseName(), chart.getHelmChartName());

        return createBagBasedOfCharts(terminatedHelmChart, sourceHelmChart, targetHelmChart);
    }

    @Nullable
    private ChartRollbackProcessingBag createBagBasedOfCharts(final Optional<HelmChart> terminatedHelmChart,
                                                              final Optional<HelmChart> sourceHelmChart,
                                                              final Optional<HelmChart> targetHelmChart) {
        if (terminatedHelmChart.isPresent()) {
            return new ChartRollbackProcessingBag(ChartRollbackType.INSTANTIATE_CHART, terminatedHelmChart.get());
        } else if (sourceHelmChart.isPresent() && targetHelmChart.isPresent()) {
            return new ChartRollbackProcessingBag(ChartRollbackType.ROLLBACK_CHART_VERSION, targetHelmChart.get());
        } else if (sourceHelmChart.isEmpty() && targetHelmChart.isPresent()) {
            return new ChartRollbackProcessingBag(ChartRollbackType.TERMINATE_CHART, targetHelmChart.get());
        }

        return null; // when no charts to rollback are left
    }

    public void triggerRollbackOperation(LifecycleOperation operation, VnfInstance vnfInstance, HelmChart helmChart) {
        LOGGER.info("Starting Rollback operation for chart name: {} ", helmChart.getHelmChartName());
        String revisionNumber = getTargetRevisionNumber(operation, vnfInstance, helmChart);
        LifecycleOperationState state = LifecycleOperationState.ROLLING_BACK;
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService
                .routeRollbackRequest(vnfInstance, operation, helmChart.getReleaseName(), revisionNumber);
        if (workflowRoutingResponse.getHttpStatus().isError()) {
            state = LifecycleOperationState.FAILED;
            handleError(operation, vnfInstance, helmChart, workflowRoutingResponse);
        }
        updateOperationState(operation, state);
    }

    public void triggerInstantiateOperation(LifecycleOperation operation, VnfInstance vnfInstance, HelmChart helmChart) {
        LOGGER.info("Starting Install operation for chart name: {} ", helmChart.getHelmChartName());
        LifecycleOperationState state = LifecycleOperationState.ROLLING_BACK;
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService
                .routeInstantiateRequest(vnfInstance, operation, helmChart);
        if (workflowRoutingResponse.getHttpStatus().isError()) {
            state = LifecycleOperationState.FAILED;
            handleError(operation, vnfInstance, helmChart, workflowRoutingResponse);
        }
        updateOperationState(operation, state);
    }

    public void triggerTerminateOperation(LifecycleOperation operation, VnfInstance vnfInstance, HelmChart helmChart) {
        LOGGER.info("Starting Terminate operation for chart name: {} ", helmChart.getHelmChartName());
        LifecycleOperationState state = LifecycleOperationState.ROLLING_BACK;
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService
                .routeTerminateRequest(vnfInstance, operation, helmChart.getReleaseName());
        if (workflowRoutingResponse.getHttpStatus().isError()) {
            state = LifecycleOperationState.FAILED;
            handleError(operation, vnfInstance, helmChart, workflowRoutingResponse);
        }
        updateOperationState(operation, state);
    }

    private void handleError(final LifecycleOperation operation,
                             final VnfInstance vnfInstance,
                             final HelmChart helmChart,
                             final WorkflowRoutingResponse workflowRoutingResponse) {
        helmChart.setState(LifecycleOperationState.FAILED.toString());
        vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getId().equals(helmChart.getReleaseName()))
                .findFirst()
                .ifPresent(chart -> chart.setState(LifecycleOperationState.FAILED.toString()));
        String errorMsg = getErrorMsg(workflowRoutingResponse.getErrorMessage(), helmChart.getReleaseName());
        appendError(errorMsg, operation);
    }

    private String getTargetRevisionNumber(LifecycleOperation operation, VnfInstance vnfInstance, HelmChart helmChart) {
        String targetRevisionNumber;
        Optional<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsRepository.findById(operation.getOperationOccurrenceId());
        String releaseName = helmChart.getReleaseName();
        if (isDowngradeOperation(changePackageOperationDetails)) {
            targetRevisionNumber = helmChart.getRevisionNumber();
            LOGGER.info("Downgrading chart release {} to revision: {}", releaseName, targetRevisionNumber);
        } else {
            Optional<HelmChart> chartInInstance = vnfInstance.getHelmCharts().stream()
                    .filter(obj -> obj.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst();
            targetRevisionNumber = chartInInstance.map(HelmChart::getRevisionNumber).orElse("0");
            LOGGER.info("Rolling back chart release {} to revision: {}", releaseName, targetRevisionNumber);
        }
        return targetRevisionNumber;
    }

    public String getErrorMsg(String errorMessage, String releaseName) {
        return errorMessage != null
                ? formatRollbackErrorMessage(errorMessage, releaseName)
                : formatRollbackErrorMessage(ERROR_MESSAGE, releaseName);
    }
}
