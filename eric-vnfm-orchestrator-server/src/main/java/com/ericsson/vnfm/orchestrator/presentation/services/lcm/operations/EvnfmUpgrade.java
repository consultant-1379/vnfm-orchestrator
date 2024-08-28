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

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.*;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.indexExists;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getNextHelmChartToTerminate;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

@Slf4j
@Component
public class EvnfmUpgrade implements UpgradeOperation {

    @Autowired
    private CcvpPatternCommandFactory ccvpPatternCommandFactory;

    @Autowired
    private MessageUtility messageUtility;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Override
    public void execute(LifecycleOperation operation) {
        VnfInstance actualInstance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> actualHelmCharts = actualInstance.getHelmCharts();
        List<HelmChart> tempHelmCharts = tempInstance.getHelmCharts();
        List<TerminatedHelmChart> terminatedCharts = Optional.ofNullable(tempInstance.getTerminatedHelmCharts())
                .orElse(Collections.emptyList());

        List<LinkedHashMap<String, String>> upgradePatternList = getPatternList(operation, terminatedCharts);
        LOGGER.info("for instance {}, executing the following pattern {} for operation {}", actualInstance.getVnfInstanceId(),
                upgradePatternList, operation);
        ImmutablePair<HelmChart, String> firstHelmChartCommandPair = OperationsUtils.getFirstHelmCommandPair(
                upgradePatternList, actualHelmCharts, tempHelmCharts, true);
        if (firstHelmChartCommandPair.getLeft() != null) {
            firstHelmChartCommandPair.getLeft().setState(LifecycleOperationState.PROCESSING.toString());
            actualInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));

            LOGGER.info("Executing {} on the helm helm chart {}, release name {}", firstHelmChartCommandPair.getRight(),
                    firstHelmChartCommandPair.getLeft().getHelmChartUrl(), firstHelmChartCommandPair.getLeft().getReleaseName());
            Command command = getService(firstHelmChartCommandPair.getRight().toUpperCase());
            command.execute(operation, firstHelmChartCommandPair.getLeft(), false);
        } else {
            String releaseName = upgradePatternList.get(0).values().stream().findFirst().orElse(null);
            throw new NotFoundException(String.format("No matching CNF Helm Chart has been found for release : %s.",
                    releaseName));
        }
    }

    @Override
    public void triggerNextStage(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        HelmReleaseLifecycleMessage message = context.getMessage();
        VnfInstance actualInstance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> actualHelmCharts = actualInstance.getHelmCharts();
        List<HelmChart> tempHelmCharts = tempInstance.getHelmCharts();
        List<TerminatedHelmChart> terminatedCharts = Optional.ofNullable(tempInstance.getTerminatedHelmCharts())
                .orElse(Collections.emptyList());
        List<LinkedHashMap<String, String>> patternList = getPatternList(operation, terminatedCharts);

        messageUtility.updateChartStateAndRevisionNumber(tempInstance, message.getReleaseName(),
                message.getState().toString(),
                message.getRevisionNumber());
        actualInstance.setTempInstance(convertObjToJsonString(tempInstance));
        databaseInteractionService.saveVnfInstanceToDB(actualInstance);

        /*PVC deletion is automatically triggered after chart termination executed. That is why in some cases
        it can cause the message received with helm release operation type "DELETE_PVC" while it is absent in
        chart commands list*/
        checkDeleteCommandPresentWithoutDeletePvc(tempInstance, message, patternList);
        int nextStage = getCurrentStage(patternList, message, tempInstance) + 1;
        if (indexExists(patternList, nextStage)) {
            ImmutablePair<HelmChart, String> helmCommandPair = OperationsUtils
                    .getHelmCommandPair(patternList, actualHelmCharts, tempHelmCharts, true, nextStage);
            if (helmCommandPair.getLeft() != null) {
                LOGGER.info("Executing {} on the helm helm chart {}, release name {}", helmCommandPair.getRight(),
                        helmCommandPair.getLeft().getHelmChartUrl(), helmCommandPair.getLeft().getReleaseName());
                helmCommandPair.getLeft().setState(LifecycleOperationState.PROCESSING.toString());
                Command command = getService(helmCommandPair.getRight().toUpperCase());
                command.execute(operation, helmCommandPair.getLeft(), false);
                actualInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
            } else {
                String releaseName = patternList.get(nextStage).values().stream().findFirst().orElse(null);
                String errorMessage = String.format("No matching CNF Helm Chart has been found for release : %s.",
                        releaseName);
                OperationsUtils.updateOperationOnFailure(errorMessage, operation, actualInstance, releaseName);
            }
        } else {
            checkForChartsToTerminate(operation, actualInstance, tempInstance, context);
        }
    }

    @Override
    public Command getService(String service) {
        return ccvpPatternCommandFactory.getService(service);
    }

    private int getCurrentStage(List<LinkedHashMap<String, String>> patternList, HelmReleaseLifecycleMessage message,
                                VnfInstance tempInstance) {
        if (isTerminationOfDisabledHelmChartCompleted(tempInstance, message)) {
            return patternList.size() - 1;
        }
        return OperationsUtils.getCurrentStage(patternList, message);
    }

    private boolean isTerminationOfDisabledHelmChartCompleted(VnfInstance tempInstance, HelmReleaseLifecycleMessage message) {
        boolean isChartTerminated = Optional.ofNullable(tempInstance.getTerminatedHelmCharts()).orElseGet(ArrayList::new)
                .stream()
                .filter(chart -> Objects.equals(chart.getReleaseName(), message.getReleaseName()))
                .anyMatch(chart -> Objects.equals(chart.getOperationOccurrenceId(), message.getLifecycleOperationId()));
        return message.getOperationType() == HelmReleaseOperationType.DELETE_PVC && isChartTerminated;
    }

    private void checkForChartsToTerminate(LifecycleOperation operation, VnfInstance currentInstance,
                                           VnfInstance tempInstance,
                                           MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("All charts have been upgraded, checking if termination in necessary");
        Optional<TerminatedHelmChart> nextChartToTerminate = getNextHelmChartToTerminate(tempInstance, operation);
        if (nextChartToTerminate.isEmpty()) {
            LOGGER.info("All charts have been processed");
            completeUpgradeOperation(operation, currentInstance, tempInstance, messageUtility, instanceService,
                    helmChartHistoryService);
            Optional<List<ScaleInfoEntity>> scaleInfoEntityList = Optional.ofNullable(tempInstance.getScaleInfoEntity());
            scaleInfoEntityList.ifPresent(scaleInfoEntities -> scaleInfoEntities.forEach(scaleInfo -> scaleInfo.setVnfInstance(tempInstance)));
            context.setVnfInstance(tempInstance);
        } else {
            TerminatedHelmChart helmChartToTerminate = nextChartToTerminate.get();
            LOGGER.info("Next chart name to terminate is {}", helmChartToTerminate.getHelmChartName());
            WorkflowRoutingResponse response = workflowRoutingService
                    .routeTerminateRequest(tempInstance, operation, helmChartToTerminate.getReleaseName());
            if (response.getHttpStatus().isError()) {
                OperationsUtils.updateOperationOnFailure(response.getErrorMessage(),
                        operation,
                        currentInstance,
                        helmChartToTerminate.getReleaseName());
            }
        }
    }

    private List<LinkedHashMap<String, String>> getPatternList(LifecycleOperation operation,
                                                               List<TerminatedHelmChart> terminatedHelmCharts) {
        List<LinkedHashMap<String, String>> patternList = Utility
                .parseJsonToGenericType(operation.getUpgradePattern(), new TypeReference<>() {
                });
        List<TerminatedHelmChart> currentOperationTerminatedCharts = terminatedHelmCharts.stream()
                .filter(chart -> Objects.equals(chart.getOperationOccurrenceId(), operation.getOperationOccurrenceId()))
                .collect(Collectors.toList());
        return getEnabledChartsCommands(patternList, currentOperationTerminatedCharts);
    }

    private List<LinkedHashMap<String, String>> getEnabledChartsCommands(List<LinkedHashMap<String, String>> patternList,
                                                                         List<TerminatedHelmChart> terminatedCharts) {
        List<String> disabledReleases = terminatedCharts.stream()
                .map(HelmChartBaseEntity::getReleaseName)
                .collect(Collectors.toList());
        return patternList.stream()
                .filter(command -> !disabledReleases.contains(command.keySet().stream().findFirst().orElse(null)))
                .collect(Collectors.toList());
    }

    private static void completeUpgradeOperation(final LifecycleOperation currentOperation,
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

    private void checkDeleteCommandPresentWithoutDeletePvc(VnfInstance vnfInstance,
                                                           HelmReleaseLifecycleMessage message,
                                                           List<LinkedHashMap<String, String>> patternList) {
        if (Objects.equals(DELETE_PVC, OperationsUtils.getCommandType(message).toLowerCase()) &&
                !isTerminationOfDisabledHelmChartCompleted(vnfInstance, message)) {
            List<String> releaseCommands = patternList.stream()
                    .filter(command -> Objects.equals(message.getReleaseName(), command.keySet().stream()
                            .findFirst()
                            .orElse(null)))
                    .flatMap(command -> command.values().stream())
                    .collect(Collectors.toList());
            boolean deleteCommandPresentWithoutDeletePvc = !releaseCommands.contains(DELETE_PVC) &&
                    releaseCommands.contains(DELETE);
            if (deleteCommandPresentWithoutDeletePvc) {
                message.setOperationType(HelmReleaseOperationType.TERMINATE);
            }
        }
    }
}
