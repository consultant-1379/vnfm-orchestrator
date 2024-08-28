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

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_CLIENT_VERSION_YAML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_WAIT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.google.common.collect.Maps;


@SpringBootTest(classes = {
    ValuesFileComposer.class,
    YamlFileMerger.class
})
public class ValuesFileComposerTest {
    @Autowired
    private ValuesFileComposer valuesFileComposer;

    private Map<String, Object> testAdditionalParams;

    @BeforeEach
    public void setUp() {
        testAdditionalParams = new HashMap<>();
        testAdditionalParams.put("clusterName", "default");
        testAdditionalParams.put("backupTypeList", Arrays.asList("configuration-data1", "configuration-data2"));

        Map<String, Object> testInnerParameter = new HashMap<>();
        testInnerParameter.put("geode", true);
        testAdditionalParams.put("eric-pm-server", testInnerParameter);
    }

    @Test
    public void shouldMergeAdditionalParams() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("clusterName").contains("default");
        actualContentAssert.contains("backupTypeList").contains("configuration-data1", "configuration-data2");
        actualContentAssert.contains("eric-pm-server").contains("geode").contains("true");
        actualContentAssert.contains("eric-pm-server").contains("server").contains("ingress").contains("enabled")
                .contains("true");
    }

    @Test
    public void shouldReturnOriginalValuesIfNoAdditionalParams() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");

        String actualContent = valuesFileComposer.compose(valuesFileContent, Maps.newHashMap());

        assertThat(actualContent).contains("eric-pm-server").contains("server").contains("ingress").contains("enabled")
            .contains("true");
    }

    @Test
    public void shouldReturnEmptyStringIfNoAdditionalParametersAndNullValues() {
        String actualContent = valuesFileComposer.compose(null, Maps.newHashMap());

        assertThat(actualContent).isEmpty();
    }

    @Test
    public void shouldReturnOnlyAdditionalParametersIfNullValues() {
        String actualContent = valuesFileComposer.compose(null, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("clusterName").contains("default");
        actualContentAssert.contains("eric-pm-server").contains("geode").contains("true");
    }

    @Test
    public void shouldNotMergeEVNFMParams() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put(HELM_WAIT, "1000");
        testAdditionalParams.put(APPLICATION_TIME_OUT, "500");
        testAdditionalParams.put(HELM_CLIENT_VERSION_YAML, "3.8");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain(HELM_WAIT);
        actualContentAssert.doesNotContain(APPLICATION_TIME_OUT);
        actualContentAssert.doesNotContain(HELM_CLIENT_VERSION_YAML);
    }

    @Test
    public void shouldMergeValuesParameterIfPresent() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        String valuesYamlParamContent = "{\n   \"description\":\"VNF Descriptor for Ericsson SGSN-MME\"\n }";
        testAdditionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, valuesYamlParamContent);

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);
        actualContentAssert.contains("description", "VNF Descriptor for Ericsson SGSN-MME");
    }

    @Test
    public void shouldOverwriteValuesParameterByAdditionalParameter() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        String valuesYamlParamContent = "{\n   \"description\":\"VNF Values YAML Descriptor for Ericsson SGSN-MME\"\n }";
        testAdditionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, valuesYamlParamContent);
        testAdditionalParams.put("description", "VNF Additional Param Descriptor for Ericsson SGSN-MME");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);
        actualContentAssert.contains("description", "VNF Additional Param Descriptor for Ericsson SGSN-MME");
    }

    @Test
    public void shouldPassMagicValuesJsonWithDots() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        String valuesYamlParamContent = "{\n   \"intel.com/somevalue\": 100\n }";
        testAdditionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, valuesYamlParamContent);

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);
        actualContentAssert.contains("intel.com/somevalue");
    }

    @Test
    public void shouldRemoveDotDelimitersFromAdditionalParamContent() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("param.with.dot", "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("param:").contains("with:").contains("dot:");
    }

    @Test
    public void shouldNotRemoveEscapedDotDelimitersFromAdditionalParamContent() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("param\\.with\\.dot", "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("param.with.dot");
    }

    @Test
    public void shouldEscapeThirdAndFifthLevel() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("first.second.third\\.complex.sixth\\.complex", "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("first").contains("second").contains("third.complex").contains("sixth.complex");
    }

    @Test
    public void shouldNotRemoveDotDelimitersFromFileContent() {
        String valuesFileContent = readFile("values-file-composer/values-with-dots-merge.yaml");
        testAdditionalParams.put(HELM_WAIT, "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("param.with.dots:");
    }

    @Test
    public void shouldNotRemoveDotDelimitersFromAdditionalParameters() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("intel\\.com/somevalue", "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("intel.com/somevalue").contains("1000");
    }

    @Test
    public void shouldSupportAnchorValues() {
        String valuesFileContent = readFile("values-file-composer/values-merge-with-anchor.yaml");
        testAdditionalParams.put(HELM_WAIT, "1000");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.contains("appArmorProfile: docker-pcc");
    }

    @Test
    public void shouldNotMergeDay0Parameters() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("param.with.dot", "1000");
        testAdditionalParams.put("day0.configuration.secretname", "test-scret");
        testAdditionalParams.put("day0.configuration.param1.key", "test-key");
        testAdditionalParams.put("day0.configuration.param1.value", "test-value");

        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);

        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain("day0").doesNotContain("secretname").doesNotContain("param1");
    }

    @Test
    public void shouldNotMergeOssTopologyParameters() {
        String valuesFileContent = readFile("values-file-composer/values-file.yaml");
        testAdditionalParams.put("param.with.dot", "1000");
        testAdditionalParams.put("ossTopology.secretname", "test-secret");
        testAdditionalParams.put("ossTopology.param1.key", "test-key");
        testAdditionalParams.put("ossTopology.param1.value", "test-value");
        String actualContent = valuesFileComposer.compose(valuesFileContent, testAdditionalParams);
        AbstractStringAssert<?> actualContentAssert = assertThat(actualContent);
        actualContentAssert.doesNotContain("ossTopology").doesNotContain("secretname").doesNotContain("param1");
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }

}
