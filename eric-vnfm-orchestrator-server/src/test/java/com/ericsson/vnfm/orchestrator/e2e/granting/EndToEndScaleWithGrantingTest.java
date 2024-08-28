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
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.SCALE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
public class EndToEndScaleWithGrantingTest extends AbstractEndToEndTest {

    private static final String DB_VNF_ID_1 = "g3def1ce-4cf4-477c-aab3-21c454e6a396";
    private static final String DB_VNF_ID_2 = "g3def1ce-4cf4-477c-aab3-21c454e6a397";
    private static final String DB_VNF_ID_3 = "g3def1ce-4cf4-477c-aab3-21c454e6a398";
    private static final String DB_VNF_ID_4 = "g3def1ce-4cf4-477c-aab3-21c454e6a400";
    private static final String VNFD_ID = "single-chart-527c-arel4-5fcb086597zs";
    private static final String REL3_VNFD_ID = "multi-chart-477c-aab3-2b04e6a363";
    private static final String PACKAGE_ID_FOR_VNFD = "43bf1225-81e1-46b4-rel41-cadea4432939";
    private static final String PKG_ID_1 = "d3def1ce-4cf4-477c-aab3-pkgId4e6a379";
    private static final String PKG_ID_2 = "d3def1ce-4cf4-477c-aab3-pkgId4e6a400";
    private static final int TIME_OUT = 25;

    @Autowired
    @Qualifier("nfvoMockServer")
    private WireMockServer wireMockServer;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void before() throws Exception {
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, NFVO_TOKEN_PARAM, NFVO_TOKEN);
        GrantingTestUtils.stubHealthCheck(wireMockServer);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, PKG_ID_1);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, PACKAGE_ID_FOR_VNFD);
        GrantingTestUtils.stubGettingRel3Vnfd(wireMockServer, PKG_ID_2);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, PKG_ID_1);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, VNFD_ID);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, REL3_VNFD_ID);
    }

    @Test
    public void successfulScaleRequestWithGranting() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForScale(DB_VNF_ID_1, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        String jsonString = createScaleVnfRequestBody();
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_1);

        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_1, SCALE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, PROCESSING));

        GrantingTestUtils.verifyFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_1);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(lifecycleOperation.getOperationState()).isEqualTo(PROCESSING);
        assertThat(lifecycleOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.SCALE);
    }

    @Test
    public void failedScaleRequestWithGrantingWithForbiddenResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForScale(DB_VNF_ID_2, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);
        String jsonString = createScaleVnfRequestBody();
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_2, SCALE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_2, PKG_ID_1);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
    }

    @Test
    public void failedScaleRequestWithGrantingWithServiceUnavailableResponseFromNfvo() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForScale(DB_VNF_ID_3, VNFD_ID);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);
        String jsonString = createScaleVnfRequestBody();
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_3, SCALE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_3);

        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForServiceUnavailableResponse(DB_VNF_ID_3);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);
    }

    @Test // Steps to reproduce issue SM-154323 and prevent it in future
    public void shouldNotThrowScaleNotSupportedWhenGrantingEnabledForRel3Package() throws Exception {
        // given NFVO enabled, rel3 package sent to scale
        // when request sent and ACCEPTED received
        String jsonString = createScaleVnfRequestBody();
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_4, SCALE);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);

        // then
        // Operation should not fail with state ROLLED_BACK
        // and exception "Operation SCALE is not supported for package..." should not occur
        await().atMost(5, TimeUnit.SECONDS).until(awaitHelper.operationReachesState(lifeCycleOperationId,
                                                                                    PROCESSING,
                                                                                    ROLLED_BACK));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState())
                .withFailMessage("Operation %1$s has never reached state %2$s", operation.getLifecycleOperationType(), PROCESSING)
                .isEqualTo(PROCESSING);
        assertThat(operation.getError()).isNull();
    }

    private String createScaleVnfRequestBody() throws JsonProcessingException {
        ScaleVnfRequest request = new ScaleVnfRequest();

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(APPLICATION_TIME_OUT, TIME_OUT);

        request.setType(ScaleVnfRequest.TypeEnum.OUT);
        request.setAspectId("Aspect1");
        request.setNumberOfSteps(2);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }
}
