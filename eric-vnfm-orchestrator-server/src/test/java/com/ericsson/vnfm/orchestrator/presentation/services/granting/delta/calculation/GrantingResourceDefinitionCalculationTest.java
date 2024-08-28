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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceTestUtils.createTestReplicaDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinitionType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.lcm.GrantingResourceDefinitionCalculationImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class GrantingResourceDefinitionCalculationTest {

    @InjectMocks
    private final GrantingResourceDefinitionCalculation grantingResourceDefinitionCalculation = new GrantingResourceDefinitionCalculationImpl();

    @Mock
    private MappingFileService mappingFileService;

    @BeforeEach
    public void setUp() throws Exception {
        ObjectMapper objectMapper =  new ObjectMapper();
        ReplicaDetailsMapper replicaDetailsMapper = new ReplicaDetailsMapper(objectMapper);
        GrantingResourcesDeltaCalculationImpl deltaCalculation = new GrantingResourcesDeltaCalculationImpl();

        ReflectionTestUtils.setField(grantingResourceDefinitionCalculation, "replicaDetailsMapper", replicaDetailsMapper);
        ReflectionTestUtils.setField(grantingResourceDefinitionCalculation, "deltaCalculation", deltaCalculation);
    }

    @Test
    public void calculateRel4AddResourcesInUse() throws Exception {
        final List<ResourceDefinition> expected = createExpectedAddResources();

        final String vnfd = TestUtils.readDataFromFile("rel4_dm_upgrade_vnfd.yaml");
        final VnfInstance vnfInstance = createTargetVnfInstance();
        final JSONObject vnfdJson = VnfdUtility.validateYamlAndConvertToJsonObject(vnfd);

        when(mappingFileService.getScaleMapFilePathFromDescriptorModel(any())).thenReturn(Optional.empty());

        final List<ResourceDefinition> actual = grantingResourceDefinitionCalculation
                .calculateRel4ResourcesInUse(vnfdJson, vnfInstance, new HashMap<>(), "target");

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void calculateRel4RemoveResourcesInUse() throws Exception {
        final List<ResourceDefinition> expected = createExpectedRemoveResources();

        final String vnfd = TestUtils.readDataFromFile("rel4_dm_upgrade_vnfd.yaml");
        final VnfInstance vnfInstance = createSourceVnfInstance();
        final JSONObject vnfdJson = VnfdUtility.validateYamlAndConvertToJsonObject(vnfd);

        final List<ResourceDefinition> actual = grantingResourceDefinitionCalculation
                .calculateRel4ResourcesInUse(vnfdJson, vnfInstance, new HashMap<>(), "source");

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private List<ResourceDefinition> createExpectedAddResources() {
        final ResourceDefinition vnfc2 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("test-cnf-vnfc2")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("vnfc2_container"));
        final ResourceDefinition vnfc3_1 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("test-cnf-vnfc3")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("vnfc3_container"));
        final ResourceDefinition vnfc3_2 = ResourceDefinition.of(vnfc3_1);
        final ResourceDefinition vnfc4 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("test-cnf-vnfc4")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("vnfc4_container"));
        final ResourceDefinition vnfc1_1 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("test-cnf-vnfc1")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("vnfc1_container"));
        final ResourceDefinition vnfc1_2 = ResourceDefinition.of(vnfc1_1);
        final ResourceDefinition vnfc1_3 = ResourceDefinition.of(vnfc1_1);
        final ResourceDefinition vnfc0 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("test-cnf")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("test_cnf_container"));
        final ResourceDefinition vnfc0Storage = new ResourceDefinition()
                .setType(ResourceDefinitionType.STORAGE)
                .setVduId("test-cnf")
                .setVnfdId("target")
                .setResourceTemplateId(List.of("test_cnf_storage"));

        return List.of(vnfc0, vnfc0Storage, vnfc1_1, vnfc1_2, vnfc1_3, vnfc2, vnfc3_1, vnfc3_2, vnfc4);
     }

    private List<ResourceDefinition> createExpectedRemoveResources() {
        final ResourceDefinition ericPmBulkReporterOsContainer1 = new ResourceDefinition()
                .setType(ResourceDefinitionType.OSCONTAINER)
                .setVduId("eric-pm-bulk-reporter")
                .setVnfdId("source")
                .setResourceTemplateId(List.of("bulk_reporter_container"));
        final ResourceDefinition ericPmBulkReporterOsContainer2 = ResourceDefinition.of(ericPmBulkReporterOsContainer1);
        final ResourceDefinition ericPmBulkReporterStorage = new ResourceDefinition()
                .setType(ResourceDefinitionType.STORAGE)
                .setVduId("eric-pm-bulk-reporter")
                .setVnfdId("source")
                .setResourceTemplateId(List.of("bulk_reporter_storage"));

        return List.of(ericPmBulkReporterOsContainer1, ericPmBulkReporterOsContainer2, ericPmBulkReporterStorage);
    }

    private VnfInstance createTargetVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        List<HelmChart> helmCharts = new ArrayList<>();

        Map<String, Integer> enabledReplicas = Map.of(
                "test-cnf", 1, "test-cnf-vnfc1", 3, "test-cnf-vnfc2", 1, "test-cnf-vnfc3", 2,
                "test-cnf-vnfc4", 1);
        final String enabledReplicaDetails = createTestReplicaDetails(enabledReplicas);

        HelmChart enabledHelmChart = new HelmChart();
        enabledHelmChart.setId("enabled-helm-chart-id");
        enabledHelmChart.setChartEnabled(true);
        enabledHelmChart.setHelmChartType(HelmChartType.CNF);
        enabledHelmChart.setReplicaDetails(enabledReplicaDetails);
        enabledHelmChart.setHelmChartUrl("Definitions/OtherTemplates/helm-package1-1.0.0.tgz");

        Map<String, Integer> disabledReplicas = Map.of("eric-pm-bulk-reporter", 2);
        final String disabledReplicaDetails = createTestReplicaDetails(disabledReplicas);

        HelmChart disabledHelmChart = new HelmChart();
        disabledHelmChart.setId("disabled-helm-chart-id");
        disabledHelmChart.setChartEnabled(false);
        disabledHelmChart.setHelmChartType(HelmChartType.CNF);
        disabledHelmChart.setReplicaDetails(disabledReplicaDetails);
        disabledHelmChart.setHelmChartUrl("Definitions/OtherTemplates/helm-package2-2.0.0.tgz");

        helmCharts.add(enabledHelmChart);
        helmCharts.add(disabledHelmChart);

        vnfInstance.setHelmCharts(helmCharts);

        return vnfInstance;
    }

    private VnfInstance createSourceVnfInstance() {
        final VnfInstance vnfInstance = createTargetVnfInstance();
        vnfInstance.getHelmCharts()
                .forEach(helmChart -> helmChart.setChartEnabled(!helmChart.isChartEnabled()));
        return vnfInstance;
    }
}