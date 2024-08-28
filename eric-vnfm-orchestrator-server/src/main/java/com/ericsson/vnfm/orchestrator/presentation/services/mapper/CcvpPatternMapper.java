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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;

public final class CcvpPatternMapper {

    private CcvpPatternMapper() {
    }

    public static List<MutablePair<String, String>> mapHelmChartArtifactKeyToReleaseName(
            List<MutablePair<String, String>> helmChartCommandList,
            List<HelmChart> sourceInstanceHelmCharts,
            List<HelmChart> targetInstanceHelmCharts) {
        List<MutablePair<String, String>> releaseNameCommandList = new ArrayList<>(helmChartCommandList);
        for (MutablePair<String, String> pair : releaseNameCommandList) {
            for (HelmChart helmChart : sourceInstanceHelmCharts) {
                if (helmChart.getHelmChartArtifactKey().equals(pair.getLeft())) {
                    pair.setLeft(helmChart.getReleaseName());
                    break;
                }
            }
            for (HelmChart helmChart : targetInstanceHelmCharts) {
                if (helmChart.getHelmChartArtifactKey().equals(pair.getLeft())) {
                    pair.setLeft(helmChart.getReleaseName());
                    break;
                }
            }
        }
        return releaseNameCommandList;
    }

    public static List<MutablePair<String, String>> mapHelmChartArtifactKeyToReleaseName(
            List<MutablePair<String, String>> helmChartCommandList,
            List<HelmChart> helmCharts) {
        List<MutablePair<String, String>> releaseNameCommandList = new ArrayList<>(helmChartCommandList);
        for (MutablePair<String, String> pair : releaseNameCommandList) {
            for (HelmChart helmChart : helmCharts) {
                if (helmChart.getHelmChartArtifactKey().equals(pair.getLeft())) {
                    pair.setLeft(helmChart.getReleaseName());
                    break;
                }
            }
        }
        return releaseNameCommandList;
    }
}
