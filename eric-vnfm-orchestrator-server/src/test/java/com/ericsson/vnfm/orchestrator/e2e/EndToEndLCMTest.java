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
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.CNF_BRO_URL_INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.TestUtils.CNF_BRO_URL_UPGRADE;
import static com.ericsson.vnfm.orchestrator.TestUtils.DEFAULT_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FAILED;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.UsageStateRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;


public class EndToEndLCMTest extends AbstractEndToEndTest {

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Test
    public void successfulInstantiateChangePackageInfoWithMultipleHelmChartPackages() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end";
        final String namespace = "end-to-end-instantiate-upgrade-multi";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, namespace);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        verifyVnfInstanceDetails(instance);
        verificationHelper.checkComplexTypesProperlyMapped(result, vnfInstanceResponse);
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_INSTANTIATE);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        // Check operation and history records after instantiate
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                             lifeCycleOperationId,
                                                                             LifecycleOperationType.INSTANTIATE,
                                                                             InstantiationState.INSTANTIATED);

        //Verify that instance contains correct values
        verifyMapContainsKey(vnfInstance.getCombinedValuesFile(), "testValue");

        assertThat(vnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-1", "end-to-end-2");

        // Check instantiate request denied for duplicates
        VnfInstanceResponse duplicateInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        // Try to instantiate duplicate instance with the release name
        MvcResult duplicateInstantiateRequestResult =
                requestHelper.getMvcResultNegativeInstantiateRequest(duplicateInstanceResponse, namespace, DEFAULT_CLUSTER_NAME, false);
        assertInstantiateRequestDenied(duplicateInstantiateRequestResult, duplicateInstanceResponse.getId());

        // Testcase - Rollback to previous version (downgrade) successful

        instance.setVnfPackageId("aabbcc-DOWNGRADE-PACKAGE");

        //Make preparations before downgrade/rollback
        stepsHelper.setupPreRollbackOperationHistory(releaseName, vnfInstanceResponse);

        // Downgrade/rollback to previous version: No rollback pattern in the vnfd, default pattern will be generated.
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        final String[] rollbackPattern = afterRollbackRequest.getRollbackPattern().split(",");
        assertThat(rollbackPattern).hasSize(2);

        VnfInstance actualInstance = afterRollbackRequest.getVnfInstance();
        VnfInstance tempInsAfterRollback = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInsAfterRollback.getHelmCharts()).hasSize(2);

        // Second chart first stage : Rollback
        HelmReleaseLifecycleMessage rollback2Completed = new HelmReleaseLifecycleMessage();
        rollback2Completed.setLifecycleOperationId(lifeCycleOperationId);
        rollback2Completed.setReleaseName(secondHelmReleaseNameFor(releaseName));
        rollback2Completed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollback2Completed.setState(HelmReleaseState.COMPLETED);
        rollback2Completed.setRevisionNumber("2");
        rollback2Completed.setMessage("Rollback completed");
        testingMessageSender.sendMessage(rollback2Completed);

        await().until(awaitHelper.helmChartReachesState(secondHelmReleaseNameFor(releaseName),
                                                        vnfInstanceResponse.getId(),
                                                        HelmReleaseState.COMPLETED,
                                                        true));

        // Check still rolling back
        LifecycleOperation lifecycleOperationAfterFirstStage = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperationAfterFirstStage.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);

        // First chart, first stage : Rollback
        HelmReleaseLifecycleMessage rollback1Completed = new HelmReleaseLifecycleMessage();
        rollback1Completed.setLifecycleOperationId(lifeCycleOperationId);
        rollback1Completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        rollback1Completed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        rollback1Completed.setState(HelmReleaseState.COMPLETED);
        rollback1Completed.setRevisionNumber("2");
        rollback1Completed.setMessage("Rollback completed");
        testingMessageSender.sendMessage(rollback1Completed);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository.findById(
                lifeCycleOperationId);

        //Assertion that it is a DOWNGRADE operation
        if (changePackageOperationDetails.isPresent()) {
            assertThat(changePackageOperationDetails.get()
                               .getChangePackageOperationSubtype()).isEqualTo(ChangePackageOperationSubtype.DOWNGRADE);
        } else {
            fail("missing changePackageOperationDetails");
        }

        assertThat(instance.getResourceDetails()).isBlank();

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);

        // Check operation and history records have been updated as is successful downgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        // End of testCase Rollback to previous version (downgrade) successful

        //Scale Out failure after retry
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        HelmReleaseLifecycleMessage scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                 HelmReleaseState.FAILED,
                                                                                 lifeCycleOperationId,
                                                                                 HelmReleaseOperationType.SCALE,
                                                                                 "2");
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 1, true));
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 2, true));
        testingMessageSender.sendMessage(scaleFailed);
        await().timeout(10, TimeUnit.MINUTES).until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstance.getVnfInstanceId());
        assertThat(StringUtils.countMatches(vnfInstance.getTempInstance(), vnfInstance.getVnfInstanceId())).isEqualTo(2);
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        checkRetryCount(vnfInstance, 1, 2);
        // Check operation and history records have not been updated as is failed scale
        verificationHelper.checkOperationValuesAndHistoryRecordsNotSet(lifeCycleOperationId);

        HelmReleaseLifecycleMessage rollback = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                              HelmReleaseState.COMPLETED,
                                                                              lifeCycleOperationId,
                                                                              HelmReleaseOperationType.ROLLBACK,
                                                                              "3");
        testingMessageSender.sendMessage(rollback);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        //Scale Out failure verifying count reset
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstance.getVnfInstanceId());
        checkRetryCount(vnfInstance, 1, 0);
        scaleFailed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                     HelmReleaseState.FAILED,
                                                     lifeCycleOperationId,
                                                     HelmReleaseOperationType.SCALE,
                                                     "4");
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 1, true));
        testingMessageSender.sendMessage(scaleFailed);
        await().until(awaitHelper.helmChartReachesRetryCount(scaleFailed.getReleaseName(), vnfInstance.getVnfInstanceId(), 2, true));
        testingMessageSender.sendMessage(scaleFailed);

        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstance.getVnfInstanceId());
        assertThat(StringUtils.countMatches(vnfInstance.getTempInstance(), vnfInstance.getVnfInstanceId())).isEqualTo(2);
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);
        checkRetryCount(vnfInstance, 1, 2);

        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.SCALE,
                                                   "5");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);

        //Assertions on state of instance after successful instantiate
        vnfInstance = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                 lifeCycleOperationId,
                                                                 LifecycleOperationType.SCALE,
                                                                 InstantiationState.INSTANTIATED);
        verificationHelper.checkScaleLevel(vnfInstance, 1, 0);

        //Scale In completed
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.SCALE,
                                                   "6");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.SCALE,
                                                   InstantiationState.INSTANTIATED);
        verificationHelper.checkScaleLevel(vnfInstance, 0, 0);

        // Check operation and history records have been updated as is successful scale
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Upgrade - Completed
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithComplexTypes(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        verificationHelper.checkComplexTypesProperlyMapped(result, vnfInstanceResponse);
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_UPGRADE);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of the operation and instance
        VnfInstance vnfInstanceAfterUpgrade = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                                         lifeCycleOperationId,
                                                                                         LifecycleOperationType.CHANGE_VNFPKG,
                                                                                         InstantiationState.INSTANTIATED);

        //Check that instance contains correct values in case of self Upgrade
        verifyMapDoesNotContainKey(vnfInstanceAfterUpgrade.getCombinedValuesFile(), "testValue");
        verifyValuesFilePassedToWfs(restTemplate, 6, UPGRADE_URL_ENDING, "testValue", false);

        assertThat(instance.getResourceDetails()).isBlank();
        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Upgrade - Failed
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FAILED, true);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setMessage("Helm/kubectl command timedOut");
        failed.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        failed.setState(HelmReleaseState.FAILED);
        failed.setRevisionNumber("7");

        testingMessageSender.sendMessage(failed);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLING_BACK));

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState())
                .isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(helmChartRepository.findByVnfInstance(instance))
                .extracting("releaseName", "state", "revisionNumber")
                .contains(
                        tuple(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), "7"),
                        tuple(secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED.toString(), "7"));
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);

        //Check that instance contains correct values
        verifyMapDoesNotContainKey(upgradedInstance.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 7, UPGRADE_URL_ENDING, "listType", false);

        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstHelmReleaseNameFor(releaseName), HelmReleaseState.ROLLING_BACK.toString()),
                        tuple(secondHelmReleaseNameFor(releaseName), null));

        failed.setLifecycleOperationId(lifeCycleOperationId);
        failed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        failed.setOperationType(HelmReleaseOperationType.ROLLBACK);
        failed.setState(HelmReleaseState.COMPLETED);

        testingMessageSender.sendMessage(failed);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(lifecycleOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);
        assertThat(helmChartRepository.findByVnfInstance(instance))
                .extracting("releaseName", "state", "revisionNumber")
                .contains(
                        tuple(firstHelmReleaseNameFor(releaseName), HelmReleaseState.ROLLED_BACK.toString(), "7"),
                        tuple(secondHelmReleaseNameFor(releaseName), null, null));

        // Check operation and history records have not been updated as is failed upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsNotSet(lifeCycleOperationId);

        //Heal
        result = requestHelper.getMvcResultHealRequestAndVerifyAccepted(vnfInstanceResponse, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        // Fake termination completion message within heal
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendHealCompleteTerminateMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false);

        // Fake instantiation completion message within heal
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.INSTANTIATE,
                                                   "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        VnfInstance healedVnfInstance = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                                   lifeCycleOperationId,
                                                                                   LifecycleOperationType.HEAL,
                                                                                   InstantiationState.INSTANTIATED);

        //Check that instance contains correct values
        verifyMapContainsKey(healedVnfInstance.getCombinedValuesFile(), "listType");

        verificationHelper.verifyHealAdditionalParams(lifeCycleOperationId);

        assertThat(healedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-1", "end-to-end-2");

        //Terminate
        result = requestHelper.getMvcResultTerminateRequestAndVerifyAccepted(vnfInstanceResponse);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        //Fake completion messages
        completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(firstHelmReleaseNameFor(releaseName));
        completed.setOperationType(HelmReleaseOperationType.TERMINATE);
        completed.setState(HelmReleaseState.COMPLETED);

        messageHelper.sendCompleteTerminateMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.TERMINATE,
                                                   InstantiationState.NOT_INSTANTIATED);

        byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isNotPresent();
    }

    @Test
    public void testInstantiateUpgradeFlowVnfPackageWithCrdCharts() throws Exception {
        // 1. Perform Successful Instantiate request with CRD charts
        // Create Identifier
        final String releaseNameCrdSuccess = "end-to-end-crd-success";
        String packageVnfdIdInstantiate = "d3def1ce-4cf4-477c-aab3-123456789101";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseNameCrdSuccess, packageVnfdIdInstantiate);

        // Assertions on state of instance
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "crd-instantiate-success");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // Fake CRD instantiation completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.CRD,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Instantiated CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage, vnfInstanceResponse.getId(), HelmReleaseOperationType.INSTANTIATE,
                                                            COMPLETED, true, false);

        // Fake chart instantiation completion message
        HelmReleaseLifecycleMessage vnfCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseNameCrdSuccess),
                                                                                  HelmReleaseState.COMPLETED,
                                                                                  lifeCycleOperationId,
                                                                                  HelmReleaseOperationType.INSTANTIATE,
                                                                                  "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(vnfCompleted, vnfInstanceResponse.getId(), false, COMPLETED);

        // Check operation and history records after instantiate
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);
        //Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.INSTANTIATE,
                                                   InstantiationState.INSTANTIATED);

        // 2. Perform Successful ChangeVnfPackage request with CRD charts
        String upgradePackageVnfdId = "a3f91625-c12c-4897-854e-3bd49b84263b";
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, upgradePackageVnfdId, false);
        String upgradeOperationId = getLifeCycleOperationId(result);

        // Fake CRD upgrade completion message
        WorkflowServiceEventMessage completedUpgradeWfsMessage = getWfsEventMessage(upgradeOperationId,
                                                                                    WorkflowServiceEventType.CRD,
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    "Upgraded CRD chart.");
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedUpgradeWfsMessage, vnfInstanceResponse.getId(),
                                                            HelmReleaseOperationType.INSTANTIATE, COMPLETED, true, false);

        // Fake chart upgrade completion message
        HelmReleaseLifecycleMessage vnfUpgradeCompleted = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseNameCrdSuccess),
                                                                                         HelmReleaseState.COMPLETED,
                                                                                         upgradeOperationId,
                                                                                         HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                         "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(vnfUpgradeCompleted, vnfInstanceResponse.getId(), true, COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   upgradeOperationId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getResourceDetails()).isBlank();
        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(upgradeOperationId);

        // 3. Perform failure ChangeVnfPackage request with CRD charts
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, packageVnfdIdInstantiate, false);
        String upgradeFailedOperationId = getLifeCycleOperationId(result);

        // Fake CRD instantiation completion message
        WorkflowServiceEventMessage failedWfsUpgradeMessage = getWfsEventMessage(upgradeFailedOperationId,
                                                                                 WorkflowServiceEventType.CRD,
                                                                                 WorkflowServiceEventStatus.FAILED,
                                                                                 "Helm/kubectl command timedOut");
        failedWfsUpgradeMessage.setReleaseName("crd-package1");
        messageHelper.sendInternalApiFailedMessageForCRDChart(failedWfsUpgradeMessage, vnfInstanceResponse.getId(), HelmReleaseState.ROLLED_BACK);

        awaitHelper.awaitOperationReachingState(upgradeFailedOperationId, LifecycleOperationState.ROLLED_BACK);
        LifecycleOperation operationAfterFailure = lifecycleOperationRepository.findByOperationOccurrenceId(upgradeFailedOperationId);
        assertThat(operationAfterFailure.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                       + "\"detail\":\"Helm/kubectl command timedOut\",\"instance\":\"about:blank\"}");

        // usage state for the package should be updated two times: 1) after successful first upgrade and 2) after failed second upgrade
        final URI updateUsageStateUri = onboardingUriProvider.getUpdateUsageStateUri(packageVnfdIdInstantiate);
        verify(onboardingClient, times(2)).put(eq(updateUsageStateUri),
                                               ArgumentMatchers.<UsageStateRequest>argThat(request -> !request.isInUse()));

        // 4. Perform failure Instantiate with CRDs
        // Create Identifier
        final String releaseNameCrdFailure = "end-to-end-crd-failure";
        VnfInstanceResponse vnfInstanceResponseCrdFailed = requestHelper.executeCreateVnfRequest(releaseNameCrdFailure, packageVnfdIdInstantiate);

        // Assertions on state of instance
        VnfInstance vnfInstanceCrdFailed = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponseCrdFailed.getId());
        assertThat(vnfInstanceCrdFailed.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // Instantiate
        MvcResult resultCrdFailed = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponseCrdFailed,
                                                                                                  "crd-instantiate-failure");
        String lifeCycleOperationIdCrdFailed = getLifeCycleOperationId(resultCrdFailed);

        // Fake CRD instantiation completion message
        WorkflowServiceEventMessage failedWfsMessage = getWfsEventMessage(lifeCycleOperationIdCrdFailed,
                                                                          WorkflowServiceEventType.CRD,
                                                                          WorkflowServiceEventStatus.FAILED,
                                                                          "Helm/kubectl command timedOut");
        failedWfsMessage.setReleaseName("crd-package1");
        messageHelper.sendInternalApiFailedMessageForCRDChart(failedWfsMessage, vnfInstanceResponseCrdFailed.getId(), HelmReleaseState.FAILED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationIdCrdFailed, FAILED);
        operationAfterFailure = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationIdCrdFailed);
        assertThat(operationAfterFailure.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                       + "\"detail\":\"Helm/kubectl command timedOut\",\"instance\":\"about:blank\"}");
    }

    @Test //Steps to reproduce issue SM-152558
    public void testRequestAnyLcmOpToInstanceWithCurrentOperationInProgressWillNotFailOngoingOp() throws Exception {
        // 1. Perform Successful Create Instance request
        // Create Identifier
        final String releaseName = "end-to-end-previous-op-not-failed";
        String packageVnfdIdInstantiate = "d3def1ce-4cf4-477c-aab3-123456789101";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, packageVnfdIdInstantiate);

        // Assertions on state of instance
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // 2. Execute Instantiate Request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "previous-op-not-failed-ns");
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // 3. Execute any Lcm Request that will conflict with execution of instantiate lcm operation
        MvcResult conflictResult = requestHelper.getMvcResultTerminateRequest(vnfInstanceResponse);
        assertThat(conflictResult.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());

        // 4. Check that ongoing Lcm Operation is still processing and has no error
        final LifecycleOperation instantiateOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(instantiateOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(instantiateOperation.getError()).isNull();
    }

    @Test
    public void successfulSingleToMultiUpgradeChain() throws Exception {
        //Create Identifier
        final String releaseName = "single-to-multi-upgrade-chain-with-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        "single-helm-chart-for-release-naming-instantiate");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "single-to-multi-upgrade-with-release-suffix");
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
                .containsOnly("sample-crd1", "sample-crd2", releaseName + "-1");

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
                              releaseName + "-1",
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
                .containsOnly("sample-crd1", "sample-crd2", releaseName + "-1");

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
        final String releaseName = "multi-to-multi-upgrade-chain-with-release-suffix";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        "multi-helm-chart-for-release-naming-instantiate");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "multi-to-multi-upgrade-with-release-suffix");
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
                .containsOnly("sample-crd1", "sample-crd2", releaseName + "-1", releaseName + "-2");

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
                              releaseName + "-1",
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
                .containsOnly("sample-crd1", "sample-crd2", releaseName + "-1", releaseName + "-2");

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

    private void assertInstantiateRequestDenied(MvcResult result, String vnfInstanceId) {
        assertThat(result.getResponse().getStatus()).isEqualTo(409);

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        assertThat(instance).isNotNull();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(lifecycleOperationRepository.findByVnfInstance(instance)).isEmpty();
    }

    private void checkRetryCount(final VnfInstance vnfInstance, final int chartPriority, final int expectedCount) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> helmCharts = tempInstance.getHelmCharts();
        Optional<HelmChart> helmChart = helmCharts.stream().filter(obj -> obj.getPriority() == chartPriority).findFirst();
        if (helmChart.isPresent()) {
            assertThat(helmChart.get().getRetryCount())
                    .withFailMessage("Failing scale test failed due to a retry count of {} when expecting {}",
                                     helmChart.get().getRetryCount(), expectedCount)
                    .isEqualTo(expectedCount);
        } else {
            fail("Failed to find a chart from VNF {} with a priority of {}", vnfInstance.getVnfInstanceName(), chartPriority);
        }
    }

    private void verifyVnfInstanceDetails(final VnfInstance instance) {
        ClusterConfigFile configFileByName = clusterConfigService.getConfigFileByName(DEFAULT_CLUSTER_NAME);
        String clusterServer = configFileByName.getClusterServer();
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isPresent();
        VnfInstanceNamespaceDetails details = byVnfId.get();
        assertThat(details.isDeletionInProgress()).isFalse();
        assertThat(details.getNamespace()).isEqualTo("end-to-end-instantiate-upgrade-multi");
        assertThat(details.getClusterServer()).isEqualTo(clusterServer);
    }

    private static HttpEntity<UsageStateRequest> usageStateRequestNotInUse(final VnfInstance vnfInstance) {
        return argThat(request -> Objects.equals(request.getBody().getVnfId(), vnfInstance.getVnfInstanceId()) && !request.getBody().isInUse());
    }
}
