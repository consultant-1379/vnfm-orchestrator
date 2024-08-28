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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.updateCombinedValuesEntity;
import static com.ericsson.vnfm.orchestrator.utils.Utility.UNEXPECTED_EXCEPTION_OCCURRED;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertClusterConfigToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.getProblemDetails;
import static com.ericsson.vnfm.orchestrator.utils.Utility.setManageElementIdIfNotPresent;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ClusterConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public class UtilityTest {

    @Test
    public void testUpdateCombinedValuesEntityBothEmptySuccess() {
        VnfInstance tempInstance = createVnfInstance(true);
        updateCombinedValuesEntity(tempInstance, new HashMap<>(), new HashMap<>());
        assertThat(tempInstance).isNotNull();
        assertThat(tempInstance.getCombinedAdditionalParams()).isNull();
        assertThat(tempInstance.getCombinedValuesFile()).isNull();
    }

    @Test
    public void testUpdateCombinedValuesEntityNotEmptyCombinedParamsSuccess() {
        VnfInstance tempInstance = createVnfInstance(true);
        Map<String, Object> valuesYamlMap = Map.of("test_key", "test_value");
        Map<String, Object> additionalParams = Map.of("test_params", "test_params");
        updateCombinedValuesEntity(tempInstance, valuesYamlMap, additionalParams);
        assertThat(tempInstance.getCombinedValuesFile()).isEqualTo(convertObjToJsonString(valuesYamlMap));
        assertThat(tempInstance.getCombinedAdditionalParams()).isEqualTo(convertObjToJsonString(additionalParams));
    }

    @Test
    public void testUpdateCombinedValuesEntityWithValueSuccess() {
        VnfInstance tempInstance = createVnfInstance(true);
        Map<String, Object> valuesYamlMap = Map.of("test_key", "test_value");
        updateCombinedValuesEntity(tempInstance, valuesYamlMap, new HashMap<>());
        assertThat(tempInstance.getCombinedValuesFile()).isEqualTo(convertObjToJsonString(valuesYamlMap));
        assertThat(tempInstance.getCombinedAdditionalParams()).isNull();
    }

    @Test
    public void testUpdateCombinedValuesEntityWithAdditionalParamsSuccess() {
        VnfInstance tempInstance = createVnfInstance(true);
        Map<String, Object> additionalParams = Map.of("test_params", "test_params");
        updateCombinedValuesEntity(tempInstance, new HashMap<>(), additionalParams);
        assertThat(tempInstance.getCombinedAdditionalParams()).isEqualTo(convertObjToJsonString(additionalParams));
        assertThat(tempInstance.getCombinedValuesFile()).isNull();
    }

    @Test
    public void testSetManagedElementIdIfManageElementIdNotPresent() {
        VnfInstance tempInstance = createVnfInstance(false);
        Map<String, Object> ossParams = new HashMap<>();
        ossParams.put("networkElementId", "nElementId");
        setManageElementIdIfNotPresent(ossParams, tempInstance);
        assertThat(ossParams.get("managedElementId")).isEqualTo("nElementId");
    }

    @Test
    public void testSetManagedElementIdIfNetworkElementIdNotPresent() {
        VnfInstance tempInstance = createVnfInstance(false);
        Map<String, Object> ossParams = new HashMap<>();
        ossParams.put("managedElementId", "mElementId");
        setManageElementIdIfNotPresent(ossParams, tempInstance);
        assertThat(ossParams.get("managedElementId")).isEqualTo("mElementId");
    }

    @Test
    public void testSetManagedElementIdIfBothElementIdPresent() {
        VnfInstance tempInstance = createVnfInstance(false);
        Map<String, Object> ossParams = new HashMap<>();
        ossParams.put("managedElementId", "mElementId");
        ossParams.put("networkElementId", "nElementId");
        setManageElementIdIfNotPresent(ossParams, tempInstance);
        assertThat(ossParams.get("managedElementId")).isEqualTo("nElementId");
    }

    @Test
    public void testGetProblemDetailsWhenErrorInJsonFormat() {
        String error = "{\"detail\":\"Release \\\"eric-sec-certm-crd\\\" does not exist\",\"status\":\"422 UNPROCESSABLE_ENTITY\"}";
        var actual = getProblemDetails(error);
        var expected = buildProblemDetails(UNPROCESSABLE_ENTITY.value(), UNPROCESSABLE_ENTITY.getReasonPhrase(),
                "Release \"eric-sec-certm-crd\" does not exist");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetProblemDetailsWhenErrorInStringFormat() {
        String error = "The lifecycle operation on the resource timed out. It may complete in the background on the cluster. " +
                "You can clean up the resource on the UI";
        var errorObj = new ErrorParser(error);
        var actual = errorObj.getDetails();

        assertThat(actual).isEqualTo(error);
    }

    @Test
    public void testGetProblemDetailsWhenJSONObjectCreationFailed() {
        String error = "400 : \"{\"errorDetails\":[{\"parameterName\":\"namespace\",\"message\":\"namespace cannot be null\"}]}\"400" +
                ": \"{\"errorDetails\":[{\"parameterName\":\"namespace\",\"message\":\"namespace cannot be null\"}]}\"";
        var actual = getProblemDetails(error);
        var expected = buildProblemDetails(BAD_REQUEST.value(), BAD_REQUEST.getReasonPhrase(), "[{\"parameterName\":\"namespace\","
                + "\"message\":\"namespace cannot be null\"}]");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetProblemDetailsWhenErrorHasInvalidFormat() {
        String error = "INVALID ERROR FORMAT}";
        var actual = getProblemDetails(error);
        var expected = buildProblemDetails(BAD_REQUEST.value(), BAD_REQUEST.getReasonPhrase(), error);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetProblemDetailsWhenJsonObjectDetailNotString() {
        String error = "{\"detail\":1,\"status\":\"422 UNPROCESSABLE_ENTITY\"}";
        var actual = getProblemDetails(error);
        var expected = buildProblemDetails(UNPROCESSABLE_ENTITY.value(), UNPROCESSABLE_ENTITY.getReasonPhrase(), UNEXPECTED_EXCEPTION_OCCURRED);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetProblemDetailsWhenJsonObjectTitleNotNull() {
        String error = "{\"detail\":\"Release \\\"eric-sec-certm-crd\\\" does not exist\",\"status\":\"422 UNPROCESSABLE_ENTITY\",\"title\":\"TITLE\"}";
        var actual = getProblemDetails(error);
        var expected = buildProblemDetails(UNPROCESSABLE_ENTITY.value(), "TITLE", "Release \"eric-sec-certm-crd\" does not exist");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testConvertClusterConfigToMap() {
        final ClusterConfig clusterConfig = new ClusterConfig()
                .kind("kind")
                .apiVersion("apiVersion")
                .preferences("preferences")
                .clusters(List.of("clusters"))
                .contexts(List.of("contexts"))
                .currentContext("currentContext")
                .users(List.of("users"));
        final Map<String, Object> expected = new HashMap<>(Map.of("apiVersion", "apiVersion",
                                                                  "clusters", List.of("clusters"),
                                                                  "contexts", List.of("contexts"),
                                                                  "current-context", "currentContext",
                                                                  "kind", "kind",
                                                                  "preferences", "preferences",
                                                                  "users", List.of("users")));

        assertThat(expected).isEqualTo(convertClusterConfigToMap(clusterConfig));
    }

    @Test
    public void testShouldReturnAnEmptyHashMapIfConvertClusterConfigIsNull() {
        assertThat(new HashMap<>()).isEqualTo(convertClusterConfigToMap(null));
    }

    private static ProblemDetails buildProblemDetails(int status, String title, String detail) {
        return new ProblemDetails()
                .status(status)
                .type(URI.create(TYPE_BLANK))
                .title(title)
                .detail(detail)
                .instance(URI.create(TYPE_BLANK));
    }

    private Path getResource(final String fileName) {
        return TestUtils.getResource(getClass(), fileName);
    }
}
