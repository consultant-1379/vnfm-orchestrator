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
import static org.assertj.core.api.Assertions.entry;

import static com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceTestUtils.createTestReplicaDetails;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplicaDetailsMapperTest {

    private ReplicaDetailsMapper replicaDetailsMapper;

    @BeforeEach
    public void init() {
        ObjectMapper objectMapper = new ObjectMapper();
        replicaDetailsMapper = new ReplicaDetailsMapper(objectMapper);
    }

    @Test
    public void getReplicaDetailsFromHelmChart() {
        VnfInstance vnfInstance = createVnfInstanceWithDisabledChart();
        Map<String, Integer> expected = Map.of("eric-pm-bulk-reporter1", 1, "eric-pm-bulk-reporter2", 2);

        final Map<String, Integer> actual = replicaDetailsMapper.getReplicaCountFromHelmCharts(vnfInstance.getHelmCharts());

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    public void getReplicaDetailsFromHelmHistory() {
        // given
        final HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();

        final Map<String, ReplicaDetails> replicaDetails = Map.of(
                "chart-1",
                ReplicaDetails.builder()
                        .withScalingParameterName("scalingCount")
                        .withCurrentReplicaCount(2)
                        .withAutoScalingEnabledValue(true)
                        .withMaxReplicasParameterName("maxReplicas")
                        .withMaxReplicasCount(5)
                        .withMinReplicasParameterName("minReplicas")
                        .withMinReplicasCount(1)
                        .build());
        helmChartHistoryRecord.setReplicaDetails(convertObjToJsonString(replicaDetails));

        // when
        final Map<String, Object> actual = replicaDetailsMapper.getReplicaDetailsFromHelmHistory(helmChartHistoryRecord);

        // then
        assertThat(actual).containsOnly(entry("scalingCount", 2),
                                        entry("maxReplicas", 5),
                                        entry("minReplicas", 1));
    }

    private VnfInstance createVnfInstanceWithDisabledChart() {
        VnfInstance vnfInstance = new VnfInstance();
        List<HelmChart> helmCharts = new ArrayList<>();

        Map<String, Integer> enabledReplicas = Map.of("eric-pm-bulk-reporter1", 1, "eric-pm-bulk-reporter2", 2);
        final String enabledReplicaDetails = createTestReplicaDetails(enabledReplicas);

        HelmChart enabledHelmChart = new HelmChart();
        enabledHelmChart.setId("enabled-helm-chart-id");
        enabledHelmChart.setChartEnabled(true);
        enabledHelmChart.setHelmChartType(HelmChartType.CNF);
        enabledHelmChart.setReplicaDetails(enabledReplicaDetails);

        Map<String, Integer> disabledReplicas = Map.of("eric-pm-bulk-reporter3", 1, "eric-pm-bulk-reporter4", 2);
        final String disabledReplicaDetails = createTestReplicaDetails(disabledReplicas);

        HelmChart disabledHelmChart = new HelmChart();
        disabledHelmChart.setId("disabled-helm-chart-id");
        disabledHelmChart.setChartEnabled(false);
        disabledHelmChart.setHelmChartType(HelmChartType.CNF);
        disabledHelmChart.setReplicaDetails(disabledReplicaDetails);

        helmCharts.add(enabledHelmChart);
        helmCharts.add(disabledHelmChart);

        vnfInstance.setHelmCharts(helmCharts);

        return vnfInstance;
    }
}