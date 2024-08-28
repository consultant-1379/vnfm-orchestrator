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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MAX_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MIN_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.REPLICA_PARAMETER_NAME;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.ericsson.am.shared.vnfd.ScalingMapUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public class ReplicaDetailsTestCommon {
    protected VnfInstance vnfInstanceRel4;
    protected VnfInstance vnfInstanceRel4WithoutInstantiationLevel;
    protected VnfInstance vnfInstanceRel4WithoutDefaultAndInstantiationLevel;
    protected String vnfd;
    protected Map<String, ScaleMapping> scaleMappingMap;
    protected Path toValuesFile;
    protected Policies policies;
    protected Policies policiesWithoutDefaultLevel;

    protected void setUp() throws URISyntaxException, IOException {
        HelmChart scaleHelmChart = new HelmChart();
        scaleHelmChart.setId("1");
        scaleHelmChart.setHelmChartUrl("https://registry-host:9999/test-scale-chart-6.0.0.tgz");
        HelmChart spiderHelmChart = new HelmChart();
        spiderHelmChart.setId("2");
        spiderHelmChart.setHelmChartUrl("https://registry-host:9999/spider-app-8.0.0.tgz");

        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(scaleHelmChart);
        helmCharts.add(spiderHelmChart);

        vnfInstanceRel4 = new VnfInstance();
        vnfInstanceRel4.setHelmCharts(helmCharts);
        vnfInstanceRel4.setRel4(true);
        vnfInstanceRel4.setInstantiationLevel("instantiation_level_2");

        vnfInstanceRel4WithoutInstantiationLevel = new VnfInstance();
        vnfInstanceRel4WithoutInstantiationLevel.setHelmCharts(helmCharts);
        vnfInstanceRel4WithoutInstantiationLevel.setRel4(true);

        vnfInstanceRel4WithoutDefaultAndInstantiationLevel = new VnfInstance();
        vnfInstanceRel4WithoutDefaultAndInstantiationLevel.setHelmCharts(helmCharts);
        vnfInstanceRel4WithoutDefaultAndInstantiationLevel.setRel4(true);

        String policiesContent = TestUtils.readDataFromFile("replicaCalculation/policies.json");
        String policiesContentWithoutDefaultLevel = TestUtils.readDataFromFile("replicaCalculation/policies-without-default-level.json");

        ObjectMapper objectMapper = new ObjectMapper();
        policies = objectMapper.readValue(policiesContent, Policies.class);
        policiesWithoutDefaultLevel = objectMapper.readValue(policiesContentWithoutDefaultLevel, Policies.class);

        vnfInstanceRel4.setPolicies(policiesContent);
        vnfInstanceRel4WithoutInstantiationLevel.setPolicies(policiesContent);
        vnfInstanceRel4WithoutDefaultAndInstantiationLevel.setPolicies(policiesContentWithoutDefaultLevel);

        Path scalingMappingPath = TestUtils.getResource("replicaCalculation/sm.yaml");
        scaleMappingMap = ScalingMapUtility.getScalingMap(scalingMappingPath);

        JSONObject vnfdJson = VnfdUtility.validateYamlAndConvertToJsonObject(TestUtils.readDataFromFile("replicaCalculation/vnfd.yaml"));
        vnfd = String.valueOf(vnfdJson);

        toValuesFile = TestUtils.getResource("replicaCalculation/values.yaml");
    }

    protected Map<String, ReplicaDetails> buildExpectedReplicaDetails(boolean fromScalingMapping) {
        Map<String, Integer> replicaCounts = buildExpectedReplicaCounts();
        return Map.of("test-cnf-vnfc0", buildReplicaDetails("vnfc0", replicaCounts, fromScalingMapping),
                      "eric-pm-bulk-reporter", buildReplicaDetails("eric-pm-bulk-reporter", replicaCounts, fromScalingMapping),
                      "test-cnf-vnfc1", buildReplicaDetails("vnfc1", replicaCounts, fromScalingMapping),
                      "test-cnf-vnfc3", buildReplicaDetails("vnfc3", replicaCounts, fromScalingMapping),
                      "test-cnf-vnfc4", buildReplicaDetails("vnfc4", replicaCounts, fromScalingMapping),
                      "test-cnf-vnfc5", buildReplicaDetails("vnfc5", replicaCounts, fromScalingMapping));
    }

    protected Map<String, Integer> buildExpectedReplicaCounts() {
        return Map.of("test-cnf-vnfc0", 1, "test-cnf-vnfc3", 1, "test-cnf-vnfc1", 1,
                      "test-cnf-vnfc4", 1, "test-cnf-vnfc5", 1, "eric-pm-bulk-reporter", 2);
    }

    private ReplicaDetails buildReplicaDetails(String vnfcName, Map<String, Integer> replicaCounts, boolean fromScalingMapping) {
        final Integer replicaCount = replicaCounts.entrySet().stream()
                .filter(replicaCountEntry -> replicaCountEntry.getKey().contains(vnfcName))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
        return ReplicaDetails.builder()
                .withMinReplicasParameterName(String.format(MIN_REPLICA_PARAMETER_NAME, vnfcName))
                .withMinReplicasCount(fromScalingMapping ? 1 : null)
                .withMaxReplicasParameterName(String.format(MAX_REPLICA_PARAMETER_NAME, vnfcName))
                .withMaxReplicasCount(fromScalingMapping ? replicaCount : null)
                .withScalingParameterName(String.format(REPLICA_PARAMETER_NAME, vnfcName))
                .withCurrentReplicaCount(replicaCount)
                .withAutoScalingEnabledParameterName(
                        fromScalingMapping ? String.format("%s.autoscaling.enabled", vnfcName) : null)
                .withAutoScalingEnabledValue(true)
                .build();
    }

}
