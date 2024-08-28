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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.INSTANTIATE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.DelayUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class HealOperationTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Autowired
    private HealOperation healOperation;

    @Autowired
    private OssNodeService ossNodeService;

    @MockBean
    private RestoreBackupFromEnm restoreBackupFromEnm;

    @Autowired
    private InstantiateOperation instantiateOperation;

    @SpyBean
    private WorkflowRoutingServicePassThrough workflowRoutingService;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @MockBean
    private ClusterConfigInstanceRepository clusterConfigInstanceRepository;

    @MockBean
    private OnboardingClient onboardingClient;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @BeforeEach
    public void setupOnboardingClient() {
        Mockito.when(onboardingClient.get(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new PackageResponse[]{new PackageResponse()}));
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(healOperation, "ossNodeService", ossNodeService);
        ReflectionTestUtils.setField(ossNodeService, "restoreBackupFromEnm", restoreBackupFromEnm);

        ClusterConfigFile cluster = new ClusterConfigFile();
        cluster.setName("heal-operation");
        cluster.setClusterServer("heal-operation-cluster-server");
        cluster.setContent("heal-content");
        cluster.setStatus(ConfigFileStatus.IN_USE);
        cluster.setDefault(false);

        when(clusterConfigFileRepository.findClusterServerByClusterName(any())).thenReturn(Optional.of(cluster.getClusterServer()));
        when(clusterConfigFileRepository.findByName(any())).thenReturn(Optional.of(cluster));
    }

    @Test
    public void getLastSuccessfulInstallOrUpgradeOperationTest() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h1def1ce-4cf4-477c-aab3-21c454e6666");
        Optional<LifecycleOperation> optionalOperation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0);
        LifecycleOperation operation = optionalOperation.get();
        assertThat(operation).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG); //legacy operation
        assertThat(operation.getStateEnteredTime()).isEqualTo("2020-08-03T12:12:49.824");
        assertThat(operation.getOperationOccurrenceId()).isEqualTo("h12bcbc1-474f-4673-91ee-656fd8366666");
    }

    @Test
    public void getLastSuccessfulInstallOrUpgradeOperationNotFoundTest() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h2def1ce-4cf4-477c-aab3-21c454e6666");
        Optional<LifecycleOperation> operation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0);
        assertThat(operation).isEmpty();
    }

    @Test
    public void getLastSuccessfulInstallOperationTest() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h1def1ce-4cf4-477c-aab3-21c454e6666");
        Optional<LifecycleOperation> optionalOperation = lcmOpSearchService.searchLastSuccessfulInstallOperation(vnfInstance);
        LifecycleOperation operation = optionalOperation.get();
        assertThat(operation).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(operation.getStateEnteredTime()).isEqualTo("2020-08-03T12:12:49.823");
        assertThat(operation.getOperationOccurrenceId()).isEqualTo("h1fbcbc1-474f-4673-91ee-656fd8366666");
    }

    @Test
    public void mergeInstantiatedWithChangePackageShouldReturnNull() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h4def1ce-4cf4-477c-aab3-21c454e6666");
        InstantiateVnfRequest instantiateVnfRequest = healOperation
                .mergeInstantiatedWithChangePackage(null, vnfInstance);
        assertThat(instantiateVnfRequest).isNull();
    }

    @Test
    public void createInstantiatedVnfRequestTest() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h1def1ce-4cf4-477c-aab3-21c454e6666");
        Optional<LifecycleOperation> lastSuccessfulInstallOrUpgradeOperation = lcmOpSearchService
                .searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0);
        InstantiateVnfRequest instantiatedVnfRequest = healOperation
                .getInstantiateVnfRequest(lastSuccessfulInstallOrUpgradeOperation.get(), vnfInstance);
        assertThat(((Map) instantiatedVnfRequest.getAdditionalParams()).size()).isEqualTo(5);
    }

    @Test
    public void healCompletedTest() {
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId("h1def1ce-4cf4-477c-aab3-21c454e6666");
        assertThat(vnfInstanceBefore.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder("COMPLETED", "COMPLETED");
        assertThat(vnfInstanceBefore.getCombinedAdditionalParams()).isEqualTo("{\"skipVerification\": true, "
                                                                                      + "\"skipJobVerification\": true,"
                                                                                      + "\"applicationTimeOut\":\"300\"}");
        assertThat(vnfInstanceBefore.getCombinedValuesFile())
                .isEqualTo("{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}}}");
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getOperationParams()).isEqualTo("{\"cause\":\"latest\"}");
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getCombinedValuesFile()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        Optional<VnfInstanceNamespaceDetails> namespaceDetails = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(vnfInstanceBefore.getVnfInstanceId());
        assertThat(namespaceDetails.isPresent()).isFalse();

        LifecycleOperation lastInstallOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId("h12bcbc1-474f-4673-91ee-656fd8366666");
        lastInstallOperation.setCombinedAdditionalParams(vnfInstanceBefore.getCombinedAdditionalParams());
        lifecycleOperationRepository.save(lastInstallOperation);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operationBefore.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId("h13bcbc1-474f-4673-91ee-656fd8366666");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h1def1ce-4cf4-477c-aab3-21c454e6666");
        LifecycleOperation operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operation.getStateEnteredTime()).isEqualTo("2020-08-03T12:12:49.825");
        assertThat(operation.getOperationOccurrenceId()).isEqualTo("h13bcbc1-474f-4673-91ee-656fd8366666");
        assertThat(operation.getOperationParams()).isEqualTo("{\"cause\":\"latest\"}");
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo("{\"skipVerification\": true, "
                                                                              + "\"skipJobVerification\": true,\"applicationTimeOut\":\"300\"}");
        assertThat(operation.getCombinedValuesFile())
                .isEqualTo("{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}}}");
        assertThat(operation.getValuesFileParams()).isEqualTo(null);
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder("PROCESSING", null);

        Optional<VnfInstanceNamespaceDetails> namespaceDetailsAfter = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(vnfInstance.getVnfInstanceId());
        assertThat(namespaceDetailsAfter.isPresent()).isTrue();
    }

    @Test
    public void helmCompletedOperationNotFoundTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId("h24bcbc1-474f-4673-91ee-656fd8366666");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h2def1ce-4cf4-477c-aab3-21c454e6666");
        LifecycleOperation operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation).isNotNull();
        assertThat(operation.getError()).isEqualTo(
                "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Unable to retrieve a previous completed install or"
                        + " upgrade operation for VnfInstance h2def1ce-4cf4-477c-aab3-21c454e6666\",\"instance\":\"about:blank\"}");
        assertThat(operation.getOperationState().toString()).isEqualTo("FAILED");
    }

    @Test
    public void helmCompletedInstantiationOperationNotFoundTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId("h51bcbc1-474f-4673-91ee-656fd8366666");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h5def1ce-4cf4-477c-aab3-21c454e6666");
        LifecycleOperation operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation).isNotNull();
        assertThat(operation.getError()).isEqualTo(
                "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Unable to create the instantiate request for "
                        + "VnfInstance h5def1ce-4cf4-477c-aab3-21c454e6666\",\"instance\":\"about:blank\"}");
        assertThat(operation.getOperationState().toString()).isEqualTo("FAILED");
    }

    @Test
    public void healCompletedTestWithOtp() {
        String instanceWithOtp = "o9i99iuq-4cf4-477c-aab3-21c454e6666";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId(instanceWithOtp);
        assertThat(vnfInstanceBefore.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder("COMPLETED");
        assertThat(vnfInstanceBefore.getCombinedValuesFile()).isNull();
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getOperationParams()).isEqualTo("{\"cause\":\"latest\", \"additionalParams\":{\"ipVersion\": \"ipv6\"}}");
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getCombinedValuesFile()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(100));
        operationBefore.setApplicationTimeout("120");
        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(vnfInstanceBefore.getOperationOccurrenceId());
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(instanceWithOtp);
        LifecycleOperation operationAfter = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfter.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationAfter.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operationAfter.getStateEnteredTime()).isEqualTo("2020-08-04T12:12:49.825");
        assertThat(operationAfter.getOperationOccurrenceId()).isEqualTo("vf7vju8f-474f-4673-91ee-656fd8366666");
        assertThat(operationAfter.getOperationParams()).isEqualTo("{\"cause\":\"latest\", \"additionalParams\":{\"ipVersion\": \"ipv6\"}}");
        assertThat(operationAfter.getCombinedAdditionalParams()).isNull();
        assertThat(vnfInstance.getHelmCharts()).extracting("releaseName").containsExactlyInAnyOrder("heal-operation-otp-3");
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder("PROCESSING");
        //Instantiate completed.
        message.setOperationType(INSTANTIATE);
        message.setReleaseName("heal-operation-otp-3");
        message.setState(HelmReleaseState.COMPLETED);
        instantiateOperation.completed(message);
        LifecycleOperation operationAfterInstantiate = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operationAfterInstantiate.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
    }

    @Test
    public void healCompletedFailedWithoutOtp() {
        String instanceWithoutOtp = "jifuje87-4cf4-477c-aab3-21c454e6666";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId(instanceWithoutOtp);
        assertThat(vnfInstanceBefore.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder("COMPLETED");
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getOperationParams()).isEqualTo(
                "{\"cause\":\"Full Restore\", \"additionalParams\":{\"ipVersion\": \"ipv4\", \"restore.backupFileReference\": "
                        + "\"sftp://users@14BCP04/my-backup\", \"restore.password\": \"password\"}}");
        assertThat(operationBefore.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId("oki98ec3-474f-4673-91ee-656fd8366666");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(instanceWithoutOtp);
        LifecycleOperation operationAfter = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfter.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(operationAfter.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operationAfter.getOperationOccurrenceId()).isEqualTo("oki98ec3-474f-4673-91ee-656fd8366666");
        assertThat(operationAfter.getError()).contains("Unable to retrieve oss node protocol file for VnfInstance");
    }

    @Test
    public void operationCompletesNotAddedToOss() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "23ty78oi-b16d-45fb-acb2-f2c631cb19ed";
        completed.setLifecycleOperationId(lifecycleOperationId);
        healOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    @Test
    public void healCompletedTestWithCNFRestore() {
        String instanceWithOtp = "o9i99iuq-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId(instanceWithOtp);
        assertThat(vnfInstanceBefore.getCombinedValuesFile()).isNull();

        vnfInstanceRepository.save(vnfInstanceBefore);
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(100));
        operationBefore.setApplicationTimeout("120");

        when(restoreBackupFromEnm.restoreBackup(any(), any())).thenReturn(true);
        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();
        whenWfsSecretsRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(vnfInstanceBefore.getOperationOccurrenceId());
        message.setOperationType(HelmReleaseOperationType.TERMINATE);

        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(instanceWithOtp);
        LifecycleOperation operationAfter = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfter.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationAfter.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operationAfter.getCombinedAdditionalParams()).isNull();
        assertThat(vnfInstance.getHelmCharts()).extracting("releaseName").containsExactlyInAnyOrder("heal-operation-otp-3");
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder("PROCESSING");
        //Instantiate completed.
        message.setOperationType(INSTANTIATE);
        message.setReleaseName("heal-operation-otp-3");
        message.setState(HelmReleaseState.COMPLETED);
        instantiateOperation.completed(message);
        DelayUtils.delaySeconds(2);

        verify(restoreBackupFromEnm, times(1)).restoreBackup(any(), any());
        LifecycleOperation operationAfterInstantiate = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operationAfterInstantiate.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
    }

    @Test
    public void healCompletedTestWithCNARestore() {
        String instance = "o9i99iuq-4cf4-477c-aab3-21c454e1111";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId(instance);
        assertThat(vnfInstanceBefore.getCombinedValuesFile()).isNull();

        vnfInstanceRepository.save(vnfInstanceBefore);
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getVnfInstance().getOssNodeProtocolFile()).isEqualTo(null);
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(100));
        operationBefore.setApplicationTimeout("120");

        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(vnfInstanceBefore.getOperationOccurrenceId());
        message.setOperationType(HelmReleaseOperationType.TERMINATE);

        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(instance);
        LifecycleOperation operationAfter = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfter.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationAfter.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operationAfter.getCombinedAdditionalParams()).isNull();
        assertThat(operationAfter.getOperationParams()).contains("restore.scope");
        assertThat(operationAfter.getOperationParams()).contains("restore.backupName");
        assertThat(operationAfter.getOperationParams()).contains("day0.configuration.secretname");
        assertThat(vnfInstance.getHelmCharts()).extracting("releaseName").containsExactlyInAnyOrder("heal-operation");
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder("PROCESSING");
        assertThat(vnfInstance.getSensitiveInfo()).isNull();
        //Instantiate completed.
        message.setOperationType(INSTANTIATE);
        message.setReleaseName("heal-operation");
        message.setState(HelmReleaseState.COMPLETED);
        instantiateOperation.completed(message);

        LifecycleOperation operationAfterInstantiate = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operationAfterInstantiate.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
    }

    @Test
    public void healCompletedTestWithCrds() {
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId("vnfi100e-4cf4-477c-aab3-21c454e6666");
        assertThat(vnfInstanceBefore.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder("COMPLETED", "COMPLETED");
        assertThat(vnfInstanceBefore.getCombinedAdditionalParams()).isEqualTo("{\"skipVerification\": true, "
                                                                                      + "\"skipJobVerification\": true,"
                                                                                      + "\"applicationTimeOut\":\"300\"}");
        assertThat(vnfInstanceBefore.getCombinedValuesFile())
                .isEqualTo("{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}}}");
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getOperationParams()).isEqualTo("{\"cause\":\"latest\"}");
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getCombinedValuesFile()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        LifecycleOperation lastInstallOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId("oo101bc1-474f-4673-91ee-656fd8366666");
        lastInstallOperation.setCombinedAdditionalParams(vnfInstanceBefore.getCombinedAdditionalParams());
        lifecycleOperationRepository.save(lastInstallOperation);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operationBefore.setApplicationTimeout("80");
        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId("oo104bc1-474f-4673-91ee-656fd8366666");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        message.setReleaseName("crd-operation-1");

        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("vnfi100e-4cf4-477c-aab3-21c454e6666");
        LifecycleOperation operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operation.getStateEnteredTime()).isEqualTo("2020-08-03T12:12:49.825");
        assertThat(operation.getOperationOccurrenceId()).isEqualTo("oo104bc1-474f-4673-91ee-656fd8366666");
        assertThat(operation.getOperationParams()).isEqualTo("{\"cause\":\"latest\"}");
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo("{\"skipVerification\": true, "
                                                                              + "\"skipJobVerification\": true,\"applicationTimeOut\":\"300\"}");
        assertThat(operation.getCombinedValuesFile())
                .isEqualTo("{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}}}");
        assertThat(operation.getValuesFileParams()).isEqualTo(null);
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder(null, "PROCESSING");
        verify(workflowRoutingService, times(1))
                .routeInstantiateRequest(anyInt(), any(LifecycleOperation.class), any(VnfInstance.class));
    }

    @Test
    public void healCompletedTestWithCNFRestoreAndSkipCrd() {
        String instanceWithOtp = "vnfi200q-4cf4-477c-aab3-21c454e7777";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository
                .findByVnfInstanceId(instanceWithOtp);
        assertThat(vnfInstanceBefore.getCombinedValuesFile()).isNull();

        vnfInstanceRepository.save(vnfInstanceBefore);
        LifecycleOperation operationBefore = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(operationBefore.getCombinedAdditionalParams()).isEqualTo(null);
        assertThat(operationBefore.getValuesFileParams()).isEqualTo(null);

        operationBefore.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(100));
        operationBefore.setApplicationTimeout("120");

        when(restoreBackupFromEnm.restoreBackup(any(), any())).thenReturn(true);
        lifecycleOperationRepository.save(operationBefore);

        whenWfsResourcesRespondsWithAccepted();
        whenWfsSecretsRespondsWithAccepted();

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(vnfInstanceBefore.getOperationOccurrenceId());
        message.setOperationType(HelmReleaseOperationType.TERMINATE);

        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(instanceWithOtp);
        LifecycleOperation operationAfter = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfter.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationAfter.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(operationAfter.getCombinedAdditionalParams()).isNull();
        assertThat(vnfInstance.getHelmCharts()).extracting("releaseName")
                .containsExactlyInAnyOrder("heal-operation-otp-200", "heal-operation-otp-200");
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsExactlyInAnyOrder("PROCESSING", null);
        //Instantiate completed.
        message.setOperationType(INSTANTIATE);
        message.setReleaseName("heal-operation-otp-200");
        message.setState(HelmReleaseState.COMPLETED);
        instantiateOperation.completed(message);
        DelayUtils.delaySeconds(2);

        verify(restoreBackupFromEnm, times(1)).restoreBackup(any(), any());
        LifecycleOperation operationAfterInstantiate = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operationAfterInstantiate.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(operationAfterInstantiate.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        verify(workflowRoutingService, times(1))
                .routeInstantiateRequest(anyInt(), any(LifecycleOperation.class), any(VnfInstance.class));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted() {
        when(restTemplate.exchange(endsWith("instantiate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsSecretsRespondsWithAccepted() {
        when(restTemplate.exchange(contains("secrets"), eq(HttpMethod.PUT), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    @Test
    public void testHealOperationShouldHaveEnabledChartsOnly() {
        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenReturn(ResponseEntity.ok().build());

        final String vnfInstanceId = "h10bcbc1-474f-4673-91ee-656fd8388888";
        final String lifecycleOperationId = "oo104bc1-474f-4673-91ee-656fd8366668";
        VnfInstance vnfInstanceBefore = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        List<HelmChart> helmCharts = vnfInstanceBefore.getHelmCharts();

        assertThat(helmCharts.isEmpty()).isFalse();
        assertThat(vnfInstanceBefore.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder("COMPLETED", "COMPLETED");
        assertThat(vnfInstanceBefore.getOssNodeProtocolFile()).isNull();

        Optional<HelmChart> enabledHelmChart = vnfInstanceBefore.getHelmCharts().stream().filter(chart -> chart.getId().equals("hc101jo98-07e7-41d5-9324-bbb717b7777")).findFirst();
        assertThat(enabledHelmChart.isPresent()).isTrue();
        assertThat(enabledHelmChart.get().isChartEnabled()).isTrue();
        assertThat(enabledHelmChart.get().getPriority()).isEqualTo(1);

        Optional<HelmChart> disabledHelmChart = vnfInstanceBefore.getHelmCharts().stream().filter(chart -> chart.getId().equals("hc100jo98-07e7-41d5-9324-bbb717b7777")).findFirst();
        assertThat(disabledHelmChart.isPresent()).isTrue();
        assertThat(disabledHelmChart.get().isChartEnabled()).isFalse();
        assertThat(disabledHelmChart.get().getPriority()).isEqualTo(0);

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(vnfInstanceBefore.getOperationOccurrenceId());
        assertThat(lifecycleOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        lifecycleOperation.setApplicationTimeout("80");
        lifecycleOperationRepository.save(lifecycleOperation);

        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(lifecycleOperationId);
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        healOperation.completed(message);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        LifecycleOperation operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(vnfInstance.getOperationOccurrenceId());
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);

        verify(workflowRoutingService, times(1)).routeInstantiateRequest(eq(1), any(), any(VnfInstance.class));
        verify(workflowRoutingService, times(0)).routeInstantiateRequest(eq(0), any(), any(VnfInstance.class));
    }

}