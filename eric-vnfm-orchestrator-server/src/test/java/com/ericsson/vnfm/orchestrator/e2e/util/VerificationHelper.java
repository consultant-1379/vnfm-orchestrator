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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.POST;

import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.loadYamlToMap;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_SCOPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.readFileContent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestComponent
public class VerificationHelper {

    private static ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private ScaleInfoRepository scaleInfoRepository;
    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;
    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;
    @Autowired
    private HelmChartRepository helmChartRepository;
    @Autowired
    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @SuppressWarnings("unchecked")
    public static void checkDefaultExtensionsAreSet(final VnfInstance instance) {
        // Check that default extensions set correctly
        Map<String, Object> extensions =
                (Map<String, Object>) convertStringToJSONObj(instance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions.get(PAYLOAD)).isEqualTo(MANUAL_CONTROLLED); //PL & CL
        assertThat(extensions.get(PAYLOAD_2)).isEqualTo(CISM_CONTROLLED); //TL
        assertThat(extensions.get(PAYLOAD_3)).isEqualTo(MANUAL_CONTROLLED); //JL
    }

    @SuppressWarnings("unchecked")
    public static void verifyValueInMap(String mapAsString, String key, String expectedValue) {
        assertThat(mapAsString).isNotEmpty();
        Map<String, Object> additionalParams = parseJson(mapAsString, HashMap.class);
        Object helmNoHooks = additionalParams.getOrDefault(key, "value not found");
        if (expectedValue == null) {
            expectedValue = "value not found";
        }
        assertThat(helmNoHooks.toString()).isEqualTo(expectedValue);
    }

    public static void verifyMapContainsKey(String mapAsString, String key) {
        assertThat(mapAsString).isNotEmpty();
        Map<String, Object> additionalParams = parseJson(mapAsString, HashMap.class);
        assertThat(additionalParams).containsKey(key);
    }

    public static void verifyMapDoesNotContainKey(String mapAsString, String key) {
        assertThat(mapAsString).isNotEmpty();
        Map<String, Object> additionalParams = parseJson(mapAsString, HashMap.class);
        assertThat(additionalParams).doesNotContainKey(key);
    }

    public static void verifyValuesFilePassedToWfs(RestTemplate restTemplate, int wantedNumberOfTimes, String urlEnding, String paramName,
                                               boolean shouldContain) throws IOException {
        final ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(wantedNumberOfTimes)).exchange(endsWith(urlEnding), eq(POST), requestCaptor.capture(),
                                                                  ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity httpEntity = requestCaptor.getValue();
        Map<String, Object> requestBody = (Map<String, Object>) httpEntity.getBody();

        verifyValuesFileForContainingParam(requestBody, paramName, shouldContain);
    }

    private static void verifyValuesFileForContainingParam(Map<String, Object> requestBody, String paramName, boolean shouldContain) throws IOException {
        FileSystemResource values = (FileSystemResource) (getRequestPart(requestBody, "values")).get(0);
        Path filePath = values.getFile().toPath();
        String result = readFileContent(filePath);
        if (!result.isEmpty()) {
            Map<String, Object> valuesMap = loadYamlToMap(filePath);
            if (shouldContain) {
                assertThat(valuesMap).containsKey(paramName);
            } else {
                assertThat(valuesMap).doesNotContainKey(paramName);
            }
        }
    }

    private static ArrayList getRequestPart(Map<String, Object> requestBody, String requestPart) {
        return (ArrayList) requestBody.get(requestPart);
    }

    public static void checkChartDownsizeState(VnfInstance vnfInstance, String releaseName, String expectedState, final boolean checkTempInstance) {
        VnfInstance instance = checkTempInstance ? parseJson(vnfInstance.getTempInstance(), VnfInstance.class) : vnfInstance;
        Optional<HelmChart> helmChart = instance.getHelmCharts()
                .stream()
                .filter(chart -> StringUtils.equals(releaseName, chart.getReleaseName()))
                .findFirst();
        if (helmChart.isPresent()) {
            assertThat(helmChart.get().getDownsizeState()).isEqualTo(expectedState);
        } else {
            fail(String.format("No helm chart with the release name '%s' found in vnfInstance '%s'", releaseName, vnfInstance.getVnfInstanceName()));
        }
    }

    public static void checkLevelsReplicaDetailsNull(final VnfInstance instance) {
        //Check levels and replica details are not set
        assertThat(instance.getInstantiationLevel()).isNull();
        List<HelmChart> charts = instance.getHelmCharts();
        assertThat(charts).extracting(HelmChartBaseEntity::getReplicaDetails).containsOnly(null, null);
    }

    public void verifyNoEvnfmParamsPassedToWfs(RestTemplate restTemplate, int wantedNumberOfTimes, String urlEnding) throws IOException {
        final var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(wantedNumberOfTimes)).exchange(endsWith(urlEnding), eq(POST), requestCaptor.capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        var httpEntity = requestCaptor.getValue();
        assertThatNoEvnfmParamsPassedToWfs(httpEntity);
    }

    public static void assertThatNoEvnfmParamsPassedToWfs(HttpEntity request) throws IOException {
        Map<String, Object> requestBody = (Map<String, Object>) request.getBody();
        verifyJsonContentForNoEvnfmParams(requestBody);
        verifyValuesFileForNoEvnfmParams(requestBody);
    }

    private static void verifyJsonContentForNoEvnfmParams(Map<String, Object> requestBody) throws JsonProcessingException {
        var json = getRequestPart(requestBody, "json").get(0);
        var additionalParams = mapper.readTree((String) json).get("additionalParams");

        if (!additionalParams.isNull()) {
            EVNFM_PARAMS.forEach(param -> assertThat(additionalParams.has(param)).isFalse());
        }
    }

    private static void verifyValuesFileForNoEvnfmParams(Map<String, Object> requestBody) throws IOException {
        FileSystemResource values = (FileSystemResource) (getRequestPart(requestBody, "values")).get(0);
        Path filePath = values.getFile().toPath();
        String result = readFileContent(filePath);
        if (!result.isEmpty()) {
            Map<String, Object> valuesMap = loadYamlToMap(filePath);
            valuesMap.keySet().forEach(key -> assertThat(EVNFM_PARAMS.contains(key)).isFalse());
        }
    }

    public void checkScaleLevel(final VnfInstance vnfInstance, final int firstExpectedValue, final int secondExpectedValue) {
        List<ScaleInfoEntity> preScaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(vnfInstance);
        assertThat(preScaleInfoEntities).extracting("scaleLevel").containsExactlyInAnyOrder(firstExpectedValue, secondExpectedValue);
    }

    public void checkOperationValuesAndHistoryRecordsSet(String lifeCycleOperationId) {
        // Operation has been updated wih values as is successful
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfInstance.getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(vnfInstance.getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(vnfInstance.getCombinedValuesFile());
        // HelmChartHistoryRecords were stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifeCycleOperationId).size())
                .isEqualTo(vnfInstance.getHelmCharts().size());
    }

    public void checkOperationValuesAndHistoryRecordsNotSet(String lifeCycleOperationId) {
        // Operation has not been updated wih values as is not successful
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getCombinedValuesFile()).isNull();
        // HelmChartHistoryRecords were not stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifeCycleOperationId).size())
                .isEqualTo(0);
    }

    public VnfInstance verifyOperationAndModel(final VnfInstanceResponse vnfInstanceResponse,
                                               final String lifeCycleOperationId,
                                               final LifecycleOperationType type,
                                               final InstantiationState state) {

        final VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(type);
        assertThat(instance.getInstantiationState()).isEqualTo(state);
        assertThat(helmChartRepository.findByVnfInstance(instance))
                .extracting(HelmChartBaseEntity::getState)
                .containsOnly(HelmReleaseState.COMPLETED.toString());
        assertThat(instance.getCombinedValuesFile()).isNotEmpty();
        assertThat(instance.getTempInstance()).isNull();

        return instance;
    }

    public void verifyHealAdditionalParams(final String lifeCycleOperationId) {
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        Map<String, Object> healAdditionalParams = MessageUtility.getAdditionalParams(operation);
        assertThat(healAdditionalParams).containsKeys(RESTORE_BACKUP_NAME,
                RESTORE_SCOPE,
                DAY0_CONFIGURATION_PREFIX);
    }

    @SuppressWarnings("unchecked")
    public void checkComplexTypesProperlyMapped(MvcResult requestResult,
                                                VnfInstanceResponse vnfInstanceResponse) throws UnsupportedEncodingException {

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        Map<String, Object> expectedContent = TestUtils.getComplexTypeAdditionalParams();
        Map<String, Object> requestContent = (Map<String, Object>) parseJson(requestResult.getRequest().getContentAsString(), Map.class)
                .getOrDefault("additionalParams", Collections.emptyMap());
        Map<String, Object> combinedValuesFile = parseJson(instance.getCombinedValuesFile(), Map.class);

        assertThat(requestContent).containsAllEntriesOf(expectedContent);
        assertThat(combinedValuesFile).containsAllEntriesOf(expectedContent);
    }

    public void checkBroEndpointUrl(VnfInstanceResponse vnfInstanceResponse, String broUrl) {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getBroEndpointUrl()).isEqualTo(broUrl);
    }
}
