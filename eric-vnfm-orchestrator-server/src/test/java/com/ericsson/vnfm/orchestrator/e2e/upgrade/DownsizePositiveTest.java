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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkChartDownsizeState;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValueInMap;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

public class DownsizePositiveTest extends AbstractEndToEndTest {

    @Test
    public void successfulChangePackageInfoDownsizeAllowedSingleChart() throws Exception {
        final String releaseName = "end-to-end-downsize";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "downsize-namespace", "test-downsize");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), false);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);

        //Fake upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "2");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        vnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getTempInstance()).isNull();
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), null, false);
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, null); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", "testing value"); //non-evnfm value set during upgrade
    }

    @Test
    public void successfulChangePackageInfoDownsizeAllowedMultiChartParamsAndYamlFile() throws Exception {
        final String releaseName = "end-to-end-downsize-multi-with-values-file";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithYamlFile(vnfInstanceResponse,
                                                                                    "downsize-namespace-multi-with-values-2",
                                                                                    EXISTING_CLUSTER_CONFIG_NAME,
                                                                                    "valueFiles/large-values-a.yaml");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        instance = vnfInstanceRepository.findByVnfInstanceId(instance.getVnfInstanceId());
        verifyValueInMap(instance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm additionalParam value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "eric-evnfm-testing",
                         "{instantiate=not included in upgrade}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm additionalParam value set during upgrade
        verifyValueInMap(instance.getCombinedValuesFile(), "eric-data-distributed-coordinator-ed", null); //non-evnfm yaml value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithYamlFile(vnfInstanceResponse,
                                                                           E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                           true,
                                                                           "valueFiles/large-values-b.yaml");
        lifeCycleOperationId = getLifeCycleOperationId(result);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), null, false);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), false);

        //Fake downsize completion message for release 2
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        completedWfsMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(secondHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                false));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), false);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);

        //Fake downsize completion message for release 1
        completedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(firstHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                false));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);

        //Fake upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "2");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        instance = operationAfterUpgrade.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(instance.getHelmClientVersion()).isEqualTo("3.10");
        verifyValueInMap(instance.getCombinedAdditionalParams(), HELM_NO_HOOKS, null); //evnfm value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm additionalParam value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "eric-evnfm-testing",
                         "{instantiate=not included in upgrade}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "upgradeParam", "testing value"); //non-evnfm additionalParam value set during upgrade
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "eric-data-distributed-coordinator-ed",
                         "{security={tls={agentToBro={enabled=false}}}}"); //non-evnfm yaml value set during upgrade
    }

    @Test
    public void testDownsizeOperationPassDownsizeFailUpgradeTriggersAutoRollback() throws Exception {
        final String releaseName = "end-to-end-downsize-fail-upgrade";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-namespace-1",
                                                                                         "test-downsize");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterInstantiate = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instantiatedVnfInstance = operationAfterInstantiate.getVnfInstance();

        verifyValueInMap(instantiatedVnfInstance.getCombinedValuesFile(), "testValue", "test");

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart-2", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        LifecycleOperation operationAfterFailedUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeUpgradeFail = mapper.readValue(operationAfterFailedUpgrade.getVnfInstance().getTempInstance(), VnfInstance.class);

        verifyValueInMap(instanceBeforeUpgradeFail.getCombinedValuesFile(), "testValue", "test2");

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        LifecycleOperation operationAfterRollingBack = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceAfterRollingBack = mapper.readValue(operationAfterRollingBack.getVnfInstance().getTempInstance(), VnfInstance.class);
        assertThat(instanceAfterRollingBack.getVnfPackageId()).isNotEqualTo(operationAfterRollingBack.getVnfInstance().getVnfPackageId());

        //Fake autorollback's downsize completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.DOWNSIZE,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), true);

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), true);

        //Fake autorollback's upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "3");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        completedHelmMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedHelmMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterAutoRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance rollbackedVnfInstance = operationAfterAutoRollback.getVnfInstance();
        assertThat(rollbackedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(rollbackedVnfInstance.getTempInstance()).isNull();

        verifyValueInMap(rollbackedVnfInstance.getCombinedValuesFile(), "testValue", "test");
    }

    @Test
    public void testDownsizeOperationPassDownsizeFailUpgradeNoAutoRollbackFailedTemp() throws Exception {
        // Create CNF Indetifier
        // Instantiate CNF
        // Upgrade CNF with dowsizeAllowed=true, autoRollbackAllowed=false
        // Downsize before actual Upgrade success
        // Upgrade fail, should be in FAILED_TEMP state
        final String releaseName = "end-to-end-downsize-upgrade-fail-no-autorollback";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                "downsize-fail-upgrade-namespace-22",
                "test-downsize");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                lifeCycleOperationId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize and no autorollback
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, false, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                WorkflowServiceEventType.DOWNSIZE,
                WorkflowServiceEventStatus.COMPLETED,
                "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.FAILED_TEMP);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(upgradedVnfInstance.getTempInstance()).isNotNull();
    }

    @Test
    public void testDownsizeOperationPassDownsizeFailUpgradeFailAutoRollbackPersistsAsFailed() throws Exception {
        final String releaseName = "end-to-end-downsize-fail-upgrade-fail-autorollback";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-namespace-2",
                                                                                         "test-downsize");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's downsize completion message
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.DOWNSIZE,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), true);

        //Fake autorollback's upgrade failed messages
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(releaseName);
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("3");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.FAILED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(upgradedVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationMultiChartPassFirstChartFailSecondChartDuringUpgradeTriggersAutoRollbackOnBothCharts() throws Exception {
        final String releaseName = "end-to-end-downsize-upgrade-multi-fail-second";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-namespace-multi-2",
                                                                                         "test-downsize");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                                true,
                                                                                true,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        //Fake all chart's downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade completion messages on chart 1
        HelmReleaseLifecycleMessage complete = new HelmReleaseLifecycleMessage();
        complete.setLifecycleOperationId(lifeCycleOperationId);
        complete.setReleaseName(firstHelmReleaseNameFor(releaseName));
        complete.setMessage("Helm/kubectl command timedOut");
        complete.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete.setState(HelmReleaseState.COMPLETED);
        complete.setRevisionNumber("2");
        testingMessageSender.sendMessage(complete);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        //Fake upgrade failed messages on chart 2
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("3");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), null, true);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), true);

        //Fake autorollback's downsize completion message for release 2
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.DOWNSIZE,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        completedWfsMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(secondHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                true));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), true);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), true);

        //Fake autorollback's downsize completion message for release 1
        completedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(firstHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                true));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), true);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), true);

        //Fake autorollback's upgrade completion messages on release 1
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "4");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        testingMessageSender.sendMessage(completedHelmMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 2
        completedHelmMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "5");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        testingMessageSender.sendMessage(completedHelmMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance rolledbackVnfInstance = operationAfterRollback.getVnfInstance();
        assertThat(rolledbackVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationMultiChartFailFirstChartDuringUpgradeTriggersAutoRollbackOnBothCharts() throws Exception {
        final String releaseName = "end-to-end-downsize-upgrade-multi-fail-first";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-namespace-multi-3",
                                                                                         "test-downsize");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                                true,
                                                                                true,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        //Fake all chart's downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade failed messages on chart 1
        HelmReleaseLifecycleMessage complete = new HelmReleaseLifecycleMessage();
        complete.setLifecycleOperationId(lifeCycleOperationId);
        complete.setReleaseName(firstHelmReleaseNameFor(releaseName));
        complete.setMessage("Helm/kubectl command timedOut");
        complete.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete.setState(HelmReleaseState.FAILED);
        complete.setRevisionNumber("2");
        testingMessageSender.sendMessage(complete);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), true);
        checkChartDownsizeState(instance, secondHelmReleaseNameFor(releaseName), null, true);

        //Fake autorollback's downsize completion message on chart 1
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.DOWNSIZE,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        completedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(firstHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                true));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), true);

        //Fake autorollback's upgrade completion messages on release 1
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "4");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        testingMessageSender.sendMessage(completedHelmMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 2
        completedHelmMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "4");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        testingMessageSender.sendMessage(completedHelmMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance rolledbackVnfInstance = operationAfterRollback.getVnfInstance();
        assertThat(rolledbackVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationFailDownsizeTriggersAutoRollback() throws Exception {
        final String releaseName = "end-to-end-downsize-fail-upgrade-auto-rollback";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-namespace-3",
                                                                                         "test-downsize");

        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize failed message
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(firstHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.FAILED,
                                                                false));

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(instance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED.toString(), false);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake rollback upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "2");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage,
                                                         vnfInstanceResponse.getId(),
                                                         false,
                                                         LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(upgradedVnfInstance.getVnfDescriptorId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        assertThat(upgradedVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationFailDownsizeFailUpgradeDuringAutoRollbackPersistsAsFailed() throws Exception {
        final String releaseName = "downsize-fail-upgrade-fail-auto-rollback";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-namespace-4",
                                                                                         "test-downsize");

        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize failed message
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade failed messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.FAILED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(upgradedVnfInstance.getVnfDescriptorId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        assertThat(upgradedVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationMultiChartPassFirstChartFailSecondChartDuringDownsizeTriggersAutorollbackOnBothCharts() throws Exception {
        String clusterName = "config";
        final String releaseName = "end-to-end-downsize-multi-fail-second-chart";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        vnfInstanceResponse.setClusterName(clusterName);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-namespace-multi-2",
                                                                                         clusterName);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                                true,
                                                                                true,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message for release 2
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        completedWfsMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(secondHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                false));

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.PROCESSING.toString(), false);
        checkChartDownsizeState(vnfInstance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);

        //Fake downsize failed message for release 1
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(firstHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.FAILED,
                                                                false));

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED.toString(), false);
        checkChartDownsizeState(vnfInstance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), false);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 2
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.COMPLETED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 1
        HelmReleaseLifecycleMessage complete = new HelmReleaseLifecycleMessage();
        complete.setLifecycleOperationId(lifeCycleOperationId);
        complete.setReleaseName(firstHelmReleaseNameFor(releaseName));
        complete.setMessage("Helm/kubectl command timedOut");
        complete.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete.setState(HelmReleaseState.COMPLETED);
        complete.setRevisionNumber("3");
        testingMessageSender.sendMessage(complete);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        vnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getTempInstance()).isNull();

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21cb04e6a378");
    }

    @Test
    public void testDownsizeOperationMultiChartFailFirstChartDuringDownsizeTriggersAutorollbackOnFirstChart() throws Exception {
        String clusterName = "config";
        final String releaseName = "end-to-end-downsize-multi-fail-first-chart";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        vnfInstanceResponse.setClusterName(clusterName);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-namespace-multi-4",
                                                                                         clusterName);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                                true,
                                                                                true,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize failed message for release 2
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(secondHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.FAILED,
                                                                false));

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        checkChartDownsizeState(vnfInstance, firstHelmReleaseNameFor(releaseName), null, false);
        checkChartDownsizeState(vnfInstance, secondHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED.toString(), false);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 1
        HelmReleaseLifecycleMessage complete = new HelmReleaseLifecycleMessage();
        complete.setLifecycleOperationId(lifeCycleOperationId);
        complete.setReleaseName(firstHelmReleaseNameFor(releaseName));
        complete.setMessage("Helm/kubectl command timedOut");
        complete.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete.setState(HelmReleaseState.COMPLETED);
        complete.setRevisionNumber("2");
        testingMessageSender.sendMessage(complete);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's upgrade completion messages on release 2
        HelmReleaseLifecycleMessage complete2 = new HelmReleaseLifecycleMessage();
        complete2.setLifecycleOperationId(lifeCycleOperationId);
        complete2.setReleaseName(secondHelmReleaseNameFor(releaseName));
        complete2.setMessage("Helm/kubectl command timedOut");
        complete2.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete2.setState(HelmReleaseState.COMPLETED);
        complete2.setRevisionNumber("3");
        testingMessageSender.sendMessage(complete2);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        vnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getTempInstance()).isNull();

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21cb04e6a378");
    }

    @Test
    public void testDownsizeOperationPassDownsizeFailUpgradeFailDownsizeDuringAutoRollback() throws Exception {
        final String releaseName = "end-to-end-downsize-fail-upgrade-fail-downsize";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-fail-downsize-namespace-1",
                                                                                         "test-downsize");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "single-helm-chart", true, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("2");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's downsize failed message
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.FAILED);

        LifecycleOperation operationAfterAutoRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance rollbackedVnfInstance = operationAfterAutoRollback.getVnfInstance();
        assertThat(rollbackedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(rollbackedVnfInstance.getTempInstance()).isNull();
    }

    @Test
    public void testDownsizeOperationMultiChartPassDownsizeFailUpgradeFailDownsizeDuringAutoRollback() throws Exception {
        final String releaseName = "end-to-end-downsize-multi-fail-upgr-fail-downsize";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-fail-upgrade-fail-downsize-namespace-2",
                                                                                         "test-downsize");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                                true,
                                                                                true,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        //Fake all chart's downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade completion messages on chart 1
        HelmReleaseLifecycleMessage complete = new HelmReleaseLifecycleMessage();
        complete.setLifecycleOperationId(lifeCycleOperationId);
        complete.setReleaseName(firstHelmReleaseNameFor(releaseName));
        complete.setMessage("Helm/kubectl command timedOut");
        complete.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        complete.setState(HelmReleaseState.COMPLETED);
        complete.setRevisionNumber("2");
        testingMessageSender.sendMessage(complete);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, PROCESSING);

        //Fake upgrade failed messages on chart 2
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("3");
        testingMessageSender.sendMessage(failed);
        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK);

        //Fake autorollback's downsize completion message for release 2
        completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                 WorkflowServiceEventType.DOWNSIZE,
                                                 WorkflowServiceEventStatus.COMPLETED,
                                                 "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        completedWfsMessage.setReleaseName(secondHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(completedWfsMessage);
        await().until(awaitHelper.helmChartDownSizeReachesState(secondHelmReleaseNameFor(releaseName),
                                                                vnfInstanceResponse.getId(),
                                                                HelmReleaseState.COMPLETED,
                                                                true));

        //Fake autorollback's downsize failed message for release 1
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                          WorkflowServiceEventType.DOWNSIZE,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Failed scaled down all ReplicaSets and StatefulSets to 0 so we can test "
                                                                                  + "autorollback.");
        failedWfsMessage.setReleaseName(firstHelmReleaseNameFor(releaseName));
        testingMessageSender.sendMessage(failedWfsMessage);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, LifecycleOperationState.FAILED);

        LifecycleOperation operationAfterAutoRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance rollbackedVnfInstance = operationAfterAutoRollback.getVnfInstance();
        assertThat(rollbackedVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(rollbackedVnfInstance.getTempInstance()).isNull();
    }
}
