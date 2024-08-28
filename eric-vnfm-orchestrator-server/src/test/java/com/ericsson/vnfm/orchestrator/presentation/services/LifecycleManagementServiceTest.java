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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.createDuplicateResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.SITEBASIC_XML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENTITY_PROFILE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_IN_MINUTES;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.checkVnfInstanceForBashInjection;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.WORKFLOW_UNAVAILABLE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.HealRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ScaleRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.TerminateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(LifecycleManagementServiceTestConfig.class)
@MockBean(classes = {
    GrantingNotificationsConfig.class,
    LcmOpAdditionalParamsProcessor.class
})
public class LifecycleManagementServiceTest extends AbstractEndToEndTest {

    private static final String DEFAULT_TIMEOUT = "3600";
    private static final String INSTANTIATE_URL = "/api/lcm/v3/resources/%s/instantiate";
    private static final TypeReference<Map<String, Object>> ADDITIONAL_PARAMS_TYPE = new TypeReference<>() {
    };

    @TempDir
    public File temporaryFolder;

    @Autowired
    private LifeCycleManagementService lifeCycleManagementService;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private WireMockServer mockServer;

    @Autowired
    private LcmOperationsConfig lcmOperationsConfig;

    @SpyBean
    private InstantiateRequestHandler instantiateRequestHandler;

    @SpyBean
    private VnfInstanceRepository vnfInstanceRepository;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @SpyBean
    private AsyncGrantingAndOrchestrationProcessor asyncOrchestrationProcessor;

    @SpyBean
    private HealRequestHandler healRequestHandler;

    @SpyBean
    private ClusterConfigService clusterConfigService;

    @SpyBean
    WorkflowRoutingServicePassThrough workflowRoutingService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private LcmOpSearchService lcmOpSearchService;

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @MockBean
    VnfLcmOperationService vnfLcmOperationService;

    @SpyBean
    private ExtensionsMapper extensionsMapper;

    @SpyBean
    private ScaleRequestHandler scaleRequestHandler;

    @SpyBean
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @SpyBean
    private TerminateRequestHandler terminateRequestHandler;

    @MockBean
    private ExtensionsService extensionsService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Captor
    private ArgumentCaptor<ChangeOperationContext> updateVnfScaleInfoArgumentCaptor;

    @Captor
    private ArgumentCaptor<ChangeOperationContext> changeOperationContextArgumentCaptor;

    @Captor
    private ArgumentCaptor<ChangeOperationContext> ccvpContextCaptor;

    @Captor
    private ArgumentCaptor<VnfInstance> vnfInstanceArgumentCaptor;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(workflowRoutingService, "workflowHost", "localhost:" + mockServer.port());
        lcmOperationsConfig.setLcmOperationsLimit(Integer.MAX_VALUE);
    }

    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @Test
    public void testCheckForBashInjectionWithValidInstance() {
        VnfInstance instance = getVnfInstance();
        checkVnfInstanceForBashInjection(instance);
    }

    @Test
    public void testCheckForBashInjectionWithInvalidVnfInstanceIdContainingSemicolon() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId("test;echo");
        assertThatThrownBy(() -> checkVnfInstanceForBashInjection(instance)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testCheckForBashInjectionWithInvalidVnfInstanceId() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId("test|echo");
        assertThatThrownBy(() -> checkVnfInstanceForBashInjection(instance)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testCheckForBashInjectionWithInvalidVnfInstanceIdContainingDoubleAmpersand() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId("test&&echo");
        assertThatThrownBy(() -> checkVnfInstanceForBashInjection(instance)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testCheckForBashInjectionWithInvalidVnfInstanceIdContainingSingleQuote() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId("test'echo");
        assertThatThrownBy(() -> checkVnfInstanceForBashInjection(instance)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testCheckForBashInjectionWithInvalidVnfInstanceIdContainingDoubleQuote() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId("test\"echo");
        assertThatThrownBy(() -> checkVnfInstanceForBashInjection(instance)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    public void testAdditionalParamsApplicationTimeOutRandomCase() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("aPpliCatIontImeOut", 25);
        assertThat(lifeCycleManagementHelper.getApplicationTimeout(additionalParams)).isEqualTo("25");
    }

    @Test
    public void testAdditionalParamsApplicationTimeOutNormalCase() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("applicationTimeOut", 30);
        assertThat(lifeCycleManagementHelper.getApplicationTimeout(additionalParams)).isEqualTo("30");
    }

    @Test
    public void cleanupSuccessfulWithFailedInstantiateInvalidCluster() {
        VnfLcmOpOcc vnfLcmOpOcc = new VnfLcmOpOcc();
        vnfLcmOpOcc.setOperationState(VnfLcmOpOcc.OperationStateEnum.FAILED);
        vnfLcmOpOcc.setOperation(VnfLcmOpOcc.OperationEnum.INSTANTIATE);
        when(vnfLcmOperationService.getLcmOperationByOccId(anyString())).thenReturn(vnfLcmOpOcc);
        VnfInstance vnfInstanceBefore = vnfInstanceRepository.findByVnfInstanceId("45xc7s4q-4cf4-477c-aab3-21c454e6a380");
        assertThat(vnfInstanceBefore).isNotNull();
        String operationId = lifeCycleManagementService
                .cleanup(vnfInstanceBefore, new CleanupVnfRequest(), anyString());
        LifecycleOperation operationAfter = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operationAfter).isNull();
        VnfInstance vnfInstanceAfter = vnfInstanceRepository.findByVnfInstanceId("45xc7s4q-4cf4-477c-aab3-21c454e6a380");
        assertThat(vnfInstanceAfter).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulHealRequestNoBackupFileRef() throws JsonProcessingException {
        setSupportedOperationsToVnfInstance("3de67785-66e4-48c2-a97a-1500dfd66cb1");

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), anyString());
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("Full Restore");
        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "3de67785-66e4-48c2-a97a-1500dfd66cb1",
                                healVnfRequest, null, null);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        HealVnfRequest operationParams = mapper.readValue(operation.getOperationParams(), HealVnfRequest.class);
        assertThat((Map<String, String>) operationParams.getAdditionalParams()).containsOnly(entry(APPLICATION_TIME_OUT, DEFAULT_TIMEOUT));

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulHealRequestBackupFileRef() throws JsonProcessingException {
        setSupportedOperationsToVnfInstance("9095c4ee-7a5e-41ad-8898-f679b33eae0f");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "lcm-mgmt-service/test-descriptor-model.json");

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), anyString());
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("Full Restore");
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(RESTORE_BACKUP_FILE_REFERENCE, "sftp://users@14BCP04/my-backup");
        additionalParams.put(RESTORE_PASSWORD, "password");
        healVnfRequest.setAdditionalParams(additionalParams);

        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "9095c4ee-7a5e-41ad-8898-f679b33eae0f",
                                healVnfRequest, null, null);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        HealVnfRequest operationParams = mapper.readValue(operation.getOperationParams(), HealVnfRequest.class);
        assertThat(operationParams).isNotNull();
        assertThat((Map<String, String>) operationParams.getAdditionalParams()).containsOnly(entry(RESTORE_BACKUP_FILE_REFERENCE,
                                                                                                   "sftp://users@14BCP04/my-backup"),
                                                                                             entry(APPLICATION_TIME_OUT, DEFAULT_TIMEOUT),
                                                                                             entry(RESTORE_PASSWORD, "password"));
        ArgumentCaptor<LifecycleOperation> lifecycleOperationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(workflowRoutingService, timeout(2000)).routeTerminateRequest(any(), lifecycleOperationCaptor.capture(), anyString());
        LifecycleOperation actualLifecycleOperation = lifecycleOperationCaptor.getValue();
        assertThat(actualLifecycleOperation.getCombinedAdditionalParams()).isNotNull();

        Map<String, Object> combinedAdditionalParams =
                mapper.readValue(actualLifecycleOperation.getCombinedAdditionalParams(), ADDITIONAL_PARAMS_TYPE);
        assertThat(combinedAdditionalParams)
                .contains(entry(CLEAN_UP_RESOURCES, true))
                .doesNotContainKey(RESTORE_BACKUP_FILE_REFERENCE);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedHealRequestTerminateFails() throws JsonProcessingException {
        //given
        setSupportedOperationsToVnfInstance("dbdc2622-9026-4d44-8795-b0f923c710ff");

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), anyString());
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("Full restore");
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(RESTORE_BACKUP_FILE_REFERENCE, "sftp://users@14BCP04/my-backup");
        additionalParams.put(RESTORE_PASSWORD, "password");
        healVnfRequest.setAdditionalParams(additionalParams);
        //when
        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "dbdc2622-9026-4d44-8795-b0f923c710ff",
                                healVnfRequest, null, null);
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(workflowRoutingService, timeout(2000).times(1)).routeTerminateRequest(any(), any(), anyString());
        verify(databaseInteractionService, timeout(2000).times(5))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        HealVnfRequest operationParams = mapper.readValue(operation.getOperationParams(), HealVnfRequest.class);
        assertThat(operationParams).isNotNull();
        assertThat((Map<String, String>) operationParams.getAdditionalParams()).containsOnly(entry(RESTORE_BACKUP_FILE_REFERENCE,
                                                                                                   "sftp://users@14BCP04/my-backup"),
                                                                                             entry(APPLICATION_TIME_OUT, DEFAULT_TIMEOUT),
                                                                                             entry(RESTORE_PASSWORD, "password"));

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulHealRequestDisableSupervisionFails() throws JsonProcessingException {
        //given
        setSupportedOperationsToVnfInstance("69f5b897-daea-4f93-bc11-2b14c728ed7c");

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), anyString());
        doThrow(new FileExecutionException("disable failed")).when(ossNodeService)
                .setAlarmSuperVisionInENM(any(), eq(EnmOperationEnum.DISABLE_ALARM_SUPERVISION));
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("full Restore");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(RESTORE_BACKUP_FILE_REFERENCE, "sftp://users@14BCP04/my-backup");
        additionalParams.put(RESTORE_PASSWORD, "password");
        healVnfRequest.setAdditionalParams(additionalParams);
        LocalDateTime beforeOperation = LocalDateTime.now();
        //when
        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "69f5b897-daea-4f93-bc11-2b14c728ed7c",
                                healVnfRequest, null, null);
        //then
        verify(ossNodeService, timeout(2000).times(1)).setAlarmSuperVisionInENM(any(), eq(EnmOperationEnum.DISABLE_ALARM_SUPERVISION));
        verify(workflowRoutingService, timeout(2000).times(1)).routeTerminateRequest(any(), any(), anyString());

        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService, timeout(2000).times(4))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getStateEnteredTime()).isAfter(beforeOperation);

        HealVnfRequest operationParams = mapper.readValue(operation.getOperationParams(), HealVnfRequest.class);
        assertThat(operationParams).isNotNull();
        assertThat((Map<String, String>) operationParams.getAdditionalParams()).containsOnly(entry(RESTORE_BACKUP_FILE_REFERENCE,
                                                                                                   "sftp://users@14BCP04/my-backup"),
                                                                                             entry(APPLICATION_TIME_OUT, DEFAULT_TIMEOUT),
                                                                                             entry(RESTORE_PASSWORD, "password"));

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void successfulUpgradeRequestDisableSupervisionFails() throws JsonProcessingException {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("f3def1ce-4cf4-477c-aab3-21c454e6a389", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doThrow(new FileExecutionException("disable failed")).when(ossNodeService)
                .setAlarmSuperVisionInENM(any(), eq(EnmOperationEnum.DISABLE_ALARM_SUPERVISION));
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId("f3def1ce-4cf4-477c-aab3-21c454e6a389");
        LocalDateTime beforeOperation = LocalDateTime.now();

        //when
        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, "c22ac5f7-7065-49b1-831c-d687130c6123",
                                changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(ossNodeService, timeout(2000).times(1)).setAlarmSuperVisionInENM(any(), eq(EnmOperationEnum.DISABLE_ALARM_SUPERVISION));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(operation.getStateEnteredTime()).isAfter(beforeOperation);

        ChangeCurrentVnfPkgRequest operationParams = mapper.readValue(operation.getOperationParams(), ChangeCurrentVnfPkgRequest.class);
        assertThat(operationParams).isNotNull();

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void successfulUpgradeRequestDisableSupervision() throws JsonProcessingException {
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("f3def1ce-4cf4-477c-aab3-21c454e6a389", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId("f3def1ce-4cf4-477c-aab3-21c454e6a389");
        LocalDateTime beforeOperation = LocalDateTime.now();

        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, "a98drf7-7065-49b1-831c-d687130c6123",
                                changeCurrentVnfPkgRequest, null, new HashMap<>());

        verify(ossNodeService, timeout(2000).times(1)).setAlarmSuperVisionInENM(any(), eq(EnmOperationEnum.DISABLE_ALARM_SUPERVISION));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(operation.getStateEnteredTime()).isAfter(beforeOperation);

        ChangeCurrentVnfPkgRequest operationParams = mapper.readValue(operation.getOperationParams(), ChangeCurrentVnfPkgRequest.class);
        assertThat(operationParams).isNotNull();

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testPerformInstantiateForWFSUnavailable() {
        //given
        String vnfInstanceId = "r5def1ce-4cf4-477c-aab3-21c454e6666";
        mockWorkflowCreateResourceWfsUnavailable("some-release-name");
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn(new PackageResponse()).when(packageService).getPackageInfo(anyString());
        doReturn(new JSONObject()).when(packageService).getVnfd(any());

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName("testinstantiate");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put("helm_client_version", "3.8");
        additionalParams.put(NAMESPACE, "testinstantiate");
        request.setAdditionalParams(additionalParams);
        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, request, null, new HashMap<>());
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(databaseInteractionService, timeout(5000).atLeast(3))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        verify(databaseInteractionService, timeout(2000).times(1)).releaseNamespaceDeletion(any());
        verify(instantiateRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation afterInstantiate = lifecycleOperationArgumentCaptor.getValue();
        assertThat(afterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testPerformInstantiateClusterStaysInInUseStateForWFSUnavailable() {
        //given
        String vnfInstanceId = "r5def1ce-4cf4-477c-aab3-21c454e6666";
        String clusterName = "testinstantiate";
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(clusterName);
        clusterConfigFile.setContent("content");

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName("testinstantiate");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put("helm_client_version", "3.8");
        additionalParams.put(NAMESPACE, "testinstantiatetest");
        request.setAdditionalParams(additionalParams);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(new PackageResponse()).when(packageService).getPackageInfo(anyString());
        doReturn(new JSONObject()).when(packageService).getVnfd(any());
        when(clusterConfigService.getOrDefaultConfigFileByName(ArgumentMatchers.anyString())).thenReturn(clusterConfigFile);
        mockWorkflowCreateResourceWfsUnavailable("some-release-name");

        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, request, null, new HashMap<>());
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(instantiateRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation afterInstantiate = lifecycleOperationArgumentCaptor.getValue();
        assertThat(afterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        clusterConfigFile = clusterConfigService.getConfigFileByName(instance.getClusterName());

        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.IN_USE);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testPerformHealForWFSBadRequest() throws Exception {
        //given
        setSupportedOperationsToVnfInstance("3f4becee-27b5-11ed-a261-0242ac120002");

        String vnfInstanceId = "3f4becee-27b5-11ed-a261-0242ac120002";
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage("Workflow service is unavailable or unable to process the request");
        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), any());
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        HealVnfRequest request = new HealVnfRequest();
        request.setCause("full restore");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put(NAMESPACE, "heal-namespace");
        request.setAdditionalParams(additionalParams);
        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.HEAL, vnfInstanceId, request, null, null);
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(databaseInteractionService, timeout(2000).times(1)).releaseNamespaceDeletion(any());
        verify(databaseInteractionService, timeout(5000).atLeast(3))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation afterHeal = lifecycleOperationArgumentCaptor.getValue();
        assertThat(afterHeal.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        ProblemDetails problemDetails = mapper.readValue(afterHeal.getError(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .isEqualTo("Workflow service is unavailable or unable to process the request");

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testPerformScaleForWFSBadRequest() throws Exception {
        //given
        whenOnboardingRespondsWithVnfd("9392468011745350001", "lcm-mgmt-service/test-vnfd.json");

        final String vnfId = "d8a8da6b-4488-4b14-a578-38b4f989";
        setSupportedOperationsToVnfInstance(vnfId);
        final ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("Payload", 3, ScaleVnfRequest.TypeEnum.OUT);
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage("Workflow service is unavailable or unable to process the request");
        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        doReturn(response).when(workflowRoutingService).routeScaleRequest(any(), any(), any(ScaleVnfRequest.class));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(0);
        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.SCALE, vnfId, scaleRequest, null, null);
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(databaseInteractionService, timeout(2000).atLeastOnce())
                .persistVnfInstanceAndOperation(any(VnfInstance.class), any(LifecycleOperation.class));
        verify(databaseInteractionService, times(0)).releaseNamespaceDeletion(any());
        verify(databaseInteractionService, timeout(2000).atLeast(3))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        verify(scaleRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation afterScale = lifecycleOperationArgumentCaptor.getValue();
        assertThat(afterScale.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        ProblemDetails problemDetails = mapper.readValue(afterScale.getError(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .isEqualTo("Workflow service is unavailable or unable to process the request");
        assertThat(problemDetails.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    public void testScaleWithNoPolicies() {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setSupportedOperations(createSupportedOperations(LCMOperationsEnum.SCALE));
        final VnfInstance persistedVnfInstance = vnfInstanceRepository.save(vnfInstance);
        final ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("test", 3,
                                                                          ScaleVnfRequest.TypeEnum.IN);
        String vnfInstanceId = persistedVnfInstance.getVnfInstanceId();
        assertThatThrownBy(() -> lifeCycleManagementService
                .executeRequest(LifecycleOperationType.SCALE, vnfInstanceId, scaleRequest, null, new HashMap<>())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scale not supported as " +
                                    "policies not present for instance " + persistedVnfInstance.getVnfInstanceId());
    }

    @Test
    public void testScale() {
        final String vnfId = "d8a8da6b-4488-4b14-a578-38b4f989";
        setSupportedOperationsToVnfInstance(vnfId);
        whenOnboardingRespondsWithVnfd("9392468011745350001", "lcm-mgmt-service/test-vnfd.json");

        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeScaleRequest(any(), any(), any());
        final ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("Payload", 3,
                                                                          ScaleVnfRequest.TypeEnum.OUT);
        lifeCycleManagementService.executeRequest(LifecycleOperationType.SCALE, vnfId, scaleRequest, null, null);
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(instance
                                                                                                        .getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testChangePackageInfo() throws Exception {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da6", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");

        final String vnfId = "d8a8da6";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        //when
        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(asyncOrchestrationProcessor, timeout(2000).times(1))
                .process(any(), any(), any(VnfInstance.class), any(LifecycleOperation.class), any());
        verify(databaseInteractionService, timeout(2000).atLeast(3))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(any(), any(), anyInt());

        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        Map<String, Object> inputParams = checkAndCastObjectToMap(changeCurrentVnfPkgRequest.getAdditionalParams());
        Map<String, Object> actualParams = checkAndCastObjectToMap(
                mapper.readValue(operation.getOperationParams(), ChangeCurrentVnfPkgRequest.class).getAdditionalParams());

        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(actualParams.size()).isGreaterThan(inputParams.size());
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testUpdateScaleInfo() {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da6b-4488-4b14", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "lcm-mgmt-service/test-vnfd.json");

        final String vnfId = "d8a8da6b-4488-4b14";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PERSIST_SCALE_INFO, true);
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParams);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();

        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(updateVnfScaleInfoArgumentCaptor.capture(), any(), anyInt());
        //then complicated
        ChangeOperationContext actualUpdateVnfScaleInfoVnfInstance = updateVnfScaleInfoArgumentCaptor.getValue();
        int scaleLevel = 0;
        int expectedScaleLevel = 2;
        for (ScaleInfoEntity scaleInfoEntity : actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getScaleInfoEntity()) {
            if (scaleInfoEntity.getAspectId().equals(PAYLOAD)) {
                scaleLevel = scaleInfoEntity.getScaleLevel();
            }
        }
        Assertions.assertEquals(expectedScaleLevel, scaleLevel);
        List<HelmChart> helmCharts = actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(3);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get("PL__scaled_vm");
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(9);
        assertFalse(plScaledVm.getAutoScalingEnabledValue());
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(9);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(9);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get("CL_scaled_vm");
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(11);
        assertFalse(clScaledVm.getAutoScalingEnabledValue());
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(11);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(11);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get("TL_scaled_vm");
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertFalse(tlScaledVm.getAutoScalingEnabledValue());
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testUpdateScaleInfoForScaleDetailsForVnfInstance() {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da6b-4488-4b14", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "lcm-mgmt-service/test-vnfd.json");

        final String vnfId = "d8a8da6b-4488-4b14";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PERSIST_SCALE_INFO, true);
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParams);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        Optional<OperationInProgress> operationInProgress = Optional.empty();
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        when(operationsInProgressRepository.findByVnfId(vnfId)).thenReturn(operationInProgress);
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(0);
        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(updateVnfScaleInfoArgumentCaptor.capture(), any(), anyInt());
        //then complicated
        ChangeOperationContext actualUpdateVnfScaleInfoVnfInstance = updateVnfScaleInfoArgumentCaptor.getValue();

        int scaleLevel = 0;
        int expectedScaleLevel = 2;
        for (ScaleInfoEntity scaleInfoEntity : actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getScaleInfoEntity()) {
            if (scaleInfoEntity.getAspectId().equals(PAYLOAD)) {
                scaleLevel = scaleInfoEntity.getScaleLevel();
            }
        }
        Assertions.assertEquals(expectedScaleLevel, scaleLevel);
        List<HelmChart> helmCharts = actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(3);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get("PL__scaled_vm");
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(9);
        assertFalse(plScaledVm.getAutoScalingEnabledValue());
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(9);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(9);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get("CL_scaled_vm");
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(11);
        assertFalse(clScaledVm.getAutoScalingEnabledValue());
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(11);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(11);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get("TL_scaled_vm");
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertFalse(tlScaledVm.getAutoScalingEnabledValue());
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testUpdateScaleInfoForWithNoPolicyInVnfd() {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("no-scaling", "lcm-mgmt-service/no-scaling-descriptor-model.json");

        final String vnfId = "d8a8da6b-44101";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId("no-scaling");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PERSIST_SCALE_INFO, true);
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParams);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(updateVnfScaleInfoArgumentCaptor.capture(), anyInt());
        ChangeOperationContext actualUpdateVnfScaleInfoVnfInstance = updateVnfScaleInfoArgumentCaptor.getValue();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getScaleInfoEntity()).isEmpty();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getPolicies()).isNull();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getResourceDetails()).isNull();

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testUpdateScaleInfoForWithNoPolicyInVnfdAndVnfInstancePolicyNotNull() {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("no-scaling", "lcm-mgmt-service/no-scaling-descriptor-model.json");

        final String vnfId = "d8a8da6b-44102";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId("no-scaling");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PERSIST_SCALE_INFO, true);
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParams);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(updateVnfScaleInfoArgumentCaptor.capture(), anyInt());
        ChangeOperationContext actualUpdateVnfScaleInfoVnfInstance = updateVnfScaleInfoArgumentCaptor.getValue();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getScaleInfoEntity()).isEmpty();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getPolicies()).isNull();
        assertThat(actualUpdateVnfScaleInfoVnfInstance.getTempInstance().getResourceDetails()).isNull();

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testChangePackageInfoWithoutEvnfmParameters() throws Exception {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da7", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");

        final String vnfId = "d8a8da7";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        Map<String, String> additionalParameter = new HashMap<>();
        additionalParameter.put("param1", "value1");
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParameter);
        String stringAdditionalParam = mapper.writeValueAsString(additionalParameter);
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        //when
        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(workflowRoutingService, timeout(2000).times(1))
                .routeChangePackageInfoRequest(changeOperationContextArgumentCaptor.capture(), any(), anyInt());
        verify(databaseInteractionService, timeout(2000).atLeast(3))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        verify(changeVnfPackageRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();

        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);

        Map<String, Object> inputOperationAdditionalParams = new HashMap<>();
        inputOperationAdditionalParams.put("applicationTimeOut", "3600");
        inputOperationAdditionalParams.put("param1", "value1");
        Map<String, Object> actualOperationAdditionalParams = checkAndCastObjectToMap(
                mapper.readValue(operation.getOperationParams(), ChangeCurrentVnfPkgRequest.class).getAdditionalParams());

        assertThat(actualOperationAdditionalParams).hasSizeGreaterThan(inputOperationAdditionalParams.size());
        assertThat(operation.getValuesFileParams().length()).isGreaterThan(stringAdditionalParam.length());
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);
        ChangeOperationContext workflowOperationContext = changeOperationContextArgumentCaptor.getValue();
        final String combinedAdditionalParams = mapper.writeValueAsString(workflowOperationContext.getAdditionalParams());
        assertThat(combinedAdditionalParams).isEqualTo("{\"applicationTimeOut\":\"3600\"}");

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testChangePackageInfoWithEvnfmParameters() throws Exception {
        //given
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da8", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");

        final String vnfId = "d8a8da8";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        Map<String, String> additionalParameter = new HashMap<>();
        additionalParameter.put("param1", "value1");
        additionalParameter.put("helmNoHooks", "true");
        additionalParameter.put("applicationTimeOut", "500");
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParameter);
        String inputValuesString = "{\"param1\":\"value1\"}";
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(createVnfdResponse()).when(packageService).getVnfd(anyString());

        //when
        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>());
        //then
        verify(workflowRoutingService, timeout(2000).times(1)).routeChangePackageInfoRequest(
                ccvpContextCaptor.capture(), any(), anyInt()
        );
        LifecycleOperation operation = ccvpContextCaptor.getValue().getOperation();
        Map<String, Object> inputParams = checkAndCastObjectToMap(changeCurrentVnfPkgRequest.getAdditionalParams());
        Map<String, Object> actualParams = checkAndCastObjectToMap(
                mapper.readValue(operation.getOperationParams(), ChangeCurrentVnfPkgRequest.class).getAdditionalParams());

        VnfInstance vnfInstance = operation.getVnfInstance();
        VnfInstance upgradedInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(actualParams).hasSizeGreaterThan(inputParams.size());
        assertThat(operation.getValuesFileParams().length()).isGreaterThan(inputValuesString.length());
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);
        assertThat(upgradedInstance.getCombinedAdditionalParams()).isEqualTo("{\"applicationTimeOut\":\"500\",\"helmNoHooks\":\"true\"}");

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testChangePackageInfoWithEvnfmParametersAdditionalParamsContainsNull() {
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        whenOnboardingRespondsWithDescriptor("d8a8da8", "lcm-mgmt-service/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "lcm-mgmt-service/test-descriptor-model.json");
        doReturn(0).when(databaseInteractionService).getOperationsCountNotInTerminalStatesByVnfInstance(any());

        final String vnfId = "d8a8da8";
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
        changeCurrentVnfPkgRequest.setVnfdId(vnfId);
        Map<String, String> additionalParameter = new HashMap<>();
        additionalParameter.put("param1", null);
        additionalParameter.put("helmNoHooks", null);
        additionalParameter.put("applicationTimeOut", "500");
        changeCurrentVnfPkgRequest.setAdditionalParams(additionalParameter);

        WorkflowRoutingResponse response = TestUtils.createResponse(vnfId, null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeChangePackageInfoRequest(any(), anyInt());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();


        assertThatThrownBy(() -> lifeCycleManagementService
                .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfId, changeCurrentVnfPkgRequest, null, new HashMap<>()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You cannot merge yaml where value is null for helmNoHooks, param1");
    }

    @Test
    public void testPerformInstantiateWithValidSitebasicFile() throws Exception {
        String vnfInstanceId = "alksjvcn1-7065-49b1-831c-d687130c6123";
        WorkflowRoutingResponse response = TestUtils.createResponse(vnfInstanceId, null, HttpStatus.ACCEPTED);
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn(response).when(workflowRoutingService).routeInstantiateRequest(any(), any(), any(InstantiateVnfRequest.class), any());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(new JSONObject()).when(packageService).getVnfd(any());

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        String sitebasic = TestUtils.readDataFromFile("node.xml");
        additionalParams.put(SITEBASIC_XML, sitebasic);
        additionalParams.put("helm_client_version", "3.8");
        request.setAdditionalParams(additionalParams);
        String lifecycleId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, request, null, new HashMap<>());
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(instance.getSitebasicFile())
                .isNotNull()
                .contains("<nodeFdn>VPP00001</nodeFdn>")
                .contains("<enrollmentMode>CMPv2_INITIAL</enrollmentMode>");

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void testPerformInstantiateWithInvalidSitebasicFile() {
        String vnfInstanceId = "oldi87ed-7065-49b1-831c-d687130c6123";
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(new JSONObject()).when(packageService).getVnfd(any());
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.8");
        additionalParams.put(SITEBASIC_XML, "<Nodes>\n<Node>\n<nodeFdn>VPP00001/nodeFdn>");
        request.setAdditionalParams(additionalParams);
        assertThatThrownBy(() -> lifeCycleManagementService
                .executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, request, null, new HashMap<>()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Invalid XML format:: Error when converting");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void healSuccessfulWithOssNodeProtocolFileAdded() throws URISyntaxException, IOException {
        setSupportedOperationsToVnfInstance("ok98uyh6-4cf4-477c-aab3-21c454e6666");

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), anyString());
        Path ossNodeProtocolFile = createDuplicateResource("ossNodeProtocolvDU.xml", temporaryFolder);
        when(ossNodeService.generateOssNodeProtocolFile(any(), any())).thenReturn(TestUtils.readDataFromFile(ossNodeProtocolFile));
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("full Restore");
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(IP_VERSION, "ipV6");
        healVnfRequest.setAdditionalParams(additionalParams);

        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "ok98uyh6-4cf4-477c-aab3-21c454e6666",
                                healVnfRequest, null, null);

        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(ossNodeService, times(1)).generateOssNodeProtocolFile(any(), any());
        verify(healRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getOperationOccurrenceId()).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        HealVnfRequest operationParams = mapper.readValue(operation.getOperationParams(), HealVnfRequest.class);
        assertThat(operationParams).isNotNull();
        assertThat((Map<String, String>) operationParams.getAdditionalParams()).contains(entry(IP_VERSION, "ipV6"));

        verify(workflowRoutingService).routeTerminateRequest(any(), lifecycleOperationArgumentCaptor.capture(), anyString());
        operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getCombinedAdditionalParams()).isNotNull();

        Map<String, Object> combinedAdditionalParams = mapper.readValue(operation.getCombinedAdditionalParams(), ADDITIONAL_PARAMS_TYPE);
        assertThat(combinedAdditionalParams)
                .contains(entry(CLEAN_UP_RESOURCES, true))
                .doesNotContainKey(IP_VERSION);

        VnfInstance vnfInstanceAfter = vnfInstanceArgumentCaptor.getValue();
        assertThat(vnfInstanceAfter.getVnfInstanceId()).isEqualTo("ok98uyh6-4cf4-477c-aab3-21c454e6666");
        assertThat(vnfInstanceAfter.getOssNodeProtocolFile()).isNotNull();
        assertThat(vnfInstanceAfter.getOssNodeProtocolFile())
                .contains("<bind-dn>cn=ProxyAccount_3,ou=proxyagent,ou=com,dc=rani-venm-1,dc=com</bind-dn>")
                .contains("<rpc message-id=\"Close Session\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
                .contains("<rpc message-id=\"OAM Trust Store\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
                .contains("<truststore xmlns=\"urn:ietf:params:xml:ns:yang:ietf-truststore\">")
                .contains("<capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>");

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @Test
    public void healFailsWithInvalidConfig0() {
        setSupportedOperationsToVnfInstance("123po8ys-4cf4-477c-aab3-21c454e6666");
        doThrow(new InternalRuntimeException("Failed to download enrollmentConfigurationFile")).when(ossNodeService)
                .generateOssNodeProtocolFile(any(), any());
        HealVnfRequest healVnfRequest = new HealVnfRequest();
        healVnfRequest.setCause("full Restore");
        assertThatThrownBy(() -> lifeCycleManagementService
                .executeRequest(LifecycleOperationType.HEAL, "123po8ys-4cf4-477c-aab3-21c454e6666",
                                healVnfRequest, null, new HashMap<>())).isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void shouldFailInstantiateIfNoEnabledHelmChartsFound() {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setDeployableModulesSupported(true);
        instance.setSupportedOperations(createSupportedOperations(LCMOperationsEnum.INSTANTIATE));
        instance.setVnfDescriptorId("d8a8da6b-4488-4b14");
        HelmChart disabledChart = getHelmChart("disabled-chart", "disabled-release", instance, 1, HelmChartType.CNF);
        disabledChart.setChartEnabled(false);
        instance.setHelmCharts(Collections.singletonList(disabledChart));
        instance.setOssTopology("{}");
        final VnfInstance vnfInstance = vnfInstanceRepository.save(instance);

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName("testinstantiate");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put("helm_client_version", "3.8");
        additionalParams.put(NAMESPACE, "testinstantiate");
        request.setAdditionalParams(additionalParams);
        request.setExtensions(TestUtils.createExtensionsWithDeployableModules());

        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(Collections.emptyMap()).when(extensionsMapper).getDeployableModulesValues(anyString());
        doReturn(new JSONObject()).when(packageService).getVnfd(any());
        whenOnboardingRespondsWithDescriptor("d8a8da6b-4488-4b14", "lcm-mgmt-service/test-descriptor-model.json");

        NotFoundException exception = assertThrows(NotFoundException.class,
                                                   () -> lifeCycleManagementService.executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstance.getVnfInstanceId(),
                                                                                                   request, null, new HashMap<>()));
        String expectedExceptionMessage = String.format("No unprocessed Helm Chart has been found in VNF Instance with Id : %s.",
                                                        vnfInstance.getVnfInstanceId());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testOssParamsRemovedFromValuesFile() throws IOException, URISyntaxException {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setDeployableModulesSupported(true);
        instance.setSupportedOperations(createSupportedOperations(LCMOperationsEnum.INSTANTIATE));
        instance.setOssTopology("{}");
        final VnfInstance vnfInstance = vnfInstanceRepository.save(instance);

        WorkflowRoutingResponse response = TestUtils.createResponse(vnfInstance.getVnfInstanceId(), null, HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeInstantiateRequest(any(), any(), any(InstantiateVnfRequest.class), any());
        doReturn(getHelmVersionsResponse()).when(workflowRoutingService).getHelmVersionsRequest();
        doReturn(new JSONObject()).when(packageService).getVnfd(any());
        doReturn(createPackageResponse()).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn("ldap-config").when(ossNodeService).generateLDAPServerConfiguration(any(), any());
        doReturn("certm-config").when(ossNodeService).generateCertificateEnrollmentConfiguration(any());

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        String sitebasic = TestUtils.readDataFromFile("node.xml");
        additionalParams.put(SITEBASIC_XML, sitebasic);
        additionalParams.put("helm_client_version", "3.8");
        additionalParams.put(ADD_NODE_TO_OSS, Boolean.TRUE.toString());
        additionalParams.put(CMP_V2_ENROLLMENT, Boolean.TRUE.toString());
        additionalParams.put(ENTITY_PROFILE_NAME, "profileName");
        additionalParams.put(OTP_VALIDITY_PERIOD_IN_MINUTES, 100);
        request.setAdditionalParams(additionalParams);

        lifeCycleManagementService.executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstance.getVnfInstanceId(), request, null, new HashMap<>());

        ArgumentCaptor<LifecycleOperation> operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> valuesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(asyncOrchestrationProcessor, timeout(2000))
                .process(any(), any(), any(VnfInstance.class), operationCaptor.capture(), valuesCaptor.capture());

        assertThat(operationCaptor.getValue().getValuesFileParams())
                .doesNotContain(ADD_NODE_TO_OSS, CMP_V2_ENROLLMENT, ENTITY_PROFILE_NAME, OTP_VALIDITY_PERIOD_IN_MINUTES);
        assertThat(valuesCaptor.getValue())
                .doesNotContainKeys(ADD_NODE_TO_OSS, CMP_V2_ENROLLMENT, ENTITY_PROFILE_NAME, OTP_VALIDITY_PERIOD_IN_MINUTES);

        verify(databaseInteractionService, timeout(1000)).persistVnfInstanceAndOperation(any(), any());
    }

    @NotNull
    public static VnfInstance getVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfDescriptorId("someId");
        instance.setVnfProviderName("test");
        instance.setVnfProductName("dummy_name");
        instance.setVnfInstanceName("name");
        instance.setVnfInstanceDescription("dummy_description");
        instance.setVnfSoftwareVersion("1.0");
        instance.setVnfdVersion("1.0");
        instance.setVnfPackageId("123456");
        instance.setClusterName("my-cluster");
        instance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        return instance;
    }

    private void mockWorkflowCreateResourceWfsUnavailable(String releaseName) {
        String expectedResourceResponse = String.format(
                "{\"httpStatus\":\"%1$s\",\"errorMessage\":\"%2$s\"}",
                HttpStatus.SERVICE_UNAVAILABLE, WORKFLOW_UNAVAILABLE);
        mockWorkflowCreateResourceEndpoint(HttpStatus.SERVICE_UNAVAILABLE, expectedResourceResponse, releaseName);
    }

    private void mockWorkflowCreateResourceEndpoint(HttpStatus status, String expectedResponse, String releaseName) {
        String expectedUrl = String.format(INSTANTIATE_URL, releaseName);
        mockServer.stubFor(post(urlPathEqualTo(expectedUrl)).willReturn(
                aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(status.value())
                        .withBody(expectedResponse)));
    }

    private void whenOnboardingRespondsWithDescriptor(final String packageId, final String fileName) {
        when(packageService.getPackageInfoWithDescriptorModel(eq(packageId))).thenReturn(readObject(fileName, PackageResponse.class));
    }

    private void whenOnboardingRespondsWithVnfd(final String vnfdId, final String fileName) {
        when(packageService.getVnfd(eq(vnfdId))).thenReturn(new JSONObject(readFile(fileName)));
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readFile(fileName), targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }

    private void setSupportedOperationsToVnfInstance(String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        vnfInstance.setSupportedOperations(buildSupportedOperations());
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    private List<OperationDetail> buildSupportedOperations() {
        OperationDetail instantiateOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.INSTANTIATE.getOperation());
        OperationDetail scaleOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.SCALE.getOperation());
        OperationDetail ccvpOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.CHANGE_VNFPKG.getOperation());
        OperationDetail healOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.HEAL.getOperation());
        OperationDetail terminateOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.TERMINATE.getOperation());
        OperationDetail rollbackOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.ROLLBACK.getOperation());
        OperationDetail modifyOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.MODIFY_INFO.getOperation());
        OperationDetail syncOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.SYNC.getOperation());
        return List.of(instantiateOperation, scaleOperation, ccvpOperation, healOperation, terminateOperation,
                       rollbackOperation, modifyOperation, syncOperation);
    }


    private HelmVersionsResponse getHelmVersionsResponse() {
        List<String> helmVersions = Arrays.asList("3.8", "3.10", "latest");

        HelmVersionsResponse helmVersionsResponse = new HelmVersionsResponse();
        helmVersionsResponse.setHelmVersions(helmVersions);

        return helmVersionsResponse;
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "lcm-mgmt-service/descriptor-model.json");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }

    private JSONObject createVnfdResponse() {
        return new JSONObject(TestUtils.readDataFromFile(getClass(), "lcm/request/change-vnfpkg/test-vnfd.json"));
    }
}
