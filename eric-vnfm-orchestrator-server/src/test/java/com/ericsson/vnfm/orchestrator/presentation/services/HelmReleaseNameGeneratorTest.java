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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;

public class HelmReleaseNameGeneratorTest {

    @Test
    public void testGenerateForReturnSameChartNameForCrdHelmPackage() {
        HelmPackage testHelmPackage = new HelmPackage();
        testHelmPackage.setChartType(HelmChartType.CRD);
        testHelmPackage.setChartName("test-chart-name");

        PackageResponse testPackageResponse = new PackageResponse();
        List<HelmPackage> testHelmPackages = List.of(testHelmPackage);
        testPackageResponse.setHelmPackageUrls(testHelmPackages);


        HelmReleaseNameGenerator nameGenerator =
                HelmReleaseNameGenerator.forInstantiate(null, testPackageResponse, false);
        String result = nameGenerator.generateFor(testHelmPackage);

        assertEquals(testHelmPackage.getChartName(), result);
    }

    @Test
    public void testGenerateForReturnNullForCnfHelmPackage() {
        HelmPackage testHelmPackage = new HelmPackage();
        testHelmPackage.setChartType(HelmChartType.CRD);

        PackageResponse testPackageResponse = new PackageResponse();
        List<HelmPackage> testHelmPackages = List.of(testHelmPackage);
        testPackageResponse.setHelmPackageUrls(testHelmPackages);

        HelmChart testHelmChart = new HelmChart();
        testHelmChart.setHelmChartType(HelmChartType.CNF);
        testHelmChart.setReleaseName("test-chart-name");
        List<HelmChart> testPreviousCharts = List.of(testHelmChart);

        VnfInstance testVnfInstance = new VnfInstance();
        testVnfInstance.setVnfInstanceName("test-chart-name");

        HelmReleaseNameGenerator nameGenerator =
                HelmReleaseNameGenerator.forUpgrade(testVnfInstance, testPackageResponse, testPreviousCharts);
        String result = nameGenerator.generateFor(testHelmPackage);

        assertEquals(null, result);
    }

    @Test
    public void testGenerateForReturnVnfInstanceName() {
        HelmPackage testHelmPackage = new HelmPackage();
        testHelmPackage.setChartType(HelmChartType.CNF);
        testHelmPackage.setPriority(2);

        PackageResponse testPackageResponse = new PackageResponse();
        List<HelmPackage> testHelmPackages = List.of(testHelmPackage);
        testPackageResponse.setHelmPackageUrls(testHelmPackages);

        HelmChart testHelmChart = new HelmChart();
        testHelmChart.setHelmChartType(HelmChartType.CNF);
        testHelmChart.setReleaseName("test-chart-name");
        List<HelmChart> testPreviousCharts = List.of(testHelmChart);

        VnfInstance testVnfInstance = new VnfInstance();
        testVnfInstance.setVnfInstanceName("test-chart-name");

        HelmReleaseNameGenerator nameGenerator =
                HelmReleaseNameGenerator.forUpgrade(testVnfInstance, testPreviousCharts);
        String result = nameGenerator.generateFor(testHelmPackage);

        assertEquals(testVnfInstance.getVnfInstanceName() + "-2", result);
    }

    @Test
    public void testGenerateForReturnVnfInstanceNamePlusSuffix() {
        HelmPackage testHelmPackage = new HelmPackage();
        testHelmPackage.setChartType(HelmChartType.CNF);
        testHelmPackage.setPriority(2);

        PackageResponse testPackageResponse = new PackageResponse();
        List<HelmPackage> testHelmPackages = List.of(testHelmPackage);
        testPackageResponse.setHelmPackageUrls(testHelmPackages);

        HelmChart testHelmChart = new HelmChart();
        testHelmChart.setHelmChartType(HelmChartType.CNF);
        testHelmChart.setReleaseName("test-chart-name");
        List<HelmChart> testPreviousCharts = List.of(testHelmChart);

        VnfInstance testVnfInstance = new VnfInstance();
        testVnfInstance.setVnfInstanceName("test-chart-name");

        HelmReleaseNameGenerator nameGenerator =
                HelmReleaseNameGenerator.forUpgrade(testVnfInstance, testPreviousCharts);
        String result = nameGenerator.generateFor(testHelmPackage);

        assertEquals(testVnfInstance.getVnfInstanceName() + "-2", result);
    }

    @Test
    public void testCalculateLastSuffixReleaseNameWithSameBaseNameChart() {
        HelmChart testHelmChart1 = new HelmChart();
        testHelmChart1.setReleaseName("vnf-instance-base-");
        HelmChart testHelmChart2 = new HelmChart();
        testHelmChart2.setReleaseName("vnf-instance-base-plus-some-text-2");

        TerminatedHelmChart terminatedHelmChart1 = new TerminatedHelmChart();
        terminatedHelmChart1.setReleaseName("vnf-instance-base-plus-some-text-3");
        TerminatedHelmChart terminatedHelmChart2 = new TerminatedHelmChart();
        terminatedHelmChart2.setReleaseName("terminated-chart-name-4");

        VnfInstance testVnfInstance = new VnfInstance();
        testVnfInstance.setVnfInstanceName("vnf-instance-base");
        testVnfInstance.setHelmCharts(List.of(testHelmChart1, testHelmChart2));
        testVnfInstance.setTerminatedHelmCharts(List.of(terminatedHelmChart1, terminatedHelmChart2));

        List<HelmChart> testPreviousCharts = List.of(testHelmChart1);

        HelmReleaseNameGenerator nameGenerator =
                HelmReleaseNameGenerator.forUpgrade(testVnfInstance, testPreviousCharts);
        String result = nameGenerator.generateNextReleaseName();

        assertEquals(testVnfInstance.getVnfInstanceName() + "-4", result);
    }


}