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

import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.thirdHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.extractEvnfmWorkflowRequest;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.STARTING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowInstantiateRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowRollbackRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.EvnfmWorkFlowUpgradeRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.scheduler.CheckApplicationTimeout;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.type.TypeReference;


public class DowngradeTest extends AbstractEndToEndTest {

    private static final int OPERATION_TIME_EXCEED_DELTA_SECONDS = 125;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private CheckApplicationTimeout checkApplicationTimeoutComponent;

    @Captor
    private ArgumentCaptor<Path> toValuesFileCaptor;

    @Test
    public void multiHelmChartRollbackForDifferentChartCountSuccessWithPattern() throws Exception {
        // create identifier
        final String releaseName = "rollback-pattern-for-different-chart-count-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getHelmClientVersion()).isEqualTo("3.8");
        verifyMapContainsKey(instance.getCombinedValuesFile(), "listType");

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-3", false,
                                                                                          false, true);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation operationBeforeRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeRollback = operationBeforeRollback.getVnfInstance();
        assertThat(instanceBeforeRollback.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(operationBeforeRollback.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationBeforeRollback.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeRollback.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeRollback.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeRollback.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(3);

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));
        VnfInstance instanceAfterUpgrade = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterUpgrade.getHelmClientVersion()).isEqualTo("3.10");
        verifyMapDoesNotContainKey(instanceAfterUpgrade.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 3, UPGRADE_URL_ENDING, "listType", false);

        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "d3def1ce-4cf4-477c-aab3-21cb04e6a378", false,
                                                                                false, false);

        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        String afterRollbackOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"rollback-pattern-for-different-chart-count-success-1\":\"rollback\"}"
                                   + ",{\"rollback-pattern-for-different-chart-count-success-2\":\"rollback\"},"
                                   + "{\"rollback-pattern-for-different-chart-count-success-3\":\"delete\"}]");

        // First Command: Chart 1 - Rollback
        final HelmReleaseLifecycleMessage firstChart = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                      HelmReleaseState.COMPLETED,
                                                                                      afterRollbackOperationOccId,
                                                                                      HelmReleaseOperationType.ROLLBACK,
                                                                                      "1");
        messageHelper.sendMessageForChart(firstChart,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Rollback
        final HelmReleaseLifecycleMessage secondChart = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                       HelmReleaseState.COMPLETED,
                                                                                       afterRollbackOperationOccId,
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
                                                                                      afterRollbackOperationOccId,
                                                                                      HelmReleaseOperationType.TERMINATE, null);
        messageHelper.sendMessageForDeletedChart(thirdChart, thirdHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(), false);

        await().until(awaitHelper.operationReachesState(afterRollbackOperationOccId, LifecycleOperationState.COMPLETED));

        LifecycleOperation operationAfterRollbackFinished =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));

        Map<String, Object> valuesFile = parseJsonToGenericType(operationAfterRollbackFinished.getCombinedValuesFile(), new TypeReference<>() {
        });
        assertThat(valuesFile)
                .contains(Assertions.entry("override-key", "install"),
                          Assertions.entry("instantiate-key", "install"));
        VnfInstance vnfInstanceAfterRollback = afterRollbackRequest.getVnfInstance();
        List<HelmChart> helmCharts = helmChartRepository.findByVnfInstance(vnfInstanceAfterRollback);
        assertThat(helmCharts).isNotEmpty();
        assertThat(helmCharts.size()).isEqualTo(2);

        //Assertion that it is a DOWNGRADE operation
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository.findById(
                afterRollbackOperationOccId);
        if (changePackageOperationDetails.isPresent()) {
            assertThat(changePackageOperationDetails.get()
                               .getChangePackageOperationSubtype()
                               .equals(ChangePackageOperationSubtype.DOWNGRADE)).isTrue();
        } else {
            fail("missing changePackageOperationDetails");
        }
        assertThat(vnfInstanceAfterRollback.getResourceDetails()).isBlank();

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   afterRollbackOperationOccId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);
        VnfInstance instanceAfterRollback = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instanceAfterRollback.getHelmClientVersion()).isEqualTo("3.8");

        assertThat(instanceAfterRollback.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("rollback-pattern-for-different-chart-count-success-1", "rollback-pattern-for-different-chart-count-success-2");

        verifyMapContainsKey(instanceAfterRollback.getCombinedValuesFile(), "listType");


        // Check operation and history records have been updated as is successful downgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(afterRollbackOperationOccId);
    }

    @Test
    public void multiHelmChartRollbackSuccessWithPattern() throws Exception {
        // create identifier
        final String releaseName = "multi-chart-rollback-pattern-success";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

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
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false,
                                                                                          false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        final LifecycleOperation operationBeforeRollback = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instanceBeforeRollback = operationBeforeRollback.getVnfInstance();
        assertThat(instanceBeforeRollback.getHelmCharts()).hasSize(2);

        // assertions on state
        assertThat(operationBeforeRollback.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operationBeforeRollback.getStateEnteredTime()).isAfter(beforeOperation);
        assertThat(instanceBeforeRollback.getVnfdVersion()).isEqualTo(vnfInstanceResponse.getVnfdVersion());
        assertThat(instanceBeforeRollback.getTempInstance()).isNotEmpty();
        VnfInstance temp = mapper.readValue(instanceBeforeRollback.getTempInstance(), VnfInstance.class);
        assertThat(temp.getHelmCharts()).hasSize(2);

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

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

        result = requestHelper.getMvcResultChangeVnfpkgRequest(vnfInstanceResponse.getId(), "d3def1ce-4cf4-477c-aab3-21cb04e6a378", false, false,
                                                               false);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(),
                                                        HelmReleaseState.PROCESSING, false));
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        String afterRollbackOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(STARTING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"multi-chart-rollback-pattern-success-1\":\"upgrade\"}"
                                   + ",{\"multi-chart-rollback-pattern-success-2\":\"delete\"},"
                                   + "{\"multi-chart-rollback-pattern-success-2\":\"delete_pvc\"},"
                                   + "{\"multi-chart-rollback-pattern-success-1\":\"rollback\"},"
                                   + "{\"multi-chart-rollback-pattern-success-2\":\"install\"}]");

        // verify helm version is 3.8 during downgrade for upgrade command
        final var upgradeInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(endsWith("/multi-chart-rollback-pattern-success-1/upgrade"),
                                                eq(HttpMethod.POST),
                                                upgradeInPatternRequestCaptor
                                                        .capture(),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowUpgradeRequest upgradeRequestInPattern = extractEvnfmWorkflowRequest(upgradeInPatternRequestCaptor.getAllValues().get(1),
                                                                                          EvnfmWorkFlowUpgradeRequest.class);

        assertThat(upgradeRequestInPattern.getHelmClientVersion()).isEqualTo("3.8");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage1 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackOperationOccId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                            "2");
        messageHelper.sendMessageForChart(firstChartStage1,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // verify helm version is 3.8 during downgrade for terminate command
        verify(restTemplate, times(1)).exchange(ArgumentMatchers.contains("&helmClientVersion=3.8"),
                                                eq(HttpMethod.POST),
                                                any(HttpEntity.class),
                                                ArgumentMatchers.<Class<ResourceResponse>>any());

        // Second Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             afterRollbackOperationOccId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage1,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Third Command: Chart 2 - Delete Pvc
        final WorkflowServiceEventMessage secondChartStage2 = getWfsEventMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 WorkflowServiceEventStatus.COMPLETED,
                                                                                 afterRollbackOperationOccId,
                                                                                 WorkflowServiceEventType.DELETE_PVC);
        messageHelper.sendMessageForChart(secondChartStage2,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // verify helm version is 3.8 during downgrade for rollback command
        final var rollbackInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        await().untilAsserted(() -> {
            verify(restTemplate, times(1)).exchange(endsWith("/rollback"), eq(HttpMethod.POST), rollbackInPatternRequestCaptor
                    .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        });
        EvnfmWorkFlowRollbackRequest rollbackRequestInPattern = extractEvnfmWorkflowRequest(rollbackInPatternRequestCaptor.getValue(),
                                                                                            EvnfmWorkFlowRollbackRequest.class);

        assertThat(rollbackRequestInPattern.getHelmClientVersion()).isEqualTo("3.8");

        // Fourth Command: Chart 1 - Rollback
        final HelmReleaseLifecycleMessage firstChartStage2 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackOperationOccId,
                                                                                            HelmReleaseOperationType.ROLLBACK,
                                                                                            "1");
        messageHelper.sendMessageForChart(firstChartStage2,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Fifth Command: Chart 2 - Install/Instantiate
        final HelmReleaseLifecycleMessage secondChartStage3 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             afterRollbackOperationOccId,
                                                                                             HelmReleaseOperationType.INSTANTIATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage3,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);
        await().until(awaitHelper.operationReachesState(afterRollbackOperationOccId, LifecycleOperationState.COMPLETED));

        // verify helm version is 3.8 during downgrade for install command
        final var installInPatternRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(3)).exchange(endsWith("/instantiate"), eq(HttpMethod.POST),
                                                installInPatternRequestCaptor
                                                        .capture(), ArgumentMatchers.<Class<ResourceResponse>>any());
        EvnfmWorkFlowInstantiateRequest installInPatternRequest = extractEvnfmWorkflowRequest(installInPatternRequestCaptor.getAllValues().get(2),
                                                                                              EvnfmWorkFlowInstantiateRequest.class);

        assertThat(installInPatternRequest.getHelmClientVersion()).isEqualTo("3.8");

        LifecycleOperation operationAfterRollbackFinished =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));

        Map<String, Object> valuesFile = parseJsonToGenericType(operationAfterRollbackFinished.getCombinedValuesFile(), new TypeReference<>() {
        });
        assertThat(valuesFile)
                .contains(Assertions.entry("override-key", "install"),
                          Assertions.entry("instantiate-key", "install"));
        VnfInstance vnfInstanceAfterRollback = afterRollbackRequest.getVnfInstance();
        assertThat(vnfInstanceAfterRollback.getHelmCharts()).isNotEmpty();

        //Assertion that it is a DOWNGRADE operation
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository.findById(
                afterRollbackOperationOccId);
        if (changePackageOperationDetails.isPresent()) {
            assertThat(changePackageOperationDetails.get()
                               .getChangePackageOperationSubtype()
                               .equals(ChangePackageOperationSubtype.DOWNGRADE)).isTrue();
        } else {
            fail("missing changePackageOperationDetails");
        }
        assertThat(vnfInstanceAfterRollback.getResourceDetails()).isBlank();

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   afterRollbackOperationOccId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);

        // Check operation and history records have been updated as is successful downgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(afterRollbackOperationOccId);
    }

    @Test
    public void multiHelmChartRollbackFailInDeleteStage() throws Exception {
        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-rollback-pattern-fail-delete";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false,
                                                                                          false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

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

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        result = requestHelper.getMvcResultChangeVnfpkgRequest(vnfInstanceResponse.getId(), "d3def1ce-4cf4-477c-aab3-21cb04e6a378",
                                                               false, false,false);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(),
                                                        HelmReleaseState.PROCESSING, false));
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        String afterRollbackOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(STARTING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"multi-chart-rollback-pattern-fail-delete-1\":\"upgrade\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-2\":\"delete\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-2\":\"delete_pvc\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-1\":\"rollback\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-2\":\"install\"}]");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage1 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackOperationOccId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                            "2");
        messageHelper.sendMessageForChart(firstChartStage1,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.FAILED,
                                                                                             afterRollbackOperationOccId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             null);
        secondChartStage1.setMessage("Terminate operation failed.");
        testingMessageSender.sendMessage(secondChartStage1);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(afterRollbackOperationOccId, LifecycleOperationState.FAILED));
        assertThat(lifecycleOperationRepository.findByOperationOccurrenceId(afterRollbackOperationOccId).getError())
                .contains("Terminate operation failed.");
        assertRollbackOperationProperlyFailed(afterRollbackOperationOccId);
    }

    @Test
    public void multiHelmChartRollbackFailInDeletePvcStage() throws Exception {
        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-rollback-pattern-fail-delete-pvc";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false,
                                                                                          false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

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

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        result = requestHelper.getMvcResultChangeVnfpkgRequest(vnfInstanceResponse.getId(), "d3def1ce-4cf4-477c-aab3-21cb04e6a378", false, false,
                                                               false);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(),
                                                        HelmReleaseState.PROCESSING, false));
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        String afterRollbackOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(STARTING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"multi-chart-rollback-pattern-fail-delete-pvc-1\":\"upgrade\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-pvc-2\":\"delete\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-pvc-2\":\"delete_pvc\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-pvc-1\":\"rollback\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-delete-pvc-2\":\"install\"}]");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage1 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackOperationOccId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                            "2");
        messageHelper.sendMessageForChart(firstChartStage1,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             afterRollbackOperationOccId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage1,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Third Command: Chart 2 - Delete Pvc
        final WorkflowServiceEventMessage secondChartStage2 = getWfsEventMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 WorkflowServiceEventStatus.FAILED,
                                                                                 afterRollbackOperationOccId,
                                                                                 WorkflowServiceEventType.DELETE_PVC);
        secondChartStage2.setMessage("Delete pvc operation failed.");
        testingMessageSender.sendMessage(secondChartStage2);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(afterRollbackOperationOccId, LifecycleOperationState.FAILED));
        assertThat(lifecycleOperationRepository.findByOperationOccurrenceId(afterRollbackOperationOccId).getError())
                .contains("Delete pvc operation failed.");
        assertRollbackOperationProperlyFailed(afterRollbackOperationOccId);
    }

    @Test
    public void multiHelmChartRollbackFailInUpgradeStage() throws Exception {
        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-rollback-pattern-fail-upgrade";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false,
                                                                                          false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

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

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        result = requestHelper.getMvcResultChangeVnfpkgRequest(
                vnfInstanceResponse.getId(), "d3def1ce-4cf4-477c-aab3-21cb04e6a378", false, false, false);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(),
                                                        HelmReleaseState.PROCESSING, false));
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(getLifeCycleOperationId(result));
        String afterRollbackOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(STARTING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"multi-chart-rollback-pattern-fail-upgrade-1\":\"upgrade\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-upgrade-2\":\"delete\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-upgrade-2\":\"delete_pvc\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-upgrade-1\":\"rollback\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-upgrade-2\":\"install\"}]");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage2 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.FAILED,
                                                                                            afterRollbackOperationOccId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        firstChartStage2.setMessage("Upgrade operation failed.");
        testingMessageSender.sendMessage(firstChartStage2);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(afterRollbackOperationOccId, LifecycleOperationState.FAILED));
        assertThat(lifecycleOperationRepository.findByOperationOccurrenceId(afterRollbackOperationOccId).getError())
                .contains("Upgrade operation failed.");
        assertRollbackOperationProperlyFailed(afterRollbackOperationOccId);
    }

    @Test
    public void multiHelmChartRollbackFailInInstallStage() throws Exception {
        int revisionNumber = 1;
        // create identifier
        final String releaseName = "multi-chart-rollback-pattern-fail-install";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "d3def1ce-4cf4-477c-aab3-21cb04e6a378");
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        // upgrade request
        LocalDateTime beforeOperation = LocalDateTime.now();
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "multi-helm-rollback-2", false,
                                                                                          false, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

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

        HelmReleaseLifecycleMessage upgradeMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                    lifeCycleOperationId,
                                                                                    HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendCompleteMessageForAllCnfCharts(upgradeMessage, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, COMPLETED));

        result = requestHelper.getMvcResultChangeVnfpkgRequest(vnfInstanceResponse.getId(), "d3def1ce-4cf4-477c-aab3-21cb04e6a378", false, false,
                                                               false);
        final String afterRollbackLifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse.getId(),
                                                                                      HelmReleaseState.PROCESSING, false));
        LifecycleOperation afterRollbackRequest =
                lifecycleOperationRepository.findByOperationOccurrenceId(afterRollbackLifeCycleOperationId);
        String afterRollbackRequestOperationOccId = afterRollbackRequest.getOperationOccurrenceId();
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(afterRollbackRequest.getRollbackPattern())
                .isEqualTo("[{\"multi-chart-rollback-pattern-fail-install-1\":\"upgrade\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-install-2\":\"delete\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-install-2\":\"delete_pvc\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-install-1\":\"rollback\"},"
                                   + "{\"multi-chart-rollback-pattern-fail-install-2\":\"install\"}]");

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage1 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackRequestOperationOccId,
                                                                                            HelmReleaseOperationType.CHANGE_VNFPKG,
                                                                                            "2");
        messageHelper.sendMessageForChart(firstChartStage1,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.COMPLETED,
                                                                                             afterRollbackRequestOperationOccId,
                                                                                             HelmReleaseOperationType.TERMINATE,
                                                                                             null);
        messageHelper.sendMessageForChart(secondChartStage1,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Third Command: Chart 2 - Delete Pvc
        final WorkflowServiceEventMessage secondChartStage2 = getWfsEventMessage(secondHelmReleaseNameFor(releaseName),
                                                                                 WorkflowServiceEventStatus.COMPLETED,
                                                                                 afterRollbackRequestOperationOccId,
                                                                                 WorkflowServiceEventType.DELETE_PVC);
        messageHelper.sendMessageForChart(secondChartStage2,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Fourth Command: Chart 1 - Rollback
        final HelmReleaseLifecycleMessage firstChartStage2 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                            HelmReleaseState.COMPLETED,
                                                                                            afterRollbackRequestOperationOccId,
                                                                                            HelmReleaseOperationType.ROLLBACK,
                                                                                            "1");
        messageHelper.sendMessageForChart(firstChartStage2,
                                          firstHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        // Fifth Command: Chart 2 - Install/Instantiate
        final HelmReleaseLifecycleMessage secondChartStage3 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                             HelmReleaseState.FAILED,
                                                                                             afterRollbackRequestOperationOccId,
                                                                                             HelmReleaseOperationType.INSTANTIATE,
                                                                                             null);
        secondChartStage3.setMessage("Instantiate operation failed.");
        testingMessageSender.sendMessage(secondChartStage3);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(afterRollbackRequestOperationOccId, LifecycleOperationState.FAILED));
        assertThat(lifecycleOperationRepository.findByOperationOccurrenceId(afterRollbackRequestOperationOccId).getError())
                .contains("Instantiate operation failed.");
        assertRollbackOperationProperlyFailed(afterRollbackRequestOperationOccId);
    }

    @Test
    public void testRollbackFailedOnFirstRollbackPatternCommand() throws Exception {
        //Create Identifier
        final String releaseName = "rel-for-rb-failure-1";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        final String[] rollbackPattern = afterRollbackRequest.getRollbackPattern().split(",");
        assertThat(rollbackPattern).hasSize(2);
        assertThat(rollbackPattern[0]).contains("rollback");
        assertThat(rollbackPattern[1]).contains("rollback");

        //Fail operation on the first command of the rollback pattern
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId,
                                                                            HelmReleaseOperationType.ROLLBACK, "2");
        failed.setMessage("Rollback stage failed");
        testingMessageSender.sendMessage(failed);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(failed.getLifecycleOperationId(), LifecycleOperationState.FAILED));
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
    }

    @Test
    public void testRollbackFailedOnSecondRollbackPatternCommand() throws Exception {
        //Create Identifier
        final String releaseName = "rel-for-rb-failure-2";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before Rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);

        final String[] rollbackPattern = afterRollbackRequest.getRollbackPattern().split(",");
        assertThat(rollbackPattern).hasSize(2);
        assertThat(rollbackPattern[0]).contains("rollback");
        assertThat(rollbackPattern[1]).contains("rollback");

        // Complete the first command of the rollback pattern
        final HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                                     lifeCycleOperationId,
                                                                                     HelmReleaseOperationType.ROLLBACK, "3");
        messageHelper.sendMessageForChart(completed,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          true,
                                          HelmReleaseState.COMPLETED);

        //Fail on the second command of the rollback pattern
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId,
                                                                            HelmReleaseOperationType.ROLLBACK, "3");
        failed.setMessage("Helm/kubectl command failed");
        testingMessageSender.sendMessage(failed);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(failed.getLifecycleOperationId(), LifecycleOperationState.FAILED));
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
    }

    @Test
    public void testRollbackFailedOnRollbackFirstChart() throws Exception {
        //Create Identifier
        final String releaseName = "rel-for-rb-failure-3";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before Rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        final String[] rollbackPattern = afterRollbackRequest.getRollbackPattern().split(",");
        assertThat(rollbackPattern).hasSize(2);
        assertThat(rollbackPattern[0]).contains("rollback");
        assertThat(rollbackPattern[1]).contains("rollback");
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));

        //Fail operation of the first command for rollback pattern
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId,
                                                                            HelmReleaseOperationType.ROLLBACK, "3");
        failed.setMessage("Helm/kubectl command failed");
        testingMessageSender.sendMessage(failed);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(failed.getLifecycleOperationId(), LifecycleOperationState.FAILED));
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
    }

    @Test
    public void testRollbackFailedOnRollbackSecondChart() throws Exception {
        //Create Identifier
        final String releaseName = "rel-for-rb-failure-4";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before Rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation afterRollbackRequest = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(afterRollbackRequest.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        final String[] rollbackPattern = afterRollbackRequest.getRollbackPattern().split(",");
        assertThat(rollbackPattern).hasSize(2);
        assertThat(rollbackPattern[0]).contains("rollback");
        assertThat(rollbackPattern[1]).contains("rollback");
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));

        // Complete the first command of the rollback pattern
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.ROLLBACK, "4");
        testingMessageSender.sendMessage(completed);
        await().until(awaitHelper.helmChartInTempInstanceReachesState(secondHelmReleaseNameFor(releaseName),
                                                                      vnfInstanceResponse.getId(),
                                                                      HelmReleaseState.COMPLETED));

        //Fail on the second command of the rollback pattern
        HelmReleaseLifecycleMessage failed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName), HelmReleaseState.FAILED,
                                                                            lifeCycleOperationId,
                                                                            HelmReleaseOperationType.ROLLBACK, "4");

        failed.setMessage("Helm/kubectl command failed");
        testingMessageSender.sendMessage(failed);

        //Rollback operation should fail on any failed helm chart
        await().until(awaitHelper.operationReachesState(failed.getLifecycleOperationId(), LifecycleOperationState.FAILED));
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
    }

    @Test
    public void testRollbackFailedOnDataMigrationTimedOut() throws Exception {
        //Create Identifier and instantiate
        final String releaseName = "rel-for-rb-data-migration-timed-out";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);
        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before Rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        setExpiredApplicationTimeToPast(lifeCycleOperationId);
        mockFindProgressExpiredOperation(lifeCycleOperationId);

        //Rollback operation should fail by timeout if no response received from WFS
        checkApplicationTimeoutComponent.checkForApplicationOut();
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // Assert that operation was failed due to time out
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
        assertThat("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Lifecycle operation " +
                           lifeCycleOperationId + " failed due to timeout\",\"instance\":\"about:blank\"}")
                .isEqualTo(lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId).getError());
    }

    @Test
    public void testRollbackFailedOnRollbackTimedOut() throws Exception {
        //Create Identifier and instantiate
        final String releaseName = "rel-for-rb-rollback-timed-out";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK);

        instantiateVnfForIdentifier(vnfInstanceResponse, releaseName);

        //Make preparations before Rollback
        stepsHelper.setupPreRollbackOperationHistory(firstHelmReleaseNameFor(releaseName), vnfInstanceResponse);

        // Downgrade to previous version
        final MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                                E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);

        // Complete rollback of first command of pattern
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName), HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.ROLLBACK, "3");
        testingMessageSender.sendMessage(completed);
        await().until(awaitHelper.helmChartInTempInstanceReachesState(secondHelmReleaseNameFor(releaseName),
                                                                      vnfInstanceResponse.getId(),
                                                                      HelmReleaseState.COMPLETED));

        //Rollback operation should fail by timeout if no response received from WFS for rollback phase
        setExpiredApplicationTimeToPast(lifeCycleOperationId);
        mockFindProgressExpiredOperation(lifeCycleOperationId);

        checkApplicationTimeoutComponent.checkForApplicationOut();
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // Assert that operation was failed due to time out
        assertRollbackOperationProperlyFailed(lifeCycleOperationId);
        assertThat("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Lifecycle operation "
                           + lifeCycleOperationId + " failed due to timeout\",\"instance\":\"about:blank\"}")
                .isEqualTo(lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId).getError());
    }

    private void instantiateVnfForIdentifier(final VnfInstanceResponse vnfCreateIdentifierResponse, final String releaseName) throws Exception {
        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfCreateIdentifierResponse, UUID.randomUUID().toString());
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName, HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE, "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfCreateIdentifierResponse.getId(), false, LifecycleOperationState.COMPLETED);

        // Check operation values and history records after instantiate
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Assertions on state of instance after successful instantiate
        verificationHelper.verifyOperationAndModel(vnfCreateIdentifierResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.INSTANTIATE,
                                                   InstantiationState.INSTANTIATED);
    }

    private void assertRollbackOperationProperlyFailed(final String lifeCycleOperationId) {
        //Assert that it was a DOWNGRADE operation
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsRepository.findById(lifeCycleOperationId);
        if (changePackageOperationDetails.isPresent()) {
            assertThat(changePackageOperationDetails.get().getChangePackageOperationSubtype()
                               .equals(ChangePackageOperationSubtype.DOWNGRADE)).isTrue();
        } else {
            fail("missing changePackageOperationDetails");
        }

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance instance = operation.getVnfInstance();

        assertThat(instance.getTempInstance()).isNull();

        // Check operation values and history records have not been updated as Rollback failed
        verificationHelper.checkOperationValuesAndHistoryRecordsNotSet(lifeCycleOperationId);
    }

    private void setExpiredApplicationTimeToPast(final String lifeCycleOperationId) {
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().minusSeconds(OPERATION_TIME_EXCEED_DELTA_SECONDS));
        lifecycleOperationRepository.save(lifecycleOperation);
    }

    /**
     * Mocks the original method of repository to be used by {@link CheckApplicationTimeout}.
     * Required to make tests independent and avoid cross-test side effects.
     *
     * @param operationId id of operation to be failed by timeout
     */
    private void mockFindProgressExpiredOperation(String operationId) {
        Mockito.when(lifecycleOperationRepository.findProgressExpiredOperation(any(), any()))
                .thenReturn(Collections.singletonList(operationId));
    }
}
