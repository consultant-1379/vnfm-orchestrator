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
package com.ericsson.vnfm.orchestrator.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FAILED;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.thirdHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;


@TestPropertySource(properties = "orchestrator.suffixFirstCnfReleaseSchema=false")
public class EndToEndLCMReleaseWithoutSuffixTest extends AbstractEndToEndTest {

    @Test
    public void successfulInstantiateUpgradeHeal() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end-no-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "end-to-end-no-release-suffix");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage("end-to-end-no-release-suffix",
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-no-release-suffix", "end-to-end-no-release-suffix-2");

        //Upgrade - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage("end-to-end-no-release-suffix",
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);

        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-no-release-suffix", "end-to-end-no-release-suffix-2");

        //Upgrade - Failed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FAILED, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName("end-to-end-no-release-suffix");
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("3");

        testingMessageSender.sendMessage(failed);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName("end-to-end-no-release-suffix");
        failed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        failed.setState(HelmReleaseState.COMPLETED);

        testingMessageSender.sendMessage(failed);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        //Heal
        result = requestHelper.getMvcResultHealRequestAndVerifyAccepted(vnfInstanceResponse, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // Fake termination completion message within heal
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName("end-to-end-no-release-suffix");
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendHealCompleteTerminateMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false);

        // Fake instantiation completion message within heal
        completed = getHelmReleaseLifecycleMessage("end-to-end-no-release-suffix",
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.INSTANTIATE,
                                                   "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        VnfInstance healedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(healedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-no-release-suffix", "end-to-end-no-release-suffix-2");

        //Terminate
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName("end-to-end-no-release-suffix");
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), COMPLETED);
    }

    @Test
    public void successfulInstantiateUpgradeRollback() throws Exception {
        // create identifier
        final String releaseName = "rollback-pattern-no-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "rollback-pattern-no-release-suffix");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName,
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, LifecycleOperationState.COMPLETED);

        // upgrade request
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-3", false, false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(releaseName,
                                                                                    HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                    "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        // rollback request
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "d3def1ce-4cf4-477c-aab3-21cb04e6a378",
                                                                                false,
                                                                                false,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // First Command: Chart 1 - Rollback
        final HelmReleaseLifecycleMessage firstChart = getHelmReleaseLifecycleMessage(releaseName,
                                                                                      HelmReleaseState.COMPLETED,
                                                                                      lifeCycleOperationId,
                                                                                      HelmReleaseOperationType.ROLLBACK,
                                                                                      "1");
        messageHelper.sendMessageForChart(firstChart, releaseName, vnfInstanceResponse.getId(), true, HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Rollback
        final HelmReleaseLifecycleMessage secondChart = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                       HelmReleaseState.COMPLETED,
                                                                                       lifeCycleOperationId,
                                                                                       HelmReleaseOperationType.ROLLBACK,
                                                                                       "1");
        messageHelper.sendMessageForChart(secondChart,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Third Command: Chart 3 - Terminate/Delete
        final HelmReleaseLifecycleMessage thirdChart = getHelmReleaseLifecycleMessage(thirdHelmReleaseNameFor(releaseName),
                                                                                      HelmReleaseState.COMPLETED,
                                                                                      lifeCycleOperationId,
                                                                                      HelmReleaseOperationType.TERMINATE,
                                                                                      null);
        messageHelper.sendMessageForDeletedChart(thirdChart, thirdHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(), false);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.COMPLETED));

        VnfInstance instanceAfterRollback = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterRollback.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("rollback-pattern-no-release-suffix", "rollback-pattern-no-release-suffix-2");
    }

    @Test
    public void successfulSingleToMultiUpgradeChain() throws Exception {
        //Create Identifier
        final String releaseName = "single-to-multi-upgrade-chain-no-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        "single-helm-chart-for-release-naming-instantiate");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "single-to-multi-upgrade-no-release-suffix");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.CRD,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Instantiated CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.INSTANTIATE,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName,
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1", "sample-crd2", releaseName);

        //Upgrade #1 - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "multi-helm-chart-for-release-naming-upgrade-1",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), COMPLETED);

        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1",
                              "sample-crd2",
                              releaseName,
                              releaseName + "-2",
                              releaseName + "-3");

        //Upgrade #2 - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "multi-helm-chart-for-release-naming-upgrade-2",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.CRD,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Upgraded CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "3");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), PROCESSING);

        //Fake termination completion messages
        HelmReleaseLifecycleMessage completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.TERMINATE,
                "3");
        HelmReleaseLifecycleMessage completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.DELETE_PVC,
                "3");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       vnfInstanceResponse.getId(),
                                                                       COMPLETED);

        upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1",
                              "sample-crd2",
                              releaseName + "-2",
                              releaseName + "-4");

        //Upgrade #3 (to original package) - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "single-helm-chart-for-release-naming-instantiate",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.CRD,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Upgraded CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "4");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), PROCESSING);

        //Fake termination completion messages
        completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.TERMINATE,
                "4");
        completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.DELETE_PVC,
                "4");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       vnfInstanceResponse.getId(),
                                                                       COMPLETED);

        upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1", "sample-crd2", releaseName);

        //Terminate
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName("instantiate-upgrade-chain-no-release-suffix");
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);
    }

    @Test
    public void successfulMultiToMultiUpgradeChain() throws Exception {
        //Create Identifier
        final String releaseName = "multi-to-multi-upgrade-chain-no-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        "multi-helm-chart-for-release-naming-instantiate");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "multi-to-multi-upgrade-no-release-suffix");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.CRD,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Instantiated CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.INSTANTIATE,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName,
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1", "sample-crd2", releaseName, releaseName + "-2");

        //Upgrade #1 - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "multi-helm-chart-for-release-naming-upgrade-1",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), COMPLETED);

        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1",
                              "sample-crd2",
                              releaseName,
                              releaseName + "-2",
                              releaseName + "-3");

        //Upgrade #2 - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "multi-helm-chart-for-release-naming-upgrade-2",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.CRD,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Upgraded CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "3");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), PROCESSING);

        //Fake termination completion messages
        HelmReleaseLifecycleMessage completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.TERMINATE,
                "3");
        HelmReleaseLifecycleMessage completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.DELETE_PVC,
                "3");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       vnfInstanceResponse.getId(),
                                                                       COMPLETED);

        upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1",
                              "sample-crd2",
                              releaseName + "-3",
                              releaseName + "-4");

        //Upgrade #3 (to original package) - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                "multi-helm-chart-for-release-naming-instantiate",
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake CRD instantiation completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.CRD,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Upgraded CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage,
                                                            vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            COMPLETED,
                                                            true,
                                                            false);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName,
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "4");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completed, vnfInstanceResponse.getId(), PROCESSING);

        //Fake termination completion messages
        completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.TERMINATE,
                "4");
        completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                releaseName,
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.DELETE_PVC,
                "4");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       vnfInstanceResponse.getId(),
                                                                       COMPLETED);

        upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("sample-crd1", "sample-crd2", releaseName, releaseName + "-2");

        //Terminate
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName("instantiate-upgrade-chain-no-release-suffix");
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);
    }
}
