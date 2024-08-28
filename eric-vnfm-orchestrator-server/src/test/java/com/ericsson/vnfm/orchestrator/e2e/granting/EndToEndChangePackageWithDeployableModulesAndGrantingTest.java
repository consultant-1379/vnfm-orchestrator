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
package com.ericsson.vnfm.orchestrator.e2e.granting;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN;
import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN_PARAM;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedTerminatedHelmCharts;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedTerminatedHelmChartsPersistDMConfigTrue;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedTerminatedHelmChartsWithMandatoryCharts;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedUpgradedHelmChartsForDefaultDeployableModules;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedUpgradedHelmChartsForDefaultDeployableModulesAndMandatoryCharts;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedUpgradedHelmChartsForPersistDMConfigTrue;
import static com.ericsson.vnfm.orchestrator.e2e.granting.testData.HelmChartsTestData.createExpectedUpgradedHelmChartsForRequestDeployableModules;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.ENABLED_MODULE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_DM_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CHANGE_VNFPKG;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
@Slf4j
public class EndToEndChangePackageWithDeployableModulesAndGrantingTest extends AbstractEndToEndTest {
    private static final String DB_VNF_ID_1 = "186dc69a-0c2f-11ed-861d-0242ac120dm1";
    private static final String DB_VNF_ID_2 = "186dc69a-0c2f-11ed-861d-0242ac120dm2";
    private static final String DB_VNF_ID_3 = "186dc69a-0c2f-11ed-861d-0242ac120dm3";
    private static final String DB_VNF_ID_4 = "186dc69a-0c2f-11ed-861d-0242ac120dm4";

    private static final String SOURCE_VNFD_ID = "single-chart-527c-arel4-5fcsourcedm1";
    private static final String SOURCE_PACKAGE_ID_1 = "43bf1225-81e1-46b4-rel41-cadsourcedm1";

    private static final String TARGET_VNFD_ID_1 = "single-chart-527c-arel4-5fbtargetdm1";
    private static final String TARGET_PACKAGE_ID_1 = "43bf1225-81e1-46b4-rel41-cadtargetdm1";
    private static final String TARGET_VNFD_ID_2 = "single-chart-527c-arel4-5fbtargetdm2";
    private static final String TARGET_PACKAGE_ID_2 = "43bf1225-81e1-46b4-rel41-cadtargetdm2";
    private static final String TARGET_VNFD_ID_3 = "single-chart-527c-arel4-5fbtargetdm3";
    private static final String TARGET_PACKAGE_ID_3 = "43bf1225-81e1-46b4-rel41-cadtargetdm3";
    private static final String TARGET_VNFD_ID_4 = "single-chart-527c-arel4-5fbtargetdm4";
    private static final String TARGET_PACKAGE_ID_4 = "43bf1225-81e1-46b4-rel41-cadtargetdm4";

    @Autowired
    @Qualifier("nfvoMockServer")
    private WireMockServer wireMockServer;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void prep() throws Exception {
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, NFVO_TOKEN_PARAM, NFVO_TOKEN);

        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, TARGET_VNFD_ID_1);
        GrantingTestUtils.stubGettingVnfd(wireMockServer, TARGET_PACKAGE_ID_1);
        GrantingTestUtils.stubGettingScalingMappingFileWithDeployableModules(wireMockServer, TARGET_PACKAGE_ID_1);

        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, TARGET_VNFD_ID_2);
        GrantingTestUtils.stubGettingVnfd(wireMockServer, TARGET_PACKAGE_ID_2);
        GrantingTestUtils.stubGettingScalingMappingFileForPackage(wireMockServer, TARGET_PACKAGE_ID_2);

        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, TARGET_VNFD_ID_3);
        GrantingTestUtils.stubGettingVnfd(wireMockServer, TARGET_PACKAGE_ID_3);
        GrantingTestUtils.stubGettingScalingMappingFileForPackage(wireMockServer, TARGET_PACKAGE_ID_3);

        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, TARGET_VNFD_ID_4);
        GrantingTestUtils.stubGettingVnfd(wireMockServer, TARGET_PACKAGE_ID_4);
        GrantingTestUtils.stubGettingScalingMappingFileWithDeployableModules(wireMockServer, TARGET_PACKAGE_ID_4);

        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, SOURCE_VNFD_ID);
        GrantingTestUtils.stubGettingVnfd(wireMockServer, SOURCE_PACKAGE_ID_1);
        GrantingTestUtils.stubGettingScalingMappingFileWithDeployableModules(wireMockServer, SOURCE_PACKAGE_ID_1);
    }

    @Test
    public void successfulUpgradeRequestWithGrantingAndDefaultDeployableModules() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequest(DB_VNF_ID_1, SOURCE_VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        Map<String, String> deployableModules = Collections.emptyMap();
        String requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_1, deployableModules, false);

        final String lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_1, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL))
                                      .withRequestBody(equalToJson(grantRequestBody, true, true)));

        WorkflowServiceEventMessage completedCrdUpgradeMessage =
                getWfsEventMessage(lifecycleOperationId, WorkflowServiceEventType.CRD, WorkflowServiceEventStatus.COMPLETED, null);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedCrdUpgradeMessage, DB_VNF_ID_1, HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            PROCESSING, true, true);

        HelmReleaseLifecycleMessage completedCnfUpgradeMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completedCnfUpgradeMessage, DB_VNF_ID_1, PROCESSING);
        HelmReleaseLifecycleMessage completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.TERMINATE, "2");
        HelmReleaseLifecycleMessage completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.DELETE_PVC, "2");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       DB_VNF_ID_1,
                                                                       COMPLETED);

        final LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        final VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(DB_VNF_ID_1);
        final Map<String, HelmChart> expectedUpgradedHelmCharts = createExpectedUpgradedHelmChartsForDefaultDeployableModules();
        verifyUpgradedHelmCharts(vnfInstance, expectedUpgradedHelmCharts);
        final Map<String, HelmChart> expectedTerminatedCharts = createExpectedTerminatedHelmCharts();
        verifyTerminatedHelmCharts(vnfInstance, expectedTerminatedCharts);
    }

    @Test
    public void successfulUpgradeRequestWithGrantingAndRequestDeployableModulesAndDifferentDeployableModulesAndPackagesNames() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequest(DB_VNF_ID_2, SOURCE_VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        Map<String, String> deployableModules = Map.of("dm_cnf_6", ENABLED_MODULE);
        String requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_2, deployableModules, false);

        final String lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_2, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL))
                                      .withRequestBody(equalToJson(grantRequestBody, true, true)));

        WorkflowServiceEventMessage completedCrdUpgradeMessage =
                getWfsEventMessage(lifecycleOperationId, WorkflowServiceEventType.CRD, WorkflowServiceEventStatus.COMPLETED, null);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedCrdUpgradeMessage, DB_VNF_ID_2, HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            PROCESSING, true, true);

        HelmReleaseLifecycleMessage completedCnfUpgradeMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completedCnfUpgradeMessage, DB_VNF_ID_2, COMPLETED);

        final LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        final VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(DB_VNF_ID_2);
        final Map<String, HelmChart> expectedUpgradedHelmCharts = createExpectedUpgradedHelmChartsForRequestDeployableModules();
        verifyUpgradedHelmCharts(vnfInstance, expectedUpgradedHelmCharts);
    }

    @Test
    public void successfulUpgradeRequestWithGrantingAndDefaultDeployableModulesWithDifferentNumberHelmPackagesAndMandatoryCharts() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequest(DB_VNF_ID_3, SOURCE_VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        Map<String, String> deployableModules = Collections.emptyMap();
        String requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_3, deployableModules, false);

        final String lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_3, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL))
                                      .withRequestBody(equalToJson(grantRequestBody, true, true)));

        WorkflowServiceEventMessage completedCrdUpgradeMessage =
                getWfsEventMessage(lifecycleOperationId, WorkflowServiceEventType.CRD, WorkflowServiceEventStatus.COMPLETED, null);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedCrdUpgradeMessage, DB_VNF_ID_3, HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            PROCESSING, true, true);

        HelmReleaseLifecycleMessage completedCnfUpgradeMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completedCnfUpgradeMessage, DB_VNF_ID_3, PROCESSING);
        HelmReleaseLifecycleMessage completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.TERMINATE, "2");
        HelmReleaseLifecycleMessage completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.DELETE_PVC, "2");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       DB_VNF_ID_3,
                                                                       COMPLETED);

        final LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        final VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(DB_VNF_ID_3);
        final Map<String, HelmChart> expectedUpgradedHelmCharts = createExpectedUpgradedHelmChartsForDefaultDeployableModulesAndMandatoryCharts();
        verifyUpgradedHelmCharts(vnfInstance, expectedUpgradedHelmCharts);
        final Map<String, HelmChart> expectedTerminatedCharts = createExpectedTerminatedHelmChartsWithMandatoryCharts();
        verifyTerminatedHelmCharts(vnfInstance, expectedTerminatedCharts);
    }

    @Test
    public void successfulUpgradeAndDowngradeRequestWithGrantingDeployableModulesAndPersistDMConfigIsTrue() throws Exception {
        wireMockServer.stubFor(
                GrantingTestUtils.prepareAnyGrantingRequest().willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        // CCVP #1
        Map<String, String> deployableModules = Collections.emptyMap();
        String requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_4, deployableModules, true);

        String lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_4, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));

        sendWfsFakeCompletionMessagesForEveryChartForUpgradeAndDeletePatterns(lifecycleOperationId);

        // asserts
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        VnfInstance vnfInstanceAfterFirstCcvp = lifecycleOperation.getVnfInstance();
        Map<String, HelmChart> expectedUpgradedHelmCharts = createExpectedUpgradedHelmChartsForPersistDMConfigTrue();
        verifyUpgradedHelmCharts(vnfInstanceAfterFirstCcvp, expectedUpgradedHelmCharts);
        Map<String, HelmChart> expectedTerminatedCharts = createExpectedTerminatedHelmChartsPersistDMConfigTrue();
        verifyTerminatedHelmCharts(vnfInstanceAfterFirstCcvp, expectedTerminatedCharts);

        // CCVP #2
        requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_3, Collections.emptyMap(), true);

        lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_4, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));

        sendWfsFakeCompletionMessagesForEveryChartForUpgradePatterns(lifecycleOperationId);

        // asserts
        lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        // Downgrade
        requestBody = createChangeVnfPkgRequestBodyWithDeployableModules(TARGET_VNFD_ID_4, Collections.emptyMap(), true);

        lifecycleOperationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_4, requestBody);
        await().until(awaitHelper.operationReachesState(lifecycleOperationId, LifecycleOperationState.PROCESSING));

        sendWfsFakeCompletionMessagesForEveryChartForUpgradePatterns(lifecycleOperationId);

        // asserts (the same charts state as after CCVP #1)
        lifecycleOperation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(COMPLETED);

        VnfInstance vnfInstanceAfterDowngrade = lifecycleOperation.getVnfInstance();
        assertThat(getAllEnabledCharts(vnfInstanceAfterDowngrade))
                .containsExactlyInAnyOrderElementsOf(getAllEnabledCharts(vnfInstanceAfterFirstCcvp));
    }

    private List<String> getAllEnabledCharts(final VnfInstance vnfInstanceAfterDowngrade) {
        return vnfInstanceAfterDowngrade.getHelmCharts()
                .stream()
                .filter(HelmChartBaseEntity::isChartEnabled)
                .map(HelmChart::getHelmChartArtifactKey)
                .collect(Collectors.toList());
    }

    private void sendWfsFakeCompletionMessagesForEveryChartForUpgradeAndDeletePatterns(final String lifecycleOperationId)
    throws JsonProcessingException {
        WorkflowServiceEventMessage completedCrdUpgradeMessage = getWfsEventMessage(lifecycleOperationId,
                                                                                    WorkflowServiceEventType.CRD,
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    null);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedCrdUpgradeMessage, DB_VNF_ID_4, HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            PROCESSING, true, true);

        HelmReleaseLifecycleMessage completedCnfUpgradeMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completedCnfUpgradeMessage, DB_VNF_ID_4, PROCESSING);
        HelmReleaseLifecycleMessage completedCnfTerminateMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.TERMINATE, "2");
        HelmReleaseLifecycleMessage completedCnfDeletePvcMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.DELETE_PVC, "2");
        messageHelper.sendCompleteTerminateMessagesForUpgradeCnfCharts(completedCnfTerminateMessage,
                                                                       completedCnfDeletePvcMessage,
                                                                       DB_VNF_ID_4,
                                                                       COMPLETED);
    }

    private void sendWfsFakeCompletionMessagesForEveryChartForUpgradePatterns(final String lifecycleOperationId) throws JsonProcessingException {
        WorkflowServiceEventMessage completedCrdUpgradeMessage = getWfsEventMessage(lifecycleOperationId,
                                                                                    WorkflowServiceEventType.CRD,
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    null);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedCrdUpgradeMessage, DB_VNF_ID_4, HelmReleaseOperationType.CHANGE_VNFPKG,
                                                            PROCESSING, true, true);

        HelmReleaseLifecycleMessage completedCnfUpgradeMessage = getHelmReleaseLifecycleMessage(
                null, HelmReleaseState.COMPLETED, lifecycleOperationId, HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteUpgradeMessageForUpgradeCnfCharts(completedCnfUpgradeMessage, DB_VNF_ID_4, COMPLETED);
    }

    private void verifyUpgradedHelmCharts(VnfInstance vnfInstance, Map<String, HelmChart> expectedHelmCharts) {
        final List<HelmChart> helmCharts = vnfInstance.getHelmCharts();

        verifyUpgradedHelmCharts(helmCharts, expectedHelmCharts);
        verifyUpgradeInvocations(helmCharts);
    }

    private void verifyTerminatedHelmCharts(VnfInstance vnfInstance, Map<String, HelmChart> expectedHelmCharts) {
        final List<TerminatedHelmChart> disabledHelmCharts = vnfInstance.getTerminatedHelmCharts();

        verifyTerminatedHelmCharts(disabledHelmCharts, expectedHelmCharts);
        verifyTerminateInvocations(disabledHelmCharts);
        verifyDeletePvcInvocations(disabledHelmCharts);
    }

    private void verifyUpgradeInvocations(List<HelmChart> helmCharts) {
        final HelmChart helmChartWithHighestPriority = helmCharts.stream()
                .filter(HelmChartBaseEntity::isChartEnabled)
                .min(Comparator.comparing(HelmChart::getPriority))
                .orElseThrow();
        verify(workflowRoutingService, times((1))).routeChangePackageInfoRequest(any(ChangeOperationContext.class),
                                                                                 any(Path.class),
                                                                                 eq(helmChartWithHighestPriority.getPriority()));
        final List<HelmChart> enabledHelmCharts = helmCharts.stream()
                .filter(helmChart -> !helmChart.getId().equals(helmChartWithHighestPriority.getId()) && helmChart.isChartEnabled())
                .collect(Collectors.toList());
        enabledHelmCharts.forEach(enabledHelmChart -> verify(workflowRoutingService, times((1)))
                .routeChangePackageInfoRequest(eq(enabledHelmChart.getPriority()), any(LifecycleOperation.class), any(VnfInstance.class)));
    }

    private void verifyTerminateInvocations(List<TerminatedHelmChart> helmCharts) {
        helmCharts.forEach(disabledHelmChart -> verify(workflowRoutingService, times((1)))
                .routeTerminateRequest(any(VnfInstance.class),
                                       any(LifecycleOperation.class),
                                       eq(disabledHelmChart.getReleaseName())));
    }

    private void verifyDeletePvcInvocations(List<TerminatedHelmChart> helmCharts) {
        helmCharts.forEach(disabledHelmChart -> verify(workflowRoutingService, times((1)))
                .routeDeletePvcRequest(any(VnfInstance.class),
                                       eq(disabledHelmChart.getReleaseName()),
                                        any(),
                                        any()));
    }

    private void verifyUpgradedHelmCharts(List<HelmChart> actual, Map<String, HelmChart> expected) {
        assertThat(actual).hasSameSizeAs(expected.values());
        for (HelmChart actualHelmChart : actual) {
            final HelmChart expectedHelmChart = expected.get(actualHelmChart.getHelmChartName());
            assertThat(actualHelmChart.getHelmChartName()).isEqualTo(expectedHelmChart.getHelmChartName());
            assertThat(actualHelmChart.getHelmChartVersion()).isEqualTo(expectedHelmChart.getHelmChartVersion());
            assertThat(actualHelmChart.getHelmChartArtifactKey()).isEqualTo(expectedHelmChart.getHelmChartArtifactKey());
            assertThat(actualHelmChart.getHelmChartUrl()).isEqualTo(expectedHelmChart.getHelmChartUrl());
            assertThat(actualHelmChart.getPriority()).isEqualTo(expectedHelmChart.getPriority());
            assertThat(actualHelmChart.getReleaseName()).isEqualTo(expectedHelmChart.getReleaseName());
            assertThat(actualHelmChart.getState()).isEqualTo(expectedHelmChart.getState());
            assertThat(actualHelmChart.isChartEnabled()).isEqualTo(expectedHelmChart.isChartEnabled());
        }
    }

    private void verifyTerminatedHelmCharts(List<TerminatedHelmChart> actual, Map<String, HelmChart> expected) {
        assertThat(actual).hasSameSizeAs(expected.values());
        for (TerminatedHelmChart actualHelmChart : actual) {
            final HelmChart expectedHelmChart = expected.get(actualHelmChart.getHelmChartName());
            assertThat(actualHelmChart.getHelmChartName()).isEqualTo(expectedHelmChart.getHelmChartName());
            assertThat(actualHelmChart.getHelmChartVersion()).isEqualTo(expectedHelmChart.getHelmChartVersion());
            assertThat(actualHelmChart.getHelmChartArtifactKey()).isEqualTo(expectedHelmChart.getHelmChartArtifactKey());
            assertThat(actualHelmChart.getHelmChartUrl()).isEqualTo(expectedHelmChart.getHelmChartUrl());
            assertThat(actualHelmChart.getPriority()).isEqualTo(expectedHelmChart.getPriority());
            assertThat(actualHelmChart.getReleaseName()).isEqualTo(expectedHelmChart.getReleaseName());
            assertThat(actualHelmChart.getState()).isEqualTo(expectedHelmChart.getState());
        }
    }

    private String sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(String vnfId, String requestBody) throws Exception {
        final MvcResult result = requestHelper.makePostRequest(requestBody, vnfId, CHANGE_VNFPKG);

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().timeout(60, TimeUnit.SECONDS).until(
                awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING)
        );
        return lifeCycleOperationId;
    }

    private String createChangeVnfPkgRequestBodyWithDeployableModules(final String vnfdId, final Map<String, String> deployableModules,
                                                                      final boolean isPersistDmConfig) throws JsonProcessingException {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        final Map<Object, Object> additionalParams = new HashMap<>();
        additionalParams.put(IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY, true);
        additionalParams.put(PERSIST_SCALE_INFO, true);
        additionalParams.put(PERSIST_DM_CONFIG, isPersistDmConfig);
        request.vnfdId(vnfdId).additionalParams(additionalParams);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(DEPLOYABLE_MODULES, deployableModules);
        request.setExtensions(extensions);
        return mapper.writeValueAsString(request);
    }
}
