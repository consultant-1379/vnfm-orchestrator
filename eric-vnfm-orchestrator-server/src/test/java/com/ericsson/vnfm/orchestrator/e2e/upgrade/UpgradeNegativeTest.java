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
package com.ericsson.vnfm.orchestrator.e2e.upgrade;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.assertThatNoEvnfmParamsPassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.ROLLED_BACK;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.ROLLING_BACK;

import java.io.IOException;
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

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowInstantiateRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowRollbackRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowUpgradeRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.core.type.TypeReference;


public final class UpgradeNegativeTest extends AbstractEndToEndTest {

    @Test
    public void singleHelmChartUpgradeFailsRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING"
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG ROLLED_BACK"
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "single-chart-upgrade-fails-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        testingMessageSender.sendMessage(completed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart-2", true);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());
        verifyMapDoesNotContainKey(temp.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING, "listType", false);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
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
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(1);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(1);
        assertThat(tempInstance.getHelmCharts()).extracting("state").contains(ROLLING_BACK.toString());
        assertThat(tempInstance.getVnfPackageId()).isNotEqualTo(instanceAfterUpgradeFailed.getVnfPackageId());

        // rollback success message
        HelmReleaseLifecycleMessage rollbackSuccess = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.COMPLETED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK,
                                                                                     String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(rollbackSuccess);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state").contains(ROLLED_BACK.toString());
        assertThat(instanceAfterSuccessfulRollback.getHelmClientVersion()).isEqualTo("3.8");
        verifyMapContainsKey(instanceAfterSuccessfulRollback.getCombinedValuesFile(), "listType");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartUpgradeFailsAutoRollbackNotAllowed() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        int expectedCountOfCallsMessagingService = 7;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "upgrade-fails-auto-rollback-not-allowed";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        testingMessageSender.sendMessage(completed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart-2", false, false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                   HelmReleaseState.FAILED,
                                                                                   lifeCycleOperationId,
                                                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                   String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");
        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

        // assertions on state
        LifecycleOperation afterFailedUpgrade =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedUpgrade.getError()).contains("upgrade failed");

        VnfInstance instanceAfterFailedUpgrade = afterFailedUpgrade.getVnfInstance();
        assertThat(instanceAfterFailedUpgrade.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedUpgrade.getTempInstance()).isNotEmpty();
        assertThat(instanceAfterFailedUpgrade.getHelmClientVersion()).isEqualTo("3.8");

        VnfInstance tempInstanceAfterFailedUpgrade = parseJson(instanceAfterFailedUpgrade.getTempInstance(), VnfInstance.class);
        assertThat(tempInstanceAfterFailedUpgrade.getHelmCharts()).extracting("state").contains(FAILED.toString());

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartUpgradeFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "single-chart-upgrade-fails-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        testingMessageSender.sendMessage(completed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart-2", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
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
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(1);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(1);
        assertThat(tempInstance.getHelmCharts()).extracting("state").contains(ROLLING_BACK.toString());

        // rollback success message
        HelmReleaseLifecycleMessage rollbackFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.FAILED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.ROLLBACK,
                                                                                    String.valueOf(revisionNumber++));
        rollbackFailed.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackFailed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        LifecycleOperation afterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedRollback.getHelmCharts()).extracting("state").contains(FAILED.toString());
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsRollbackSucceeds() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-first-upgrade-fails-rollback-succeeds";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "be3086ea-1e4b-4364-9301-7303de0d49c3");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "8021b781-1ae9-4b53-adfe-4bf99427b316", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
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
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(ROLLING_BACK.toString(), null);

        // rollback success message
        HelmReleaseLifecycleMessage rollbackSuccess = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.COMPLETED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK,
                                                                                     String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(rollbackSuccess);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on state
        checkInstanceAfterSuccessfulRollback(instance, lifeCycleOperationId);

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsRollbackSucceeds() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-second-upgrade-fails-rollback-succeeds";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "facaa38f-623d-44f4-8dd8-13005ccea157");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

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

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "e504db63-d439-41d9-88b9-869e61a7ca17", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade succeeds message
        HelmReleaseLifecycleMessage upgradeSucceeded = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                      HelmReleaseState.COMPLETED,
                                                                                      lifeCycleOperationId,
                                                                                      HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                      String.valueOf(revisionNumber));
        testingMessageSender.sendMessage(upgradeSucceeded);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                   HelmReleaseState.FAILED,
                                                                                   lifeCycleOperationId,
                                                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                   String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");
        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().atMost(60, TimeUnit.SECONDS).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        // assertions on state
        LifecycleOperation afterUpgradeFailed =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterUpgradeFailed.getError()).contains("upgrade failed");
        assertThat(afterUpgradeFailed.getStateEnteredTime()).isAfter(priorToFailedUpgrade.getStateEnteredTime());
        assertThat(afterUpgradeFailed.getStartTime()).isEqualTo(priorToFailedUpgrade.getStartTime());

        VnfInstance instanceAfterUpgradeFailed = afterUpgradeFailed.getVnfInstance();
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(2);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state")
                .containsExactly(PROCESSING.toString(), ROLLING_BACK.toString());

        // verify helm version is 3.10 during upgrade
        final var upgradeRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/upgrade"), any(HttpMethod.class), upgradeRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        var upgradeRequests = upgradeRequestCaptor.getAllValues();
        EvnfmWorkFlowUpgradeRequest upgradeRequestForFirstChart = extractEvnfmWorkflowRequest(upgradeRequests.get(0),
                                                                                              EvnfmWorkFlowUpgradeRequest.class);
        EvnfmWorkFlowUpgradeRequest upgradeRequestForSecondChart = extractEvnfmWorkflowRequest(upgradeRequests.get(1),
                                                                                               EvnfmWorkFlowUpgradeRequest.class);
        assertThat(upgradeRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(upgradeRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.10");

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
        await().atMost(60, TimeUnit.SECONDS).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state")
                .containsExactly(ROLLED_BACK.toString(), ROLLED_BACK.toString());

        // verify helm version is 3.8 during rollback for upgrade
        final var rollbackRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/rollback"), eq(HttpMethod.POST), rollbackRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowRollbackRequest rollbackRequestForSecondChart = extractEvnfmWorkflowRequest(rollbackRequestCaptor.getAllValues().get(0),
                                                                                                 EvnfmWorkFlowRollbackRequest.class);
        EvnfmWorkFlowRollbackRequest rollbackRequestForFirstChart = extractEvnfmWorkflowRequest(rollbackRequestCaptor.getAllValues().get(1),
                                                                                                EvnfmWorkFlowRollbackRequest.class);

        assertThat(rollbackRequestForSecondChart.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(rollbackRequestForFirstChart.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-first-upgrade-fails-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "be3086ea-1e4b-4364-9301-7303de0d49c3");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "8021b781-1ae9-4b53-adfe-4bf99427b316", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
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
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(ROLLING_BACK.toString(), null);

        // rollback success message
        HelmReleaseLifecycleMessage rollbackFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                    HelmReleaseState.FAILED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.ROLLBACK,
                                                                                    String.valueOf(revisionNumber++));
        rollbackFailed.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackFailed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterSuccessfulRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedRollback.getHelmCharts()).extracting("state").containsOnly(FAILED.toString(), null);
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsRollbackFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-second-upgrade-fails-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "facaa38f-623d-44f4-8dd8-13005ccea157");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "e504db63-d439-41d9-88b9-869e61a7ca17", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade succeeds message
        HelmReleaseLifecycleMessage upgradeSucceeded = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                      HelmReleaseState.COMPLETED,
                                                                                      lifeCycleOperationId,
                                                                                      HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                      String.valueOf(revisionNumber));
        testingMessageSender.sendMessage(upgradeSucceeded);

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
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state")
                .containsExactly(PROCESSING.toString(), ROLLING_BACK.toString());

        // rollback success message
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
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // assertions on state
        LifecycleOperation afterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedRollback.getHelmCharts())
                .extracting("state", "releaseName")
                .contains(tuple(FAILED.toString(), TestUtils.secondHelmReleaseNameFor(releaseName)),
                          tuple(ROLLED_BACK.toString(), firstHelmReleaseNameFor(releaseName)));
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void multiHelmChartFirstUpgradeFailsEnsureLevelsReset() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG ROLLING_BACK
        // 8 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-failed-upgrade-levels-extensions-reset";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName,
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        assertThat(instance.getHelmCharts().get(0).getRevisionNumber()).isEqualTo("1");
        assertThat(instance.getHelmCharts().get(1).getRevisionNumber()).isEqualTo("1");

        final List<ScaleInfoEntity> scaleInfoAtInstantiate = instance.getScaleInfoEntity();

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();

        // assertions on state
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        //Check that instantiationLevel and extensions are correctly set in the temp instance
        assertThat(temp.getInstantiationLevel()).isEqualTo(INST_LEVEL_1);
        Map<String, Object> extensions =
                (Map<String, Object>) convertStringToJSONObj(temp.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions.get(PAYLOAD)).isEqualTo(MANUAL_CONTROLLED);
        assertThat(extensions.get(PAYLOAD_2)).isEqualTo(CISM_CONTROLLED);
        assertThat(extensions.get(PAYLOAD_3)).isEqualTo(MANUAL_CONTROLLED);

        //check levels and extensions are null
        assertThat(instanceBeforeUpgradeFail.getInstantiationLevel()).isNull();
        assertThat(instanceBeforeUpgradeFail.getVnfInfoModifiableAttributesExtensions()).isNull();

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
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
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(ROLLING_BACK.toString(), null);

        //check levels and extensions are null
        assertThat(instanceAfterUpgradeFailed.getInstantiationLevel()).isNull();
        assertThat(instanceAfterUpgradeFailed.getVnfInfoModifiableAttributesExtensions()).isNull();

        // rollback success message
        HelmReleaseLifecycleMessage rollbackSuccess = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                     HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK,
                                                                                     String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(rollbackSuccess);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on state
        VnfInstance instanceAfterSuccessfulRollback = checkInstanceAfterSuccessfulRollback(instance, lifeCycleOperationId);

        //check levels and extensions are null
        assertThat(instanceAfterSuccessfulRollback.getInstantiationLevel()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfInfoModifiableAttributesExtensions()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getHelmClientVersion()).isEqualTo("3.8");

        //check helm chart replica details have not been updated
        List<HelmChart> instanceAfterSuccessfulRollbackChartList = instanceAfterSuccessfulRollback.getHelmCharts();
        for (HelmChart chartAfterRollback : instanceAfterSuccessfulRollbackChartList) {
            HelmChart chartBeforeUpgradeFail =
                    instanceBeforeUpgradeFail.getHelmCharts()
                            .stream()
                            .filter(
                                    beforeUpgradeChart -> beforeUpgradeChart.getHelmChartName().equals(chartAfterRollback.getHelmChartName()))
                            .findFirst()
                            .get();
            final Map<String, ReplicaDetails> detailsBeforeRollback = replicaDetailsMapper.getReplicaDetailsFromHelmChart(chartBeforeUpgradeFail);
            final Map<String, ReplicaDetails> detailsAfterRollback = replicaDetailsMapper.getReplicaDetailsFromHelmChart(chartAfterRollback);
            assertThat(detailsBeforeRollback).isEqualTo(detailsAfterRollback);
        }

        //check scale info has not been updated
        final List<ScaleInfoEntity> scaleInfoAfterRollback = instanceAfterSuccessfulRollback.getScaleInfoEntity();
        assertThat(scaleInfoAtInstantiate).usingElementComparatorIgnoringFields("vnfInstance").isEqualTo(scaleInfoAfterRollback);

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    private VnfInstance checkInstanceAfterSuccessfulRollback(final VnfInstance instance, final String lifeCycleOperationId) {
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state").containsExactlyInAnyOrder(ROLLED_BACK.toString(), null);
        return instanceAfterSuccessfulRollback;
    }
}
