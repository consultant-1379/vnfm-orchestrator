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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.drac;

import static java.util.Collections.emptyMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.INSTANTIATE_VNF_REQUEST_PARAM;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.BACKUP_ACTION;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CHANGE_PACKAGE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLEAN_UP;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.FAIL_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.HEAL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_LCM_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_LCM_OPS_ALL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS_ID;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.ROLLBACK_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.SCALE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.SYNC;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.TERMINATE;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageInfoVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CreateBackupsRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.SyncVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfLcmOperationsController;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.JwtDecoder;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "drac.enabled=true",
        "drac.config.json={\"roles\": ["
                + "{\"name\": \"Role 1\", \"nodeTypes\": [\"Product name 1\", \"Product name 2\"]},"
                + "{\"name\": \"Role 2\", \"nodeTypes\": [\"Product name 3\", \"Product name 4\"]}]}" })
public class DracInterceptorsTest extends AbstractDbSetupTest {

    private static final String VNF_INSTANCE_ID = "vnfInstanceId";
    private static final String OPERATION_ID = "operationId";
    private static final String VNF_INSTANCES_PATH = REST_URL + REST_URL_VNFS;
    private static final String VNF_INSTANCE_ID_PATH = REST_URL + REST_URL_VNFS_ID + VNF_INSTANCE_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private JwtDecoder JwtDecoder;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private VnfInstancesControllerImpl vnfInstancesController;

    @MockBean
    private VnfLcmOperationsController vnfLcmOperationsController;

    @BeforeEach
    public void setUp() {
        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("Role 1", "Role 3"));
    }

    // **/vnf_instances/** endpoints

    @Test
    public void createInstanceShouldSucceedWhenHasRole() throws Exception {
        // given
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 1");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);

        // when
        makeInstancePostRequest(request);

        // then
        verify(vnfInstancesController).createVnfInstance(any(), any(), any(), eq(request));
    }

    @Test
    public void createInstanceShouldSucceedWhenPackageDetailsRequestFails() throws Exception {
        // given
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        when(packageService.getPackageInfo(any())).thenThrow(new PackageDetailsNotFoundException("Not found"));

        // when
        makeInstancePostRequest(request);

        // then
        verify(vnfInstancesController).createVnfInstance(any(), any(), any(), eq(request));
    }

    @Test
    public void createInstanceShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 3");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);

        // when
        final var mvcResult = makeInstancePostRequest(request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void instantiateShouldSucceedWhenHasRole() throws Exception {
        // given
        final var request = new InstantiateVnfRequest();

        givenInstanceWithNodeType("Product name 1");

        // when
        makeInstancePostRequest(INSTANTIATE, request);

        // then
        verify(vnfInstancesController).instantiateVnfInstance(eq(VNF_INSTANCE_ID), any(), any(), any(), eq(request));
    }

    @Test
    public void instantiateShouldSucceedWhenInstanceNotFound() throws Exception {
        // given
        final var request = new InstantiateVnfRequest();

        when(databaseInteractionService.getVnfInstance(any())).thenThrow(new NotFoundException("Not found"));

        // when
        makeInstancePostRequest(INSTANTIATE, request);

        // then
        verify(vnfInstancesController).instantiateVnfInstance(eq(VNF_INSTANCE_ID), any(), any(), any(), eq(request));
    }

    @Test
    public void instantiateShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new InstantiateVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(INSTANTIATE, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void instantiateMultipartShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new InstantiateVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostMultipartRequest(INSTANTIATE, request, INSTANTIATE_VNF_REQUEST_PARAM);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgInfoShouldFailWhenNoRoleForSourcePackage() throws Exception {
        // given
        final var request = new ChangePackageInfoVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(CHANGE_PACKAGE_INFO, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgInfoMultipartShouldFailWhenNoRoleForSourcePackage() throws Exception {
        // given
        final var request = new ChangePackageInfoVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final MvcResult mvcResult = makeInstancePostMultipartRequest(CHANGE_PACKAGE_INFO, request, "changePackageInfoVnfRequest");

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgInfoShouldFailWhenNoRoleForTargetPackage() throws Exception {
        // given
        final var request = new ChangePackageInfoVnfRequest();
        request.setVnfdId("vnfd-id");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 3");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);
        givenInstanceWithNodeType("Product name 1");

        // when
        final var mvcResult = makeInstancePostRequest(CHANGE_PACKAGE_INFO, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgInfoMultipartShouldFailWhenNoRoleForTargetPackage() throws Exception {
        // given
        final JsonObject request = new JsonObject();
        request.addProperty("vnfdId", "vnfd-id");
        request.addProperty("unknownField", "unknown-field");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 3");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);

        givenInstanceWithNodeType("Product name 1");

        // when
        final MvcResult mvcResult = makeInstancePostMultipartRequest(CHANGE_PACKAGE_INFO, request, "changePackageInfoVnfRequest");

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgShouldFailWhenNoRoleForSourcePackage() throws Exception {
        // given
        final var request = new ChangeCurrentVnfPkgRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(CHANGE_VNFPKG, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgMultipartShouldFailWhenNoRoleForSourcePackage() throws Exception {
        // given
        final var request = new ChangeCurrentVnfPkgRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final MvcResult mvcResult = makeInstancePostMultipartRequest(CHANGE_VNFPKG, request, "changeCurrentVnfPkgRequest");

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgShouldFailWhenNoRoleForTargetPackage() throws Exception {
        // given
        final var request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId("vnfd-id");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 3");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);

        givenInstanceWithNodeType("Product name 1");

        // when
        final var mvcResult = makeInstancePostRequest(CHANGE_VNFPKG, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void changeCurrentVnfPkgMultipartShouldFailWhenNoRoleForTargetPackage() throws Exception {
        // given
        final JsonObject request = new JsonObject();
        request.addProperty("vnfdId", "vnfd-id");
        request.addProperty("unknownField", "unknown-field");

        final var packageResponse = new PackageResponse();
        packageResponse.setVnfProductName("Product name 3");

        when(packageService.getPackageInfo(any())).thenReturn(packageResponse);

        givenInstanceWithNodeType("Product name 1");

        // when
        final MvcResult mvcResult = makeInstancePostMultipartRequest(CHANGE_VNFPKG, request, "changeCurrentVnfPkgRequest");

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void cleanupShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new CleanupVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(CLEAN_UP, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void healShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new HealVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(HEAL, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void modifyInstanceShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new VnfInfoModificationRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePatchRequest(request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void scaleShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new ScaleVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(SCALE, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void syncShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new SyncVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(SYNC, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void terminateShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new TerminateVnfRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(TERMINATE, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void deleteInstanceShouldFailWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstanceDeleteRequest();

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void backupShouldFailWhenNoRole() throws Exception {
        // given
        final var request = new CreateBackupsRequest();

        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstancePostRequest(BACKUP_ACTION, request);

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    @Test
    public void deleteBackupShouldFailWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        final var mvcResult = makeInstanceDeleteRequest(BACKUP_ACTION + "/backup-name/scope");

        // then
        assertInstanceRequestFailed(mvcResult);
    }

    // **/vnf_lcm_op_occs/** endpoints

    @Test
    public void failOperationShouldSucceedWhenHasRole() throws Exception {
        // given
        givenOperationWithNodeType("Product name 1");

        // when
        makeOperationPostRequest(FAIL_OPS);

        // then
        verify(vnfLcmOperationsController).failLifecycleManagementOperationById(eq(OPERATION_ID), any(), any());
    }

    @Test
    public void failOperationShouldSucceedWhenFetchingOperationFailed() throws Exception {
        // given
        when(databaseInteractionService.getLifecycleOperation(any())).thenThrow(new JpaSystemException(new RuntimeException("Not found")));

        // when
        makeOperationPostRequest(FAIL_OPS);

        // then
        verify(vnfLcmOperationsController).failLifecycleManagementOperationById(eq(OPERATION_ID), any(), any());
    }

    @Test
    public void failOperationShouldSucceedWhenOperationNotFound() throws Exception {
        // given
        when(databaseInteractionService.getLifecycleOperation(any())).thenReturn(null);

        // when
        makeOperationPostRequest(FAIL_OPS);

        // then
        verify(vnfLcmOperationsController).failLifecycleManagementOperationById(eq(OPERATION_ID), any(), any());
    }

    @Test
    public void failOperationShouldFailWhenNoRole() throws Exception {
        // given
        givenOperationWithNodeType("Product name 3");

        // when
        final var mvcResult = makeOperationPostRequest(FAIL_OPS);

        // then
        assertOperationRequestFailed(mvcResult);
    }

    @Test
    public void rollbackOperationShouldFailWhenNoRole() throws Exception {
        // given
        givenOperationWithNodeType("Product name 3");

        // when
        final var mvcResult = makeOperationPostRequest(ROLLBACK_OPS);

        // then
        assertOperationRequestFailed(mvcResult);
    }

    @Test
    public void getInstanceByIdShouldSucceedWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        makeGetRequest(VNF_INSTANCE_ID_PATH);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfInstancesController).getVnfInstanceById(any(), eq(VNF_INSTANCE_ID));
    }

    @Test
    public void getInstanceByIdShouldSucceedWhenHasRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 1");

        // when
        makeGetRequest(VNF_INSTANCE_ID_PATH);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfInstancesController).getVnfInstanceById(any(), eq(VNF_INSTANCE_ID));
    }

    @Test
    public void getInstancesShouldSucceedWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        makeGetRequest(VNF_INSTANCES_PATH);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfInstancesController).getAllVnfInstances(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void getInstancesShouldSucceedWhenHasRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 1");

        // when
        makeGetRequest(VNF_INSTANCES_PATH);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfInstancesController).getAllVnfInstances(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void getOperationByIdShouldSucceedWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        makeGetRequest(REST_URL + REST_URL_LCM_OPS + OPERATION_ID);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfLcmOperationsController).getLifecycleManagementOperationById(eq(OPERATION_ID), any());
    }

    @Test
    public void getOperationByIdShouldSucceedWhenHasRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 1");

        // when
        makeGetRequest(REST_URL + REST_URL_LCM_OPS + OPERATION_ID);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfLcmOperationsController).getLifecycleManagementOperationById(eq(OPERATION_ID), any());
    }

    @Test
    public void getOperationsShouldSucceedWhenNoRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 3");

        // when
        makeGetRequest(REST_URL + REST_URL_LCM_OPS_ALL);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfLcmOperationsController).getAllLifecycleManagementOperations(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    public void getOperationsShouldSucceedWhenHasRole() throws Exception {
        // given
        givenInstanceWithNodeType("Product name 1");

        // when
        makeGetRequest(REST_URL + REST_URL_LCM_OPS_ALL);

        // then
        verifyNoInteractions(JwtDecoder);
        verify(vnfLcmOperationsController).getAllLifecycleManagementOperations(
                any(), any(), any(), any(), any(), any());
    }

    private void givenInstanceWithNodeType(final String vnfProductName) {
        final var instance = new VnfInstance();
        instance.setVnfProductName(vnfProductName);
        when(databaseInteractionService.getVnfInstance(any())).thenReturn(instance);
    }

    private void givenOperationWithNodeType(final String vnfProductName) {
        final var operation = new LifecycleOperation();
        operation.setVnfProductName(vnfProductName);
        when(databaseInteractionService.getLifecycleOperation(any())).thenReturn(operation);
    }

    private MvcResult makeInstancePostRequest(final Object request) throws Exception {
        return makePostRequest(VNF_INSTANCES_PATH, request);
    }

    private MvcResult makeInstancePostRequest(final String urlEnding, final Object request) throws Exception {
        return makePostRequest(VNF_INSTANCE_ID_PATH + urlEnding, request);
    }

    private MvcResult makeInstancePostMultipartRequest(final String urlEnding, final Object request, final String requestPartName) throws Exception {
        final var requestPart = new MockMultipartFile(requestPartName,
                                                      "file.json",
                                                      "application/json",
                                                      request instanceof JsonObject ?
                                                              request.toString().getBytes(StandardCharsets.UTF_8) :
                                                              mapper.writeValueAsString(request).getBytes());
        final var valuesPart = new MockMultipartFile("valuesFile",
                                                     "values.yaml",
                                                     "application/x-yaml",
                                                     mapper.writeValueAsString(emptyMap()).getBytes());
        final var requestBuilder = MockMvcRequestBuilders
                .multipart(VNF_INSTANCE_ID_PATH + urlEnding)
                .file(requestPart)
                .file(valuesPart)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeInstanceDeleteRequest() throws Exception {
        return makeDeleteRequest(VNF_INSTANCE_ID_PATH);
    }

    private MvcResult makeInstanceDeleteRequest(final String urlEnding) throws Exception {
        return makeDeleteRequest(VNF_INSTANCE_ID_PATH + urlEnding);
    }

    private MvcResult makeOperationPostRequest(final String urlEnding) throws Exception {
        final var requestBuilder = post(REST_URL + REST_URL_LCM_OPS + OPERATION_ID + urlEnding)
                .accept(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeInstancePatchRequest(final Object request) throws Exception {
        final var body = mapper.writeValueAsString(request);

        final var requestBuilder = patch(VNF_INSTANCE_ID_PATH)
                .content(body)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE);

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makePostRequest(final String requestUrl, final Object request) throws Exception {
        final var body = mapper.writeValueAsString(request);

        final var requestBuilder = post(requestUrl)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeGetRequest(final String requestUrl) throws Exception {
        final var requestBuilder = get(requestUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeDeleteRequest(final String requestUrl) throws Exception {
        final var requestBuilder = delete(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private void assertInstanceRequestFailed(final MvcResult mvcResult) throws UnsupportedEncodingException {
        assertResponseUnauthorized(mvcResult);
        verifyNoInteractions(vnfInstancesController);
    }

    private void assertOperationRequestFailed(final MvcResult mvcResult) throws UnsupportedEncodingException {
        assertResponseUnauthorized(mvcResult);
        verifyNoInteractions(vnfLcmOperationsController);
    }

    private static void assertResponseUnauthorized(final MvcResult mvcResult) throws UnsupportedEncodingException {
        final var response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("User is not authorized to perform LCM operations on VNF with Product name 3 node type");
    }

}
