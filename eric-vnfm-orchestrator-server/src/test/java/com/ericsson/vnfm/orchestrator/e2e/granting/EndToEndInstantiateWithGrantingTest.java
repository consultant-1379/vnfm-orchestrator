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
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.INSTANTIATE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import java.util.HashMap;
import java.util.Map;

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

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
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
public class EndToEndInstantiateWithGrantingTest extends AbstractEndToEndTest {

    private static final String DB_VNF_ID_1 = "g3def1ce-4cf4-477c-aab3-21c454e6a390";
    private static final String DB_VNF_ID_2 = "g3def1ce-4cf4-477c-aab3-21c454e6a391";
    private static final String DB_VNF_ID_3 = "g3def1ce-4cf4-477c-aab3-21c454e6a392";
    private static final String DB_VNF_ID_4 = "g3def1ce-4cf4-477c-aab3-21c454e6a399";
    private static final String DB_VNF_CLUSTER_NAME = "my-cluster";
    private static final String VNFD_ID = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
    private static final String PKG_ID = "d3def1ce-4cf4-477c-aab3-pkgId4e6a379";
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
        GrantingTestUtils.stubHealthCheck(wireMockServer);

        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, PKG_ID);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, PKG_ID);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, VNFD_ID);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, VNFD_ID);
    }

    @Test
    public void successfulInstantiateRequestWithGranting() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForInstantiate(DB_VNF_ID_1, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        String jsonString = createInstantiateVnfRequestBody("my-namespace-grant-1");
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_1);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_1, INSTANTIATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        String operationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(operationId, LifecycleOperationState.PROCESSING));

        // Fake CRD upgrade completion message
        WorkflowServiceEventMessage completedUpgradeWfsMessage = getWfsEventMessage("integration-testing-granting-1",
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    operationId,
                                                                                    WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedUpgradeWfsMessage, DB_VNF_ID_1, HelmReleaseOperationType.INSTANTIATE, COMPLETED,
                                                            true, false);

        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage("integration-testing-granting-1",
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               operationId,
                                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedLifecycleMessage, DB_VNF_ID_1, false, COMPLETED);

        LifecycleOperation completedChangeVnfPkgOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(completedChangeVnfPkgOperation.getOperationState()).isEqualTo(COMPLETED);
    }

    @Test
    public void successfulInstantiateRequestWithGrantingWithScalingMappingAndValuesFile() throws Exception {
        String valuesFile = TestUtils.readDataFromFile("valueFiles/test-values-for-granting.yaml");

        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForInstantiate(DB_VNF_ID_4, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        String jsonString = createInstantiateVnfRequestBody("my-namespace-grant-values-file");
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_4);

        MvcResult result = requestHelper.makePostRequestWithFile("instantiateVnfRequest", jsonString, valuesFile, DB_VNF_ID_4, INSTANTIATE);

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        String operationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(operationId, LifecycleOperationState.PROCESSING));

        // Fake CRD upgrade completion message
        WorkflowServiceEventMessage completedUpgradeWfsMessage = getWfsEventMessage("integration-testing-granting-1",
                                                                                    WorkflowServiceEventStatus.COMPLETED,
                                                                                    operationId,
                                                                                    WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedUpgradeWfsMessage, DB_VNF_ID_4, HelmReleaseOperationType.INSTANTIATE, COMPLETED,
                                                            true, false);

        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage("integration-testing-granting-1",
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               operationId,
                                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedLifecycleMessage, DB_VNF_ID_4, false, COMPLETED);

        LifecycleOperation completedChangeVnfPkgOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(completedChangeVnfPkgOperation.getOperationState()).isEqualTo(COMPLETED);
    }

    @Test
    public void failedInstantiateRequestWithGrantingWithForbiddenResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForInstantiate(DB_VNF_ID_2, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);
        String jsonString = createInstantiateVnfRequestBody("my-namespace-grant-2");
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_2, INSTANTIATE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_2, PKG_ID);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
    }

    @Test
    public void failedInstantiateRequestWithGrantingWithServiceUnavailableResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForInstantiate(DB_VNF_ID_3, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);
        String jsonString = createInstantiateVnfRequestBody("my-namespace-grant-3");
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_3, INSTANTIATE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForServiceUnavailableResponse(DB_VNF_ID_3);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
    }

    private String createInstantiateVnfRequestBody(String namespace) throws JsonProcessingException {
        InstantiateVnfRequest request = createInstantiateVnfRequest(namespace);
        return mapper.writeValueAsString(request);
    }

    private InstantiateVnfRequest createInstantiateVnfRequest(String namespace) {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName(DB_VNF_CLUSTER_NAME);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, namespace);
        addTimeoutsToRequest(additionalParams);
        request.setAdditionalParams(additionalParams);
        return request;
    }

    private static void addTimeoutsToRequest(final Map<String, Object> additionalParams) {
        additionalParams.put(APPLICATION_TIME_OUT, TIME_OUT);
    }
}
