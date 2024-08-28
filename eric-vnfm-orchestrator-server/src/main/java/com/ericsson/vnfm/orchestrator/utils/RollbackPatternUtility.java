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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.ericsson.am.shared.vnfd.VnfPackageChangePatternCommand;
import org.apache.commons.lang3.tuple.MutablePair;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.fasterxml.jackson.core.type.TypeReference;

public final class RollbackPatternUtility {

    private RollbackPatternUtility() {
    }

    /**
     * Gets the default downgrade pattern
     *
     * @param vnfInstance
     * @param tempInstance
     * @return
     */
    public static List<MutablePair<String, String>> getDefaultDowngradePattern(VnfInstance vnfInstance,
                                                                               VnfInstance tempInstance) {
        final List<String> sourceReleaseNames = vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .map(HelmChartBaseEntity::getReleaseName)
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        final List<String> targetReleaseNames = tempInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .map(HelmChartBaseEntity::getReleaseName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        final List<MutablePair<String, String>> allCharts = new ArrayList<>();
        sourceReleaseNames.forEach(name -> {
            if (targetReleaseNames.contains(name)) {
                allCharts.add(new MutablePair<>(name, VnfPackageChangePatternCommand.ROLLBACK.getCommand()));
            }
        });
        sourceReleaseNames.forEach(name -> {
            if (!targetReleaseNames.contains(name)) {
                allCharts.add(new MutablePair<>(name, VnfPackageChangePatternCommand.DELETE.getCommand()));
                allCharts.add(new MutablePair<>(name, VnfPackageChangePatternCommand.DELETE_PVC.getCommand()));
            }
        });
        targetReleaseNames.forEach(name -> {
            if (!sourceReleaseNames.contains(name)) {
                allCharts.add(new MutablePair(name, VnfPackageChangePatternCommand.INSTALL.getCommand()));
            }
        });

        return allCharts;
    }

    /**
     * Gets the current pattern
     *
     * @param operation
     * @return
     */
    public static List<LinkedHashMap<String, String>> getPattern(LifecycleOperation operation) {
        if (operation.getFailurePattern() != null) {
            return Utility
                    .parseJsonToGenericType(operation.getFailurePattern(), new TypeReference<>() {
                    });
        } else if (operation.getRollbackPattern() != null) {
            return Utility
                    .parseJsonToGenericType(operation.getRollbackPattern(), new TypeReference<>() {
                    });
        }
        return Collections.emptyList();
    }
}
