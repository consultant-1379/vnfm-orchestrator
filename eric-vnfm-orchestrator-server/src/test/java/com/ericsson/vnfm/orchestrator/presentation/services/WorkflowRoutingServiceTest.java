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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY_POST_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.VduInitialDeltaProperties;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ComponentStatusResponseList;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class WorkflowRoutingServiceTest extends AbstractDbSetupTest {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void before() {
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());
    }

    @Test
    public void successfulResponseFromRoutedWFS() {
        whenWfsResourcesRespondsWithAccepted();

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a379";
        final InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        final HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "test");
        addTimeoutsToRequest(additionalParams);
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = new LifecycleOperation();

        WorkflowRoutingResponse response = workflowRoutingService
                .routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getInstanceId()).matches("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        assertThat(response.getLinks()).hasSize(2);
    }

    private static void addTimeoutsToRequest(final Map<String, Object> additionalParams) {
        additionalParams.put(APPLICATION_TIME_OUT, 25);
    }

    @Test
    public void successfulResponseForValidInstantiationReqWithCrdFromRoutedWFS() {
        whenWfsResourcesRespondsWithAccepted();

        final String vnfInstanceId = "9845971235-as49-4c24-8796-6e5afa2535g1";
        final InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        final HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "test");

        addTimeoutsToRequest(additionalParams);
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = new LifecycleOperation();

        WorkflowRoutingResponse response = workflowRoutingService
                .routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getInstanceId()).matches("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        assertThat(response.getLinks()).hasSize(2);
    }

    @Test
    public void failureResponseFromRoutedWFS() {
        whenWfsResourcesRespondsWithBadRequest("[{\"message\": \"releaseName must consist of lower case alphanumeric characters\"}]");

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-214jx84e6a379";
        final InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        final HashMap<Object, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "test");
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = new LifecycleOperation();

        WorkflowRoutingResponse response = workflowRoutingService.routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getErrorMessage()).contains("releaseName must consist of lower case alphanumeric characters");
    }

    @Test
    public void failureResponseForInvalidInstantiationReqWithCrdFromRoutedWFS() {
        whenWfsResourcesRespondsWithBadRequest("[{\"message\": \"chartVersion is required for CRD chartType\"}]");

        final String vnfInstanceId = "89883055-as49-4c24-8796-6e5afa2535g1";
        final InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        final HashMap<Object, Object> additionalParams = new HashMap<>();
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = new LifecycleOperation();

        WorkflowRoutingResponse response = workflowRoutingService.routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getErrorMessage()).contains("chartVersion is required for CRD chartType");
    }

    @Test
    public void testSuccessfulResponseForValidInstantiationReqWithCrdAndValuesFiles() throws Exception {
        whenWfsResourcesRespondsWithAccepted();

        final String vnfInstanceId = "9845971235-as49-4c24-8796-6e5afa2535g1";
        Path valuesFilePath = TestUtils.getValuesFileCopy("combined-values-test.yaml");
        final InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        final HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "test");
        addTimeoutsToRequest(additionalParams);
        instantiateVnfRequest.setAdditionalParams(additionalParams);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = new LifecycleOperation();

        WorkflowRoutingResponse response = workflowRoutingService
                .routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest, valuesFilePath);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getInstanceId()).matches("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        assertThat(response.getLinks()).hasSize(2);
        assertThat(valuesFilePath.toFile().exists()).isFalse();
    }

    public TerminateVnfRequest setTerminateRequest(TerminateVnfRequest.TerminationTypeEnum type) {
        final TerminateVnfRequest terminateVnfRequest = new TerminateVnfRequest();
        terminateVnfRequest.setTerminationType(type);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        addTimeoutsToRequest(additionalParams);
        terminateVnfRequest.setAdditionalParams(additionalParams);
        return terminateVnfRequest;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulResponseFromRoutedWFSDeletePvc() {
        when(restTemplate.exchange(contains("pvcs"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));;

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a379";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        vnfInstance.setClusterName("testpvcdeletion");
        vnfInstance.setNamespace("pvc-deletion");
        ResponseEntity<Object> objectResponseEntity = workflowRoutingService
                .routeDeletePvcRequest(vnfInstance, "my-release-name", "m08fcbc8-474f-4673-91ee-761fd83991e6");
        assertThat(objectResponseEntity).isNotNull();
        assertThat(objectResponseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                .containsOnly(LifecycleOperationState.PROCESSING.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failureResponseFromRoutedWFSDeletePvc() {
        when(restTemplate.exchange(contains("pvcs"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a379";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        vnfInstance.setClusterName("mycluster-delete-pvc");
        vnfInstance.setNamespace(null);
        ResponseEntity<Object> objectResponseEntity = workflowRoutingService
                .routeDeletePvcRequest(vnfInstance, "my-release-name", "m08fcbc8-474f-4673-91ee-761fd83991e6");
        assertThat(objectResponseEntity).isNotNull();
        assertThat(objectResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                .containsOnly(LifecycleOperationState.PROCESSING.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulResponseFromRouteToEvnfmWfsForPatchingSecrets() {
        when(restTemplate.exchange(contains("secrets"), eq(HttpMethod.PUT), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));

        ResponseEntity<Object> objectResponseEntity = workflowRoutingService
                .routeToEvnfmWfsForPatchingSecrets(KMS_UNSEAL_KEY_POST_STRING, KMS_UNSEAL_KEY, "key-value",
                                                   "test-patch-secret", "patch-secret");
        assertThat(objectResponseEntity).isNotNull();
        assertThat(objectResponseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failureResponseFromRouteToEvnfmWfsForPatchingSecrets() {
        when(restTemplate.exchange(contains("secrets"), eq(HttpMethod.PUT), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>("{\"errorDetails\": [{\"message\": \"namespace cannot be null\"}]}", HttpStatus.BAD_REQUEST));

        ResponseEntity<Object> objectResponseEntity = workflowRoutingService
                .routeToEvnfmWfsForPatchingSecrets(KMS_UNSEAL_KEY_POST_STRING, KMS_UNSEAL_KEY, "key-value",
                                                   "test-patch-secret", null);
        assertThat(objectResponseEntity).isNotNull();
        assertThat(objectResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(objectResponseEntity.getBody()).toString()).contains("namespace cannot be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulResponseFromRoutedWFSUpgrade() {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a379";
        final ChangeCurrentVnfPkgRequest changeVnfpkgVnfRequest = new ChangeCurrentVnfPkgRequest();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams);
        changeVnfpkgVnfRequest.setAdditionalParams(additionalParams);
        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setOperationRequest(changeVnfpkgVnfRequest);
        changeOperationContext.setSourceVnfInstance(vnfInstance);
        changeOperationContext.setTempInstance(vnfInstance);
        changeOperationContext.setOperation(operation);
        changeOperationContext.setAdditionalParams((Map<String, Object>) changeVnfpkgVnfRequest.getAdditionalParams());
        WorkflowRoutingResponse response = workflowRoutingService.routeChangePackageInfoRequest(changeOperationContext);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getInstanceId()).matches("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        assertThat(response.getLinks()).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failureResponseFromRoutedWFSUpgrade() {
        whenWfsRespondsWithBadRequest("upgrade");

        final String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-214jx84e6a379";
        final ChangeCurrentVnfPkgRequest changeVnfpkgVnfRequest = new ChangeCurrentVnfPkgRequest();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setOperationRequest(changeVnfpkgVnfRequest);
        changeOperationContext.setSourceVnfInstance(vnfInstance);
        changeOperationContext.setTempInstance(vnfInstance);
        changeOperationContext.setOperation(operation);
        changeOperationContext.setAdditionalParams((Map<String, Object>) changeVnfpkgVnfRequest.getAdditionalParams());
        WorkflowRoutingResponse response = workflowRoutingService.routeChangePackageInfoRequest(changeOperationContext);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getErrorMessage()).contains("releaseName must consist of lower case alphanumeric characters");
    }

    @Test
    public void successfulResponseFromRoutePodStatusRequest() {
        whenWfsPodsRespondsWith("workflow-service/pods-status-response.json");

        final String vnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        ComponentStatusResponse response = workflowRoutingService.getComponentStatusRequest(vnfInstance);
        assertThat(response).isNotNull();
        assertThat(response.getPods()).hasSize(5);
    }

    @Test
    public void successfulResponseFromRoutePodStatusRequestForList() {
        whenWfsPodsRespondsWith("workflow-service/pods-status-response.json");

        final String vnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        List<ComponentStatusResponse> responses = workflowRoutingService.getComponentStatusRequest(Collections.singletonList(vnfInstance));
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getPods()).isNotNull();
        assertThat(responses.get(0).getPods()).hasSize(3);
        assertThat(responses.get(1).getPods()).isNotNull();
        assertThat(responses.get(1).getPods()).hasSize(2);
    }

    @Test
    public void successfulResponseFromRouteComponentStatusRequestForRel4() {
        whenWfsAdditionalResourceInfoRespondsWith("workflow-service/additional-resource-info-response.json");

        final String vnfInstanceId = "30cdfe9a-a72a-45c2-a43a-c83bd9d13606";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        assertThat(vnfInstance.isRel4()).isTrue();
        ComponentStatusResponse response = workflowRoutingService.getComponentStatusRequest(vnfInstance);
        assertThat(response).isNotNull();
        assertThat(response.getPods()).hasSize(7);
        assertThat(response.getDeployments()).isNotEmpty();
        assertThat(response.getStatefulSets()).isNotEmpty();
    }

    @Test
    public void failedResponseFromRoutePodStatusRequestForLegacyWithInvalidClusterName() {
        ComponentStatusResponse expected = new ComponentStatusResponse();
        final String vnfInstanceId = "f3def1ce-4cf4-477c-aab3";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        vnfInstance.setClusterName("hart070__>.config");
        ComponentStatusResponse actual = workflowRoutingService.getComponentStatusRequest(vnfInstance);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void successfulResponseFromRouteComponentStatusRequestListForLegacyAndRel4() {
        whenWfsPodsRespondsWith("workflow-service/pods-status-response.json");
        whenWfsAdditionalResourceInfoRespondsWith("workflow-service/additional-resource-info-response.json");

        final String rel4VnfInstanceId = "30cdfe9a-a72a-45c2-a43a-c83bd9d13606";
        final String legacyVnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";

        VnfInstance rel4VnfInstance = vnfInstanceRepository.findByVnfInstanceId(rel4VnfInstanceId);
        VnfInstance legacyVnfInstance = vnfInstanceRepository.findByVnfInstanceId(legacyVnfInstanceId);

        List<VnfInstance> vnfInstances = List.of(rel4VnfInstance, legacyVnfInstance);

        List<ComponentStatusResponse> componentStatusResponses = workflowRoutingService.getComponentStatusRequest(vnfInstances);

        assertThat(componentStatusResponses).hasSize(3);

        ComponentStatusResponse legacyComponentStatusResponseOne = componentStatusResponses.get(0);
        ComponentStatusResponse legacyComponentStatusResponseTwo = componentStatusResponses.get(1);
        ComponentStatusResponse rel4ComponentStatusResponse = componentStatusResponses.get(2);

        assertThat(legacyComponentStatusResponseOne.getPods()).hasSize(3);
        assertThat(legacyComponentStatusResponseOne.getDeployments()).isNull();
        assertThat(legacyComponentStatusResponseOne.getStatefulSets()).isNull();

        assertThat(legacyComponentStatusResponseTwo.getPods()).hasSize(2);
        assertThat(legacyComponentStatusResponseTwo.getDeployments()).isNull();
        assertThat(legacyComponentStatusResponseTwo.getStatefulSets()).isNull();

        assertThat(rel4ComponentStatusResponse.getPods()).hasSize(7);
        assertThat(rel4ComponentStatusResponse.getDeployments()).hasSize(5);
        assertThat(rel4ComponentStatusResponse.getStatefulSets()).hasSize(1);
    }

    @Test
    public void failedResponseFromRouteComponentStatusRequestListForLegacyAndRel4WithInvalidClusterName() {
        final String rel4VnfInstanceId = "30cdfe9a-a72a-45c2-a43a-c83bd9d13606";
        final String legacyVnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";

        VnfInstance rel4VnfInstance = vnfInstanceRepository.findByVnfInstanceId(rel4VnfInstanceId);
        VnfInstance legacyVnfInstance = vnfInstanceRepository.findByVnfInstanceId(legacyVnfInstanceId);
        rel4VnfInstance.setClusterName("haber900__>.config");
        legacyVnfInstance.setClusterName("hart070__>.config");

        List<VnfInstance> vnfInstances = List.of(rel4VnfInstance, legacyVnfInstance);

        List<ComponentStatusResponse> componentStatusResponses = workflowRoutingService.getComponentStatusRequest(vnfInstances);

        assertThat(componentStatusResponses).isEmpty();
    }

    @Test
    public void successfulResponseFromRouteComponentStatusRequestListForLegacyWithInvalidClusterNameAndRel4() {
        when(restTemplate.exchange(contains("pods?clusterName=hart070__>.config"), eq(HttpMethod.POST), any(), eq(ComponentStatusResponseList.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);
        whenWfsAdditionalResourceInfoRespondsWith("workflow-service/additional-resource-info-response.json");

        final String rel4VnfInstanceId = "30cdfe9a-a72a-45c2-a43a-c83bd9d13606";
        final String legacyVnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";

        VnfInstance rel4VnfInstance = vnfInstanceRepository.findByVnfInstanceId(rel4VnfInstanceId);
        VnfInstance legacyVnfInstance = vnfInstanceRepository.findByVnfInstanceId(legacyVnfInstanceId);
        legacyVnfInstance.setClusterName("hart070__>.config");

        List<VnfInstance> vnfInstances = List.of(rel4VnfInstance, legacyVnfInstance);

        List<ComponentStatusResponse> componentStatusResponses = workflowRoutingService.getComponentStatusRequest(vnfInstances);

        ComponentStatusResponse rel4ComponentStatusResponse = componentStatusResponses.get(0);
        assertThat(componentStatusResponses).hasSize(1);
        assertThat(rel4ComponentStatusResponse.getPods()).hasSize(7);
        assertThat(rel4ComponentStatusResponse.getDeployments()).hasSize(5);
        assertThat(rel4ComponentStatusResponse.getStatefulSets()).hasSize(1);
    }

    @Test
    public void failedResponseFromRouteComponentStatusRequestListForLegacyAndRel4WithoutClusterName() {
        final String rel4VnfInstanceId = "30cdfe9a-a72a-45c2-a43a-c83bd9d13606";
        final String legacyVnfInstanceId = "f3def1ce-4cf4-477c-aab3-21c454e6a389";

        VnfInstance rel4VnfInstance = vnfInstanceRepository.findByVnfInstanceId(rel4VnfInstanceId);
        VnfInstance legacyVnfInstance = vnfInstanceRepository.findByVnfInstanceId(legacyVnfInstanceId);
        legacyVnfInstance.setClusterName(null);
        rel4VnfInstance.setClusterName(null);

        List<VnfInstance> vnfInstances = List.of(rel4VnfInstance, legacyVnfInstance);

        List<ComponentStatusResponse> componentStatusResponses = workflowRoutingService.getComponentStatusRequest(vnfInstances);

        assertThat(componentStatusResponses).isEmpty();
    }

    @Test
    public void testChangePkgInfoOperationWithDoubleQuotesOnKey() throws IOException {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        final ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance instance = getPojoFromJsonString(vnfInstance.getTempInstance());
        changeOperationContext.setTempInstance(instance);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);
        changeOperationContext.setOperation(lifecycleOperation);
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, 1);
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    public void testChangeVnfPkgInfoOperationWithDoubleQuotesOnKey() throws IOException {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.vnfdId("e3def1ce-4236-477c-abb3-21c454e6a645");

        ChangeOperationContext changeOperationContext = new ChangeOperationContext(vnfInstance, changeCurrentVnfPkgRequest);
        changeOperationContext.setTempInstance(getPojoFromJsonString(vnfInstance.getTempInstance()));
        changeOperationContext.setOperation(lifecycleOperation);
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, 1);
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    public void testChangeVnfPkgOperationWhenReplicaDetailsPresent() throws IOException {
        // given
        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance tempInstance = getPojoFromJsonString(vnfInstance.getTempInstance());

        tempInstance.setResourceDetails(convertObjToJsonString(Map.of("target1", 2)));

        final InitialDelta initialDelta = new InitialDelta();
        initialDelta.setTargets(new String[] { "target1" });
        initialDelta.setProperties(new VduInitialDeltaProperties());
        tempInstance.setPolicies(convertObjToJsonString(new Policies.Builder()
                                                                .allInitialDelta(Map.of("Payload_InitialDelta", initialDelta))
                                                                .build()));

        tempInstance.setManoControlledScaling(true);

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);

        final ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setTempInstance(tempInstance);
        changeOperationContext.setOperation(lifecycleOperation);

        whenWfsUpgradeRespondsWithAccepted();

        // when
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, 1);

        // then
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    public void testChangeVnfPkgOperationWhenReplicaDetailsPresentSizeMismatch() throws IOException {
        // given
        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance tempInstance = getPojoFromJsonString(vnfInstance.getTempInstance());

        tempInstance.setResourceDetails(convertObjToJsonString(Map.of("target1", 2)));

        final InitialDelta initialDelta = new InitialDelta();
        initialDelta.setTargets(new String[] { "target1", "target2" });
        tempInstance.setPolicies(convertObjToJsonString(new Policies.Builder()
                                                                .allInitialDelta(Map.of("Payload_InitialDelta", initialDelta))
                                                                .build()));

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);

        final ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setTempInstance(tempInstance);
        changeOperationContext.setOperation(lifecycleOperation);

        // when
        whenWfsUpgradeRespondsWithAccepted();

        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, 1);

        // then
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    public void testChangeVnfPkgOperationWhenReplicaDetailsPresentTargetMismatch() throws IOException {
        // given
        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance tempInstance = getPojoFromJsonString(vnfInstance.getTempInstance());

        tempInstance.setResourceDetails(convertObjToJsonString(Map.of("target1", 2)));

        final InitialDelta initialDelta = new InitialDelta();
        initialDelta.setTargets(new String[] { "target2" });
        tempInstance.setPolicies(convertObjToJsonString(new Policies.Builder()
                                                                .allInitialDelta(Map.of("Payload_InitialDelta", initialDelta))
                                                                .build()));

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);

        final ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setTempInstance(tempInstance);
        changeOperationContext.setOperation(lifecycleOperation);

        // when
        whenWfsUpgradeRespondsWithAccepted();

        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, 1);

        // then
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    public void testChangeVnfpkgInfoOperationContextWithValuesFile() throws Exception {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "41def1ce-4cf4-477c-aab3-21c454e7777";
        Path valuesFile = TestUtils.getValuesFileCopy("combined-values-test.yaml");
        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance instance = getPojoFromJsonString(vnfInstance.getTempInstance());
        changeOperationContext.setTempInstance(instance);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);
        changeOperationContext.setOperation(lifecycleOperation);
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, valuesFile, 1);
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(valuesFile.toFile().exists()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulResponseFromRoutedWFSUpgradeWithCRD() {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "afbc35b1-e510-47bd-89ae-b1f20f7f3b09";
        final ChangeCurrentVnfPkgRequest changeVnfpkgVnfRequest = new ChangeCurrentVnfPkgRequest();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams);
        changeVnfpkgVnfRequest.setAdditionalParams(additionalParams);
        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        changeOperationContext.setOperationRequest(changeVnfpkgVnfRequest);
        changeOperationContext.setSourceVnfInstance(vnfInstance);
        changeOperationContext.setTempInstance(vnfInstance);
        changeOperationContext.setOperation(operation);
        changeOperationContext.setAdditionalParams((Map<String, Object>) changeVnfpkgVnfRequest.getAdditionalParams());
        WorkflowRoutingResponse response = workflowRoutingService.routeChangePackageInfoRequest(changeOperationContext);
        assertThat(response).isNotNull();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getInstanceId()).matches("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        assertThat(response.getLinks()).hasSize(2);
    }

    @Test
    public void testChangeVnfpkgInfoOperationContextWithCRDsWithValuesFile() throws Exception {
        whenWfsUpgradeRespondsWithAccepted();

        final String vnfInstanceId = "afbc35b1-e510-47bd-89ae-b1f20f7f3b09";
        Path valuesFile = TestUtils.getValuesFileCopy("combined-values-test.yaml");
        ChangeOperationContext changeOperationContext = new ChangeOperationContext();
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        VnfInstance instance = getPojoFromJsonString(vnfInstance.getTempInstance());
        changeOperationContext.setTempInstance(instance);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        lifecycleOperation.setApplicationTimeout("500");
        lifecycleOperationRepository.save(lifecycleOperation);
        changeOperationContext.setOperation(lifecycleOperation);
        WorkflowRoutingResponse workflowRoutingResponse = workflowRoutingService.routeToEvnfmWfsUpgrade(changeOperationContext, valuesFile, 1);
        assertThat(workflowRoutingResponse.getHttpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(valuesFile.toFile().exists()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private <T> T getPojoFromJsonString(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, (Class<T>) VnfInstance.class);
    }

    private void whenWfsTerminateRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("terminate");
    }

    private void whenWfsUpgradeRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("upgrade");
    }

    @SuppressWarnings("unchecked")
    private void whenWfsRespondsWithAccepted(final String urlFragment) {
        final var response = new ResourceResponse();
        response.setInstanceId("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        response.setLinks(Map.of("self", "http://self-link",
                                 "instance", "http://instance-link"));

        when(restTemplate.exchange(contains(urlFragment), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsRespondsWithBadRequest(final String urlFragment) {
        final var response = new ResourceResponse();
        response.setErrorDetails("[{\"message\": \"releaseName must consist of lower case alphanumeric characters\"}]");

        when(restTemplate.exchange(contains(urlFragment), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.BAD_REQUEST));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted() {
        final var response = new ResourceResponse();
        response.setInstanceId("4d2cf935-3b56-11e9-be54-02d5f77aae63_dummy_id");
        response.setLinks(Map.of("self", "http://self-link",
                                 "instance", "http://instance-link"));

        when(restTemplate.exchange(endsWith("instantiate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithBadRequest(final String errorDetails) {
        final var response = new ResourceResponse();
        response.setErrorDetails(errorDetails);

        when(restTemplate.exchange(endsWith("instantiate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.BAD_REQUEST));
    }

    private void whenWfsAdditionalResourceInfoRespondsWith(final String fileName) {
        when(restTemplate.exchange(contains("additionalResourceInfo"), eq(HttpMethod.POST), any(), eq(ComponentStatusResponseList.class)))
                .thenReturn(new ResponseEntity<>(readObject(fileName, ComponentStatusResponseList.class), HttpStatus.OK));
    }

    private void whenWfsPodsRespondsWith(final String fileName) {
        when(restTemplate.exchange(contains("pods"), eq(HttpMethod.POST), any(), eq(ComponentStatusResponseList.class)))
                .thenReturn(new ResponseEntity<>(readObject(fileName, ComponentStatusResponseList.class), HttpStatus.OK));
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readDataFromFile(getClass(), fileName), targetClass);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
