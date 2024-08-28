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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.license;

import static java.util.Collections.emptyMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ONBOARDING;
import static com.ericsson.vnfm.orchestrator.model.onboarding.OperationalState.ENABLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CLUSTER_MANAGEMENT_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ENM_INTEGRATION_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.LCM_OPERATIONS_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.INSTANTIATE_VNF_REQUEST_PARAM;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_ADD_NODE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_LCM_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS_ID;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ClusterConfigMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
public class LicenseInterceptorsTest extends AbstractDbSetupTest {

    private static final String REST_URL_CLUSTER_CONFIGS = "/clusterconfigs/";
    private static final String STUB_VNF_INSTANCE_ID = "abcd-1234";
    private static final String CONFIG_FILE_PATH = "configs/cluster01_te+st_config.config";
    private static final String DESCRIPTION = "Cluster config file description.";

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @SpyBean
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @SpyBean
    private VnfInstanceRepository vnfInstanceRepository;

    @SpyBean
    private PackageService packageService;

    @SpyBean
    private InstanceService instanceService;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private ClusterConfigMapper clusterConfigMapper;

    @MockBean
    private VnfdHelper vnfdHelper;

    @MockBean
    private VnfInstanceMapper vnfInstanceMapper;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void addNodeShouldFailWhenNoEnmIntegrationPermission() throws Exception {
        // given
        givenLcmOperationsPermitted();

        // when
        final var mvcResult = makeInstancePostRequest("vnfInstanceId",
                                                      REST_URL_ADD_NODE,
                                                      emptyMap());

        // then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void addNodeShouldNotFailWithNoPermissionExceptionWhenNoLcmPermission() throws Exception {
        givenEnmIntegrationOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + REST_URL_ADD_NODE;

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isNotEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).doesNotContain(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void addNodeShouldNotFailBecauseOfNoLcmPermissionFound() throws Exception {
        givenClusterManagementAndOnboardingOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/addNode";

        final var mvcResult = makePostRequest(url);

        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).doesNotContain(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void addNodeMultipartShouldFailWhenNoEnmIntegrationPermission() throws Exception {
        // given
        givenLcmOperationsPermitted();

        // when
        final var valuesPart = new MockMultipartFile("valuesFile",
                                                     "values.yaml",
                                                     "application/x-yaml",
                                                     mapper.writeValueAsString(emptyMap()).getBytes());
        final var requestBuilder = MockMvcRequestBuilders
                .multipart(REST_URL + REST_URL_VNFS_ID + "vnfInstanceId" + REST_URL_ADD_NODE)
                .file(valuesPart)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());


        final var mvcResult = mockMvc.perform(requestBuilder)
                .andReturn();

        // then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void addNodeMultipartShouldNotFailWithNoPermissionExceptionWhenNoLcmPermission() throws Exception {
        givenEnmIntegrationOperationsPermitted();

        final var valuesPart = new MockMultipartFile("valuesFile",
                                                     "values.yaml",
                                                     "application/x-yaml",
                                                     mapper.writeValueAsString(emptyMap()).getBytes());
        final var requestBuilder = MockMvcRequestBuilders
                .multipart(REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + REST_URL_ADD_NODE)
                .file(valuesPart)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA);

        final var mvcResult = mockMvc.perform(requestBuilder)
                .andReturn();

        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isNotEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).doesNotContain(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void deleteNodeShouldFailWhenNoEnmIntegrationPermission() throws Exception {
        // given
        givenLcmOperationsPermitted();

        // when
        final var mvcResult = makeInstancePostRequest("vnfInstanceId",
                                                      REST_URL_DELETE_NODE,
                                                      emptyMap());

        // then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void deleteNodeShouldNotFailWithNoPermissionExceptionWhenNoLcmPermission() throws Exception {
        givenEnmIntegrationOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + REST_URL_DELETE_NODE;

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isNotEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).doesNotContain(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void deleteNodeShouldNotFailBecauseOfNoLcmPermissionFound() throws Exception {
        givenClusterManagementAndOnboardingOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/deleteNode";

        final var mvcResult = makePostRequest(url);

        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).doesNotContain(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void instantiateShouldFailWhenNoEnmIntegrationPermission() throws Exception {
        // given
        givenLcmOperationsPermitted();

        final var request = new InstantiateVnfRequest();
        request.setAdditionalParams(Map.of(ADD_NODE_TO_OSS, true));

        // when
        final var mvcResult = makeInstancePostRequest("vnfInstanceId",
                                                      INSTANTIATE,
                                                      request);

        // then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void instantiateMultipartShouldFailWhenNoEnmIntegrationPermission() throws Exception {
        // given
        givenLcmOperationsPermitted();

        final var request = new InstantiateVnfRequest();
        request.setAdditionalParams(Map.of(ADD_NODE_TO_OSS, true));

        // when
        final var requestPart = new MockMultipartFile(INSTANTIATE_VNF_REQUEST_PARAM,
                                                      "instantiate.json",
                                                      "application/json",
                                                      mapper.writeValueAsString(request).getBytes());
        final var valuesPart = new MockMultipartFile("valuesFile",
                                                     "replicaCalculation/values.yaml",
                                                     "application/x-yaml",
                                                     mapper.writeValueAsString(emptyMap()).getBytes());
        final var requestBuilder = MockMvcRequestBuilders
                .multipart(REST_URL + REST_URL_VNFS_ID + "vnfInstanceId" + INSTANTIATE)
                .file(requestPart)
                .file(valuesPart)
                .accept(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                .contentType(MediaType.MULTIPART_FORM_DATA);

        final var mvcResult = mockMvc.perform(requestBuilder)
                .andReturn();

        // then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(ENM_INTEGRATION_LICENSE_TYPE);
    }

    @Test
    public void registerClusterConfigShouldFailWhenNoClusterManagementPermisson() throws Exception {
        //given
        givenLcmOperationsPermitted();

        //when
        final var mvcResult = makeClusterConfigPostRequest(CONFIG_FILE_PATH, DESCRIPTION);

        //then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(CLUSTER_MANAGEMENT_LICENSE_TYPE);
    }

    @Test
    public void updateClusterConfigShouldFailWhenNoClusterManagementPermisson() throws Exception {
        //given
        givenLcmOperationsPermitted();

        //when
        final var mvcResult = makeClusterConfigPutRequest("clusterName", emptyMap());

        //then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(CLUSTER_MANAGEMENT_LICENSE_TYPE);
    }

    @Test
    public void patchClusterConfigShouldFailWhenNoClusterManagementPermisson() throws Exception {
        //given
        givenLcmOperationsPermitted();

        //when
        final var mvcResult = makeClusterConfigPatchRequest("clusterName", emptyMap());

        //then
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(CLUSTER_MANAGEMENT_LICENSE_TYPE);
    }

    @Test
    public void deleteBackupShouldFailWhenNoLcmPermissions() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/backups/backupName/scope";

        var mvcResult = makeVnfInstanceDeleteRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void createVnfInstanceShouldFailWhenNoLcmPermission() throws Exception {

        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS;

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void createBackupShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/backups";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void changeCurrentVnfPkgInfoShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/change_package_info";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void changeCurrentVnfPkgShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/change_vnfpkg";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void healShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/heal";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void instantiateShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/instantiate";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void scaleShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/scale";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void syncShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID + "/sync";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void modifyVnfInstanceShouldFailWhenNoLcmPermissions() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_VNFS_ID + STUB_VNF_INSTANCE_ID;

        var mvcResult = makeVnfInstancePatchRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void failLifecycleOperationByOccIdShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_LCM_OPS + "/occuranceId/fail";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void rollbackLifecycleOperationByOccIdShouldFailWhenNoLcmPermission() throws Exception {
        givenAllButLcmOperationsPermitted();

        String url = REST_URL + REST_URL_LCM_OPS + "/occurrenceId/rollback";

        var mvcResult = makePostRequest(url);

        var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(response.getContentAsString()).contains(LCM_OPERATIONS_LICENSE_TYPE);
    }

    @Test
    public void testInterceptorFailedValidationWithClusterControllerRejectedRequest() throws Exception {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(clusterConfigFileRepository.count())
                .thenReturn(1L, 1L, 1L, 2L);
        doReturn(null).when(clusterConfigService).prepareRegistrationClusterConfig(any(), any(), any(), any());
        doNothing().when(clusterConfigService).registerClusterConfig(any());
        when(clusterConfigMapper.toInternalModel(any())).thenReturn(null);


        final MockHttpServletResponse firstResponse = makeClusterConfigPostRequest(CONFIG_FILE_PATH, DESCRIPTION).getResponse();
        assertThat(firstResponse.getStatus()).isEqualTo(CREATED.value());

        final MockHttpServletResponse secondResponse = makeClusterConfigPostRequest(CONFIG_FILE_PATH, DESCRIPTION).getResponse();
        assertThat(secondResponse.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(secondResponse.getContentAsString()).contains(String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE));
    }

    @Test
    public void testInterceptorFailedValidationWithVnfControllerRejectedRequest() throws Exception {
        //given
        PackageResponse packageInfo = new PackageResponse();
        packageInfo.setOperationalState(ENABLED);
        packageInfo.setDescriptorModel("{\n  \"someKey\": \"someValue\"\n}");

        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("someVnfInstanceId");
        vnfInstance.setVnfPackageId("someVnfPackageId");

        VnfInstanceResponse vnfInstanceResponse = new VnfInstanceResponse();

        final CreateVnfRequest request = new CreateVnfRequest();
        request.setVnfdId("someVnfdId");
        request.setVnfInstanceName("some-instance");

        //when
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(vnfInstanceRepository.count())
                .thenReturn(4L, 4L, 4L, 5L);
        doReturn(packageInfo).when(packageService).getPackageInfoWithDescriptorModel(any());
        doReturn(null).when(vnfdHelper).getVnfdScalingInformation(any());
        doReturn(vnfInstance).when(instanceService).createVnfInstanceEntity(any(), any(), any());
        doReturn(vnfInstance).when(databaseInteractionService).saveVnfInstanceToDB(any());
        doNothing().when(databaseInteractionService).saveTasksInNewTransaction(any());
        doNothing().when(instanceService).createAndSaveAssociationBetweenPackageAndVnfInstance(any(), any(), anyBoolean());
        doReturn(vnfInstanceResponse).when(vnfInstanceMapper).toOutputModel(any());
        doNothing().when(notificationService).sendVnfIdentifierCreationEvent(any());
        doNothing().when(databaseInteractionService).deleteTasks(any());

        final var firstResponse = makeInstancePostRequest("", "", request).getResponse();
        assertThat(firstResponse.getStatus()).isEqualTo(CREATED.value());

        final var secondResponse = makeInstancePostRequest("", "", request).getResponse();
        assertThat(secondResponse.getStatus()).isEqualTo(METHOD_NOT_ALLOWED.value());
        assertThat(secondResponse.getContentAsString()).contains(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, LCM_OPERATIONS_LICENSE_TYPE));
    }

    private String testableVnfInstance() {
        var instanceId = "rel4-jswq-4cf4-477c-aab3-21cb04e6a380";
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(instanceId);
        try {
            return mapper.writeValueAsString(vnfInstance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void givenLcmOperationsPermitted() {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(LCM_OPERATIONS));
    }

    private void givenAllButLcmOperationsPermitted() {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(CLUSTER_MANAGEMENT, ENM_INTEGRATION, ONBOARDING));
    }

    private void givenClusterManagementAndOnboardingOperationsPermitted() {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(CLUSTER_MANAGEMENT, ONBOARDING));
    }

    private void givenEnmIntegrationOperationsPermitted() {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.of(ENM_INTEGRATION));
    }

    private MvcResult makeInstancePostRequest(final String vnfInstanceId,
                                              final String urlEnding,
                                              final Object request) throws Exception {

        final var body = mapper.writeValueAsString(request);
        final var vnfUrl = StringUtils.isEmpty(vnfInstanceId) ? REST_URL_VNFS : REST_URL_VNFS_ID;
        final var requestBuilder = post(REST_URL + vnfUrl + vnfInstanceId + urlEnding)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeClusterConfigPatchRequest(final String clusterName,
                                                    final Object request) throws Exception {
        final var body = mapper.writeValueAsString(request);

        final var requestBuilder = MockMvcRequestBuilders
                .patch(REST_URL + REST_URL_CLUSTER_CONFIGS + clusterName)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType("application/merge-patch+json");
        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeClusterConfigPutRequest(final String clusterName,
                                                  final Object request) throws Exception {
        final var body = mapper.writeValueAsString(request);

        final var requestBuilder = MockMvcRequestBuilders
                .put(REST_URL + REST_URL_CLUSTER_CONFIGS + clusterName)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA);
        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    public MvcResult makeClusterConfigPostRequest(String filePath, String description) throws Exception {
        Resource fileResource = new ClassPathResource(filePath);

        MockMultipartFile configFile = new MockMultipartFile(
                "clusterConfig", fileResource.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource.getInputStream());

        return mockMvc.perform(MockMvcRequestBuilders
                        .multipart(REST_URL + CLUSTER_CONFIG)
                        .file(configFile)
                        .header("wfs-clusterconfig", "someObject").header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON)
                        .param("description", description))
                .andReturn();
    }

    private MvcResult makePostRequest(String url) throws Exception {
        final var requestBuilder = post(url)
                .content(testableVnfInstance())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeVnfInstanceDeleteRequest(String url) throws Exception {
        final var requestBuilder = delete(url)
                .content(testableVnfInstance())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeVnfInstancePatchRequest(String url) throws Exception {
        final var requestBuilder = patch(url)
                .content(testableVnfInstance())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE);

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }
}
