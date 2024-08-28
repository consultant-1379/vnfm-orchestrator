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

import static java.util.stream.Collectors.toList;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.HELM_RELEASE_NAME_SEPARATOR;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HelmReleaseNameGenerator {

    private final VnfInstance vnfInstance;
    private final int crdChartCount;
    private final boolean shouldOmitSuffixInFirstRelease;
    private final boolean skipCNFReleaseNameGeneration;

    private HelmReleaseNameGenerator(final VnfInstance vnfInstance, final PackageResponse packageInfo, boolean shouldOmitSuffixInFirstRelease,
                                     boolean skipCNFReleaseNameGeneration) {
        this.vnfInstance = vnfInstance;
        this.crdChartCount = calculateCrdChartCount(packageInfo);
        this.shouldOmitSuffixInFirstRelease = shouldOmitSuffixInFirstRelease;
        this.skipCNFReleaseNameGeneration = skipCNFReleaseNameGeneration;
    }

    private HelmReleaseNameGenerator(final VnfInstance vnfInstance, boolean shouldOmitSuffixInFirstRelease) {
        this.vnfInstance = vnfInstance;
        this.crdChartCount = 0;
        this.shouldOmitSuffixInFirstRelease = shouldOmitSuffixInFirstRelease;
        this.skipCNFReleaseNameGeneration = false;
    }

    static HelmReleaseNameGenerator forInstantiate(final VnfInstance vnfInstance,
                                                   final PackageResponse packageInfo,
                                                   boolean suffixFirstCnfReleaseSchema) {

        return new HelmReleaseNameGenerator(vnfInstance, packageInfo, !suffixFirstCnfReleaseSchema, false);
    }

    static HelmReleaseNameGenerator forUpgrade(final VnfInstance vnfInstance,
                                               final PackageResponse packageInfo,
                                               final List<HelmChart> previousCharts) {

        return new HelmReleaseNameGenerator(vnfInstance, packageInfo, hasChartWithReleaseNameWithoutSuffix(previousCharts, vnfInstance), true);
    }

    public static HelmReleaseNameGenerator forUpgrade(final VnfInstance vnfInstance,
                                                      final List<HelmChart> previousCharts) {
        return new HelmReleaseNameGenerator(vnfInstance, hasChartWithReleaseNameWithoutSuffix(previousCharts, vnfInstance));
    }

    public String generateFor(final HelmPackage helmPackage) {
        if (helmPackage.getChartType() == HelmChartType.CRD) {
            return helmPackage.getChartName();
        }

        if (skipCNFReleaseNameGeneration) {
            return null;
        }

        final var suffix = helmPackage.getPriority() - crdChartCount;

        /* Following logic is required to support these cases:
           1. Upgrading existing instances based on single-chart package to multi-chart package and subsequent upgrades.
              Single-chart package based instances previously had helm release name without suffix that must be preserved when upgraded to
              multi-chart package.
              Example:
                - single-chart package based instance has the following mapping:
                  helm_package_1 -> release_name
                - when it's being upgraded to multi-chart package the following mapping is expected:
                  helm_package_1 -> release_name, helm_package_2 -> release_name_2
                - when the same instance is being upgraded to another multi-chart package the following mapping is expected:
                  helm_package_1 -> release_name, helm_package_2 -> release_name_2, helm_package_3 -> release_name_3
           2. Instantiating and upgrading instances (single-chart or multi-chart) if naming policy for the first release is "do not add suffix".
        */
        if (shouldOmitSuffixInFirstRelease && suffix == 1) {
            return vnfInstance.getVnfInstanceName();
        }
        /* end of special logic */

        return StringUtils.joinWith(HELM_RELEASE_NAME_SEPARATOR, vnfInstance.getVnfInstanceName(), suffix);
    }

    public String generateNextReleaseName() {
        var suffix = calculateLastSuffixReleaseName() + 1;

        if (shouldOmitSuffixInFirstRelease && suffix == 1) {
            suffix++;
        }

        return StringUtils.joinWith(HELM_RELEASE_NAME_SEPARATOR, vnfInstance.getVnfInstanceName(), suffix);
    }

    private int calculateLastSuffixReleaseName() {
        List<String> helmReleases = vnfInstance.getHelmCharts().stream()
                .map(HelmChartBaseEntity::getReleaseName)
                .filter(Objects::nonNull)
                .collect(toList());
        vnfInstance.getTerminatedHelmCharts().stream()
                .map(HelmChartBaseEntity::getReleaseName)
                .filter(Objects::nonNull)
                .forEach(helmReleases::add);
        String releaseNameFilteredPrefix = vnfInstance.getVnfInstanceName() + HELM_RELEASE_NAME_SEPARATOR;

        return helmReleases.stream()
                .filter(name -> name.startsWith(releaseNameFilteredPrefix))
                .map(HelmReleaseNameGenerator::trimChartNumber)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    private static Integer trimChartNumber(String name) {
        String[] chartNameParts = name.split(HELM_RELEASE_NAME_SEPARATOR);
        try {
            return Integer.parseInt(chartNameParts[chartNameParts.length - 1]);
        } catch (NumberFormatException e) {
            LOGGER.info(String.format("Index of the chart %s hasn't been parsed due to: %s", name, e));
            return 0;
        }
    }

    private static boolean hasChartWithReleaseNameWithoutSuffix(final List<HelmChart> previousCharts, final VnfInstance vnfInstance) {
        return previousCharts.stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .anyMatch(chart -> Objects.equals(chart.getReleaseName(), vnfInstance.getVnfInstanceName()));
    }

    private static int calculateCrdChartCount(final PackageResponse packageInfo) {
        return (int) packageInfo.getHelmPackageUrls().stream()
                .filter(helmPackage -> helmPackage.getChartType() == HelmChartType.CRD)
                .count();
    }
}
