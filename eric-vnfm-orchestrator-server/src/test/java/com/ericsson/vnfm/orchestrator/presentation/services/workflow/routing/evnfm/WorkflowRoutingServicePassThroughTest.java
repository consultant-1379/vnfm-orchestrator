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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_RELEASE_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChart;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DEFAULT_CRD_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder.JSON_REQUEST_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder.VALUES_REQUEST_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.utils.Utility.copyParametersMap;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.NamespaceValidationResponse;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.AdditionalArtifactModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DeleteNamespaceException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HelmVersionsException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ScaleRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.TerminateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowRoutingServicePassThroughTest extends AbstractDbSetupTest {

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    @Autowired
    private ScaleRequestHandler scaleRequestHandler;

    @Autowired
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @Autowired
    private InstantiateRequestHandler instantiateRequestHandler;

    @Autowired
    private TerminateRequestHandler terminateRequestHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LcmOperationsConfig lcmOperationsConfig;

    @MockBean
    private HelmClientVersionValidator helmClientVersionValidator;

    @SpyBean
    private PackageServiceImpl packageService;

    @MockBean
    private OnboardingClient onboardingClient;

    @Autowired
    private OnboardingUriProvider onboardingUriProvider;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @SpyBean
    private ExtensionsMapper extensionsMapper;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap<String, Object>>> evnfmWorkflowScaleRequest;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap<String, Object>>> evnfmWorkFlowInstantiateRequestWithValues;

    @Captor
    private ArgumentCaptor<HttpEntity<EvnfmWorkFlowInstantiateRequest>> evnfmWorkFlowInstantiateRequest;

    @Captor
    private ArgumentCaptor<HttpEntity<EvnfmWorkFlowUpgradeRequest>> evnfmWorkFlowUpgradeRequest;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap>> requestWithClusterConfigFile;

    @Captor
    private ArgumentCaptor<String> terminateUrl;

    private static final Logger LOGGER = getLogger(WorkflowRoutingServicePassThroughTest.class);
    private static final String PATH_ARTIFACTS_APPENDIX = "Definitions/OtherTemplates/";
    private static final String DESIRE_ARTIFACT_CONTENT = "test: content";

    @BeforeEach
    public void setupOnboardingClient() {
        Mockito.when(onboardingClient.get(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{new PackageResponse()}));
        lcmOperationsConfig.setLcmOperationsLimit(Integer.MAX_VALUE);
    }

    @Test
    public void testClusterIsRetrievedFromInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setClusterName("my-cluster");
        TerminateVnfRequest request = new TerminateVnfRequest();
        Map<String, Object> additionalParams = copyParametersMap(request.getAdditionalParams());
        EvnfmWorkFlowTerminateRequest terminateRequest =
                new EvnfmWorkFlowTerminateRequest.EvnfmWorkFlowTerminateBuilder(
                        additionalParams).withClusterName(instance.getClusterName()).build();
        Map<String, Object> additionalParamsMap = terminateRequest.getAdditionalParams();
        assertThat(additionalParamsMap).containsEntry(CLUSTER_NAME, "my-cluster");
    }

    @Test
    public void testGlobalRegistrySetFalse() {
        EvnfmWorkFlowInstantiateRequest instantiateRequest = getEvnfmWorkFlowInstantiateRequest(false);
        assertThat(instantiateRequest.isOverrideGlobalRegistry()).isFalse();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getEvnfmWorkFlowUpgradeRequest(false);
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isFalse();
        EvnfmWorkflowScaleRequest evnfmWorkFlowScaleRequest = getEvnfmWorkFlowScaleRequest(false);
        assertThat(evnfmWorkFlowScaleRequest.isOverrideGlobalRegistry()).isFalse();
    }

    @Test
    public void testGlobalRegistrySetTrue() {
        EvnfmWorkFlowInstantiateRequest instantiateRequest = getEvnfmWorkFlowInstantiateRequest(true);
        assertThat(instantiateRequest.isOverrideGlobalRegistry()).isTrue();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getEvnfmWorkFlowUpgradeRequest(true);
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isTrue();
        EvnfmWorkflowScaleRequest evnfmWorkFlowScaleRequest = getEvnfmWorkFlowScaleRequest(true);
        assertThat(evnfmWorkFlowScaleRequest.isOverrideGlobalRegistry()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackTakingParameters() {
        ChangeCurrentVnfPkgRequest changePackageRequest = new ChangeCurrentVnfPkgRequest();
        changePackageRequest.setVnfdId("test");
        Map<String, Object> additionalParameter = createAdditionalParameter();
        changePackageRequest.setAdditionalParams(additionalParameter);
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        LOGGER.error("instance vals: " + instance.getCombinedAdditionalParams());
        LOGGER.error("instance vals: " + instance.getCombinedValuesFile());
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(instance,
                                                                                       changePackageRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG,
                                                                                       null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operation);
        EvnfmWorkFlowRollbackRequest rollbackRequest = WorkflowRoutingServicePassThrough
                .createRollbackRequestBody(instance, operation, "7");
        LOGGER.error("rollback: " + rollbackRequest.isSkipVerification());
        assertThat(rollbackRequest.getClusterName()).isNull();
        assertThat(rollbackRequest.getNamespace()).isNull();
        assertThat(rollbackRequest.getLifecycleOperationId()).isEqualTo(operation.getOperationOccurrenceId());
        assertThat(rollbackRequest.isSkipVerification()).isTrue();
        assertThat(rollbackRequest.getApplicationTimeOut()).isEqualTo("79");
        assertThat(rollbackRequest.getRevisionNumber()).isEqualTo("7");
        assertThat(rollbackRequest.isSkipJobVerification()).isTrue();
        assertThat(rollbackRequest.getHelmClientVersion()).isNull();
    }

    @Test
    public void testRollbackWillNoAdditionalParams() {
        ChangeCurrentVnfPkgRequest changePackageRequest = new ChangeCurrentVnfPkgRequest();
        changePackageRequest.setVnfdId("test");
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(instance, changePackageRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operation);
        EvnfmWorkFlowRollbackRequest rollbackRequest = WorkflowRoutingServicePassThrough
                .createRollbackRequestBody(instance, operation, "7");
        assertThat(rollbackRequest.getClusterName()).isNull();
        assertThat(rollbackRequest.getNamespace()).isNull();
        assertThat(rollbackRequest.getLifecycleOperationId()).isEqualTo(operation.getOperationOccurrenceId());
        assertThat(rollbackRequest.isSkipVerification()).isFalse();
        assertThat(rollbackRequest.getApplicationTimeOut()).isEqualTo("79");
        assertThat(rollbackRequest.getRevisionNumber()).isEqualTo("7");
        assertThat(rollbackRequest.isSkipJobVerification()).isFalse();
        assertThat(rollbackRequest.getHelmClientVersion()).isNull();
    }

    @Test
    public void testScale() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        HelmChart helmChartCrd = getHelmChart("chart-url-1", "scale-chart-1", instance, 1, HelmChartType.CRD);
        HelmChart helmChartCnf = getHelmChart("chart-url-2", "scale-chart-2", instance, 2, HelmChartType.CNF);
        instance.setHelmCharts(Arrays.asList(helmChartCrd, helmChartCnf));
        instance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv" +
                                     ".ScalingAspects\",\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\"" +
                                     ",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 " +
                                     "instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"" +
                                     "delta_1\",\"delta_2\",\"delta_3\"],\"allScalingAspectDelta\":{\"Payload_S" +
                                     "calingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\"," +
                                     "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_ins" +
                                     "tances\":4},\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_ins" +
                                     "tances\":7}}},\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta" +
                                     "\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\"," +
                                     "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":" +
                                     "[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"" +
                                     "tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number" +
                                     "_of_instances\":1}},\"targets\":[\"CL_scaled_vm\"]}}}}}}}}},\"allInitialDelta\":" +
                                     "{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"pro" +
                                     "perties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__sc" +
                                     "aled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.polici" +
                                     "es.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instanc" +
                                     "es\":1}},\"targets\":[\"CL_scaled_vm\"]}},\"allScalingAspectDelta\":{\"Payload_S" +
                                     "calingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"" +
                                     "properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instan" +
                                     "ces\":4},\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_instan" +
                                     "ces\":7}}},\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":" +
                                     "{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"pro" +
                                     "perties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__sca" +
                                     "led_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.policie" +
                                     "s.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instance" +
                                     "s\":1}},\"targets\":[\"CL_scaled_vm\"]}}}}}");
        helmChartCnf.setReplicaDetails("{\"PL__scaled_vm\":{\"currentReplicaCount\":3,\"scalingPara" +
                                               "meterName\":\"PL__scaled" +
                                               "_vm.replicaCount\",\"autoScalingEnabledParameterNam" +
                                               "e\":\"PL__scaled_vm.autoScal" +
                                               "ingEnabled\",\"autoScalingEnabledValue\":true,\"minRepl" +
                                               "icasParameterName\":\"PL" +
                                               "__scaled_vm.maxReplicas\",\"minReplicasCount\":1,\"max" +
                                               "ReplicasParameterName\":\"PL" +
                                               "__scaled_vm.minReplicas\",\"maxReplicasCount\":3},\"TL" +
                                               "_scaled_vm\":{\"currentRepl" +
                                               "icaCount\":3,\"scalingParameterName\":\"TL_scaled_vm.r" +
                                               "eplicaCount\"},\"CL__scal" +
                                               "ed_vm\":{\"currentReplicaCount\":3,\"scalingParameter" +
                                               "Name\":\"CL__scaled_vm.rep" +
                                               "licaCount\",\"autoScalingEnabledParameterName\":\"CL__" +
                                               "scaled_vm.autoScalingEnab" +
                                               "led\",\"autoScalingEnabledValue\":false,\"minReplicasP" +
                                               "arameterName\":\"CL__scal" +
                                               "ed_vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplica" +
                                               "sParameterName\":\"CL__sca" +
                                               "led_vm.minReplicas\",\"maxReplicasCount\":3}}");
        instance.setTempInstance(mapper.writeValueAsString(instance));
        ScaleVnfRequest scale = TestUtils.createScaleRequest("Payload", 3,
                                                             ScaleVnfRequest.TypeEnum.IN);
        vnfInstanceRepository.save(instance);
        LifecycleOperation operation = scaleRequestHandler
                .persistOperation(instance, scale, null, LifecycleOperationType.SCALE, null, "3600");
        workflowRoutingServicePassThrough.routeScaleRequest(instance, operation, scale);
        Mockito.verify(restTemplate).exchange(contains("scale-chart-2"), any(HttpMethod.class), evnfmWorkflowScaleRequest.capture(),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap<String, Object>> request = evnfmWorkflowScaleRequest.getValue();
        EvnfmWorkflowScaleRequest evnfmWorkflowScaleRequest = extractEvnfmWorkflowRequest(request, EvnfmWorkflowScaleRequest.class);
        Map additionalParameter = (Map) evnfmWorkflowScaleRequest.getAdditionalParams();
        assertThat(additionalParameter.size()).isEqualTo(2);
        assertThat(evnfmWorkflowScaleRequest.getChartUrl()).isEqualTo("chart-url-2");
        Map<String, Map<String, Integer>> allReplicaParameter = evnfmWorkflowScaleRequest.getScaleResources();
        assertThat(allReplicaParameter.size()).isEqualTo(3);
        for (Map.Entry<String, Map<String, Integer>> entry : allReplicaParameter.entrySet()) {
            if (entry.getKey().equals("PL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("PL__scaled_vm.replicaCount", 3))
                        .contains(Assertions.entry("PL__scaled_vm.minReplicas", 3))
                        .contains(Assertions.entry("PL__scaled_vm.maxReplicas", 1));
            } else if (entry.getKey().equals("CL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("CL__scaled_vm.replicaCount", 3));
            } else if (entry.getKey().equals("TL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("TL__scaled_vm.replicaCount", 3));
            }
        }
    }

    private <T> T extractEvnfmWorkflowRequest(HttpEntity<MultiValueMap<String, Object>> request, Class<T> targetClass) {
        MultiValueMap<String, Object> requestBody = request.getBody();
        List<Object> jsonParameterList = requestBody.get(JSON_REQUEST_PARAMETER_NAME);
        try {
            return objectMapper.readValue((String) jsonParameterList.get(0), targetClass);
        } catch (JsonProcessingException e) {
            fail("Request can`t be converted request={}", request);
            return null;
        }
    }

    @Test
    public void testScaleRequest() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setClusterName("cluster21");
        instance.setNamespace("namespace3");
        HelmChart chart = new HelmChart();
        chart.setHelmChartUrl("test-chart-2");
        chart.setReleaseName("test-chart-2");
        chart.setPriority(2);
        chart.setVnfInstance(instance);
        chart.setReplicaDetails("{\"PL__scaled_vm\":{\"currentReplicaCount\":3,\"scalingParameterName\":\"PL__scaled" +
                                        "_vm.replicaCount\",\"autoScalingEnabledParameterName\":\"PL__scaled_vm.autoScal" +
                                        "ingEnabled\",\"autoScalingEnabledValue\":true,\"minReplicasParameterName\":\"PL" +
                                        "__scaled_vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplicasParameterName\":\"PL" +
                                        "__scaled_vm.minReplicas\",\"maxReplicasCount\":3},\"TL_scaled_vm\":{\"currentRepl" +
                                        "icaCount\":3,\"scalingParameterName\":\"TL_scaled_vm.replicaCount\"},\"CL__scal" +
                                        "ed_vm\":{\"currentReplicaCount\":3,\"scalingParameterName\":\"CL__scaled_vm.rep" +
                                        "licaCount\",\"autoScalingEnabledParameterName\":\"CL__scaled_vm.autoScalingEnab" +
                                        "led\",\"autoScalingEnabledValue\":false,\"minReplicasParameterName\":\"CL__scal" +
                                        "ed_vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplicasParameterName\":\"CL__sca" +
                                        "led_vm.minReplicas\",\"maxReplicasCount\":3}}");
        instance.getHelmCharts().add(chart);
        instance.setResourceDetails("{\"PL__scaled_vm\": 1, \"CL_scaled_vm\": 1, \"TL_scaled_vm\": 1}");

        instance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv." +
                                     "ScalingAspects\",\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description" +
                                     "\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\\n\",\"" +
                                     "max_scale_level\":10,\"step_deltas\":[\"delta_1\",\"delta_2\",\"delta_3\"],\"allScalingAspectD" +
                                     "elta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\"" +
                                     ",\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},\"del" +
                                     "ta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_instances\":7}}},\"targets\":[\"PL__" +
                                     "scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.poli" +
                                     "cies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targe" +
                                     "ts\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.policies.nfv" +
                                     ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"" +
                                     "CL_scaled_vm\"]}}}}}}}}},\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies" +
                                     ".nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":" +
                                     "[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.policies.nfv.Vdu" +
                                     "InitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"CL_sc" +
                                     "aled_vm\"]}},\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies" +
                                     ".nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"num" +
                                     "ber_of_instances\":4},\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_instances\"" +
                                     ":7}}},\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta" +
                                     "\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of" +
                                     "_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type" +
                                     "\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\"" +
                                     ":1}},\"targets\":[\"CL_scaled_vm\"]}}}}}");
        ScaleInfoEntity scaleInfo = new ScaleInfoEntity();
        scaleInfo.setAspectId("Payload");
        scaleInfo.setScaleLevel(0);
        scaleInfo.setVnfInstance(instance);
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(scaleInfo);

        instance.setScaleInfoEntity(allScaleInfoEntity);

        instance.setCombinedAdditionalParams("{\"helmNoHooks\":\"true\"}");

        instance.setTempInstance(mapper.writeValueAsString(instance));
        instance = vnfInstanceRepository.save(instance);
        ScaleVnfRequest scale = TestUtils.createScaleRequest("Payload", 3,
                                                             ScaleVnfRequest.TypeEnum.OUT);
        LifecycleOperation operation = scaleRequestHandler
                .persistOperation(instance, scale, null, LifecycleOperationType.SCALE, null, "3600");
        operation.setOperationParams(mapper.writeValueAsString(scale));
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operation);

        workflowRoutingServicePassThrough.routeScaleRequest(2, operation, instance);
        Mockito.verify(restTemplate).exchange(contains("test-chart-2"), any(HttpMethod.class), requestWithClusterConfigFile.capture(),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkflowScaleRequest evnfmWorkflowScaleRequest = getRequestFromMultiValueMap(request, EvnfmWorkflowScaleRequest.class);
        Map additionalParameter = (Map) evnfmWorkflowScaleRequest.getAdditionalParams();
        assertThat(additionalParameter.size()).isEqualTo(3);
        assertThat(additionalParameter.get(HELM_NO_HOOKS)).isEqualTo("true");
        assertThat(evnfmWorkflowScaleRequest.getChartUrl()).isEqualTo("test-chart-2");
        Map<String, Map<String, Integer>> allReplicaParameter = evnfmWorkflowScaleRequest.getScaleResources();
        assertThat(allReplicaParameter.size()).isEqualTo(3);
        for (Map.Entry<String, Map<String, Integer>> entry : allReplicaParameter.entrySet()) {
            switch (entry.getKey()) {
                case "PL__scaled_vm":
                    assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("PL__scaled_vm.replicaCount", 3))
                            .contains(Assertions.entry("PL__scaled_vm.minReplicas", 3))
                            .contains(Assertions.entry("PL__scaled_vm.maxReplicas", 1));
                    break;
                case "CL__scaled_vm":
                    assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("CL__scaled_vm.replicaCount", 3));
                    break;
                case "TL__scaled_vm":
                    assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("TL__scaled_vm.replicaCount", 3));
                    break;
            }
        }

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testScaleWithSecondValues() throws JsonProcessingException {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        HelmChart helmChart = vnfInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";
        PackageResponse packageResponse = new PackageResponse();

        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));
        ScaleVnfRequest scale = TestUtils.createScaleRequest("Payload", 3,
                ScaleVnfRequest.TypeEnum.IN);
        LifecycleOperation operation = scaleRequestHandler
                .persistOperation(vnfInstance, scale, null, LifecycleOperationType.SCALE, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);
        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeScaleRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkflowScaleRequest.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkflowScaleRequest.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
    }

    @Test
    public void testScaleWithAdditionalValuesWhenArtifactNotMatch() throws JsonProcessingException {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.getHelmCharts().get(0).setHelmChartName("spider-a");
        PackageResponse packageResponse = new PackageResponse();

        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts("path1/spider-b.yaml", "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));
        ScaleVnfRequest scale = TestUtils.createScaleRequest("Payload", 3,
                ScaleVnfRequest.TypeEnum.IN);
        LifecycleOperation operation = scaleRequestHandler
                .persistOperation(vnfInstance, scale, null, LifecycleOperationType.SCALE, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfPackageId());
        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));

        workflowRoutingServicePassThrough.routeScaleRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkflowScaleRequest.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkflowScaleRequest.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNull();
    }

    @Test
    public void testScaleWithEvnfmSpecificParameter() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setNamespace("namespace1");
        instance.setClusterName("cluster1");
        instance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv" +
                                     ".ScalingAspects\",\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\"" +
                                     ",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 " +
                                     "instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"" +
                                     "delta_1\",\"delta_2\",\"delta_3\"],\"allScalingAspectDelta\":{\"Payload_S" +
                                     "calingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\"," +
                                     "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_ins" +
                                     "tances\":4},\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_ins" +
                                     "tances\":7}}},\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta" +
                                     "\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\"," +
                                     "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":" +
                                     "[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"" +
                                     "tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number" +
                                     "_of_instances\":1}},\"targets\":[\"CL_scaled_vm\"]}}}}}}}}},\"allInitialDelta\":" +
                                     "{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"pro" +
                                     "perties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__sc" +
                                     "aled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.polici" +
                                     "es.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instanc" +
                                     "es\":1}},\"targets\":[\"CL_scaled_vm\"]}},\"allScalingAspectDelta\":{\"Payload_S" +
                                     "calingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"" +
                                     "properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instan" +
                                     "ces\":4},\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_instan" +
                                     "ces\":7}}},\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":" +
                                     "{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"pro" +
                                     "perties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__sca" +
                                     "led_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta1\":{\"type\":\"tosca.policie" +
                                     "s.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instance" +
                                     "s\":1}},\"targets\":[\"CL_scaled_vm\"]}}}}}");
        instance.getHelmCharts().get(0).setReplicaDetails("{\"PL__scaled_vm\":{\"currentReplicaCount\":3,\"scalingPara" +
                                                                  "meterName\":\"PL__scaled" +
                                                                  "_vm.replicaCount\",\"autoScalingEnabledParameterNam" +
                                                                  "e\":\"PL__scaled_vm.autoScal" +
                                                                  "ingEnabled\",\"autoScalingEnabledValue\":true,\"minRepl" +
                                                                  "icasParameterName\":\"PL" +
                                                                  "__scaled_vm.maxReplicas\",\"minReplicasCount\":1,\"max" +
                                                                  "ReplicasParameterName\":\"PL" +
                                                                  "__scaled_vm.minReplicas\",\"maxReplicasCount\":3},\"TL" +
                                                                  "_scaled_vm\":{\"currentRepl" +
                                                                  "icaCount\":3,\"scalingParameterName\":\"TL_scaled_vm.r" +
                                                                  "eplicaCount\"},\"CL__scal" +
                                                                  "ed_vm\":{\"currentReplicaCount\":3,\"scalingParameter" +
                                                                  "Name\":\"CL__scaled_vm.rep" +
                                                                  "licaCount\",\"autoScalingEnabledParameterName\":\"CL__" +
                                                                  "scaled_vm.autoScalingEnab" +
                                                                  "led\",\"autoScalingEnabledValue\":false,\"minReplicasP" +
                                                                  "arameterName\":\"CL__scal" +
                                                                  "ed_vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplica" +
                                                                  "sParameterName\":\"CL__sca" +
                                                                  "led_vm.minReplicas\",\"maxReplicasCount\":3}}");
        instance.setCombinedAdditionalParams("{\"applicationTimeOut\":340," +
                                                     "\"overrideGlobalRegistry\":true," +
                                                     "\"helmNoHooks\":\"false\"}");
        instance = vnfInstanceRepository.save(instance);
        instance.setTempInstance(mapper.writeValueAsString(instance));
        ScaleVnfRequest scale = TestUtils.createScaleRequest("test", 3,
                                                             ScaleVnfRequest.TypeEnum.IN);
        Map<String, Object> additionalParameter = createAdditionalParameter();
        scale.setAdditionalParams(additionalParameter);
        LifecycleOperation operation = scaleRequestHandler
                .persistOperation(instance, scale, null, LifecycleOperationType.SCALE, null, "3600");
        workflowRoutingServicePassThrough.routeScaleRequest(instance, operation, scale);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile.capture(),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkflowScaleRequest evnfmWorkflowScaleRequest = getRequestFromMultiValueMap(request, EvnfmWorkflowScaleRequest.class);
        Map additionalParameterInWorkflowService = (Map) evnfmWorkflowScaleRequest.getAdditionalParams();
        assertThat(additionalParameterInWorkflowService.size()).isEqualTo(3);
        assertThat(additionalParameterInWorkflowService.get(HELM_NO_HOOKS)).isEqualTo("false");
        assertThat(evnfmWorkflowScaleRequest.getApplicationTimeOut()).isEqualTo("340");
        assertThat(evnfmWorkflowScaleRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowScaleRequest.getClusterName()).isEqualTo("cluster1");
        assertThat(evnfmWorkflowScaleRequest.getNamespace()).isEqualTo("namespace1");
        Map<String, Map<String, Integer>> allReplicaParameter = evnfmWorkflowScaleRequest.getScaleResources();
        assertThat(allReplicaParameter.size()).isEqualTo(3);
        for (Map.Entry<String, Map<String, Integer>> entry : allReplicaParameter.entrySet()) {
            if (entry.getKey().equals("PL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("PL__scaled_vm.replicaCount", 3))
                        .contains(Assertions.entry("PL__scaled_vm.minReplicas", 3))
                        .contains(Assertions.entry("PL__scaled_vm.maxReplicas", 1));
            } else if (entry.getKey().equals("CL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("CL__scaled_vm.replicaCount", 3));
            } else if (entry.getKey().equals("TL__scaled_vm")) {
                assertThat(entry.getValue()).isNotEmpty().contains(Assertions.entry("TL__scaled_vm.replicaCount", 3));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiate() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace1");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "test-secret");
        additionalParameter.put("day0.configuration.param1.key", "test-key");
        additionalParameter.put("day0.configuration.param1.value", "test-value1");
        additionalParameter.put("day0.configuration.param2.key", "key2");
        additionalParameter.put("day0.configuration.param2.value", "value2");

        Map<String, Map<String, Object>> secrets = prepareDay0ConfigurationSecrets();
        additionalParameter.put(DAY0_CONFIGURATION_SECRETS, secrets);

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE, new HashMap<>());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, new HashMap<>(), "3600");
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        vnfInstanceRepository.save(instance);

        //Check day 0 configuration
        Map<String, String> sensitiveInfoDetailsToMap = getSensitiveInfo(instance);
        verifyDay0Params(sensitiveInfoDetailsToMap);

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(14);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("340");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster1");
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo("namespace1");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        Map<String, String> day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(4);

        verifyDay0Params(day0Configuration);

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithOldSecretsFormat() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "test-secret");
        additionalParameter.put("day0.configuration.param1.key", "test-key");
        additionalParameter.put("day0.configuration.param1.value", "test-value1");
        additionalParameter.put("day0.configuration.param2.key", "key2");
        additionalParameter.put("day0.configuration.param2.value", "value2");

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE, new HashMap<>());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, new HashMap<>(), "3600");
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        vnfInstanceRepository.save(instance);

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(14);

        Map day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(1);

        //Check day 0 configuration
        Map<String, String> sensitiveInfoDetailsToMap = getSensitiveInfo(instance);
        assertThat(sensitiveInfoDetailsToMap.get("test-secret"))
                .isEqualTo("{\"key2\":\"value2\",\"test-key\":\"test-value1\"}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithDuplicatedSecretNameWithinOldSecretsFormat() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace1");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "secret1");
        additionalParameter.put("day0.configuration.param1.key", "test-key");
        additionalParameter.put("day0.configuration.param1.value", "test-value1");
        additionalParameter.put("day0.configuration.param2.key", "key2");
        additionalParameter.put("day0.configuration.param2.value", "value2");

        Map<String, Map<String, Object>> secrets = prepareDay0ConfigurationSecrets();
        additionalParameter.put(DAY0_CONFIGURATION_SECRETS, secrets);

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());

        assertThatThrownBy(() -> instantiateRequestHandler.formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE,
                                                                            new HashMap<>()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Duplicate secret name 'secret1' in Day0Configuration");
    }

    @Test
    public void testWorkflowInstantiateWithDuplicatedKeysWithinOldSecretsFormat() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace1");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "secret1");
        additionalParameter.put("day0.configuration.param1.key", "test-key");
        additionalParameter.put("day0.configuration.param1.value", "test-value1");
        additionalParameter.put("day0.configuration.param2.key", "test-key");
        additionalParameter.put("day0.configuration.param2.value", "value2");

        Map<String, Map<String, Object>> secrets = prepareDay0ConfigurationSecrets();
        additionalParameter.put(DAY0_CONFIGURATION_SECRETS, secrets);

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());

        assertThatThrownBy(() -> instantiateRequestHandler.formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE,
                                                                            new HashMap<>()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Duplicate keys within secret name 'secret1' in Day0Configuration");
    }

    @Test()
    public void testWorkflowInstantiateOldSecretsAtLeastOneKeyValuePair() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "day0-secret1");

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");

        assertThrows(InvalidInputException.class,
                () -> instantiateRequestHandler.formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE, new HashMap<>()));
    }

    @Test()
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateNewSecretsAtLeastOneKeyValuePair() throws Exception {
        String secretName = "secret1";

        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();

        //adding day0 configuration
        Map<String, Map<String, Object>> secrets = prepareDay0ConfigurationSecretsWithEmptyOneKeyValuePair();
        additionalParameter.put(DAY0_CONFIGURATION_SECRETS, secrets);

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");

        assertThrows(InvalidInputException.class,
                () -> instantiateRequestHandler.formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE, new HashMap<>()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithValuesFile() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setClusterName("cluster3");
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("clusterName", "cluster3");
        additionalParameter.put("namespace", "namespace1");

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster3",
                                                                               additionalParameter);
        Path values = TestUtils.getValuesFileCopy("valid_values.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(values);
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, valuesYamlMap, "3600");

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate, values);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class),
                                              evnfmWorkFlowInstantiateRequestWithValues.capture(), ArgumentMatchers
                                                      .<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap<String, Object>> request = evnfmWorkFlowInstantiateRequestWithValues.getValue();
        MultiValueMap<String, Object> body = request.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get(JSON_REQUEST_PARAMETER_NAME)).isNotNull();
        assertThat(body.get(VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = mapper.readValue(body.
                                                                                                   get(JSON_REQUEST_PARAMETER_NAME).get(0).toString(),
                                                                                           EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(1);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("340");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster3");
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo("namespace1");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        Map day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(0);

        assertThat(values.toFile().exists()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithValuesFile_additionalParameterSetToFalse_excluded() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setClusterName("cluster3");
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("clusterName", "cluster3");
        additionalParameter.put("namespace", "namespace1");
        additionalParameter.put("manoControlledScaling", false);

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster3",
                additionalParameter);
        Path values = TestUtils.getValuesFileCopy("valid_values.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(values);
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, valuesYamlMap, "3600");

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate, values);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class),
                evnfmWorkFlowInstantiateRequestWithValues.capture(), ArgumentMatchers
                        .<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap<String, Object>> request = evnfmWorkFlowInstantiateRequestWithValues.getValue();
        MultiValueMap<String, Object> body = request.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get(JSON_REQUEST_PARAMETER_NAME)).isNotNull();
        assertThat(body.get(VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = mapper.readValue(body.
                        get(JSON_REQUEST_PARAMETER_NAME).get(0).toString(),
                EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(1);
        assertThat(values.toFile().exists()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithNullValuesInRequest() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setClusterName("cluster3");
        instance.setTempInstance(mapper.writeValueAsString(instance));

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster3",
                                                                               null);
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));

        workflowRoutingServicePassThrough.routeInstantiateRequest(1, operation, instance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class),
                                              evnfmWorkFlowInstantiateRequestWithValues.capture(), ArgumentMatchers
                                                      .<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap<String, Object>> request = evnfmWorkFlowInstantiateRequestWithValues.getValue();

        MultiValueMap<String, Object> body = request.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get(JSON_REQUEST_PARAMETER_NAME)).isNotNull();
        assertThat(body.get(VALUES_REQUEST_PARAMETER_NAME)).isNull();

        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = mapper.readValue(body.
                                                                                                   get(JSON_REQUEST_PARAMETER_NAME).get(0).toString(),
                                                                                           EvnfmWorkFlowInstantiateRequest.class);
        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.STARTING.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWorkflowInstantiateForMultiChart() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setClusterName("cluster23");
        instance.setNamespace("namespace3");
        HelmChart chart = new HelmChart();
        chart.setHelmChartUrl("test-chart-2");
        chart.setReleaseName("test-chart-2");
        chart.setPriority(2);
        chart.setVnfInstance(instance);
        instance.getHelmCharts().add(chart);
        instance.setTempInstance(mapper.writeValueAsString(instance));

        instance = vnfInstanceRepository.save(instance);

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace3");

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster23",
                                                                               additionalParameter);
        Map additionalParams = (Map) instantiate.getAdditionalParams();
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        additionalParams.keySet().removeIf(parameter -> !EVNFM_PARAMS.contains(parameter));
        operation.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        operation.setOperationParams(mapper.writeValueAsString(instantiate));
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operation);

        workflowRoutingServicePassThrough.routeInstantiateRequest(2, operation, instance);
        Mockito.verify(restTemplate).exchange(contains("test-chart-2"), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(0);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test-chart-2");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("79");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster23");
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo("namespace3");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.STARTING.toString());

        Map day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(0);

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithInvalidValueForSkipVerification() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("skipVerification", "fsdd");
        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        final LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");

        assertThatThrownBy(() -> {
            workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The value [fsdd] is not a boolean");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithCrd() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        HelmChart helmChart = instance.getHelmCharts().get(0);
        helmChart.setHelmChartType(HelmChartType.CRD);
        helmChart.setHelmChartVersion("1.2.3");
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(helmChart);
        instance.setHelmCharts(helmCharts);
        instance.setCrdNamespace(DEFAULT_CRD_NAMESPACE);
        instance.setTempInstance(mapper.writeValueAsString(instance));
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace1");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "day0TestSecret");
        additionalParameter.put("day0.configuration.param1.key", "day0TestKey");
        additionalParameter.put("day0.configuration.param1.value", "day0TestValue");

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster1",
                                                                               additionalParameter);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(instance, instantiate, LifecycleOperationType.INSTANTIATE, new HashMap<>());

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, new HashMap<>(), "3600");

        instantiateRequestHandler.updateInstanceModel(instance, (Map<String, Object>) instantiate.getAdditionalParams());
        vnfInstanceRepository.save(instance);

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(15);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("340");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster1");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();
        //Check namespace and version for CRD
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo(DEFAULT_CRD_NAMESPACE);
        assertThat(evnfmWorkflowInstantiateRequest.getChartType()).isEqualTo(HelmChartType.CRD);
        assertThat(evnfmWorkflowInstantiateRequest.getChartVersion()).isEqualTo("1.2.3");

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        Map<String, String> day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(1);
        System.out.println("day0Configuration" + day0Configuration);

        //Check day 0 configuration
        assertThat(day0Configuration).containsKeys("day0TestSecret");
        Map<String, String> secretContent = convertSensitiveInfoToMap(day0Configuration.get("day0TestSecret"));
        assertThat(secretContent).contains(entry("day0TestKey", "day0TestValue"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkflowInstantiateWithCrdAndWithValuesFile() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();

        HelmChart helmChart = instance.getHelmCharts().get(0);
        helmChart.setHelmChartType(HelmChartType.CRD);
        helmChart.setHelmChartVersion("1.2.3");
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(helmChart);
        instance.setCrdNamespace(DEFAULT_CRD_NAMESPACE);
        instance.setHelmCharts(helmCharts);
        instance.setClusterName("cluster3");
        instance.setTempInstance(mapper.writeValueAsString(instance));
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("clusterName", "cluster3");
        additionalParameter.put("namespace", "namespace1");

        instance = vnfInstanceRepository.save(instance);
        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster3",
                                                                               additionalParameter);
        Path values = TestUtils.getValuesFileCopy("valid_values.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(values);
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, valuesYamlMap, "3600");

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiate, values);
        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class),
                                              evnfmWorkFlowInstantiateRequestWithValues.capture(), ArgumentMatchers
                                                      .<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap<String, Object>> request = evnfmWorkFlowInstantiateRequestWithValues.getValue();
        MultiValueMap<String, Object> body = request.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get(JSON_REQUEST_PARAMETER_NAME)).isNotNull();
        assertThat(body.get(VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = mapper.readValue(body.
                                                                                                   get(JSON_REQUEST_PARAMETER_NAME).get(0).toString(),
                                                                                           EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(2);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("340");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster3");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();
        //Check namespace and version for CRD
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo(DEFAULT_CRD_NAMESPACE);
        assertThat(evnfmWorkflowInstantiateRequest.getChartType()).isEqualTo(HelmChartType.CRD);
        assertThat(evnfmWorkflowInstantiateRequest.getChartVersion()).isEqualTo("1.2.3");

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        Map day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(0);
        assertThat(values.toFile().exists()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWorkflowInstantiateWithCrdsForMultiChart() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setClusterName("cluster3");

        HelmChart chart1 = instance.getHelmCharts().get(0);
        chart1.setHelmChartUrl("test-chart-1");
        chart1.setReleaseName("test-chart-1");
        chart1.setPriority(1);
        chart1.setHelmChartType(HelmChartType.CRD);
        chart1.setHelmChartVersion("1.2.0");
        chart1.setVnfInstance(instance);

        HelmChart chart2 = new HelmChart();
        chart2.setHelmChartUrl("test-chart-2");
        chart2.setReleaseName("test-chart-2");
        chart2.setVnfInstance(instance);
        chart1.setHelmChartType(HelmChartType.CNF);
        chart2.setPriority(2);

        HelmChart chart3 = new HelmChart();
        chart3.setHelmChartUrl("test-chart-3");
        chart3.setReleaseName("test-chart-3");
        chart3.setHelmChartVersion("1.2.3.4");
        chart3.setHelmChartType(HelmChartType.CRD);
        chart3.setVnfInstance(instance);
        chart3.setPriority(3);

        HelmChart chart4 = new HelmChart();
        chart4.setHelmChartUrl("test-chart-4");
        chart4.setReleaseName("test-chart-4");
        chart4.setHelmChartType(HelmChartType.CNF);
        chart4.setVnfInstance(instance);
        chart4.setPriority(4);

        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(chart1);
        helmCharts.add(chart2);
        helmCharts.add(chart3);
        helmCharts.add(chart4);
        instance.setHelmCharts(helmCharts);
        instance.setCrdNamespace(DEFAULT_CRD_NAMESPACE);

        instance.setTempInstance(mapper.writeValueAsString(instance));

        instance = vnfInstanceRepository.save(instance);

        Map<String, Object> additionalParameter = createAdditionalParameter();
        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("cluster3",
                                                                               additionalParameter);
        Map additionalParams = (Map) instantiate.getAdditionalParams();
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        additionalParams.keySet().removeIf(parameter -> !EVNFM_PARAMS.contains(parameter));
        operation.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        operation.setOperationParams(mapper.writeValueAsString(instantiate));
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operation);

        workflowRoutingServicePassThrough.routeInstantiateRequest(3, operation, instance);
        Mockito.verify(restTemplate).exchange(contains("test-chart-3"), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(((Map) evnfmWorkflowInstantiateRequest.getAdditionalParams()).size()).isEqualTo(0);
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo("test-chart-3");
        assertThat(evnfmWorkflowInstantiateRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                                .getOperationOccurrenceId());
        assertThat(evnfmWorkflowInstantiateRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getApplicationTimeOut()).isEqualTo("79");
        assertThat(evnfmWorkflowInstantiateRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkflowInstantiateRequest.getClusterName()).isEqualTo("cluster3");
        assertThat(evnfmWorkflowInstantiateRequest.isOverrideGlobalRegistry()).isTrue();
        //Check namespace and version for CRD
        assertThat(evnfmWorkflowInstantiateRequest.getNamespace()).isEqualTo(DEFAULT_CRD_NAMESPACE);
        assertThat(evnfmWorkflowInstantiateRequest.getChartType()).isEqualTo(HelmChartType.CRD);
        assertThat(evnfmWorkflowInstantiateRequest.getChartVersion()).isEqualTo("1.2.3.4");

        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.STARTING.toString());

        Map day0Configuration = (Map) evnfmWorkflowInstantiateRequest.getDay0Configuration();
        assertThat(day0Configuration.size()).isEqualTo(0);

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void shouldSendEnabledChartWhenInstantiateWithDeployableModulesSupported() throws Exception {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setDeployableModulesSupported(true);
        instance.setOssTopology("{}");

        HelmChart disabledChart = getHelmChart("disabled-chart", "disabled-release", instance, 1, HelmChartType.CNF);
        disabledChart.setChartEnabled(false);
        HelmChart enabledChart = getHelmChart("enabled-chart", "enabled-release", instance, 2, HelmChartType.CNF);

        instance.setHelmCharts(Arrays.asList(disabledChart, enabledChart));
        vnfInstanceRepository.save(instance);

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace1");

        InstantiateVnfRequest instantiateRequest = TestUtils.createInstantiateRequest("cluster1", additionalParameter);
        instantiateRequest.setExtensions(TestUtils.createExtensionsWithDeployableModules());

        doReturn("test-ns").when(databaseInteractionService).getClusterConfigServerByClusterName(any());
        doReturn("test-ns").when(databaseInteractionService).getClusterConfigCrdNamespaceByClusterName(any());
        doReturn(Collections.emptyMap()).when(extensionsMapper).getDeployableModulesValues(anyString());
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "with-deployable-modules-vnfd.yaml");
        doReturn(new JSONObject(vnfdString)).when(packageService).getVnfd(anyString());
        doReturn(new PackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(anyString());

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(instance, instantiateRequest, null, LifecycleOperationType.INSTANTIATE, new HashMap<>(), "3600");
        instantiateRequestHandler.createTempInstance(instance, instantiateRequest);
        instantiateRequestHandler.updateInstance(instance, instantiateRequest, LifecycleOperationType.INSTANTIATE, operation, additionalParameter);
        vnfInstanceRepository.save(instance);

        workflowRoutingServicePassThrough.routeInstantiateRequest(instance, operation, instantiateRequest);

        Mockito.verify(restTemplate).exchange(contains("enabled-release"), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowInstantiateRequest evnfmWorkflowInstantiateRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowInstantiateRequest.class);
        assertThat(evnfmWorkflowInstantiateRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
        assertThat(evnfmWorkflowInstantiateRequest.getChartUrl()).isEqualTo(enabledChart.getHelmChartUrl());
    }

    @Test
    public void testWorkflowInstantiateWithTwoValuesFiles() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        HelmChart helmChart = vnfInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";

        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("Payload", createAdditionalParameter());
        LifecycleOperation operation = instantiateRequestHandler.persistOperation(vnfInstance, instantiate,
                null, LifecycleOperationType.INSTANTIATE, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);

        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeInstantiateRequest(vnfInstance, operation, instantiate,
                TestUtils.getValuesFileCopy("valid_values.yaml"));

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
    }

    @Test
    public void testWorkflowInstantiateWithAdditionalValuesFile() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        HelmChart helmChart = vnfInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";

        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("Payload", createAdditionalParameter());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);

        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeInstantiateRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
    }

    @Test
    public void testWorkflowInstantiateWithAdditionalValuesFileWhenArtifactsNotFound() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.getHelmCharts().get(0).setHelmChartName("spider-a");
        PackageResponse packageResponse = new PackageResponse();

        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts("path1/spider-b.yaml", "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));
        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("Payload", createAdditionalParameter());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        lifecycleOperationRepository.save(operation);

        workflowRoutingServicePassThrough.routeInstantiateRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNull();
    }

    @Test
    public void testWorkflowUpgrade() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("parameter1", "value1");

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster4");
        instance.setNamespace("namespace1");
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("parameter2", "value2");

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        operation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(operation);
        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setTempInstance(instance);
        context.setTargetVnfdId("test-vnfdid");
        context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        context.setAdditionalParams(changePackageAdditionalParameter);

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowUpgradeRequest.class);
        assertThat(((Map) evnfmWorkFlowUpgradeRequest.getAdditionalParams()).size()).isEqualTo(1);
        assertThat(evnfmWorkFlowUpgradeRequest.getChartUrl()).isEqualTo("test");

        assertThat(evnfmWorkFlowUpgradeRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                            .getOperationOccurrenceId());
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getApplicationTimeOut()).isEqualTo("499");
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkFlowUpgradeRequest.getClusterName()).isEqualTo("cluster4");
        assertThat(evnfmWorkFlowUpgradeRequest.getNamespace()).isEqualTo("namespace1");
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkFlowUpgradeRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
    }

    @Test
    public void testWorkflowUpgradeForMultiChart() throws Exception {
        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("namespace", "namespace3");

        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setClusterName("cluster22");
        instance.setNamespace("namespace3");
        HelmChart chart = new HelmChart();
        chart.setHelmChartUrl("test-chart-2");
        chart.setReleaseName("test-chart-2");
        chart.setPriority(2);
        chart.setVnfInstance(instance);
        instance.getHelmCharts().add(chart);
        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setTempInstance(mapper.writeValueAsString(instance));

        instance = vnfInstanceRepository.save(instance);

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                additionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");
        operation.setOperationParams(mapper.writeValueAsString(changePackageInfoVnfRequest));
        operation.setCombinedAdditionalParams(instance.getCombinedAdditionalParams());
        lifecycleOperationRepository.save(operation);
        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setTempInstance(instance);
        context.setTargetVnfdId("test-vnfdid");
        context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        context.setAdditionalParams(additionalParameter);

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(2, operation, instance);

        Mockito.verify(restTemplate).exchange(contains("test-chart-2"), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowUpgradeRequest.class);
        assertThat(((Map) evnfmWorkFlowUpgradeRequest.getAdditionalParams()).size()).isEqualTo(0);
        assertThat(evnfmWorkFlowUpgradeRequest.getChartUrl()).isEqualTo("test-chart-2");

        assertThat(evnfmWorkFlowUpgradeRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                            .getOperationOccurrenceId());
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getApplicationTimeOut()).isEqualTo("79");
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getClusterName()).isEqualTo("cluster22");
        assertThat(evnfmWorkFlowUpgradeRequest.getNamespace()).isEqualTo("namespace3");
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkFlowUpgradeRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testWorkflowUpgradeWithInvalidValueForSkipVerification() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster5");
        instance.setNamespace("namespace1");
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("parameter2", "value2");
        changePackageAdditionalParameter.put("skipVerification", "true3434");

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        operation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(operation);

        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setTempInstance(instance);
        context.setTargetVnfdId("test-vnfdid");
        context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        context.setAdditionalParams(changePackageAdditionalParameter);

        assertThatThrownBy(() -> {
            workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The value [true3434] is not a boolean");
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testWorkflowUpgradeWithNullOperationSubTypeAndVnfdId() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("parameter1", "value1");

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster6");
        instance.setNamespace("namespace1");
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        operation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(operation);
        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setTempInstance(instance);
        context.setAdditionalParams(changePackageAdditionalParameter);

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowUpgradeRequest.class);
        assertThat(((Map) evnfmWorkFlowUpgradeRequest.getAdditionalParams()).size()).isEqualTo(0);
        assertThat(evnfmWorkFlowUpgradeRequest.getChartUrl()).isEqualTo("test");

        assertThat(evnfmWorkFlowUpgradeRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                            .getOperationOccurrenceId());
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getApplicationTimeOut()).isEqualTo("499");
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipJobVerification()).isTrue();
        assertThat(evnfmWorkFlowUpgradeRequest.getClusterName()).isEqualTo("cluster6");
        assertThat(evnfmWorkFlowUpgradeRequest.getNamespace()).isEqualTo("namespace1");
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isTrue();

        assertThat(evnfmWorkFlowUpgradeRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
        HttpHeaders headers = request.getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
    }

    @Test
    public void testWorkflowUpgradeWithNullTempInstance() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");

        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setAdditionalParams(changePackageAdditionalParameter);

        assertThatThrownBy(() -> {
            workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);
        }).isInstanceOf(NullPointerException.class);
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testWorkflowUpgradeWithNullAdditionalParameterInContext() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster10");
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("parameter2", "value2");

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(instance, changePackageInfoVnfRequest, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        operation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(operation);
        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setOperation(operation);
        context.setTempInstance(instance);

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), requestWithClusterConfigFile
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        HttpEntity<MultiValueMap> request = requestWithClusterConfigFile.getValue();
        EvnfmWorkFlowUpgradeRequest evnfmWorkFlowUpgradeRequest = getRequestFromMultiValueMap(request, EvnfmWorkFlowUpgradeRequest.class);
        //If additional parameter is empty in the context then the additional parameter in the workflow request
        // would be empty
        assertThat(((Map) evnfmWorkFlowUpgradeRequest.getAdditionalParams()).size()).isEqualTo(0);

        assertThat(evnfmWorkFlowUpgradeRequest.getLifecycleOperationId()).isEqualTo(operation
                                                                                            .getOperationOccurrenceId());
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipVerification()).isFalse();
        assertThat(evnfmWorkFlowUpgradeRequest.getApplicationTimeOut()).isEqualTo("499");
        assertThat(evnfmWorkFlowUpgradeRequest.isSkipJobVerification()).isFalse();
        assertThat(evnfmWorkFlowUpgradeRequest.isOverrideGlobalRegistry()).isFalse();

        //Below details are taken from temp instance
        assertThat(evnfmWorkFlowUpgradeRequest.getChartUrl()).isEqualTo("test");
        assertThat(evnfmWorkFlowUpgradeRequest.getClusterName()).isEqualTo("cluster10");

        assertThat(evnfmWorkFlowUpgradeRequest.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
    }

    @Test
    public void testWorkflowUpgradeWithNullOperation() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("parameter1", "value1");

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster8");
        instance = vnfInstanceRepository.save(instance);
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("parameter2", "value2");

        ChangeCurrentVnfPkgRequest changePackageInfoVnfRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                                                                                                changePackageAdditionalParameter);

        ChangeOperationContext context = new ChangeOperationContext();
        context.setOperationRequest(changePackageInfoVnfRequest);
        context.setTempInstance(instance);

        assertThatThrownBy(() -> {
            workflowRoutingServicePassThrough.routeChangePackageInfoRequest(context);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testWorkflowUpgradeWithTwoValuesFiles() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        HelmChart helmChart = vnfInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        helmChart.setPriority(1);
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";

        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));

        ChangeCurrentVnfPkgRequest upgradeRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                createAdditionalParameter());
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, upgradeRequest,
                null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        lifecycleOperationRepository.save(operation);

        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setTempInstance(vnfInstance);
        changeOperationContext.setOperation(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);

        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(changeOperationContext, TestUtils.getValuesFileCopy("valid_values.yaml"),
                                                                        helmChart.getPriority());

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();
        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
    }

    @Test
    public void testWorkflowUpgradeWithAdditionalValuesFile() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        HelmChart helmChart = vnfInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));

        ChangeCurrentVnfPkgRequest upgradeRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                createAdditionalParameter());
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, upgradeRequest,
                null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);

        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();
        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();
    }

    @Test
    public void testWorkflowUpgradeWithAdditionalValuesFileWhenArtifactsNotFound() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.getHelmCharts().get(0).setHelmChartName("spider-a");
        PackageResponse packageResponse = new PackageResponse();

        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts("path1/spider-b.yaml", "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(vnfInstance));
        ChangeCurrentVnfPkgRequest upgradeRequest = TestUtils.createUpgradeRequest("test-vnfdid",
                createAdditionalParameter());
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, upgradeRequest,
                null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        lifecycleOperationRepository.save(operation);

        workflowRoutingServicePassThrough.routeChangePackageInfoRequest(1, operation, vnfInstance);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();
        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNull();
    }

    @Test
    public void testRouteTerminateRequest() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("parameter1", "value1");

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster20");
        instance.setNamespace("namespace14");
        instance = vnfInstanceRepository.save(instance);

        Map<String, Object> additionalParameterInRequest = new HashMap<>();
        additionalParameterInRequest.put("skipVerification", true);
        additionalParameterInRequest.put("applicationTimeOut", 340);
        additionalParameterInRequest.put("skipJobVerification", true);

        TerminateVnfRequest terminateVnfRequest = TestUtils.createTerminateRequest(additionalParameterInRequest);

        LifecycleOperation operation = terminateRequestHandler.persistOperation(instance, terminateVnfRequest, null,
                                                                                LifecycleOperationType.TERMINATE, null, "3600");
        ArgumentCaptor<HttpEntity<MultiValueMap<String, Object>>> evnfmTerminateRequest = ArgumentCaptor.forClass(HttpEntity.class);
        workflowRoutingServicePassThrough.routeTerminateRequest(instance, operation, additionalParameterInRequest,
                                                                "testRelease");
        Mockito.verify(restTemplate).exchange(terminateUrl.capture(), any(HttpMethod.class), evnfmTerminateRequest.capture(),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        assertThat(terminateUrl.getValue()).isEqualTo("http://localhost:10103/api/lcm/v3/resources/testRelease/terminate?" +
                                                              "applicationTimeOut=340&clusterName=cluster20&skipJobVerification=true&namespace"
                                                              + "=namespace14&lifecycleOperationId="
                                                              + operation.getOperationOccurrenceId() + "&state=STARTING&skipVerification=true");



        HttpHeaders headers = evnfmTerminateRequest.getValue().getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testRouteTerminateRequestWithNoAdditionalParameter() throws Exception {
        VnfInstance instance = vnfInstanceRepository.save(TestUtils.getVnfInstance());
        instance.setTempInstance(mapper.writeValueAsString(instance));

        Map<String, Object> additionalParameter = createAdditionalParameter();
        additionalParameter.put("parameter1", "value1");

        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParameter));
        instance.setClusterName("cluster18");
        instance.setNamespace("namespace14");
        instance = vnfInstanceRepository.save(instance);

        Map<String, Object> additionalParameterInRequest = null;

        TerminateVnfRequest terminateVnfRequest = TestUtils.createTerminateRequest(additionalParameterInRequest);

        LifecycleOperation operation = terminateRequestHandler.persistOperation(instance, terminateVnfRequest, null,
                                                                                LifecycleOperationType.TERMINATE, null, "3600");
        workflowRoutingServicePassThrough.routeTerminateRequest(instance, operation, additionalParameterInRequest,
                                                                "testRelease");
        Mockito.verify(restTemplate).exchange(terminateUrl.capture(), any(HttpMethod.class), any(HttpEntity.class),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        assertThat(terminateUrl.getValue()).isEqualTo("http://localhost:10103/api/lcm/v3/resources/testRelease/terminate?" +
                                                              "clusterName=cluster18&namespace=namespace14"
                                                              + "&lifecycleOperationId="
                                                              + operation.getOperationOccurrenceId() + "&state=STARTING");

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(operation.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllSecretsWhenExceptionOccurs() {
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenThrow(new RuntimeException("Error occurred"));

        assertThatThrownBy(() -> workflowRoutingServicePassThrough.routeToEvnfmWfsForGettingAllSecrets("cluster", "namespace"))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessage("An error occurred during getting all Secret");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllSecretsWhenErrorResponseWithBody() {
        final var responseEntity = ResponseEntity.badRequest().body(new WorkflowSecretResponse());
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenReturn(responseEntity);

        assertThatThrownBy(() -> workflowRoutingServicePassThrough.routeToEvnfmWfsForGettingAllSecrets("cluster", "namespace"))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessage("Failed due to WorkflowSecretResponse[allSecrets={}]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllSecretsWhenErrorResponse() {
        final var responseEntity = ResponseEntity.badRequest().build();
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenReturn(responseEntity);

        assertThatThrownBy(() -> workflowRoutingServicePassThrough.routeToEvnfmWfsForGettingAllSecrets("cluster", "namespace"))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessage("Failed due to response is null from the Workflow service");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRouteDeleteNamespaceWhenHttpException() {
        // given
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(restTemplate).postForEntity(any(String.class), any(), any(), any(Map.class));

        // when and then
        assertThatThrownBy(() -> workflowRoutingServicePassThrough.routeDeleteNamespace("namespace", "cluster", "releaseName", "1234", "operationId"))
                .isInstanceOf(DeleteNamespaceException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRouteDeleteNamespaceWithIdempotency() {

        ArgumentCaptor<HttpEntity<MultiValueMap<String, Object>>> deleteNamespaceRequest = ArgumentCaptor.forClass(HttpEntity.class);
        workflowRoutingServicePassThrough.routeDeleteNamespace("namespace", "cluster", "releaseName", "1234", "operationId");
        verify(restTemplate).postForEntity(any(String.class), deleteNamespaceRequest.capture(), any(), any(Map.class));

        HttpHeaders headers = deleteNamespaceRequest.getValue().getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith("operationId");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRouteDeletePvcWithIdempotency() {

        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(ResponseEntity.accepted().build());
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstanceRepository.save(vnfInstance);

        Map<String, Object> additionalParameterInRequest = null;

        TerminateVnfRequest terminateVnfRequest = TestUtils.createTerminateRequest(additionalParameterInRequest);

        LifecycleOperation operation = terminateRequestHandler.persistOperation(vnfInstance, terminateVnfRequest, null,
                                                                                LifecycleOperationType.TERMINATE, null, "3600");

        ArgumentCaptor<HttpEntity<MultiValueMap<String, Object>>> deletePvcRequest = ArgumentCaptor.forClass(HttpEntity.class);
        workflowRoutingServicePassThrough.routeDeletePvcRequest(vnfInstance, "releaseName", operation.getOperationOccurrenceId());
        verify(restTemplate).exchange(any(String.class), any(HttpMethod.class), deletePvcRequest.capture(), any(Class.class));

        HttpHeaders headers = deletePvcRequest.getValue().getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRouteDeleteNamespaceWhenException() {
        // given
        doThrow(new RuntimeException("Error occurred")).when(restTemplate).postForEntity(any(String.class), any(), any(), any(Map.class));

        // when and then
        assertThatThrownBy(() -> workflowRoutingServicePassThrough.routeDeleteNamespace("namespace", "cluster", "releaseName", "1234", "operationId"))
                .isInstanceOf(DeleteNamespaceException.class)
                .hasMessage("Workflow service is unavailable or unable to process the request");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testGetChartValuesRequestWhenFirstRequestFailedAndRetried() {
        // given
        final ResponseEntity<Map> responseEntity = ResponseEntity.ok(Map.of("key1", "value1"));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR))
                .thenReturn(responseEntity);

        final VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setClusterName("clusterName");
        vnfInstance.setNamespace("namespace");

        // when
        final ResponseEntity<Map> actualResponse = workflowRoutingServicePassThrough.getChartValuesRequest(vnfInstance, "releaseName");

        // then
        assertThat(actualResponse).isSameAs(responseEntity);
    }

    @Test
    public void testGetChartValuesRequestWhenAllRequestsFailed() {
        // given
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        final VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setClusterName("clusterName");
        vnfInstance.setNamespace("namespace");

        // when and then
        assertThatThrownBy(() -> workflowRoutingServicePassThrough.getChartValuesRequest(vnfInstance, "releaseName"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetHelmVersions() {
        // given
        ResponseEntity<HelmVersionsResponse> responseEntity = ResponseEntity.ok(getHelmVersionsResponse());

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(HelmVersionsResponse.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR))
                .thenReturn(responseEntity);

        // when
        HelmVersionsResponse actualResponse = workflowRoutingServicePassThrough.getHelmVersionsRequest();

        // then
        assertThat(actualResponse).isEqualTo(responseEntity.getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetHelmVersionsWhenWorkflowServiceUnavailable() {
        // given
        doThrow(new RuntimeException("Error occurred"))
                .when(restTemplate).exchange(any(String.class), eq(HttpMethod.GET), any(), eq(HelmVersionsResponse.class));

        // when and then
        assertThatThrownBy(() -> workflowRoutingServicePassThrough.getHelmVersionsRequest())
                .isInstanceOf(HelmVersionsException.class)
                .hasMessage("Workflow service is unavailable or unable to process the request");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetHelmVersionsWhenHttpException() {
        // given
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(HelmVersionsResponse.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        // when and then
        assertThatThrownBy(() -> workflowRoutingServicePassThrough.getHelmVersionsRequest())
                .isInstanceOf(HelmVersionsException.class)
                .hasMessage("No helm versions found");
    }

    @Test
    public void testWorkflowInstantiateForRollbackWithAdditionalValuesFile() throws Exception {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        VnfInstance tempInstance = TestUtils.getVnfInstance();
        tempInstance.setVnfDescriptorId("target_package_vnfd_id");
        HelmChart helmChart = tempInstance.getHelmCharts().get(0);
        helmChart.setHelmChartName("spider-a");
        helmChart.setHelmChartVersion("1.0");
        String desiredArtifactsPath = PATH_ARTIFACTS_APPENDIX + "spider-a-1.0.yaml";

        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setAdditionalArtifacts(createAdditionalArtifacts(desiredArtifactsPath, "/", "path1/1-spider-a.yaml", "path1/spider-a-2.yaml"));
        vnfInstanceRepository.save(vnfInstance);
        vnfInstance.setTempInstance(mapper.writeValueAsString(tempInstance));

        InstantiateVnfRequest instantiate = TestUtils.createInstantiateRequest("Payload", createAdditionalParameter());
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, instantiate, null, LifecycleOperationType.INSTANTIATE, null, "3600");
        lifecycleOperationRepository.save(operation);

        URI getVnfdUri = onboardingUriProvider.getOnboardingPackagesQueryUri(tempInstance.getVnfDescriptorId());
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfInstance.getVnfPackageId(), desiredArtifactsPath);

        Mockito.when(onboardingClient.get(Mockito.eq(getVnfdUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{packageResponse}));
        Mockito.when(onboardingClient.get(Mockito.eq(getArtifactUri), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(DESIRE_ARTIFACT_CONTENT));

        workflowRoutingServicePassThrough.routeInstantiateRequest( vnfInstance, operation,
                                                                   helmChart, new HashMap<>(), null);

        Mockito.verify(restTemplate).exchange(contains(DUMMY_RELEASE_NAME), any(HttpMethod.class), evnfmWorkFlowInstantiateRequestWithValues.capture(),
                                              ArgumentMatchers.<Class<ResourceResponse>>any());
        MultiValueMap body = evnfmWorkFlowInstantiateRequestWithValues.getValue().getBody();

        assertThat(body.get(WorkflowRequestBodyBuilder.SECOND_VALUES_REQUEST_PARAMETER_NAME)).isNotNull();

        HttpHeaders headers = evnfmWorkFlowInstantiateRequestWithValues.getValue().getHeaders();
        String idempotencyHeader = headers.getFirst(IDEMPOTENCY_KEY_HEADER);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader).startsWith(operation.getOperationOccurrenceId());
    }

    @Test
    public void testIsEvnfmNamespaceAndCluster() {
        // given
        final NamespaceValidationResponse responseBody = NamespaceValidationResponse.builder().isEvnfmAndClusterNamespace(true).build();
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        // when
        final boolean result = workflowRoutingServicePassThrough.isEvnfmNamespaceAndCluster("namespace", "clusterName");

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testIsEvnfmNamespaceAndClusterWhenServerErrorOccurs() {
        // given
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        // when and then
        assertThatExceptionOfType(NamespaceValidationException.class)
                .isThrownBy(() -> workflowRoutingServicePassThrough.isEvnfmNamespaceAndCluster("namespace", "clusterName"))
                .withMessage("Workflow service is unavailable or unable to process the request")
                .satisfies(exception -> assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR));
    }

    @Test
    public void testIsEvnfmNamespaceAndClusterWhenNetworkErrorOccurs() {
        // given
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new ResourceAccessException("I/O error occurred"));

        // when and then
        assertThatExceptionOfType(NamespaceValidationException.class)
                .isThrownBy(() -> workflowRoutingServicePassThrough.isEvnfmNamespaceAndCluster("namespace", "clusterName"))
                .withMessage("Workflow service is unavailable or unable to process the request")
                .satisfies(exception -> assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST));
    }

    private HelmVersionsResponse getHelmVersionsResponse() {
        List<String> helmVersions = Arrays.asList("3.8", "3.10", "latest");

        HelmVersionsResponse helmVersionsResponse = new HelmVersionsResponse();
        helmVersionsResponse.setHelmVersions(helmVersions);

        return helmVersionsResponse;
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

    private Map<String, Object> createAdditionalParameter() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("skipVerification", true);
        additionalParameter.put("applicationTimeOut", 340);
        additionalParameter.put("skipJobVerification", true);
        additionalParameter.put("overrideGlobalRegistry", "true");
        return additionalParameter;
    }

    private EvnfmWorkFlowInstantiateRequest getEvnfmWorkFlowInstantiateRequest(final boolean overrideGlobalRegistry) {
        VnfInstance instance = new VnfInstance();
        instance.setOverrideGlobalRegistry(overrideGlobalRegistry);
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = copyParametersMap(request.getAdditionalParams());
        return new EvnfmWorkFlowInstantiateRequest.EvnfmWorkFlowInstantiateBuilder( "chartUrl", additionalParams) // release name = "releaseName"
                .withOverrideGlobalRegistry(instance.isOverrideGlobalRegistry())
                .withChartType(HelmChartType.CNF)
                .build();
    }

    private EvnfmWorkFlowUpgradeRequest getEvnfmWorkFlowUpgradeRequest(final boolean overrideGlobalRegistry) {
        VnfInstance instance = new VnfInstance();
        instance.setOverrideGlobalRegistry(overrideGlobalRegistry);
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        Map<String, Object> additionalParams = copyParametersMap(request.getAdditionalParams());
        return new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder("chartUrl", additionalParams)
                .withOverrideGlobalRegistry(instance.isOverrideGlobalRegistry()).build();
    }

    private EvnfmWorkflowScaleRequest getEvnfmWorkFlowScaleRequest(final boolean overrideGlobalRegistry) {
        Map<String, Map<String, Integer>> currentScaleParameters = new HashMap<>();
        VnfInstance instance = new VnfInstance();
        instance.setOverrideGlobalRegistry(overrideGlobalRegistry);
        ScaleVnfRequest request = new ScaleVnfRequest();
        Map<String, Object> additionalParams = copyParametersMap(request.getAdditionalParams());
        return new EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder(additionalParams, "chartUrl")
                .withOverrideGlobalRegistry(instance.isOverrideGlobalRegistry())
                .withScaleResources(currentScaleParameters).build();
    }

    @SuppressWarnings("unchecked")
    private <T> T getRequestFromMultiValueMap(HttpEntity<MultiValueMap> request, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(((ArrayList<String>) request.getBody().get("json")).get(0), clazz);
    }

    private Map<String, String> getSensitiveInfo(final VnfInstance instance) {
        String sensitiveInfo = instance.getSensitiveInfo();
        Map<String, String> sensitiveInfoToMap = convertSensitiveInfoToMap(sensitiveInfo);
        String sensitiveInfoDetails = sensitiveInfoToMap.get(DAY0_CONFIGURATION_PREFIX);
        return convertSensitiveInfoToMap(sensitiveInfoDetails);
    }

    private Map<String, String> convertSensitiveInfoToMap(String jsonContent) {
        Map<String, String> sensitiveInfoMap;
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {
        };
        try {
            sensitiveInfoMap = mapper.readValue(jsonContent, typeRef);
        } catch (JsonProcessingException exp) {
            LOGGER.error("Json parsing error", exp);
            throw new IllegalArgumentException(
                    "Unable to parse resource details, Invalid value provided during " + "instantiate " + exp.getMessage());
        }

        return sensitiveInfoMap;
    }

    @NotNull
    private Map<String, Map<String, Object>> prepareDay0ConfigurationSecrets() {
        Map<String, Object> secretContent1 = new HashMap<>();
        secretContent1.put("username", "John");
        secretContent1.put("password", "hashpassword");
        secretContent1.put("users", 3);
        Map<String, Map<String, Object>> secrets = new HashMap<>();
        secrets.put("secret1", secretContent1);

        Map<String, Object> secretContent2 = new HashMap<>();
        secretContent2.put("username", "TestKey");
        secretContent2.put("password", "TestPass");
        secrets.put("secret2", secretContent2);

        Map<String, Object> secretContent3 = new HashMap<>();
        secretContent3.put("username", "TestKey2");
        secretContent3.put("password", "TestPass2");
        secrets.put("secret3", secretContent3);
        return secrets;
    }

    @NotNull
    private Map<String, Map<String, Object>> prepareDay0ConfigurationSecretsWithEmptyOneKeyValuePair() {
        Map<String, Object> secretContent1 = new HashMap<>();
        secretContent1.put("username", "John");
        secretContent1.put("password", "hashpassword");
        secretContent1.put("users", 3);
        Map<String, Map<String, Object>> secrets = new HashMap<>();
        secrets.put("secret1", secretContent1);

        Map<String, Object> secretContent2 = new HashMap<>();
        secretContent2.put("", "");
        secretContent2.put("password", "TestPass");
        secrets.put("secret2", secretContent2);

        Map<String, Object> secretContent3 = new HashMap<>();
        secrets.put("secret3", secretContent3);
        return secrets;
    }

    private void verifyDay0Params(Map<String, String> sensitiveInfoDetailsToMap) {
        assertThat(sensitiveInfoDetailsToMap).containsKeys("test-secret", "secret1", "secret2", "secret3");

        Map<String, String> secretContent = convertSensitiveInfoToMap(sensitiveInfoDetailsToMap.get("test-secret"));
        Map<String, String> secret1Content = convertSensitiveInfoToMap(sensitiveInfoDetailsToMap.get("secret1"));
        Map<String, String> secret2Content = convertSensitiveInfoToMap(sensitiveInfoDetailsToMap.get("secret2"));
        Map<String, String> secret3Content = convertSensitiveInfoToMap(sensitiveInfoDetailsToMap.get("secret3"));

        assertThat(secretContent).contains(entry("key2", "value2"), entry("test-key", "test-value1"));
        assertThat(secret1Content).contains(entry("username", "John"), entry("users", "3"), entry("password", "hashpassword"));
        assertThat(secret2Content).contains(entry("username", "TestKey"), entry("password", "TestPass"));
        assertThat(secret3Content).contains(entry("username", "TestKey2"), entry("password", "TestPass2"));
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "with-deployable-modules-vnfd.yaml");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }
}
