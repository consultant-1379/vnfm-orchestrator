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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN;
import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN_PARAM;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.ENABLED_MODULE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.INSTANTIATE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
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


@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
public class EndToEndInstantiateWithDeployableModulesAndGrantingTest extends AbstractEndToEndTest {

    private static final String VNFD_ID_DEPLOYABLE_MODULES = "d3def1ce-4cf4-477c-aab3-21cb04e6a390";
    private static final String PKG_ID_DEPLOYABLE_MODULES = "d3def1ce-4cf4-477c-aab3-pkgId4e6a390";
    private static final String DB_VNF_CLUSTER_NAME = "my-cluster";
    private static final int TIME_OUT = 25;

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

        GrantingTestUtils.stubGettingVnfd(wireMockServer, PKG_ID_DEPLOYABLE_MODULES);
        GrantingTestUtils.stubGettingScalingMappingFileWithDeployableModules(wireMockServer, PKG_ID_DEPLOYABLE_MODULES);
        GrantingTestUtils.stubGettingPackageResponseByVnfdWithDeployableModules(wireMockServer, VNFD_ID_DEPLOYABLE_MODULES);
    }

    @Test
    public void successfulCreateIdentifierAndInstantiateRequestWithGrantingAndDefaultDeployableModules() throws Exception {
        final VnfInstanceResponse vnfInstanceResponse = requestHelper
                .executeCreateVnfRequest("vnf-instance-default-deployable-modules", VNFD_ID_DEPLOYABLE_MODULES);
        final String vnfInstanceId = vnfInstanceResponse.getId();

        String grantRequestBody = GrantingTestUtils
                .getGrantRequestBodyForInstantiateWithDefaultDeployableModules(vnfInstanceId, VNFD_ID_DEPLOYABLE_MODULES);

        final List<String> expectedInstantiatedHelmReleases =
                List.of("vnf-instance-default-deployable-modules-1",
                        "vnf-instance-default-deployable-modules-2",
                        "eric-sec-certm-crd-1", "eric-sec-certm-crd-2");

        executeAndAssertInstantiateWithGrantingAndDeployableModules(vnfInstanceId, grantRequestBody,
                                                                    new HashMap<>(), expectedInstantiatedHelmReleases);
    }

    @Test
    public void successfulCreateIdentifierAndInstantiateRequestWithGrantingAndUpdatedDeployableModules() throws Exception {
        final VnfInstanceResponse vnfInstanceResponse = requestHelper
                .executeCreateVnfRequest("vnf-instance-updated-deployable-modules", VNFD_ID_DEPLOYABLE_MODULES);
        final String vnfInstanceId = vnfInstanceResponse.getId();

        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForInstantiateWithUpdatedDeployableModules(vnfInstanceId,
                                                                                                                  VNFD_ID_DEPLOYABLE_MODULES);

        Map<String, String> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_cnf_3", ENABLED_MODULE);

        final List<String> expectedInstantiatedHelmReleases =
                List.of("vnf-instance-updated-deployable-modules-1", "vnf-instance-updated-deployable-modules-2",
                        "vnf-instance-updated-deployable-modules-3", "eric-sec-certm-crd-1", "eric-sec-certm-crd-2");

        executeAndAssertInstantiateWithGrantingAndDeployableModules(vnfInstanceId, grantRequestBody,
                                                                    deployableModules, expectedInstantiatedHelmReleases);
    }

    private void executeAndAssertInstantiateWithGrantingAndDeployableModules(String vnfInstanceId, String grantRequestBody,
                                                                             Map<String, String> deployableModules,
                                                                             List<String> expectedInstantiatedCharts) throws Exception {
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        String jsonString = createInstantiateVnfRequestBodyWithDeployableModules("namespace-deployable-modules", deployableModules);
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, vnfInstanceId);
        MvcResult result = requestHelper.makePostRequest(jsonString, vnfInstanceId, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        String operationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(operationId, LifecycleOperationState.PROCESSING));
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL))
                                      .withRequestBody(equalToJson(grantRequestBody, true, true)));

        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage("integration-testing-deployable-modules-1",
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             operationId,
                                                                             WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage, vnfInstanceId, HelmReleaseOperationType.INSTANTIATE, PROCESSING,
                                                            true, false);

        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage("integration-testing-deployable-modules-1",
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               operationId,
                                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                                               "1");
        messageHelper.sendCompleteInstantiateMessageForCnfCharts(completedLifecycleMessage, vnfInstanceId, COMPLETED, true);

        LifecycleOperation completedInstantiateOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(completedInstantiateOperation.getOperationState()).isEqualTo(COMPLETED);

        final VnfInstance instantiatedVnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        final List<String> instantiatedHelmReleases = instantiatedVnfInstance.getHelmCharts().stream()
                .filter(helmChart -> COMPLETED.name().equals(helmChart.getState()) && helmChart.isChartEnabled())
                .map(HelmChart::getReleaseName)
                .collect(Collectors.toList());
        final boolean isAllChartsCompleted = instantiatedVnfInstance.getHelmCharts().stream()
                        .allMatch(helmChart -> helmChart.getState().equals(COMPLETED.name()));
        assertThat(instantiatedHelmReleases).containsExactlyInAnyOrderElementsOf(expectedInstantiatedCharts);
        assertThat(isAllChartsCompleted).isTrue();
    }

    private String createInstantiateVnfRequestBodyWithDeployableModules(String namespace, Map<String, String> deployableModules) throws
            JsonProcessingException {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName(DB_VNF_CLUSTER_NAME);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, namespace);
        addTimeoutsToRequest(additionalParams);
        request.setAdditionalParams(additionalParams);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(DEPLOYABLE_MODULES, deployableModules);
        request.setExtensions(extensions);
        return mapper.writeValueAsString(request);
    }

    private static void addTimeoutsToRequest(final Map<String, Object> additionalParams) {
        additionalParams.put(APPLICATION_TIME_OUT, TIME_OUT);
    }
}
