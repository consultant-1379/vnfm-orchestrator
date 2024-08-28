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
package com.ericsson.vnfm.orchestrator.e2e.scale;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.CL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.JL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.PL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.TL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowInstantiateRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowRollbackRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkflowScaleRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class ScaleNegativeTest extends AbstractEndToEndTest {

    @Test
    public void singleHelmChartScaleFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING
        // 6 SCALE PROCESSING
        // 7 SCALE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "single-chart-scale-fails-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        // scale
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED,
                                                                                 lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE,
                                                                                 "2");
        scaleFailed.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        String firstLifeCycleOperationId = lifeCycleOperationId;

        // send failed message for rollback
        HelmReleaseLifecycleMessage rollback = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                              HelmReleaseState.FAILED,
                                                                              lifeCycleOperationId,
                                                                              HelmReleaseOperationType.ROLLBACK,
                                                                              "3");
        rollback.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollback);

        await().timeout(10, TimeUnit.MINUTES).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(firstLifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);
        assertThat(failedOperation.getError()).contains("scale failed", "rollback failed");

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("FAILED", firstHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartScaleFailsRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING
        // 6 SCALE PROCESSING
        // 7 SCALE ROLLED_BACK
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "single-chart-scale-fails-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        // scale
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED,
                                                                                 lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE,
                                                                                 "2");
        scaleFailed.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        // send success message for rollback
        HelmReleaseLifecycleMessage rollback = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                              HelmReleaseState.COMPLETED,
                                                                              lifeCycleOperationId,
                                                                              HelmReleaseOperationType.ROLLBACK,
                                                                              "3");
        rollback.setMessage("rollback success");
        testingMessageSender.sendMessage(rollback);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);
        assertThat(failedOperation.getError()).contains("scale failed");

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("ROLLED_BACK", firstHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartScaleFirstChartFailsRollbackSuccess() throws Exception {
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
        final String releaseName = "multi-chart-scale-one-fails-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");

        testingMessageSender.sendMessage(completed);

        completed.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        // scale
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED, lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE, "2");
        scaleFailed.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        // send success message for rollback
        HelmReleaseLifecycleMessage rollback = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                              HelmReleaseState.COMPLETED,
                                                                              lifeCycleOperationId,
                                                                              HelmReleaseOperationType.ROLLBACK, "3");
        rollback.setMessage("rollback success");
        testingMessageSender.sendMessage(rollback);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);
        assertThat(failedOperation.getError()).contains("scale failed");

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("ROLLED_BACK", firstHelmReleaseNameFor(releaseName)), tuple(
                null, secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartScaleFirstChartFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING
        // 6 SCALE PROCESSING
        // 7 SCALE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "multi-chart-scale-one-fails-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(completed);

        completed.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        // scale
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED, lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE, "2");
        scaleFailed.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        // send failed message for rollback
        HelmReleaseLifecycleMessage rollback = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                              HelmReleaseState.FAILED,
                                                                              lifeCycleOperationId,
                                                                              HelmReleaseOperationType.ROLLBACK, "3");
        rollback.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollback);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getStartTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);
        assertThat(failedOperation.getError()).contains("scale failed", "rollback failed");

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("FAILED", firstHelmReleaseNameFor(releaseName)), tuple(
                null, secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartScaleSecondChartFailsRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING
        // 6 SCALE PROCESSING
        // 7 SCALE ROLLING_BACK
        // 8 SCALE ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        // create identifier
        final String releaseName = "multi-chart-scale-two-fails-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(completed);

        completed.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(completed);

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

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        // scale first chart
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        String firstChartLifeCycleOperationId = lifeCycleOperationId;

        HelmReleaseLifecycleMessage scaleCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.SCALE, "2");
        scaleCompleted.setMessage("scale completed");
        testingMessageSender.sendMessage(scaleCompleted);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstance.getVnfInstanceId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        // scale second chart
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED, lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE, "3");
        scaleFailed.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 1, true));
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 2, true));
        testingMessageSender.sendMessage(scaleFailed);

        // verify helm version is 3.8 during scale
        final var scaleRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(4)).exchange(endsWith("/scale"), any(HttpMethod.class), scaleRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        var scaleRequests = scaleRequestCaptor.getAllValues();
        EvnfmWorkflowScaleRequest scaleRequestForFirstChart = extractEvnfmWorkflowRequest(scaleRequests.get(0),
                                                                                          EvnfmWorkflowScaleRequest.class);
        EvnfmWorkflowScaleRequest scaleRequestForSecondChart = extractEvnfmWorkflowRequest(scaleRequests.get(1),
                                                                                           EvnfmWorkflowScaleRequest.class);
        EvnfmWorkflowScaleRequest scaleRequestForSecondChartRetryOne = extractEvnfmWorkflowRequest(scaleRequests.get(2),
                                                                                                   EvnfmWorkflowScaleRequest.class);
        EvnfmWorkflowScaleRequest scaleRequestForSecondChartRetryTwo = extractEvnfmWorkflowRequest(scaleRequests.get(3),
                                                                                                   EvnfmWorkflowScaleRequest.class);
        assertThat(scaleRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(scaleRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(scaleRequestForSecondChartRetryOne.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(scaleRequestForSecondChartRetryTwo.getHelmClientVersion()).isEqualTo("3.8");

        // send success message for rollback of second chart
        HelmReleaseLifecycleMessage rollbackSecondChart = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                         HelmReleaseState.COMPLETED,
                                                                                         lifeCycleOperationId,
                                                                                         HelmReleaseOperationType.ROLLBACK, "4");
        rollbackSecondChart.setMessage("rollback success");
        testingMessageSender.sendMessage(rollbackSecondChart);

        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                                                        vnfInstance.getVnfInstanceId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        // send success message for rollback of first chart
        HelmReleaseLifecycleMessage rollbackFirstChart = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                        HelmReleaseState.COMPLETED,
                                                                                        firstChartLifeCycleOperationId,
                                                                                        HelmReleaseOperationType.ROLLBACK, "5");
        rollbackFirstChart.setMessage("rollback success");
        testingMessageSender.sendMessage(rollbackFirstChart);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // verify helm version is 3.8 during rollback for scale
        final var rollbackRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/rollback"), eq(HttpMethod.POST), rollbackRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowRollbackRequest rollbackRequestForSecondChart = extractEvnfmWorkflowRequest(rollbackRequestCaptor.getAllValues().get(0),
                                                                                                 EvnfmWorkFlowRollbackRequest.class);
        EvnfmWorkFlowRollbackRequest rollbackRequestForFirstChart = extractEvnfmWorkflowRequest(rollbackRequestCaptor.getAllValues().get(1),
                                                                                                EvnfmWorkFlowRollbackRequest.class);

        assertThat(rollbackRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(rollbackRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("ROLLED_BACK", firstHelmReleaseNameFor(releaseName)), tuple(
                "ROLLED_BACK", secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartScaleSecondChartFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 TERMINATE STARTING"
        // 6 TERMINATE PROCESSING
        // 7 SCALE ROLLING_BACK
        // 8 TERMINATE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "multi-chart-scale-two-fails-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart");

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");

        testingMessageSender.sendMessage(completed);

        completed.setReleaseName(secondHelmReleaseNameFor(releaseName));

        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        // scale first chart
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        String firstChartLifeCycleOperationId = lifeCycleOperationId;

        HelmReleaseLifecycleMessage scaleCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.SCALE, "2");
        scaleCompleted.setMessage("scale completed");
        testingMessageSender.sendMessage(scaleCompleted);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstance.getVnfInstanceId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        // scale second chart
        LocalDateTime beforeOperation = LocalDateTime.now();
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED, lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE, "3");
        scaleCompleted.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        // send failure message for rollback of second chart
        HelmReleaseLifecycleMessage rollbackSecondChart = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                         HelmReleaseState.FAILED,
                                                                                         lifeCycleOperationId,
                                                                                         HelmReleaseOperationType.ROLLBACK, "4");
        rollbackSecondChart.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackSecondChart);

        // send success message for rollback of first chart
        HelmReleaseLifecycleMessage rollbackFirstChart = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                        HelmReleaseState.COMPLETED,
                                                                                        firstChartLifeCycleOperationId,
                                                                                        HelmReleaseOperationType.ROLLBACK, "5");
        rollbackFirstChart.setMessage("rollback success");
        testingMessageSender.sendMessage(rollbackFirstChart);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName)), tuple(
                "FAILED", secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmScaleWithDefaultLevelsExtensionsSetSecondChartFailsRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 SCALE STARTING"
        // 6 SCALE PROCESSING
        // 7 SCALE ROLLING_BACK // temporary missed
        // 8 SCALE ROLLED_BACK
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "levels-extensions-scale-fails-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiation request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed messages
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");
        testingMessageSender.sendMessage(completed);

        completed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();

        //check scale levels as expected after instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails1 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);

        ReplicaDetails clScaledVm = replicaDetails1.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(19);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isNull();
        assertThat(clScaledVm.getMinReplicasCount()).isNull();

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(3);

        // Check replicaDetails increased as expected
        ReplicaDetails plScaledVm = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(17);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isNull();
        assertThat(plScaledVm.getMinReplicasCount()).isNull();

        ReplicaDetails tlScaledVm = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails jlScaledVm = replicaDetails2.get(JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(jlScaledVm.getMinReplicasCount()).isNull();

        // Scale out
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        String firstChartLifeCycleOperationId = lifeCycleOperationId;

        HelmReleaseLifecycleMessage scaleCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.SCALE, "2");
        scaleCompleted.setMessage("scale completed");
        testingMessageSender.sendMessage(scaleCompleted);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstance.getVnfInstanceId(),
                                                        HelmReleaseState.COMPLETED,
                                                        false));

        // scale second chart
        LocalDateTime beforeOperation = LocalDateTime.now();
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED, lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE, "3");
        scaleCompleted.setMessage("scale failed");
        testingMessageSender.sendMessage(scaleFailed);

        // send success message for rollback of second chart
        HelmReleaseLifecycleMessage rollbackSecondChart = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                         HelmReleaseState.COMPLETED,
                                                                                         lifeCycleOperationId,
                                                                                         HelmReleaseOperationType.ROLLBACK, "4");
        rollbackSecondChart.setMessage("rollback success");
        testingMessageSender.sendMessage(rollbackSecondChart);

        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                                                        vnfInstance.getVnfInstanceId(),
                                                        HelmReleaseState.ROLLED_BACK,
                                                        false));

        // send success message for rollback of first chart
        HelmReleaseLifecycleMessage rollbackFirstChart = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                        HelmReleaseState.COMPLETED,
                                                                                        firstChartLifeCycleOperationId,
                                                                                        HelmReleaseOperationType.ROLLBACK, "5");
        rollbackFirstChart.setMessage("rollback success");
        testingMessageSender.sendMessage(rollbackFirstChart);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // verify state of the operation
        LifecycleOperation failedOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);

        // verify state of instance
        vnfInstance = getAndVerifyVnfInstanceFromFailedOperation(releaseName, failedOperation);
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_1);
        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions()).isNotNull();

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName)), tuple(
                "ROLLED_BACK", secondHelmReleaseNameFor(releaseName)));

        //check scale level after failed scale is same as prior to the operation
        List<ScaleInfoEntity> scaleInfoEntitiesAfterFailure = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterFailure) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //check that replica details of all charts haven't been changed
        List<HelmChart> helmChartsAfterFail = vnfInstance.getHelmCharts();
        HelmChart packageOneAfterFail = getHelmChartByName(helmChartsAfterFail, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails1AfterFail = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOneAfterFail);
        assertThat(replicaDetails1AfterFail).isEqualTo(replicaDetails1);

        HelmChart packageTwoAfterFail = getHelmChartByName(helmChartsAfterFail, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2AfterFail = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwoAfterFail);
        assertThat(replicaDetails2AfterFail).isEqualTo(replicaDetails2);

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    private VnfInstance getAndVerifyVnfInstanceFromFailedOperation(final String releaseName, final LifecycleOperation failedOperation) {
        final VnfInstance vnfInstance;
        vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags")
                .containsIgnoringCase("pm");
        return vnfInstance;
    }
}
