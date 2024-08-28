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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE_PVC;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.INSTALL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.ROLLBACK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.UPGRADE;
import static com.ericsson.vnfm.orchestrator.presentation.services.mapper.CcvpPatternMapper.mapHelmChartArtifactKeyToReleaseName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.google.common.annotations.VisibleForTesting;

@Component
public class CcvpPatternTransformerImpl implements CcvpPatternTransformer {

    @Autowired
    private PackageService packageService;
    @Autowired
    private DatabaseInteractionService databaseInteractionService;
    @Autowired
    private InstanceService instanceService;

    @Override
    public List<MutablePair<String, String>> saveRollbackFailurePatternInOperationForOperationRollback(VnfInstance sourceVnfInstance,
                                                                                                       VnfInstance targetVnfInstance,
                                                                                                       LifecycleOperation lifecycleOperation,
                                                                                                       HelmChart failedHelmChart) {
        List<MutablePair<String, String>> chartCommandList =
                instanceService.getHelmChartCommandRollbackFailurePattern(lifecycleOperation, targetVnfInstance, failedHelmChart);

        chartCommandList = transformChartCommandList(sourceVnfInstance,
                targetVnfInstance,
                chartCommandList,
                true);

        if (!CollectionUtils.isEmpty(chartCommandList)) {
            lifecycleOperation.setFailurePattern(Utility.convertObjToJsonString(chartCommandList));
            targetVnfInstance.setHelmCharts(sourceVnfInstance.getHelmCharts());
            sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));
        }
        return chartCommandList;
    }

    @Override
    public void saveRollbackPatternInOperationForDowngradeCcvp(final ChangeOperationContext context) {
        List<MutablePair<String, String>> chartCommandList =
                instanceService.getHelmChartCommandRollbackPattern(
                        context.getSourceVnfInstance(), context.getTempInstance());

        chartCommandList = transformChartCommandList(context.getSourceVnfInstance(), context.getTempInstance(), chartCommandList,
                true);

        if (!CollectionUtils.isEmpty(chartCommandList)) {
            LifecycleOperation lifecycleOperation = context.getOperation();
            lifecycleOperation.setRollbackPattern(Utility.convertObjToJsonString(chartCommandList));
            databaseInteractionService.persistLifecycleOperation(lifecycleOperation);
        }
    }

    @Override
    public void saveUpgradePatternInOperationCcvp(ChangeOperationContext context) {
        List<MutablePair<String, String>> chartCommandList =
                instanceService.getHelmChartCommandUpgradePattern(
                        context.getSourceVnfInstance(), context.getTempInstance());

        chartCommandList = transformChartCommandList(context.getSourceVnfInstance(), context.getTempInstance(), chartCommandList,
                false);

        if (!CollectionUtils.isEmpty(chartCommandList)) {
            LifecycleOperation lifecycleOperation = context.getOperation();
            lifecycleOperation.setUpgradePattern(Utility.convertObjToJsonString(chartCommandList));
            databaseInteractionService.persistLifecycleOperation(lifecycleOperation);
        }
    }

    @VisibleForTesting
    protected List<MutablePair<String, String>> transformChartCommandList(final VnfInstance sourceVnfInstance,
                                                                          final VnfInstance targetVnfInstance,
                                                                          final List<MutablePair<String, String>> chartCommandList,
                                                                          boolean isRollback) {
        if (!CollectionUtils.isEmpty(chartCommandList)) {
            List<MutablePair<String, String>> processedChartCommandList =
                    mapHelmChartArtifactKeyToReleaseName(chartCommandList, sourceVnfInstance.getHelmCharts(), targetVnfInstance.getHelmCharts());

            if (isRollback) {
                processedChartCommandList = transformChartCommandListForDisabledCharts(sourceVnfInstance, targetVnfInstance,
                        processedChartCommandList);
            }

            processedChartCommandList = addDeletePatternBeforeDeletePvcPatternIfRequired(processedChartCommandList);

            return removeDuplicatesFromPatterns(processedChartCommandList);
        }
        return Collections.emptyList();
    }

    private List<MutablePair<String, String>> transformChartCommandListForDisabledCharts(
            final VnfInstance sourceVnfInstance,
            final VnfInstance targetVnfInstance,
            final List<MutablePair<String, String>> chartCommandList) {

        List<MutablePair<String, String>> result = new ArrayList<>();

        for (final MutablePair<String, String> chartWithCommand : chartCommandList) {
            final List<MutablePair<String, String>> renderedCommands =
                    transformPatternCommand(sourceVnfInstance, targetVnfInstance, chartWithCommand);
            result.addAll(renderedCommands);
        }

        return result;
    }

    @SuppressWarnings("java:S1541")
    private List<MutablePair<String, String>> transformPatternCommand(final VnfInstance sourceVnfInstance,
                                                                      final VnfInstance targetVnfInstance,
                                                                      final MutablePair<String, String> chartWithCommand) {

        final List<MutablePair<String, String>> result = new ArrayList<>();
        final String releaseName = chartWithCommand.getLeft();
        final String commandName = chartWithCommand.getRight();
        final HelmChart sourceChart = getChartWithReleaseName(sourceVnfInstance, releaseName);
        final HelmChart targetChart = getChartWithReleaseName(targetVnfInstance, releaseName);

        /*
        Implementation notes.
        4 cases:
        1. DM enabled -> DM enabled: no transformations.
        2. DM disabled -> DM disabled: ignore all commands.
        3. DM disabled -> DM enabled: ignore delete, delete_pvc; rollback -> install.
        4. DM enabled -> DM disabled: ignore install; upgrade -> delete + delete_pvc; rollback -> delete + delete_pvc.
         */
        final boolean sourceChartEnabled = sourceChart != null && sourceChart.isChartEnabled();
        final boolean targetChartEnabled = targetChart != null && targetChart.isChartEnabled();
        if (sourceChartEnabled && targetChartEnabled) {
            if (shouldMapRollbackOrUpgradeToDeleteWithInstall(chartWithCommand, sourceChart, targetChart)) {
                result.add(new MutablePair<>(releaseName, DELETE));
                result.add(new MutablePair<>(releaseName, DELETE_PVC));
                result.add(new MutablePair<>(releaseName, INSTALL));
            } else {
                result.add(chartWithCommand);
            }
        } else if (!sourceChartEnabled && !targetChartEnabled) {
            // skip this pattern, no need to upgrade, rollback, delete or install

        } else if (!sourceChartEnabled && targetChartEnabled) {
            List<String> commandsToIgnore = List.of(DELETE, DELETE_PVC);
            if (ROLLBACK.equalsIgnoreCase(commandName)) {
                result.add(new MutablePair<>(releaseName, INSTALL));
            } else if (!commandsToIgnore.contains(commandName)) {
                result.add(chartWithCommand);
            }
        } else if (sourceChartEnabled && !targetChartEnabled) {
            List<String> commandsToIgnore = List.of(INSTALL);
            if (commandName.equalsIgnoreCase(UPGRADE)
                    || commandName.equalsIgnoreCase(ROLLBACK)) {
                result.add(new MutablePair<>(releaseName, DELETE));
                result.add(new MutablePair<>(releaseName, DELETE_PVC));
            } else if (!commandsToIgnore.contains(commandName)) {
                result.add(chartWithCommand);
            }
        }
        return result;
    }

    private boolean shouldMapRollbackOrUpgradeToDeleteWithInstall(final MutablePair<String, String> chartWithCommand,
                                                                  final HelmChart sourceChart,
                                                                  final HelmChart targetChart) {
        return (chartWithCommand.getRight().equalsIgnoreCase(ROLLBACK)
                || chartWithCommand.getRight().equalsIgnoreCase(UPGRADE))
                && !sourceChart.getHelmChartName().equalsIgnoreCase(targetChart.getHelmChartName());
    }

    private HelmChart getChartWithReleaseName(final VnfInstance sourceVnfInstance, final String releaseName) {
        return sourceVnfInstance.getHelmCharts().stream()
                .filter(chart -> releaseName.equalsIgnoreCase(chart.getReleaseName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds delete command as a pre step if the pattern contains delete_pvc.
     * Will only add if delete is not already defined as a pre step.
     *
     * @param releaseNameCommandList
     * @return
     */
    public static List<MutablePair<String, String>> addDeletePatternBeforeDeletePvcPatternIfRequired(
            List<MutablePair<String, String>> releaseNameCommandList) {
        List<MutablePair<String, String>> pattern = new ArrayList<>();
        Map<String, String> previousDeleteCommands = new HashMap<>();

        for (MutablePair<String, String> pair : releaseNameCommandList) {
            String value = pair.getValue();
            String key = pair.getKey();

            if (DELETE.equals(value)) {
                previousDeleteCommands.put(key, value);
            } else if (value.contains(DELETE_PVC) && !previousDeleteCommands.containsKey(key)) {
                pattern.add(new MutablePair<>(key, DELETE));
            }
            pattern.add(pair);
        }
        return pattern;
    }

    /**
     * Removes any duplicate commands found in pattern.
     *
     * @param releaseNameCommandList
     * @return
     */
    public static List<MutablePair<String, String>> removeDuplicatesFromPatterns(List<MutablePair<String, String>> releaseNameCommandList) {
        List<MutablePair<String, String>> patternWithoutDuplicates = new ArrayList<>();
        Map<String, List<String>> patternCommands = new HashMap<>();

        for (MutablePair<String, String> pair : releaseNameCommandList) {
            String value = pair.getValue();
            String key = pair.getKey();

            if (patternCommands.containsKey(key)) {
                if (patternCommands.get(key).stream().noneMatch(value::equals)) {
                    patternCommands.get(key).add(value);
                    patternWithoutDuplicates.add(pair);
                }
            } else {
                patternCommands.put(key, new ArrayList<>(Arrays.asList(value)));
                patternWithoutDuplicates.add(pair);
            }
        }
        return patternWithoutDuplicates;
    }
}
