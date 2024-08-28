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
package com.ericsson.vnfm.orchestrator.e2e.modifypkg;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.assertThatNoEvnfmParamsPassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.ROLLED_BACK;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.ROLLING_BACK;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowInstantiateRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowRollbackRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowUpgradeRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class ModifyPackageNegativeTest extends AbstractEndToEndTest {

    @Test
    public void multiHelmChartSecondModifyFailsRollbackSucceeds() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 MODIFY_INFO STARTING"
        // 6 MODIFY_INFO PROCESSING
        // 7 MODIFY_INFO ROLLING_BACK
        // 8 MODIFY_INFO ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "end-to-end-modify-rollback-success";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        Map<String, Object> extensions = new HashMap<>();
        Map<String, String> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            releaseName,
                                                                                            INST_LEVEL_3,
                                                                                            extensions,
                                                                                            false);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // modify request
        extensions = new HashMap<>();
        vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           extensions,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // modify succeed message first chart
        HelmReleaseLifecycleMessage upgradeSucceed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                    String.valueOf(revisionNumber));

        testingMessageSender.sendMessage(upgradeSucceed);

        // modify failed message second chart
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                   HelmReleaseState.FAILED,
                                                                                   lifeCycleOperationId,
                                                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                   String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");
        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        // assertions on state
        LifecycleOperation afterUpgradeFailed =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterUpgradeFailed.getError()).contains("upgrade failed");
        assertThat(afterUpgradeFailed.getStateEnteredTime()).isAfter(priorToFailedUpgrade.getStateEnteredTime());
        assertThat(afterUpgradeFailed.getStartTime()).isEqualTo(priorToFailedUpgrade.getStartTime());

        VnfInstance instanceAfterUpgradeFailed = afterUpgradeFailed.getVnfInstance();
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(2);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state")
                .contains(PROCESSING.toString(), ROLLING_BACK.toString());

        // rollback success message
        HelmReleaseLifecycleMessage rollbackSuccess = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.COMPLETED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK,
                                                                                     String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(rollbackSuccess);

        rollbackSuccess.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(rollbackSuccess);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state")
                .containsExactly(ROLLED_BACK.toString(), ROLLED_BACK.toString());

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondModifyFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 MODIFY_INFO STARTING"
        // 6 MODIFY_INFO PROCESSING
        // 7 MODIFY_INFO ROLLING_BACK
        // 8 MODIFY_INFO FAILED
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        ///Create Identifier
        final String releaseName = "end-to-end-modify-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

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

        // modify request
        Map<String, Object> extensions = new HashMap<>();
        Map<String, String> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           extensions,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade succeed message
        HelmReleaseLifecycleMessage upgradeSucceed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                    String.valueOf(revisionNumber));

        testingMessageSender.sendMessage(upgradeSucceed);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                   HelmReleaseState.FAILED,
                                                                                   lifeCycleOperationId,
                                                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                   String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");
        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        // assertions on state
        LifecycleOperation afterUpgradeFailed =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterUpgradeFailed.getError()).contains("upgrade failed");
        assertThat(afterUpgradeFailed.getStateEnteredTime()).isAfter(priorToFailedUpgrade.getStateEnteredTime());
        assertThat(afterUpgradeFailed.getStartTime()).isEqualTo(priorToFailedUpgrade.getStartTime());

        VnfInstance instanceAfterUpgradeFailed = afterUpgradeFailed.getVnfInstance();
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(2);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder(PROCESSING.toString(), ROLLING_BACK.toString());

        // verify helm version is 3.8 for upgrade (modify)
        final var upgradeRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/upgrade"), any(HttpMethod.class), upgradeRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        var upgradeRequests = upgradeRequestCaptor.getAllValues();
        EvnfmWorkFlowUpgradeRequest upgradeRequestForFirstChart = extractEvnfmWorkflowRequest(upgradeRequests.get(0),
                                                                                              EvnfmWorkFlowUpgradeRequest.class);
        EvnfmWorkFlowUpgradeRequest upgradeRequestForSecondChart = extractEvnfmWorkflowRequest(upgradeRequests.get(1),
                                                                                               EvnfmWorkFlowUpgradeRequest.class);
        assertThat(upgradeRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(upgradeRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.8");

        // verify helm version is 3.8 during rollback for modify
        final var rollbackRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).exchange(endsWith("/rollback"), eq(HttpMethod.POST), rollbackRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowRollbackRequest rollbackRequest = extractEvnfmWorkflowRequest(rollbackRequestCaptor.getValue(),
                                                                                   EvnfmWorkFlowRollbackRequest.class);

        // verify values.yaml doesn't contain EVNFM_PARAMS during modify package request
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        assertThat(rollbackRequest.getHelmClientVersion()).isEqualTo("3.8");

        // rollback failure message
        HelmReleaseLifecycleMessage rollbackMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.FAILED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK,
                                                                                     String.valueOf(revisionNumber++));
        rollbackMessage.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackMessage);

        rollbackMessage.setState(HelmReleaseState.COMPLETED);
        rollbackMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(rollbackMessage);

        // wait for operation to complete
        await().atMost(60, TimeUnit.SECONDS).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // assertions on state
        LifecycleOperation afterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedRollback.getHelmCharts())
                .extracting("state", "releaseName")
                .contains(tuple(FAILED.toString(), secondHelmReleaseNameFor(releaseName)),
                          tuple(ROLLED_BACK.toString(), firstHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartModifyFailsDifferentVnfd() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end-modify-wrong-vnfd";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //  Modify package id before instantiation
        MvcResult result = requestHelper.getMvcResultModifyRequest(instance.getVnfInstanceId(),
                                                                   null,
                                                                   E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID,
                                                                   null,
                                                                   null,
                                                                   null);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString())
                .contains("VnfPackageId cannot be modified when packageId from onboarding doesn't match with the vnfPackageId");
    }
}
