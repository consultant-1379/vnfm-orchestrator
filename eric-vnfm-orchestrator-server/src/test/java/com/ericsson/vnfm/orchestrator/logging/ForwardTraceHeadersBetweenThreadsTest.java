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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.logging.LoggingTestConfig.WORKFLOW_RESOURCES_PATH;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({LoggingTestConfig.class})
public class ForwardTraceHeadersBetweenThreadsTest extends AbstractEndToEndTest {

    private static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER_NAME = "X-B3-SpanId";
    private static final String B3_HEADER_NAME = "b3";
    private static final String INSTANCE_ID_QUERY_PARAM_NAME = "instanceId";
    private static final String WORKFLOW_RESOURCES_HISTORY = "/api/lcm/v2/resources";
    private static final String RELEASE_NAME = "my-release-name";

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer mockServer;

    @SpyBean
    WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    private HelmChartHistoryRepository HelmChartHistoryRepository;

    @MockBean
    private ScaleService scaleService;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private VnfLcmOperationService vnfLcmOperationService;

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @MockBean
    private MessageUtility messageUtility;

    @MockBean
    private BackupsServiceImpl backupsServiceImpl;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private RestoreBackupFromEnm restoreBackupFromEnm;

    @MockBean
    private EnrollmentInfoService enrollmentInfoService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ExtensionsService extensionsService;

    @MockBean
    private ReplicaDetailsService replicaDetailsService;

    @MockBean
    private HelmChartHelper helmChartHelper;

    @MockBean
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @BeforeEach
    public void init() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        ReflectionTestUtils.setField(workflowRoutingServicePassThrough, "workflowHost", "localhost:" + mockServer.port());
    }

    @AfterEach
    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @Test
    @Timeout(30)
    public void forwardHeadersFromInstantiateRequest() throws IOException, URISyntaxException {
        //given
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String occurenceId = "m08fcbc8-474f-4673-91ee-761fd83991e6";
        String expectedTraceId = "0f0479f0b560f9a0bec8984615b9e46e";
        String releaseName = "my-release-name";
        String namespace = "test-innulic-4";
        String historyResourceUrl = WORKFLOW_RESOURCES_HISTORY + "/" + releaseName;
        String instantiateVnfRequest = "{" +
                "\"clusterName\":\"config\"," +
                " \"additionalParams\":{" +
                "   \"namespace\":\"" + namespace + "\"," +
                "   \"cleanUpResources\":true," +
                "   \"applicationTimeOut\":3600" +
                "   }" +
                "}";
        String descriptorModel = getFile("descriptorModel.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        final ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("config");
        clusterConfigFile.setContent("This is not a joke!");
        when(packageService.getVnfd(any())).thenReturn(jsonObject);
        doNothing().when(replicaDetailsService).setReplicaDetailsToVnfInstance(any(), any());
        when(databaseInteractionService.getLifecycleOperation(ArgumentMatchers.any())).thenReturn(occurenceFromDb(occurenceId));
        when(databaseInteractionService.persistLifecycleOperation(ArgumentMatchers.any())).thenReturn(lifecycleOperationInstantiate());
        when(clusterConfigService.getConfigFileByName(ArgumentMatchers.anyString())).thenReturn(clusterConfigFile);
        when(clusterConfigService.getOrDefaultConfigFileByName(ArgumentMatchers.anyString())).thenReturn(clusterConfigFile);
        when(databaseInteractionService.getVnfInstance(vnfInstanceId)).thenReturn(vnfFromDb(releaseName));
        when(lifeCycleManagementHelper.getApplicationTimeout(any())).thenCallRealMethod();
        when(packageService.getPackageInfoWithDescriptorModel(Mockito.any())).thenReturn(createPackageResponse());
        when(packageService.getPackageInfo(Mockito.any())).thenReturn(new PackageResponse());
        doNothing().when(databaseInteractionService).deleteInstanceDetailsByVnfInstanceId(any());

        mockWorkflowCreateResourceEndpoint(vnfInstanceId, expectedTraceId);
        mockWorkflowGetHistoryOfResource(historyResourceUrl, expectedTraceId, vnfInstanceId, namespace);

        //when
        ResponseEntity<Void> response = this.restTemplate.postForEntity(
                "http://localhost:" + port + "/vnflcm/v1/vnf_instances/" + vnfInstanceId + "/instantiate",
                createInstantiateRequest(instantiateVnfRequest, expectedTraceId),
                Void.class);
        //then

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(databaseInteractionService, timeout(2000).times(1)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), any(LifecycleOperation.class));
        mockServer.verify(postRequestedFor(urlPathEqualTo(format(WORKFLOW_RESOURCES_PATH, RELEASE_NAME)))
                .withHeader(TRACE_ID_HEADER_NAME, equalTo(expectedTraceId))
        );
    }

    private void mockWorkflowGetHistoryOfResource(String historyResourceUrl, String expectedTraceId, String vnfInstanceId, String namespace) {
        String completedWorkFlowHistoryResponse = "{\n" +
                "  \"workflowQueries\": [\n" +
                "    {\n" +
                "      \"namespace\": \"" + namespace + "\",\n" +
                "      \"message\": \"no message\",\n" +
                "      \"workflowState\": \"COMPLETED\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String processingWorkFlowHistoryResponse = "{\n" +
                "  \"workflowQueries\": [\n" +
                "    {\n" +
                "      \"namespace\": \"" + namespace + "\",\n" +
                "      \"message\": \"no message\",\n" +
                "      \"workflowState\": \"PROCESSING\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        mockServer.stubFor(get(urlPathEqualTo(historyResourceUrl))
                .withHeader(TRACE_ID_HEADER_NAME, equalTo(expectedTraceId))
                .withQueryParam(INSTANCE_ID_QUERY_PARAM_NAME, equalTo(vnfInstanceId))
                .inScenario("test")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("COMPLETED")
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(processingWorkFlowHistoryResponse)
                ));

        mockServer.stubFor(get(urlPathEqualTo(historyResourceUrl))
                .withHeader(TRACE_ID_HEADER_NAME, equalTo(expectedTraceId))
                .withQueryParam(INSTANCE_ID_QUERY_PARAM_NAME, equalTo(vnfInstanceId))
                .inScenario("test")
                .whenScenarioStateIs("COMPLETED")
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(completedWorkFlowHistoryResponse)
                ));
    }

    private void mockWorkflowCreateResourceEndpoint(String vnfInstanceId, String expectedTraceId) {
        String expectedResourceResponse = format("{\"instanceId\":\"%s\"}", vnfInstanceId);
        mockServer.stubFor(post(urlPathEqualTo(format(WORKFLOW_RESOURCES_PATH, RELEASE_NAME)))
                .withHeader(TRACE_ID_HEADER_NAME, equalTo(expectedTraceId))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(expectedResourceResponse)
                )
        );
    }

    private LifecycleOperation lifecycleOperationInstantiate() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId("1");
        mockLifecycleOperation.setLifecycleOperationType(INSTANTIATE);
        return mockLifecycleOperation;
    }

    private VnfInstance vnfFromDb(String releaseName) {
        VnfInstance mockVnfFromDb = new VnfInstance();
        mockVnfFromDb.setOssTopology("{}");
        mockVnfFromDb.setVnfInstanceName(releaseName);
        mockVnfFromDb.setHelmCharts(getHelmCharts(mockVnfFromDb));
        mockVnfFromDb.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        mockVnfFromDb.setHelmCharts(getHelmCharts(new VnfInstance()));
        return mockVnfFromDb;
    }

    private List<HelmChart> getHelmCharts(final VnfInstance vnfInstance) {
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartUrl("http://test/test.tgz");
        helmChart.setPriority(1);
        helmChart.setVnfInstance(vnfInstance);
        helmChart.setReleaseName(RELEASE_NAME);
        List<HelmChart> charts = new ArrayList<>();
        charts.add(helmChart);
        return charts;
    }

    private LifecycleOperation occurenceFromDb(String id) {
        LifecycleOperation mockLifeCycleOperation = new LifecycleOperation();
        mockLifeCycleOperation.setOperationOccurrenceId(id);
        mockLifeCycleOperation.setOperationState(LifecycleOperationState.PROCESSING);
        return mockLifeCycleOperation;
    }

    private HttpEntity<String> createInstantiateRequest(String instantiateVnfRequest, String expectedTraceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TRACE_ID_HEADER_NAME, expectedTraceId);
        headers.set(SPAN_ID_HEADER_NAME, "9130b1c8064a2be8");
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
        headers.set(B3_HEADER_NAME,  "%s-bb25bfe5acc626fa-1".formatted(expectedTraceId));

        return new HttpEntity<>(instantiateVnfRequest, headers);
    }

    @NotNull
    private static String getFile(final String file) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(getResource(file)));
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "test-vnfd.json");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }
}
