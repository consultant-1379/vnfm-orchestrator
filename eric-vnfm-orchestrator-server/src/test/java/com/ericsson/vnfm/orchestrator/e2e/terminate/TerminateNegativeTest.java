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
package com.ericsson.vnfm.orchestrator.e2e.terminate;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.assertThatNoEvnfmParamsPassedToWfs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
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


public final class TerminateNegativeTest extends AbstractEndToEndTest {

    @Test
    public void singleHelmChartTerminateFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 TERMINATE STARTING"
        // 6 TERMINATE PROCESSING
        // 7 TERMINATE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "single-chart-terminate-fails";
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

        LocalDateTime beforeOperation = LocalDateTime.now();
        // send terminate request
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // send terminate failed message
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId, HelmReleaseOperationType.TERMINATE, "1");
        failed.setMessage("terminate failed due to blah");

        testingMessageSender.sendMessage(failed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
        assertThat(failedOperation.getError()).contains("blah");
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("forceful");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("FAILED", firstHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartTerminateFailsFirstHelmChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 TERMINATE STARTING"
        // 6 TERMINATE PROCESSING
        // 7 TERMINATE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "multi-chart-terminate-one-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "df23d92e-3763-4608-b55c-8e2fd165b29e");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(instantiateCompleted);

        instantiateCompleted.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(instantiateCompleted);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LocalDateTime beforeOperation = LocalDateTime.now();
        // send terminate request
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // send terminate failed message
        HelmReleaseLifecycleMessage failedTerminate = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.FAILED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.TERMINATE,
                                                                                     "1");
        failedTerminate.setMessage("terminate failed due to blah");

        testingMessageSender.sendMessage(failedTerminate);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
        assertThat(failedOperation.getError()).contains("blah");
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("forceful");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                .containsOnly(tuple(firstHelmReleaseNameFor(releaseName), null),
                              tuple(secondHelmReleaseNameFor(releaseName), LifecycleOperationState.FAILED.toString()));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartTerminateFailsSecondHelmChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 TERMINATE STARTING"
        // 6 TERMINATE PROCESSING
        // 7 TERMINATE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "multi-chart-terminate-both-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "df23d92e-3763-4608-b55c-8e2fd165b29e");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(instantiateCompleted);

        instantiateCompleted.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(instantiateCompleted);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // verify helm version is 3.8 during instantiate
        final var instantiateRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/instantiate"), any(HttpMethod.class), instantiateRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        var instantiateRequests = instantiateRequestCaptor.getAllValues();
        EvnfmWorkFlowInstantiateRequest instantiateRequestForFirstChart = extractEvnfmWorkflowRequest(instantiateRequests.get(0),
                                                                                                      EvnfmWorkFlowInstantiateRequest.class);
        EvnfmWorkFlowInstantiateRequest instantiateRequestForSecondChart = extractEvnfmWorkflowRequest(instantiateRequests.get(1),
                                                                                                       EvnfmWorkFlowInstantiateRequest.class);
        assertThat(instantiateRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(instantiateRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.8");

        LocalDateTime beforeOperation = LocalDateTime.now();
        // verify values.yaml doesn't contain EVNFM_PARAMS
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        // send terminate request
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // send terminate completed and failed message
        HelmReleaseLifecycleMessage completedTerminate = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                        HelmReleaseState.COMPLETED,
                                                                                        lifeCycleOperationId,
                                                                                        HelmReleaseOperationType.TERMINATE,
                                                                                        "1");

        testingMessageSender.sendMessage(completedTerminate);

        HelmReleaseLifecycleMessage failedTerminate = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.FAILED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.TERMINATE,
                                                                                     "1");
        failedTerminate.setMessage("terminate failed due to blah-1");

        testingMessageSender.sendMessage(failedTerminate);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
        assertThat(failedOperation.getError()).contains("blah-1");
        assertThat(failedOperation.getOperationParams()).isNotNull().containsIgnoringCase("forceful");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                .containsOnly(tuple(firstHelmReleaseNameFor(releaseName), LifecycleOperationState.FAILED.toString()),
                              tuple(secondHelmReleaseNameFor(releaseName), LifecycleOperationState.COMPLETED.toString()));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());

        // verify helm version is 3.8 during terminate
        verify(restTemplate, times(2)).exchange(ArgumentMatchers.contains("&helmClientVersion=3.8"), eq(HttpMethod.POST), any(HttpEntity.class),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());
    }
}
