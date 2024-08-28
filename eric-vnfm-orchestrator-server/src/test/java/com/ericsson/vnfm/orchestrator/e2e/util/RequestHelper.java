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
import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.TestUtils.BRO_ENDPOINT_URL;
import static com.ericsson.vnfm.orchestrator.TestUtils.CNF_BRO_URL_INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.TestUtils.CNF_BRO_URL_UPGRADE;
import static com.ericsson.vnfm.orchestrator.TestUtils.DEFAULT_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DOWNSIZE_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETNAME_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_SCOPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_MERGING_PREVIOUS_VALUES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANO_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLEAN_UP;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_LCM_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS_ID;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.ROLLBACK_OPS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.SCALE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.TERMINATE;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestComponent
public class RequestHelper {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AwaitHelper awaitHelper;

    public String executeCreateVnfRequest(final String vnfdId) throws Exception {
        CreateVnfRequest createVnfRequest = createCreateVnfRequest(vnfdId, vnfdId);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS_ID, mapper.writeValueAsString(createVnfRequest));
        return checkAndReturnMockHttpServletResponseAsString(mvcResult, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public VnfInstanceResponse executeCreateVnfRequest(final String releaseName, final String vnfdId) throws Exception {
        CreateVnfRequest createVnfRequest = createCreateVnfRequest(vnfdId, releaseName);
        MvcResult mvcResult = postMvcResult(REST_URL + REST_URL_VNFS, mapper.writeValueAsString(createVnfRequest));
        String resultContents = checkAndReturnMockHttpServletResponseAsString(mvcResult, HttpStatus.CREATED);

        return mapper.readValue(resultContents, VnfInstanceResponse.class);
    }

    public MvcResult registerNewCluster(String filePath, String description) throws Exception {
        return registerNewCluster(filePath, description, UUID.randomUUID());
    }

    public MvcResult registerNewCluster(String filePath, String description, UUID idempotencyKey) throws Exception {
        Resource fileResource = new ClassPathResource(filePath);

        MockMultipartFile configFile = new MockMultipartFile(
                "clusterConfig", fileResource.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource.getInputStream());

        return mockMvc.perform(MockMvcRequestBuilders
                                       .multipart(REST_URL + CLUSTER_CONFIG)
                                       .file(configFile)
                                       .accept(MediaType.APPLICATION_JSON)
                                       .contentType(MediaType.MULTIPART_FORM_DATA)
                                       .contentType(MediaType.MULTIPART_FORM_DATA)
                                       .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                                       .param("description", description))
                .andReturn();
    }

    public MvcResult updateCluster(String filePath,
                                   String name,
                                   String description,
                                   Boolean skipSameClusterVerification,
                                   Boolean isDefault) throws Exception {

        Resource fileResource = new ClassPathResource(filePath);

        MockMultipartFile configFile = new MockMultipartFile(
                "clusterConfig", fileResource.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource.getInputStream());

        return mockMvc.perform(MockMvcRequestBuilders
                                       .multipart(REST_URL + CLUSTER_CONFIG + "/" + name)
                                       .file(configFile)
                                       .accept(MediaType.APPLICATION_JSON)
                                       .param("description", description)
                                       .queryParam("isDefault", isDefault.toString())
                                       .queryParam("skipSameClusterVerification", String.valueOf(skipSameClusterVerification))
                                       .with(request -> {
                                                 request.setMethod("PUT");
                                                 return request;
                                             }
                                       ))
                .andReturn();
    }

    public MvcResult patchCluster(String name,
                                  Map<String, Object> clusterConfigUpdateFields,
                                  Boolean skipSameClusterVerification) throws Exception {

        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .patch(REST_URL + CLUSTER_CONFIG + "/" + name)
                .accept(MediaType.APPLICATION_JSON)
                .contentType("application/merge-patch+json")
                .queryParam("skipSameClusterVerification", String.valueOf(skipSameClusterVerification))
                .content(mapper.writeValueAsString(clusterConfigUpdateFields));

        return mockMvc.perform(requestBuilder).andReturn();
    }

    public MvcResult deregisterCluster(String clusterName) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .delete(REST_URL + CLUSTER_CONFIG + "/" + clusterName)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder).andReturn();
    }

    public MvcResult getMvcResultNegativeInstantiateRequest(final VnfInstanceResponse vnfInstanceResponse,
                                                            String namespace,
                                                            String clusterName,
                                                            boolean includeCleanUpResources) throws Exception {
        String instantiateRequest = createInstantiateVnfRequestBody(
                namespace, clusterName, false, includeCleanUpResources, false
        );
        return makePostRequest(instantiateRequest, vnfInstanceResponse.getId(),
                               VnfInstancesControllerImplIntegrationTest.INSTANTIATE);
    }

    public MvcResult getMvcResultInstantiateRequest(final VnfInstanceResponse vnfInstanceResponse,
                                                    String namespace,
                                                    String clusterName,
                                                    boolean includeCleanUpResources) throws Exception {

        String instantiateRequest = createInstantiateVnfRequestBody(
                namespace, clusterName, false, includeCleanUpResources, false
        );
        final MvcResult result = makePostRequest(instantiateRequest, vnfInstanceResponse.getId(),
                                                 VnfInstancesControllerImplIntegrationTest.INSTANTIATE);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationHasCombinedAdditionalParams(lifeCycleOperationId));
        return result;
    }

    public MvcResult getMvcResultInstantiateRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                     String namespace) throws Exception {

        MvcResult result = getMvcResultInstantiateRequest(vnfInstanceResponse, namespace, DEFAULT_CLUSTER_NAME, false);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        return result;
    }

    public MvcResult getMvcResultInstantiateRequestAndVerifyStatus(final VnfInstanceResponse vnfInstanceResponse,
                                                                   String namespace, int status) throws Exception {

        MvcResult result = getMvcResultInstantiateRequest(vnfInstanceResponse, namespace, DEFAULT_CLUSTER_NAME, false);
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        return result;
    }

    public MvcResult getMvcResultInstantiateRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                     String namespace,
                                                                     String clusterName) throws Exception {

        MvcResult result = getMvcResultInstantiateRequest(vnfInstanceResponse, namespace, clusterName, false);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        return result;
    }

    public MvcResult getMvcResultInstantiateRequest(final VnfInstanceResponse vnfInstanceResponse,
                                                    String namespace) throws Exception {

        String instantiateRequest = createInstantiateVnfRequestBody(namespace, DEFAULT_CLUSTER_NAME, false, false, false);
        final MvcResult result = makePostRequest(instantiateRequest, vnfInstanceResponse.getId(),
                                                 VnfInstancesControllerImplIntegrationTest.INSTANTIATE);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationHasCombinedAdditionalParams(lifeCycleOperationId));
        return result;
    }

    public MvcResult getMvcResultInstantiateRequestWithLevelsExtensions(final VnfInstanceResponse vnfInstanceResponse,
                                                                        String namespace,
                                                                        final String levelId,
                                                                        final Map<String, Object> extensions,
                                                                        boolean isManoControlledScaling) throws Exception {

        String instantiateRequest = createInstantiateVnfRequestBodyWithLevelsExtensions(namespace,
                                                                                        DEFAULT_CLUSTER_NAME,
                                                                                        false,
                                                                                        false,
                                                                                        isManoControlledScaling,
                                                                                        levelId,
                                                                                        extensions);
        final MvcResult result = makePostRequest(instantiateRequest, vnfInstanceResponse.getId(),
                                                 VnfInstancesControllerImplIntegrationTest.INSTANTIATE);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationHasCombinedAdditionalParams(lifeCycleOperationId));
        return result;
    }

    public MvcResult getMvcResultInstantiateRequestWithYamlFile(final VnfInstanceResponse vnfInstanceResponse,
                                                                String namespace,
                                                                String clusterName,
                                                                String yamlFileName) throws Exception {

        String valuesYaml = TestUtils.readDataFromFile(yamlFileName);
        String instantiateRequest = createInstantiateVnfRequestBody(namespace, clusterName, true, false, false);
        final MvcResult result = makePostRequestWithFile("instantiateVnfRequest", instantiateRequest, valuesYaml, vnfInstanceResponse.getId(),
                                                         VnfInstancesControllerImplIntegrationTest.INSTANTIATE);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationHasCombinedAdditionalParams(lifeCycleOperationId));
        return result;
    }

    public MvcResult getMvcResultHealRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse, boolean cleanUpResources)
    throws Exception {
        String healPackageInfoRequest = createHealVnfRequestBody(cleanUpResources);
        final MvcResult result = makePostRequest(healPackageInfoRequest, vnfInstanceResponse.getId(), "/heal");
        assertThat(result.getResponse().getStatus())
                .withFailMessage("Request : {} returned incorrect http status {} with error message: {}",
                                 healPackageInfoRequest, result.getResponse().getStatus(), result.getResponse().getErrorMessage())
                .isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                                 final Map<String, Object> extensions,
                                                                                 final String packageId,
                                                                                 final String releaseName,
                                                                                 final Map<String, String> metadata,
                                                                                 final String vnfDescription) throws Exception {

        MvcResult result = getMvcResultModifyRequest(vnfInstanceResponse.getId(), extensions, packageId, releaseName, metadata, vnfDescription);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(
                lifeCycleOperationId, LifecycleOperationState.PROCESSING, LifecycleOperationState.COMPLETED)
        );
        return result;
    }

    public MvcResult getMvcResultModifyRequest(final String vnfInstanceId,
                                               final Map<String, Object> extensions,
                                               final String packageId,
                                               final String releaseName,
                                               final Map<String, String> metadata,
                                               final String vnfDescription) throws Exception {
        String modifyVnfRequestBody = createModifyVnfRequestBody(packageId, releaseName, metadata, vnfDescription, extensions);
        return makePatchRequest(modifyVnfRequestBody, vnfInstanceId);
    }

    public MvcResult getMvcResultScaleVnfRequest(final VnfInstanceResponse vnfInstanceResponse,
                                                 ScaleVnfRequest.TypeEnum type,
                                                 String aspectId) throws Exception {

        final MvcResult result;
        String scaleVnfRequest = createScaleVnfRequest(type, aspectId);
        result = makePostRequest(scaleVnfRequest, vnfInstanceResponse.getId(), SCALE);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult executeSyncVnfInstance(String vnfInstanceId) throws Exception {
        return makePostRequest("{}", vnfInstanceId, VnfInstancesControllerImplIntegrationTest.SYNC);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                      final String changePackageInfoVnfdId,
                                                                      final boolean skipMergingPreviousValues) throws Exception {

        return getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse.getId(), changePackageInfoVnfdId, false,
                                                                true, skipMergingPreviousValues);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                      final String changePackageInfoVnfdId,
                                                                      boolean downsizeAllowed,
                                                                      boolean autoRollbackAllowed,
                                                                      boolean skipMergingPreviousValues) throws Exception {

        return getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse.getId(), changePackageInfoVnfdId, downsizeAllowed,
                                                                autoRollbackAllowed, skipMergingPreviousValues);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestAndVerifyAccepted(final String vnfInstanceId,
                                                                      final String changePackageInfoVnfdId,
                                                                      boolean downsizeAllowed,
                                                                      boolean autoRollbackAllowed,
                                                                      boolean skipMergingPreviousValues) throws Exception {
        return getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceId, changePackageInfoVnfdId, downsizeAllowed, autoRollbackAllowed, true,
                                                                skipMergingPreviousValues);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestAndVerifyAccepted(final String vnfInstanceId,
                                                                      final String changePackageInfoVnfdId,
                                                                      boolean downsizeAllowed,
                                                                      boolean autoRollbackAllowed,
                                                                      boolean persistScaleInfo,
                                                                      boolean skipMergingPreviousValues) throws Exception {
        String changePackageInfoRequest = createChangeVnfpkgVnfRequestBody(
                changePackageInfoVnfdId, downsizeAllowed, autoRollbackAllowed, persistScaleInfo, skipMergingPreviousValues
        );
        final MvcResult result = makePostRequest(changePackageInfoRequest, vnfInstanceId, CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus())
                .withFailMessage("Request : {} returned incorrect http status {} with error message: {}",
                                 changePackageInfoRequest, result.getResponse().getStatus(), result.getResponse().getErrorMessage())
                .isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().timeout(1, TimeUnit.MINUTES).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultChangeVnfpkgRequest(final String vnfInstanceId,
                                                     final String changePackageInfoVnfdId,
                                                     boolean downsizeAllowed,
                                                     boolean autoRollbackAllowed,
                                                     boolean persistScaleInfo) throws Exception {
        String changePackageInfoRequest = createChangeVnfpkgVnfRequestBody(
                changePackageInfoVnfdId, downsizeAllowed, autoRollbackAllowed, persistScaleInfo, false
        );
        return makePostRequest(changePackageInfoRequest, vnfInstanceId, CHANGE_VNFPKG);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestWithYamlFile(final VnfInstanceResponse vnfInstanceResponse,
                                                                 final String changePackageInfoVnfdId,
                                                                 boolean downsizeAllowed,
                                                                 String yamlFileName) throws Exception {

        final MvcResult result;
        String valuesYaml = TestUtils.readDataFromFile(yamlFileName);
        String changePackageInfoRequest = createChangeVnfpkgVnfRequestBody(changePackageInfoVnfdId, downsizeAllowed);
        result = makePostRequestWithFile("changeCurrentVnfPkgRequest",
                                         changePackageInfoRequest,
                                         valuesYaml,
                                         vnfInstanceResponse.getId(),
                                         CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus())
                .withFailMessage("Request : {} returned incorrect http status {} with error message: {}",
                                 changePackageInfoRequest, result.getResponse().getStatus(), result.getResponse().getErrorMessage())
                .isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultRollbackOperationRequestAndVerifyAccepted(final String rollbackOperationId) throws Exception {
        final MvcResult result = getMvcResultRollbackOperationRequest(rollbackOperationId);
        assertThat(result.getResponse().getStatus())
                .withFailMessage("Request : {} returned incorrect http status {} with error message: {}",
                                 rollbackOperationId, result.getResponse().getStatus(), result.getResponse().getErrorMessage())
                .isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultRollbackOperationRequest(final String rollbackOperationId) throws Exception {
        return makePostLcmOpRequest(rollbackOperationId, ROLLBACK_OPS);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestWithExtensionsAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse,
                                                                                    final String changePackageInfoVnfdId,
                                                                                    final Map<String, Object> extensions) throws Exception {

        final MvcResult result = getMvcResultChangeVnfpkgRequestWithExtensions(vnfInstanceResponse, changePackageInfoVnfdId, extensions);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultChangeVnfpkgRequestWithExtensions(final VnfInstanceResponse vnfInstanceResponse,
                                                                   final String changePackageInfoVnfdId,
                                                                   final Map<String, Object> extensions) throws Exception {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(changePackageInfoVnfdId);
        request.setExtensions(extensions);
        request.setAdditionalParams(Map.of("persistScaleInfo", false,
                                           "skipMergingPreviousValues", true));
        String changePackageInfoRequest = mapper.writeValueAsString(request);

        return makePostRequest(changePackageInfoRequest, vnfInstanceResponse.getId(), CHANGE_VNFPKG);
    }

    public MvcResult getMvcResultChangeVnfpkgRequestWithComplexTypes(final VnfInstanceResponse vnfInstanceResponse,
                                                                     final String changePackageInfoVnfdId) throws Exception {

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(changePackageInfoVnfdId);
        Map<String, Object> additionalParams = TestUtils.getComplexTypeAdditionalParams();
        additionalParams.put(BRO_ENDPOINT_URL, CNF_BRO_URL_UPGRADE);
        additionalParams.put(SKIP_MERGING_PREVIOUS_VALUES, true);
        request.setAdditionalParams(additionalParams);

        String changePackageInfoRequest = mapper.writeValueAsString(request);
        final MvcResult result = makePostRequest(changePackageInfoRequest, vnfInstanceResponse.getId(), CHANGE_VNFPKG);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultTerminateRequestAndVerifyAccepted(final VnfInstanceResponse vnfInstanceResponse) throws Exception {
        final MvcResult result = getMvcResultTerminateRequest(vnfInstanceResponse);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await()
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResultTerminateRequest(final VnfInstanceResponse vnfInstanceResponse) throws Exception {
        final String terminateRequest = createTerminateRequest();
        return makePostRequest(terminateRequest, vnfInstanceResponse.getId(), TERMINATE);
    }

    public MvcResult getMvcResultCleanUpRequest(final String vnfInstanceId) throws Exception {
        final String cleanupRequest = createCleanUpRequest();
        final MvcResult result = makePostRequest(cleanupRequest, vnfInstanceId, CLEAN_UP);
        String lifeCycleOperationId = EndToEndTestUtils.getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));
        return result;
    }

    public MvcResult getMvcResult(String postUrl) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(postUrl)
                .accept(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder).andReturn();
    }

    public MvcResult getMvcResultWithParams(String postUrl, LinkedMultiValueMap<String, String> requestParams) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(postUrl)
                .params(requestParams)
                .accept(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult postMvcResult(String postUrl, String stringToPost) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(postUrl).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID())
                .content(stringToPost);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    public MvcResult makePostRequest(final String jsonString, final String vnfId, final String urlEnding) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(REST_URL + REST_URL_VNFS_ID + vnfId + urlEnding)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID()).content(jsonString);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    public MvcResult makePostRequestWithFile(final String jsonStringID,
                                             final String jsonString,
                                             final String yamlString,
                                             final String vnfId,
                                             final String urlEnding) throws Exception {

        MockMultipartFile jsonPart = new MockMultipartFile(jsonStringID, "request.json", "application/json", jsonString.getBytes());
        MockMultipartFile yamlPart = new MockMultipartFile("valuesFile", "values.yaml", "application/x-yaml", yamlString.getBytes());
        return mockMvc.perform(MockMvcRequestBuilders.multipart(REST_URL + REST_URL_VNFS_ID + vnfId + urlEnding)
                                       .file(jsonPart).file(yamlPart)
                                       .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)
                                       .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID()))
                .andReturn();
    }

    private MvcResult makePatchRequest(final String jsonString, final String vnfId) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .patch(REST_URL + REST_URL_VNFS_ID + vnfId)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(jsonString);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult makePostLcmOpRequest(final String vnfLcmOpOccId, final String urlEnding) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(REST_URL + REST_URL_LCM_OPS + vnfLcmOpOccId + urlEnding)
                .accept(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private static CreateVnfRequest createCreateVnfRequest(final String vnfdId, final String vnfInstanceName) {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest(vnfdId, vnfInstanceName);
        createVnfRequest.setVnfInstanceDescription("Run through all the operations");
        createVnfRequest.setMetadata(Map.of("tenantName", "ecm"));

        return createVnfRequest;
    }

    private String createInstantiateVnfRequestBody(final String namespace,
                                                   final String clusterName,
                                                   boolean includeDownsize,
                                                   boolean includeCleanUpResources,
                                                   boolean isManoControlledScaling) throws JsonProcessingException {

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.clusterName(clusterName);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, namespace);
        additionalParams.put("tags.pm", "true");
        additionalParams.put("instantiate-key", "install");
        additionalParams.put("override-key", "install");
        additionalParams.put("testValue", "test");
        additionalParams.put(HELM_NO_HOOKS, true);
        additionalParams.putAll(TestUtils.getComplexTypeAdditionalParams());
        additionalParams.put(BRO_ENDPOINT_URL, CNF_BRO_URL_INSTANTIATE);
        if (includeDownsize) {
            additionalParams.put(DOWNSIZE_VNFD_KEY, true);
        }
        if (!includeCleanUpResources) {
            additionalParams.put(CLEAN_UP_RESOURCES, false);
        }
        if (isManoControlledScaling) {
            additionalParams.put(MANO_CONTROLLED_SCALING, true);
        }
        additionalParams.put("helm_client_version", "3.8");
        request.setAdditionalParams(additionalParams);

        return mapper.writeValueAsString(request);
    }

    private String createHealVnfRequestBody(boolean cleanUpResources) throws JsonProcessingException {
        HealVnfRequest request = new HealVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(RESTORE_BACKUP_NAME, "backupName");
        additionalParams.put(RESTORE_SCOPE, "DEFAULT");
        additionalParams.put(DAY0_CONFIGURATION_SECRETNAME_KEY, "my-secret");
        additionalParams.put(CLEAN_UP_RESOURCES, cleanUpResources);
        additionalParams.put("day0.configuration.param1.key", "restore.externalStorageURI");
        additionalParams.put("day0.configuration.param1.value", "sftp://url");
        additionalParams.put("day0.configuration.param2.key", "restore.externalStorageCredentials");
        additionalParams.put("day0.configuration.param2.value", "password");

        //adding day0 configuration
        Map<String, Object> secretContent1 = new HashMap<>();
        secretContent1.put("username", "John");
        secretContent1.put("password", "hashpassword");
        secretContent1.put("others", 3);
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

        additionalParams.put(DAY0_CONFIGURATION_SECRETS, secrets);

        request.setCause("Full Restore");
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createInstantiateVnfRequestBodyWithLevelsExtensions(final String namespace,
                                                                       final String clusterName,
                                                                       boolean includeDownsize,
                                                                       boolean includeCleanupResources,
                                                                       boolean isManoControlledScaling,
                                                                       String levelid,
                                                                       Map<String, Object> extensions) throws JsonProcessingException {

        InstantiateVnfRequest request = mapper.readValue(createInstantiateVnfRequestBody(namespace,
                                                                                         clusterName,
                                                                                         includeDownsize,
                                                                                         includeCleanupResources,
                                                                                         isManoControlledScaling),
                                                         InstantiateVnfRequest.class);
        request.setInstantiationLevelId(levelid);
        request.setExtensions(extensions);
        return mapper.writeValueAsString(request);
    }

    private String createModifyVnfRequestBody(final String packageId,
                                              final String releaseName,
                                              final Map<String, String> metadata,
                                              final String vnfDescription,
                                              final Map<String, Object> extensions) throws JsonProcessingException {

        VnfInfoModificationRequest request = new VnfInfoModificationRequest();
        request.setVnfPkgId(packageId);
        request.setVnfInstanceName(releaseName);
        request.setVnfInstanceDescription(vnfDescription);
        request.setMetadata(metadata);
        request.setExtensions(extensions);
        return mapper.writeValueAsString(request);
    }

    private String createScaleVnfRequest(ScaleVnfRequest.TypeEnum type, String aspectId) throws JsonProcessingException {
        ScaleVnfRequest scaleRequest = new ScaleVnfRequest();
        scaleRequest.setType(type);
        scaleRequest.setAspectId(aspectId);
        scaleRequest.setNumberOfSteps(1);
        return mapper.writeValueAsString(scaleRequest);
    }

    private String createChangeVnfpkgVnfRequestBody(final String vnfdId,
                                                    boolean downsizeAllowed,
                                                    boolean autoRollbackAllowed) throws JsonProcessingException {
        return createChangeVnfpkgVnfRequestBody(vnfdId, downsizeAllowed, autoRollbackAllowed, false, false);
    }

    private String createChangeVnfpkgVnfRequestBody(final String vnfdId,
                                                    boolean downsizeAllowed,
                                                    boolean autoRollbackAllowed,
                                                    boolean persistScaleInfo,
                                                    boolean skipMergingPreviousValues) throws JsonProcessingException {

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        if (downsizeAllowed) {
            additionalParams.put(DOWNSIZE_VNFD_KEY, "true");
        }
        additionalParams.put(IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY, String.valueOf(autoRollbackAllowed));
        additionalParams.put("upgradeParam", "testing value");
        additionalParams.put("override-key", "upgrade");
        additionalParams.put("helm_client_version", "3.10");
        additionalParams.put("testValue", "test2");
        additionalParams.put(BRO_ENDPOINT_URL, CNF_BRO_URL_UPGRADE);
        additionalParams.put(PERSIST_SCALE_INFO, String.valueOf(persistScaleInfo));
        additionalParams.put(SKIP_MERGING_PREVIOUS_VALUES, String.valueOf(skipMergingPreviousValues));
        request.setVnfdId(vnfdId);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createChangeVnfpkgVnfRequestBody(final String vnfdId, boolean downsizeAllowed) throws JsonProcessingException {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        JSONObject failedParams = new JSONObject();
        failedParams.put("keyA", "a");
        Map<String, Object> additionalParams = new HashMap<>();
        if (downsizeAllowed) {
            additionalParams.put(DOWNSIZE_VNFD_KEY, "true");
        }
        additionalParams.put("upgradeParam", "testing value");
        additionalParams.put(UPGRADE_FAILED_VNFD_KEY, failedParams.toMap());
        additionalParams.put("helm_client_version", "3.10");
        request.setVnfdId(vnfdId);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String createTerminateRequest() throws JsonProcessingException {
        TerminateVnfRequest terminateVnfRequest = new TerminateVnfRequest();
        terminateVnfRequest.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("cleanUpResources", "false");
        terminateVnfRequest.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(terminateVnfRequest);
    }

    private String createCleanUpRequest() throws JsonProcessingException {
        CleanupVnfRequest cleanupVnfRequest = new CleanupVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("commandTimeOut", "500");
        cleanupVnfRequest.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(cleanupVnfRequest);
    }

    private static String checkAndReturnMockHttpServletResponseAsString(final MvcResult result,
                                                                        final HttpStatus httpStatus) throws UnsupportedEncodingException {

        final MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(httpStatus.value());

        return response.getContentAsString();
    }
}
