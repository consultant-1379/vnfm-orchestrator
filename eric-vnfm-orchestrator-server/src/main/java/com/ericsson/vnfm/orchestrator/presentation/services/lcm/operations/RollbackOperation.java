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

import static org.slf4j.LoggerFactory.getLogger;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.indexExists;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.fasterxml.jackson.core.type.TypeReference;

public interface RollbackOperation {

    Logger LOGGER = getLogger(RollbackOperation.class);

    RollbackType getType();

    Command getService(String service);

    void execute(LifecycleOperation operation);

    void triggerNextStage(MessageHandlingContext<HelmReleaseLifecycleMessage> context);

    default void triggerNextStage(final MessageHandlingContext<HelmReleaseLifecycleMessage> context,
                                  final boolean isDowngradeOperation,
                                  final MessageUtility utility,
                                  final InstanceService instanceService,
                                  final HelmChartHistoryService helmChartHistoryService) {

        LifecycleOperation operation = context.getOperation();
        HelmReleaseLifecycleMessage message = context.getMessage();
        VnfInstance actualInstance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> actualHelmCharts = actualInstance.getHelmCharts();
        List<HelmChart> tempHelmCharts = tempInstance.getHelmCharts();
        List<LinkedHashMap<String, String>> patternList = getPatternList(operation, isDowngradeOperation);
        int nextStage = OperationsUtils.getCurrentStage(patternList, message) + 1;

        utility.updateChartStateAndRevisionNumber(tempInstance, message.getReleaseName(),
                                                  message.getState().toString(),
                                                  message.getRevisionNumber());
        if (indexExists(patternList, nextStage)) {
            ImmutablePair<HelmChart, String> helmCommandPair = OperationsUtils
                    .getHelmCommandPair(patternList, actualHelmCharts, tempHelmCharts, false, nextStage);
            Command command = getService(helmCommandPair.getRight().toUpperCase());
            command.execute(operation, helmCommandPair.getLeft(), isDowngradeOperation);
            actualInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        } else {
            if (isDowngradeOperation) {
                completeDowngradeOperation(operation,
                                           actualInstance,
                                           tempInstance,
                                           utility,
                                           instanceService,
                                           helmChartHistoryService);
                Optional<List<ScaleInfoEntity>> scaleInfoEntityList = Optional.ofNullable(tempInstance.getScaleInfoEntity());
                scaleInfoEntityList.ifPresent(scaleInfoEntities -> scaleInfoEntities.forEach(scaleInfo -> scaleInfo.setVnfInstance(tempInstance)));
                context.setVnfInstance(tempInstance);
            } else {
                completeFailedRollbackOperation(operation, actualInstance, tempInstance, instanceService);
            }
        }
    }

    private static List<LinkedHashMap<String, String>> getPatternList(LifecycleOperation operation,
                                                                      boolean isDowngradeOperation) {
        if (isDowngradeOperation) {
            return Utility.parseJsonToGenericType(operation.getRollbackPattern(), new TypeReference<>() { });
        } else {
            return Utility.parseJsonToGenericType(operation.getFailurePattern(), new TypeReference<>() { });
        }
    }

    private static void completeDowngradeOperation(final LifecycleOperation currentOperation,
                                                   final VnfInstance instance,
                                                   final VnfInstance changedInstance,
                                                   final MessageUtility utility,
                                                   final InstanceService instanceService,
                                                   final HelmChartHistoryService helmChartHistoryService) {
        try {
            utility.updateUnusedInstance(instance, changedInstance.getVnfPackageId());
        } catch (Exception e) {
            LOGGER.error("update usage state api failed due to: {}. Flow will continue to update operation state.", e.getMessage());
        }

        instanceService.removeScaleEntriesFromInstance(instance);
        changedInstance.setCombinedValuesFile(changedInstance.getCombinedValuesFile());
        changedInstance.setCombinedAdditionalParams(changedInstance.getCombinedAdditionalParams());
        utility.updateInstanceOnChangeVnfPackageOperation(instance, changedInstance, currentOperation, LifecycleOperationState.COMPLETED);
        OperationsUtils.updateCompletedChangeVnfPackageOperation(changedInstance, currentOperation, LifecycleOperationState.COMPLETED);
        helmChartHistoryService.createAndPersistHistoryRecords(changedInstance.getHelmCharts(), currentOperation.getOperationOccurrenceId());
    }

    private static void completeFailedRollbackOperation(final LifecycleOperation operation,
                                                        final VnfInstance instance,
                                                        final VnfInstance upgradedInstance,
                                                        final InstanceService instanceService) {
        try {
            instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                      upgradedInstance.getVnfPackageId(),
                                                      upgradedInstance.getVnfPackageId(),
                                                      upgradedInstance.getVnfInstanceId(),
                                                      false);
        } catch (Exception e) {
            LOGGER.error("update usage state api failed due to: {}. Flow will continue to update operation state.", e.getMessage());
        }
        updateInstanceWithHelmChartState(instance, upgradedInstance);
        instance.setTempInstance(null);
        updateOperationState(operation, LifecycleOperationState.ROLLED_BACK);
    }

    private static void updateInstanceWithHelmChartState(VnfInstance instance, VnfInstance upgradedInstance) {
        Map<String, HelmChart> chartsByReleaseName = upgradedInstance.getHelmCharts()
                .stream()
                .collect(Collectors.toMap(HelmChart::getReleaseName, Function.identity()));

        instance.getHelmCharts().forEach(chart -> {
            chart.setState(chartsByReleaseName.get(chart.getReleaseName()).getState());
            chart.setRevisionNumber(chartsByReleaseName.get(chart.getReleaseName()).getRevisionNumber());
        });
    }
}
