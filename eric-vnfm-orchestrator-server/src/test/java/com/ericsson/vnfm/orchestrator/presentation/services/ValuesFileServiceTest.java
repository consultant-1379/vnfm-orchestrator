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

import static com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService.getPropertiesFromValuesYamlMap;
import static com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService.getPropertyFromValuesYamlMap;
import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.createHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.AdditionalArtifactModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.DefaultReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ScaleMappingReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    YamlFileMerger.class,
    ValuesFileService.class,
    ObjectMapper.class,
    ValuesFileComposer.class,
    ReplicaDetailsServiceImpl.class,
    DefaultReplicaDetailsCalculationService.class,
    ScaleMappingReplicaDetailsCalculationService.class,
    ReplicaCountCalculationServiceImpl.class,
    ReplicaDetailsBuilder.class,
    ReplicaDetailsMapper.class,
    ExtensionsMapper.class,
    AdditionalAttributesHelper.class
})
@MockBean(classes = {
    MappingFileServiceImpl.class,
    ScaleServiceImpl.class,
    VnfInstanceServiceImpl.class
})
public class ValuesFileServiceTest {
    private static final String PATH_TO_ADDITIONAL_VALUES = "Definitions/OtherTemplates/";

    @Autowired
    private ValuesFileService valuesFileService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PackageServiceImpl packageService;

    @Test
    public void testMergeMapsWithValuesFileEmpty() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(objectMapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile("{}");
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        Assertions.assertThat(stringObjectMap.size()).isEqualTo(3);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingFile());
    }

    @Test
    public void testMergeMapsWithValuesFile() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(objectMapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile(readDataFromFile(getClass(),"values-file-service/combined-values-file.json"));
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        assertThat(stringObjectMap.toString()).contains(getExpectedMergedOutput());
        assertThat(stringObjectMap.size()).isEqualTo(6);
    }

    @Test
    public void testMergeMapsWithValuesFileNull() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(objectMapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile(null);
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        Assertions.assertThat(stringObjectMap.size()).isEqualTo(3);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingFile());
    }

    @Test
    public void testMergeMapsWithAdditionalParamsAsNull() {
        VnfInstance instance = new VnfInstance();
        instance.setCombinedAdditionalParams(null);
        instance.setCombinedValuesFile(readDataFromFile(getClass(), "values-file-service/combined-values-file-no-additional-params.json"));
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        Assertions.assertThat(stringObjectMap.size()).isEqualTo(5);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingAdditionalParams());
    }

    @Test
    public void testMergeMapsWithAdditionalParamsAsEmpty() {
        VnfInstance instance = new VnfInstance();
        instance.setCombinedAdditionalParams("{}");
        instance.setCombinedValuesFile(readDataFromFile(getClass(), "values-file-service/additional-values.json"));
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        Assertions.assertThat(stringObjectMap.size()).isEqualTo(5);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingAdditionalParams());
    }

    @Test
    public void testMergeMapsWithAdditionalValuesWithFileAtrributes() {
        VnfInstance instance = new VnfInstance();
        String inputAttributes = readDataFromFile(getClass(), "values-file-service/additional-attributes-to-merge-with-file-types.json");
        String expectedAttributes = readDataFromFile(getClass(), "values-file-service/combined-values-file-with-file-attributes.txt");
        instance.setCombinedValuesFile(inputAttributes);
        Path path = valuesFileService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();

        Assertions.assertThat(stringObjectMap.size()).isEqualTo(4);
        Assertions.assertThat(stringObjectMap.toString()).isEqualToIgnoringNewLines(expectedAttributes);
    }

    @Test
    public void testMergeValuesYamlFile() {
        Map<String, Object> expectedAdditionalParams
                = new HashMap<>(Map.of("param", true, "naram", false, "extra", true));

        Path yamlFileFilePath = getResource(getClass(), "merge-values-yaml-file.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(yamlFileFilePath);

        valuesFileService.mergeValuesYamlMap(valuesYamlMap, expectedAdditionalParams);

        assertThat(expectedAdditionalParams).isEqualTo(valuesYamlMap);
    }

    @Test
    public void testMergeNumericKeys() {
        Map<String, Object> expectedValues
                = new HashMap<>(Map.of("100", "test_pass", "200", "test_pass"));

        Map<?, Object> additionalParams
                = new HashMap<>(Map.of(200, "test_pass"));

        Path yamlFileFilePath = getResource(getClass(), "merge-numeric-keys.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(yamlFileFilePath);

        valuesFileService.mergeValuesYamlMap(valuesYamlMap, (Map<String, Object>) additionalParams);

        assertThat(valuesYamlMap).isEqualTo(expectedValues);
    }

    @Test
    public void shouldUpdateValuesMapWithReplicaDetails() {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> replicateDetails = new HashMap<>();
        Map<String, ReplicaDetails> replicaDetailsList = new HashMap<>();
        replicaDetailsList.put("CL_scaled_vm",
                               ReplicaDetails.builder()
                                   .withScalingParameterName("CL_scaled_vm")
                                   .withMaxReplicasParameterName("CL_scaled_vm_maxName")
                                   .withMinReplicasParameterName("CL_scaled_vm_minName")
                                   .withAutoScalingEnabledValue(true)
                                   .withCurrentReplicaCount(2)
                                   .withMaxReplicasCount(2)
                                   .withMinReplicasCount(2)
                                   .build());
        replicaDetailsList.put("PL_scaled_vm",
                               ReplicaDetails.builder()
                                   .withScalingParameterName("PL_scaled_vm")
                                   .withMaxReplicasParameterName("PL_scaled_vm_maxName")
                                   .withMinReplicasParameterName("PL_scaled_vm_minName")
                                   .withAutoScalingEnabledValue(true)
                                   .withCurrentReplicaCount(1)
                                   .withMaxReplicasCount(1)
                                   .withMinReplicasCount(1)
                                   .build());
        createHelmChart(instance);
        instance.getHelmCharts().get(0).setReplicaDetails(convertObjToJsonString(replicaDetailsList));
        instance.setTempInstance(convertObjToJsonString(instance));

        valuesFileService.updateValuesYamlMapWithReplicaDetailsAndHighestPriority(replicateDetails, instance);

        assertThat(replicateDetails).hasSize(6);
        assertThat(replicateDetails).containsEntry("CL_scaled_vm", 2);
        assertThat(replicateDetails).containsEntry("CL_scaled_vm_minName", 2);
        assertThat(replicateDetails).containsEntry("CL_scaled_vm_maxName", 2);
        assertThat(replicateDetails).containsEntry("PL_scaled_vm", 1);
        assertThat(replicateDetails).containsEntry("PL_scaled_vm_minName", 1);
        assertThat(replicateDetails).containsEntry("PL_scaled_vm_maxName", 1);
    }

    @Test
    public void testSpecificChartValuesWhenPathExist() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfPackageId("pkg1");
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        helmChart.setPriority(1);
        vnfInstance.setHelmCharts(List.of(helmChart));
        PackageResponse packageResponse = new PackageResponse();

        String desiredPath = PATH_TO_ADDITIONAL_VALUES + "spider-a-1.0.yaml";
        String desiredValuesPath = "/secondValue.yaml";

        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredPath, "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));

        Mockito.when(packageService.getPackageInfo(vnfInstance.getVnfDescriptorId()))
                .thenReturn(packageResponse);
        Mockito.when(packageService.getPackageArtifacts(vnfInstance.getVnfPackageId(), desiredPath))
                .thenReturn(Optional.of(desiredValuesPath));

        Path path = valuesFileService.getChartSpecificValues(vnfInstance, 1);

        Mockito.verify(packageService, Mockito.times(1)).getPackageArtifacts(vnfInstance.getVnfPackageId(), desiredPath);
        assertThat(path.toString()).isNotNull();
        deleteFile(path);
        assertThat(path.toFile()).doesNotExist();
    }

    @Test
    public void testSpecificChartValuesWhenArtifactsNotExist() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfPackageId("pkg1");
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        helmChart.setPriority(1);
        vnfInstance.setHelmCharts(List.of(helmChart));

        Mockito.when(packageService.getPackageInfo(vnfInstance.getVnfDescriptorId()))
                .thenReturn(new PackageResponse());

        Path path = valuesFileService.getChartSpecificValues(vnfInstance, 1);

        Mockito.verify(packageService, Mockito.never()).getPackageArtifacts(Mockito.any(), Mockito.any());
        assertThat(path).isNull();
    }

    @Test
    public void testSpecificChartValuesWhenPathNotFound() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfPackageId("pkg1");
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartName("spider-b");
        helmChart.setPriority(1);
        vnfInstance.setHelmCharts(List.of(helmChart));

        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(
                "/",
                "/path/spider-b.yaml",
                "/path/spider-b.yml",
                PATH_TO_ADDITIONAL_VALUES,
                PATH_TO_ADDITIONAL_VALUES + "1-spider-a.yaml",
                PATH_TO_ADDITIONAL_VALUES + "spider-b-2.yaml",
                PATH_TO_ADDITIONAL_VALUES + "spider-b"));

        Mockito.when(packageService.getPackageInfo(vnfInstance.getVnfInstanceId()))
                .thenReturn(packageResponse);

        Path path = valuesFileService.getChartSpecificValues(vnfInstance, 1);
        Mockito.verify(packageService, Mockito.never()).getPackageArtifacts(Mockito.any(), Mockito.any());
        assertThat(path).isNull();
    }

    @Test
    public void testGetPropertyFromValuesYamlFileReturnsValueWhenExists() {
        Map<String, Object> valuesYaml = Map.of("key1", Map.of("innerKey", Map.of("innerKey2", "someValue")));

        Object actual = getPropertyFromValuesYamlMap(valuesYaml, "key1.innerKey.innerKey2", Object.class);
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo("someValue");
    }

    @Test
    public void testGetPropertyFromValuesYamlFileReturnsNullWhenValueNotFound() {
        Map<String, Object> valuesYaml = Map.of("key1", Map.of("innerKey", Map.of("innerKey2", "someValue")));

        Object actual = getPropertyFromValuesYamlMap(valuesYaml, "non.existent.key", Object.class);
        assertThat(actual).isNull();
    }

    @Test
    public void testGetPropertiesFromValuesYamlFileReturnMapWhenValuesFound() {
        Map<String, Object> valuesYaml = Map.of("key1", Map.of("innerKey", Map.of("innerKey2", "someValue")));

        Map<String, String> result = getPropertiesFromValuesYamlMap(valuesYaml, List.of("key1.innerKey.innerKey2"));

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("key1.innerKey.innerKey2")).isNotNull();
        assertThat(result.get("key1.innerKey.innerKey2")).isEqualTo("someValue");
    }

    @Test
    public void testGetPropertiesFromValuesYamlFileReturnEmptyMapWhenNotFound() {
        Map<String, Object> valuesYaml = Map.of("key1", Map.of("innerKey", Map.of("innerKey2", "someValue")));

        Map<String, String> result = getPropertiesFromValuesYamlMap(valuesYaml, List.of("non.existent.key"));

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetPropertiesFromValuesYamlFileReturnEmptyMapWhenOneKeyNotFound() {
        Map<String, Object> valuesYaml = Map.of("key1", Map.of("innerKey", Map.of("innerKey2", "someValue")));

        Map<String, String> result = getPropertiesFromValuesYamlMap(valuesYaml, List.of("non.existent.key", "key1.innerKey.innerKey2"));

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    private List<AdditionalArtifactModel> createAdditionalArtifacts(String... artifactPaths){
        var additionalArtifacts = new ArrayList<AdditionalArtifactModel>();
        for (String path : artifactPaths) {
            var additionalArtifactModel = new AdditionalArtifactModel();
            additionalArtifactModel.setArtifactPath(path);
            additionalArtifacts.add(additionalArtifactModel);
        }
        return additionalArtifacts;
    }

    private String getExpectedMergedOutput() {
        return "{test-with-quotes=<config xmlns=\"http://tail-f.com/ns/config/1.0\">, eric-pm-server={server={ingress={enabled=false}, "
            + "persistentVolume={storageClass=erikube-rbd}}}, "
            + "eric-adp-gs-testapp={ingress={enabled=false}}, influxdb={ext={apiAccessHostname=true}}, "
            + "pm-testapp={ingress={domain=server}}, tags={all=false, pm=<xml version=1.0 encoding=UTF-8?><Nodes>  <Node>    "
            + "<nodeFdn>VPP00001</nodeFdn>    <certType>OAM</certType>    <enrollmentMode>CMPv2_INITIAL</enrollmentMode> </Node></Nodes>}}";
    }

    private String getExpectedOutputMissingFile() {
        return "{test-with-quotes=<config xmlns=\"http://tail-f.com/ns/config/1.0\">, influxdb={ext={apiAccessHostname=true}}, "
            + "pm-testapp={ingress={domain=server}}}";
    }

    private String getExpectedOutputMissingAdditionalParams() {
        return "{eric-pm-server={server={ingress={enabled=false}, persistentVolume={storageClass=erikube-rbd}}}, "
            + "eric-adp-gs-testapp={ingress={enabled=false}}, influxdb={ext={apiAccessHostname=influxdb-service2"
            + ".rontgen010.seli.gic.ericsson.se}}, pm-testapp={ingress={domain=rontgen010.seli.gic.ericsson.se}},"
            + " tags={all=false, pm=true}}";
    }

    private static void addParams(final Map<String, Object> additionalParams) {
        additionalParams.put("pm-testapp.ingress.domain", "server");
        additionalParams.put("influxdb.ext.apiAccessHostname", true);
        additionalParams.put("test-with-quotes", "<config xmlns=\"http://tail-f.com/ns/config/1.0\">");
    }

}