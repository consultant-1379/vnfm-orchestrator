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
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
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
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;


public class ManualRollbackTest extends AbstractEndToEndTest {

    @Captor
    private ArgumentCaptor<Path> toValuesFileCaptor;

    @Test
    public void singleHelmChartUpgradeFailsManualRollbackSuccess() throws Exception {
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
        final String releaseName = "single-chart-upgrade-fails-manual-rollback-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(1);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);

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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-rollback-2", false, false, true);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);

        final LifecycleOperation priorToFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = priorToFailedUpgrade.getVnfInstance();
        assertThat(instanceBeforeUpgradeFail.getHelmCharts()).hasSize(1);
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        verifyMapContainsKey(instanceBeforeUpgradeFail.getCombinedValuesFile(), "listType");

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");
        verifyMapDoesNotContainKey(temp.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING, "listType", false);

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
        LifecycleOperation afterUpgradeFailed = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(afterUpgradeFailed.getError()).contains("upgrade failed");
        assertThat(afterUpgradeFailed.getStateEnteredTime()).isAfter(priorToFailedUpgrade.getStateEnteredTime());
        assertThat(afterUpgradeFailed.getStartTime()).isEqualTo(priorToFailedUpgrade.getStartTime());

        VnfInstance instanceAfterUpgradeFailed = afterUpgradeFailed.getVnfInstance();
        assertThat(instanceAfterUpgradeFailed.getHelmCharts()).hasSize(1);
        assertThat(instanceAfterUpgradeFailed.getTempInstance()).isNotEmpty();
        assertThat(instanceAfterUpgradeFailed.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(1);

        //First stage in failure pattern: rollback        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(afterRollbackRequest.getFailurePattern()).isEqualTo(
                "[{\"single-chart-upgrade-fails-manual-rollback-success-1\":\"rollback\"},"
                        + "{\"single-chart-upgrade-fails-manual-rollback-success-1\":\"install\"},"
                        + "{\"single-chart-upgrade-fails-manual-rollback-success-1\":\"upgrade\"},"
                        + "{\"single-chart-upgrade-fails-manual-rollback-success-1\":\"delete\"},"
                        + "{\"single-chart-upgrade-fails-manual-rollback-success-1\":\"delete_pvc[app=postgres,release=test]\"}]");
        VnfInstance actualInstance = afterRollbackRequest.getVnfInstance();
        VnfInstance tempIns = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempIns.getHelmCharts()).hasSize(1);

        //rollback message
        HelmReleaseLifecycleMessage rollbackCompleted = new HelmReleaseLifecycleMessage();
        rollbackCompleted.setLifecycleOperationId(lifeCycleOperationId);
        rollbackCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackCompleted.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackCompleted.setState(HelmReleaseState.COMPLETED);
        rollbackCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackCompleted.setMessage("rollback completed");
        testingMessageSender.sendMessage(rollbackCompleted);

        // Check still rolling back
        LifecycleOperation lifecycleOperationAfterFirstStage = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperationAfterFirstStage.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        // Second stage in failure pattern: instantiate
        // Instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompleted = new HelmReleaseLifecycleMessage();
        instantiateCompleted.setLifecycleOperationId(lifeCycleOperationId);
        instantiateCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        instantiateCompleted.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        instantiateCompleted.setState(HelmReleaseState.COMPLETED);
        instantiateCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        instantiateCompleted.setMessage("Instantiate completed");
        testingMessageSender.sendMessage(instantiateCompleted);

        // Check still rolling back
        LifecycleOperation lifecycleOperationAfterSecondStage = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperationAfterSecondStage.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        // Third stage in failure pattern: upgrade
        // Upgrade completed message
        HelmReleaseLifecycleMessage upgradeCompleted = new HelmReleaseLifecycleMessage();
        upgradeCompleted.setLifecycleOperationId(lifeCycleOperationId);
        upgradeCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        upgradeCompleted.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        upgradeCompleted.setState(HelmReleaseState.COMPLETED);
        upgradeCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        upgradeCompleted.setMessage("Upgrade completed");
        testingMessageSender.sendMessage(upgradeCompleted);

        // Check still rolling back
        LifecycleOperation lifecycleOperationAfterThirdStage = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperationAfterThirdStage.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        // Fourth stage in failure pattern: delete
        // Terminate completed message
        HelmReleaseLifecycleMessage terminateCompleted = new HelmReleaseLifecycleMessage();
        terminateCompleted.setLifecycleOperationId(lifeCycleOperationId);
        terminateCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        terminateCompleted.setOperationType(HelmReleaseOperationType.TERMINATE);
        terminateCompleted.setState(HelmReleaseState.COMPLETED);
        terminateCompleted.setRevisionNumber(String.valueOf(revisionNumber++));
        terminateCompleted.setMessage("Terminate completed");
        testingMessageSender.sendMessage(terminateCompleted);

        // Check still rolling back
        LifecycleOperation lifecycleOperationAfterFourthStage = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperationAfterFourthStage.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        // Fifth stage in failure pattern: delete_pvc
        // Delete Pvc completed message
        WorkflowServiceEventMessage deletePvcCompleted = new WorkflowServiceEventMessage();
        deletePvcCompleted.setLifecycleOperationId(lifeCycleOperationId);
        deletePvcCompleted.setReleaseName(firstHelmReleaseNameFor(releaseName));
        deletePvcCompleted.setStatus(WorkflowServiceEventStatus.COMPLETED);
        deletePvcCompleted.setType(WorkflowServiceEventType.DELETE_PVC);
        testingMessageSender.sendMessage(deletePvcCompleted);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        Mockito.verify(workflowRoutingService)
                .routeToEvnfmWfsFakeUpgrade(any(), any(), any(), any(), toValuesFileCaptor.capture());

        //check toValuesFile
        Path toValuesFile = toValuesFileCaptor.getValue();
        assertThat(toValuesFile.toString()).contains("values");

        // assertions on instance
        VnfInstance instanceAfterRollback = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterRollback.getTempInstance()).isNull();
        verifyMapContainsKey(instanceAfterRollback.getCombinedValuesFile(), "listType");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsManualRollbackSuccess() throws Exception {
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
        final String releaseName = "first-upgrade-fails-manual-rollback-success";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-rollback");

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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false, false, false);
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
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

        // upgrade failed message
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
        assertThat(afterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"first-upgrade-fails-manual-rollback-success-1\":\"rollback\"}]");
        VnfInstance actualInstance = afterRollbackRequest.getVnfInstance();

        // rollback message
        HelmReleaseLifecycleMessage rollbackSuccess = new HelmReleaseLifecycleMessage();
        rollbackSuccess.setLifecycleOperationId(lifeCycleOperationId);
        rollbackSuccess.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackSuccess.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackSuccess.setState(HelmReleaseState.COMPLETED);
        rollbackSuccess.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackSuccess.setMessage("rollback completed");

        testingMessageSender.sendMessage(rollbackSuccess);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        // NB: currently the helm chart "state" is not being update because the functionality has not been implemented yet to update the helm chart.
        //Some assertions has not been added.

        VnfInstance instanceAfterRollback = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterRollback.getTempInstance()).isNull();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsManualRollbackSuccess() throws Exception {
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
        final String releaseName = "second-upgrade-fails-manual-rollback-success";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-rollback");

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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false, false, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);

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
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

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

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        Mockito.verify(workflowRoutingService).routeToEvnfmWfsFakeUpgrade(any(), any(), any(), any(),
                                                                          toValuesFileCaptor.capture());

        //check toValuesFile
        Path toValuesFile = toValuesFileCaptor.getValue();
        assertThat(toValuesFile.toString()).contains("values");

        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(afterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"second-upgrade-fails-manual-rollback-success-1\":\"upgrade\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-success-2\":\"rollback\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-success-2\":\"delete\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-success-2\":\"delete_pvc\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-success-2\":\"install\"}]");

        // verify helm version is 3.8 during rollback for upgrade command
        final var upgradeInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/second-upgrade-fails-manual-rollback-success-1/upgrade"),
                                                eq(HttpMethod.POST),
                                                upgradeInPatternRequestCaptor
                                                        .capture(),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowUpgradeRequest upgradeRequestInPattern = extractEvnfmWorkflowRequest(upgradeInPatternRequestCaptor.getAllValues().get(1),
                                                                                          EvnfmWorkFlowUpgradeRequest.class);

        assertThat(upgradeRequestInPattern.getHelmClientVersion()).isEqualTo("3.8");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage2 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            lifeCycleOperationId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendMessageForChart(firstChartStage2,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // verify helm version is 3.8 during rollback for rollback command
        final var rollbackInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        await().untilAsserted(() -> {
            verify(restTemplate, times(1)).exchange(endsWith("/rollback"), eq(HttpMethod.POST), rollbackInPatternRequestCaptor
                .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        });

        EvnfmWorkFlowRollbackRequest rollbackRequestInPattern = extractEvnfmWorkflowRequest(rollbackInPatternRequestCaptor.getValue(),
                                                                                            EvnfmWorkFlowRollbackRequest.class);

        assertThat(rollbackRequestInPattern.getHelmClientVersion()).isEqualTo("3.8");

        // Second Command: Chart 2 - Rollback
        final HelmReleaseLifecycleMessage firstChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            lifeCycleOperationId,
                                                                                            HelmReleaseOperationType.ROLLBACK,
                                                                                            "1");
        messageHelper.sendMessageForChart(firstChartStage1,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // verify helm version is 3.8 during rollback for terminate command
        verify(restTemplate, times(1)).exchange(ArgumentMatchers.contains("&helmClientVersion=3.8"), eq(HttpMethod.POST), any(HttpEntity.class),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());

        // Third Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             lifeCycleOperationId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage1,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Forth Command: Chart 2 - Delete Pvc
        final WorkflowServiceEventMessage secondChartStage2 = getWfsEventMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 WorkflowServiceEventStatus.COMPLETED,
                                                                                 lifeCycleOperationId,
                                                                                 WorkflowServiceEventType.DELETE_PVC);
        messageHelper.sendMessageForChart(secondChartStage2,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // verify helm version is 3.8 during rollback for install command
        final var installInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        await().untilAsserted(() -> {
            verify(restTemplate, times(3)).exchange(endsWith("/instantiate"), eq(HttpMethod.POST),
                    installInPatternRequestCaptor
                            .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());

            assertThat(installInPatternRequestCaptor.getAllValues()).isNotEmpty();
        });

        EvnfmWorkFlowInstantiateRequest installInPatternRequest = extractEvnfmWorkflowRequest(installInPatternRequestCaptor.getAllValues().get(2),
                                                                                              EvnfmWorkFlowInstantiateRequest.class);

        assertThat(installInPatternRequest.getHelmClientVersion()).isEqualTo("3.8");

        // Fifth Command: Chart 2 - Install/Instantiate
        final HelmReleaseLifecycleMessage secondChartStage3 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             lifeCycleOperationId,
                                                                                             HelmReleaseOperationType.INSTANTIATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage3,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        // assertions on instance
        VnfInstance instanceAfterRollback = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterRollback.getTempInstance()).isNull();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartUpgradeFailsManualRollbackFailed() throws Exception{
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
        final String releaseName = "single-chart-upgrade-fails-manual-rollback-fails";
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
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");

        // assertions on state
        assertThat(priorToFailedUpgrade.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(priorToFailedUpgrade.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeUpgradeFail.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeUpgradeFail.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeUpgradeFail.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(1);
        assertThat(temp.getVnfDescriptorId()).isNotEqualTo(instanceBeforeUpgradeFail.getVnfDescriptorId());
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

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

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(afterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"single-chart-upgrade-fails-manual-rollback-fails-1\":\"rollback\"},"
                                   + "{\"single-chart-upgrade-fails-manual-rollback-fails-1\":\"install\"},"
                                   + "{\"single-chart-upgrade-fails-manual-rollback-fails-1\":\"upgrade\"},"
                                   + "{\"single-chart-upgrade-fails-manual-rollback-fails-1\":\"delete\"},"
                                   + "{\"single-chart-upgrade-fails-manual-rollback-fails-1\":\"delete_pvc[app=postgres,release=test]\"}]");
        VnfInstance actualInstance = afterRollbackRequest.getVnfInstance();

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
        // NB: currently the helm chart "state" is not being update because the functionality has not been implemented yet.
        //Some assertions has not been added.
        LifecycleOperation operationAfterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterFailedRollback.getError()).contains("upgrade failed", "rollback failed");

        VnfInstance instanceAfterFailedRollback = operationAfterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsManualRollbackFailed() throws Exception {
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
        final String releaseName = "first-upgrade-fails-manual-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-rollback");

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmCharts()).hasSize(2);

        // instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         releaseName,
                                                                                         EXISTING_CLUSTER_CONFIG_NAME);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false, false, false);
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
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

        // upgrade failed message
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
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(FAILED.toString(), null);

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation operationAfterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(operationAfterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"first-upgrade-fails-manual-rollback-fails-1\":\"rollback\"}]");
        assertThat(operationAfterRollbackRequest.getVnfInstance().getHelmClientVersion()).isEqualTo("3.8");

        // rollback message
        HelmReleaseLifecycleMessage rollbackFailed = new HelmReleaseLifecycleMessage();
        rollbackFailed.setLifecycleOperationId(lifeCycleOperationId);
        rollbackFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackFailed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackFailed.setState(HelmReleaseState.FAILED);
        rollbackFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackFailed.setMessage("rollback failed");
        testingMessageSender.sendMessage(rollbackFailed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        // NB: currently the helm chart "state" is not being update because the functionality has not been implemented yet.
        //Some assertions has not been added.
        LifecycleOperation operationAfterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterFailedRollback.getError()).contains("upgrade failed", "rollback failed");
        assertThat(operationAfterFailedRollback.getVnfInstance().getTempInstance()).isNull();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartFirstUpgradeFailsManualRollbackFailedWithHttpResponseError() throws Exception {
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
        final String releaseName = "manual-rollback-fails-http-response";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-rollback");

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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false, false, false);
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
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

        // upgrade failed message
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
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(FAILED.toString(), null);

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation operationAfterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(operationAfterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"manual-rollback-fails-http-response-1\":\"rollback\"}]");
        assertThat(operationAfterRollbackRequest.getVnfInstance().getHelmClientVersion()).isEqualTo("3.8");

        // rollback message
        HelmReleaseLifecycleMessage rollbackFailed = new HelmReleaseLifecycleMessage();
        rollbackFailed.setLifecycleOperationId(lifeCycleOperationId);
        rollbackFailed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollbackFailed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollbackFailed.setState(HelmReleaseState.FAILED);
        rollbackFailed.setRevisionNumber(String.valueOf(revisionNumber++));
        rollbackFailed.setMessage("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                          + "\"detail\":\"rollback failed\",\"instance\":null}");
        testingMessageSender.sendMessage(rollbackFailed);

        // wait for operation to complete
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, FAILED));

        // assertions on state
        // NB: currently the helm chart "state" is not being update because the functionality has not been implemented yet.
        //Some assertions has not been added.
        LifecycleOperation operationAfterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterFailedRollback.getError()).contains("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                             + "\"detail\":\"upgrade failed\\nrollback failed\",\"instance\":\"about:blank\"}");
        assertThat(operationAfterFailedRollback.getVnfInstance().getTempInstance()).isNull();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartSecondUpgradeFailsManualRollbackFailed() throws Exception {
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
        final String releaseName = "second-upgrade-fails-manual-rollback-fails";

        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-rollback");

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
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false, false, false);
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
        assertThat(temp.getHelmClientVersion()).isEqualTo("3.10");

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
        assertThat(instanceBeforeUpgradeFail.getHelmClientVersion()).isEqualTo("3.8");
        VnfInstance tempInstance = mapper.readValue(instanceAfterUpgradeFailed.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts()).hasSize(2);
        assertThat(tempInstance.getHelmCharts()).extracting("state").containsExactly(COMPLETED.toString(),
                                                                                     FAILED.toString());

        //send rollback message
        requestHelper.getMvcResultRollbackOperationRequest(lifeCycleOperationId);
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(afterRollbackRequest.getFailurePattern())
                .isEqualTo("[{\"second-upgrade-fails-manual-rollback-fails-1\":\"upgrade\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-fails-2\":\"rollback\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-fails-2\":\"delete\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-fails-2\":\"delete_pvc\"},"
                                   + "{\"second-upgrade-fails-manual-rollback-fails-2\":\"install\"}]");
        VnfInstance actualInstance = afterRollbackRequest.getVnfInstance();

        // rollback success message
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
        LifecycleOperation operationAfterFailedRollback =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operationAfterFailedRollback.getError()).contains("upgrade failed", "rollback failed");
        VnfInstance instanceAfterFailedRollback = operationAfterFailedRollback.getVnfInstance();
        assertThat(instanceAfterFailedRollback.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(instanceAfterFailedRollback.getTempInstance()).isNull();

        Mockito.verify(workflowRoutingService).routeToEvnfmWfsFakeUpgrade(any(), any(), any(), any(),
                                                                          toValuesFileCaptor.capture());

        //check toValuesFile
        Path toValuesFile = toValuesFileCaptor.getValue();
        assertThat(toValuesFile.toString()).contains("values");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }
}
