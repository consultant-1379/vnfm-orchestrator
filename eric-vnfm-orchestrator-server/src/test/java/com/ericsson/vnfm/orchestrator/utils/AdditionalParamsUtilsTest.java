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
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.constructor.SafeConstructor;


@SpringBootTest(classes = { AdditionalParamsUtils.class,
        YamlFileMerger.class,
        ObjectMapper.class,
        YamlUtility.class })
@MockBean({ HealOperation.class,
        HelmChartHistoryService.class,
        ReplicaDetailsMapper.class,
        OperationsUtils.class,
        LcmOpSearchService.class})
public class AdditionalParamsUtilsTest {

    private static final String SPRING = "spring";
    private static final String DATA = "data";
    private static final String URL_VALUE = "url-value";
    private static final String UPDATED_VERSION_VALUE = "updated-version";
    private static final String VERSION = "version";
    private static final String URL = "url";
    private static final String LOGIN_VALUE = "login-value";
    private static final String LOGIN = "login";
    private static final String HOST = "host";
    private static final String HOST_VALUE = "localhost";
    private static final String PASSWORD = "password";
    private static final String UPDATED_PASSWORD_VALUE = "updated-password-value";
    @Autowired
    private AdditionalParamsUtils additionalParamsUtils;
    @Test
    public void testWithNulls() {
        final String emptyParamsString = additionalParamsUtils.mergeAdditionalParams(null, null);
        Map<String, Object> emptyParams = new Yaml(new SafeConstructor(new LoaderOptions())).load(emptyParamsString);
        assertThat(emptyParams).isEmpty();
    }

    @Test
    public void testMapOverriden() throws IOException, URISyntaxException {
        Map<String, Object> testParamsOne = readYamlAndConvertToMap(
                "additionalParamsUtilsTestData_test-params-one.yaml");
        Map<String, Object> testParamsTwo = readYamlAndConvertToMap(
                "additionalParamsUtilsTestData_test-params-two.yaml");
        String mergedAdditionalParamsString = additionalParamsUtils.mergeAdditionalParams(testParamsOne, testParamsTwo);
        Map<String, Object> mergedAdditionalParamsMap = new Yaml(new SafeConstructor(new LoaderOptions())).load(mergedAdditionalParamsString);
        Map<String, Object> springMap = Utility.copyParametersMap(mergedAdditionalParamsMap.get(SPRING));
        Map<String, Object> springDataMap = Utility.copyParametersMap(springMap.get(DATA));
        assertThat(UPDATED_VERSION_VALUE).isEqualTo(mergedAdditionalParamsMap.get(VERSION));
        assertThat(HOST_VALUE).isEqualTo(mergedAdditionalParamsMap.get(HOST));
        assertThat(mergedAdditionalParamsMap).doesNotContain(Map.entry("skipVerification", false));
        assertThat(springDataMap).contains(entry(LOGIN, LOGIN_VALUE),
                                           entry(PASSWORD, UPDATED_PASSWORD_VALUE),
                                           entry(URL, URL_VALUE),
                                           entry("test", "value"));
    }

    @Test
    public void testOneMapIsEmpty() throws IOException, URISyntaxException {
        Map<String, Object> testParamsOne = readYamlAndConvertToMap(
                "additionalParamsUtilsTestData_test-params-one.yaml");
        String emptyMapToAddParamString = additionalParamsUtils.mergeAdditionalParams(new HashMap<>(), testParamsOne);
        String addParamToEmptyMapString = additionalParamsUtils.mergeAdditionalParams(testParamsOne, new HashMap<>());
        Map<String, Object> emptyMapToAddParam = new Yaml(new SafeConstructor(new LoaderOptions())).load(emptyMapToAddParamString);
        Map<String, Object> addParamToEmptyMap = new Yaml(new SafeConstructor(new LoaderOptions())).load(addParamToEmptyMapString);
        assertThat(emptyMapToAddParam).hasSize(3);
        assertThat(addParamToEmptyMap).hasSize(3);
    }
    private Map<String, Object> readYamlAndConvertToMap(String fileName) throws IOException, URISyntaxException {
        String yamlString = TestUtils.readDataFromFile(getClass(), fileName);
        String jsonString = additionalParamsUtils.convertYamlStringToJsonString(yamlString);
        return Utility.convertStringToJSONObj(jsonString);
    }
}