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
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.TERMINATE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;


@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
@DirtiesContext
public class EndToEndTerminateWithGrantingTest extends AbstractEndToEndTest {

    private static final String DB_VNF_ID_1 = "g3def1ce-4cf4-477c-aab3-21c454e6a393";
    private static final String DB_VNF_ID_2 = "g3def1ce-4cf4-477c-aab3-21c454e6a394";
    private static final String DB_VNF_ID_3 = "g3def1ce-4cf4-477c-aab3-21c454e6a395";
    private static final String VNFD_ID = "single-chart-527c-arel4-5fcb086597zs";
    private static final String PKG_ID = "d3def1ce-4cf4-477c-aab3-pkgId4e6a379";
    private static final int TIME_OUT = 360;

    @Autowired
    @Qualifier("nfvoMockServer")
    private WireMockServer wireMockServer;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void before() throws Exception {
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, NFVO_TOKEN_PARAM, NFVO_TOKEN);
        GrantingTestUtils.stubHealthCheck(wireMockServer);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, PKG_ID);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, PKG_ID);
    }

    @Test
    public void successfulTerminateRequestWithGranting() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForTerminate(DB_VNF_ID_1, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        String jsonString = createTerminateVnfRequestBody();
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_1);
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(DB_VNF_ID_1);
        assertThat(byVnfId).isEmpty();

        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_1, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));

        GrantingTestUtils.verifyFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_1);

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(lifecycleOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);

        byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(DB_VNF_ID_1);
        assertThat(byVnfId.get().isDeletionInProgress()).isTrue();
    }

    @Test
    public void failedTerminateRequestWithGrantingWithForbiddenResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForTerminate(DB_VNF_ID_2, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);
        String jsonString = createTerminateVnfRequestBody();
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_2, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_2, PKG_ID);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
    }

    @Test
    public void failedTerminateRequestWithGrantingWithServiceUnavailableResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForTerminate(DB_VNF_ID_3, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);
        String jsonString = createTerminateVnfRequestBody();
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_3, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForServiceUnavailableResponse(DB_VNF_ID_3);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
    }

    private String createTerminateVnfRequestBody() throws JsonProcessingException {
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.GRACEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        additionalParams.put(APPLICATION_TIME_OUT, TIME_OUT);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }
}
