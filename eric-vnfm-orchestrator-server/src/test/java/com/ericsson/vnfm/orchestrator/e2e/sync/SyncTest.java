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
package com.ericsson.vnfm.orchestrator.e2e.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.CL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.JL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.PL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.TL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.core.type.TypeReference;


public class SyncTest extends AbstractEndToEndTest {

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Test
    public void successfulSync() throws Exception {
        // Create Identifier
        final String releaseName = "end-to-end-sync-success";
        final VnfInstanceResponse vnfInstanceResponse =
                requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);
        final String instanceId = vnfInstanceResponse.getId();

        // Assertions on state of instance
        final VnfInstance instanceAfterCreate = vnfInstanceRepository.findByVnfInstanceId(instanceId);
        assertThat(instanceAfterCreate.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(instanceAfterCreate.getHelmCharts()).hasSize(2);

        // Instantiate
        final Map<String, Object> extensions = Map.of(
                VNF_CONTROLLED_SCALING,
                Map.of(
                        PAYLOAD, CISM_CONTROLLED,
                        PAYLOAD_2, CISM_CONTROLLED,
                        PAYLOAD_3, CISM_CONTROLLED));
        final MvcResult instantiateResult = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(
                vnfInstanceResponse, releaseName, INST_LEVEL_3, extensions, false);
        final String instantiateLifeCycleOperationId = getLifeCycleOperationId(instantiateResult);

        // Fake completion message
        final HelmReleaseLifecycleMessage instantiationCompletedMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                instantiateLifeCycleOperationId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(instantiationCompletedMessage, instanceId, false, COMPLETED);

        // Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                instantiateLifeCycleOperationId,
                LifecycleOperationType.INSTANTIATE,
                InstantiationState.INSTANTIATED);

        // Sync
        final MvcResult syncResult = requestHelper.executeSyncVnfInstance(instanceId);
        assertThat(syncResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        final String syncLifeCycleOperationId = getLifeCycleOperationId(syncResult);
        awaitHelper.awaitOperationReachingState(syncLifeCycleOperationId, COMPLETED);

        // Assertions on state and instance after successful sync
        final VnfInstance instanceAfterSync = vnfInstanceRepository.findByVnfInstanceId(instanceId);

        assertThat(instanceAfterSync.getOperationOccurrenceId()).isEqualTo(syncLifeCycleOperationId);

        final Map<String, Map<String, ReplicaDetails>> replicaDetails =
                replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterSync.getHelmCharts());
        assertThat(replicaDetails)
                .hasEntrySatisfying(
                        "end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(resourceToDetails, CL_SCALED_VM, 3, 7, 7, true))
                .hasEntrySatisfying(
                        "end-to-end-sync-success-2",
                        resourceToDetails -> {
                            verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, null, null, 1, false);
                            verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 5, 5, true);
                            verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, 1, 6, 6, true);
                        });

        assertThat(instanceAfterSync.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 1),
                        tuple(PAYLOAD_2, 0),
                        tuple(PAYLOAD_3, 3));

        final Map<String, Map<String, String>> extensionsAfterSync = readExtensions(instanceAfterSync);
        assertThat(extensionsAfterSync).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, CISM_CONTROLLED),
                        entry(PAYLOAD_2, MANUAL_CONTROLLED),
                        entry(PAYLOAD_3, CISM_CONTROLLED)));

        Map<String, Integer> resourcesDetails = mapper.readValue(instanceAfterSync.getResourceDetails(),
                new TypeReference<HashMap<String, Integer>>() {
                });
        assertThat(resourcesDetails).containsOnly(
                entry(CL_SCALED_VM, 7),
                entry(TL_SCALED_VM, 1),
                entry(PL_SCALED_VM, 5),
                entry(JL_SCALED_VM, 6));

        // Terminate
        final MvcResult terminateResult = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(terminateResult.getResponse().getStatus()).isEqualTo(202);
        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                HelmReleaseState.PROCESSING,
                false));
        final String terminateLifeCycleOperationId = getLifeCycleOperationId(terminateResult);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetails =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetails)
                .isPresent()
                .hasValueSatisfying(details ->
                        assertThat(details.isDeletionInProgress()).isFalse());

        // Fake completion messages
        final HelmReleaseLifecycleMessage terminationCompletedMessage = new HelmReleaseLifecycleMessage();
        terminationCompletedMessage.setLifecycleOperationId(terminateLifeCycleOperationId);
        terminationCompletedMessage.setReleaseName(releaseName);
        terminationCompletedMessage.setOperationType(HelmReleaseOperationType.TERMINATE);
        terminationCompletedMessage.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(terminationCompletedMessage, instanceId, COMPLETED);

        // Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                terminateLifeCycleOperationId,
                LifecycleOperationType.TERMINATE,
                InstantiationState.NOT_INSTANTIATED);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetailsAfterTerminate =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetailsAfterTerminate).isEmpty();
    }

    @Test
    public void failedSync() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SYNC STARTING
        // 6 SYNC PROCESSING
        // 7 SYNC FAILED
        // 8 TERMINATE STARTING"
        // 9 TERMINATE PROCESSING
        // 10 TERMINATE COMPLETED
        int expectedCountOfCallsMessagingService = 10;

        // Create Identifier
        final String releaseName = "end-to-end-sync-failure";
        final VnfInstanceResponse vnfInstanceResponse =
                requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);
        final String instanceId = vnfInstanceResponse.getId();

        // Assertions on state of instance
        final VnfInstance instanceAfterCreate = vnfInstanceRepository.findByVnfInstanceId(instanceId);
        assertThat(instanceAfterCreate.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(instanceAfterCreate.getHelmCharts()).hasSize(2);

        // Instantiate
        final Map<String, Object> extensions = Map.of(
                VNF_CONTROLLED_SCALING,
                Map.of(
                        PAYLOAD, CISM_CONTROLLED,
                        PAYLOAD_2, CISM_CONTROLLED,
                        PAYLOAD_3, CISM_CONTROLLED));
        final MvcResult instantiateResult = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(
                vnfInstanceResponse, releaseName, INST_LEVEL_3, extensions, false);
        final String instantiateLifeCycleOperationId = getLifeCycleOperationId(instantiateResult);

        // Fake completion message
        final HelmReleaseLifecycleMessage instantiationCompletedMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                instantiateLifeCycleOperationId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(instantiationCompletedMessage, instanceId, false, COMPLETED);

        // Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                instantiateLifeCycleOperationId,
                LifecycleOperationType.INSTANTIATE,
                InstantiationState.INSTANTIATED);

        // Sync
        final MvcResult syncResult = requestHelper.executeSyncVnfInstance(instanceId);
        assertThat(syncResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        final String syncLifeCycleOperationId = getLifeCycleOperationId(syncResult);
        awaitHelper.awaitOperationReachingState(syncLifeCycleOperationId, FAILED);

        // Assertions on state and instance after successful sync
        final VnfInstance instanceAfterSync = vnfInstanceRepository.findByVnfInstanceId(instanceId);

        assertThat(instanceAfterSync.getOperationOccurrenceId()).isEqualTo(syncLifeCycleOperationId);

        final Map<String, Map<String, ReplicaDetails>> replicaDetails =
                replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterSync.getHelmCharts());
        assertThat(replicaDetails)
                .hasEntrySatisfying(
                        "end-to-end-sync-failure-1",
                        resourceToDetails -> verifyResourceReplicaDetails(resourceToDetails, CL_SCALED_VM, 3, 3, 3, true))
                .hasEntrySatisfying(
                        "end-to-end-sync-failure-2",
                        resourceToDetails -> {
                            verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, 1, 1, 1, true);
                            verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 1, 1, true);
                            verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, 1, 4, 4, true);
                        });

        assertThat(instanceAfterSync.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 0),
                        tuple(PAYLOAD_2, 0),
                        tuple(PAYLOAD_3, 2));

        final Map<String, Map<String, String>> extensionsAfterSync = readExtensions(instanceAfterSync);
        assertThat(extensionsAfterSync).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, CISM_CONTROLLED),
                        entry(PAYLOAD_2, CISM_CONTROLLED),
                        entry(PAYLOAD_3, CISM_CONTROLLED)));

        Map<String, Integer> resourcesDetails = mapper.readValue(instanceAfterSync.getResourceDetails(),
                new TypeReference<HashMap<String, Integer>>() {
                });
        assertThat(resourcesDetails).containsOnly(
                entry(CL_SCALED_VM, 3),
                entry(TL_SCALED_VM, 1),
                entry(PL_SCALED_VM, 1),
                entry(JL_SCALED_VM, 1));

        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(syncLifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(FAILED);
        assertThat(operation.getError()).contains(
                "Autoscaling for PL__scaled_vm, CL_scaled_vm should be the same.",
                "Scale level for TL_scaled_vm is 6 which exceeds max scale level 5.",
                "For target JL_scaled_vm replica count 3 does not belong to any scaling level. Closest scaling level: 1, closets replica count: 2");

        // Terminate
        final MvcResult terminateResult = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(terminateResult.getResponse().getStatus()).isEqualTo(202);
        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                HelmReleaseState.PROCESSING,
                false));
        final String terminateLifeCycleOperationId = getLifeCycleOperationId(terminateResult);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetails =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetails)
                .isPresent()
                .hasValueSatisfying(details ->
                        assertThat(details.isDeletionInProgress()).isFalse());

        // Fake completion messages
        final HelmReleaseLifecycleMessage terminationCompletedMessage = new HelmReleaseLifecycleMessage();
        terminationCompletedMessage.setLifecycleOperationId(terminateLifeCycleOperationId);
        terminationCompletedMessage.setReleaseName(releaseName);
        terminationCompletedMessage.setOperationType(HelmReleaseOperationType.TERMINATE);
        terminationCompletedMessage.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(terminationCompletedMessage, instanceId, COMPLETED);

        // Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                terminateLifeCycleOperationId,
                LifecycleOperationType.TERMINATE,
                InstantiationState.NOT_INSTANTIATED);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetailsAfterTerminate =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetailsAfterTerminate).isEmpty();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void successfulScaleSyncUpgradeDowngrade() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING
        // 6 SCALE PROCESSING
        // 7 SCALE COMPLETED
        // 8 SYNC STARTING
        // 9 SYNC PROCESSING
        // 10 SYNC COMPLETED
        // 11 CHANGE_VNFPKG STARTING
        // 12 CHANGE_VNFPKG PROCESSING
        // 13 CHANGE_VNFPKG COMPLETED
        // 14 CHANGE_VNFPKG STARTING
        // 15 CHANGE_VNFPKG PROCESSING
        // 16 CHANGE_VNFPKG COMPLETED
        int expectedCountOfCallsMessagingService = 16;

        // Create Identifier
        final String releaseName = "end-to-end-sync-success";
        final VnfInstanceResponse createInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        // Assertions on state of instance
        final VnfInstance instanceAfterCreate = vnfInstanceRepository.findByVnfInstanceId(createInstanceResponse.getId());
        assertThat(instanceAfterCreate.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // Instantiate with non-default level and extensions
        final Map<String, Object> instantiateExtensions = Map.of(
                VNF_CONTROLLED_SCALING,
                Map.of(PAYLOAD, CISM_CONTROLLED,
                        PAYLOAD_2, CISM_CONTROLLED,
                        PAYLOAD_3, MANUAL_CONTROLLED));
        final MvcResult instantiateResult = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(
                createInstanceResponse, releaseName, INST_LEVEL_2, instantiateExtensions, false);
        final String instantiateLifeCycleOperationId = getLifeCycleOperationId(instantiateResult);

        // Fake completion message
        final HelmReleaseLifecycleMessage instantiationCompleted = getHelmReleaseLifecycleMessage(
                releaseName, HelmReleaseState.COMPLETED, instantiateLifeCycleOperationId, HelmReleaseOperationType.INSTANTIATE, "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(instantiationCompleted, createInstanceResponse.getId(), false, COMPLETED);

        // Assertions on state of instance after successful instantiate
        final VnfInstance instanceAfterInstantiate = verificationHelper.verifyOperationAndModel(createInstanceResponse,
                instantiateLifeCycleOperationId,
                LifecycleOperationType.INSTANTIATE,
                InstantiationState.INSTANTIATED);

        // Check that instantiationLevel is correctly set on the instance
        assertThat(instanceAfterInstantiate.getInstantiationLevel()).isEqualTo(INST_LEVEL_2);
        final Map<String, Map<String, String>> extensionsAfterInstantiate = readExtensions(instanceAfterInstantiate);
        assertThat(extensionsAfterInstantiate).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, CISM_CONTROLLED),
                        entry(PAYLOAD_2, CISM_CONTROLLED),
                        entry(PAYLOAD_3, MANUAL_CONTROLLED)));

        // Check ReplicaDetails of specific charts
        assertThat(replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterInstantiate.getHelmCharts()))
                .hasEntrySatisfying("end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(
                                resourceToDetails, CL_SCALED_VM, 3, 3, 3, true))
                .hasEntrySatisfying("end-to-end-sync-success-2", resourceToDetails -> {
                    verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, 1, 4, 4, true);
                    verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 1, 1, true);
                    verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, null, null, 1, false);
                });

        // Check Scale Info Correct after Instantiate
        assertThat(instanceAfterInstantiate.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 0),
                        tuple(PAYLOAD_2, 3),
                        tuple(PAYLOAD_3, 0));

        // Scale Out
        final MvcResult scaleResult = requestHelper.getMvcResultScaleVnfRequest(createInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        final String scaleLifeCycleOperationId = getLifeCycleOperationId(scaleResult);

        // Fake completion message
        final HelmReleaseLifecycleMessage scaleCompleted = getHelmReleaseLifecycleMessage(
                releaseName, HelmReleaseState.COMPLETED, scaleLifeCycleOperationId, HelmReleaseOperationType.SCALE, "2");
        messageHelper.sendMessageForChart(scaleCompleted,
                secondHelmReleaseNameFor(releaseName),
                createInstanceResponse.getId(),
                false,
                HelmReleaseState.COMPLETED);

        await().until(awaitHelper.operationReachesState(scaleLifeCycleOperationId, COMPLETED));

        final VnfInstance instanceAfterScale = vnfInstanceRepository.findByVnfInstanceId(createInstanceResponse.getId());

        // Check that scale level is only increased for Payload_2
        assertThat(instanceAfterScale.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 0),
                        tuple(PAYLOAD_2, 4),
                        tuple(PAYLOAD_3, 0));

        // Check ReplicaDetails of specific charts
        assertThat(replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterScale.getHelmCharts()))
                .hasEntrySatisfying("end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(
                                resourceToDetails, CL_SCALED_VM, 3, 3, 3, true))
                .hasEntrySatisfying("end-to-end-sync-success-2", resourceToDetails -> {
                    verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, 1, 5, 5, true);
                    verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 1, 1, true);
                    verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, null, null, 1, false);
                });

        // Sync
        final MvcResult syncResult = requestHelper.executeSyncVnfInstance(createInstanceResponse.getId());
        final String syncLifeCycleOperationId = getLifeCycleOperationId(syncResult);
        awaitHelper.awaitOperationReachingState(syncLifeCycleOperationId, COMPLETED);

        final VnfInstance instanceAfterSync = vnfInstanceRepository.findByVnfInstanceId(createInstanceResponse.getId());

        // Check extensions
        final Map<String, Map<String, String>> extensionsAfterSync = readExtensions(instanceAfterSync);
        assertThat(extensionsAfterSync).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, CISM_CONTROLLED),
                        entry(PAYLOAD_2, MANUAL_CONTROLLED),
                        entry(PAYLOAD_3, CISM_CONTROLLED)));

        // Check ReplicaDetails of specific charts
        assertThat(replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterSync.getHelmCharts()))
                .hasEntrySatisfying("end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(
                                resourceToDetails, CL_SCALED_VM, 3, 7, 7, true))
                .hasEntrySatisfying("end-to-end-sync-success-2", resourceToDetails -> {
                    verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, null, null, 1, false);
                    verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 5, 5, true);
                    verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, 1, 6, 6, true);
                });

        // Check scaling levels
        assertThat(instanceAfterSync.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 1),
                        tuple(PAYLOAD_2, 0),
                        tuple(PAYLOAD_3, 3));

        // Change Pkg Request and set Payload to Manual, Payload_2 to CISM
        final Map<String, Object> upgradeExtensions = Map.of(
                VNF_CONTROLLED_SCALING,
                Map.of(PAYLOAD, MANUAL_CONTROLLED,
                        PAYLOAD_2, CISM_CONTROLLED,
                        PAYLOAD_3, CISM_CONTROLLED));
        final MvcResult upgradeResult = requestHelper.getMvcResultChangeVnfpkgRequestWithExtensionsAndVerifyAccepted(
                createInstanceResponse, E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU, upgradeExtensions);
        final String upgradeLifeCycleOperationId = getLifeCycleOperationId(upgradeResult);

        // Fake completion messages
        final HelmReleaseLifecycleMessage upgradeCompleted = getHelmReleaseLifecycleMessage(
                releaseName, HelmReleaseState.COMPLETED, upgradeLifeCycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "4");
        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeCompleted, createInstanceResponse.getId(), true, COMPLETED);

        // Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(createInstanceResponse,
                upgradeLifeCycleOperationId,
                LifecycleOperationType.CHANGE_VNFPKG,
                InstantiationState.INSTANTIATED);

        final VnfInstance instanceAfterUpgrade = vnfInstanceRepository.findByVnfInstanceId(createInstanceResponse.getId());

        // Check extensions set on the instance correctly
        final Map<String, Map<String, String>> extensionsAfterUpgrade = readExtensions(instanceAfterUpgrade);
        assertThat(extensionsAfterUpgrade).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, MANUAL_CONTROLLED),
                        entry(PAYLOAD_2, CISM_CONTROLLED),
                        entry(PAYLOAD_3, CISM_CONTROLLED)));

        // Check level has been maintained
        assertThat(instanceAfterUpgrade.getInstantiationLevel()).isEqualTo(INST_LEVEL_2);

        // Check scale info as expected
        assertThat(instanceAfterUpgrade.getScaleInfoEntity())
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple(PAYLOAD, 0),
                        tuple(PAYLOAD_2, 3),
                        tuple(PAYLOAD_3, 0));

        // Check ReplicaDetails of specific charts
        assertThat(replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterUpgrade.getHelmCharts()))
                .hasEntrySatisfying(
                        "end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(resourceToDetails, CL_SCALED_VM, null, null, 3, false))
                .hasEntrySatisfying(
                        "end-to-end-sync-success-2",
                        resourceToDetails -> {
                            verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, 1, 1, 1, true);
                            verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, null, null, 1, false);
                            verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, 1, 4, 4, true);
                        });

        // Downgrade
        final MvcResult downgradeResult = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(createInstanceResponse,
                                                                                                         E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID, false);
        final String downgradeLifeCycleOperationId = getLifeCycleOperationId(downgradeResult);

        final LifecycleOperation downgradeLifecycleOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(downgradeLifeCycleOperationId);
        assertThat(downgradeLifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        final String rollbackPattern = downgradeLifecycleOperation.getRollbackPattern();
        final String[] split = rollbackPattern.split(",");
        assertThat(split).hasSize(2);

        final VnfInstance tempInstanceAfterRollback = parseJson(downgradeLifecycleOperation.getVnfInstance().getTempInstance(), VnfInstance.class);
        assertThat(tempInstanceAfterRollback.getHelmCharts()).hasSize(2);

        // Second chart first stage : Rollback
        final HelmReleaseLifecycleMessage rollback2Completed = new HelmReleaseLifecycleMessage();
        rollback2Completed.setLifecycleOperationId(downgradeLifeCycleOperationId);
        rollback2Completed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        rollback2Completed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollback2Completed.setState(HelmReleaseState.COMPLETED);
        rollback2Completed.setRevisionNumber("2");
        rollback2Completed.setMessage("Rollback completed");
        testingMessageSender.sendMessage(rollback2Completed);

        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                createInstanceResponse.getId(),
                HelmReleaseState.COMPLETED,
                true));

        // Check still rolling back
        final LifecycleOperation lifecycleOperationAfterFirstStage =
                lifecycleOperationRepository.findByOperationOccurrenceId(downgradeLifeCycleOperationId);
        assertThat(lifecycleOperationAfterFirstStage.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);

        // First chart, first stage : Rollback
        final HelmReleaseLifecycleMessage rollback1Completed = new HelmReleaseLifecycleMessage();
        rollback1Completed.setLifecycleOperationId(downgradeLifeCycleOperationId);
        rollback1Completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollback1Completed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollback1Completed.setState(HelmReleaseState.COMPLETED);
        rollback1Completed.setRevisionNumber("2");
        rollback1Completed.setMessage("Rollback completed");
        testingMessageSender.sendMessage(rollback1Completed);
        await().until(awaitHelper.operationReachesState(downgradeLifeCycleOperationId, COMPLETED));

        final Optional<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsRepository.findById(downgradeLifeCycleOperationId);

        // Assertion that it is a DOWNGRADE operation
        assertThat(changePackageOperationDetails).isPresent()
                .hasValueSatisfying(details -> assertThat(details.getChangePackageOperationSubtype())
                        .isEqualTo(ChangePackageOperationSubtype.DOWNGRADE));

        // Assertions on state of the operation and instance
        final VnfInstance instanceAfterDowngrade = verificationHelper.verifyOperationAndModel(createInstanceResponse,
                downgradeLifeCycleOperationId,
                LifecycleOperationType.CHANGE_VNFPKG,
                InstantiationState.INSTANTIATED);

        // Check replica details rolled back to synced state
        assertThat(replicaDetailsMapper.getReplicaDetailsForAllCharts(instanceAfterDowngrade.getHelmCharts()))
                .hasEntrySatisfying("end-to-end-sync-success-1",
                        resourceToDetails -> verifyResourceReplicaDetails(
                                resourceToDetails, CL_SCALED_VM, 3, 7, 7, true))
                .hasEntrySatisfying("end-to-end-sync-success-2", resourceToDetails -> {
                    verifyResourceReplicaDetails(resourceToDetails, TL_SCALED_VM, null, null, 1, false);
                    verifyResourceReplicaDetails(resourceToDetails, PL_SCALED_VM, 1, 5, 5, true);
                    verifyResourceReplicaDetails(resourceToDetails, JL_SCALED_VM, 1, 6, 6, true);
                });

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    @Disabled
    public void wfsCallReturnsInternalServerErrorOnSync() throws Exception {
        // Create Identifier
        final String releaseName = "end-to-end-sync-wfs-error";
        final VnfInstanceResponse vnfInstanceResponse =
                requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);
        final String instanceId = vnfInstanceResponse.getId();

        // Assertions on state of instance
        final VnfInstance instanceAfterCreate = vnfInstanceRepository.findByVnfInstanceId(instanceId);
        assertThat(instanceAfterCreate.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(instanceAfterCreate.getHelmCharts()).hasSize(2);

        // Instantiate
        final Map<String, Object> extensions = Map.of(
                VNF_CONTROLLED_SCALING,
                Map.of(
                        PAYLOAD, CISM_CONTROLLED,
                        PAYLOAD_2, CISM_CONTROLLED,
                        PAYLOAD_3, CISM_CONTROLLED));
        final MvcResult instantiateResult = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        final String instantiateLifeCycleOperationId = getLifeCycleOperationId(instantiateResult);

        // Fake completion message
        final HelmReleaseLifecycleMessage instantiationCompletedMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                instantiateLifeCycleOperationId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(instantiationCompletedMessage, instanceId, false, COMPLETED);

        // Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                instantiateLifeCycleOperationId,
                LifecycleOperationType.INSTANTIATE,
                InstantiationState.INSTANTIATED);

        // Sync
        final MvcResult syncResult = requestHelper.executeSyncVnfInstance(instanceId);
        assertThat(syncResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        final String syncLifeCycleOperationId = getLifeCycleOperationId(syncResult);
        awaitHelper.awaitOperationReachingState(syncLifeCycleOperationId, FAILED);

        // Assertions on state and instance after successful sync
        final VnfInstance instanceAfterSync = vnfInstanceRepository.findByVnfInstanceId(instanceId);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(syncLifeCycleOperationId);
        assertThat(operation.getError()).contains("Error during processing a command: ");
        assertThat(instanceAfterSync.getOperationOccurrenceId()).isEqualTo(syncLifeCycleOperationId);

        // Terminate
        final MvcResult terminateResult = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(terminateResult.getResponse().getStatus()).isEqualTo(202);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                HelmReleaseState.PROCESSING,
                false));
        final String terminateLifeCycleOperationId = getLifeCycleOperationId(terminateResult);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetails =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetails)
                .isPresent()
                .hasValueSatisfying(details ->
                        assertThat(details.isDeletionInProgress()).isFalse());

        // Fake completion messages
        final HelmReleaseLifecycleMessage terminationCompletedMessage = new HelmReleaseLifecycleMessage();
        terminationCompletedMessage.setLifecycleOperationId(terminateLifeCycleOperationId);
        terminationCompletedMessage.setReleaseName(releaseName);
        terminationCompletedMessage.setOperationType(HelmReleaseOperationType.TERMINATE);
        terminationCompletedMessage.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(terminationCompletedMessage, instanceId, COMPLETED);

        // Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(
                vnfInstanceResponse,
                terminateLifeCycleOperationId,
                LifecycleOperationType.TERMINATE,
                InstantiationState.NOT_INSTANTIATED);

        final Optional<VnfInstanceNamespaceDetails> namespaceDetailsAfterTerminate =
                vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceAfterCreate.getVnfInstanceId());
        assertThat(namespaceDetailsAfterTerminate).isEmpty();
    }

    private static void verifyResourceReplicaDetails(
            Map<String, ReplicaDetails> resourceToDetails, String resource, Integer minReplicaCount,
            Integer maxReplicaCount, Integer currentReplicaCount, boolean autoScalingEnabled) {

        assertThat(resourceToDetails)
                .hasEntrySatisfying(
                        resource,
                        details -> {
                            assertThat(details.getMinReplicasCount()).isEqualTo(minReplicaCount);
                            assertThat(details.getMaxReplicasCount()).isEqualTo(maxReplicaCount);
                            assertThat(details.getAutoScalingEnabledValue()).isEqualTo(autoScalingEnabled);
                            assertThat(details.getCurrentReplicaCount()).isEqualTo(currentReplicaCount);
                        });
    }

    private static Map<String, Map<String, String>> readExtensions(VnfInstance instance) {
        return parseJsonToGenericType(
                instance.getVnfInfoModifiableAttributesExtensions(),
                new TypeReference<>() {
                });
    }
}