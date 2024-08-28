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
package com.ericsson.vnfm.orchestrator.e2e.granting.testData;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;

public final class HelmChartsTestData {

    private HelmChartsTestData() {

    }

    public static Map<String, HelmChart> createExpectedUpgradedHelmChartsForDefaultDeployableModules() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app1")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("helm_package1")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app1-1.1.0.tgz")
                .priority(4)
                .releaseName("dm-granting-upgrade-1")
                .state("COMPLETED")
                .build();
        HelmChart secondCnfChart = HelmChart.builder()
                .helmChartName("spider-app2")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("helm_package2")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app2-2.1.0.tgz")
                .priority(5)
                .releaseName("dm-granting-upgrade-2")
                .state("COMPLETED")
                .build();
        HelmChart firstCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-sip-tls-crd")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("crd_package1")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.1.0tgz")
                .priority(1)
                .releaseName("eric-sec-sip-tls-crd")
                .state("COMPLETED")
                .build();
        HelmChart secondCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-certm-crd")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("crd_package2")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-certm-crd-2.1.0tgz")
                .priority(2)
                .releaseName("eric-sec-certm-crd")
                .state("COMPLETED")
                .build();
        HelmChart thirdCrdChart = HelmChart.builder()
                .helmChartName("scale-crd")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("crd_package3")
                .helmChartUrl("https://localhost/onboarded/charts/scale-crd-3.1.0.tgz")
                .priority(3)
                .releaseName("scale-crd")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart, secondCnfChart.getHelmChartName(), secondCnfChart,
                      firstCrdChart.getHelmChartName(), firstCrdChart, secondCrdChart.getHelmChartName(), secondCrdChart,
                      thirdCrdChart.getHelmChartName(), thirdCrdChart);
    }

    public static Map<String, HelmChart> createExpectedUpgradedHelmChartsForDefaultDeployableModulesAndMandatoryCharts() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app1")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("helm_package4")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app1-1.1.0.tgz")
                .priority(4)
                .releaseName("dm-granting-upgrade-1")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart secondCnfChart = HelmChart.builder()
                .helmChartName("spider-app2")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("helm_package5")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app2-2.1.0.tgz")
                .priority(5)
                .releaseName("dm-granting-upgrade-2")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart thirdCnfChart = HelmChart.builder()
                .helmChartName("spider-app4")
                .helmChartVersion("4.1.0")
                .helmChartArtifactKey("helm_package7")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app4-4.1.0.tgz")
                .priority(7)
                .releaseName("dm-granting-upgrade-4")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart fourthCnfChart = HelmChart.builder()
                .helmChartName("spider-app5")
                .helmChartVersion("5.1.0")
                .helmChartArtifactKey("helm_package8_mandatory")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app5-5.1.0.tgz")
                .priority(8)
                .releaseName("dm-granting-upgrade-5")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart firstCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-sip-tls-crd")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("crd_package4")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.1.0tgz")
                .priority(1)
                .releaseName("eric-sec-sip-tls-crd")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart secondCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-certm-crd")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("crd_package5")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-certm-crd-2.1.0tgz")
                .priority(2)
                .releaseName("eric-sec-certm-crd")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart thirdCrdChart = HelmChart.builder()
                .helmChartName("scale-crd")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("crd_package6")
                .helmChartUrl("https://localhost/onboarded/charts/scale-crd-3.1.0.tgz")
                .priority(3)
                .releaseName("scale-crd")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart, secondCnfChart.getHelmChartName(), secondCnfChart,
                      thirdCnfChart.getHelmChartName(), thirdCnfChart, fourthCnfChart.getHelmChartName(), fourthCnfChart,
                      firstCrdChart.getHelmChartName(), firstCrdChart, secondCrdChart.getHelmChartName(), secondCrdChart,
                      thirdCrdChart.getHelmChartName(), thirdCrdChart);
    }

    public static Map<String, HelmChart> createExpectedUpgradedHelmChartsForRequestDeployableModules() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app1")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("helm_package4")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app1-1.1.0.tgz")
                .priority(4)
                .releaseName("dm-granting-upgrade-1")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();
        HelmChart secondCnfChart = HelmChart.builder()
                .helmChartName("spider-app2")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("helm_package5")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app2-2.1.0.tgz")
                .priority(5)
                .releaseName("dm-granting-upgrade-2")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart thirdCnfChart = HelmChart.builder()
                .helmChartName("spider-app3")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("helm_package6")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app3-3.1.0.tgz")
                .priority(6)
                .releaseName("dm-granting-upgrade-3")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart firstCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-sip-tls-crd")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("crd_package4")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.1.0tgz")
                .priority(1)
                .releaseName("eric-sec-sip-tls-crd")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();
        HelmChart secondCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-certm-crd")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("crd_package5")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-certm-crd-2.1.0tgz")
                .priority(2)
                .releaseName("eric-sec-certm-crd")
                .state("COMPLETED")
                .build();
        HelmChart thirdCrdChart = HelmChart.builder()
                .helmChartName("scale-crd")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("crd_package6"
                                              + "")
                .helmChartUrl("https://localhost/onboarded/charts/scale-crd-3.1.0.tgz")
                .priority(3)
                .releaseName("scale-crd")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart, secondCnfChart.getHelmChartName(), secondCnfChart,
                      thirdCnfChart.getHelmChartName(), thirdCnfChart, firstCrdChart.getHelmChartName(), firstCrdChart,
                      secondCrdChart.getHelmChartName(), secondCrdChart, thirdCrdChart.getHelmChartName(), thirdCrdChart);
    }

    public static Map<String, HelmChart> createExpectedUpgradedHelmChartsForPersistDMConfigTrue() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app1")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("helm_package1")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app1-1.1.0.tgz")
                .priority(4)
                .releaseName("dm-granting-upgrade-1")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart secondCnfChart = HelmChart.builder()
                .helmChartName("spider-app2")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("helm_package2")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app2-2.1.0.tgz")
                .priority(5)
                .releaseName("dm-granting-upgrade-2")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();
        HelmChart firstCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-sip-tls-crd")
                .helmChartVersion("1.1.0")
                .helmChartArtifactKey("crd_package1")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.1.0tgz")
                .priority(1)
                .releaseName("eric-sec-sip-tls-crd")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart secondCrdChart = HelmChart.builder()
                .helmChartName("eric-sec-certm-crd")
                .helmChartVersion("2.1.0")
                .helmChartArtifactKey("crd_package2")
                .helmChartUrl("https://localhost/onboarded/charts/eric-sec-certm-crd-2.1.0tgz")
                .priority(2)
                .releaseName("eric-sec-certm-crd")
                .state("COMPLETED")
                .isChartEnabled(true)
                .build();
        HelmChart thirdCrdChart = HelmChart.builder()
                .helmChartName("scale-crd")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("crd_package3")
                .helmChartUrl("https://localhost/onboarded/charts/scale-crd-3.1.0.tgz")
                .priority(3)
                .releaseName("scale-crd")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart, secondCnfChart.getHelmChartName(), secondCnfChart,
                      firstCrdChart.getHelmChartName(), firstCrdChart, secondCrdChart.getHelmChartName(), secondCrdChart,
                      thirdCrdChart.getHelmChartName(), thirdCrdChart);
    }

    public static Map<String, HelmChart> createExpectedTerminatedHelmChartsPersistDMConfigTrue() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app3")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("helm_package3")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app3-3.1.0.tgz")
                .priority(6)
                .releaseName("dm-granting-upgrade-3")
                .state("COMPLETED")
                .isChartEnabled(false)
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart);
    }

    public static Map<String, HelmChart> createExpectedTerminatedHelmChartsWithMandatoryCharts() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app3")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("helm_package6")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app3-3.1.0.tgz")
                .priority(6)
                .releaseName("dm-granting-upgrade-3")
                .state("COMPLETED")
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart);
    }

    public static Map<String, HelmChart> createExpectedTerminatedHelmCharts() {
        HelmChart firstCnfChart = HelmChart.builder()
                .helmChartName("spider-app3")
                .helmChartVersion("3.1.0")
                .helmChartArtifactKey("helm_package3")
                .helmChartUrl("https://localhost/onboarded/charts/spider-app3-3.1.0.tgz")
                .priority(6)
                .releaseName("dm-granting-upgrade-3")
                .state("COMPLETED")
                .build();

        return Map.of(firstCnfChart.getHelmChartName(), firstCnfChart);
    }
}
