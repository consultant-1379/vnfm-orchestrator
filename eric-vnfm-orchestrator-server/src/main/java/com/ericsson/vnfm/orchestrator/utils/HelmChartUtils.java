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
package com.ericsson.vnfm.orchestrator.utils;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEFAULT_HIGHEST_PRIORITY_LEVEL;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmReleaseNameGenerator;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class HelmChartUtils {

    public static HelmChart toHelmChart(final HelmPackage helmPackage,
                                        final VnfInstance vnfInstance,
                                        final HelmReleaseNameGenerator helmReleaseNameGenerator) {

        final var chart = new HelmChart();

        chart.setHelmChartUrl(helmPackage.getChartUrl());
        chart.setPriority(helmPackage.getPriority());
        chart.setReleaseName(helmReleaseNameGenerator.generateFor(helmPackage));
        chart.setHelmChartName(helmPackage.getChartName());
        chart.setHelmChartVersion(helmPackage.getChartVersion());
        chart.setHelmChartType(helmPackage.getChartType());
        chart.setHelmChartArtifactKey(helmPackage.getChartArtifactKey());
        chart.setVnfInstance(vnfInstance);

        return chart;
    }

    public static void setPriorityBeforeOperation(final LifecycleOperationType lifecycleOperationType, final VnfInstance vnfInstance) {
        for (HelmChart helmChart : vnfInstance.getHelmCharts()) {
            Map<LifecycleOperationType, Integer> chartsPriority = helmChart.getOperationChartsPriority();
            if (chartsPriority.containsKey(lifecycleOperationType)) {
                helmChart.setPriority(chartsPriority.get(lifecycleOperationType));
            }
        }
    }
    public static TerminatedHelmChart toTerminateHelmChart(final HelmChart helmChart,
                                                           final LifecycleOperation lifecycleOperation) {

        final TerminatedHelmChart terminateHelmChart = new TerminatedHelmChart();

        BeanUtils.copyProperties(helmChart, terminateHelmChart, "id", "state");
        terminateHelmChart.setOperationOccurrenceId(lifecycleOperation.getOperationOccurrenceId());

        return terminateHelmChart;
    }

    public static HelmChart toHelmChart(final TerminatedHelmChart terminatedHelmChart) {
        final HelmChart helmChart = new HelmChart();

        BeanUtils.copyProperties(terminatedHelmChart, helmChart);

        return helmChart;
    }

    public static HelmChart getHelmChartByPriority(final VnfInstance vnfInstance, final int priority) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        return helmCharts.
                stream().filter(chart -> chart.getPriority() == priority).findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Helm Chart With priority %s not found",
                                                                       priority)));
    }

    public static String getFullNameOfChartWithPriority(final VnfInstance vnfInstance, final int priority) {
        Optional<String> chartInfoOptional = vnfInstance.getHelmCharts()
                .stream().filter(chart -> chart.getPriority() == priority).findFirst()
                .map(chart -> toChartFullName(chart.getHelmChartName(), chart.getHelmChartVersion()));
        if (chartInfoOptional.isEmpty()) {
            chartInfoOptional = vnfInstance.getTerminatedHelmCharts().stream().filter(chart -> chart.getPriority() == priority).findFirst()
                    .map(chart -> toChartFullName(chart.getHelmChartName(), chart.getHelmChartVersion()));
        }
        return chartInfoOptional
                .orElseThrow(() -> new NotFoundException(String.format("Helm Chart With priority %s not found", priority)));
    }

    public static HelmChart getCnfChartWithHighestPriority(final VnfInstance vnfInstance) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        return helmCharts.stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .min(Comparator.comparingInt(HelmChart::getPriority))
                .orElseThrow(() -> new NotFoundException(String.format("No CNF Helm Chart has been found in VNF Instance with Id : %s.",
                                                                       vnfInstance.getVnfInstanceId())));
    }

    public static HelmChart getEnabledNotCrdAndNotProcessedCnfChartWithHighestPriority(final VnfInstance vnfInstance) {
        List<HelmChart> helmCNFCharts = getCnfHelmCharts(vnfInstance);
        return getNotProcessedHelmChartWithHighestPriority(vnfInstance, helmCNFCharts);
    }

    public static String getFullNameOfChartWithHighestPriority(final VnfInstance vnfInstance) {
        Optional<String> chartInfoOptional = vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .min(Comparator.comparingInt(HelmChart::getPriority))
                .map(chart -> toChartFullName(chart.getHelmChartName(), chart.getHelmChartVersion()));
        if (chartInfoOptional.isEmpty()) {
            chartInfoOptional = vnfInstance.getTerminatedHelmCharts().stream()
                    .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                    .min(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                    .map(chart -> toChartFullName(chart.getHelmChartName(), chart.getHelmChartVersion()));
        }
        return chartInfoOptional
                .orElseThrow(() -> new NotFoundException(String.format("No CNF Helm Chart has been found in VNF Instance with Id : %s.",
                                                                       vnfInstance.getVnfInstanceId())));
    }

    private static String toChartFullName(String helmChartName, String helmChartVersion) {
        return String.format("%s-%s", helmChartName, helmChartVersion);
    }

    public static HelmChart getHelmChartWithHighestPriorityByDeployableModulesSupported(VnfInstance vnfInstance) {
        return vnfInstance.isDeployableModulesSupported()
                ? getNotProcessedHelmChartWithHighestPriority(vnfInstance, vnfInstance.getHelmCharts())
                : getHelmChartByPriority(vnfInstance, DEFAULT_HIGHEST_PRIORITY_LEVEL);
    }

    private static HelmChart getNotProcessedHelmChartWithHighestPriority(final VnfInstance vnfInstance, final List<HelmChart> helmCharts) {
        return helmCharts.stream()
                .filter(helmChart -> helmChart.getState() == null)
                .min(Comparator.comparingInt(HelmChart::getPriority))
                .orElseThrow(() -> new NotFoundException(String.format("No unprocessed Helm Chart has been found in VNF Instance with Id : %s.",
                                                                       vnfInstance.getVnfInstanceId())));
    }

    public static List<HelmChart> getCnfHelmCharts(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .collect(Collectors.toList());
    }

    public static HelmChart getNotProcessedHelmChartWithHighestPriority(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.getState() == null)
                .min(Comparator.comparingInt(HelmChart::getPriority))
                .orElseThrow(() -> new NotFoundException(String.format("No enabled not processed Helm Chart has been found in VNF Instance " +
                                                                               "with Id: %s.", vnfInstance.getVnfInstanceId())));
    }

    public static Optional<TerminatedHelmChart> getNextHelmChartToTerminate(final VnfInstance vnfInstance,
                                                                            final LifecycleOperation lifecycleOperation) {
        return Optional.ofNullable(vnfInstance.getTerminatedHelmCharts()).orElseGet(ArrayList::new)
                .stream()
                .sorted(Comparator.comparing(HelmChartBaseEntity::getPriority))
                .filter(chart -> chart.getState() == null && chart.getDeletePvcState() == null
                        && chart.getOperationOccurrenceId().equals(lifecycleOperation.getOperationOccurrenceId()))
                .findFirst();
    }

    public static boolean areAllCnfChartsDisabled(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .noneMatch(HelmChartBaseEntity::isChartEnabled);
    }

    public static Set<String> getAllDisabledArtifactsKeys(VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(chart -> !chart.isChartEnabled())
                .map(HelmChart::getHelmChartArtifactKey)
                .collect(Collectors.toSet());
    }

    public static void updateHelmChartsDeletePvcState(VnfInstance vnfInstance, LifecycleOperation operation, String releaseName, String state) {
        if (LifecycleOperationType.CHANGE_VNFPKG.equals(operation.getLifecycleOperationType())) {
            final VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            updateTerminatedChartDeletePvcState(tempInstance, releaseName, state);
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        } else {
            updateChartDeletePvcState(vnfInstance, releaseName, state);
        }
    }

    public static void updateChartDeletePvcState(final VnfInstance vnfInstance, final String releaseName, final String state) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        LOGGER.info("Updating state to {} for release {}", state, releaseName);
        helmCharts.stream().filter(obj -> StringUtils.equalsIgnoreCase(obj.getReleaseName(), releaseName)).findFirst()
                .ifPresent(o -> o.setDeletePvcState(state));
    }

    private static void updateTerminatedChartDeletePvcState(final VnfInstance vnfInstance, final String releaseName, final String state) {
        List<TerminatedHelmChart> terminatedHelmCharts = vnfInstance.getTerminatedHelmCharts();
        LOGGER.info("Updating state to {} for release {}", state, releaseName);
        terminatedHelmCharts.stream().filter(obj -> StringUtils.equalsIgnoreCase(obj.getReleaseName(), releaseName)).findFirst()
                .ifPresent(o -> o.setDeletePvcState(state));
    }

    public static Optional<HelmChart> getFirstProcessingCnfChart(final VnfInstance instance) {
        return instance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.PROCESSING.name()))
                .max(Comparator.comparingInt(HelmChart::getPriority));
    }

    public static Optional<HelmChart> getCnfChartWithName(final VnfInstance instance, final String releaseName, final String chartName) {
        return instance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartName() != null && chart.getReleaseName() != null)
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getReleaseName(), releaseName))
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getHelmChartName(), chartName))
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .max(Comparator.comparingInt(HelmChart::getPriority));
    }

    public static Optional<HelmChart> getCnfChartWithName(final VnfInstance instance, final String releaseName) {
        return instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getReleaseName(), releaseName))
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .max(Comparator.comparingInt(HelmChart::getPriority));
    }

    public static Optional<HelmChart> getFirstProcessingTerminatedCnfChart(final VnfInstance instance,
                                                                           final LifecycleOperation operation) {
        return CollectionUtils.emptyIfNull(instance.getTerminatedHelmCharts()).stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.PROCESSING.name()))
                .filter(chart -> chart.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                .findFirst()
                .map(HelmChartUtils::toHelmChart);
    }

    public static Optional<HelmChart> getTerminatedCnfChartWithName(final VnfInstance instance,
                                                                    final LifecycleOperation operation,
                                                                    final String releaseName,
                                                                    final String chartName) {
        return CollectionUtils.emptyIfNull(instance.getTerminatedHelmCharts()).stream()
                .filter(chart -> chart.getHelmChartName() != null && chart.getReleaseName() != null)
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getReleaseName(), releaseName))
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getHelmChartName(), chartName))
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> chart.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                .findFirst()
                .map(HelmChartUtils::toHelmChart);
    }

    public static Optional<HelmChart> getChartWithName(final VnfInstance instance, final String releaseName) {
        return instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getReleaseName(), releaseName))
                .findFirst();
    }

    public static Optional<HelmChart> getTerminatedChartWithName(final VnfInstance instance,
                                                                 final LifecycleOperation operation,
                                                                 final String releaseName) {

        return CollectionUtils.emptyIfNull(instance.getTerminatedHelmCharts()).stream()
                .filter(chart -> StringUtils.equalsIgnoreCase(chart.getReleaseName(), releaseName))
                .filter(chart -> chart.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                .findFirst()
                .map(HelmChartUtils::toHelmChart);
    }

    public static void setCompletedChartsStateToProcessing(final VnfInstance tempInstance,
                                                           final LifecycleOperation lifecycleOperation) {
        tempInstance.getHelmCharts()
                .stream()
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.COMPLETED.name()))
                .forEach(chart -> chart.setState(LifecycleOperationState.PROCESSING.name()));
        CollectionUtils.emptyIfNull(tempInstance.getTerminatedHelmCharts())
                .stream()
                .filter(chart -> lifecycleOperation.getOperationOccurrenceId().equals(chart.getOperationOccurrenceId()))
                .filter(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.COMPLETED.name()))
                .forEach(chart -> chart.setState(LifecycleOperationState.PROCESSING.name()));
    }

    public static HelmChart getFirstChartToDownsizeDuringCCVP(final VnfInstance vnfInstance) {
        final List<HelmChart> helmCnfCharts = getCnfHelmCharts(vnfInstance);

        return helmCnfCharts.stream()
                .filter(HelmChartBaseEntity::isChartEnabled)
                .max(Comparator.comparingInt(HelmChart::getPriority))
                .orElseThrow(() -> new NotFoundException(
                        String.format("No Helm Chart to downsize has been found in VNF Instance with Id : %s.", vnfInstance.getVnfInstanceId())));
    }

    public static Optional<HelmChart> getFirstChartToDownsizeDuringAutoRollback(final String failedReleaseName,
                                                                                final VnfInstance vnfInstance,
                                                                                final LifecycleOperation operation) {

        return getChartWithName(vnfInstance, failedReleaseName)
                .or(() -> getTerminatedChartWithName(vnfInstance, operation, failedReleaseName))
                .flatMap(failedChart -> getCnfHelmCharts(vnfInstance).stream()
                        .filter(HelmChartBaseEntity::isChartEnabled)
                        .filter(helmChart -> helmChart.getPriority() <= failedChart.getPriority())
                        .max(Comparator.comparingInt(HelmChart::getPriority)));
    }

    public static Optional<HelmChart> getNextChartToDownsizeAfter(final String currentReleaseName, final VnfInstance vnfInstance) {
        return getCnfChartWithName(vnfInstance, currentReleaseName)
                .flatMap(currentChart -> getCnfHelmCharts(vnfInstance).stream()
                        .filter(HelmChartBaseEntity::isChartEnabled)
                        .filter(helmChart -> helmChart.getPriority() < currentChart.getPriority())
                        .max(Comparator.comparingInt(HelmChart::getPriority)));
    }
}
