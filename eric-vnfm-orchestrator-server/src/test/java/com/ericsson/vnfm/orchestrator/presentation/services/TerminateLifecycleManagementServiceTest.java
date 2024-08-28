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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChart;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.TerminateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(LifecycleManagementServiceTestConfig.class)
@MockBean(classes = {
        GrantingNotificationsConfig.class,
        LcmOpAdditionalParamsProcessor.class
})
public class TerminateLifecycleManagementServiceTest extends AbstractDbSetupTest {

    @SpyBean
    WorkflowRoutingServicePassThrough workflowRoutingService;
    @MockBean
    VnfLcmOperationService vnfLcmOperationService;
    @Autowired
    @SpyBean
    private LifeCycleManagementService lifeCycleManagementService;
    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private WireMockServer mockServer;
    @SpyBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean
    private OssNodeService ossNodeService;
    @SpyBean
    private TerminateRequestHandler terminateRequestHandler;

    @Captor
    private ArgumentCaptor<VnfInstance> vnfInstanceArgumentCaptor;

    @NotNull
    public static VnfInstance getVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfDescriptorId("someId");
        instance.setVnfProviderName("test");
        instance.setVnfProductName("dummy_name");
        instance.setVnfInstanceName("name");
        instance.setVnfInstanceDescription("dummy_description");
        instance.setVnfSoftwareVersion("1.0");
        instance.setVnfdVersion("1.0");
        instance.setVnfPackageId("123456");
        instance.setClusterName("my-cluster");
        instance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        return instance;
    }

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(workflowRoutingService, "workflowHost", "localhost:" + mockServer.port());
    }

    public void reset() {
        mockServer.resetToDefaultMappings();
    }

    @Test
    public void terminateAndDeleteNodeSuccessful() {
        doNothing().when(ossNodeService).deleteNodeFromENM(any(), eq(false));
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), any());
        doReturn("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1").when(databaseInteractionService).getClusterConfigServerByClusterName(any());

        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.TERMINATE, "4241e63e-334b-4ee9-aa5f-155507dfcfe8",
                                new TerminateVnfRequest(), null, null);
        await().until(deleteNodeIsFinished(operationId));
        verify(ossNodeService, times(1)).deleteNodeFromENM(any(), eq(false));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.isDeleteNodeFailed()).isFalse();
        assertThat(operation.getDeleteNodeErrorMessage()).isNull();
    }

    @Test
    public void terminateAndDeleteNodeFailure() {
        doThrow(new FileExecutionException("failure")).when(ossNodeService).deleteNodeFromENM(any(), eq(false));
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), any());
        doReturn("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1").when(databaseInteractionService).getClusterConfigServerByClusterName(any());

        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.TERMINATE, "2fe76a38-dca1-4f1a-97bc-11ffc353afbf",
                                new TerminateVnfRequest(), null, null);
        await().until(deleteNodeIsFinished(operationId));
        verify(ossNodeService, times(3)).deleteNodeFromENM(any(), eq(false));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.isDeleteNodeFailed()).isTrue();
        assertThat(operation.getDeleteNodeErrorMessage()).contains("failure");
    }

    @Test
    public void terminateWithNodeNotAddedToENM() {
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.ACCEPTED);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), any());
        doReturn("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1").when(databaseInteractionService).getClusterConfigServerByClusterName(any());

        String operationId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.TERMINATE, "5038f7b4-3514-4f8b-95ba-9c65a2393b08",
                                new TerminateVnfRequest(), null, null);
        verify(ossNodeService, times(0)).deleteNodeFromENM(any(), eq(false));
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(operation.isDeleteNodeFinished()).isFalse();
    }

    @Test
    public void terminateCustomPriority() {
        final String vnfInstanceId = "5038f7b4-3514-4f8b-95ba-9c65a2393b08";
        VnfInstance vnfInstance = new VnfInstance();
        final HelmChart helm_package1 = getHelmChart("helm.tgz", "helm_package1", vnfInstance, 1, HelmChartType.CNF);
        helm_package1.getOperationChartsPriority().put(LifecycleOperationType.TERMINATE, 2);
        final HelmChart helm_package2 = getHelmChart("helm.tgz", "helm_package2", vnfInstance, 2, HelmChartType.CNF);
        helm_package2.getOperationChartsPriority().put(LifecycleOperationType.TERMINATE, 1);
        vnfInstance.setHelmCharts(List.of(helm_package1, helm_package2));

        doReturn(vnfInstance).when(databaseInteractionService).getVnfInstance(vnfInstanceId);
        doNothing().when(terminateRequestHandler).createTempInstance(any(), any());
        doNothing().when(terminateRequestHandler).specificValidation(any(), any());
        doNothing().when(terminateRequestHandler).updateClusterConfigStatus(any());
        doReturn(new LifecycleOperation()).when(terminateRequestHandler).persistOperation(any(), any(), any(), any(), any(), any());
        doReturn(Map.of(APPLICATION_TIME_OUT, "60")).when(terminateRequestHandler).formatParameters(any(), any(), any(), any());
        doReturn("some_id").when(lifeCycleManagementService).executeRequest(any(), any(), any(), any(), any(), any());

        lifeCycleManagementService
                .executeRequest(LifecycleOperationType.TERMINATE, vnfInstanceId,
                                new TerminateVnfRequest(), null, null);
        final Map<String, Integer> chartPriorityMap = vnfInstance.getHelmCharts()
                .stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::getPriority));
        assertEquals(Integer.valueOf(2), chartPriorityMap.get("helm_package1"));
        assertEquals(Integer.valueOf(1), chartPriorityMap.get("helm_package2"));
    }

    @Test
    public void testPerformTerminateForWFSBadRequest() throws Exception {
        //given
        String vnfInstanceId = "rf1ce-4cf4-477c-aab3-21c454e6a375";
        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage("Workflow service is unavailable or unable to process the request");
        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        doReturn(response).when(workflowRoutingService).routeTerminateRequest(any(), any(), any());
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(0);
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put(NAMESPACE, "testinstantiate");
        additionalParams.put(CLEAN_UP_RESOURCES, "false");
        request.setAdditionalParams(additionalParams);
        //when
        lifeCycleManagementService.executeRequest(LifecycleOperationType.TERMINATE, vnfInstanceId, request, null, null);
        //then
        ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);

        verify(databaseInteractionService, timeout(5000).atLeast(4))
                .persistLifecycleOperation(lifecycleOperationArgumentCaptor.capture());
        verify(databaseInteractionService, timeout(2000).times(1))
                .persistVnfInstanceAndOperation(any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());
        verify(terminateRequestHandler, timeout(2000).atLeastOnce())
                .persistOperationAndInstanceAfterExecution(vnfInstanceArgumentCaptor.capture(), lifecycleOperationArgumentCaptor.capture());
        LifecycleOperation afterTerminate = lifecycleOperationArgumentCaptor.getValue();
        assertThat(afterTerminate.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        ProblemDetails problemDetails = mapper.readValue(afterTerminate.getError(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .isEqualTo("Workflow service is unavailable or unable to process the request");
    }

    private Callable<Boolean> deleteNodeIsFinished(final String operationId) {
        return () -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
            return operation.isDeleteNodeFinished();
        };
    }

}
