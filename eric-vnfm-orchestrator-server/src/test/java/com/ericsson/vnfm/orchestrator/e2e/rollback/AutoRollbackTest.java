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
package com.ericsson.vnfm.orchestrator.e2e.rollback;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class AutoRollbackTest extends AbstractEndToEndTest {
    @Test
    public void singleHelmChartUpgradeFailsAutoRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 8;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "single-chart-upgrade-fails-auto-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        testingMessageSender.sendMessage(completed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-rollback-2", false, false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());

        //upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

        // assertions on state
        LifecycleOperation afterUpgradeFailed =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(afterUpgradeFailed.getError()).contains("upgrade failed");
        assertThat(afterUpgradeFailed.getStateEnteredTime()).isAfter(priorToFailedUpgrade.getStateEnteredTime());
        assertThat(afterUpgradeFailed.getStartTime()).isEqualTo(priorToFailedUpgrade.getStartTime());

        VnfInstance instanceAfterUpgradeFailed = afterUpgradeFailed.getVnfInstance();
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(1);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(1);
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");

        //rollback message
        HelmReleaseLifecycleMessage rollbackCompleted = new HelmReleaseLifecycleMessage();
        rollbackCompleted.setLifecycleOperationId(lifeCycleOperationId);
        rollbackCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackCompleted.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackCompleted.setState(HelmReleaseState.COMPLETED);
        rollbackCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackCompleted.setMessage("rollback completed");

        testingMessageSender.sendMessage(rollbackCompleted);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder(HelmReleaseState.ROLLED_BACK.toString());

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsAutoRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLING_BACK
        // 9 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 9;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "first-upgrade-fails-auto-rollback-succeeds";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "be3086ea-1e4b-4364-9301-7303de0d49c3");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate,1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "8021b781-1ae9-4b53-adfe-4bf99427b316", false,
                                                                                false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        //upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

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
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(FAILED.toString(), null);

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        //rollback message
        HelmReleaseLifecycleMessage rollbackCompleted = new HelmReleaseLifecycleMessage();
        rollbackCompleted.setLifecycleOperationId(lifeCycleOperationId);
        rollbackCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackCompleted.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackCompleted.setState(HelmReleaseState.COMPLETED);
        rollbackCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackCompleted.setMessage("rollback completed");

        testingMessageSender.sendMessage(rollbackCompleted);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceAfterSuccessfulRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterSuccessfulRollback.getTempInstance()).isNull();
        assertThat(instanceAfterSuccessfulRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterSuccessfulRollback.getHelmCharts()).extracting("state")
                .containsExactlyInAnyOrder(HelmReleaseState.ROLLED_BACK.toString(), null);

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsAutoRollbackSuccess() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLING_BACK
        // 9 CHANGE_VNFPKG ROLLED_BACK
        int expectedCountOfCallsMessagingService = 9;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "second-upgrade-fails-auto-rollback-succeeds";

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
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "e504db63-d439-41d9-88b9-869e61a7ca17", false,
                                                                                false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade succeeds message
        HelmReleaseLifecycleMessage upgradeSucceeded = new HelmReleaseLifecycleMessage();
        upgradeSucceeded.setLifecycleOperationId(lifeCycleOperationId);
        upgradeSucceeded.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeSucceeded.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeSucceeded.setState(HelmReleaseState.COMPLETED);
        upgradeSucceeded.setRevisionNumber(String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(upgradeSucceeded);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

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
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(COMPLETED.toString(),
                                                                                     FAILED.toString());
        // rollback request
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);

        // rollback success message
        HelmReleaseLifecycleMessage rollbackSuccess = new HelmReleaseLifecycleMessage();
        rollbackSuccess.setLifecycleOperationId(lifeCycleOperationId);
        rollbackSuccess.setReleaseName(secondHelmReleaseNameFor(releaseName));
        rollbackSuccess.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackSuccess.setState(HelmReleaseState.COMPLETED);
        rollbackSuccess.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackSuccess.setMessage("rollback completed");

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
        assertThat(instanceAfterSuccessfulRollback.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartUpgradeFailsAutoRollbackFailed() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLING_BACK
        // 9 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 9;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "single-chart-upgrade-fails-auto-rollback-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        testingMessageSender.sendMessage(completed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-rollback-2", false, false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());

        //upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

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

        // rollback request
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);

        // rollback failed message
        HelmReleaseLifecycleMessage rollbackMessage = new HelmReleaseLifecycleMessage();
        rollbackMessage.setLifecycleOperationId(lifeCycleOperationId);
        rollbackMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackMessage.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackMessage.setState(HelmReleaseState.FAILED);
        rollbackMessage.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackMessage.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackMessage);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        LifecycleOperation afterSuccessfulRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterSuccessfulRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterSuccessfulRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsAutoRollbackFailed() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLING_BACK
        // 9 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 9;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "first-upgrade-fails-auto-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "be3086ea-1e4b-4364-9301-7303de0d49c3");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // instantiate complete message
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "8021b781-1ae9-4b53-adfe-4bf99427b316", false,
                                                                                false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");

        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        //upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

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
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(FAILED.toString(), null);

        // rollback request
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);

        // rollback failed message
        HelmReleaseLifecycleMessage rollbackMessage = new HelmReleaseLifecycleMessage();
        rollbackMessage.setLifecycleOperationId(lifeCycleOperationId);
        rollbackMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackMessage.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackMessage.setState(HelmReleaseState.FAILED);
        rollbackMessage.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackMessage.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackMessage);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        LifecycleOperation afterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(instanceAfterFailedRollback.getHelmCharts()).extracting("state").containsOnly(FAILED.toString(), null);
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsAutoRollbackFailed() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE COMPLETED
        // 5 CHANGE_VNFPKG STARTING
        // 6 CHANGE_VNFPKG PROCESSING
        // 7 CHANGE_VNFPKG FAILED_TEMP
        // 8 CHANGE_VNFPKG ROLLING_BACK
        // 9 CHANGE_VNFPKG FAILED
        int expectedCountOfCallsMessagingService = 9;

        int revisionNumber = 1;
        // create identifier
        final String releaseName = "second-upgrade-fails-auto-rollback-fails";

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
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setRevisionNumber(String.valueOf(revisionNumber++));

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, instance.getVnfInstanceId(), false, COMPLETED);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "e504db63-d439-41d9-88b9-869e61a7ca17", false,
                                                                                false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        // upgrade succeeds message
        HelmReleaseLifecycleMessage upgradeSucceeded = new HelmReleaseLifecycleMessage();
        upgradeSucceeded.setLifecycleOperationId(lifeCycleOperationId);
        upgradeSucceeded.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeSucceeded.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeSucceeded.setState(HelmReleaseState.COMPLETED);
        upgradeSucceeded.setRevisionNumber(String.valueOf(revisionNumber++));
        testingMessageSender.sendMessage(upgradeSucceeded);

        // upgrade failed message
        HelmReleaseLifecycleMessage upgradeFailed = new HelmReleaseLifecycleMessage();
        upgradeFailed.setLifecycleOperationId(lifeCycleOperationId);
        upgradeFailed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        upgradeFailed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeFailed.setState(HelmReleaseState.FAILED);
        upgradeFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeFailed.setMessage("upgrade failed");

        testingMessageSender.sendMessage(upgradeFailed);

        // wait for ...
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED_TEMP));

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
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(COMPLETED.toString(),
                                                                                     FAILED.toString());

        // rollback request
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);

        // rollback failed message
        HelmReleaseLifecycleMessage rollbackMessage = new HelmReleaseLifecycleMessage();
        rollbackMessage.setLifecycleOperationId(lifeCycleOperationId);
        rollbackMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        rollbackMessage.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackMessage.setState(HelmReleaseState.FAILED);
        rollbackMessage.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackMessage.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackMessage);

        rollbackMessage.setState(HelmReleaseState.COMPLETED);
        rollbackMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(rollbackMessage);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        LifecycleOperation afterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = afterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();
        assertThat(instanceAfterFailedRollback.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }
}
