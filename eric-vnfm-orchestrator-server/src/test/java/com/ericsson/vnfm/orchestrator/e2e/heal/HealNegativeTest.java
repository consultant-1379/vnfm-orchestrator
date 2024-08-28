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
package com.ericsson.vnfm.orchestrator.e2e.heal;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.assertThatNoEvnfmParamsPassedToWfs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowInstantiateRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;


public class HealNegativeTest extends AbstractEndToEndTest {

    @Test
    public void healTerminateStageFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 HEAL STARTING
        // 6 HEAL PROCESSING
        // 7 HEAL FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "heal-fails-terminate";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName, "my-cluster");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LocalDateTime beforeOperation = LocalDateTime.now();
        // send heal request
        result = requestHelper.getMvcResultHealRequestAndVerifyAccepted(vnfInstanceResponse, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // send terminate failed message within heal operation
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId, HelmReleaseOperationType.TERMINATE, "1");
        failed.setMessage("Terminate failed due to some reasons");

        testingMessageSender.sendMessage(failed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(failedOperation.getError()).isNotNull();
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("Full restore");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void healInstantiateStageFailsCleanupResourcesFalse() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 HEAL STARTING
        // 6 HEAL PROCESSING
        // 7 HEAL FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "heal-fails-instantiate-cleanup-false";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // verify helm version is 3.8 during instantiate
        final var instantiateRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).exchange(endsWith("/instantiate"), any(HttpMethod.class), instantiateRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowInstantiateRequest instantiateRequestForFirstChart = extractEvnfmWorkflowRequest(instantiateRequestCaptor.getValue(),
                                                                                                      EvnfmWorkFlowInstantiateRequest.class);
        assertThat(instantiateRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");

        LocalDateTime beforeOperation = LocalDateTime.now();
        // send heal request
        result = requestHelper.getMvcResultHealRequestAndVerifyAccepted(vnfInstanceResponse, false);
        String healOperationId = getLifeCycleOperationId(result);

        // verify helm version is 3.8 during heal for terminate
        verify(restTemplate, times(1)).exchange(ArgumentMatchers.contains("&helmClientVersion=3.8"), eq(HttpMethod.POST), any(HttpEntity.class),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());

        // send terminate completed message within heal operation
        HelmReleaseLifecycleMessage terminateStageCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             healOperationId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             "1");

        testingMessageSender.sendMessage(terminateStageCompleted);

        await().until(awaitHelper.operationHasCombinedValuesFile(healOperationId));

        // verify state of operation
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(healOperationId);
        assertThat(lifecycleOperation.getCombinedAdditionalParams()).isNotNull();
        assertThat(lifecycleOperation.getCombinedValuesFile()).isNotNull();

        // verify helm version is 3.8 during heal for instantiate
        final var instantiateDuringHealRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/instantiate"), eq(HttpMethod.POST),
                                                instantiateDuringHealRequestCaptor
                                                        .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowInstantiateRequest instantiateDuringHealRequest =
                extractEvnfmWorkflowRequest(instantiateDuringHealRequestCaptor.getAllValues().get(1),
                                            EvnfmWorkFlowInstantiateRequest.class);

        assertThat(instantiateDuringHealRequest.getHelmClientVersion()).isEqualTo("3.8");

        // verify values.yaml doesn't contain EVNFM_PARAMS during heal for instantiate
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        // send instantiate failed message within heal operation
        HelmReleaseLifecycleMessage instantiateStageFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.FAILED,
                                                                                            healOperationId,
                                                                                            HelmReleaseOperationType.INSTANTIATE,
                                                                                            "1");
        instantiateStageFailed.setMessage("Instantiate failed due to some reasons");

        testingMessageSender.sendMessage(instantiateStageFailed);

        await().until(awaitHelper.operationReachesState(healOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(healOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(failedOperation.getError()).isNotNull();
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("Full restore");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void healInstantiateStageFailsCleanupResourcesTrue() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 HEAL STARTING
        // 6 HEAL PROCESSING
        // 7 HEAL FAILED
        // 8 CLEANUP STARTING
        // 9 CLEANUP PROCESSING
        // 10 CLEANUP COMPLETED
        // 11 IDENTIFIER DELETION
        int expectedCountOfCallsMessagingService = 11;

        // create identifier
        final String releaseName = "heal-fails-instantiate-cleanup-true";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // verify helm version is 3.8 during instantiate
        final var instantiateRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).exchange(endsWith("/instantiate"), any(HttpMethod.class), instantiateRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowInstantiateRequest instantiateRequestForFirstChart = extractEvnfmWorkflowRequest(instantiateRequestCaptor.getValue(),
                                                                                                      EvnfmWorkFlowInstantiateRequest.class);
        assertThat(instantiateRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");

        LocalDateTime beforeOperation = LocalDateTime.now();
        // send heal request
        result = requestHelper.getMvcResultHealRequestAndVerifyAccepted(vnfInstanceResponse, true);
        String healOperationId = getLifeCycleOperationId(result);

        // verify helm version is 3.8 during heal for terminate
        verify(restTemplate, times(1)).exchange(ArgumentMatchers.contains("&helmClientVersion=3.8"), eq(HttpMethod.POST), any(HttpEntity.class),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());

        // send terminate completed message within heal operation
        HelmReleaseLifecycleMessage terminateStageCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             healOperationId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             "1");

        testingMessageSender.sendMessage(terminateStageCompleted);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstanceResponse.getId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        final WorkflowServiceEventMessage deletePvcEvent = getWfsEventMessage(firstHelmReleaseNameFor(releaseName),
                                                                              WorkflowServiceEventStatus.COMPLETED,
                                                                              healOperationId,
                                                                              WorkflowServiceEventType.DELETE_PVC);
        messageHelper.sendMessageForChart(deletePvcEvent,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        final WorkflowServiceEventMessage deleteNamespaceEvent = getWfsEventMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    healOperationId,
                                                                                    WorkflowServiceEventType.DELETE_NAMESPACE);

        testingMessageSender.sendMessage(deleteNamespaceEvent);
        await().until(awaitHelper.operationHasCombinedValuesFile(healOperationId));

        // verify state of operation
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(healOperationId);
        assertThat(lifecycleOperation.getCombinedAdditionalParams()).isNotNull();
        assertThat(lifecycleOperation.getCombinedValuesFile()).isNotNull();

        // verify helm version is 3.8 during heal for instantiate
        final var instantiateDuringHealRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/instantiate"), eq(HttpMethod.POST),
                                                instantiateDuringHealRequestCaptor
                                                        .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        EvnfmWorkFlowInstantiateRequest instantiateDuringHealRequest =
                extractEvnfmWorkflowRequest(instantiateDuringHealRequestCaptor.getAllValues().get(1),
                                            EvnfmWorkFlowInstantiateRequest.class);

        assertThat(instantiateDuringHealRequest.getHelmClientVersion()).isEqualTo("3.8");

        // send instantiate failed message within heal operation
        HelmReleaseLifecycleMessage instantiateStageFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.FAILED,
                                                                                            healOperationId,
                                                                                            HelmReleaseOperationType.INSTANTIATE,
                                                                                            "1");
        instantiateStageFailed.setMessage("Instantiate failed due to some reasons");

        testingMessageSender.sendMessage(instantiateStageFailed);

        await().until(awaitHelper.operationReachesState(healOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(healOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.HEAL);
        assertThat(failedOperation.getError()).isNotNull();
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("Full restore");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        MvcResult cleanUpResult = requestHelper.getMvcResultCleanUpRequest(vnfInstanceResponse.getId());
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String cleanUpOperation = getLifeCycleOperationId(cleanUpResult);

        // send teardown failed messages
        HelmReleaseLifecycleMessage cleanUpMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED,
                                                                                    cleanUpOperation,
                                                                                    HelmReleaseOperationType.TERMINATE,
                                                                                    "1");

        testingMessageSender.sendMessage(cleanUpMessage);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstanceResponse.getId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        final WorkflowServiceEventMessage deletePvcEventDuringCleanup = getWfsEventMessage(firstHelmReleaseNameFor(releaseName),
                                                                                           WorkflowServiceEventStatus.COMPLETED,
                                                                                           cleanUpOperation,
                                                                                           WorkflowServiceEventType.DELETE_PVC);
        messageHelper.sendMessageForChart(deletePvcEventDuringCleanup,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        final WorkflowServiceEventMessage deleteNamespaceEventDuringCleanup = getWfsEventMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                 WorkflowServiceEventStatus.COMPLETED,
                                                                                                 cleanUpOperation,
                                                                                                 WorkflowServiceEventType.DELETE_NAMESPACE);

        testingMessageSender.sendMessage(deleteNamespaceEventDuringCleanup);
        await().until(awaitHelper.vnfInstanceIsDeleted(vnfInstance.getVnfInstanceId()));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }
}
