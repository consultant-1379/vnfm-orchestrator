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
package com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest.TerminationTypeEnum.FORCEFUL;
import static com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest.TerminationTypeEnum.GRACEFUL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.TENANT_NAME;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Strings;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.e2e.util.AwaitHelper;
import com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.AddNodeToVnfInstanceByIdRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageInfoVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TaskName;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionResponse;
import com.ericsson.vnfm.orchestrator.model.entity.CheckpointType;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationStage;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.AsyncGrantingAndOrchestrationProcessorImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.TerminateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.bro.BroHttpRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
public class VnfInstancesControllerImplIntegrationTest extends AbstractEndToEndTest {

    public static final String REST_URL = "/vnflcm/v1";
    public static final String REST_URL_VNFS_ID = "/vnf_instances/";
    public static final String REST_URL_VNFS = "/vnf_instances";
    public static final String REST_URL_LCM_OPS = "/vnf_lcm_op_occs/";
    public static final String REST_URL_LCM_OPS_ALL = "/vnf_lcm_op_occs";
    public static final String INSTANTIATE = "/instantiate";
    public static final String HEAL = "/heal";
    public static final String TERMINATE = "/terminate";
    public static final String SCALE = "/scale";
    public static final String CHANGE_PACKAGE_INFO = "/change_package_info";
    public static final String CHANGE_VNFPKG = "/change_vnfpkg";
    public static final String ROLLBACK_OPS = "/rollback";
    public static final String FAIL_OPS = "/fail";
    public static final String CLUSTER_CONFIG = "/clusterconfigs";
    public static final String CLEAN_UP = "/cleanup";
    public static final String SYNC = "/sync";
    public static final String REST_URL_ADD_NODE = "/addNode";
    public static final String REST_URL_DELETE_NODE = "/deleteNode";
    public static final String BACKUP_ACTION = "/backups";
    private static final String DB_VNF_ID_1 = "ef1ce-4cf4-477c-aab3-21c454e6a379";
    private static final String DB_VNF_ID_3 = "f3def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_4 = "d3def1ce-4cf4-477c-aab3-214jx84e6a379";
    private static final String DB_VNF_ID_5 = "5874ae7d-0324-456f-a118-5aa1e4a0cd56";
    private static final String DB_VNF_ID_7 = "20def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_8 = "21def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_9 = "30def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_10 = "41def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_11 = "4cf4-477c-aab3-21c454e6a379";
    private static final String DB_VNF_ID_12 = "f3def1ce-4cf4-aab3-21c454e6a389";
    private static final String DB_VNF_ID_13 = "f3def1ce-4cf4-aab3-21c454e6a389rt56333333";
    private static final String DB_VNF_ID_NULL_OPERATION = "d3def1ce-4cf4-477c-aab3-nooperation";
    private static final String DB_VNF_ID_LAST_OPERATION_NOT_INSTANTIATE = "00000-4cf4-477c-aab3-21c454e6a380";
    private static final String DB_VNF_ID_14 = "e3def1ce-4cf4-477c-aab3-21c454eabcd";
    private static final String DB_VNF_ID_15 = "e3def1ce-4cf4-477c-aab3-21c454efghi";
    private static final String DB_VNF_ID_16 = "r3f1ce-4cf4-477c-aab3-21c454e6a379";
    private static final String DB_VNF_ID_17 = "wf1ce-4cf4-477c-aab3-21c454e6a374";
    private static final String DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE = "clean-4cf4-477c-aab3-21c454e6a380";
    private static final String DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE_1 = "11111-4cf4-477c-aab3-21c454e6a380";
    private static final String DB_VNF_ID_18 = "a1b1f1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_19 = "a2b2f1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_20 = "a3b3f1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_21 = "a4b4f1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_22 = "a5b5f1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_ID_23 = "e3def1ce-4cf4-477c-ahb3-61c454e6a344";
    private static final String DB_VNF_ID_24 = "a4d879fb-b4d6-4d2d-bfcd-a51af941c326";
    private static final String DB_VNF_ID_25 = "g7def1ce-4cf4-477c-ahb3-61c454e6a344";
    private static final String DB_VNF_ID_26 = "81f11d1e-4cf4-477c-aab3-21c454e6a380";
    private static final String DB_VNF_ID_27 = "wf1ce-rd45-477c-vnf0-snapshot004";
    private static final String DB_VNF_ID_28 = "wf1ce-rd45-477c-vnf0-backup003";
    private static final String DB_VNF_ID_29 = "wf1ce-rd45-477c-vnf0-backup007";
    private static final String DB_VNF_ID_30 = "r8def1ce-4cf4-477c-ahb3-61c454e6a344";
    private static final String DB_VNF_ID_31 = "r9def1ce-4cf4-477c-ahb3-61c454e6a344";
    private static final String DB_VNF_ID_32 = "wfrd1-rd45-477c-vnf0-snapshot004";
    private static final String DB_VNF_ID_33 = "cm3plh4q-ileo-m43t-j3q9-7k84rre970vt";

    private static final String NON_EXISTENT_VNF_ID = "123456789";
    private static final String BAD_VNF = "h3def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String DB_VNF_VNFDID = "d3def1ce-4cf4-477c-aab3-21cb04e6a378";
    private static final String DB_VNF_VNFDID_MULTI_MCIOP = "rel4-1ce-4cf4-477c-aab3-21cb04e6a380";
    private static final String DB_VNF_CLUSTER_NAME = "my-cluster";
    private static final String SAMPLE_VNF_DESCRIPTION = "create-vnf-instance-test-description";
    private static final String SAMPLE_VNF_NAME = "create-vnf-instance-test";
    private static final String BRO_ENDPOINT_URL = "bro_endpoint_url";

    private static final String VDU_SPIDER = "Spider_VDU";
    private static final String OS_CONTAINER_SPIDER_1 = "Spider_Container_1";
    private static final String OS_CONTAINER_SPIDER_2 = "Spider_Container_2";
    private static final String STORAGE_SPIDER = "Spider_Storage";
    private static final int RABBIT_MQ_PORT = 5672;
    private static final String TENANT_NAME_ECM_123 = "ecm123";
    private static final String TENANT_NAME_ECM = "ECM";
    private static final int APP_DEFAULT_TIME_OUT = 3600;
    private static final int APP_TIME_OUT = 25;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private OssNodeService ossNodeService;

    @MockBean
    private SshHelper sshHelper;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @MockBean
    private BroHttpRoutingClient broHttpRoutingClient;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @MockBean
    private LcmOpAdditionalParamsProcessor lcmOpAdditionalParamsProcessor;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @SpyBean
    private AsyncGrantingAndOrchestrationProcessorImpl asyncOrchestrationProcessor;

    @MockBean
    private PackageService packageService;

    @MockBean
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor;

    @Captor
    private ArgumentCaptor<VnfInstance> vnfInstanceArgumentCaptor;

    @Captor
    private ArgumentCaptor<ChangeOperationContext> changeOperationContextArgumentCaptor;

    @SpyBean
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private AwaitHelper awaitHelper;

    @Autowired
    private LcmOperationsConfig lcmOperationsConfig;

    @SpyBean
    private TerminateRequestHandler terminateRequestHandler;

    @SpyBean
    private InstanceService instanceService;

    @BeforeEach
    public void setup() {
        when(restTemplate.exchange(contains("helm/versions"), eq(HttpMethod.GET), any(), any(Class.class)))
                .thenReturn(ResponseEntity.ok(getHelmVersionsResponse()));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());
        when(packageService.getPackageInfo(any())).thenReturn(createPackageResponse());
        lcmOperationsConfig.setLcmOperationsLimit(Integer.MAX_VALUE);
    }

    @Test
    public void testCreateVnfInstance() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, TENANT_NAME_ECM_123);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getId()).isNotNull();
        assertThat(vnfInstanceResponse.getVnfdId()).isEqualTo(DB_VNF_VNFDID);
        assertThat(vnfInstanceResponse.getVnfInstanceDescription()).isEqualTo(SAMPLE_VNF_DESCRIPTION);
        assertThat(vnfInstanceResponse.getVnfInstanceName()).isEqualTo(SAMPLE_VNF_NAME);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        assertThat(vnfInstance.isOverrideGlobalRegistry()).isTrue();
        assertThat(vnfInstance.getSupportedOperations()).hasSize(8);
        String metadataString = vnfInstance.getMetadata();
        Map<String, String> metadata = mapper.readValue(metadataString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(metadata).containsEntry(TENANT_NAME, TENANT_NAME_ECM_123);
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testGrantingInfoSavedWhileVnfInstancePost() throws Exception {
        whenOnboardingRespondsWithDescriptor("rel4-1ce-4cf4-477c-aab3-21cb04e6a380", "rel4-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID_MULTI_MCIOP, TENANT_NAME_ECM_123);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());

        assertThat(vnfInstance.isRel4()).isTrue();
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testCreateVnfInstanceWithoutMetadata() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, null);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getId()).isNotNull();
        assertThat(vnfInstanceResponse.getVnfdId()).isEqualTo(DB_VNF_VNFDID);
        assertThat(vnfInstanceResponse.getVnfInstanceDescription()).isEqualTo(SAMPLE_VNF_DESCRIPTION);
        assertThat(vnfInstanceResponse.getVnfInstanceName()).isEqualTo(SAMPLE_VNF_NAME);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        assertThat(vnfInstance.isOverrideGlobalRegistry()).isTrue();
        String metadataString = vnfInstance.getMetadata();
        Map<String, String> metadata = mapper.readValue(metadataString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(metadata).containsEntry(TENANT_NAME, TENANT_NAME_ECM);
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testCreateVnfInstanceWithMetadataAndWithoutTenantName() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, null);
        Map<String, String> createVnfRequestMetadata = new HashMap<>();
        createVnfRequestMetadata.put("key1", "value1");
        createVnfRequest.setMetadata(createVnfRequestMetadata);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getId()).isNotNull();
        assertThat(vnfInstanceResponse.getVnfdId()).isEqualTo(DB_VNF_VNFDID);
        assertThat(vnfInstanceResponse.getVnfInstanceDescription()).isEqualTo(SAMPLE_VNF_DESCRIPTION);
        assertThat(vnfInstanceResponse.getVnfInstanceName()).isEqualTo(SAMPLE_VNF_NAME);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        assertThat(vnfInstance.isOverrideGlobalRegistry()).isTrue();
        String metadataString = vnfInstance.getMetadata();
        Map<String, String> metadata = mapper.readValue(metadataString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(metadata)
                .hasSize(2)
                .containsEntry(TENANT_NAME, TENANT_NAME_ECM)
                .containsEntry("key1", "value1");

        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testCreateVnfInstanceWithMetadataAndValueAsObject() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, null);
        Map<String, Integer> createVnfRequestMetadata = new HashMap<>();
        createVnfRequestMetadata.put("key1", 3);
        createVnfRequest.setMetadata(createVnfRequestMetadata);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getId()).isNotNull();
        assertThat(vnfInstanceResponse.getVnfdId()).isEqualTo(DB_VNF_VNFDID);
        assertThat(vnfInstanceResponse.getVnfInstanceDescription()).isEqualTo(SAMPLE_VNF_DESCRIPTION);
        assertThat(vnfInstanceResponse.getVnfInstanceName()).isEqualTo(SAMPLE_VNF_NAME);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        assertThat(vnfInstance.isOverrideGlobalRegistry()).isTrue();
        String metadataString = vnfInstance.getMetadata();
        Map<String, String> metadata = mapper.readValue(metadataString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(metadata)
                .hasSize(2)
                .containsEntry(TENANT_NAME, TENANT_NAME_ECM)
                .containsEntry("key1", "3");

        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testMappingOfHelmChartToReleaseName() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, null);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getHelmCharts()).hasSize(2);

        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void testCreateIdentifierWithScaling() throws Exception {
        whenOnboardingRespondsWithDescriptor("with-scaling", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest("with-scaling", null);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getInstantiatedVnfInfo().getScaleStatus()).hasSize(2);

        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateIdentifierWithExtensions() throws Exception {
        whenOnboardingRespondsWithDescriptor("levels-no-vdu", "levels-no-vdu-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID, null);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceResponse.getId());
        Map<String, Object> extensions =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions)
                .containsEntry(PAYLOAD, MANUAL_CONTROLLED)
                .containsEntry(PAYLOAD_2, CISM_CONTROLLED)
                .containsEntry(PAYLOAD_3, MANUAL_CONTROLLED);

        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void successfulInstantiateRequest() throws Exception {
        whenWfsResourcesRespondsWithAccepted();
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        String jsonString = createInstantiateVnfRequestBody("my-namespace-1", DB_VNF_CLUSTER_NAME, false);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_1);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_1, INSTANTIATE);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_1);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_1);
        assertThat(vnfInstance.getManoControlledScaling()).isNull();
        assertThat(lifecycleOperationRepository.findByVnfInstance(vnfInstance).get(0).getValuesFileParams()).isNull();

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowExceptionWhenValidateScaleLevelInfoFails() throws Exception {
        InstantiateVnfRequest request = createInstantiateVnfRequest("my-namespace-1", DB_VNF_CLUSTER_NAME, false);

        request.setInstantiationLevelId("instantiation_level_1");
        request.setTargetScaleLevelInfo(TestUtils.createTargetScaleLevelInfo(
                Map.of("Aspect1", 2, "Aspect3", 4, "Aspect5", 6)));

        MvcResult response = makePostRequest(mapper.writeValueAsString(request), DB_VNF_ID_1, INSTANTIATE);
        int responseStatus = response.getResponse().getStatus();
        Map<String, Object> responseBody = mapper.readValue(response.getResponse().getContentAsString(), Map.class);

        assertThat(responseStatus).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(responseBody.get("detail")).hasToString("Instantiate scale level must be specified either " +
                                                                   "by \"instantiationLevelId\" param or by \"targetScaleLevelInfo\". " +
                                                                   "You cannot use both at the same time.");
    }

    @Test
    public void successfulInstantiateRequestWithValuesYamlParameter() throws Exception {
        whenWfsResourcesRespondsWithAccepted();

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, "values-yml-addit-params");
        additionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, readFile("value-yaml-additional-parameters.txt"));
        request.setAdditionalParams(additionalParams);
        String body = mapper.writeValueAsString(request);
        final String vnfId = "ddf4dbdd-3ddc-4fa6-84f0-83461562b1ca";
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());
        MvcResult result = makePostRequest(body, vnfId, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getValuesFileParams()).isNotNull();
        assertThat(operation.getOperationParams()).doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void successfulInstantiateRequestMultipleHelmCharts() throws Exception {
        whenWfsResourcesRespondsWithAccepted();
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        String jsonString = createInstantiateVnfRequestBody("my-namespace-2", DB_VNF_CLUSTER_NAME, false);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_16);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_16, INSTANTIATE);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_16);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void successfulInstantiateMultipartRequest() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "test-descriptor-model.json");
        whenWfsResourcesRespondsWithAccepted();
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        String multipartInstantiateJson = readDataFromFile("multipart_instantiate.json");
        String valuesYaml = readDataFromFile("valid_values.yaml");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_18);
        MockMultipartFile jsonPart = new MockMultipartFile("instantiateVnfRequest", "request.json", "application/json",
                                                           multipartInstantiateJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             valuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_18, INSTANTIATE);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_18);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final LifecycleOperation operation =
                lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_18)
                                                                                 .getOperationOccurrenceId());
        assertThat(operation.getValuesFileParams()).isNotNull();

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void successfulInstantiateMultipartRequestWithValuesYamlParameter() throws Exception {
        whenWfsResourcesRespondsWithAccepted();

        String multipartInstantiateJson = readDataFromFile("multipart_instantiate_with_values_file_parameter.json");
        String valuesYaml = readDataFromFile("valid_values.yaml");
        MockMultipartFile jsonPart = new MockMultipartFile("instantiateVnfRequest", "request.json", "application/json",
                                                           multipartInstantiateJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             valuesYaml.getBytes());
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, "12722745-66b4-4f52-bc07-5c3a84c66a2f", INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("12722745-66b4-4f52-bc07-5c3a84c66a2f");
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getValuesFileParams()).containsOnlyOnce("keyA");
        assertThat(operation.getOperationParams()).doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void failedInstantiateMultipartRequestEmptyValuesFile() throws Exception {
        String multipartInstantiateJson = readDataFromFile("multipart_instantiate_failed.json");
        String emptyValuesYaml = readDataFromFile("empty_values.yaml");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_19);
        MockMultipartFile jsonPart = new MockMultipartFile("instantiateVnfRequest", "request.json", "application/json",
                                                           multipartInstantiateJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile", VALUES_YAML_ADDITIONAL_PARAMETER, "application/x-yaml",
                                                             emptyValuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_19, INSTANTIATE);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_19);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("Values file cannot be empty");
    }

    @Test
    public void failedInstantiateMultipartRequestInvalidValuesFile() throws Exception {
        String multipartInstantiateJson = readDataFromFile("multipart_instantiate_success.json");
        String invalidValuesYaml = readDataFromFile("invalid_values.yaml");
        MockMultipartFile jsonPart = new MockMultipartFile("instantiateVnfRequest", "request.json", "application/json",
                                                           multipartInstantiateJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile", VALUES_YAML_ADDITIONAL_PARAMETER, "application/x-yaml",
                                                             invalidValuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_19, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("Values file contains invalid YAML");
    }

    @Test
    public void badInstantiateRequestDoesNotFailInitialRequest() throws Exception {
        whenWfsResourcesRespondsWithBadRequest();

        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        String jsonString = createInstantiateVnfRequestBody("my-namespace-3", DB_VNF_CLUSTER_NAME, false);

        MvcResult result = makePostRequest(jsonString, BAD_VNF, INSTANTIATE);

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.FAILED);
    }

    @Test
    public void failedInstantiateRequestOperationInProgress() throws Exception {
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());
        String jsonString = createInstantiateVnfRequestBody("my-namespace-4", DB_VNF_CLUSTER_NAME, false);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_14, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
    }

    @Test
    public void successfulTerminateRequest() throws Exception {
        whenWfsTerminateRespondsWithAccepted();

        String jsonString = createTerminateVnfRequestBody(GRACEFUL);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_12);
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(DB_VNF_ID_12);
        assertThat(byVnfId).isEmpty();
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_12, TERMINATE);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_12);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        verify(databaseInteractionService, timeout(5000))
                .persistVnfInstanceAndOperation(any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
        byVnfId = databaseInteractionService.getNamespaceDetails(DB_VNF_ID_12);
        assertThat(byVnfId.orElseThrow(() -> new NotFoundException("Namespace Details not found")).isDeletionInProgress()).isTrue();
    }

    @Test
    public void successfulMultipleChartsTerminateRequest() throws Exception {
        whenWfsTerminateRespondsWithAccepted();
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(0);

        String jsonString = createTerminateVnfRequestBody(FORCEFUL);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_17);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_17, TERMINATE);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        awaitUntilOperationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_17);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        awaitUntilOperationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING);
        verify(terminateRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        VnfInstance vnfInstance = vnfInstanceArgumentCaptor.getValue();
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                .containsOnly(LifecycleOperationState.PROCESSING.toString(), null);
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
    }

    @Test
    public void failedTerminateRequestNotInstantiated() throws Exception {
        String jsonString = createTerminateVnfRequestBody(FORCEFUL);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_14, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        MockHttpServletResponse response = result.getResponse();
        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("VNF instance ID " + DB_VNF_ID_14 + " is not in the INSTANTIATED state");
        assertThat(problemDetails.getTitle()).matches("This resource is not in the INSTANTIATED state");
    }

    @Test
    public void failedTerminateRequestDoesNotAllowAnotherOperation() throws Exception {
        String jsonString = createTerminateVnfRequestBody(FORCEFUL);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_25, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        MockHttpServletResponse response = result.getResponse();
        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("VNF instance ID " + DB_VNF_ID_25 + " is not in the INSTANTIATED state");
        assertThat(problemDetails.getTitle()).matches("This resource is not in the INSTANTIATED state");
        var afterFailure = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_25);

        assertThat(afterFailure.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        MvcResult nextResult = makePostRequest(jsonString, DB_VNF_ID_25, TERMINATE);
        MockHttpServletResponse nextResponse = nextResult.getResponse();

        assertThat(nextResponse.getContentAsString())
                .isEqualTo(
                        "{\"type\":\"about:blank\",\"title\":\"This resource is not in the INSTANTIATED state\",\"status\":409,\"detail\":\"VNF "
                                + "instance ID g7def1ce-4cf4-477c-ahb3-61c454e6a344 is not in the INSTANTIATED state\",\"instance\":\""
                                + createVnfInstanceUri(DB_VNF_ID_25) + "\"}");
    }

    @Test
    public void failedTerminateRequestNotFoundSkipValidationTrue() throws Exception {
        String jsonString = createTerminateVnfRequestBodyWithParams();
        MvcResult result = makePostRequest(jsonString, NON_EXISTENT_VNF_ID, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        MockHttpServletResponse response = result.getResponse();
        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("Vnf instance with id " + NON_EXISTENT_VNF_ID + " does not exist");
        assertThat(problemDetails.getTitle()).matches("Not Found Exception");
    }

    @Test
    public void failedTerminateRequestOperationInProgress() throws Exception {
        String jsonString = createTerminateVnfRequestBody(FORCEFUL);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_15, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
    }

    @Test
    public void successfulChangePackageInfoRequest() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-214jx84e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        String jsonString = createChangePackageInfoVnfRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_3);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_3, CHANGE_PACKAGE_INFO);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_3);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        verify(asyncOrchestrationProcessor, timeout(2000))
                .process(any(), any(), any(VnfInstance.class), any(LifecycleOperation.class), any());
        verify(databaseInteractionService, timeout(2000).atLeastOnce())
                .persistLifecycleOperationInProgress(lifecycleOperationArgumentCaptor.capture(), any(VnfInstance.class), any());
        verify(workflowRoutingService, timeout(2000).times(1))
                .routeChangePackageInfoRequest(any(ChangeOperationContext.class), any(), anyInt());
        verify(databaseInteractionService, timeout(5000)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_3);
        List<HelmChart> charts = helmChartRepository.findByVnfInstance(instance);
        assertThat(charts).extracting("state").containsOnly("COMPLETED");
        LifecycleOperation operationExecution = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operationExecution.getExpiredApplicationTime()).isAfter(LocalDateTime.now());
        assertThat(operationExecution.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationExecution.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void successfulChangePackageInfoRequestWithValuesYamlParameter() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-214jx84e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(DB_VNF_ID_4);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, "{\"a\": {\"b\": \"c\"}}");
        request.setAdditionalParams(additionalParams);
        String body = mapper.writeValueAsString(request);
        final String vnfId = "b8b77165-7065-49b1-831c-d687130c6ec7";
        MvcResult result = makePostRequest(body, vnfId, CHANGE_PACKAGE_INFO);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(
                vnfId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getValuesFileParams()).isNotNull();
        assertThat(operation.getOperationParams()).doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void successfulChangePackageInfoMultipartRequest() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        String multipartUpgradeJson = readDataFromFile("multipart_upgrade.json");
        String valuesYaml = readDataFromFile("valid_values.yaml");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_20);
        MockMultipartFile jsonPart = new MockMultipartFile("changePackageInfoVnfRequest", "request.json", "application/json",
                                                           multipartUpgradeJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             valuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_20, CHANGE_PACKAGE_INFO);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_20);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        verify(databaseInteractionService, timeout(5000))
                .persistVnfInstanceAndOperation(argThat(instance -> Objects.equals(instance.getVnfInstanceId(), DB_VNF_ID_20)),
                                                lifecycleOperationArgumentCaptor.capture());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_20);
        LifecycleOperation operation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operation.getExpiredApplicationTime()).isAfter(LocalDateTime.now());
        List<HelmChart> charts = helmChartRepository.findByVnfInstance(instance);
        assertThat(charts).extracting("state").containsOnlyNulls();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void successfulChangePackageInfoMultipartRequestWithValuesYamlParameter() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        String multipartChangePackageInfoJson = readDataFromFile(
                "multipart_upgrade_with_values_parameter.json");
        String valuesYaml = readDataFromFile("valid_values.yaml");
        MockMultipartFile jsonPart = new MockMultipartFile("changeCurrentVnfPkgRequest", "request.json",
                                                           "application/json", multipartChangePackageInfoJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             valuesYaml.getBytes());
        final String vnfId = "94f2dd1b-016d-408c-a89b-d34e42f4fb66";
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, vnfId, CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        assertThat(operation.getValuesFileParams()).containsOnlyOnce("keyA");
        assertThat(operation.getOperationParams()).doesNotContain(VALUES_YAML_ADDITIONAL_PARAMETER);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void successfulChangePackageInfoMultipartRequestWithUpgradeFailedParameter() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        String multipartChangePackageInfoJson = readDataFromFile(
                "multipart_upgrade_with_upgrade_failed_parameter.json");
        String valuesYaml = readDataFromFile("valid_values.yaml");
        MockMultipartFile jsonPart = new MockMultipartFile("changeCurrentVnfPkgRequest", "request.json",
                                                           "application/json", multipartChangePackageInfoJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             valuesYaml.getBytes());
        final String vnfId = "3de043bc-dfdc-492d-aa04-85ea53ffe97f";
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, vnfId, CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfId);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        if (Objects.nonNull(vnfInstance.getCombinedAdditionalParams())) {
            assertThat(vnfInstance.getCombinedAdditionalParams()).doesNotContain("keyA");
        }
        assertThat(operation.getValuesFileParams()).doesNotContain("keyA");
        assertThat(operation.getOperationParams()).contains(UPGRADE_FAILED_VNFD_KEY);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void failedChangePackageInfoMultipartRequestEmptyValuesFile() throws Exception {
        String multipartUpgradeJson = readDataFromFile("multipart_upgrade.json");
        String emptyValuesYaml = readDataFromFile("empty_values.yaml");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_21);
        MockMultipartFile jsonPart = new MockMultipartFile("changeCurrentVnfPkgRequest", "request.json", "application/json",
                                                           multipartUpgradeJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             emptyValuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_21, CHANGE_VNFPKG);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_21);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("Values file cannot be empty");
    }

    @Test
    public void failedChangePackageInfoMultipartRequestInvalidValuesFile() throws Exception {
        String multipartUpgradeJson = readDataFromFile("multipart_upgrade.json");
        String invalidValuesYaml = readDataFromFile("invalid_values.yaml");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_21);
        MockMultipartFile jsonPart = new MockMultipartFile("changePackageInfoVnfRequest", "request.json", "application/json",
                                                           multipartUpgradeJson.getBytes());
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile",
                                                             VALUES_YAML_ADDITIONAL_PARAMETER,
                                                             "application/x-yaml",
                                                             invalidValuesYaml.getBytes());
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_21, CHANGE_PACKAGE_INFO);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_21);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("Values file contains invalid YAML");
    }

    @Test
    public void failedChangePackageInfoRequestNotInstantiated() throws Exception {
        String jsonString = createChangeVnfpkgVnfRequestBody();
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_11, CHANGE_VNFPKG);

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());

        MockHttpServletResponse response = result.getResponse();
        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("VNF instance ID " + DB_VNF_ID_11 + " is not in the INSTANTIATED state");
        assertThat(problemDetails.getTitle()).matches("This resource is not in the INSTANTIATED state");
    }

    @Test
    public void failedChangePackageInfoRequestOperationInProgress() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-214jx84e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        String jsonString = createChangePackageInfoVnfRequestBody();
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_15, CHANGE_PACKAGE_INFO);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
    }

    @Test
    public void changePackageInfoMultipartRequestWithDuplicateKeys() throws Exception {
        String jsonString = createChangeVnfpkgVnfRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_4);
        MockMultipartFile jsonPart = new MockMultipartFile("changeCurrentVnfPkgRequest", "request.json", "application/json",
                                                           jsonString.getBytes());
        InputStream valuesFileStream = this.getClass().getResourceAsStream("/valueFiles/duplicate_keys_values.yaml");
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile", VALUES_YAML_ADDITIONAL_PARAMETER, "application/x-yaml", valuesFileStream);
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, DB_VNF_ID_4, CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("file contains invalid YAML");
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_4);
    }

    @Test
    public void changePackageInfoWithBroUrl() throws Exception {
        whenOnboardingRespondsWithDescriptor("wf1ce-rd45-477c-vnf0-snapshot004", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(0);
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        String broUrl = "http://bro-service-url.test:8080";
        String jsonString = createChangeVnfPkgRequestWithBro(broUrl);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_27);
        final VnfInstance vnfInstancePreUpgrade = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_27);
        assertThat(vnfInstancePreUpgrade.getBroEndpointUrl()).isNull();
        assertThat(vnfInstancePreUpgrade.getTempInstance()).isNull();
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_27, CHANGE_PACKAGE_INFO);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        verify(workflowRoutingService, timeout(5000).times(1))
                .routeChangePackageInfoRequest(changeOperationContextArgumentCaptor.capture(), any(), anyInt());
        final VnfInstance vnfInstancePostUpgrade = changeOperationContextArgumentCaptor.getValue().getSourceVnfInstance();
        assertThat(vnfInstancePostUpgrade.getBroEndpointUrl()).isNull();
        VnfInstance tempInstance = parseJson(vnfInstancePostUpgrade.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getBroEndpointUrl()).isEqualTo(broUrl);

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void changePackageInfoWithInvalidBroUrl() throws Exception {
        whenOnboardingRespondsWithDescriptor("wf1ce-rd45-477c-vnf0-snapshot004", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        String jsonString = createChangeVnfPkgRequestWithBro("bro-service-url.test:8080");
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_32, CHANGE_PACKAGE_INFO);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verify(databaseInteractionService, timeout(10000).atLeastOnce())
                .persistLifecycleOperation(argThat(op -> Objects.equals(op.getVnfInstance().getVnfInstanceId(), DB_VNF_ID_32) &&
                        op.getOperationState() == LifecycleOperationState.ROLLED_BACK));
        verify(databaseInteractionService, timeout(10000).atLeast(2))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        final LifecycleOperation lifecycleOperation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(lifecycleOperation.getError()).contains("The Url : bro-service-url.test:8080 is invalid due to unknown protocol: bro-service-url"
                                                                   + ".test. Please provide a valid URL.");
    }

    @Test
    public void changePackageInfoWithInstantiateBroUrl() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-214jx84e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        String jsonString = createChangePackageInfoVnfRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_28);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_28, CHANGE_PACKAGE_INFO);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_28);
        assertThat(vnfInstance.getBroEndpointUrl()).isEqualTo("http://snapshot-bro.test");

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void failedCleanupRequestLastRequestNotInstantiate() throws Exception {
        MvcResult cleanupResult = makePostRequest("", DB_VNF_ID_LAST_OPERATION_NOT_INSTANTIATE, CLEAN_UP);

        MockHttpServletResponse response = cleanupResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("Resources will not be cleaned up; last operation on instance was not a failed INSTANTIATE or TERMINATE");
    }

    @Test
    public void failedCleanupRequestOperationNotFound() throws Exception {
        MvcResult cleanupResult = makePostRequest("", DB_VNF_ID_NULL_OPERATION, CLEAN_UP);

        MockHttpServletResponse response = cleanupResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("No previous operation found for instance d3def1ce-4cf4-477c-aab3-nooperation");
    }

    @Test
    public void failedCleanupRequestNotFound() throws Exception {
        MvcResult result = makePostRequest("", "non-existent-cleanup", CLEAN_UP);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        MockHttpServletResponse response = result.getResponse();
        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail()).contains(
                "Vnf instance with id non-existent-cleanup does not exist");
        assertThat(problemDetails.getTitle()).matches("Not Found Exception");
    }

    @Test
    public void failedCleanUpInvalidLastOpWithBody() throws Exception {
        String cleanupVnfRequest = createCleanupRequestBody();
        MvcResult cleanupResult = makePostRequest(cleanupVnfRequest, DB_VNF_ID_LAST_OPERATION_NOT_INSTANTIATE, CLEAN_UP);

        MockHttpServletResponse response = cleanupResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        ProblemDetails problemDetails = mapper.readValue(response.getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .contains("Resources will not be cleaned up; last operation on instance was not a failed INSTANTIATE or TERMINATE");
    }

    @Test
    public void successfulCleanupWithRequestBody() throws Exception {
        whenWfsTerminateRespondsWithAccepted();

        String jsonString = createCleanupRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE, CLEAN_UP);
        verify(databaseInteractionService, timeout(2000)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), any(LifecycleOperation.class));
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        verify(workflowRoutingService, timeout(2000).times(1))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyString());
    }

    @Test
    public void successfulCleanupWithFailedInstantiateInvalidCluster() throws Exception {
        String jsonString = createCleanupRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_26);
        VnfInstance vnfInstanceBefore = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_26);
        LifecycleOperation operationBefore = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore).isNotNull();
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_26, CLEAN_UP);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).contains("asdqwe12-474f-4673-91ee-761fd83991f9");
        LifecycleOperation operationAfter = lifecycleOperationRepository.findByOperationOccurrenceId("asdqwe12-474f-4673-91ee-761fd83991f9");
        assertThat(operationAfter).isNull();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_26);
        VnfInstance vnfInstanceAfter = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_26);
        assertThat(vnfInstanceAfter).isNull();
    }

    @Disabled
    @Test
    public void successfulCleanupWithoutRequestBody() throws Exception {
        whenWfsTerminateRespondsWithAccepted();

        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE_1);
        MvcResult result = makePostRequest("", DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE_1, CLEAN_UP);
        verify(databaseInteractionService, timeout(2000)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), any(LifecycleOperation.class));
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE_1);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_LAST_OPERATION_FAILED_INSTANTIATE_1);
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                .containsOnly(LifecycleOperationState.PROCESSING.toString());
    }

    @Test
    public void shouldGetValuesFile() throws Exception {
        String vnfId = "values-4cf4-477c-aab3-21c454e6a380";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(REST_URL + REST_URL_VNFS_ID + vnfId + "/values")
                .accept(MediaType.TEXT_PLAIN);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("text/plain");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        checkIfValuesContentCorrect(mvcResult.getResponse());
    }

    private void checkIfValuesContentCorrect(MockHttpServletResponse response) throws UnsupportedEncodingException {
        byte[] responseInByteArray = response.getContentAsByteArray();
        final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        final Map<String, Object> responseMap = yaml.load(new ByteArrayInputStream(responseInByteArray));
        assertThat(EVNFM_PARAMS.stream().anyMatch(responseMap::containsKey)).isFalse();
        assertThat(responseMap).hasToString("{Payload_InitialDelta={replicaCount=3}, Payload_InitialDelta_1={replicaCount=1}}");
        assertThat(EVNFM_PARAMS.stream().anyMatch(responseMap::containsKey)).isFalse();
        Assertions.assertThat(response.getContentAsString()).contains("# Aspects and Current Scale level\n"
                                                                              + "# Aspect1: 3\n"
                                                                              + "# Aspect2: 3");
    }

    @Test
    public void successDeleteIdentifierRequest() throws Exception {
        MvcResult result = makeDeleteRequest(DB_VNF_ID_5);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.DELETE_VNF_INSTANCE));
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.UPDATE_PACKAGE_STATE));
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.SEND_NOTIFICATION));
    }

    @Test
    public void failedDeleteIdentifierNotFoundRequest() throws Exception {
        MvcResult result = makeDeleteRequest(NON_EXISTENT_VNF_ID);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void failedDeleteIdentifierAsIsInInstantiatedStateRequest() throws Exception {
        MvcResult result = makeDeleteRequest(DB_VNF_ID_13);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
    }

    @Test
    public void successAddNode() throws Exception {
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        MvcResult result = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_7 + REST_URL_ADD_NODE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void successAddNodeWithAdditionalParams() throws Exception {
        AddNodeToVnfInstanceByIdRequest additionalParams = new AddNodeToVnfInstanceByIdRequest("networkElementTypeTest",
                                                                                               "networkElementUsernameTest",
                                                                                               "networkElementPasswordTest",
                                                                                               "nodeIpAddressTest",
                                                                                               "22");
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        MvcResult result = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_8 + REST_URL_ADD_NODE,
                                         mapper.writeValueAsString(additionalParams));
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasks(any());
    }

    @Test
    public void failedDeleteNodeAsNotAddedToOss() throws Exception {
        doNothing().when(ossNodeService).deleteNode(any(), anyMap(), any(), eq(true));
        MvcResult result = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_7 + REST_URL_DELETE_NODE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        ProblemDetails problemDetails = mapper.readValue(result.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("VNF Instance has not been added to OSS");
        assertThat(problemDetails.getDetail()).contains("The resource " + DB_VNF_ID_7 + " has not been added to OSS");
        assertThat(problemDetails.getInstance().toString()).isEqualTo(createVnfInstanceUri(DB_VNF_ID_7));
        verify(databaseInteractionService, never()).saveTasksInNewTransaction(any());
    }

    @Test
    public void failedDeleteNodeMissingManagedElementId() throws Exception {
        doThrow(new NotFoundException("Vnf instance with id " + DB_VNF_ID_10 + " does not have an associated managedElementId"))
                .when(ossNodeService).deleteNodeFromENM(any(), eq(true));
        MvcResult result = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_10 + REST_URL_DELETE_NODE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        ProblemDetails problemDetails = mapper.readValue(result.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Not Found Exception");
        assertThat(problemDetails.getDetail()).contains("Vnf instance with id " + DB_VNF_ID_10 + " does not have an associated managedElementId");
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
    }

    @Test
    public void successDeleteNode() throws Exception {
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        MvcResult result = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_9 + REST_URL_DELETE_NODE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.DELETE_NODE));
        verify(databaseInteractionService, atLeastOnce()).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                              eq(TaskName.SEND_NOTIFICATION));
    }

    @Test
    public void successfulScaleRequest() throws Exception {
        // given
        whenOnboardingRespondsWithVnfd("9392468011745350001", "test-vnfd.json");
        whenWfsScaleRespondsWithAccepted();

        VnfInstance vnfInstanceId = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_22);
        vnfInstanceId.setSupportedOperations(buildSupportedOperations());
        vnfInstanceId.setRel4(false);
        vnfInstanceRepository.save(vnfInstanceId);
        String jsonString = createScaleVnfRequestBody(ScaleVnfRequest.TypeEnum.OUT);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_22);

        // when
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_22, SCALE);

        // then
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_22);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void failedScaleRequestInvalidScaleType() throws Exception {
        String jsonString = createScaleVnfRequestBody(null);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_22);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_22, SCALE);
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_22);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("type value not supported");
    }

    @Test
    public void instantiateMultipartRequestWithDuplicateKeys() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");

        CreateVnfRequest createVnfRequest = createCreateVnfRequest(DB_VNF_VNFDID, null);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult);
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(resultContents, VnfInstanceResponse.class);

        String jsonString = createInstantiateVnfRequestBody("my-namespace-5", DB_VNF_CLUSTER_NAME, false);
        verifyNotFoundInOperationsInProgressTable(vnfInstanceResponse.getId());
        MockMultipartFile jsonPart = new MockMultipartFile("instantiateVnfRequest", "request.json", "application/json",
                                                           jsonString.getBytes());
        InputStream valuesFileStream = this.getClass().getResourceAsStream("/valueFiles/duplicate_keys_values.yaml");
        MockMultipartFile valuesPart = new MockMultipartFile("valuesFile", VALUES_YAML_ADDITIONAL_PARAMETER, "application/x-yaml", valuesFileStream);
        MvcResult result = makePostRequestWithMultipart(jsonPart, valuesPart, vnfInstanceResponse.getId(), INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString()).contains("file contains invalid YAML");
        verifyNotFoundInOperationsInProgressTable(vnfInstanceResponse.getId());
    }

    @Test
    public void shouldFailForDeleteNamespaceClusterInProgress() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350001", "test-vnfd.json");

        String jsonString = createInstantiateVnfRequestBody("namespace-deletion", "instantiate-with-otp-2", false);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_30, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getResponse().getContentAsString()).isEqualTo(
                "{\"type\":\"about:blank\",\"title\":\"This namespace is restricted as it is marked for deletion\","
                        + "\"status\":409,\"detail\":\"Namespace namespace-deletion for cluster "
                        + "instantiate-with-otp-2 is marked for deletion\",\"instance\":\"" + createVnfInstanceUri(DB_VNF_ID_30) + "\"}");
    }

    @Test
    public void shouldNotSetDeleteNamespaceTrueOnOperationInProgressException() throws Exception {
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-214jx84e6a379", "test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "test-descriptor-model.json");
        whenWfsUpgradeRespondsWithAccepted();

        //Go into change package state
        String changePackageInfoVnfRequestBody = createChangePackageInfoVnfRequestBody();
        verifyNotFoundInOperationsInProgressTable(DB_VNF_ID_33);
        MvcResult changePackageResult = makePostRequest(changePackageInfoVnfRequestBody, DB_VNF_ID_33, CHANGE_PACKAGE_INFO);
        verifyFoundInOperationsInProgressTable(DB_VNF_ID_33);
        assertThat(changePackageResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(changePackageResult.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");
        //Change package now in progress, time to fail on termination
        String terminateVnfRequestBody = createTerminateVnfRequestBody(FORCEFUL);
        MvcResult failedTerminationResult = makePostRequest(terminateVnfRequestBody, DB_VNF_ID_33, TERMINATE);
        ProblemDetails problemDetails = mapper.readValue(failedTerminationResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetails.getTitle()).startsWith("VNF instance is already being processed");
        assertThat(problemDetails.getDetail()).endsWith("is already being processed");
        //At this point namespace should not be locked (not marked for deletion)
        Optional<VnfInstanceNamespaceDetails> namespaceDetails = vnfInstanceNamespaceDetailsRepository.findByVnfId(DB_VNF_ID_33);
        assertThat(namespaceDetails.orElseThrow().isDeletionInProgress()).isFalse();

        awaitUntilOperationReachesState(getLifeCycleOperationId(changePackageResult), LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void shouldFailForInstantiateOnKubernetesNamespaceCluster() throws Exception {
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        String jsonString = createInstantiateVnfRequestBody("kube-system", "cluster-validate", false);
        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_23, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Malformed Request\",\"status\":400,\"detail\":\"Cannot instantiate in any of the "
                                   + "Kubernetes initialized namespaces : [default, kube-system, kube-public, kube-node-lease]\","
                                   + "\"instance\":\"" + createVnfInstanceUri(DB_VNF_ID_23) + "\"}");
    }

    @Test
    public void shouldGenerateNamespaceBasedOnInstanceName() throws Exception {
        whenWfsResourcesRespondsWithAccepted();

        InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        instantiateVnfRequest.setClusterName("cluster-validate");
        String jsonString = mapper.writeValueAsString(instantiateVnfRequest);

        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_24, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        verify(asyncOrchestrationProcessor, timeout(2000)).process(
                any(), any(), vnfInstanceArgumentCaptor.capture(), any(LifecycleOperation.class), any());
        VnfInstance vnfInstance = vnfInstanceArgumentCaptor.getValue();
        assertThat(vnfInstance.getNamespace()).isEqualTo("namespace-validation-2");

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void testLifecycleOperationStageFlow() throws Exception {
        InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        instantiateVnfRequest.setClusterName("cluster-validate");
        instantiateVnfRequest.setAdditionalParams(Map.of(NAMESPACE, "dummy-namespace"));
        String jsonString = mapper.writeValueAsString(instantiateVnfRequest);

        when(packageService.getVnfd(any())).thenReturn(new JSONObject());
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());

        whenWfsResourcesRespondsWithAccepted();

        AtomicReference<LifecycleOperationStage> stageBeforeSendingToWFS = new AtomicReference<>();

        // capturing stage here since a snapshot of its value at the time of the method call is needed for verification,
        // but the method changes its value to null, causing race condition if ArgumentCaptor is used to perform verification later
        doAnswer(invocation -> {
            stageBeforeSendingToWFS.set(invocation.getArgument(3, LifecycleOperation.class).getLifecycleOperationStage());
            return invocation.callRealMethod();
        })
                .when(asyncOrchestrationProcessor)
                .sendToWFS(any(), any(), any(), any(), any());

        MvcResult result = makePostRequest(jsonString, DB_VNF_ID_31, INSTANTIATE);

        verify(asyncOrchestrationProcessor, timeout(2000)).process(any(), any(), any(VnfInstance.class), any(), any());
        verify(asyncOrchestrationProcessor, timeout(2000)).sendToWFS(any(), any(), any(), any(), any());

        assertThat(stageBeforeSendingToWFS).hasValueSatisfying(stage -> assertThat(stage.getCheckpoint()).isEqualTo(CheckpointType.GRANTED));

        verify(databaseInteractionService, timeout(2000).atLeastOnce())
                .persistLifecycleOperationInProgress(lifecycleOperationArgumentCaptor.capture(), any(VnfInstance.class), any());
        verify(databaseInteractionService, timeout(5000)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());

        LifecycleOperation operationExecution = lifecycleOperationArgumentCaptor.getValue();
        assertThat(operationExecution.getLifecycleOperationStage()).isNull();

        awaitUntilOperationReachesState(getLifeCycleOperationId(result), LifecycleOperationState.PROCESSING);
    }

    @Test
    public void shouldGenerateSnapshotIdOnExporting() throws Exception {
        String exportSnapshotRequest = TestUtils.parseJsonFile("backups/validRemoteBackupRequest.json");
        BroActionResponse broActionResponse = new BroActionResponse();
        broActionResponse.setId("54321");
        when(broHttpRoutingClient.exportBackup(any(), any(), any(), any(), any())).thenReturn(broActionResponse);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_29 + BACKUP_ACTION, exportSnapshotRequest);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
    }

    @Test
    public void testNoExceptionThrownOnEmptyRemoteRequest() {
        String exportSnapshotRequest = TestUtils.parseJsonFile("backups/validRemoteBackupRequestEmptyParams.json");
        assertThatNoException().isThrownBy(() -> postMvcResult(REST_URL + REST_URL_VNFS_ID + DB_VNF_ID_29 + BACKUP_ACTION,
                                                               exportSnapshotRequest));
    }

    private String createTerminateVnfRequestBody(TerminateVnfRequest.TerminationTypeEnum terminationType) throws JsonProcessingException {
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(terminationType);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        addTimeoutsToRequest(additionalParams, APP_DEFAULT_TIME_OUT);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createTerminateVnfRequestBodyWithParams() throws JsonProcessingException {
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(FORCEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(APPLICATION_TIME_OUT, APP_TIME_OUT);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createChangePackageInfoVnfRequestBody() throws JsonProcessingException {
        ChangePackageInfoVnfRequest request = new ChangePackageInfoVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);
        request.setVnfdId(DB_VNF_ID_4);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createChangeVnfpkgVnfRequestBody() throws JsonProcessingException {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);
        request.setVnfdId(DB_VNF_ID_4);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createChangeVnfPkgRequestWithBro(String broUrl) throws JsonProcessingException {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);
        additionalParams.put(BRO_ENDPOINT_URL, broUrl);
        request.setVnfdId(DB_VNF_ID_27);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createScaleVnfRequestBody(ScaleVnfRequest.TypeEnum scaleType) throws JsonProcessingException {
        ScaleVnfRequest request = new ScaleVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);

        request.setType(scaleType);
        request.setAspectId("Payload");
        request.setNumberOfSteps(2);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createCleanupRequestBody() throws JsonProcessingException {
        CleanupVnfRequest request = new CleanupVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private MvcResult makePostRequest(final String jsonString, final String vnfId, final String urlEnding) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post(REST_URL + REST_URL_VNFS_ID + vnfId + urlEnding)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID()).content(jsonString);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult makeDeleteRequest(final String vnfId) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete(REST_URL + REST_URL_VNFS_ID + vnfId)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult postMvcResult(String postUrl, String stringToPost) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(postUrl).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(stringToPost)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult postMvcResult(String postUrl) throws Exception {
        return postMvcResult(postUrl, UUID.randomUUID());
    }

    private MvcResult postMvcResult(String postUrl, UUID idempotencyId) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(postUrl).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY_HEADER, idempotencyId);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult makePostRequestWithMultipart(final MockMultipartFile jsonPart, final MockMultipartFile filePart, final String vnfId,
                                                   final String urlEnding) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.multipart(REST_URL + REST_URL_VNFS_ID + vnfId + urlEnding)
                                       .file(jsonPart).file(filePart)
                                       .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)
                                       .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID()))
                .andReturn();
    }

    private String checkAndReturnMockHttpServletResponseAsString(final MvcResult result) throws UnsupportedEncodingException {
        final MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        return response.getContentAsString();
    }

    private CreateVnfRequest createCreateVnfRequest(final String vnfdId, final String tenantName) {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest();
        createVnfRequest.setVnfdId(vnfdId);
        createVnfRequest.setVnfInstanceName(SAMPLE_VNF_NAME);
        createVnfRequest.setVnfInstanceDescription(SAMPLE_VNF_DESCRIPTION);
        if (!Strings.isNullOrEmpty(tenantName)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(TENANT_NAME, tenantName);
            createVnfRequest.setMetadata(metadata);
        }
        return createVnfRequest;
    }

    private void verifyNotFoundInOperationsInProgressTable(String vnfId) {
        assertThat(operationsInProgressRepository.findByVnfId(vnfId)).isNotPresent();
    }

    private void verifyFoundInOperationsInProgressTable(String vnfId) {
        assertThat(operationsInProgressRepository.findByVnfId(vnfId)).isPresent();
    }

    private String createInstantiateVnfRequestBody(String namespace,
                                                   String clusterName,
                                                   final boolean cleanUpResources) throws JsonProcessingException {
        InstantiateVnfRequest request = createInstantiateVnfRequest(namespace, clusterName, cleanUpResources);
        return mapper.writeValueAsString(request);
    }

    private InstantiateVnfRequest createInstantiateVnfRequest(String namespace,
                                                              String clusterName,
                                                              final boolean cleanUpResources) {
        InstantiateVnfRequest request = new InstantiateVnfRequest();

        Map<String, Object> additionalParams =
                createAdditionalParamsForInstantiateVnfRequest(namespace, cleanUpResources);
        addTimeoutsToRequest(additionalParams, APP_TIME_OUT);

        request.setClusterName(clusterName);
        request.setAdditionalParams(additionalParams);

        return request;
    }

    private Map<String, Object> createAdditionalParamsForInstantiateVnfRequest(
            String namespace,
            final boolean cleanUpResources) {

        Map<String, Object> additionalParams = new HashMap<>();

        additionalParams.put(NAMESPACE, namespace);
        if (cleanUpResources) {
            additionalParams.put(CLEAN_UP_RESOURCES, true);
        }

        return additionalParams;
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }

    private String getLifeCycleOperationId(final MvcResult result) {
        return result.getResponse().getHeader(HttpHeaders.LOCATION).split("/")[6];
    }

    private void awaitUntilOperationReachesState(final String lifeCycleOperationId, final LifecycleOperationState expectedState) {
        await().until(() -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

            return operation.getOperationState() == expectedState;
        });
    }

    private void whenOnboardingRespondsWithDescriptor(final String packageId, final String fileName) {
        when(packageService.getPackageInfoWithDescriptorModel(eq(packageId))).thenReturn(readObject(fileName, PackageResponse.class));
        when(packageService.getPackageInfo(eq(packageId))).thenReturn(readObject(fileName, PackageResponse.class));
    }

    private void whenOnboardingRespondsWithVnfd(final String vnfdId, final String fileName) {
        when(packageService.getVnfd(eq(vnfdId))).thenReturn(new JSONObject(readFile(fileName)));
    }

    private void whenOnboardingRespondsWithSupportedOperations() {
        doReturn(createSupportedOperations(LCMOperationsEnum.INSTANTIATE,
                                           LCMOperationsEnum.TERMINATE,
                                           LCMOperationsEnum.CHANGE_VNFPKG,
                                           LCMOperationsEnum.ROLLBACK,
                                           LCMOperationsEnum.SCALE,
                                           LCMOperationsEnum.SYNC,
                                           LCMOperationsEnum.MODIFY_INFO,
                                           LCMOperationsEnum.HEAL))
                .when(packageService).getSupportedOperations(anyString());
    }

    private List<OperationDetail> buildSupportedOperations() {
        OperationDetail instantiateOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.INSTANTIATE.getOperation());
        OperationDetail scaleOperation = OperationDetail.ofSupportedOperation(LCMOperationsEnum.SCALE.getOperation());
        return List.of(instantiateOperation, scaleOperation);
    }

    private void whenWfsTerminateRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("terminate");
    }

    private void whenWfsUpgradeRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("upgrade");
    }

    private void whenWfsScaleRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("scale");
    }

    @SuppressWarnings("unchecked")
    private void whenWfsRespondsWithAccepted(final String urlFragment) {
        when(restTemplate.exchange(contains(urlFragment), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted() {
        when(restTemplate.exchange(endsWith("instantiate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private void whenWfsResourcesRespondsWithBadRequest() {
        when(restTemplate.exchange(endsWith("instantiate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.BAD_REQUEST));
    }

    private HelmVersionsResponse getHelmVersionsResponse() {
        List<String> helmVersions = Arrays.asList("3.8", "3.10", "latest");

        HelmVersionsResponse helmVersionsResponse = new HelmVersionsResponse();
        helmVersionsResponse.setHelmVersions(helmVersions);

        return helmVersionsResponse;
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readFile(fileName), targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T readObject(final String fileName, final TypeReference<T> targetType) {
        try {
            return mapper.readValue(readFile(fileName), targetType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SshResponse createSshResponse(int exitStatus) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(exitStatus);
        return sshResponse;
    }

    private static void addTimeoutsToRequest(final Map<String, Object> additionalParams, final int appTimeOut) {
        additionalParams.put(APPLICATION_TIME_OUT, appTimeOut);
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "test-vnfd.json");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }

    private String createVnfInstanceUri(String vnfInstanceId) {
        return HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + vnfInstanceId;
    }
}
