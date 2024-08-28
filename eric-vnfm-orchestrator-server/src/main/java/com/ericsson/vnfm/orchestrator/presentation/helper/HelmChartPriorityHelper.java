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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

public final class HelmChartPriorityHelper {
    private HelmChartPriorityHelper() {
    }

    public static List<HelmChart> getHelmChartsWithReversedTerminate(final VnfInstance vnfInstance, final List<HelmChart> helmCharts) {
        Map<HelmChart, Integer> lastOperationHelmChartPriorityMap = getLastOperationHelmChartPriorityMap(vnfInstance, helmCharts);
        List<HelmChart> reverseSortedHelmCharts = getReverseSortedHelmCharts(lastOperationHelmChartPriorityMap);
        int priorityIndex = 1;
        for (HelmChart helmChart : reverseSortedHelmCharts) {
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            operationChartsPriority.putIfAbsent(LifecycleOperationType.TERMINATE, priorityIndex);
            helmChart.setOperationChartsPriority(operationChartsPriority);
            priorityIndex++;
        }
        return reverseSortedHelmCharts;
    }

    public static Map<LifecycleOperationType, Integer> calculatePriority(final HelmChart helmChartToChange,
                       final Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap) {
        Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap =
                new EnumMap<>(LifecycleOperationType.class);

        List<HelmPackage> vnfInstanceHelmPackagesInstantiate =
                vnfLcmPrioritizedHelmPackageMap.get(LCMOperationsEnum.INSTANTIATE);
        lifecycleOperationTypeIntegerMap.put(LifecycleOperationType.INSTANTIATE,
                getInstantiatePriority(helmChartToChange, vnfInstanceHelmPackagesInstantiate));

        int scalePriority = getScalePriority(helmChartToChange, vnfLcmPrioritizedHelmPackageMap, vnfInstanceHelmPackagesInstantiate);
        lifecycleOperationTypeIntegerMap.put(LifecycleOperationType.SCALE, scalePriority);

        Integer terminatePriority = getTerminatePriority(helmChartToChange, vnfLcmPrioritizedHelmPackageMap);

        if (terminatePriority != null) {
            lifecycleOperationTypeIntegerMap.put(LifecycleOperationType.TERMINATE, terminatePriority);
        }
        lifecycleOperationTypeIntegerMap.put(LifecycleOperationType.CHANGE_VNFPKG,
                getCcvpPriority(helmChartToChange, vnfLcmPrioritizedHelmPackageMap));

        return lifecycleOperationTypeIntegerMap;
    }

    private static int getInstantiatePriority(final HelmChart helmChartToChange,
                                              final List<HelmPackage> vnfInstanceHelmPackagesInstantiate) {
        int instantiatePriority = helmChartToChange.getPriority();
        if (vnfInstanceHelmPackagesInstantiate != null) {
            instantiatePriority = getPriority(vnfInstanceHelmPackagesInstantiate, helmChartToChange);
        }
        return instantiatePriority;
    }

    private static int getScalePriority(final HelmChart helmChartToChange,
                                        final Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap,
                                        final List<HelmPackage> vnfInstanceHelmPackagesInstantiate) {
        int scalePriority = helmChartToChange.getPriority();
        List<HelmPackage> vnfInstanceHelmPackagesScale = vnfLcmPrioritizedHelmPackageMap.get(LCMOperationsEnum.SCALE);
        if (vnfInstanceHelmPackagesScale != null) {
            scalePriority = getPriority(vnfInstanceHelmPackagesScale, helmChartToChange);
        } else if (vnfInstanceHelmPackagesInstantiate != null) {
            scalePriority = getPriority(vnfInstanceHelmPackagesInstantiate, helmChartToChange);
        }
        return scalePriority;
    }

    private static Integer getTerminatePriority(final HelmChart helmChartToChange,
                                                final Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap) {
        Integer terminatePriority = null;
        List<HelmPackage> vnfInstanceHelmPackagesTerminate = vnfLcmPrioritizedHelmPackageMap.get(LCMOperationsEnum.TERMINATE);
        if (vnfInstanceHelmPackagesTerminate != null) {
            terminatePriority = getTerminatePriority(vnfInstanceHelmPackagesTerminate, helmChartToChange);
        }
        return terminatePriority;
    }

    private static int getCcvpPriority(final HelmChart helmChartToChange,
                                       final Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap) {
        int ccvpPriority = helmChartToChange.getPriority();
        List<HelmPackage> vnfInstanceHelmPackagesCCVP = getHelmPackagesForCCVP(vnfLcmPrioritizedHelmPackageMap);
        if (vnfInstanceHelmPackagesCCVP != null) {
            ccvpPriority = getPriority(vnfInstanceHelmPackagesCCVP, helmChartToChange);
        }
        return ccvpPriority;
    }

    private static List<HelmPackage> getHelmPackagesForCCVP(final Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap) {
        List<HelmPackage> helmPackages = vnfLcmPrioritizedHelmPackageMap.get(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE);
        if (helmPackages == null) {
            helmPackages = vnfLcmPrioritizedHelmPackageMap.get(LCMOperationsEnum.CHANGE_VNFPKG);
        }
        return helmPackages;
    }

    private static Integer getPriority(final List<HelmPackage> vnfInstanceHelmPackages, final HelmChart helmChartToChange) {
        return vnfInstanceHelmPackages.stream().filter(helmPackage -> helmPackage.getId().equals(helmChartToChange.getHelmChartArtifactKey()))
                .map(HelmPackage::getPriority)
                .findFirst().orElse(helmChartToChange.getPriority());
    }

    private static Integer getTerminatePriority(final List<HelmPackage> vnfInstanceHelmPackages, HelmChart helmChartToChange) {
        return vnfInstanceHelmPackages.stream().filter(helmPackage -> helmPackage.getId().equals(helmChartToChange.getHelmChartArtifactKey()))
                .map(HelmPackage::getPriority)
                .findFirst().orElse(null);
    }

    @NotNull
    private static List<HelmChart> getReverseSortedHelmCharts(final Map<HelmChart, Integer> lastOperationHelmChartPriorityMap) {
        return lastOperationHelmChartPriorityMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey).collect(toList());
    }

    @NotNull
    private static Map<HelmChart, Integer> getLastOperationHelmChartPriorityMap(final VnfInstance vnfInstance,
                                                                                final List<HelmChart> helmCharts) {
        List<LifecycleOperation> allOperations = getAllOperations(vnfInstance);
        LifecycleOperationType lastCompletedVnfInstanceOperation = getLastCompletedLifecycleOperationType(allOperations);
        Map<HelmChart, Integer> getLastOperationHelmChartPriorityMap = new HashMap<>();
        for (HelmChart helmChart : helmCharts) {
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart
                    .getOperationChartsPriority();
            if (operationChartsPriority.isEmpty()) {
                throw new InternalRuntimeException("The operation charts priority map should not be empty.");
            }
            if (getLastOperationHelmChartPriorityMap.put(helmChart, operationChartsPriority
                    .get(lastCompletedVnfInstanceOperation)) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return getLastOperationHelmChartPriorityMap;
    }

    @NotNull
    private static LifecycleOperationType getLastCompletedLifecycleOperationType(final List<LifecycleOperation> allOperations) {
        return allOperations.stream()
                .filter(lcm -> LifecycleOperationState.COMPLETED.equals(lcm.getOperationState()))
                .filter(lcm -> LifecycleOperationType.INSTANTIATE.equals(lcm.getLifecycleOperationType()) ||
                        LifecycleOperationType.CHANGE_VNFPKG.equals(lcm.getLifecycleOperationType()))
                .max(Comparator.comparing(LifecycleOperation::getStateEnteredTime))
                .map(LifecycleOperation::getLifecycleOperationType)
                .orElse(LifecycleOperationType.INSTANTIATE);
    }

    @NotNull
    private static List<LifecycleOperation> getAllOperations(final VnfInstance vnfInstance) {
        return vnfInstance.getAllOperations() == null ? new ArrayList<>() : vnfInstance.getAllOperations();
    }
}