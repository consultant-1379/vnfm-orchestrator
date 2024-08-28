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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.addInstance;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RollbackOperation extends AbstractLifeCycleOperationProcessor {

    private static final String DOWNGRADE_FAILED_MESSAGE = "Downgrade operation with id %s failed on release: %s.";

    private static final String UPDATE_USAGE_STATE_FAILED_MESSAGE = "Update usage state api failed. Flow will continue to update operation state.";

    private static final TypeReference<List<ScaleInfoEntity>> SCALE_INFO_ENTITY_LIST_REF = new TypeReference<>() {
    };

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;
    @Autowired
    private RollbackService rollbackService;

    @Override
    public Conditions getConditions() {
        return new Conditions(MessagingLifecycleOperationType.ROLLBACK.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    public void completed(HelmReleaseLifecycleMessage message) {
        LifecycleOperation operation = databaseInteractionService.getLifecycleOperation(message.getLifecycleOperationId());
        VnfInstance instance = operation.getVnfInstance();
        VnfInstance upgradedInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        upgradedInstance = utility.updateChartStateAndRevisionNumber(upgradedInstance, message.getReleaseName(),
                                                                     LifecycleOperationState.ROLLED_BACK.toString(), message.getRevisionNumber());
        instance.setTempInstance(convertObjToJsonString(upgradedInstance));

        final boolean chartWasFoundAndUpdated = rollbackService.rollbackNextChart(instance, upgradedInstance, operation);

        if (!chartWasFoundAndUpdated) {
            Optional<ChangePackageOperationDetails> changePackageOperationDetails =
                    changePackageOperationDetailsRepository.findById(operation.getOperationOccurrenceId());
            if (isDowngradeOperation(changePackageOperationDetails)) {
                String targetOperationId = changePackageOperationDetails.get().getTargetOperationOccurrenceId();
                completeDowngradeOperation(operation, targetOperationId, instance, upgradedInstance);
                instance = upgradedInstance;
            } else {
                completeRollbackOperation(message, operation, instance, upgradedInstance);
            }
        }
        databaseInteractionService.persistVnfInstanceAndOperation(instance, operation);
    }

    private void completeDowngradeOperation(final LifecycleOperation currentOperation,
                                            final String targetOperationId,
                                            final VnfInstance instance,
                                            final VnfInstance changedInstance) {
        LOGGER.info("Instance {} has been downgraded to version: {} ", changedInstance.getVnfInstanceName(),
                    changedInstance.getVnfSoftwareVersion());
        LifecycleOperation targetOperation = databaseInteractionService.getLifecycleOperation(targetOperationId);
        try {
            utility.updateUnusedInstance(instance, changedInstance.getVnfPackageId());
        } catch (Exception e) {
            LOGGER.warn(UPDATE_USAGE_STATE_FAILED_MESSAGE, e);
        }
        updateScaleInfo(targetOperation, instance, changedInstance);
        changedInstance.setCombinedValuesFile(targetOperation.getCombinedValuesFile());
        changedInstance.setCombinedAdditionalParams(targetOperation.getCombinedAdditionalParams());
        utility.updateInstanceOnChangeVnfPackageOperation(instance, changedInstance, currentOperation, LifecycleOperationState.COMPLETED);
        OperationsUtils.updateCompletedChangeVnfPackageOperation(changedInstance, currentOperation, LifecycleOperationState.COMPLETED);
        helmChartHistoryService.createAndPersistHistoryRecords(changedInstance.getHelmCharts(), currentOperation.getOperationOccurrenceId());
    }

    private void updateScaleInfo(final LifecycleOperation operation,
                                 final VnfInstance originalInstance,
                                 final VnfInstance changedInstance) {
        changedInstance.setResourceDetails(operation.getResourceDetails());
        instanceService.removeScaleEntriesFromInstance(originalInstance);
        List<ScaleInfoEntity> scaleInfoEntities = Optional.ofNullable(operation.getScaleInfoEntities())
                .map(json -> parseJsonToGenericType(json, SCALE_INFO_ENTITY_LIST_REF))
                .orElseGet(ArrayList::new);
        scaleInfoEntities.forEach(entity -> entity.setVnfInstance(changedInstance));
        changedInstance.setScaleInfoEntity(scaleInfoEntities);
    }

    private void completeRollbackOperation(final HelmReleaseLifecycleMessage message,
                                           final LifecycleOperation operation,
                                           final VnfInstance instance,
                                           final VnfInstance upgradedInstance) {
        Optional<HelmChart> helmChartFailed = upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.FAILED.toString()))
                .findFirst();
        if (helmChartFailed.isPresent()) {
            LOGGER.info("Rollback operation success for : {} but operation is failed due to failed chart name: {} ",
                        message.getReleaseName(), helmChartFailed.get().getHelmChartName());
            updateInstanceWithTemporaryHelmCharts(instance, upgradedInstance);
            updateOperationState(operation, LifecycleOperationState.FAILED);
        } else {
            try {
                instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                                 upgradedInstance.getVnfPackageId(),
                                                                                                 upgradedInstance.getVnfPackageId(),
                                                                                                 upgradedInstance.getVnfInstanceId(),
                                                                                                 false);
            } catch (Exception e) {
                LOGGER.warn(UPDATE_USAGE_STATE_FAILED_MESSAGE, e);
            }
            LOGGER.info("All the charts have been rolled back for : {} ", upgradedInstance.getVnfInstanceName());
            updateInstanceWithHelmChartState(instance, upgradedInstance);
            instance.setTempInstance(null);
            updateOperationState(operation, LifecycleOperationState.ROLLED_BACK);
        }
    }

    private static void updateInstanceWithTemporaryHelmCharts(VnfInstance instance, VnfInstance upgradedInstance) {
        instance.getHelmCharts().clear();
        List<HelmChart> upgradedInstanceHelmCharts = upgradedInstance.getHelmCharts();
        List<HelmChart> labelledHelmCharts = addInstance(instance, upgradedInstanceHelmCharts);
        instance.getHelmCharts().addAll(labelledHelmCharts);
        instance.setTempInstance(null);
    }

    private static void updateInstanceWithHelmChartState(VnfInstance instance, VnfInstance upgradedInstance) {
        Map<String, HelmChart> chartsByReleaseName = upgradedInstance.getHelmCharts()
                .stream()
                .collect(Collectors.toMap(HelmChart::getReleaseName, Function.identity()));

        instance.getHelmCharts().stream()
                .filter(chart -> chartsByReleaseName.containsKey(chart.getReleaseName()))
                .forEach(chart -> {
                    chart.setState(chartsByReleaseName.get(chart.getReleaseName()).getState());
                    chart.setRevisionNumber(chartsByReleaseName.get(chart.getReleaseName()).getRevisionNumber());
                });
    }

    @Override
    public void failed(HelmReleaseLifecycleMessage message) {
        LifecycleOperation operation = databaseInteractionService
                .getLifecycleOperation(message.getLifecycleOperationId());
        VnfInstance instance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        try {
            instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                             tempInstance.getVnfPackageId(),
                                                                                             tempInstance.getVnfPackageId(),
                                                                                             tempInstance.getVnfInstanceId(),
                                                                                             false);
        } catch (Exception e) {
            LOGGER.warn(UPDATE_USAGE_STATE_FAILED_MESSAGE, e);
        }
        String operationOccurrenceId = operation.getOperationOccurrenceId();
        if (isDowngradeOperation(changePackageOperationDetailsRepository.findById(operationOccurrenceId))) {
            final String errorLogMessage = String.format(DOWNGRADE_FAILED_MESSAGE, operationOccurrenceId, message.getReleaseName());
            utility.updateOperationOnFail(operationOccurrenceId, message.getMessage(), errorLogMessage,
                                          InstantiationState.INSTANTIATED, LifecycleOperationState.FAILED);
            return;
        }

        tempInstance = utility.updateChartState(tempInstance, message.getReleaseName(), LifecycleOperationState.FAILED.toString());
        String errorMsg = rollbackService.getErrorMsg(message.getMessage(), message.getReleaseName());
        appendError(errorMsg, operation);
        Optional<HelmChart> completedHelmChart = HelmChartUtils.getFirstProcessingCnfChart(tempInstance);
        if (completedHelmChart.isPresent()) {
            HelmChart helmChart = completedHelmChart.get();
            LOGGER.info("Starting rollback operation for : {} ", helmChart.getHelmChartName());
            tempInstance = utility.updateChartState(tempInstance, helmChart.getReleaseName(), LifecycleOperationState.ROLLING_BACK.toString());
            instance.setTempInstance(convertObjToJsonString(tempInstance));
            rollbackService.triggerRollbackOperation(operation, instance, helmChart);
        } else {
            instance.setTempInstance(null);
            updateInstanceWithTemporaryHelmCharts(instance, tempInstance);
            updateOperationState(operation, LifecycleOperationState.FAILED);
        }
        databaseInteractionService.persistVnfInstanceAndOperation(instance, operation);
    }

    @Override
    public void rollBack(HelmReleaseLifecycleMessage message) {
        try {
            LifecycleOperation operation = databaseInteractionService
                    .getLifecycleOperation(message.getLifecycleOperationId());
            VnfInstance instance = operation.getVnfInstance();
            VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);

            instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                             tempInstance.getVnfPackageId(),
                                                                                             tempInstance.getVnfPackageId(),
                                                                                             tempInstance.getVnfInstanceId(),
                                                                                             false);
        } catch (Exception e) {
            LOGGER.warn(UPDATE_USAGE_STATE_FAILED_MESSAGE, e);
        }
        utility.lifecycleTimedOut(message.getLifecycleOperationId(), InstantiationState.INSTANTIATED, message.getMessage());
    }
}
