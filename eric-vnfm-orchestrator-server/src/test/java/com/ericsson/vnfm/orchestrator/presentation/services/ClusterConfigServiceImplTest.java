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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.model.ConfigFileStatus.NOT_IN_USE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NAMESPASES_MISSING_MSG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.NOT_SAME_CLUSTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.NO_VERIFICATION_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.VERIFICATION_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.VALUES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.multipartFileToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.locks.Lock;
import com.ericsson.am.shared.locks.LockManager;
import com.ericsson.am.shared.locks.LockMode;
import com.ericsson.vnfm.orchestrator.filters.ClusterConfigQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.ClusterConfigPatchRequest;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.TaskRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.validator.impl.ClusterDeregistrationValidatorImpl;
import com.ericsson.vnfm.orchestrator.validator.impl.ClusterRegistrationRespondentValidatorImpl;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;

import wiremock.org.apache.commons.lang3.RandomStringUtils;

@SpringBootTest(classes = {
        ClusterConfigServiceImpl.class,
        ClusterRegistrationRespondentValidatorImpl.class,
        ClusterDeregistrationValidatorImpl.class,
        LcmOperationsConfig.class
})
@MockBean(classes = {
        ClusterConfigInstanceRepository.class,
        VnfInstanceRepository.class,
        LifecycleOperationRepository.class,
        VnfInstanceNamespaceDetailsRepository.class,
        ScaleInfoRepository.class,
        OperationsInProgressRepository.class,
        ClusterConfigQuery.class,
        ChangedInfoRepository.class,
        VnfResourceViewRepository.class,
        HelmChartRepository.class,
        PartialSelectionQueryExecutor.class,
        TaskRepository.class,
        LifecycleOperationStageRepository.class
})
public class ClusterConfigServiceImplTest {

    private static final String VERIFICATION_UID = "6af5feca-2d49-4f3e-bb1b-c51bfd8d2733";
    private static final String CLUSTER_01_CONFIG = "cluster-config/cluster01.config";
    private static final String CLUSTER_02_CONFIG = "cluster-config/cluster02.config";
    private InputStream fileContent;
    private MultipartFile multipartFile;
    private ClusterConfigFile savedConfigFile;

    @Autowired
    private ClusterConfigServiceImpl clusterConfigService;

    @MockBean
    private ClusterConfigFileRepository configFileRepository;

    @MockBean
    private WorkflowService workflowService;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ClusterConfigQuery clusterConfigQuery;

    @MockBean
    private LockManager lockManager;

    @BeforeEach
    public void init() throws IOException, URISyntaxException {
        fileContent = createInputStream(CLUSTER_01_CONFIG);
        multipartFile = new MockMultipartFile(CLUSTER_01_CONFIG, CLUSTER_01_CONFIG, "text/plain", fileContent);
        String configFileAsString = multipartFileToString(multipartFile);
        savedConfigFile = ClusterConfigFile.builder()
                .name(multipartFile.getOriginalFilename())
                .content(configFileAsString)
                .status(NOT_IN_USE)
                .description("testing")
                .crdNamespace("eric-crd-ns")
                .clusterServer("")
                .verificationNamespaceUid(VERIFICATION_UID)
                .isDefault(true)
                .build();
        savedConfigFile.setId("1");
    }

    @Test
    public void testUpdateClusterConfig() {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        Mockito.when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setContent(multipartFileToString(multipartFile));
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);
        clusterConfigFile.setClusterServer("test");
        clusterConfigFile.setCrdNamespace("eric-crd-ns");

        ClusterConfigFile defaultConfig = new ClusterConfigFile();
        defaultConfig.setContent(multipartFileToString(multipartFile));
        defaultConfig.setName("default");
        defaultConfig.setDefault(true);
        defaultConfig.setClusterServer("test");
        defaultConfig.setCrdNamespace("eric-crd-ns");

        Mockito.when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        Mockito.when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(defaultConfig));
        Mockito.when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updatedClusterConfig = clusterConfigService.updateClusterConfig("test",
                                                                                          multipartFile,
                                                                                          "description",
                                                                                          true,
                                                                                          false);

        assertFalse(updatedClusterConfig.isDefault());
    }

    @Test
    public void testUpdateDefaultClusterConfig() {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        Mockito.when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setContent(multipartFileToString(multipartFile));
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);
        clusterConfigFile.setClusterServer("test");
        clusterConfigFile.setCrdNamespace("eric-crd-ns");

        ClusterConfigFile defaultConfig = new ClusterConfigFile();
        defaultConfig.setContent(multipartFileToString(multipartFile));
        defaultConfig.setName("default");
        defaultConfig.setDefault(true);
        defaultConfig.setClusterServer("test");
        defaultConfig.setCrdNamespace("eric-crd-ns");

        Mockito.when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        Mockito.when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(defaultConfig));
        Mockito.when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updateClusterConfig = clusterConfigService.updateClusterConfig("test",
                                                                                         multipartFile,
                                                                                         "test",
                                                                                         true,
                                                                                         true);
        assertTrue(updateClusterConfig.isDefault());
    }

    @Test
    public void testUpdateClusterConfigWhenUnmarkDefaultConfig() {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        Mockito.when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setContent(multipartFileToString(multipartFile));
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(true);
        clusterConfigFile.setClusterServer("test");
        clusterConfigFile.setCrdNamespace("eric-crd-ns");

        Mockito.when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        Mockito.when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(clusterConfigFile));

        ValidationException exception = assertThrows(ValidationException.class, () -> clusterConfigService.updateClusterConfig("test",
                                                                                                                               multipartFile,
                                                                                                                               "test",
                                                                                                                               true,
                                                                                                                               false));
        assertEquals("One of the clusters must be marked as default", exception.getMessage());
    }

    @Test
    public void testRegisterConfigFileWithValidConfig() {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        when(databaseInteractionService.getClusterConfigByName(anyString())).thenReturn(Optional.empty());
        when(databaseInteractionService.saveClusterConfig(any())).thenReturn(savedConfigFile);
        when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);
        final ClusterConfigFile clusterConfigFile = clusterConfigService.prepareRegistrationClusterConfig(multipartFile, null, "eric-crd-ns", false);
        clusterConfigService.registerClusterConfig(clusterConfigFile);
        assertTrue(clusterConfigFile.isDefault());
    }

    @Test
    public void testRegisterConfigFileWithDescriptionLengthMoreThenMaxLimit() {
        String descriptionMock = RandomStringUtils.random(251);
        assertThatThrownBy(() -> {
            final ClusterConfigFile clusterConfigFile = clusterConfigService.prepareRegistrationClusterConfig(multipartFile,
                                                                                                              descriptionMock,
                                                                                                              "eric-crd-ns",
                                                                                                              false);
            clusterConfigService.registerClusterConfig(clusterConfigFile);
        })
                .isInstanceOf(ValidationException.class)
                .hasMessage("Description should not be longer then 250 characters");
    }

    @Test
    public void testRegisterConfigFileWithKubeNamespaceForCRD() {
        assertThatThrownBy(() -> {
            final ClusterConfigFile clusterConfigFile = clusterConfigService.prepareRegistrationClusterConfig(multipartFile,
                                                                                                              null,
                                                                                                              "kube-system",
                                                                                                              false);
            clusterConfigService.registerClusterConfig(clusterConfigFile);
        })
                .isInstanceOf(ValidationException.class)
                .hasMessage("Kubernetes reserved namespace cannot be used as a CRD namespace.");
    }

    @Test
    public void testGetConfigFileByName() {
        when(databaseInteractionService.getClusterConfigByName(anyString())).thenReturn(Optional.of(savedConfigFile));
        final ClusterConfigFile fileByName = clusterConfigService.getConfigFileByName("filename");
        assertEquals(savedConfigFile, fileByName);
    }

    @Test
    public void testGetConfigFileByNameThrowsError() {
        assertThatThrownBy(() -> clusterConfigService.getConfigFileByName("doesntExist.config"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Cluster config file doesntExist.config not found");
    }

    @Test
    public void testDeregisterConfigFile() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(VALUES);
        clusterConfigFile.setDefault(false);

        when(configFileRepository.findByName(VALUES)).thenReturn(Optional.of(clusterConfigFile));
        Lock mockedLock = mockLock(true);
        when(lockManager.getLock(any(LockMode.class), anyString())).thenReturn(mockedLock);
        assertThatNoException().isThrownBy(
                () -> clusterConfigService.deregisterClusterConfig(VALUES));
    }

    @Test
    public void testDeregisterConfigFileWhenClusterIsDefault() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(VALUES);
        clusterConfigFile.setDefault(true);

        when(configFileRepository.findByName(VALUES)).thenReturn(Optional.of(clusterConfigFile));
        Lock mockedLock = mockLock(true);
        when(lockManager.getLock(any(LockMode.class), anyString())).thenReturn(mockedLock);
        assertThatThrownBy(() -> clusterConfigService.deregisterClusterConfig(VALUES))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Default cluster config can not be deregister.", "Cluster operation failed.",
                            "Default cluster config can not be removed.");
    }

    @Test
    public void testDeregisterConfigFileWhenConfigNotExist() {
        when(databaseInteractionService.getClusterConfigByName(ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        Lock mockedLock = mockLock(true);
        when(lockManager.getLock(any(LockMode.class), anyString())).thenReturn(mockedLock);
        assertThatThrownBy(() -> clusterConfigService.deregisterClusterConfig(CLUSTER_01_CONFIG))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Cluster config file " + CLUSTER_01_CONFIG + " does not exist.");
    }

    @Test
    public void testDeregisterConfigFileWhenClusterIsBeingUsingByCnfPackage() {
        ClusterConfigFile configFile = new ClusterConfigFile();
        configFile.setStatus(ConfigFileStatus.IN_USE);
        when(configFileRepository.findByName(any())).thenReturn(Optional.of(configFile));
        Lock mockedLock = mockLock(true);
        when(lockManager.getLock(any(LockMode.class), anyString())).thenReturn(mockedLock);
        assertThatThrownBy(() -> clusterConfigService.deregisterClusterConfig(CLUSTER_01_CONFIG))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Cluster config file " + CLUSTER_01_CONFIG + " is in use and not available for deletion.");
    }

    @Test
    public void testDeregisterConfigFileWhenClusterIsLocked() {
        ClusterConfigFile configFile = new ClusterConfigFile();
        configFile.setStatus(NOT_IN_USE);
        when(configFileRepository.findByName(any())).thenReturn(Optional.of(configFile));
        Lock mockedLock = mockLock(false);
        when(lockManager.getLock(any(LockMode.class), anyString())).thenReturn(mockedLock);
        assertThatThrownBy(() -> clusterConfigService.deregisterClusterConfig(CLUSTER_01_CONFIG))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Cluster config file " + CLUSTER_01_CONFIG + " is in use and not available for deletion.");
    }

    @Test
    public void registerNewDefaultClusterWhenDefaultClusterAlreadyExists() throws Exception {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        MultipartFile secondCluster = new MockMultipartFile(CLUSTER_02_CONFIG, CLUSTER_02_CONFIG, "text/plain", createInputStream(CLUSTER_02_CONFIG));
        String configFileAsString = multipartFileToString(secondCluster);
        ClusterConfigFile currentClusterConfig = ClusterConfigFile.builder()
                .name(secondCluster.getOriginalFilename())
                .content(configFileAsString)
                .status(NOT_IN_USE)
                .description("testing")
                .crdNamespace("eric-crd-ns")
                .clusterServer("")
                .verificationNamespaceUid(VERIFICATION_UID)
                .isDefault(true)
                .build();

        when(databaseInteractionService.getClusterConfigByName(anyString())).thenReturn(Optional.empty());
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(savedConfigFile));
        when(databaseInteractionService.saveClusterConfig(any())).thenReturn(currentClusterConfig);
        when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);

        final ClusterConfigFile clusterConfigFile = clusterConfigService.prepareRegistrationClusterConfig(secondCluster, null, "eric-crd-ns", true);
        clusterConfigService.registerClusterConfig(clusterConfigFile);
        assertTrue(clusterConfigFile.isDefault());

        assertFalse(savedConfigFile.isDefault());
        savedConfigFile.setDefault(true);
    }

    @Test
    public void registerNotDefaultClusterWhenDefaultClusterExists() throws Exception {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        MultipartFile secondCluster = new MockMultipartFile(CLUSTER_02_CONFIG, CLUSTER_02_CONFIG, "text/plain", createInputStream(CLUSTER_02_CONFIG));
        String configFileAsString = multipartFileToString(secondCluster);
        ClusterConfigFile currentClusterConfig = ClusterConfigFile.builder()
                .name(secondCluster.getOriginalFilename())
                .content(configFileAsString)
                .status(NOT_IN_USE)
                .description("testing")
                .crdNamespace("eric-crd-ns")
                .clusterServer("")
                .verificationNamespaceUid(VERIFICATION_UID)
                .isDefault(false)
                .build();

        when(databaseInteractionService.getClusterConfigByName(anyString())).thenReturn(Optional.empty());
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(savedConfigFile));
        when(databaseInteractionService.saveClusterConfig(any())).thenReturn(currentClusterConfig);
        when(workflowService.validateClusterConfigFile(any())).thenReturn(validResponse);

        final ClusterConfigFile clusterConfigFile = clusterConfigService.prepareRegistrationClusterConfig(secondCluster, null, "eric-crd-ns", false);
        clusterConfigService.registerClusterConfig(clusterConfigFile);
        assertFalse(clusterConfigFile.isDefault());

        assertTrue(savedConfigFile.isDefault());
    }

    @Test
    public void testVerifySameClusterUidWithUidMatch() {
        ClusterServerDetailsResponse validResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(VERIFICATION_UID))
                );
        assertThatNoException().isThrownBy(
                () -> clusterConfigService.verifySameClusterByUid(savedConfigFile, validResponse));
    }

    @Test
    public void testVerifySameClusterUidWithUidMismatch() {
        String uid = UUID.randomUUID().toString();
        ClusterServerDetailsResponse invalidResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(uid))
                );
        assertThatThrownBy(() -> clusterConfigService.verifySameClusterByUid(savedConfigFile, invalidResponse))
                .isInstanceOf(ValidationException.class).hasMessage(NOT_SAME_CLUSTER);
    }

    @Test
    public void testVerifySameClusterUidWithMissingNamespace() {
        String uid = UUID.randomUUID().toString();
        ClusterServerDetailsResponse invalidResponse = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name("VERIFICATION_NAMESPACE").uid(uid))
                );
        assertThatThrownBy(() -> clusterConfigService.verifySameClusterByUid(savedConfigFile, invalidResponse))
                .isInstanceOf(ValidationException.class).hasMessage(NO_VERIFICATION_NAMESPACE);
    }

    @Test
    public void testVerifySameClusterByNamespacesExists() {
        ClusterServerDetailsResponse clusterServerDetails = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(UUID.randomUUID().toString()),
                                new Namespace().name("vnf_ns_1").uid(UUID.randomUUID().toString()),
                                new Namespace().name("vnf_ns_2").uid(UUID.randomUUID().toString()))
                );
        when(databaseInteractionService.getNamespacesAssociatedWithCluster(any()))
                .thenReturn(List.of("vnf_ns_1", "vnf_ns_2"));
        assertThatNoException().isThrownBy(() ->
                                                   clusterConfigService.verifySameClusterByNamespaces("testCluster", clusterServerDetails));
    }

    @Test
    public void testVerifySameClusterByNamespacesMissing() {
        ClusterServerDetailsResponse clusterServerDetails = new ClusterServerDetailsResponse()
                .hostUrl("valid_host")
                .apiKey("valid_token")
                .namespaces(
                        List.of(new Namespace().name(VERIFICATION_NAMESPACE).uid(UUID.randomUUID().toString()),
                                new Namespace().name("vnf_ns_1").uid(UUID.randomUUID().toString()),
                                new Namespace().name("vnf_ns_3").uid(UUID.randomUUID().toString()),
                                new Namespace().name("vnf_ns_4").uid(UUID.randomUUID().toString()))
                );
        when(databaseInteractionService.getNamespacesAssociatedWithCluster(any()))
                .thenReturn(List.of("vnf_ns_1", "vnf_ns_2"));
        assertThatThrownBy(() ->
                                   clusterConfigService.verifySameClusterByNamespaces("testCluster", clusterServerDetails))
                .isInstanceOf(ValidationException.class)
                .hasMessage(String.format(CLUSTER_NAMESPASES_MISSING_MSG, "vnf_ns_2"));
    }

    @Test
    public void testMergePatchJson() {
        Map<String, List<String>> tests = new HashMap<>();
        tests.put("{\"a\":\"c\"}", Stream.of("{\"a\":\"b\"}", "{\"a\":\"c\"}").collect(Collectors.toList()));
        tests.put("{\"a\":\"b\",\"b\":\"c\"}", Stream.of("{\"a\":\"b\"}", "{\"b\":\"c\"}").collect(Collectors.toList()));
        tests.put("{}", Stream.of("{\"a\":\"b\"}", "{\"a\":null}").collect(Collectors.toList()));
        tests.put("{\"b\":\"c\"}", Stream.of("{\"a\":\"b\",\"b\":\"c\"}", "{\"a\":null}").collect(Collectors.toList()));
        tests.put("{\"a\":\"d\"}", Stream.of("{\"a\":[\"b\"]}", "{\"a\":\"d\"}").collect(Collectors.toList()));
        tests.put("{\"a\":[\"b\"]}", Stream.of("{\"a\":\"c\"}", "{\"a\":[\"b\"]}").collect(Collectors.toList()));
        tests.put("{\"a\": {\"b\": \"d\"}}",
                  Stream.of("{\"a\": {\"b\": \"c\"}}", "{\"a\": {\"b\": \"d\",\"c\": null}}").collect(Collectors.toList()));
        tests.put("{\"a\": [1]}", Stream.of("{\"a\": [{\"b\":\"c\"}]}", "{\"a\": [1]}").collect(Collectors.toList()));
        tests.put("{\"a\":{\"bb\":{}}}", Stream.of("{}", "{\"a\":{\"bb\":{}}}").collect(Collectors.toList()));
        tests.put("{\"e\":null,\"a\":1}", Stream.of("{\"e\":null}", "{\"a\":1}").collect(Collectors.toList()));

        for (String key : tests.keySet()) {
            List<String> list = tests.get(key);
            JSONObject customResult = clusterConfigService.mergePatchJson(new JSONObject(list.get(0)), new JSONObject(list.get(1)));
            JSONObject realResult = new JSONObject(key);
            assertEquals(customResult.toMap(), realResult.toMap());
        }
    }

    @Test
    public void testModifyClusterConfigCaseIsDefaultTrue() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);

        ClusterConfigFile defaultConfig = new ClusterConfigFile();
        defaultConfig.setName("default");
        defaultConfig.setDefault(true);

        ClusterConfigPatchRequest updateFields = new ClusterConfigPatchRequest();
        updateFields.setIsDefault(true);

        when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(defaultConfig));
        when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updatedClusterConfig = clusterConfigService.modifyClusterConfig("test",
                                                                                          updateFields,
                                                                                          true);
        assertThat(updatedClusterConfig.isDefault()).isTrue();
    }

    @Test
    public void testModifyClusterConfigCaseIsDefaultFalse() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);

        ClusterConfigFile defaultConfig = new ClusterConfigFile();
        defaultConfig.setName("default");
        defaultConfig.setDefault(true);

        ClusterConfigPatchRequest updateFields = new ClusterConfigPatchRequest();
        updateFields.setIsDefault(false);

        when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.of(defaultConfig));
        when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updatedClusterConfig = clusterConfigService.modifyClusterConfig("test",
                                                                                          updateFields,
                                                                                          true);
        assertThat(updatedClusterConfig.isDefault()).isFalse();
        assertThat(defaultConfig.isDefault()).isTrue();
    }

    @Test
    public void testModifyClusterConfigCaseIsDefaultTrueAndNoDefaultClusterExists() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);

        ClusterConfigPatchRequest updateFields = new ClusterConfigPatchRequest();
        updateFields.setIsDefault(true);

        when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.empty());
        when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updatedClusterConfig = clusterConfigService.modifyClusterConfig("test",
                                                                                          updateFields,
                                                                                          true);
        assertThat(updatedClusterConfig.isDefault()).isTrue();
    }

    @Test
    public void testModifyClusterConfigCaseIsDefaultFalseAndNoDefaultClusterExists() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("test");
        clusterConfigFile.setDefault(false);

        ClusterConfigPatchRequest updateFields = new ClusterConfigPatchRequest();
        updateFields.setIsDefault(false);

        when(databaseInteractionService.getClusterConfigByName(any())).thenReturn(Optional.of(clusterConfigFile));
        when(databaseInteractionService.getDefaultCluster()).thenReturn(Optional.empty());
        when(databaseInteractionService.saveClusterConfig(clusterConfigFile)).thenReturn(clusterConfigFile);

        ClusterConfigFile updatedClusterConfig = clusterConfigService.modifyClusterConfig("test",
                                                                                          updateFields,
                                                                                          true);
        assertThat(updatedClusterConfig.isDefault()).isFalse();
    }

    @Test
    public void testPaginatedResponseWithoutFilter() {
        List<ClusterConfigFile> configs = populateClusterConfigs();
        int pages = configs.size() / 3;
        int tail = configs.size() % 3;
        if (tail > 0) {
            pages++;
        } else {
            tail = 3;
        }

        Pageable pageableFirst = PageRequest.of(0, 3, Sort.Direction.DESC, "name");
        configs.sort(Comparator.comparing(ClusterConfigFile::getName).reversed());

        given(configFileRepository.findAll(
                any(Pageable.class)
        )).willAnswer(invocationOnMock -> new PageImpl<>(configs.subList(0, 3), invocationOnMock.getArgument(0), 15));

        configs.sort(Comparator.comparing(ClusterConfigFile::getName).reversed());
        Page<ClusterConfigFile> page = clusterConfigService.getClusterConfigs(
                "", pageableFirst);
        assertEquals(3, page.getContent().size());
        assertEquals(configs.size(), page.getTotalElements());
        assertEquals(pages, page.getTotalPages());
        assertEquals(configs.subList(0, 3).stream().map(ClusterConfigFile::getId).collect(Collectors.toList()),
                     page.getContent().stream().map(ClusterConfigFile::getId).collect(Collectors.toList()));

        given(configFileRepository.findAll(
                any(Pageable.class)
        )).willAnswer(invocationOnMock -> new PageImpl<>(configs.subList(12, 15), invocationOnMock.getArgument(0),
                                                         15));

        Pageable pageableSecond = PageRequest.of(pages - 1, 3, Sort.Direction.DESC, "name");

        page = clusterConfigService.getClusterConfigs(
                "", pageableSecond);
        assertEquals(tail, page.getContent().size());
        assertEquals(configs.subList(configs.size() - tail, configs.size()).stream()
                             .map(ClusterConfigFile::getId).collect(Collectors.toList()),
                     page.getContent().stream().map(ClusterConfigFile::getId).collect(Collectors.toList()));
    }

    @Test
    public void testPaginatedResponseWithFilter() {
        List<ClusterConfigFile> configs = populateClusterConfigs();

        List<ClusterConfigFile> filteredConfigs = configs.stream()
                .filter(s -> s.getName().contains("1"))
                .filter(s -> s.getStatus() == NOT_IN_USE)
                .collect(Collectors.toList());

        filteredConfigs.sort(Comparator.comparing(ClusterConfigFile::getName));
        String filter = "(cont,name,1);(eq,status,NOT_IN_USE)";

        given(clusterConfigQuery.getPageWithFilter(
                eq(filter), any(Pageable.class)
        )).willAnswer(invocationOnMock -> new PageImpl<>(filteredConfigs,
                                                         invocationOnMock.getArgument(1),
                                                         5));

        Page<ClusterConfigFile> page = clusterConfigService.getClusterConfigs(
                filter, PageRequest.of(0, 5, Sort.by("name")));
        assertEquals(filteredConfigs.size(), page.getTotalElements());
        assertEquals(Math.min(filteredConfigs.size(), 5), page.getContent().size());
    }

    @Test
    public void testPaginatedResponseForCismCluster() {
        List<ClusterConfigFile> configs = populateClusterConfigs();
        int pages = configs.size() / 4;
        int tail = configs.size() % 4;
        if (tail > 0) {
            pages++;
        } else {
            tail = 4;
        }

        configs.sort(Comparator.comparing(ClusterConfigFile::getName).reversed());

        given(configFileRepository.findAll(
                any(Pageable.class)
        )).willAnswer(invocationOnMock -> new PageImpl<>(configs.subList(0, 4), invocationOnMock.getArgument(0),
                                                         15));

        Page<ClusterConfigFile> page = clusterConfigService.getCismClusterConfigs(PageRequest.of(0, 4, Sort.Direction.DESC, "name"));
        assertEquals(4, page.getContent().size());
        assertEquals(configs.size(), page.getTotalElements());
        assertEquals(pages, page.getTotalPages());
        assertEquals(configs.subList(0, 4).stream().map(ClusterConfigFile::getId).collect(Collectors.toList()),
                     page.getContent().stream().map(ClusterConfigFile::getId).collect(Collectors.toList()));

        given(configFileRepository.findAll(
                any(Pageable.class)
        )).willAnswer(invocationOnMock -> new PageImpl<>(configs.subList(12, 15), invocationOnMock.getArgument(0),
                                                         15));

        page = clusterConfigService.getCismClusterConfigs(PageRequest.of(pages - 1, 4, Sort.Direction.DESC, "name"));
        assertEquals(tail, page.getContent().size());
        assertEquals(configs.subList(configs.size() - tail, configs.size()).stream()
                             .map(ClusterConfigFile::getId).collect(Collectors.toList()),
                     page.getContent().stream().map(ClusterConfigFile::getId).collect(Collectors.toList()));
    }

    private List<ClusterConfigFile> populateClusterConfigs() {
        List<ClusterConfigFile> configs = new ArrayList<>();
        configs.add(createClusterConfig("default", "default.config", ConfigFileStatus.IN_USE, "Default cluster config file", "eric-crd-ns", true));
        configs.add(createClusterConfig("503",
                                        "cluster503ForDeregister.config",
                                        NOT_IN_USE,
                                        "Description for config file 503",
                                        "eric-crd-ns", false));
        for (int i = 1; i < 14; i++) {
            String clusterName = "cluster" + i + ".config";
            String description = "Description for config file " + i;
            String namespace = "namespace-" + i;
            configs.add(createClusterConfig(String.valueOf(i), clusterName, NOT_IN_USE, description, namespace, false));
        }
        return configs;
    }

    private ClusterConfigFile createClusterConfig(String id, String name, ConfigFileStatus status, String description, String ns, boolean isDefault) {
        final ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId(id);
        clusterConfigFile.setName(name);
        clusterConfigFile.setStatus(status);
        clusterConfigFile.setDescription(description);
        clusterConfigFile.setCrdNamespace(ns);
        clusterConfigFile.setDefault(isDefault);

        return clusterConfigFile;
    }

    private InputStream createInputStream(String fileName) throws IOException {
        return Files.newInputStream(getResource(getClass(), fileName));
    }

    private Lock mockLock(boolean lockAcquired) {
        Lock lock = mock(Lock.class);
        when(lock.withAcquireRetries(anyInt(), anyLong())).then(InvocationOnMock::getMock);
        when(lock.lock(anyLong(), any())).thenReturn(lockAcquired);

        return lock;
    }
}
