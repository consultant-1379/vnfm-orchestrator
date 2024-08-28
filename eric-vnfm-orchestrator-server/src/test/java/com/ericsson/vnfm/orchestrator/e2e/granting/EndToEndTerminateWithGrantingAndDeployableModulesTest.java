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
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN;
import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN_PARAM;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.TERMINATE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
public class EndToEndTerminateWithGrantingAndDeployableModulesTest extends AbstractEndToEndTest {

    private static final String VNF_INSTANCE_ID = "g3def1ce-4cf4-477c-aab3-21c454e6a777";
    private static final String VNFD_ID_DEPLOYABLE_MODULES = "single-chart-527c-arel4-5fcb086597zs";
    private static final String PKG_ID_DEPLOYABLE_MODULES = "d3def1ce-4cf4-477c-aab3-pkgId4e6a379";
    private static final int TIME_OUT = 25;

    @Autowired
    @Qualifier("nfvoMockServer")
    private WireMockServer wireMockServer;

    @Value("${workflow.host}")
    private String workflowHost;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void prep() throws Exception {
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, NFVO_TOKEN_PARAM, NFVO_TOKEN);

        GrantingTestUtils.stubGettingVnfdWithDeployableModules(wireMockServer, PKG_ID_DEPLOYABLE_MODULES);
        GrantingTestUtils.stubGettingScalingMappingFileWithDeployableModules(wireMockServer, PKG_ID_DEPLOYABLE_MODULES);

    }

    @Test
    public void successfulTerminateRequestWithGrantingAndDefaultDeployableModules() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForTerminateWithDefaultDeployableModules(VNF_INSTANCE_ID,
                                                                                                                VNFD_ID_DEPLOYABLE_MODULES);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));
        String jsonString = createTerminateVnfRequestBody();
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, VNF_INSTANCE_ID);
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(VNF_INSTANCE_ID);
        assertThat(byVnfId).isEmpty();

        MvcResult result = requestHelper.makePostRequest(jsonString, VNF_INSTANCE_ID, TERMINATE);
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING));

        GrantingTestUtils.verifyFoundInOperationsInProgressTable(operationsInProgressRepository, VNF_INSTANCE_ID);

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifeCycleOperationId);

        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(lifecycleOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);

        byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(VNF_INSTANCE_ID);
        assertThat(byVnfId.get().isDeletionInProgress()).isTrue();

        long enabledChartsCount = vnfInstanceRepository.findByVnfInstanceId(VNF_INSTANCE_ID)
                .getHelmCharts().stream()
                .filter(HelmChartBaseEntity::isChartEnabled)
                .count();
        verify(workflowRoutingService, times(Math.toIntExact(enabledChartsCount)))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), any());
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
