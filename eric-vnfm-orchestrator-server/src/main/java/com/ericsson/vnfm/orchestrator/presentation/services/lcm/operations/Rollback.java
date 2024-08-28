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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Rollback implements Command {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Override
    public CommandType getType() {
        return CommandType.ROLLBACK;
    }

    @Override
    public void execute(final LifecycleOperation operation, final HelmChart helmChart,
            final boolean isDowngradeOperation) {
        LOGGER.info("Start Rollback command");
        VnfInstance actualInstance = operation.getVnfInstance();
        if (isDowngradeOperation) {
            final Optional<ChangePackageOperationDetails> changePackageOperationDetails =
                    changePackageOperationDetailsRepository
                    .findById(operation.getOperationOccurrenceId());
            final String targetOperationOccurrenceId = changePackageOperationDetails.get()
                    .getTargetOperationOccurrenceId();
            startRollbackFlowForDowngrade(actualInstance, operation, targetOperationOccurrenceId, helmChart, true);
        } else {
            final String revisionNumber = actualInstance.getHelmCharts().stream()
                    .filter(hc -> hc.getHelmChartType() != HelmChartType.CRD)
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(helmChart.getReleaseName())).findFirst()
                    .map(HelmChartBaseEntity::getRevisionNumber).orElse(null);
            triggerRollbackOperation(operation, helmChart.getReleaseName(), revisionNumber, actualInstance, false);
        }
    }

    public void triggerRollbackOperation(LifecycleOperation operation, String releaseName, String revisionNumber,
            final VnfInstance actualInstance, boolean isDowngradeOperation) {
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService
                .routeRollbackRequest(actualInstance, operation, releaseName, revisionNumber);
        if (workflowRoutingResponse.getHttpStatus().isError()) {
            OperationsUtils
                    .updateOperationOnFailure(workflowRoutingResponse.getErrorMessage(), operation, actualInstance,
                            releaseName);
        } else {
            if (isDowngradeOperation) {
                operation.setOperationState(LifecycleOperationState.PROCESSING);
            } else {
                operation.setOperationState(LifecycleOperationState.ROLLING_BACK);
            }
        }
        operation.setStateEnteredTime(LocalDateTime.now());
    }

    private void startRollbackFlowForDowngrade(final VnfInstance instance, final LifecycleOperation operation,
            final String targetOperationOccurrenceId, final HelmChart chart, boolean isDowngradeOperation) {
        LOGGER.info("Starting package rollback with downgrade operation {} for instance : {}",
                operation.getOperationOccurrenceId(), instance.getVnfInstanceName());
        final List<HelmChartHistoryRecord> helmChartHistoryRecords = helmChartHistoryService
                .getHelmChartHistoryRecordsByOperationId(targetOperationOccurrenceId);

        final Optional<HelmChartHistoryRecord> helmChart = helmChartHistoryRecords.stream()
                .filter(hc -> hc.getReleaseName().equals(chart.getReleaseName())).findFirst();

        helmChart.ifPresentOrElse(hc -> triggerRollbackOperation(operation, hc
            .getReleaseName(), hc.getRevisionNumber(), instance, isDowngradeOperation), () -> setOperationToFail(instance, operation));
    }

    private static void setOperationToFail(final VnfInstance instance, final LifecycleOperation operation) {
        LOGGER.error("Cannot downgrade instance: {} by rollback. There are no appropriate revision", instance);
        updateOperationState(operation, LifecycleOperationState.FAILED);
    }
}
