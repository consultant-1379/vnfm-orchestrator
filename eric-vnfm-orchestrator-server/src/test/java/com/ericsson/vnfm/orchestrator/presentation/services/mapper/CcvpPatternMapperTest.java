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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;

public class CcvpPatternMapperTest {

    @Test
    public void testMapHelmChartToReleaseName() {
        List<MutablePair<String, String>> helmChartCommandList = buildHelmChartCommandList();
        List<HelmChart> helmCharts = buildHelmChartsList();
        List<MutablePair<String, String>> result = CcvpPatternMapper.mapHelmChartArtifactKeyToReleaseName(helmChartCommandList, helmCharts);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getLeft()).isEqualTo("release-name-1");
        assertThat(result.get(0).getRight()).isEqualTo("rollback");
        assertThat(result.get(1).getLeft()).isEqualTo("release-name-2");
        assertThat(result.get(1).getRight()).isEqualTo("rollback");
    }

    @Test
    public void testMapHelmChartToReleaseNameWithTargetChart() {
        List<MutablePair<String, String>> helmChartCommandList = buildHelmChartCommandListWithThreeCharts();
        List<HelmChart> helmCharts = buildHelmChartsList();
        List<HelmChart> targetHelmCharts = buildTargetHelmChartsList();
        List<MutablePair<String, String>> result = CcvpPatternMapper.mapHelmChartArtifactKeyToReleaseName(helmChartCommandList,
                                                                                                              helmCharts,
                                                                                                              targetHelmCharts);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getLeft()).isEqualTo("release-name-1");
        assertThat(result.get(0).getRight()).isEqualTo("rollback");
        assertThat(result.get(1).getLeft()).isEqualTo("release-name-2");
        assertThat(result.get(1).getRight()).isEqualTo("rollback");
        assertThat(result.get(2).getLeft()).isEqualTo("release-name-3");
        assertThat(result.get(2).getRight()).isEqualTo("rollback");
    }

    public List<MutablePair<String, String>> buildHelmChartCommandList() {
        List<MutablePair<String, String>> helmChartCommandList = new ArrayList<>();
        MutablePair<String, String> firstPar = new MutablePair<>();
        firstPar.setLeft("helm_package1");
        firstPar.setRight("rollback");
        MutablePair<String, String> secondPair = new MutablePair<>();
        secondPair.setLeft("helm_package2");
        secondPair.setRight("rollback");
        helmChartCommandList.add(firstPar);
        helmChartCommandList.add(secondPair);
        return helmChartCommandList;
    }

    public List<MutablePair<String, String>> buildHelmChartCommandListWithThreeCharts() {
        List<MutablePair<String, String>> helmChartCommandList = new ArrayList<>();
        MutablePair<String, String> firstPar = new MutablePair<>();
        firstPar.setLeft("helm_package1");
        firstPar.setRight("rollback");
        MutablePair<String, String> secondPair = new MutablePair<>();
        secondPair.setLeft("helm_package2");
        secondPair.setRight("rollback");
        MutablePair<String, String> thirdPair = new MutablePair<>();
        thirdPair.setLeft("helm_package3");
        thirdPair.setRight("rollback");

        helmChartCommandList.add(firstPar);
        helmChartCommandList.add(secondPair);
        helmChartCommandList.add(thirdPair);
        return helmChartCommandList;
    }

    public List<HelmChart> buildHelmChartsList() {
        List<HelmChart> helmCharts = new ArrayList<>();
        HelmChart firstHelmChart = new HelmChart();
        firstHelmChart.setReleaseName("release-name-1");
        firstHelmChart.setHelmChartArtifactKey("helm_package1");

        HelmChart secondHelmChart = new HelmChart();
        secondHelmChart.setReleaseName("release-name-2");
        secondHelmChart.setHelmChartArtifactKey("helm_package2");
        helmCharts.add(firstHelmChart);
        helmCharts.add(secondHelmChart);
        return helmCharts;
    }

    public List<HelmChart> buildTargetHelmChartsList() {

        HelmChart firstHelmChart = new HelmChart();
        firstHelmChart.setReleaseName("release-name-1");
        firstHelmChart.setHelmChartArtifactKey("helm_package1");

        HelmChart thirdHelmChart = new HelmChart();
        thirdHelmChart.setReleaseName("release-name-3");
        thirdHelmChart.setHelmChartArtifactKey("helm_package3");

        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(firstHelmChart);
        helmCharts.add(thirdHelmChart);
        return helmCharts;
    }
}