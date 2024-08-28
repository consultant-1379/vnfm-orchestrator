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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.utils.RollbackPatternUtility.getDefaultDowngradePattern;
import static com.ericsson.vnfm.orchestrator.utils.RollbackPatternUtility.getPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.ericsson.am.shared.vnfd.VnfPackageChangePatternCommand;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;

public class CcvpPatternTransformerImplTest {
    private final CcvpPatternTransformerImpl rollbackPatternTransformation = new CcvpPatternTransformerImpl();

    @Test
    public void testRenderChartCommandListAccordingToDeployableModules() {
        // given
        VnfInstance sourceVnfInstance = new VnfInstance();
        VnfInstance targetVnfInstance = new VnfInstance();
        final String chart1 = "chart-1";
        final String chart2 = "chart-2";

        final ArrayList<HelmChart> sourceHelmCharts = new ArrayList<>();
        sourceHelmCharts.add(helmChartPrototype(chart1, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart2, sourceVnfInstance, false));
        sourceVnfInstance.setHelmCharts(sourceHelmCharts);

        final ArrayList<HelmChart> targetHelmCharts = new ArrayList<>();
        targetHelmCharts.add(helmChartPrototype(chart1, targetVnfInstance, false));
        targetHelmCharts.add(helmChartPrototype(chart2, targetVnfInstance, true));
        targetVnfInstance.setHelmCharts(targetHelmCharts);

        List<MutablePair<String, String>> rollbackPattern = new ArrayList<>();
        rollbackPattern.add(new MutablePair<>(chart1, "delete")); // enabled -> disabled
        rollbackPattern.add(new MutablePair<>(chart1, "delete_pvc"));
        rollbackPattern.add(new MutablePair<>(chart1, "install"));
        rollbackPattern.add(new MutablePair<>(chart1, "delete"));
        rollbackPattern.add(new MutablePair<>(chart1, "upgrade"));
        rollbackPattern.add(new MutablePair<>(chart1, "rollback"));
        rollbackPattern.add(new MutablePair<>(chart1, "delete"));
        rollbackPattern.add(new MutablePair<>(chart1, "install"));

        rollbackPattern.add(new MutablePair<>(chart2, "upgrade")); // disabled -> enabled
        rollbackPattern.add(new MutablePair<>(chart2, "rollback"));
        rollbackPattern.add(new MutablePair<>(chart2, "delete"));
        rollbackPattern.add(new MutablePair<>(chart2, "install"));
        rollbackPattern.add(new MutablePair<>(chart2, "rollback"));

        // when
        final List<MutablePair<String, String>> resultRollbackPattern =
                rollbackPatternTransformation.transformChartCommandList(sourceVnfInstance, targetVnfInstance, rollbackPattern,
                        true);

        // then
        assertThat(resultRollbackPattern).containsExactlyInAnyOrder(
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart1, "delete_pvc"),
                new MutablePair<>(chart2, "upgrade"),
                new MutablePair<>(chart2, "install")
        );
    }

    @Test
    public void testDuplicateCommandIsRemovedFromFailurePattern() {
        VnfInstance sourceVnfInstance = new VnfInstance();
        VnfInstance targetVnfInstance = new VnfInstance();
        final String chart1 = "release-1";
        final String chart2 = "release-3";
        final String chart3 = "release-13";

        final List<HelmChart> sourceHelmCharts = new ArrayList<>();
        sourceHelmCharts.add(helmChartPrototype(chart1, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart2, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart3, sourceVnfInstance, true));
        sourceVnfInstance.setHelmCharts(sourceHelmCharts);

        final List<HelmChart> targetHelmCharts = new ArrayList<>();
        targetHelmCharts.add(helmChartPrototype(chart1, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart2, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart3, targetVnfInstance, true));
        targetVnfInstance.setHelmCharts(targetHelmCharts);

        List<MutablePair<String, String>> releaseNameCommandList = Arrays.asList(
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart1, "delete_pvc[]"),
                new MutablePair<>(chart2, "delete"),
                new MutablePair<>(chart3, "delete"),
                new MutablePair<>(chart1, "delete")
        );
        releaseNameCommandList =
                rollbackPatternTransformation.transformChartCommandList(sourceVnfInstance, targetVnfInstance, releaseNameCommandList,
                        true);
        assertThat(releaseNameCommandList.size()).isEqualTo(4);
        assertThat(releaseNameCommandList.get(0).right).isEqualTo("delete");
        assertThat(releaseNameCommandList.get(releaseNameCommandList.size() - 1).left).isEqualTo("release-13");
    }

    @Test
    public void testDeletePvcsAreNotRemovedIfOneContainsLabels() {
        VnfInstance sourceVnfInstance = new VnfInstance();
        VnfInstance targetVnfInstance = new VnfInstance();
        final String chart1 = "release-1";
        final String chart2 = "release-3";
        final String chart3 = "release-13";

        final List<HelmChart> sourceHelmCharts = new ArrayList<>();
        sourceHelmCharts.add(helmChartPrototype(chart1, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart2, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart3, sourceVnfInstance, true));
        sourceVnfInstance.setHelmCharts(sourceHelmCharts);

        final List<HelmChart> targetHelmCharts = new ArrayList<>();
        targetHelmCharts.add(helmChartPrototype(chart1, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart2, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart3, targetVnfInstance, true));
        targetVnfInstance.setHelmCharts(targetHelmCharts);

        List<MutablePair<String, String>> releaseNameCommandList = Arrays.asList(
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart1, "delete_pvc[app=x]"),
                new MutablePair<>(chart3, "delete"),
                new MutablePair<>(chart1, "delete_pvc[]"),
                new MutablePair<>(chart1, "delete")
        );
        releaseNameCommandList =
                rollbackPatternTransformation.transformChartCommandList(sourceVnfInstance, targetVnfInstance, releaseNameCommandList,
                        true);
        assertThat(releaseNameCommandList.size()).isEqualTo(4);
        assertThat(releaseNameCommandList.get(1).right).isEqualTo("delete_pvc[app=x]");
        assertThat(releaseNameCommandList.get(releaseNameCommandList.size() - 1).right).isEqualTo("delete_pvc[]");
    }

    @Test
    public void testDeleteCommandIsAddedToFailurePattern() {
        VnfInstance sourceVnfInstance = new VnfInstance();
        VnfInstance targetVnfInstance = new VnfInstance();
        final String chart1 = "release-1";
        final String chart2 = "release-2";
        final String chart3 = "release-3";

        final List<HelmChart> sourceHelmCharts = new ArrayList<>();
        sourceHelmCharts.add(helmChartPrototype(chart1, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart2, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart3, sourceVnfInstance, true));
        sourceVnfInstance.setHelmCharts(sourceHelmCharts);

        final List<HelmChart> targetHelmCharts = new ArrayList<>();
        targetHelmCharts.add(helmChartPrototype(chart1, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart2, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart3, targetVnfInstance, true));
        targetVnfInstance.setHelmCharts(targetHelmCharts);

        List<MutablePair<String, String>> releaseNameCommandList = new ArrayList<>();
        releaseNameCommandList.add(new MutablePair<>(chart1, "delete"));
        releaseNameCommandList.add(new MutablePair<>(chart2, "delete_pvc[]"));
        releaseNameCommandList.add(new MutablePair<>(chart3, "delete"));
        releaseNameCommandList =
                rollbackPatternTransformation.transformChartCommandList(sourceVnfInstance, targetVnfInstance, releaseNameCommandList,
                        true);
        assertThat(releaseNameCommandList.size()).isEqualTo(4);
        assertThat(releaseNameCommandList.get(1).left).isEqualTo("release-2");
        assertThat(releaseNameCommandList.get(1).right).isEqualTo("delete");
    }

    @Test
    public void testDeleteCommandIsNotAddedToFailurePattern() {
        VnfInstance sourceVnfInstance = new VnfInstance();
        VnfInstance targetVnfInstance = new VnfInstance();
        final String chart1 = "release-1";
        final String chart2 = "release-2";
        final String chart3 = "release-3";

        final List<HelmChart> sourceHelmCharts = new ArrayList<>();
        sourceHelmCharts.add(helmChartPrototype(chart1, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart2, sourceVnfInstance, true));
        sourceHelmCharts.add(helmChartPrototype(chart3, sourceVnfInstance, true));
        sourceVnfInstance.setHelmCharts(sourceHelmCharts);

        final List<HelmChart> targetHelmCharts = new ArrayList<>();
        targetHelmCharts.add(helmChartPrototype(chart1, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart2, targetVnfInstance, true));
        targetHelmCharts.add(helmChartPrototype(chart3, targetVnfInstance, true));
        targetVnfInstance.setHelmCharts(targetHelmCharts);

        List<MutablePair<String, String>> releaseNameCommandList = Arrays.asList(
                new MutablePair<>(chart1, "delete"),
                new MutablePair<>(chart2, "delete"),
                new MutablePair<>(chart1, "delete_pvc[]"),
                new MutablePair<>(chart3, "delete")
        );
        releaseNameCommandList =
                rollbackPatternTransformation.transformChartCommandList(sourceVnfInstance, targetVnfInstance, releaseNameCommandList,
                        true);
        assertThat(releaseNameCommandList.size()).isEqualTo(4);
    }

    @Test
    public void testGetDefaultRollbackPattern() {
        VnfInstance instance = new VnfInstance();
        List<HelmChart> charts = new ArrayList<>();
        HelmChart chart1 = new HelmChart();
        chart1.setReleaseName("release-1");
        charts.add(chart1);
        HelmChart chart2 = new HelmChart();
        chart2.setReleaseName("release-2");
        charts.add(chart2);
        HelmChart chart3 = new HelmChart();
        chart3.setReleaseName("release-3");
        charts.add(chart3);
        HelmChart chart4 = new HelmChart();
        chart4.setReleaseName("release-4");
        chart4.setHelmChartType(HelmChartType.CRD);
        charts.add(chart4);
        instance.setHelmCharts(charts);
        List<MutablePair<String, String>> defaultRollbackPattern = getDefaultDowngradePattern(instance, instance);
        assertThat(defaultRollbackPattern.size()).isEqualTo(3);
        assertThat(defaultRollbackPattern.get(0).getLeft()).isEqualTo("release-3");
        assertThat(defaultRollbackPattern.get(1).getLeft()).isEqualTo("release-2");
        assertThat(defaultRollbackPattern.get(2).getLeft()).isEqualTo("release-1");
        assertThat(defaultRollbackPattern.stream()
                           .map(MutablePair::getRight).collect(Collectors.toList())).containsOnly(VnfPackageChangePatternCommand.ROLLBACK.getCommand());
    }

    @Test
    public void testGetPattern() {
        LifecycleOperation operation = new LifecycleOperation();
        List<LinkedHashMap<String, String>> pattern = getPattern(operation);
        assertThat(pattern).isNotNull().isEmpty();
        operation.setRollbackPattern("[{\"rollback-operation-1\": \"rollback\"},{\"rollback-operation-2\":\"rollback\"}]");
        pattern = getPattern(operation);
        assertThat(pattern).isNotNull().hasSize(2);
        operation.setRollbackPattern(null);
        operation.setFailurePattern("[{\"rollback-operation-1\": \"rollback\"}]");
        pattern = getPattern(operation);
        assertThat(pattern).isNotNull().hasSize(1);
    }

    @NotNull
    private HelmChart helmChartPrototype(final String name, final VnfInstance sourceVnfInstance, boolean isEnabled) {
        final HelmChart helmChart = new HelmChart(name, sourceVnfInstance, name, null, HelmChartType.CNF, name, null);
        helmChart.setReleaseName(name);
        helmChart.setChartEnabled(isEnabled);
        return helmChart;
    }
}
