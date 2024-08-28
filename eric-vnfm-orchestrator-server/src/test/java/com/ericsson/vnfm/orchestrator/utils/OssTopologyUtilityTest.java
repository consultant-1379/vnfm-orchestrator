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

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENTITY_PROFILE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_IN_MINUTES;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologySpecificParameters;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getTopology;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.mergeMaps;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.removeOssTopologyFromKey;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.removeOssTopologyFromKeyWithAdditionalAttributes;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.validateOtpValidityPeriod;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologyManagedElementId;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.getInstantiateName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.google.common.collect.ImmutableMap;

public class OssTopologyUtilityTest {

    @Test
    public void testGetOssTopologySpecificParametersShouldFilter() {
        Map<String, Object> map = ImmutableMap.of("ossTopology", ImmutableMap.of("test", "true"), "Two", "false", "Three", "temp");
        Map<String, Object> ossTopologySpecificParameters = getOssTopologySpecificParameters(new HashMap<>(map));
        assertThat(ossTopologySpecificParameters).hasSize(1);
        assertThat(ossTopologySpecificParameters).doesNotContainKey("ossTopology.test");
        assertThat(ossTopologySpecificParameters).containsKey("test");
    }

    @Test
    public void testGetOssTopologySpecificParametersShouldReturnEmptyMap() {
        Map<String, String> map = ImmutableMap.of("test", "true", "Two", "false", "Three", "temp");
        Map<String, String> ossTopologySpecificParameters = getOssTopologySpecificParameters(new HashMap<>(map));
        assertThat(ossTopologySpecificParameters).isEmpty();
    }

    @Test
    public void testGetOssTopologySpecificParametersShouldSkipNullValues() {
        Map<String, String> map = new HashMap<>();
        map.put("ossTopology.one", "true");
        map.put("ossTopology.two", "temp");
        map.put("ossTopology.three", null);
        map.put("four", null);
        Map<String, String> ossTopologySpecificParameters = getOssTopologySpecificParameters(map);
        assertThat(ossTopologySpecificParameters).hasSize(2);
        assertThat(ossTopologySpecificParameters).containsKey("one");
        assertThat(ossTopologySpecificParameters).containsKey("two");
    }

    @Test
    public void testRemoveOssTopologyFromKeyShouldSkipNullValues() {
        Map<String, String> map = new HashMap<>();
        map.put("ossTopology.one", "true");
        map.put("ossTopology.two", "temp");
        map.put("ossTopology.three", null);
        Map<String, String> ossTopologySpecificParameters = removeOssTopologyFromKey(map);
        assertThat(ossTopologySpecificParameters).hasSize(2);
        assertThat(ossTopologySpecificParameters).containsKey("one");
        assertThat(ossTopologySpecificParameters).containsKey("two");
    }

    @Test
    public void testGetTopologyShouldReturnFilteredMap() throws IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model.json");
        String type = "ericsson.datatypes.nfv.InstantiateVnfOperationAdditionalParameters";
        JSONObject jsonObject = new JSONObject(descriptorModel);
        Map<String, PropertiesModel> topology = getTopology(jsonObject, type);
        assertThat(topology).hasSize(30);
        Optional<String> ossTopology = topology.keySet().stream().filter(key -> key.contains("ossTopology"))
                .findFirst();
        assertThat(ossTopology).isEmpty();
    }

    @Test
    public void testGetTopologyShouldReturnEmptyMap() throws URISyntaxException, IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model-no-oss-topology.json");
        String type = "ericsson.datatypes.nfv.InstantiateVnfOperationAdditionalParameters";
        JSONObject jsonObject = new JSONObject(descriptorModel);
        Map<String, PropertiesModel> topology = getTopology(jsonObject, type);
        assertThat(topology).isEmpty();
    }

    @Test
    public void testGetInstantiateNameShouldReturnName() throws URISyntaxException, IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model.json");
        String type = "ericsson.datatypes.nfv.InstantiateVnfOperationAdditionalParameters";
        JSONObject jsonObject = new JSONObject(descriptorModel);
        assertThat(getInstantiateName(jsonObject)).isEqualTo(type);
    }

    @Test
    public void testGetTopologyShouldReturnNullWhenInstantiateObjectMissing() throws URISyntaxException,
            IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model-instantiate-model-missing.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        assertThat(getInstantiateName(jsonObject)).isNull();
    }

    @Test
    public void testGetTopologyShouldReturnNullWhenInstantiateInputsObjectMissing() throws URISyntaxException,
            IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model-instantiates-inputs-missing.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        assertThat(getInstantiateName(jsonObject)).isNull();
    }

    @Test
    public void testGetTopologyShouldReturnNullWhenInstantiateAdditionalAttributesObjectMissing() throws URISyntaxException,
            IOException {
        String descriptorModel = getFile("oss-topology/descriptor-model-missing-additional-params.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        assertThat(getInstantiateName(jsonObject)).isNull();
    }

    @Test
    public void testMergeMaps() throws URISyntaxException,
            IOException {
        Map<String, PropertiesModel> map = new HashMap<>();
        map.put("first", new PropertiesModel("true"));
        map.put("second", new PropertiesModel("false"));
        Map<String, Object> map1 = new HashMap<>(ImmutableMap.of("first", "false", "third", "false"));
        mergeMaps(map, map1);
        assertThat(map).hasSize(3);
        assertThat(map.get("first").getDefaultValue()).isEqualTo("false");
        assertThat(map).containsKeys("first", "second", "third");
    }

    @Test
    public void testRemovingKeysFromAdditionalAttributes() {
        Map<String, Object> mapContainingAdditionalAttributeObject = new HashMap<>();
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("ossTopology.key", "value");
        mapContainingAdditionalAttributeObject.put("additionalAttributes", additionalAttributes);
        assertThat(mapContainingAdditionalAttributeObject).containsKey("additionalAttributes");
        assertThat(removeOssTopologyFromKeyWithAdditionalAttributes(mapContainingAdditionalAttributeObject))
                .doesNotContainKey("additionalAttributes");
        assertThat(removeOssTopologyFromKeyWithAdditionalAttributes(mapContainingAdditionalAttributeObject)).containsEntry("key", "value");
    }

    @Test
    public void testRemovingKeysWithoutAdditionalAttributesObject() {
        Map<String, Object> mapWithoutAdditionalAttributeObject = new HashMap<>();
        mapWithoutAdditionalAttributeObject.put("ossTopology.key", "value");
        assertThat(mapWithoutAdditionalAttributeObject.containsKey("additionalAttributes")).isFalse();
        assertThat(removeOssTopologyFromKeyWithAdditionalAttributes(mapWithoutAdditionalAttributeObject)).doesNotContainKey("additionalAttributes");
        assertThat(removeOssTopologyFromKeyWithAdditionalAttributes(mapWithoutAdditionalAttributeObject)).containsEntry("key", "value");
    }

    @Test
    public void testCreateSitebasicFileWithEmptyOssParams() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceName("test-vnf-node");
        assertThat(OssTopologyUtility.createSitebasicFileFromOSSParams(vnfInstance, null))
                .isNotNull()
                .contains(String.format("<nodeFdn>%s</nodeFdn>", vnfInstance.getVnfInstanceName()));
        assertThat(OssTopologyUtility.createSitebasicFileFromOSSParams(vnfInstance, new HashMap<>()))
                .isNotNull()
                .contains(String.format("<nodeFdn>%s</nodeFdn>", vnfInstance.getVnfInstanceName()));
    }

    @Test
    public void testCreateSitebasicFileWithExtendedOssParams() {
        VnfInstance vnfInstance = new VnfInstance();
        Map<String, Object> params = Map.of(OTP_VALIDITY_PERIOD_IN_MINUTES, 100,
                                            ENTITY_PROFILE_NAME, "testName");
        vnfInstance.setVnfInstanceName("test-vnf-node");

        assertThat(OssTopologyUtility.createSitebasicFileFromOSSParams(vnfInstance, params))
                .isNotNull()
                .contains("<otpValidityPeriodInMinutes>100</otpValidityPeriodInMinutes>")
                .contains("<entityProfileName>testName</entityProfileName>");
    }

    @Test
    public void testGenerateSitebasicXMLWhenParsingExceptionThrown() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceName("test-vnf-node");

        try (MockedStatic<XMLUtility> utility = Mockito.mockStatic(XMLUtility.class)) {
            utility.when(XMLUtility::createEmptyXMLDocument)
                    .thenThrow(new ParserConfigurationException());
            assertThat(OssTopologyUtility.createSitebasicFileFromOSSParams(vnfInstance, Map.of("ossTopology.managedElementId", "vnf-inst-node-id")))
                    .isEmpty();
        }
    }

    @Test
    public void testVerifyOtpValidityPeriodWithValidValue() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, 1);

        validateOtpValidityPeriod(additionalParams);

        assertThatNoException().isThrownBy(() -> validateOtpValidityPeriod(additionalParams));
    }

    @Test
    public void testVerifyOtpValidityPeriodWithValidValueNeverExpires() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, -1);

        assertThatNoException().isThrownBy(() -> validateOtpValidityPeriod(additionalParams));
    }

    @Test
    public void testVerifyOtpValidityPeriodWithInvalidValue() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, "value");

        assertThatThrownBy(() -> validateOtpValidityPeriod(additionalParams))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testVerifyOtpValidityPeriodWithInvalidUpperBoundValue() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, 43201);

        assertThatThrownBy(() -> validateOtpValidityPeriod(additionalParams))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testVerifyOtpValidityPeriodWithInvalidLowerBoundValue() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, 0);

        assertThatThrownBy(() -> validateOtpValidityPeriod(additionalParams))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testGetOssTopologyManagedElementIdAlias() {
        Optional<Object> managedElementId = getOssTopologyManagedElementId("{\"managedElementId\":\"mElementId\"}");
        assertThat(managedElementId).isPresent();
        assertThat(managedElementId.get()).isEqualTo("mElementId");

        Optional<Object> networkElementId = getOssTopologyManagedElementId("{\"networkElementId\":\"nElementId\"}");
        assertThat(networkElementId).isPresent();
        assertThat(networkElementId.get()).isEqualTo("nElementId");
    }

    @Test
    public void testGetOssTopologyManagedElementIdIfBothElementIdPresent() {
        Optional<Object> managedElementIdPriority = getOssTopologyManagedElementId("{\"managedElementId\":\"mElementId\", \"networkElementId\":\"nElementId\"}");
        assertThat(managedElementIdPriority).isPresent();
        assertThat(managedElementIdPriority.get()).isEqualTo("nElementId");

        Optional<Object> managedElementId = getOssTopologyManagedElementId("{\"managedElementId\":\"mElementId\", \"networkElementId\":\"\"}");
        assertThat(managedElementId).isPresent();
        assertThat(managedElementId.get()).isEqualTo("mElementId");

        Optional<Object> networkElementId = getOssTopologyManagedElementId("{\"managedElementId\":\"\", \"networkElementId\":\"nElementId\"}");
        assertThat(networkElementId).isPresent();
        assertThat(networkElementId.get()).isEqualTo("nElementId");
    }

    public String getFile(final String file) {
        return readDataFromFile(getClass(), file);
    }
}
