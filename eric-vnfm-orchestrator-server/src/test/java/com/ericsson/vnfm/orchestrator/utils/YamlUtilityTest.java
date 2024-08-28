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
package com.ericsson.vnfm.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_CLIENT_VERSION_YAML;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertMapToYamlFormat;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.removeDotDelimitersFromYamlMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.groovy.util.Maps;
import org.assertj.core.api.AbstractStringAssert;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidFileException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YamlUtilityTest {

    @Test
    public void readLargeEscapedJsonIntoYaml() throws IOException, URISyntaxException {
        String escapedJson = readDataFromFile("yaml/large-escaped-json-a.txt");
        String converted = YamlUtility.convertEscapedJsonToYaml(escapedJson);
        assertThat(converted).contains("brAgent");
    }

    @Test
    public void readSmallJsonIntoYamlObject() {
        String json = "{\"keyA\": \"valueA\",\"keyB\": \"valueB\"}";
        String converted = YamlUtility.convertEscapedJsonToYaml(json);
        assertThat(converted).matches("keyA: valueA\nkeyB: valueB\n");
    }

    @Test
    public void readNestedJsonIntoYamlObject() {
        String json = "{\"a\": {\"b\": \"c\"}}";
        String converted = YamlUtility.convertEscapedJsonToYaml(json);
        assertThat(converted).matches("a:\n  b: c\n");
    }

    @Test
    public void readInvalidJsonIntoYamlObjectThrowsException() {
        String json = "{\"a\": {\"b: \"c\"}}";
        assertThatThrownBy(() -> YamlUtility.convertEscapedJsonToYaml(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contents of values.yaml additional parameter are malformed");
    }

    @Test
    public void convertMapToYamlFormatWithListType() {
        Map<String, Object> mapWithListType = Maps.of("backupTypeList",
                                                      Arrays.asList("configuration-data1", "configuration-data2"));

        String actualYaml = convertMapToYamlFormat(mapWithListType);

        assertThat(actualYaml).contains("backupTypeList").contains("configuration-data1", "configuration-data2");
    }

    @Test
    public void convertMapToYamlFormatWithMapType() {
        Map<String, Object> mapWithMapType = Maps.of("eric-pm-server",
                                                     Maps.of("rbac", "false",
                                                             "server", "true"));

        String actualYamlContent = convertMapToYamlFormat(mapWithMapType);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualYamlContent);
        actualContentAssert.contains("eric-pm-server").contains("rbac").contains("false");
        actualContentAssert.contains("eric-pm-server").contains("server").contains("true");
    }

    @Test
    public void testValidValuesYaml() throws URISyntaxException {
        assertThat(convertYamlFileIntoJson(TestUtils.getResource("valid_values.yaml"))).isNotNull();
    }

    @Test
    public void testInvalidValesYaml() {
        assertThatThrownBy(() -> convertYamlFileIntoJson(TestUtils.getResource("invalid_values.yaml")))
                .isInstanceOf(InvalidFileException.class).hasMessageStartingWith("Values file contains invalid YAML");
    }

    @Test
    public void testCovertInvalidYamlStringToJsonTrowsException() {
        assertThatThrownBy(() -> convertYamlStringIntoJson("123-abc:::::xyz")).isInstanceOf(InvalidInputException.class)
                .hasMessageStartingWith("Invalid YAML format. ");
    }

    @Test
    public void testConvertYamlStringToJson() {
        assertThat(convertYamlStringIntoJson("\"clusterData\": \"apiVersion: v1\\nkind: Config\\nclusters:\\n- name: \\\"clusterdev\\\"\\n " +
                "cluster:\\n certificate-authority-data: \\\"HGH0tLS1CRUdJTiBDRVJUSUZJQ0F=\\\"\\n "
                + "server: \\\"https://kubernetes.clusterdev/\\\"\\n\\"
                +
                "nusers:\\n- name: \\\"clusterdev\\\"\\n user:\\n token: "
                + "\\\"WDFTe56IjZ\\\"\\n\\ncontexts:\\n- name: \\\"clusterdev\\\"\\n context:\\n "
                +
                "user: \\\"clusterdev\\\"\\n cluster: \\\"clusterdev\\\"\\n\\ncurrent-context: "
                + "\\\"clusterdev\\\"\"")
                .toString()).isNotNull();
    }

    @Test
    public void testEvnfmValuesAreOmittedInFile() {
        Map<String, Object> values = new HashMap<>();
        values.put("keyA", "valueA");
        values.put("keyB", "valueB");
        values.put("keyC", null);
        values.put(APPLICATION_TIME_OUT, 3600);
        values.put(HELM_CLIENT_VERSION_YAML, "3.8");

        Path valuesFile = YamlUtility.writeMapToValuesFile(values);
        Map<String, Object> valuesFromFile = YamlUtility.convertYamlFileIntoMap(valuesFile);

        assertThat(valuesFromFile.size()).isEqualTo(2);
        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyA", "valueA"));
        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyB", "valueB"));
        assertThat(valuesFromFile.keySet()).doesNotContain("keyC");

        deleteFile(valuesFile);
    }

    @Test
    public void testEvnfmValuesAreOmittedInFileWithComplexValue() {
        Map<String, Object> values = new HashMap<>();
        values.put("keyA", "valueA");
        values.put("keyB", "valueB");
        Map<String, Object> map = new HashMap<>();
        map.put("allowed", true);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("downsize", map);
        values.put("upgrade", map1);

        Path valuesFile = YamlUtility.writeMapToValuesFile(values);
        Map<String, Object> valuesFromFile = YamlUtility.convertYamlFileIntoMap(valuesFile);

        assertThat(valuesFromFile.size()).isEqualTo(2);
        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyA", "valueA"));
        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyB", "valueB"));

        deleteFile(valuesFile);
    }

    @Test
    public void testEvnfmValuesAreOmittedInFileWithComplexValueNotAllIsEvnfm() {
        Map<String, Object> values = new HashMap<>();
        values.put("keyA", "valueA");
        values.put("keyB", "valueB");
        Map<String, Object> map = new HashMap<>();
        map.put("allowed", true);
        map.put("keyC", "valueC");
        map.put("keyD", null);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("downsize", map);
        values.put("upgrade", map1);

        Path valuesFile = YamlUtility.writeMapToValuesFile(values);
        Map<String, Object> valuesFromFile = YamlUtility.convertYamlFileIntoMap(valuesFile);

        assertThat(valuesFromFile.size()).isEqualTo(3);
        Map<String, Object> mapToStay = new HashMap<>();
        Map<String, Object> innerMapToStay = new HashMap<>();
        innerMapToStay.put("keyC", "valueC");
        innerMapToStay.put("keyD", null);
        mapToStay.put("downsize", innerMapToStay);

        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyA", "valueA"));
        MatcherAssert.assertThat(valuesFromFile, hasEntry("keyB", "valueB"));
        MatcherAssert.assertThat(valuesFromFile, hasEntry("upgrade", mapToStay));

        deleteFile(valuesFile);
    }

    @Test
    public void removeDotDelimitersFromYamlMapReplaceOldValueTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map;
        String data = "{\"eric-ccsm-mapproxy\":{\"eric-ccsm-mapproxy-3\":{\"nodeSelector\":{\"Deployment\":{\"test\":\"test\"}}}}, "
                + "\"eric-ccsm-mapproxy.eric-ccsm-mapproxy-3.nodeSelector.Deployment\":{}}";
        map = objectMapper.readValue(data,  Map.class);
        map = removeDotDelimitersFromYamlMap(map);
        assertThat(objectMapper.writeValueAsString(map)).isEqualTo("{\"eric-ccsm-mapproxy\":{\"eric-ccsm-mapproxy-3\":{\"nodeSelector\":{\"Deployment\":{}}}}}");
    }

    @Test
    public void removeDotDelimitersFromYamlMapEditOldValueTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map;
        String data = "{\"eric-ccsm-mapproxy\":{\"eric-ccsm-mapproxy-3\":{\"nodeSelector\":{\"Deployment\":{\"test\":\"test\"}}}}, "
                + "\"eric-ccsm-mapproxy.eric-ccsm-mapproxy-4.nodeSelector.Deployment\":{}}";
        map = objectMapper.readValue(data,  Map.class);
        map = removeDotDelimitersFromYamlMap(map);
        assertThat(objectMapper.writeValueAsString(map))
                .isEqualTo("{\"eric-ccsm-mapproxy\":{\"eric-ccsm-mapproxy-3\":{\"nodeSelector\":{\"Deployment\":{\"test\":\"test\"}}},"
                                                           + "\"eric-ccsm-mapproxy-4\":{\"nodeSelector\":{\"Deployment\":{}}}}}");
    }
}
