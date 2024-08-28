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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceDeletionInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        TerminateRequestHandler.class,
        WorkflowRoutingServicePassThrough.class,
        InstantiateVnfRequestValidatingServiceImpl.class,
        Day0ConfigurationService.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        HelmChartHelper.class,
        CryptoUtils.class,
        ObjectMapper.class
})
@MockBean(classes = {
        ValuesFileComposer.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        UsernameCalculationService.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        LifeCycleManagementHelper.class,
        GrantingResourceDefinitionCalculation.class,
        MappingFileService.class,
        ValuesFileService.class,
        ScaleService.class,
        YamlMapFactoryBean.class,
        CryptoService.class,
        WorkflowRequestBodyBuilder.class,
        LcmOpAdditionalParamsProcessor.class,
        LcmOpSearchService.class,
        PackageService.class,
        LifecycleOperationHelper.class,
        NetworkDataTypeValidationService.class,
        VnfInstanceService.class,
        NotificationService.class,
        HelmChartRepository.class})
@MockBean(classes = RetryTemplate.class, name = "wfsRoutingRetryTemplate")
public class TerminateRequestHandlerTest {

    @Autowired
    private TerminateRequestHandler terminateRequestHandler;

    @SpyBean
    private WorkflowRoutingServicePassThrough workflowRoutingService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private ScheduledThreadPoolExecutor taskExecutor;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @MockBean
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Test
    public void testSendRequestToWfsAndShouldSkipCrdCharts() {

        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setHelmCharts(Arrays.asList(createHelmChart(HelmChartType.CRD, "crd-chart-0", 1),
                                                createHelmChart(HelmChartType.CNF, "cnf-chart-0", 3),
                                                createHelmChart(HelmChartType.CRD, "crd-chart-1", 2),
                                                createHelmChart(HelmChartType.CNF, "cnf-chart-1", 4)));

        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("cleanUpResources", true);
        additionalParams.put("applicationTimeOut", "500");
        additionalParams.put("deleteIdentifier", true);
        request.additionalParams(additionalParams);

        LifecycleOperation operation = new LifecycleOperation();
        operation.setExpiredApplicationTime(LocalDateTime.now().plusHours(1));

        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenReturn(ResponseEntity.ok().build());
        terminateRequestHandler.sendRequest(vnfInstance, operation, request, null);

        verify(workflowRoutingService, never()).routeTerminateRequest(any(VnfInstance.class), any(), eq("crd-chart-0"));
        verify(workflowRoutingService, never()).routeTerminateRequest(any(VnfInstance.class), any(), eq("crd-chart-1"));
        verify(workflowRoutingService).routeTerminateRequest(any(VnfInstance.class), any(), eq("cnf-chart-0"));
    }

    @Test
    public void testUpdateInstanceWhenNamespaceIsRestrictedFail() {
        TerminateVnfRequest vnfRequest = new TerminateVnfRequest();

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setClusterName("Default");
        vnfInstance.setNamespace("Default");
        lifecycleOperation.setVnfInstance(vnfInstance);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, "true");

        when(clusterConfigService.getConfigFileByName(any())).thenReturn(new ClusterConfigFile());
        doThrow(new NamespaceDeletionInProgressException(String.format(NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE,
                                                                       vnfInstance.getNamespace(),
                                                                       vnfInstance.getClusterName())))
                .when(lifeCycleManagementHelper).persistNamespaceDetails(any());

        assertThatThrownBy(() -> terminateRequestHandler
                .updateInstance(vnfInstance, vnfRequest, LifecycleOperationType.TERMINATE, lifecycleOperation, additionalParams))
                .isInstanceOf(NamespaceDeletionInProgressException.class)
                .hasMessage(String.format(NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE, vnfInstance.getNamespace(),
                                          vnfInstance.getClusterName()));
    }

    @Test
    public void testSpecificValidation() {
        var vnfInstanceId = "vnf-instance-id";
        var vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setVnfInstanceId(vnfInstanceId);
        vnfInstance.setClusterName("workflow-routine-namespace");
        vnfInstance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        ClusterConfigFile clusterConfigFile = ClusterConfigFile.builder().name("workflow-routine-namespace")
                .build();

        when(clusterConfigService.getConfigFileByName(vnfInstance.getClusterName())).thenReturn(clusterConfigFile);

        terminateRequestHandler.specificValidation(vnfInstance, new TerminateVnfRequest());

        verify(instantiateVnfRequestValidatingService).validateTimeouts(any());
    }

    @Test
    public void testSetCleanUpResource() {
        var vnfInstance = new VnfInstance();
        vnfInstance.setCleanUpResources(false);

        terminateRequestHandler.setCleanUpResources(vnfInstance, Map.of(CLEAN_UP_RESOURCES, true));

        assertThat(vnfInstance.isCleanUpResources()).isTrue();
    }

    @Test
    public void testDeleteNodeFromENM() {
        String lifecycleOperationId = "lifecycle-operation-id";
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setAddedToOss(true);
        Map<String, Object> nodeOssTopology = Map.of(MANAGED_ELEMENT_ID, 444);
        vnfInstance.setAddNodeOssTopology(nodeOssTopology.toString());
        lifecycleOperation.setVnfInstance(vnfInstance);

        doNothing().when(ossNodeService).deleteNodeFromENM(vnfInstance, false);
        when(databaseInteractionService.getLifecycleOperation(lifecycleOperationId)).thenReturn(lifecycleOperation);
        doAnswer(invocation -> {
                     ((Runnable) invocation.getArgument(0)).run();
                     return null;
                 }
        ).when(taskExecutor).execute(any());

        terminateRequestHandler.deleteNodeFromENM(vnfInstance, lifecycleOperation);

        verify(taskExecutor, atLeast(1)).execute(any(Runnable.class));
        verify(ossNodeService, atLeast(1)).deleteNodeFromENM(vnfInstance, false);
        verify(databaseInteractionService, atLeast(2)).persistLifecycleOperation(lifecycleOperation);

        assertThat(lifecycleOperation.isDeleteNodeFinished()).isTrue();
    }

    @Test
    public void testRequestToWfsShouldHaveEnabledChartsOnly() {
        final String chartReleaseNameEnabled = "enabled-chart-name";
        final String chartReleaseNameDisabled = "disabled-chart-name";
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setHelmCharts(Arrays.asList(createHelmChart(HelmChartType.CNF, chartReleaseNameEnabled, true),
                                                createHelmChart(HelmChartType.CNF, chartReleaseNameDisabled, false)));
        TerminateVnfRequest request = new TerminateVnfRequest();
        LifecycleOperation operation = new LifecycleOperation();
        operation.setExpiredApplicationTime(LocalDateTime.now().plusHours(1));

        when(restTemplate.exchange(any(String.class), any(), any(), any(Class.class))).thenReturn(ResponseEntity.ok().build());

        terminateRequestHandler.updateInstance(vnfInstance, request, LifecycleOperationType.TERMINATE, operation, new HashMap<>());
        terminateRequestHandler.sendRequest(vnfInstance, operation, request, null);

        verify(workflowRoutingService, times(1)).routeTerminateRequest(any(VnfInstance.class), any(), eq(chartReleaseNameEnabled));
        verify(workflowRoutingService, never()).routeTerminateRequest(any(VnfInstance.class), any(), eq(chartReleaseNameDisabled));
    }

    private HelmChart createHelmChart(final HelmChartType crd, final String releaseName, boolean isChartEnabled) {
        HelmChart helmChart = createHelmChart(crd, releaseName, 1);
        helmChart.setChartEnabled(isChartEnabled);
        return helmChart;
    }

    @NotNull
    private HelmChart createHelmChart(final HelmChartType helmChartType, final String releaseName, final int priority) {
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartType(helmChartType);
        helmChart.setReleaseName(releaseName);
        helmChart.setPriority(priority);
        return helmChart;
    }
}