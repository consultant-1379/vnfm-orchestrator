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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.vnfm.orchestrator.TestUtils.ADDITIONAL_PARAMS_FIELD;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyLifecycleOperation;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder.JSON_REQUEST_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logLifecycleOperationRequest;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logVnfInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.McioInfo;
import com.ericsson.vnfm.orchestrator.model.OwnerReference;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoDeploymentStatefulSet;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public class LoggingUtilsTest {

    private static final String LIFECYCLE_OPERATION_DAY0_CONFIGURATION_EXPOSED_MESSAGE =
            "Logs of LifecycleOperation contains forbidden day0.configuration params";
    private static final String SECRET_LOGIN_TEST_VALUE = "supersecretlogin";
    private static final String SECRET_PASSWORD_TEST_VALUE = "supersecretvalue";
    private static final String NON_DAY0_SENSITIVE_DATA = "customParamPassword";

    @Test
    public void testLogsOfLifecycleOperationNotContainsDay0ConfigurationValues() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);

        String loggedLifecycleOperation = LoggingUtils.logLifecycleOperation(lifecycleOperation);

        assertNotContainsDay0ConfigValues(loggedLifecycleOperation);
    }

    @Test
    public void testLoggingOfLifecycleOperationHandlesNullAdditionalParams() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON);

        String loggedLifecycleOperation = LoggingUtils.logLifecycleOperation(lifecycleOperation);

        assertTrue(loggedLifecycleOperation.contains(ADDITIONAL_PARAMS_FIELD));
    }

    @Test
    public void testLogsOfLifecycleOperationListNotContainsDay0ConfigurationValues() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);
        List<LifecycleOperation> lifecycleOperations = new ArrayList<>(2);
        lifecycleOperations.add(0, lifecycleOperation);
        lifecycleOperations.add(1, lifecycleOperation);

        String loggedLifecycleOperationList = LoggingUtils.logLifecycleOperations(lifecycleOperations);

        assertNotContainsDay0ConfigValues(loggedLifecycleOperationList);
    }

    @Test
    public void testLogsOfVnfInstanceNotContainsDay0ConfigurationValues() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        String loggedVnfInstance = logVnfInstance(vnfInstance);

        assertNotContainsDay0ConfigValues(loggedVnfInstance);
    }

    @Test
    public void testLogsOfVnfInstanceNotContainsDay0ConfigurationValuesInAllOperations() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);
        vnfInstance.setAllOperations(List.of(lifecycleOperation, lifecycleOperation));

        String loggedVnfInstance = logVnfInstance(vnfInstance);

        assertNotContainsDay0ConfigValues(loggedVnfInstance);
    }

    @Test
    public void testLogsOfOperationRequestsNotContainsDay0ConfigurationValues() {
        Map<String, Object> additionalParams = createDummyDay0ConfigurationParams();
        InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        TerminateVnfRequest terminateVnfRequest = new TerminateVnfRequest();
        terminateVnfRequest.setAdditionalParams(additionalParams);
        ScaleVnfRequest scaleVnfRequest = new ScaleVnfRequest();
        scaleVnfRequest.setAdditionalParams(additionalParams);
        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = new ChangeCurrentVnfPkgRequest();
        changePackageInfoVnfRequest.setAdditionalParams(additionalParams);
        List<Object> assortedOperationRequests = new ArrayList<>(4);
        assortedOperationRequests.add(instantiateVnfRequest);
        assortedOperationRequests.add(terminateVnfRequest);
        assortedOperationRequests.add(scaleVnfRequest);
        assortedOperationRequests.add(changePackageInfoVnfRequest);

        StringBuilder stringBuilder = new StringBuilder();
        assortedOperationRequests.forEach(r -> stringBuilder.append(logLifecycleOperationRequest(r)).append(", "));
        String loggedOperationRequests = stringBuilder.toString();

        assertFalse(loggedOperationRequests.isEmpty());
        assertNotContainsDay0ConfigValues(loggedOperationRequests);
    }

    @Test
    public void testLogsOfWfsInstantiateRequestNotContainsDay0ConfigurationValues() {
        Map<String, Object> day0Configuration = createDummyDay0ConfigurationParams();
        Object dummyRequest = createDummyWfsRequest();

        String loggableWfsRequest = LoggingUtils.logWfsRequestWithDay0Configuration(dummyRequest, day0Configuration);

        assertFalse(loggableWfsRequest.contains(SECRET_PASSWORD_TEST_VALUE),
                "Sensitive data exposed for logging: " + loggableWfsRequest);
    }

    @Test
    public void testLogsOfHelmChartsNotContainsDay0ConfigurationValues() {
        HelmChart helmChart = new HelmChart();
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);
        vnfInstance.setAllOperations(Collections.singletonList(createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON)));
        helmChart.setVnfInstance(vnfInstance);
        List<HelmChart> helmCharts = new ArrayList<>(2);
        helmCharts.add(helmChart);
        helmCharts.add(helmChart);
        vnfInstance.setHelmCharts(helmCharts);

        String loggableHelmCharts = LoggingUtils.logHelmCharts(helmCharts);

        assertFalse(loggableHelmCharts.isEmpty());
        assertNotContainsDay0ConfigValues(loggableHelmCharts);
    }

    @Test
    public void testLogsOfAdditionalParametersNotContainsSensitiveData() {
        Map<String, Object> additionalParams = createDummyDay0ConfigurationParams();
        additionalParams.put("not.day0.param.password", NON_DAY0_SENSITIVE_DATA);

        String loggableAdditionalParams = LoggingUtils.logAdditionalParameters(additionalParams);

        assertFalse(loggableAdditionalParams.contains(NON_DAY0_SENSITIVE_DATA),
                "Sensitive non-day0 data exposed for logging: " + loggableAdditionalParams);
        assertNotContainsDay0ConfigValues(loggableAdditionalParams);
    }

    @Test
    public void testMultiValueMapRemovesSenisitiveDay0Config() {
        String requestBody = readDataFromFile(getClass(), "logging-instantiate-vnf-wfs-request.json");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(JSON_REQUEST_PARAMETER_NAME, requestBody);
        String loggableAdditionalParams = LoggingUtils.logMultiValueMap(body);
        assertFalse(loggableAdditionalParams.contains(SECRET_PASSWORD_TEST_VALUE));
        assertTrue(loggableAdditionalParams.contains("namespace"));
    }

    @Test
    public void testlogComponentStatusResponse() {
        ComponentStatusResponse statusResponse = buildMockedComponentStatusResponse();

        String loggedComponentStatusResponse = LoggingUtils.logComponentStatusResponse(statusResponse);

        assertTrue(loggedComponentStatusResponse.contains("Pods size 1"));
        assertTrue(loggedComponentStatusResponse.contains("availableReplicas=2"));
        assertTrue(loggedComponentStatusResponse.contains("replicas=3"));
        assertTrue(loggedComponentStatusResponse.contains("availableReplicas=1"));
        assertTrue(loggedComponentStatusResponse.contains("replicas=1"));
        assertFalse(loggedComponentStatusResponse.contains("eric-am-onboarding-service-85748b467-tg2vf"));
    }

    private ComponentStatusResponse buildMockedComponentStatusResponse() {
        final List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setClusterName("hart070");
        componentStatusResponse.setReleaseName("release-ns");
        componentStatusResponse.setPods(pods);
        VimLevelAdditionalResourceInfoDeploymentStatefulSet deployment =
                buildVimLevelAdditionalResourceInfoDeploymentStatefulSet(McioInfo.McioTypeEnum.DEPLOYMENT.toString());
        deployment.setReplicas(2);
        deployment.setAvailableReplicas(1);
        componentStatusResponse.setDeployments(List.of(deployment));
        VimLevelAdditionalResourceInfoDeploymentStatefulSet statefulSet =
                buildVimLevelAdditionalResourceInfoDeploymentStatefulSet(McioInfo.McioTypeEnum.STATEFULSET.toString());
        deployment.setReplicas(3);
        deployment.setAvailableReplicas(2);
        componentStatusResponse.setStatefulSets(List.of(statefulSet));
        return componentStatusResponse;
    }

    private List<VimLevelAdditionalResourceInfo> buildVimLevelAdditionalResourceInfoList() {
        VimLevelAdditionalResourceInfo vimLevelAdditionalResourceInfo = new VimLevelAdditionalResourceInfo()
                .uid("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .name("eric-am-onboarding-service-85748b467-tg2vf")
                .status("Running")
                .namespace("unit-testing-ns")
                .hostname("hostname")
                .ownerReferences(List.of(new OwnerReference()));
        return List.of(vimLevelAdditionalResourceInfo);
    }

    private VimLevelAdditionalResourceInfoDeploymentStatefulSet buildVimLevelAdditionalResourceInfoDeploymentStatefulSet(String kind) {
        return new VimLevelAdditionalResourceInfoDeploymentStatefulSet()
                .uid("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .name("eric-pm-server")
                .kind(kind)
                .status("Running")
                .namespace("unit-testing-ns")
                .replicas(1)
                .availableReplicas(1)
                .ownerReferences(List.of(new OwnerReference().uid("c0734032ad4c")));
    }

    private static Map<String, Object> createDummyDay0ConfigurationParams() {
        Map<String, Object> day0Configuration = new HashMap<>();
        day0Configuration.put("day0.configuration.param1.value", SECRET_PASSWORD_TEST_VALUE);
        day0Configuration.put("day0.configuration.param1.key", "password");
        day0Configuration.put("day0.configuration.secretname", "secret");
        return day0Configuration;
    }

    private static void assertNotContainsDay0ConfigValues(final String loggedObject) {
        boolean containsDay0ConfigValues =
                loggedObject.contains(SECRET_LOGIN_TEST_VALUE) || loggedObject.contains(SECRET_PASSWORD_TEST_VALUE);
        assertFalse(containsDay0ConfigValues, LIFECYCLE_OPERATION_DAY0_CONFIGURATION_EXPOSED_MESSAGE);
    }

    private static Object createDummyWfsRequest() {
        return new Object() {
            Map<String, Object> day0Configuration = createDummyDay0ConfigurationParams();
        };
    }
}