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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.logging.LoggingTestConfig.WORKFLOW_RESOURCES_PATH;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_TYPE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.VNF_INSTANCE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.WORKFLOW_UNAVAILABLE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
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
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.github.tomakehurst.wiremock.WireMockServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({LoggingTestConfig.class})
public class LoggingTest extends AbstractEndToEndTest {

    private static final String VNF_INSTANCE_ID = "e3def1ce-4cf4-477c-aab3-21c454e6a389";

    private static final String OCCURRENCE_ID = "m08fcbc8-474f-4673-91ee-761fd83991e6";

    private static final String RELEASE_NAME = "my-release-name";

    private static final String NAMESPACE_VALUE = "test-innulic-4";

    private static final String LIFECYCLE_OPERATION_TYPE = "INSTANTIATE";

    private static final String INSTANTIATE_VNF_REQUEST = "{" +
            "\"clusterName\":\"config\"," +
            " \"additionalParams\":{" +
            "   \"namespace\":\"" + NAMESPACE_VALUE + "\"," +
            "   \"cleanUpResources\":true," +
            "   \"applicationTimeOut\":3600" +
            "   }" +
            "}";
    private static final String DESCRIPTOR_MODEL_FILE_NAME = "descriptorModel.json";

    private static final ClusterConfigFile CLUSTER_CONFIG_FILE;

    private static final String EVNFM_INSTANTIATE_URL_FORMAT = "http://localhost:%d/vnflcm/v1/vnf_instances/%s/instantiate";

    private static final String ILOGGING_EVENT_DURING_INSTANTIATION = "Operation Instantiate VNF Instance with VNF Instance ID";

    private static final String ILOGGING_EVENT_DURING_PERSISTING_OPERATION = "Persisting operation after execution";

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger("com.ericsson.vnfm.orchestrator");

    private InMemoryAppender inMemoryAppender;

    @Autowired
    private TestRestTemplate restTemplate;

    @SpyBean
    WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    @Autowired
    private WireMockServer mockServer;

    @LocalServerPort
    private int port;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

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

    @SpyBean
    private InstantiateRequestHandler instantiateRequestHandler;

    static {
        CLUSTER_CONFIG_FILE = new ClusterConfigFile();
        CLUSTER_CONFIG_FILE.setName("config");
        CLUSTER_CONFIG_FILE.setContent("This is not a joke!");
    }

    @BeforeEach
    public void init() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        ReflectionTestUtils.setField(workflowRoutingServicePassThrough, "workflowHost", "localhost:" + mockServer.port());

        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        LOGGER.addAppender(inMemoryAppender);
        LOGGER.setLevel(Level.DEBUG);
        inMemoryAppender.start();
    }

    @AfterEach
    public void reset() {
        mockServer.resetToDefaultMappings();
        inMemoryAppender.reset();
    }

    @Test
    public void testSuccessOperationDoesNotHaveErrorLevelLogs() throws IOException, URISyntaxException {
        //given
        String descriptorModel = getFile(DESCRIPTOR_MODEL_FILE_NAME);
        JSONObject jsonObject = new JSONObject(descriptorModel);
        when(packageService.getPackageInfoWithDescriptorModel(Mockito.any())).thenReturn(createPackageResponse());
        when(packageService.getVnfd(any())).thenReturn(jsonObject);
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());
        doNothing().when(replicaDetailsService).setReplicaDetailsToVnfInstance(any(), any());
        when(clusterConfigService.getConfigFileByName(anyString())).thenReturn(CLUSTER_CONFIG_FILE);
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(CLUSTER_CONFIG_FILE);
        when(databaseInteractionService.getVnfInstance(VNF_INSTANCE_ID)).thenReturn(vnfFromDb());
        when(lifeCycleManagementHelper.getApplicationTimeout(any())).thenCallRealMethod();
        doReturn(occurrenceFromDb()).when(databaseInteractionService).getLifecycleOperation(ArgumentMatchers.any());
        doReturn(lifecycleOperationInstantiate()).when(databaseInteractionService).persistLifecycleOperation(ArgumentMatchers.any());
        doReturn(lifecycleOperationInstantiate()).when(instantiateRequestHandler).persistOperation(any(), any(), any(), any(), any(), any());
        mockWorkflowCreateResourceSuccess(VNF_INSTANCE_ID);

        //when
        ResponseEntity<Void> response = this.restTemplate.postForEntity(
                format(EVNFM_INSTANTIATE_URL_FORMAT, port, VNF_INSTANCE_ID),
                createInstantiateRequest(),
                Void.class);
        //then

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(databaseInteractionService, timeout(2000).times(1)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), any(LifecycleOperation.class));

        Map<String,String> lastMdcMap = inMemoryAppender.getlastLoggingEvent().getMDCPropertyMap();
        Map<String,String> firstVnfInstantiateMdcMap = inMemoryAppender.searchMdcMap(ILOGGING_EVENT_DURING_INSTANTIATION, Level.INFO);

        assertThat(lastMdcMap.get(VNF_INSTANCE_KEY)).isEqualTo(VNF_INSTANCE_ID);
        assertThat(lastMdcMap.get(LIFECYCLE_OPERATION_TYPE_KEY)).isEqualTo(LIFECYCLE_OPERATION_TYPE);
        assertThat(lastMdcMap.get(LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY)).isEqualTo(OCCURRENCE_ID);
        assertThat(firstVnfInstantiateMdcMap.get(VNF_INSTANCE_KEY)).isEqualTo(VNF_INSTANCE_ID);
        assertThat(firstVnfInstantiateMdcMap.get(LIFECYCLE_OPERATION_TYPE_KEY)).isEqualTo(LIFECYCLE_OPERATION_TYPE);
        assertThat(inMemoryAppender.countEventsWithLevel(Level.ERROR)).isZero();
        assertThat(inMemoryAppender.search(ILOGGING_EVENT_DURING_INSTANTIATION, Level.INFO)).isNotEmpty();
        assertThat(inMemoryAppender.search(ILOGGING_EVENT_DURING_PERSISTING_OPERATION, Level.INFO)).isNotEmpty();
    }

    @Test
    public void testFailureOperationDoesHaveErrorLevelLogs() throws IOException, URISyntaxException {
        //given
        String descriptorModel = getFile(DESCRIPTOR_MODEL_FILE_NAME);
        JSONObject descriptorModelJson = new JSONObject(descriptorModel);
        when(packageService.getPackageInfoWithDescriptorModel(Mockito.any())).thenReturn(createPackageResponse());
        when(packageService.getVnfd(any())).thenReturn(descriptorModelJson);
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());
        doNothing().when(replicaDetailsService).setReplicaDetailsToVnfInstance(any(), any());
        when(clusterConfigService.getConfigFileByName(anyString())).thenReturn(CLUSTER_CONFIG_FILE);
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(CLUSTER_CONFIG_FILE);
        when(databaseInteractionService.getVnfInstance(VNF_INSTANCE_ID)).thenReturn(vnfFromDb());
        when(lifeCycleManagementHelper.getApplicationTimeout(any())).thenCallRealMethod();
        doReturn(occurrenceFromDb()).when(databaseInteractionService).getLifecycleOperation(ArgumentMatchers.any());
        doReturn(lifecycleOperationInstantiate()).when(databaseInteractionService).persistLifecycleOperation(ArgumentMatchers.any());
        doAnswer(invocation -> {
            final LifecycleOperation argument = invocation.getArgument(0, LifecycleOperation.class);
            argument.setOperationOccurrenceId(OCCURRENCE_ID);
            return argument;
        }).when(lifeCycleManagementHelper).persistLifecycleOperationInProgress(any(), any(), any());
        mockWorkflowCreateResourceWfsBadRequest();

        //when
        ResponseEntity<Void> response = this.restTemplate.postForEntity(
                format(EVNFM_INSTANTIATE_URL_FORMAT, port, VNF_INSTANCE_ID),
                createInstantiateRequest(),
                Void.class);
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(databaseInteractionService, timeout(2000).times(1)).persistVnfInstanceAndOperation(
                any(VnfInstance.class), any(LifecycleOperation.class));

        Map<String,String> lastMdcMap = inMemoryAppender.getlastLoggingEvent().getMDCPropertyMap();
        Map<String,String> firstVnfInstantiateMdcMap = inMemoryAppender.searchMdcMap(ILOGGING_EVENT_DURING_INSTANTIATION, Level.INFO);

        assertThat(lastMdcMap.get(VNF_INSTANCE_KEY)).isEqualTo(VNF_INSTANCE_ID);
        assertThat(lastMdcMap.get(LIFECYCLE_OPERATION_TYPE_KEY)).isEqualTo(LIFECYCLE_OPERATION_TYPE);
        assertThat(lastMdcMap.get(LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY)).isEqualTo(OCCURRENCE_ID);
        assertThat(firstVnfInstantiateMdcMap.get(VNF_INSTANCE_KEY)).isEqualTo(VNF_INSTANCE_ID);
        assertThat(firstVnfInstantiateMdcMap.get(LIFECYCLE_OPERATION_TYPE_KEY)).isEqualTo(LIFECYCLE_OPERATION_TYPE);
        assertThat(inMemoryAppender.countEventsWithLevel(Level.ERROR)).isNotZero();
        assertThat(inMemoryAppender.search("Updating operation state to FAILED", Level.INFO)).isNotEmpty();
        assertThat(inMemoryAppender.search("Persisting operation after execution", Level.INFO)).isNotEmpty();
    }

    private void mockWorkflowCreateResourceSuccess(String vnfInstanceId) {
        String expectedResourceResponse = format("{\"instanceId\":\"%s\"}", vnfInstanceId);
        mockWorkflowCreateResourceEndpoint(HttpStatus.OK, expectedResourceResponse);
    }

    private void mockWorkflowCreateResourceWfsBadRequest() {
        String expectedResourceResponse = format(
                "{\"httpStatus\":\"%1$s\",\"errorMessage\":\"%2$s\"}",
                HttpStatus.BAD_REQUEST, WORKFLOW_UNAVAILABLE);
        mockWorkflowCreateResourceEndpoint(HttpStatus.BAD_REQUEST, expectedResourceResponse);
    }

    private void mockWorkflowCreateResourceEndpoint(HttpStatus status, String expectedResponse) {
        mockServer.stubFor(post(urlPathEqualTo(format(WORKFLOW_RESOURCES_PATH, RELEASE_NAME))).willReturn(
                aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(status.value())
                        .withBody(expectedResponse)));
    }

    private LifecycleOperation lifecycleOperationInstantiate() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(OCCURRENCE_ID);
        mockLifecycleOperation.setLifecycleOperationType(INSTANTIATE);
        return mockLifecycleOperation;
    }

    private VnfInstance vnfFromDb() {
        VnfInstance mockVnfFromDb = new VnfInstance();
        mockVnfFromDb.setOssTopology("{}");
        mockVnfFromDb.setVnfInstanceName(RELEASE_NAME);
        mockVnfFromDb.setHelmCharts(getHelmCharts(mockVnfFromDb));
        mockVnfFromDb.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        mockVnfFromDb.setOperationOccurrenceId(OCCURRENCE_ID);
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

    private LifecycleOperation occurrenceFromDb() {
        LifecycleOperation mockLifeCycleOperation = new LifecycleOperation();
        mockLifeCycleOperation.setOperationOccurrenceId(OCCURRENCE_ID);
        mockLifeCycleOperation.setOperationState(LifecycleOperationState.PROCESSING);
        return mockLifeCycleOperation;
    }

    private HttpEntity<String> createInstantiateRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
        return new HttpEntity<>(INSTANTIATE_VNF_REQUEST, headers);
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
