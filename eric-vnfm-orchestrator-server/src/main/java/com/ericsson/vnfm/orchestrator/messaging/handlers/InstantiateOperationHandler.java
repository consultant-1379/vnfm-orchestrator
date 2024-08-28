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

import static java.util.stream.Collectors.toMap;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnCompleted;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Map;
import java.util.function.Function;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class InstantiateOperationHandler implements HealHandler {

    private HelmChartHistoryService helmChartHistoryService;
    private ReplicaCountCalculationService replicaCountCalculationService;

    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        HelmReleaseLifecycleMessage message = context.getMessage();

        if (LifecycleOperationType.TERMINATE.toString().equals(message.getOperationType().toString())) {
            updateOperationAndInstanceOnCompleted(operation, NOT_INSTANTIATED, instance, LifecycleOperationState.ROLLED_BACK);
        } else {
            setValuesForCompleted(instance, operation);
            updateOperationAndInstanceOnCompleted(operation, INSTANTIATED, instance, LifecycleOperationState.COMPLETED);
        }
    }

    private void setValuesForCompleted(VnfInstance instance, LifecycleOperation operation) {
        updateInstanceModel(instance);
        updateOperationState(operation, LifecycleOperationState.COMPLETED);
        OperationsUtils.mergeFieldsFromVnfInstanceToLifeCycleOperation(instance, operation);
        if (!Strings.isNullOrEmpty(instance.getTempInstance())) {
            VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
            setRevisionToTempInstance(instance, tempInstance);
            helmChartHistoryService
                    .createAndPersistHistoryRecords(tempInstance.getHelmCharts(), operation.getOperationOccurrenceId());
        } else {
            helmChartHistoryService
                    .createAndPersistHistoryRecords(instance.getHelmCharts(), operation.getOperationOccurrenceId());
        }
    }

    private static void setRevisionToTempInstance(final VnfInstance instance, final VnfInstance tempInstance) {
        Map<String, HelmChart> helmChartMap = instance.getHelmCharts()
                .stream()
                .collect(toMap(HelmChart::getReleaseName, Function.identity()));
        tempInstance.getHelmCharts()
                .forEach(chart -> {
                    HelmChart helmChart = helmChartMap.get(chart.getReleaseName());
                    chart.setRevisionNumber(helmChart != null ? helmChart.getRevisionNumber() : null);
                });
    }

    private void updateInstanceModel(final VnfInstance instance) {
        final VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        instance.setResourceDetails(replicaCountCalculationService.getResourceDetails(tempInstance));
        instance.setCleanUpResources(false);
    }
}
