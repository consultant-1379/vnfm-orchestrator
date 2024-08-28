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
package com.ericsson.vnfm.orchestrator.logging;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({LoggingTestConfig.class})
public class AuthorizationHeaderForwardingTest extends AbstractDbSetupTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    OnboardingConfig onboardingConfig;

    // Have to declare these beans for creating applicationContext in test
    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private ScaleInfoRepository scaleInfoRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @MockBean
    private LifeCycleManagementService lifeCycleManagementService;

    @MockBean
    private ResourcesService resourcesService;

    @MockBean
    private HelmChartRepository helmChartRepository;

    @MockBean
    private VnfLcmOperationService vnfLcmOperationService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private BackupsServiceImpl backupsServiceImpl;

    @MockBean
    private RestoreBackupFromEnm restoreBackupFromEnm;

    @MockBean
    private EnrollmentInfoService enrollmentInfoService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ValuesFileComposer valuesFileComposer;

    @MockBean
    private ExtensionsService extensionsService;

    @BeforeEach
    public void init() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + mockServer.port());
    }

    @AfterEach
    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @Test
    public void forwardAuthorizationHeaderWhenItExistInIncomingRequest() throws JsonProcessingException {
        //given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJncWFMd1h0TG5UbldULXpJTW1KSm43OE15MWRESm8xWmwwSFplcThiUHdZIn0"
                +
                ".eyJqdGkiOiIwZjNmY2QzNC02YzhkLTQyM2EtYjBiMi1lMmFjMDZiODZkOGMiLCJleHAiOjE1NTg2OTc4NzIsIm5iZiI6MCwiaWF0IjoxNTU4Njk3NTcyLCJpc3MiOiJodHRwOi8vaWFtLmdlci50b2RkMDQxLnJuZC5naWMuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvQURQLUFwcGxpY2F0aW9uLU1hbmFnZXIiLCJhdWQiOiJlcmljc3Nvbi1hcHAiLCJzdWIiOiI2ZTNhNDViYy0zYmVkLTQwMGQtOWNlZi0yMmMyNDIyYjRhZWEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlcmljc3Nvbi1hcHAiLCJhdXRoX3RpbWUiOjE1NTg2OTc1NzIsInNlc3Npb25fc3RhdGUiOiJhYjkzMTVlZC01NDgyLTRlZjEtYWYzNi1hN2U3NzkzOWRhZWIiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIG9mZmxpbmVfYWNjZXNzIGVtYWlsIGFkZHJlc3MgcGhvbmUiLCJhZGRyZXNzIjp7fSwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiZnVuY1VzZXIgZnVuY1VzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJmdW5jdXNlciIsImdpdmVuX25hbWUiOiJmdW5jVXNlciIsImZhbWlseV9uYW1lIjoiZnVuY1VzZXIiLCJlbWFpbCI6ImZ1bmN1c2VyQGVyaWNzc29uLmNvbSJ9.JKyofwJUccHUIIWeNeSWyDsjJOOltMqn3ia1amb6RdJQCD6cZr4O1GPI6fgjDGfyC_rfayk9MReF5ZUmmwfVFLTXRKfWGUWMccmwRsFAaJy1Sl_29L_38kBeDGtOBByHHXylLN3F5wub0qjmr7VSQRnRcabtRrSf-9FfvHfYhDduCBJUPnkvSec8djbpfY3nSP-W5Wj9QT8BuTk-MtDwJ_D-JnFFBvguDNab_7ClJ96_TevyblCowfDFpEUaqKl-OWJO2_-3FiCrolqxFisFXhvGzFGWCwp7wExUGO7otTBPmko57IbMtpAqH3iJcBx_D8QZnNe1ndW8HXTCS-ik7g";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());

        CreateVnfRequest request = new CreateVnfRequest();
        String vnfdId = "test-id";
        request.setVnfdId(vnfdId);
        request.setVnfInstanceName("test-application");

        String mockServiceResponseInStr = objectMapper.writeValueAsString(Collections.emptyList());

        HttpEntity<CreateVnfRequest> entity = new HttpEntity<>(request, headers);

        mockServer.stubFor(get(urlPathEqualTo("/api/vnfpkgm/v1/vnf_packages"))
                                   .withQueryParam("filter", equalTo("(eq,vnfdId," + vnfdId + ")"))
                                   .withHeader(HttpHeaders.AUTHORIZATION, equalTo(token))
                                   .willReturn(aResponse()
                                                       .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                       .withStatus(200)
                                                       .withBody(mockServiceResponseInStr)
                                   )
        );
        mockServer.stubFor(get(urlPathEqualTo("/actuator/health")).willReturn(aResponse().withStatus(200)));
        //when
        this.restTemplate.postForEntity(
                "http://localhost:" + port + "/vnflcm/v1/vnf_instances",
                entity,
                String.class);

        //then
        mockServer.verify(getRequestedFor(urlPathEqualTo("/api/vnfpkgm/v1/vnf_packages"))
                                  .withQueryParam("filter", equalTo("(eq,vnfdId," + vnfdId + ")"))
                                  .withHeader(HttpHeaders.AUTHORIZATION, equalTo(token))
        );
    }
}
