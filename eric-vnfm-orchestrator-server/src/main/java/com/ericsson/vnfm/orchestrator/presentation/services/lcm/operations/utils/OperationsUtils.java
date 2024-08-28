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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE_PVC;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.INSTALL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.UPGRADE;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.vnfm.orchestrator.utils.RollbackPatternUtility;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class OperationsUtils {

    private static final List<String> VALID_COMMANDS = unmodifiableList(
            new ArrayList<>(asList("INSTALL", "ROLLBACK")));

    @Autowired
    private HealOperation healOperation;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    public static HelmChart retrieveFailedHelmChart(VnfInstance tempInstance, LifecycleOperation operation) {
        return tempInstance.getHelmCharts().stream()
                .filter(chart -> LifecycleOperationState.FAILED.toString().equals(chart.getState()))
                .findFirst()
                .or(() -> tempInstance.getTerminatedHelmCharts()
                        .stream()
                        .filter(chart -> LifecycleOperationState.FAILED.toString().equals(chart.getState()))
                        .filter(chart -> chart.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                        .map(HelmChartUtils::toHelmChart)
                        .findFirst())
                .orElseThrow(() -> new NotFoundException("Unable to identify the chart to rollback."));
    }

    public static Optional<HelmChart> retrieveFailingUpgradeHelmChart(VnfInstance sourceInstance, VnfInstance targetInstance) {
        return targetInstance.getHelmCharts().stream()
                .filter(chart -> chart.getState().equals(LifecycleOperationState.FAILED.toString()))
                .filter(helmChart -> sourceInstance.getHelmCharts()
                        .stream()
                        .anyMatch(chart -> chart.getReleaseName().equals(helmChart.getReleaseName())))
                .findFirst();
    }

    public static ImmutablePair<HelmChart, String> getFirstHelmCommandPair(final List<LinkedHashMap<String, String>> releaseNameCommandList,
                                                                           final List<HelmChart> actualHelmCharts,
                                                                           final List<HelmChart> tempHelmCharts,
                                                                           boolean isUpgrade) {
        return getHelmCommandPair(releaseNameCommandList, actualHelmCharts, tempHelmCharts, isUpgrade, 0);
    }

    public static ImmutablePair<HelmChart, String> getHelmCommandPair(final List<LinkedHashMap<String, String>> releaseNameCommandList,
                                                                      final List<HelmChart> actualHelmCharts, final List<HelmChart> tempHelmCharts,
                                                                      boolean isUpgrade, final int index) {
        LinkedHashMap<String, String> firstPair = releaseNameCommandList.get(index);
        Iterator<Map.Entry<String, String>> iterator = firstPair.entrySet().iterator();
        Map.Entry<String, String> entry = iterator.next();
        String releaseName = entry.getKey();
        String command = entry.getValue();
        boolean isNotFakeUpgrade = command.equalsIgnoreCase(UPGRADE) && isUpgrade;
        HelmChart helmChart = ((command.equalsIgnoreCase(INSTALL) || isNotFakeUpgrade) ? tempHelmCharts : actualHelmCharts).stream()
                .filter(x -> releaseName.equals(x.getReleaseName()))
                .findFirst().orElse(null);
        return new ImmutablePair<>(helmChart, command);
    }

    public static int getCurrentStage(final List<LinkedHashMap<String, String>> releaseNameCommandList,
                                      HelmReleaseLifecycleMessage message) {
        String releaseName = message.getReleaseName();
        String commandType = getCommandType(message);
        int count = 0;
        for (Map<String, String> map : releaseNameCommandList) {
            Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
            Map.Entry<String, String> entry = iterator.next();
            if (releaseName.equals(entry.getKey()) && entry.getValue().toLowerCase().startsWith(commandType.toLowerCase())) {
                break;
            }
            count++;
        }
        return count;
    }

    public static String getCommandType(HelmReleaseLifecycleMessage message) {
        HelmReleaseOperationType operationType = message.getOperationType();
        switch (operationType) {
            case TERMINATE:
                return DELETE;
            case INSTANTIATE:
                return INSTALL;
            case CHANGE_VNFPKG:
                return UPGRADE;
            default:
                return operationType.toString();
        }
    }

    public static boolean indexExists(final List<?> list, final int index) {
        return index >= 0 && index < list.size();
    }

    public static void updateOperationOnFailure(String errorMessage, LifecycleOperation operation,
                                                VnfInstance vnfInstance, String releaseName) {
        final String operationType = operation.getLifecycleOperationType().toString();
        updateChartState(vnfInstance, releaseName, LifecycleOperationState.FAILED.toString());
        String errorMsg = formatOperationErrorMessage(operationType,
                                                      releaseName,
                                                      errorMessage != null ? errorMessage : "Failure event did not contain an error message.");
        appendError(errorMsg, operation);
        operation.setOperationState(LifecycleOperationState.FAILED);
    }

    public static void updateChartState(VnfInstance vnfInstance, String releaseName, String state) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        LOGGER.info("Updating state to {} for release {}", state, releaseName);
        helmCharts.stream().filter(obj -> obj.getReleaseName().equalsIgnoreCase(releaseName)).findFirst()
                .ifPresent(o -> o.setState(state));
    }

    private static String formatOperationErrorMessage(String operationType, String releaseName, String errorMsg) {
        return String.format("%s failed for %s due to %s", operationType, releaseName, errorMsg);
    }

    @SuppressWarnings("unchecked")
    public static String[] parsePvcLabels(final LifecycleOperation operation, String releaseName) {
        List<LinkedHashMap<String, String>> patternList = RollbackPatternUtility.getPattern(operation);
        return patternList
                .stream().map(pattern -> pattern.get(releaseName))
                .filter(pattern -> StringUtils.containsIgnoreCase(pattern, DELETE_PVC))
                .filter(pattern -> pattern.contains("[") && pattern.contains("]"))
                .map(pattern -> pattern.substring(pattern.indexOf("[") + 1, pattern.indexOf("]")).split(","))
                .flatMap(Stream::of)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    public static void logPattern(final String vnfInstanceId, final LifecycleOperation operation,
                                  final List<LinkedHashMap<String, String>> pattern) {
        String logMessage = "for instance {}, executing the following pattern {} for operation {}";
        String patternAsString = Utility.convertObjToJsonString(pattern);
        String errorMessage;
        if (isValidPattern(pattern)) {
            errorMessage = String.format("Pattern is valid for %s", logMessage);
            LOGGER.info(errorMessage, vnfInstanceId, patternAsString, operation.getOperationOccurrenceId());
        } else {
            errorMessage = String.format("Pattern may lead to a non deterministic state %s", logMessage);
            LOGGER.warn(errorMessage, vnfInstanceId, patternAsString, operation.getOperationOccurrenceId());
        }
    }

    @VisibleForTesting
    public static boolean isValidPattern(final List<LinkedHashMap<String, String>> pattern) {
        Iterator<LinkedHashMap<String, String>> iterator = pattern.iterator();
        Map<String, String> commands = new HashMap<>();
        while (iterator.hasNext()) {
            LinkedHashMap<String, String> commandPair = iterator.next();
            Map.Entry<String, String> entry = commandPair.entrySet().iterator().next();
            commands.put(entry.getKey(), entry.getValue().toUpperCase());
        }

        return CollectionUtils.isEmpty(commands.values()
                                               .stream()
                                               .filter(command -> !VALID_COMMANDS.contains(command))
                                               .collect(Collectors.toList()));
    }

    public Map<String, Object> getAdditionalParamsFromLastOperation(final VnfInstance vnfInstance, final boolean isDowngradeOperation) {
        int skip = isDowngradeOperation ? 1 : 0;
        Optional<LifecycleOperation> installOrUpgradeOperation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, skip);
        return installOrUpgradeOperation
                .map((LifecycleOperation operation) -> getAdditionalParamsFromOperation(vnfInstance, operation)).orElse(emptyMap());
    }

    public Map<String, Object> getAdditionalParamsFromOperation(final VnfInstance vnfInstance, final LifecycleOperation lifecycleOperation) {
        Optional<InstantiateVnfRequest> lastVnfRequest =
                Optional.ofNullable(healOperation.getInstantiateVnfRequest(lifecycleOperation, vnfInstance));
        return lastVnfRequest.map(instantiateVnfRequest -> {
            Map<String, Object> additionalParams = checkAndCastObjectToMap(instantiateVnfRequest.getAdditionalParams());
            Optional<String> valuesFileString = Optional.ofNullable(lifecycleOperation.getValuesFileParams());
            valuesFileString.ifPresent(valuesFile -> additionalParams.putAll(convertStringToJSONObj(valuesFile)));
            return additionalParams;
        }).orElse(emptyMap());
    }

    @SuppressWarnings("java:S135")
    public List<LifecycleOperation> getSelfUpgradesSeriesWhichBeginningFromOperation(
            String targetPackageId, LifecycleOperation beginOperation) {

        final VnfInstance vnfInstance = beginOperation.getVnfInstance();
        List<LifecycleOperation> selfUpgradesSeries = new ArrayList<>();
        boolean startingOperationOccurred = false;
        boolean scaleOccurred = false;
        List<LifecycleOperation> lifecycleOperations = lcmOpSearchService.searchAllNotFailedInstallOrUpgradeOrScaleOperations(vnfInstance);
        for (final LifecycleOperation operation : lifecycleOperations) {
            scaleOccurred = LifecycleOperationType.SCALE.equals(operation.getLifecycleOperationType());
            if (scaleOccurred) {
                continue; // SM-139154
            }
            final boolean operationIsBeginOperation =
                    beginOperation.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId());
            if (operationIsBeginOperation) {
                startingOperationOccurred = true; // self-upgrade series begin
            }
            if (startingOperationOccurred) {
                if ((operationIsBeginOperation && targetPackageId.equals(operation.getSourceVnfdId()))
                                || targetPackageId.equals(operation.getTargetVnfdId())) {
                    selfUpgradesSeries.add(operation);
                } else {
                    break; // self-upgrade series is ended
                }
            }
        }
        return selfUpgradesSeries;
    }

    public Map<String, Object> getScaleParamsLastOperation(final VnfInstance vnfInstance,
                                                           final HelmChart helmChart,
                                                           final boolean isDowngradeOperation) {
        int skip = isDowngradeOperation ? 1 : 0;
        Optional<LifecycleOperation> installOrUpgradeOperation = lcmOpSearchService
                .searchLastCompletedInstallOrUpgradeOrScaleOperation(vnfInstance, skip);
        return installOrUpgradeOperation.map(lifecycleOperation -> getReplicaDetailsFromHelmHistory(lifecycleOperation, helmChart))
                .orElse(new HashMap<>());
    }

    private Map<String, Object> getReplicaDetailsFromHelmHistory(final LifecycleOperation installOrUpgradeOperation, final HelmChart helmChart) {
        final List<HelmChartHistoryRecord> helmChartHistoryRecords = helmChartHistoryService
                .getHelmChartHistoryRecordsByOperationId(installOrUpgradeOperation.getOperationOccurrenceId());
        final Optional<HelmChartHistoryRecord> helmChartHistoryRecord = helmChartHistoryRecords.stream()
                .filter(hc -> hc.getReleaseName().equals(helmChart.getReleaseName())).findFirst();
        return helmChartHistoryRecord.map(historyRecord ->
                                                  replicaDetailsMapper.getReplicaDetailsFromHelmHistory(historyRecord)).orElse(new HashMap<>());
    }

    /**
     * Merge common fields from VnfInstance to LifecycleOperation. Call this method before storing operation during
     * lifecycle operations.
     *
     * @param instance  - VnfInstance with parameters which should be stored in operation also
     * @param operation - LifeCycleOperation
     */
    public static void mergeFieldsFromVnfInstanceToLifeCycleOperation(VnfInstance instance, LifecycleOperation operation) {
        operation.setTargetVnfdId(instance.getVnfDescriptorId());
        operation.setCombinedAdditionalParams(instance.getCombinedAdditionalParams());
        operation.setCombinedValuesFile(instance.getCombinedValuesFile());
        operation.setResourceDetails(instance.getResourceDetails());
    }

    public static void updateCompletedChangeVnfPackageOperation(VnfInstance changedInstance,
                                                                LifecycleOperation operation,
                                                                LifecycleOperationState lifecycleOperationState) {

        operation.setVnfInstance(changedInstance);
        updateOperationState(operation, lifecycleOperationState);
        operation.setVnfProductName(changedInstance.getVnfProductName());
        operation.setVnfSoftwareVersion(changedInstance.getVnfSoftwareVersion());

        mergeFieldsFromVnfInstanceToLifeCycleOperation(changedInstance, operation);
    }
}
