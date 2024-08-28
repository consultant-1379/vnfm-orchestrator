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
package com.ericsson.vnfm.orchestrator.e2e.clusterconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoJson;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import wiremock.com.jayway.jsonpath.JsonPath;
import wiremock.net.minidev.json.JSONArray;

public class ClusterConfigTest extends AbstractEndToEndTest {

    private static final String TEST_CONFIG_PATH = "configs/cluster01.config";

    @Autowired
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @BeforeEach
    public void init() throws Exception {
        requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));
    }

    @Test
    public void registerClusterWithInvalidNameNegative() throws Exception {
        String description = "Cluster config file description.";
        String configFilePath = "configs/cluster01_te+st_config.config";
        String errorMessage = "An error occurred during validating cluster config: ClusterConfig name not in correct format";

        //register new cluster
        MvcResult result = requestHelper.registerNewCluster(configFilePath, description);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);

        // verify cluster is not registered
        Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(FilenameUtils.getName(configFilePath));
        assertThat(clusterConfigFile).isEmpty();
    }

    @Test
    public void registerAndDeregisterClusterWithInstantiation() throws Exception {
        final String releaseName = "external-cluster-namespace";
        String description = "";
        String errorMessageInUse = String.format("Cluster config file cluster01.config is in use and not available for deletion.",
                                                 FilenameUtils.getName(TEST_CONFIG_PATH));

        //register new cluster
        MvcResult result = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        // verify cluster is registered
        Optional<ClusterConfigFile> clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        ClusterConfigFile clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getName()).isEqualTo(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.NOT_IN_USE);

        // deregister cluster with status "NOT_IN_USE"
        result = requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(result.getResponse().getStatus()).isEqualTo(204);

        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isEmpty();

        //register new cluster
        result = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        // verify cluster is registered
        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getName()).isEqualTo(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.NOT_IN_USE);

        //Create Identifier
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                               releaseName,
                                                                               FilenameUtils.getName(TEST_CONFIG_PATH));
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = EndToEndTestUtils.getHelmReleaseLifecycleMessage(releaseName, HelmReleaseState.COMPLETED,
                                                                                                 lifeCycleOperationId,
                                                                                                 HelmReleaseOperationType.INSTANTIATE, "1");

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, LifecycleOperationState.COMPLETED);

        // verify state of operation
        LifecycleOperation instantiateOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(instantiateOperation).isNotNull();
        assertThat(instantiateOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(instantiateOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);

        // verify state of instance
        VnfInstance vnfInstance = instantiateOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);

        // deregister cluster with status "in-use"
        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.IN_USE);

        result = requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessageInUse);

        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.IN_USE);

        //Terminate
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), LifecycleOperationState.COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.TERMINATE,
                                                   InstantiationState.NOT_INSTANTIATED);

        // check cluster config status changed
        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getStatus()).isEqualTo(ConfigFileStatus.NOT_IN_USE);

        // deregister cluster with status "NOT_IN_USE"
        result = requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(result.getResponse().getStatus()).isEqualTo(204);

        // check cluster was deregistered
        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isEmpty();

        // instantiate on cluster which was deregistered
        // Create Identifier
        vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        result = requestHelper.getMvcResultNegativeInstantiateRequest(
                vnfInstanceResponse, releaseName, FilenameUtils.getName(TEST_CONFIG_PATH), false
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentAsString()).contains("Cluster config file cluster01.config not found");
    }

    @Test
    public void testRegisterDuplicateClusterNegative() throws Exception {
        String description = "";

        String errorMessage = "File with name cluster01.config already exists.";

        //        register new cluster
        MvcResult result = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description);
        //  registerResult.getResponse();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        // verify cluster is registered
        Optional<ClusterConfigFile> clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isPresent();

        ClusterConfigFile clusterConfigFile = clusterConfigFileOptional.get();
        assertThat(clusterConfigFile.getName()).isEqualTo(FilenameUtils.getName(TEST_CONFIG_PATH));

        // register cluster second time
        result = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description);
        result.getResolvedException().getMessage();

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        ValidationException ex = (ValidationException) result.getResolvedException();
        assertThat(ex.getMessage()).isEqualTo(errorMessage);

        result = requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(result.getResponse().getStatus()).isEqualTo(204);

        clusterConfigFileOptional = clusterConfigFileRepository.findByName(FilenameUtils.getName(TEST_CONFIG_PATH));
        assertThat(clusterConfigFileOptional).isEmpty();
    }

    @Test
    public void testDeregisterNotExistingClusterNegative() throws Exception {
        String configFile = "notExist.config";
        String errorMessage = String.format("Cluster config file %s does not exist.", configFile);

        // verify cluster isn't registered
        Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(configFile);
        assertThat(clusterConfigFile).isEmpty();

        // register cluster
        MvcResult result = requestHelper.deregisterCluster(configFile);
        result.getResolvedException().getMessage();
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void updateClusterConfigNotExist() throws Exception {
        final String name = "configNotExist.config";
        final String description = "update cluster config not exist";
        final String errorMessage = "Cluster config file configNotExist.config does not exist.";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, false, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);

        // verify cluster config is not exist
        Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(clusterConfigFile).isEmpty();
    }

    @Test
    public void updateClusterConfigTooLongDescription() throws Exception {
        final String name = "updateCluster.config";
        final String description = RandomStringUtils.random(251);
        final String errorMessage = "Description should not be longer then 250 characters";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, false, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);

        // verify cluster config description is not updated
        final Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(clusterConfigFile.get().getDescription()).isNotEqualTo(description);
    }

    @Test
    public void updateClusterConfig() throws Exception {
        final String name = "updateCluster.config";
        final String description = "updated cluster config";

        Optional<ClusterConfigFile> oldClusterConfigFile = clusterConfigFileRepository.findByName(name);

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, false, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // verify cluster description is updated
        Optional<ClusterConfigFile> newClusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(oldClusterConfigFile.get().getDescription()).isNotEqualTo(newClusterConfigFile.get().getDescription());
        assertThat(newClusterConfigFile.get().getDescription()).isEqualTo(description);

        // verify cluster config is updated
        final String newConfig = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(TEST_CONFIG_PATH),
                StandardCharsets.UTF_8
        );

        assertThat(oldClusterConfigFile.get().getContent()).isNotEqualTo(newClusterConfigFile.get().getContent());
        assertThat(newClusterConfigFile.get().getContent()).isEqualTo(newConfig);
    }

    @Test
    public void updateClusterInvalidUUID() throws Exception {
        final String name = "updateClusterInvalidUUID.config";
        final String description = "update cluster config invalidUUID";
        final String errorMessage = "Cluster config belongs to another cluster than original, kube-system uid differs";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, false, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void updateClusterSkipInvalidUUID() throws Exception {
        final String name = "updateClusterInvalidUUID.config";
        final String description = "update cluster config invalidUUID success";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, true, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void updateClusterMissingNamespaces() throws Exception {
        final String name = "updateClusterMissingNamespaces.config";
        final String description = "update cluster missing namespaces";
        final String errorMessage = "Following VNF namespaces are missing on cluster: updateClusterMissingNamespaces";
        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, false, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void updateDefaultClusterConfig() throws Exception {
        final String name = "default66.config";
        final String description = "update cluster missing namespaces";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, true, true);
        Optional<ClusterConfigFile> newClusterConfigFile = clusterConfigFileRepository.findByName(name);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertTrue(newClusterConfigFile.orElseThrow().isDefault());

        //unmark default config
        MvcResult unmarkResult = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, true, false);
        Optional<ClusterConfigFile> defaultCluster = clusterConfigFileRepository.findByName(name);

        assertThat(unmarkResult.getResponse().getStatus()).isEqualTo(400);
        assertThat(unmarkResult.getResolvedException().getMessage()).isEqualTo("One of the clusters must be marked as default");
        assertTrue(defaultCluster.orElseThrow().isDefault());
    }

    @Test
    public void updateClusterSkipMissingNamespaces() throws Exception {
        final String name = "updateClusterMissingNamespaces.config";
        final String description = "update cluster missing namespaces success";

        //update cluster config
        MvcResult result = requestHelper.updateCluster(TEST_CONFIG_PATH, name, description, true, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void patchClusterConfigNotExist() throws Exception {
        final String name = "configNotExist.config";
        final String errorMessage = "Cluster config file configNotExist.config does not exist.";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);

        // verify cluster config is not exist
        Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(clusterConfigFile).isEmpty();
    }

    @Test
    public void patchClusterConfigTooLongDescription() throws Exception {
        final String name = "patchCluster.config";
        final String description = RandomStringUtils.randomAlphabetic(251);
        final String errorMessage = "size must be between 0 and 250";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody(description, false);
        clusterConfigUpdateFields.put("description", description);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        // verify cluster config description is not updated
        final Optional<ClusterConfigFile> clusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(clusterConfigFile.get().getDescription()).isNotEqualTo(description);
        assertThat(result.getResolvedException().getMessage()).contains(errorMessage);
    }

    @Test
    public void patchClusterConfigIsDefault() throws Exception {
        final String name = "default66.config";

        final Map<String, Object> clusterConfigFirstUpdateFields = buildSuccessPatchBody("Patched description", true);
        final Map<String, Object> clusterConfigSecondUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigFirstUpdateFields, true);
        Optional<ClusterConfigFile> newClusterConfigFile = clusterConfigFileRepository.findByName(name);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertTrue(newClusterConfigFile.orElseThrow().isDefault());

        //unmark default config
        MvcResult unmarkResult = requestHelper.patchCluster(name, clusterConfigSecondUpdateFields, true);
        Optional<ClusterConfigFile> defaultCluster = clusterConfigFileRepository.findByName(name);

        assertThat(unmarkResult.getResponse().getStatus()).isEqualTo(400);
        assertThat(unmarkResult.getResolvedException().getMessage()).isEqualTo("One of the clusters must be marked as default");
        assertTrue(defaultCluster.orElseThrow().isDefault());
    }

    @Test
    public void patchClusterInvalidUUID() throws Exception {
        final String name = "patchClusterInvalidUUID.config";
        final String errorMessage = "Cluster config belongs to another cluster than original, kube-system uid differs";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void patchClusterSkipInvalidUUID() throws Exception {
        final String name = "patchClusterInvalidUUID.config";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, true);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void patchClusterMissingNamespaces() throws Exception {
        final String name = "patchClusterMissingNamespaces.config";
        final String errorMessage = "Following VNF namespaces are missing on cluster: patchClusterMissingNamespaces";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void patchClusterSkipMissingNamespaces() throws Exception {
        final String name = "patchClusterMissingNamespaces.config";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody("Patched description", false);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, true);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    public void patchClusterConfig() throws Exception {
        final String name = "patchCluster.config";
        final String description = "Patched description";
        final Map<String, Object> clusterConfigUpdateFields = buildSuccessPatchBody(description, false);

        Optional<ClusterConfigFile> oldClusterConfigFile = clusterConfigFileRepository.findByName(name);

        //patch cluster config
        MvcResult result = requestHelper.patchCluster(name, clusterConfigUpdateFields, false);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // verify cluster description is patched
        Optional<ClusterConfigFile> newClusterConfigFile = clusterConfigFileRepository.findByName(name);
        assertThat(oldClusterConfigFile.get().getDescription()).isNotEqualTo(newClusterConfigFile.get().getDescription());
        assertThat(newClusterConfigFile.get().getDescription()).isEqualTo(description);

        // verify cluster config is patched
        final String oldUsers = convertYamlStringIntoJson(oldClusterConfigFile.get().getContent()).get("users").toString();
        final String newUsers = convertYamlStringIntoJson(newClusterConfigFile.get().getContent()).get("users").toString();
        final String expectedUsers = new JSONObject(clusterConfigUpdateFields).getJSONObject("clusterConfig").get("users").toString();

        assertThat(oldUsers).isNotEqualTo(newUsers);
        assertThat(newUsers).isEqualTo(expectedUsers);
    }

    @Test
    public void paginationNoQueriesPositive() throws Exception {
        MvcResult result = requestHelper.getMvcResult(REST_URL + CLUSTER_CONFIG);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.next.href")).contains("page=2");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(15);
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(15);
    }

    @Test
    public void paginationPageAndSizePositive() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "2");
        requestParams.add("size", "20");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=2");
        assertThat((String) JsonPath.read(responseBody, "$._links.next.href")).contains("page=3");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(20);
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(20);
    }

    @Test
    public void paginationPageAndSizeAndFilterPositive() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "2");
        requestParams.add("size", "20");
        requestParams.add("filter", "(eq,status,NOT_IN_USE)");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();

        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=2");
        assertThat((String) JsonPath.read(responseBody, "$._links.next.href")).contains("page=3");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(20);
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(20);
    }

    @Test
    public void paginationPageNegative() throws Exception {
        // Negative test for bad pagination value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "two");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("pagge", "2");
        result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.next.href")).contains("page=2");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(1);            //should return first page
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(15);             //default page size
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(15);        //default
    }

    @Test
    public void paginationSizeNegative() throws Exception {
        // Negative test for bad pagination value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("size", "two");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sizze", "2");
        result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.next.href")).contains("page=2");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(1);            //should return first page
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(15);             //default page size
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(15);        //default
    }

    @Test
    public void paginationFilterNegative() throws Exception {
        // Bad filter type
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(eeq,status,IN_USE)");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.detail")).contains("Invalid operation provided eeq");

        // Bad field name
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(eq,somefield,IN_USE)");
        result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.detail")).contains("Filter eq,somefield,IN_USE not supported");
    }

    @Test
    public void paginationSearchNoResults() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(eq,name,willNotFind)");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.next")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.last.href")).contains("page=1");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.totalPages")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(15);
        assertThat((JSONArray) JsonPath.read(responseBody, "$.items")).size().isEqualTo(0);
    }

    @Test
    public void paginationSearchResultsLessThanPageSize() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(eq,status,IN_USE)");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$._links.first.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.prev")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.self.href")).contains("page=1");
        assertThat((String) JsonPath.read(responseBody, "$._links.next")).isNull();
        assertThat((String) JsonPath.read(responseBody, "$._links.last.href")).contains("page=1");
        assertThat((Integer) JsonPath.read(responseBody, "$.page.number")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.totalPages")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(responseBody, "$.page.size")).isEqualTo(15);
    }

    @Test
    public void paginationPageLimitExceeded() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "3");
        requestParams.add("filter", "(eq,status,IN_USE)");
        MvcResult result = requestHelper.getMvcResultWithParams(REST_URL + CLUSTER_CONFIG, requestParams);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Requested page number exceeds the total number of pages. Requested page:: 3. Total page size:: 1");
    }

    private static Map<String, Object> buildSuccessPatchBody(String description, boolean isDefault) {
        return new HashMap<>(Map.of(
                "description", description,
                "isDefault", isDefault,
                "clusterConfig", Map.of(
                        "users", List.of(Map.of("name", "patch")))));
    }
}
