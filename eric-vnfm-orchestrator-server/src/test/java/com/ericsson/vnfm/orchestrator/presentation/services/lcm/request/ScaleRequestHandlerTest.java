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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleLevelCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleParametersServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ScaleRequestHandler.class,
        ObjectMapper.class,
        MappingFileServiceImpl.class,
        VnfInstanceServiceImpl.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        ScaleServiceImpl.class,
        ScaleParametersServiceImpl.class,
        ScaleLevelCalculationServiceImpl.class,
        ValuesFileComposer.class,
        YamlFileMerger.class,
        YamlMapFactoryBean.class,
        LifeCycleManagementHelper.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        InstantiateVnfRequestValidatingServiceImpl.class,
        HelmChartHelper.class})
@MockBean(classes = {
        OssNodeService.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        GrantingResourceDefinitionCalculation.class,
        VnfInstanceMapper.class,
        UsernameCalculationService.class,
        GrantingResourceDefinitionCalculation.class,
        LcmOpSearchService.class,
        LcmOpAdditionalParamsProcessor.class,
        LifecycleOperationHelper.class,
        NetworkDataTypeValidationService.class,
        ReplicaCountCalculationService.class,
        NetworkDataTypeValidationService.class,
        ClusterConfigService.class,
        HelmClientVersionValidator.class,
        HelmChartRepository.class
})
public class ScaleRequestHandlerTest {

    private static final String POLICIES_FOR_SCALE_JSON = "scale/scale-policies.json";
    private static final String VNFD_JSON = "scale/vnfd.json";

    @Autowired
    private ScaleRequestHandler scaleRequestHandler;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @Test
    public void testFormatParametersKeepsHelmNoHooksFalse() {
        // given
        final VnfInstance instance = new VnfInstance();

        final ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAdditionalParams(new HashMap<>(Map.of(APPLICATION_TIME_OUT, "600",
                                                         HELM_NO_HOOKS, FALSE.toString())));

        // when
        final Map<String, Object> result = scaleRequestHandler.formatParameters(instance, request, null, null);

        // then
        assertThat(result)
                .hasSize(2)
                .contains(entry(HELM_NO_HOOKS, FALSE.toString()));
    }

    @Test
    public void testFormatParametersKeepsHelmNoHooksTrue() {
        // given
        final VnfInstance instance = new VnfInstance();

        final ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAdditionalParams(new HashMap<>(Map.of(APPLICATION_TIME_OUT, "600",
                                                         HELM_NO_HOOKS, TRUE.toString())));

        // when
        final Map<String, Object> result = scaleRequestHandler.formatParameters(instance, request, null, null);

        // then
        assertThat(result)
                .hasSize(2)
                .contains(entry(HELM_NO_HOOKS, TRUE.toString()));
    }

    @Test
    public void testFormatParametersSetsDefaultHelmNoHooksTrue() {
        // given
        final VnfInstance instance = new VnfInstance();

        final ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAdditionalParams(new HashMap<>(Map.of(APPLICATION_TIME_OUT, "600")));

        // when
        final Map<String, Object> result = scaleRequestHandler.formatParameters(instance, request, null, null);

        // then
        assertThat(result)
                .hasSize(2)
                .contains(entry(HELM_NO_HOOKS, TRUE.toString()));
    }

    @Test
    public void testUpdateInstance() throws JsonProcessingException {
        VnfInstance instance = createVnfInstance();
        instance.setPolicies(getFile(POLICIES_FOR_SCALE_JSON));
        instance.getHelmCharts().get(0).setReplicaDetails(getFile("scale/chart-replica-details.json"));
        instance.setResourceDetails("{\"PL__scaled_vm\":14,\"CL_scaled_vm\":14,\"TL__scaled_vm\":14}");
        instance.setScaleInfoEntity(List.of(createScaleInfoEntity("Payload", 3)));
        ScaleVnfRequest scale = TestUtils.createScaleRequest("Payload", 3, ScaleVnfRequest.TypeEnum.IN);

        Map<String, Object> additionalParams = scaleRequestHandler
                .formatParameters(instance, scale, LifecycleOperationType.SCALE, null);

        LifecycleOperation operation = scaleRequestHandler.persistOperation(instance, scale, null, LifecycleOperationType.SCALE, additionalParams, "3600");

        verify(databaseInteractionService).persistLifecycleOperationInProgress(any(), any(), any());

        when(packageService.getVnfd(anyString())).thenReturn(new JSONObject(getFile(VNFD_JSON)));

        scaleRequestHandler.createTempInstance(instance, scale);
        scaleRequestHandler.updateInstance(instance, scale, LifecycleOperationType.SCALE, operation,  additionalParams);

        assertThat(instance.getTempInstance()).isNotEmpty();
        String tempVnfInstance = instance.getTempInstance();
        VnfInstance tempInstance = mapper.readValue(tempVnfInstance, VnfInstance.class);
        assertThat(tempInstance.getVnfSoftwareVersion()).isEqualTo(instance.getVnfSoftwareVersion());
        assertThat(tempInstance.getVnfProviderName()).isEqualTo(instance.getVnfProviderName());
        assertThat(tempInstance.getVnfInstanceName()).isEqualTo(instance.getVnfInstanceName());
        assertThat(tempInstance.getVnfProductName()).isEqualTo(instance.getVnfProductName());
        assertThat(tempInstance.getVnfSoftwareVersion()).isEqualTo(instance.getVnfSoftwareVersion());
        assertThat(tempInstance.getVnfdVersion()).isEqualTo(instance.getVnfdVersion());
        assertThat(tempInstance.getVnfDescriptorId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(tempInstance.getVnfPackageId()).isEqualTo(instance.getVnfPackageId());
        assertThat(tempInstance.getHelmCharts().size()).isEqualTo(1);
        assertThat(tempInstance.getHelmCharts().get(0).getHelmChartUrl()).isEqualTo(instance.getHelmCharts().get(0).getHelmChartUrl());
        assertThat(tempInstance.getHelmCharts().get(0).getReleaseName()).isEqualTo(instance.getHelmCharts().get(0).getReleaseName());
    }

    @Test
    public void testSendRequest() {
        ScaleVnfRequest scale = TestUtils.createScaleRequest("test", 3, ScaleVnfRequest.TypeEnum.IN);
        VnfInstance instance = createVnfInstance();

        LifecycleOperation operation = scaleRequestHandler.persistOperation(instance, scale, null, LifecycleOperationType.SCALE, null, "3600");

        WorkflowRoutingResponse response = TestUtils.createResponse(instance.getVnfInstanceId(), null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeScaleRequest(any(), any(), any())).thenReturn(response);

        scaleRequestHandler.sendRequest(instance, operation, scale, null);

        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
    }

    @Test
    public void testRunScaleSpecificValidation() {
        ScaleVnfRequest request = TestUtils.createScaleRequest("Payload", 4, ScaleVnfRequest.TypeEnum.IN);
        when(packageService.getVnfd(any())).thenReturn(null);

        final VnfInstance vnfInstance = createVnfInstance();
        vnfInstance.setClusterName("cluster");

        vnfInstance.setScaleInfoEntity(List.of(createScaleInfoEntity("Payload", 5)));
        vnfInstance.setPolicies(getFile(POLICIES_FOR_SCALE_JSON));
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);

        assertThatNoException().isThrownBy(() -> scaleRequestHandler.specificValidation(vnfInstance, request));
    }

    @Test
    public void testScaleRequestScalesOnlyOneChart() throws JsonProcessingException {
        VnfInstance instance = createVnfInstanceForOnlyOneChartScaling();

        ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("Aspect2", 1, ScaleVnfRequest.TypeEnum.OUT);
        Map<String, Object> additionalParam = new HashMap<>(Map.of("applicationTimeOut", "320"));
        scaleRequest.setAdditionalParams(additionalParam);
        LifecycleOperation operation = scaleRequestHandler.persistOperation(instance, scaleRequest, null, LifecycleOperationType.SCALE, null, "3600");

        when(packageService.getVnfd(anyString())).thenReturn(new JSONObject(getFile(VNFD_JSON)));

        scaleRequestHandler.createTempInstance(instance, scaleRequest);
        scaleRequestHandler.updateInstance(instance, scaleRequest, LifecycleOperationType.SCALE, operation, additionalParam);

        VnfInstance tempVnfInstance = mapper.readValue(instance.getTempInstance(), VnfInstance.class);
        assertThat(tempVnfInstance.getHelmCharts().size()).isEqualTo(1);
        assertThat(tempVnfInstance.getHelmCharts().get(0).getHelmChartUrl())
                .isEqualTo("http://registry-url/onboarded/charts/test-scale-chart-0.2.2.tgz");
    }

    @Test
    public void testScaleOperationShouldHaveEnabledChartsOnlyWhenRequestToWfs() {
        final String chartReleaseNameEnabled = "enabled-chart-name";
        final String chartReleaseNameDisabled = "disabled-chart-name";
        VnfInstance vnfInstance = createVnfInstanceForOnlyOneChartScaling();
        vnfInstance.setHelmCharts(Arrays.asList(createHelmChart("http://registry-url/onboarded/charts/test-scale-chart-0.2.2.tgz", chartReleaseNameEnabled, true),
                createHelmChart("http://registry-url/onboarded/charts/test-scale-chart-0.2.2.tgz", chartReleaseNameDisabled, false)));
        ScaleVnfRequest scaleVnfRequest = TestUtils.createScaleRequest("Aspect2", 1, ScaleVnfRequest.TypeEnum.OUT);
        Map<String, Object> additionalParams = new HashMap<>(Map.of("applicationTimeOut", "320"));
        scaleVnfRequest.setAdditionalParams(additionalParams);
        LifecycleOperation operation = scaleRequestHandler.persistOperation(vnfInstance, scaleVnfRequest, null, LifecycleOperationType.SCALE, null, "3600");

        WorkflowRoutingResponse response = TestUtils.createResponse(vnfInstance.getVnfInstanceId(), null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeScaleRequest(any(), any(), any())).thenReturn(response);

        scaleRequestHandler.sendRequest(vnfInstance, operation, scaleVnfRequest, null);

        verify(workflowRoutingService, times(1)).routeScaleRequest(vnfInstance, operation, scaleVnfRequest);

        final ArgumentCaptor<LifecycleOperation> operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistLifecycleOperationInProgress(operationCaptor.capture(), any(), any());

        LifecycleOperation updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        assertThat(updatedOperation.getVnfInstance().getHelmCharts().get(0).isChartEnabled()).isEqualTo(true);
        assertThat(updatedOperation.getVnfInstance().getHelmCharts().get(1).isChartEnabled()).isEqualTo(false);
    }


    @Test
    public void testSendRequestForErrorResponseFromWorkflowService() throws JsonProcessingException {
        ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("test", 3, ScaleVnfRequest.TypeEnum.IN);
        VnfInstance instance = createVnfInstance();

        LifecycleOperation operation = scaleRequestHandler.persistOperation(instance,
                                                                            scaleRequest,
                                                                            null,
                                                                            LifecycleOperationType.SCALE,
                                                                            null, "3600");

        WorkflowRoutingResponse response = TestUtils.createResponse(instance.getVnfInstanceId(), "Error occurred", HttpStatus.BAD_REQUEST);
        when(workflowRoutingService.routeScaleRequest(any(), any(), any())).thenReturn(response);

        scaleRequestHandler.sendRequest(instance, operation, scaleRequest, null);

        final ArgumentCaptor<LifecycleOperation> operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistLifecycleOperation(operationCaptor.capture());
        LifecycleOperation updatedOperation = operationCaptor.getValue();

        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        ProblemDetails problemDetails = mapper.readValue(updatedOperation.getError(), ProblemDetails.class);
        assertThat(problemDetails.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problemDetails.getDetail()).isEqualTo("Error occurred");
    }

    @Test
    public void testSendRequestForErrorWorkflowBadRequest() throws JsonProcessingException {
        ScaleVnfRequest scaleRequest = TestUtils.createScaleRequest("test", 3, ScaleVnfRequest.TypeEnum.IN);
        VnfInstance instance = createVnfInstance();

        LifecycleOperation operation = scaleRequestHandler.persistOperation(instance,
                                                                            scaleRequest,
                                                                            null,
                                                                            LifecycleOperationType.SCALE,
                                                                            null, "3600");

        WorkflowRoutingResponse response = TestUtils.createResponse(instance.getVnfInstanceId(),
                                                                    WorkflowRoutingServicePassThrough.WORKFLOW_UNAVAILABLE,
                                                                    HttpStatus.BAD_REQUEST);

        when(workflowRoutingService.routeScaleRequest(any(), any(), any())).thenReturn(response);

        scaleRequestHandler.sendRequest(instance, operation, scaleRequest, null);

        final ArgumentCaptor<LifecycleOperation> operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistLifecycleOperation(operationCaptor.capture());
        LifecycleOperation updatedOperation = operationCaptor.getValue();

        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        ProblemDetails problemDetails = mapper.readValue(updatedOperation.getError(), ProblemDetails.class);
        assertThat(problemDetails.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problemDetails.getDetail()).isEqualTo(WorkflowRoutingServicePassThrough.WORKFLOW_UNAVAILABLE);
        assertThat(problemDetails.getTitle()).isEqualTo("Unprocessable Entity");
    }

    private static VnfInstance createVnfInstance() {
        final VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("test");
        instance.setVnfPackageId("test");
        instance.setHelmCharts(List.of(createHelmChart("chart-url", null)));
        instance.setSupportedOperations(createSupportedOperations(LCMOperationsEnum.SCALE));

        return instance;
    }

    private VnfInstance createVnfInstanceForOnlyOneChartScaling() {
        VnfInstance instance = createVnfInstance();
        instance.setCombinedAdditionalParams("{\"Payload_InitialDelta.replicaCount\":3,\"Payload_InitialDelta_1.replicaCount\":1}");
        instance.setPolicies(getFile("scale/only-one-chart-scaling-scale-policies.json"));
        instance.setResourceDetails("{\"test-cnf-vnfc3\":1,"
                                            + "\"test-cnf-vnfc4\":1,"
                                            + "\"test-cnf\":1,"
                                            + "\"test-cnf-vnfc5\":1,"
                                            + "\"eric-pm-bulk-reporter\":1,"
                                            + "\"test-cnf-vnfc1\":1}");
        instance.setScaleInfoEntity(List.of(createScaleInfoEntity("Aspect1", 0),
                                            createScaleInfoEntity("Aspect2", 0),
                                            createScaleInfoEntity("Aspect3", 0),
                                            createScaleInfoEntity("Aspect5", 0)));
        instance.setHelmCharts(List.of(createHelmChart(
                                               "http://registry-url/onboarded/charts/test-scale-chart-0.2.2.tgz",
                                               getFile("scale/only-one-chart-scaling-chart-1-replica-details.json")),
                                       createHelmChart(
                                               "http://registry-url/onboarded/charts/spider-app-2.208.1.tgz",
                                               getFile("scale/only-one-chart-scaling-chart-2-replica-details.json"))));

        return instance;
    }

    private static ScaleInfoEntity createScaleInfoEntity(final String aspectId, final int scaleLevel) {
        ScaleInfoEntity entity = new ScaleInfoEntity();
        entity.setScaleLevel(scaleLevel);
        entity.setAspectId(aspectId);
        return entity;
    }

    private static HelmChart createHelmChart(final String helmChartUrl, final String replicaDetails) {
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartType(HelmChartType.CNF);
        helmChart.setHelmChartUrl(helmChartUrl);
        helmChart.setReplicaDetails(replicaDetails);

        return helmChart;
    }

    private static HelmChart createHelmChart(final String helmChartUrl, final String replicaDetails, boolean isChartEnabled) {
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartType(HelmChartType.CNF);
        helmChart.setHelmChartUrl(helmChartUrl);
        helmChart.setReplicaDetails(replicaDetails);
        helmChart.setChartEnabled(isChartEnabled);

        return helmChart;
    }

    private String getFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }

}
