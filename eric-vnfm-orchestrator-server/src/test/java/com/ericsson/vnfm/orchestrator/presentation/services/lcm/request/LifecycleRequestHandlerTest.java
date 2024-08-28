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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.SITEBASIC_XML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.DISABLE_OPENAPI_VALIDATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.SCALE;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AlreadyInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LifecycleInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.EvnfmOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.NfvoOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClientImpl;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        YamlUtility.class,
        LifecycleOperationRequestImplTestHelper.class,
        InstanceService.class,
        ObjectMapper.class,
        LifeCycleManagementHelper.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        LcmOperationsConfig.class
})
@MockBean(classes = {
        OssNodeService.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        OnboardingClientImpl.class,
        EvnfmOnboardingRoutingClient.class,
        NfvoOnboardingRoutingClient.class,
        OnboardingUriProvider.class,
        GrantingResourceDefinitionCalculation.class,
        MappingFileService.class,
        OnboardingConfig.class,
        RestTemplate.class,
        VnfInstanceQuery.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ChangePackageOperationDetailsRepository.class,
        VnfInstanceService.class,
        YamlMapFactoryBean.class,
        ExtensionsService.class,
        InstantiationLevelService.class,
        PackageService.class,
        ValuesFileService.class,
        NotificationService.class,
        InstanceUtils.class,
        VnfInstanceMapper.class,
        VnfInstanceRepository.class,
        AdditionalAttributesHelper.class,
        LifecycleOperationHelper.class,
        LcmOpSearchService.class,
        ClusterConfigService.class,
        ReplicaCountCalculationService.class,
        ChangePackageOperationDetailsService.class,
        ReplicaCountCalculationService.class,
        HelmClientVersionValidator.class,
        WorkflowRoutingService.class,
        ChangeVnfPackageService.class
})
public class LifecycleRequestHandlerTest {

    private static final String USERNAME = "eo-cm-user";

    @Autowired
    private LifecycleOperationRequestImplTestHelper lifecycleOperationRequest;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UsernameCalculationService usernameCalculationService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ValuesFileComposer valuesFileComposer;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @Captor
    private ArgumentCaptor<VnfInstance> vnfInstanceArgumentCaptor;

    @Captor
    private ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor;

    @Test
    public void parameterPresentInAdditionalParams(){
        final HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(VALUES_YAML_ADDITIONAL_PARAMETER, "....");
        assertThat(LifecycleRequestHandler.parameterPresent(additionalParams,
                                            VALUES_YAML_ADDITIONAL_PARAMETER)).isTrue();
    }

    @Test
    public void parameterNotPresentInAdditionalParams() {
        final HashMap additionalParams = new HashMap();
        assertThat(LifecycleRequestHandler.parameterPresent(additionalParams,
                                            VALUES_YAML_ADDITIONAL_PARAMETER)).isFalse();
    }

    @Test
    public void testCommonValidationInstanceInInstantiatedState() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        when(databaseInteractionService.getVnfInstance("any-id")).thenReturn(vnfInstance);

        assertThatThrownBy(() -> lifecycleOperationRequest
                .commonValidation(vnfInstance, InstantiationState.INSTANTIATED, LCMOperationsEnum.INSTANTIATE))
                .isInstanceOf(AlreadyInstantiatedException.class)
                .hasMessageStartingWith("VNF instance ID any-id is already in the INSTANTIATED state");
    }

    @Test
    public void testCommonValidationInstanceInNotInstantiatedState() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        when(databaseInteractionService.getVnfInstance("any-id")).thenReturn(vnfInstance);

        assertThatThrownBy(() -> lifecycleOperationRequest
                .commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.INSTANTIATE))
                .isInstanceOf(NotInstantiatedException.class)
                .hasMessageStartingWith("VNF instance ID any-id is not in the INSTANTIATED state");
    }

    @Test
    public void testCommonValidationInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        vnfInstance.setSupportedOperations(
                Collections.singletonList(OperationDetail.ofSupportedOperation(LCMOperationsEnum.INSTANTIATE.getOperation())));
        when(databaseInteractionService.getVnfInstance("any-id")).thenReturn(vnfInstance);

       lifecycleOperationRequest
                .commonValidation(vnfInstance, InstantiationState.INSTANTIATED, LCMOperationsEnum.INSTANTIATE);
        assertThat(vnfInstance).isNotNull();
        assertThat(vnfInstance.getVnfInstanceId()).isEqualTo("any-id");
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
    }

    @Test
    public void testCommonValidationInstanceOperationNotInProgress() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setSupportedOperations(
                Collections.singletonList(OperationDetail.ofSupportedOperation(LCMOperationsEnum.TERMINATE.getOperation())));
        when(databaseInteractionService.getVnfInstance("any-id")).thenReturn(vnfInstance);
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(vnfInstance)).thenReturn(0);

        assertThatNoException().isThrownBy(() -> lifecycleOperationRequest
                .commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.TERMINATE));
    }

    @Test
    public void testPersistOperation() throws Exception {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setVnfInstanceName("any-name");
        vnfInstance.setVnfInstanceDescription("any-description");
        vnfInstance.setVnfDescriptorId("any-descriptor");
        vnfInstance.setVnfProviderName("any-provider");
        vnfInstance.setVnfProductName("any-product");
        vnfInstance.setVnfSoftwareVersion("any-software-version");
        vnfInstance.setVnfdVersion("any-vnfd-version");
        vnfInstance.setVnfPackageId("any-package-id");
        ScaleVnfRequest scale = TestUtils.createScaleRequest("test", 3,
                                                             ScaleVnfRequest.TypeEnum.IN);
        doNothing().when(databaseInteractionService)
                .persistLifecycleOperationInProgress(
                        any(LifecycleOperation.class),
                        any(VnfInstance.class),
                        any(LifecycleOperationType.class));

        doNothing().when(databaseInteractionService)
                .addToOperationsInProgressTable("any-id", LifecycleOperationType.SCALE);

        LifecycleOperation operation = lifecycleOperationRequest
                .persistOperation(vnfInstance,
                                  scale,
                                  USERNAME,
                                  LifecycleOperationType.SCALE,
                                  null, "3600");

        assertThat(operation).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        ScaleVnfRequest opScale = mapper.readValue(operation.getOperationParams(), ScaleVnfRequest.class);
        assertThat(opScale.getAspectId()).isEqualTo(scale.getAspectId());
        assertThat(opScale.getNumberOfSteps()).isEqualTo(scale.getNumberOfSteps());
        assertThat(opScale.getType()).isEqualTo(scale.getType());
        assertThat(operation.getVnfSoftwareVersion()).isEqualTo("any-software-version");
        assertThat(operation.getVnfProductName()).isEqualTo("any-product");
        assertThat(operation.getStartTime()).isNotNull();
        assertThat(operation.getStateEnteredTime()).isNotNull();
        assertThat(operation.getSourceVnfdId()).isEqualTo(vnfInstance.getVnfDescriptorId());
        assertThat(operation.getUsername()).isNotNull();
        assertThat(operation.getApplicationTimeout()).isEqualTo("3600");
        assertThat(operation.getExpiredApplicationTime()).isNotNull();
        assertThat(operationsInProgressRepository.findByVnfId(vnfInstance.getVnfInstanceId())).isNotNull();
        assertThat(vnfInstance.getOperationOccurrenceId()).isEqualTo(operation.getOperationOccurrenceId());
    }

    @Test
    public void testPersistOperationThrowsLifecycleInProgressException() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        ScaleVnfRequest scale = TestUtils.createScaleRequest("any-id", 3,
                ScaleVnfRequest.TypeEnum.IN);

        when(usernameCalculationService.calculateUsername()).thenReturn(USERNAME);
        doCallRealMethod()
                .when(databaseInteractionService)
                .persistLifecycleOperationInProgress(
                        any(LifecycleOperation.class),
                        any(VnfInstance.class),
                        any(LifecycleOperationType.class));
        doCallRealMethod()
                .when(databaseInteractionService)
                .addToOperationsInProgressTable("any-id", LifecycleOperationType.SCALE);
        doThrow(new LifecycleInProgressException("Lifecycle operation INSTANTIATE is in progress for vnf instance any-id, hence cannot perform operation"))
                .when(databaseInteractionService)
                .checkLifecycleInProgress("any-id");
        doNothing().when(databaseInteractionService).failIfRunningLcmOperationsAmountExceeded();

        assertThatThrownBy(() -> lifecycleOperationRequest.persistOperation(vnfInstance, scale, null, LifecycleOperationType.SCALE,null, "3600"))
                .isInstanceOf(LifecycleInProgressException.class)
                .hasMessageStartingWith("Lifecycle operation INSTANTIATE is in progress for vnf instance "
                                                + vnfInstance.getVnfInstanceId() + ", hence cannot perform operation");

    }

    @Test
    public void testPersistOperationWithValuesFile() throws Exception{
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        vnfInstance.setVnfInstanceName("any-name");
        vnfInstance.setVnfInstanceDescription("any-description");
        vnfInstance.setVnfDescriptorId("any-descriptor");
        vnfInstance.setVnfProviderName("any-provider");
        vnfInstance.setVnfProductName("any-product");
        vnfInstance.setVnfSoftwareVersion("any-software-version");
        vnfInstance.setVnfdVersion("any-vnfd-version");
        vnfInstance.setVnfPackageId("any-package-id");
        ScaleVnfRequest scale = TestUtils.createScaleRequest("test", 3,
                                                             ScaleVnfRequest.TypeEnum.IN);

        Path valuesFile = getResource("valueFiles/large-values-a.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        JSONObject valuesJson = YamlUtility.convertYamlFileIntoJson(valuesFile);

        when(usernameCalculationService.calculateUsername()).thenReturn(USERNAME);
        LifecycleOperation operation = lifecycleOperationRequest.persistOperation(vnfInstance, scale, USERNAME,
                                                                                  LifecycleOperationType.SCALE, valuesYamlMap, "3600");

        assertThat(operation).isNotNull();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.STARTING);
        ScaleVnfRequest opScale = mapper.readValue(operation.getOperationParams(), ScaleVnfRequest.class);
        assertThat(opScale.getAspectId()).isEqualTo(scale.getAspectId());
        assertThat(opScale.getNumberOfSteps()).isEqualTo(scale.getNumberOfSteps());
        assertThat(opScale.getType()).isEqualTo(scale.getType());
        assertThat(operation.getVnfSoftwareVersion()).isEqualTo("any-software-version");
        assertThat(operation.getVnfProductName()).isEqualTo("any-product");
        assertThat(operation.getStartTime()).isNotNull();
        assertThat(operation.getStateEnteredTime()).isNotNull();
        assertThat(operation.getUsername()).isNotNull();
        assertThat(operation.getSourceVnfdId()).isEqualTo(vnfInstance.getVnfDescriptorId());
        assertThat(operationsInProgressRepository.findByVnfId(vnfInstance.getVnfInstanceId())).isNotNull();
        assertThat(vnfInstance.getOperationOccurrenceId()).isEqualTo(operation.getOperationOccurrenceId());
        assertThat(operation.getValuesFileParams()).isEqualTo(valuesJson.toString());
    }

    @Test
    public void testPersistOperationShouldStoreActualUserForUsername(){
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        ScaleVnfRequest scale = TestUtils.createScaleRequest("test", 3,
                                                             ScaleVnfRequest.TypeEnum.IN);

        LifecycleOperation operation = lifecycleOperationRequest.persistOperation(vnfInstance, scale, USERNAME,
                                                                                  LifecycleOperationType.SCALE, null, "3600");

        assertThat(operation.getUsername()).isEqualTo(USERNAME);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void additionalParamsContainsEvnfmSpecificParams() {
        final HashMap additionalParams = new HashMap();
        List<String> allParams = new ArrayList<>(EVNFM_PARAMS);
        allParams.addAll(EVNFM_PARAMS_FOR_WFS);
        assertThat(LifecycleRequestHandler.isMapContainsNotOnlyExpectedParameters(additionalParams,
                                                                                  allParams)).isFalse();
        additionalParams.put(NAMESPACE, "test-2");
        additionalParams.put(DISABLE_OPENAPI_VALIDATION, "true");
        assertThat(LifecycleRequestHandler.isMapContainsNotOnlyExpectedParameters(additionalParams,
                                                                                  allParams)).isFalse();

        additionalParams.put(SITEBASIC_XML, "xml file");
        additionalParams.put("day0.configuration.secret", "day0-secret");
        assertThat(LifecycleRequestHandler.isMapContainsNotOnlyExpectedParameters(additionalParams,
                                                                                  allParams)).isTrue();
    }

    @Test
    public void testCheckForErrorsWithWorkflowRoutingResponse() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        String releaseName = "workflow-routine-namespace-0";
        HelmChart helmChart1 = new HelmChart();
        helmChart1.setHelmChartType(HelmChartType.CNF);
        helmChart1.setState(LifecycleOperationState.PROCESSING.toString());
        helmChart1.setReleaseName(releaseName);

        List<HelmChart> helmCharts = List.of(helmChart1);

        vnfInstance.setHelmCharts(helmCharts);
        vnfInstance.setClusterName("workflow-routine-namespace");
        WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setHttpStatus(HttpStatus.BAD_REQUEST);

        String lifecycleOperationId = "any-id";
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(lifecycleOperationId);
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setOperationState(LifecycleOperationState.PROCESSING);

        lifecycleOperationRequest.checkAndProcessFailedError(lifecycleOperation, response);

        var expectedLifecycleOperationError = "{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,"
                + "\"detail\":\"Unexpected Exception occurred\",\"instance\":\"about:blank\"}";
        var actualLifecycleOperationError = lifecycleOperation.getError();
        var helmChart = vnfInstance.getHelmCharts().stream().findFirst().get();

        assertThat(actualLifecycleOperationError).isEqualTo(expectedLifecycleOperationError);

        assertThat(helmChart.getState()).isEqualTo(LifecycleOperationState.FAILED.toString());
    }

    @Test
    public void testCheckForErrorsWithResponseEntity() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        String releaseName = "workflow-routine-namespace-0";
        String lifecycleOperationId = "any-id";
        HelmChart helmChart1 = new HelmChart();
        helmChart1.setHelmChartType(HelmChartType.CRD);
        helmChart1.setReleaseName(releaseName);

        List<HelmChart> helmCharts = List.of(helmChart1);

        vnfInstance.setHelmCharts(helmCharts);

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(lifecycleOperationId);
        lifecycleOperation.setVnfInstance(vnfInstance);

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Object> responseEntity = new ResponseEntity<>(
                "workflow-routine-namespace-0",
                header,
                HttpStatus.BAD_REQUEST
        );

        lifecycleOperationRequest.checkAndProcessFailedError(lifecycleOperation,
                responseEntity,
                releaseName);
        var expectedLifecycleOperationError = "{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,"
                + "\"detail\":\"workflow-routine-namespace-0\",\"instance\":\"about:blank\"}";
        var actualLifecycleOperationError = lifecycleOperation.getError();

        assertThat(actualLifecycleOperationError).isEqualTo(expectedLifecycleOperationError);
    }

    @Test
    public void testIsMapContainsNotOnlyExpectedParameters() {
        Map<String, Object> mapUnderTest = Map.of(
                "one", "param",
                "two", "taram",
                "three", "naram");
        List<String> expectedParameters = List.of("param", "naram");
        var actual = LifecycleRequestHandler.isMapContainsNotOnlyExpectedParameters(mapUnderTest, expectedParameters);

        assertThat(actual).isTrue();
    }

    @Test
    public void testSetInstanceWithCleanUpResources() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        Map<String, Object> additionalParams = Map.of(CLEAN_UP_RESOURCES, true);
        vnfInstance.setCleanUpResources(false);

        LifecycleRequestHandler.setInstanceWithCleanUpResources(vnfInstance, additionalParams);

        assertThat(vnfInstance.isCleanUpResources()).isTrue();
        assertThat(additionalParams.containsKey(CLEAN_UP_RESOURCES)).isTrue();
    }

    @Test
    public void testGetAdditionalParamsInstantiate() {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setFlavourId("flavourId");
        request.setAdditionalParams(Map.of(INSTANTIATE, true));

        Map<String, Object> actualAdditionalParamsMap = LifecycleRequestHandler.getAdditionalParams(request);

        Map<String, Boolean> expectedMap = Map.of(INSTANTIATE, true);

        assertThat(expectedMap).isEqualTo(actualAdditionalParamsMap);
    }

    @Test
    public void testGetAdditionalParamsScale() {
        ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAdditionalParams(Map.of(SCALE, ScaleVnfRequest.TypeEnum.IN));

        Map<String, Object> actualAdditionalParamsMap = LifecycleRequestHandler.getAdditionalParams(request);

        Map<String, ScaleVnfRequest.TypeEnum> expectedMap = Map.of(SCALE, ScaleVnfRequest.TypeEnum.IN);

        assertThat(expectedMap).isEqualTo(actualAdditionalParamsMap);
    }

    @Test
    public void testRemoveExcessAdditionalParams() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PERSIST_SCALE_INFO, true);
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        additionalParams.put(SKIP_VERIFICATION, true);
        additionalParams.put(SKIP_JOB_VERIFICATION, true);
        additionalParams.put(DISABLE_OPENAPI_VALIDATION, true);
        additionalParams.put(HELM_NO_HOOKS, true);
        additionalParams.put(APPLICATION_TIME_OUT, "3600");
        additionalParams.put("any other param to-remove", false);

        LifecycleRequestHandler.removeExcessAdditionalParams(additionalParams);

        assertThat(additionalParams)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        PERSIST_SCALE_INFO, true,
                        CLEAN_UP_RESOURCES, true,
                        SKIP_VERIFICATION, true,
                        SKIP_JOB_VERIFICATION, true,
                        DISABLE_OPENAPI_VALIDATION, true,
                        HELM_NO_HOOKS, true,
                        APPLICATION_TIME_OUT, "3600"
                ));
    }

    @Test
    public void testPersistOperationAndInstanceAfterExecution() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("any-id");
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        String lifecycleOperationId = "lifecycleOperationId";
        lifecycleOperation.setOperationOccurrenceId(lifecycleOperationId);

        doNothing().when(databaseInteractionService).persistVnfInstanceAndOperation(vnfInstance, lifecycleOperation);

        lifecycleOperationRequest.persistOperationAndInstanceAfterExecution(vnfInstance, lifecycleOperation);

        verify(databaseInteractionService)
                .persistVnfInstanceAndOperation(
                        vnfInstanceArgumentCaptor.capture(),
                        lifecycleOperationArgumentCaptor.capture());

        assertThat(lifecycleOperationArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(lifecycleOperation);
        assertThat(vnfInstanceArgumentCaptor.getValue()).isEqualTo(vnfInstance);
    }
}
