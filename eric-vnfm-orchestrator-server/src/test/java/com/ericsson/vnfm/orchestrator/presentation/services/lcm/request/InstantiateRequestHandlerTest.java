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

import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ClusterConfigFileNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DuplicateCombinationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationAlreadyInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.DeployableModulesServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.DefaultReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ScaleMappingReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleLevelCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleParametersServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.sync.SyncOperationValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.createScaleInfoEntity;
import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEPLOYABLE_MODULE_VALUES_INVALID_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALID_DEPLOYABLE_MODULE_VALUES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.EXTENSIONS_SHOULD_BE_KEY_VALUE_PAIR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VNF_CONTROLLED_SCALING_SHOULD_BE_KEY_VALUE_PAIR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_INSTANCE_IS_BEING_PROCESSED;
import static com.ericsson.vnfm.orchestrator.presentation.services.InstanceServiceTopologyTest.getOssToplogy;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifecycleManagementServiceTest.getVnfInstance;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;


@SpringBootTest(classes = {
        ObjectMapper.class,
        InstantiateRequestHandler.class,
        ReplicaDetailsMapper.class,
        ExtensionsServiceImpl.class,
        InstantiationLevelServiceImpl.class,
        LifeCycleManagementHelper.class,
        WorkflowRoutingServicePassThrough.class,
        ReplicaDetailsServiceImpl.class,
        ScaleServiceImpl.class,
        MappingFileServiceImpl.class,
        VnfInstanceServiceImpl.class,
        ScaleParametersServiceImpl.class,
        ScaleLevelCalculationServiceImpl.class,
        InstantiateVnfRequestValidatingServiceImpl.class,
        ReplicaCountCalculationServiceImpl.class,
        ReplicaDetailsBuilder.class,
        ExtensionsMapper.class,
        DefaultReplicaDetailsCalculationService.class,
        ScaleMappingReplicaDetailsCalculationService.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        DeployableModulesServiceImpl.class,
        CryptoUtils.class,
        CMPEnrollmentHelper.class
})
@MockBean(classes = {
        ClusterConfigInstanceRepository.class,
        LifecycleOperationRepository.class,
        HelmChartRepository.class,
        Day0ConfigurationService.class,
        ValuesFileComposer.class,
        UsernameCalculationService.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        GrantingResourceDefinitionCalculation.class,
        SyncOperationValidator.class,
        YamlMapFactoryBean.class,
        CryptoService.class,
        WorkflowRequestBodyBuilder.class,
        RestTemplate.class,
        ValuesFileService.class,
        HelmChartHelper.class,
        LifecycleOperationHelper.class,
        LcmOpSearchService.class,
        NetworkDataTypeValidationService.class
})
@MockBean(classes = RetryTemplate.class, name = "wfsRoutingRetryTemplate")
public class InstantiateRequestHandlerTest {

    public static final String INSTANCE_ID = "instanceID";
    private static final String HALL_914_CONFIG = "hall914.config";

    @Autowired
    private InstantiateRequestHandler instantiateRequestHandler;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private ExtensionsService extensionsService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private LcmOpSearchService lcmOpSearchService;

    @MockBean
    private HelmClientVersionValidator helmClientVersionValidator;

    @MockBean
    private InstantiationLevelService instantiationLevelService;

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private EnmMetricsExposers enmMetricsExposers;

    @SpyBean
    WorkflowRoutingServicePassThrough workflowRoutingService;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepositorySpy;

    @SpyBean
    private VnfdParametersHelper vnfdParametersHelper;

    private static final String EMPTY_JSON = "{}";

    public static String getInstantiateOssTopology() {
        return "{\"disableLdapUser\":{\"defaultValue\":\"true\",\"type\":\"boolean\",\"required\":\"false\"},"
                + "\"second\":{\"defaultValue\":\"false\"}}";
    }

    private static final TypeReference<HashMap<String, ReplicaDetails>> REPLICA_DETAILS_MAP_TYPE_REF = new TypeReference<>() {
    };

    private static final String ERIC_PM_BULK_REPORTER = "eric-pm-bulk-reporter";

    @SuppressWarnings("unchecked")
    @Test
    public void testSetInstantiateOssTopology() {
        VnfInstance instance = getVnfInstance();
        instance.setOssTopology(getOssToplogy());
        InstantiateVnfRequest request = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        InstantiateRequestHandler.setOssTopology(instance, (Map<String, Object>) request.getAdditionalParams(), new HashMap<>());
        assertThat(instance.getOssTopology()).isEqualTo(getOssToplogy());
        assertThat(instance.getInstantiateOssTopology()).isEqualTo(getInstantiateOssTopology());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetInstantiateOssTopologyWithEmptyOssTopology() {
        VnfInstance instance = getVnfInstance();
        instance.setOssTopology(EMPTY_JSON);
        InstantiateVnfRequest request = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        InstantiateRequestHandler.setOssTopology(instance, (Map<String, Object>) request.getAdditionalParams(), new HashMap<>());
        assertThat(instance.getOssTopology()).isEqualTo(EMPTY_JSON);
        assertThat(instance.getInstantiateOssTopology())
                .isEqualTo("{\"disableLdapUser\":{\"defaultValue\":\"true\"},\"second\":{\"defaultValue\":\"false\"}}");
    }

    @Test
    public void testSetInstantiateOssTopologyWithEmptyOssTopologyAndEmptyAdditionalParams() {
        VnfInstance instance = getVnfInstance();
        instance.setOssTopology(EMPTY_JSON);
        InstantiateRequestHandler.setOssTopology(instance, new HashMap<>(), new HashMap<>());
        assertThat(instance.getOssTopology()).isEqualTo(EMPTY_JSON);
        assertThat(instance.getInstantiateOssTopology()).isEqualTo(EMPTY_JSON);
    }

    @Test
    public void testSetInstantiateOssTopologyWithOssTopologyAndEmptyAdditionalParams() {
        VnfInstance instance = getVnfInstance();
        instance.setOssTopology(getOssToplogy());
        InstantiateRequestHandler.setOssTopology(instance, new HashMap<>(), new HashMap<>());
        assertThat(instance.getOssTopology()).isEqualTo(getOssToplogy());
        assertThat(instance.getInstantiateOssTopology()).isEqualTo(getOssToplogy());
    }

    @Test
    public void testUpdateInstance() {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setNamespace("testchangepackage");
        vnfInstance.setClusterName("instantiate-1266");
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(List.of(createHelmChart(1, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(2, LifecycleOperationState.COMPLETED.toString())));
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(INST_LEVEL_1));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Aspect1", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate",
                constructInstantiateAdditionalParameters("testinstantiate"));
        Map<String, Integer> scaleLevelInfoMap =
                Map.of("Aspect1", 2, "Aspect3", 4, "Aspect5", 6);
        request.setInstantiationLevelId(null);
        request.setTargetScaleLevelInfo(TestUtils.createTargetScaleLevelInfo(scaleLevelInfoMap));

        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");

        String vnfd = readDataFromFile(getClass(), "instantiate/descriptorModel.json");

        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(anyString())).thenReturn("eric-crd-ns");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);

        verifyUpdatedInstance(vnfInstance, 2, "testinstantiate", "instantiate-1266", "eric-crd-ns");
        verifyInstantiationLevelIsSet(vnfInstance, scaleLevelInfoMap);
    }

    @Test
    public void testDefaultValuesFromInstantiateAddedWhenInstantiate() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("skipVerification", true);

        InstantiateVnfRequest instantiateRequest =
                TestUtils.createInstantiateRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a", additionalParameters);
        instantiateRequest.setExtensions(createExtensions());

        HashMap<String, Object> additionalParametersBefore = new HashMap<>(additionalParameters);
        instantiateRequestHandler.formatParameters(vnfInstance, instantiateRequest, INSTANTIATE, new HashMap<>());

        verify(vnfdParametersHelper).mergeDefaultParameters(anyString(), any(), any(), any());
        assertThat(additionalParameters.size()).isGreaterThan(additionalParametersBefore.size());
        assertThat(additionalParameters).containsEntry("test.instantiate.param", "defaultInstantiateValue");
    }

    @Test
    public void testDefaultValuesHaveLowerPriorityThenValuesYaml() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("skipVerification", true);

        InstantiateVnfRequest instantiateRequest =
                TestUtils.createInstantiateRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a", additionalParameters);
        instantiateRequest.setExtensions(createExtensions());

        Map<String, Object> valuesYaml =
                createMutableMap("test",
                        createMutableMap("instantiate",
                                createMutableMap("param", "propertyFromValuesYaml")));

        HashMap<String, Object> additionalParametersBefore = new HashMap<>(additionalParameters);

        instantiateRequestHandler.formatParameters(vnfInstance, instantiateRequest, INSTANTIATE, valuesYaml);

        verify(vnfdParametersHelper).mergeDefaultParameters(anyString(), any(), any(), any());
        assertThat(additionalParameters.size()).isGreaterThan(additionalParametersBefore.size());
        assertThat(additionalParameters).doesNotContainKey("test.instantiate.param");
    }

    @Test
    public void testFormatParametersGeneratesOTPSecretWhenCMPv2Enabled() throws IOException, URISyntaxException {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setNamespace("testchangepackage");
        vnfInstance.setClusterName("instantiate-1266");
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(List.of(createHelmChart(1, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(2, LifecycleOperationState.COMPLETED.toString())));
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(INST_LEVEL_1));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Aspect1", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        Map<String, Object> additionalParams = constructInstantiateAdditionalParameters("testinstantiate");
        additionalParams.put("ipVersion", "ipV4");
        additionalParams.put("addNodeToOSS", "true");
        additionalParams.put("CMPv2Enrollment", "true");

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate", additionalParams);
        Map<String, Integer> scaleLevelInfoMap =
                Map.of("Aspect1", 2, "Aspect3", 4, "Aspect5", 6);
        request.setInstantiationLevelId(null);
        request.setTargetScaleLevelInfo(TestUtils.createTargetScaleLevelInfo(scaleLevelInfoMap));

        String expectedLdapConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validLdapConfig.json");
        String expectedEnrollmentConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentConfig.json");
        when(ossNodeService.generateLDAPServerConfiguration(any(), any())).thenReturn(expectedLdapConfiguration);
        when(ossNodeService.generateCertificateEnrollmentConfiguration(any())).thenReturn(expectedEnrollmentConfiguration);

        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        Map<String, Object> resultAdditionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, new HashMap<>());

        verify(ossNodeService).addNode(vnfInstance);
        assertTrue(additionalParams.containsKey(DAY0_CONFIGURATION_SECRETS));
        Map<String, Object> secrets = (Map<String, Object>) additionalParams.get(DAY0_CONFIGURATION_SECRETS);
        assertEquals(2, secrets.size());
        assertTrue(secrets.containsKey(EXTERNAL_LDAP_SECRET_NAME));
        assertTrue(secrets.containsKey(ENROLLMENT_CERTM_SECRET_NAME));
        assertEquals(Map.of(EXTERNAL_LDAP_SECRET_KEY, expectedLdapConfiguration), secrets.get(EXTERNAL_LDAP_SECRET_NAME));
        assertEquals(Map.of(ENROLLMENT_CERTM_SECRET_KEY, expectedEnrollmentConfiguration), secrets.get(ENROLLMENT_CERTM_SECRET_NAME));
    }

    @Test
    public void testFormatParametersSkipsOTPSecretGenerationWhenCMPv2Disabled() {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setNamespace("testchangepackage");
        vnfInstance.setClusterName("instantiate-1266");
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(List.of(createHelmChart(1, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(2, LifecycleOperationState.COMPLETED.toString())));
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(INST_LEVEL_1));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Aspect1", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        Map<String, Object> additionalParams = constructInstantiateAdditionalParameters("testinstantiate");
        additionalParams.put("ipVersion", "ipV4");
        additionalParams.put("addNodeToOSS", "true");
        additionalParams.put("CMPv2Enrollment", "false");

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate", additionalParams);
        Map<String, Integer> scaleLevelInfoMap =
                Map.of("Aspect1", 2, "Aspect3", 4, "Aspect5", 6);
        request.setInstantiationLevelId(null);
        request.setTargetScaleLevelInfo(TestUtils.createTargetScaleLevelInfo(scaleLevelInfoMap));

        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        Map<String, Object> resultAdditionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, new HashMap<>());

        verifyNoInteractions(ossNodeService);
        assertFalse(resultAdditionalParams.containsKey(DAY0_CONFIGURATION_SECRETS));
        assertNull(vnfInstance.getOssNodeProtocolFile());
    }

    @Test
    public void testUpdateInstanceWithDefaultDeployableModulesExtension() throws Exception {
        Map<String, Object> defaultExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "enabled");
        deployableModules.put("deployable_module_3", "disabled");
        defaultExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", false);

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        runUpdateInstanceAndAssertResults(defaultExtensions, null, expectedCharts, vnfdFileName);
    }

    @Test
    public void testUpdateInstanceWithDefaultDeployableModulesExtensionDeployableModuleTwoDisabled() throws Exception {
        Map<String, Object> defaultExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "disabled");
        deployableModules.put("deployable_module_3", "disabled");
        defaultExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", false);

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        runUpdateInstanceAndAssertResults(defaultExtensions, null, expectedCharts, vnfdFileName);
    }

    @Test
    public void testUpdateInstanceWithDefaultDeployableModulesExtensionOnlyDeployableModuleThreeEnabled() throws Exception {
        Map<String, Object> defaultExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "disabled");
        deployableModules.put("deployable_module_2", "disabled");
        deployableModules.put("deployable_module_3", "enabled");
        defaultExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", false);
        expectedCharts.put("sample-helm2", false);
        expectedCharts.put("sample-helm3", true);

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        runUpdateInstanceAndAssertResults(defaultExtensions, null, expectedCharts, vnfdFileName);
    }

    @Test
    public void testUpdateInstanceWithRequestExtensionsDeployableModuleThreeEnabled() throws Exception {
        Map<String, Object> defaultExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "enabled");
        deployableModules.put("deployable_module_3", "disabled");
        defaultExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);

        final Map<String, Object> requestExtensions = createDeployableModuleExtension();

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        runUpdateInstanceAndAssertResults(defaultExtensions, requestExtensions, expectedCharts, vnfdFileName);
    }

    @Test
    public void shouldThrowExceptionWhenUpdateInstanceWithNonExistentDeployableModule() throws Exception {
        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("non_existent_deployable_module", "enabled");
        requestExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "instantiate/deployableModules/packageResponse.json"),
                PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        VnfInstance vnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        vnfInstance.setDeployableModulesSupported(true);

        mockServicesForUpdateInstance(vnfInstance, vnfd, packageInfo);

        InstantiateVnfRequest request = TestUtils
                .createInstantiateRequest("testinstantiate", constructInstantiateAdditionalParameters("testinstantiate"));
        request.setExtensions(requestExtensions);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> instantiateRequestHandler.specificValidation(vnfInstance, request));
        String expectedExceptionMessage = String.format("Deployable modules %s are not present in VNFD ID instanceID",
                deployableModules.keySet());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenUpdateInstanceWithInvalidDeployableModuleValuesInRequestExtensions() throws Exception {
        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_2", "i_want_to_enable_it");
        deployableModules.put("deployable_module_1", "i_want_to_disable_it");
        deployableModules.put("deployable_module_3", "enabled");
        requestExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        List<String> invalidDeployableModulesValues = List.of("i_want_to_enable_it", "i_want_to_disable_it");

        String vnfdFileName = "instantiate/deployableModules/descriptorModel.json";
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "instantiate/deployableModules/packageResponse.json"),
                PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        VnfInstance vnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        vnfInstance.setDeployableModulesSupported(true);

        mockServicesForUpdateInstance(vnfInstance, vnfd, packageInfo);

        InstantiateVnfRequest request = TestUtils
                .createInstantiateRequest("testinstantiate", constructInstantiateAdditionalParameters("testinstantiate"));
        request.setExtensions(requestExtensions);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> instantiateRequestHandler.specificValidation(vnfInstance, request));
        String expectedExceptionMessage = String.format(DEPLOYABLE_MODULE_VALUES_INVALID_ERROR,
                invalidDeployableModulesValues, VALID_DEPLOYABLE_MODULE_VALUES);
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenUpdateInstanceWithDisabledSingleHelmChart() throws Exception {
        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "disabled");
        requestExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        String vnfdFileName = "instantiate/deployableModules/descriptorModelWithSingleHelmChart.json";
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "instantiate/deployableModules/packageResponse.json"),
                PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartArtifactKey("helm_package1");
        helmChart.setHelmChartType(HelmChartType.CNF);
        helmChart.setHelmChartName("test-scale-chart");

        VnfInstance vnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        vnfInstance.setHelmCharts(List.of(helmChart));
        vnfInstance.setDeployableModulesSupported(true);

        mockServicesForUpdateInstance(vnfInstance, vnfd, packageInfo);

        InstantiateVnfRequest request = TestUtils
                .createInstantiateRequest("testinstantiate", constructInstantiateAdditionalParameters("testinstantiate"));
        request.setExtensions(requestExtensions);

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");

        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams));
        String expectedExceptionMessage = "No enabled Helm Charts present";
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }


    @Test
    public void testUpdateInstanceWithDefaultDeployableModulesExtensionWithMandatoryCharts() throws Exception {
        Map<String, Object> defaultExtensions = new HashMap<>();
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "disabled");
        deployableModules.put("deployable_module_2", "enabled");
        defaultExtensions.put(DEPLOYABLE_MODULES, deployableModules);

        Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", false);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);

        String vnfdFileName = "instantiate/deployableModules/descriptorModelWithMandatoryCharts.json";
        runUpdateInstanceAndAssertResults(defaultExtensions, null, expectedCharts, vnfdFileName);
    }

    @Test
    public void testUpdateInstanceWithVnfControlledScaling() throws Exception {
        String vnfd = readDataFromFile(getClass(), "instantiate/descriptorModelUpdatedScaling.json");
        Policies policies = PolicyUtility.createPolicies(new JSONObject(vnfd));
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "instantiate/packageResponse.json"),
                PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put("Payload", MANUAL_CONTROLLED);
        vnfControlledScaling.put("Payload_2", CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(mapper.writeValueAsString(policies));
        vnfInstance.setClusterName(HALL_914_CONFIG);
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(createHelmChartsFromPackageInfo(packageInfo, vnfInstance));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Payload", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions(Utility.convertObjToJsonString(extensions));

        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());
        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(vnfInstance);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate",
                constructInstantiateAdditionalParameters("testinstantiate"));
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null,
                        INSTANTIATE, null, "3600");
        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);

        assertThat(vnfInstance.getHelmCharts().size()).isEqualTo(2);
        assertThat(vnfInstance.getHelmCharts().get(0).getReplicaDetails()).isNull();

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getHelmCharts().size()).isEqualTo(2);
        Map<String, ReplicaDetails> tempReplicaDetailsFirstHelmChart =
                replicaDetailsMapper.getReplicaDetailsFromHelmChart(tempInstance.getHelmCharts().get(0));

        ReplicaDetails clScaledVm = tempReplicaDetailsFirstHelmChart.get("CL_scaled_vm");
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(3);
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(3);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(11);

        Map<String, ReplicaDetails> tempReplicaDetailsSecondHelmChart =
                replicaDetailsMapper.getReplicaDetailsFromHelmChart(tempInstance.getHelmCharts().get(1));

        ReplicaDetails tlScaledVm = tempReplicaDetailsSecondHelmChart.get("TL_scaled_vm");
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);

        ReplicaDetails pLScaledVm = tempReplicaDetailsSecondHelmChart.get("PL__scaled_vm");
        assertThat(pLScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(pLScaledVm.getMinReplicasCount()).isEqualTo(1);
        assertThat(pLScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(pLScaledVm.getCurrentReplicaCount()).isEqualTo(9);
    }

    @Test
    public void testPositiveInstantiateVnfRequestWithExtensions() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);
        instance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel("instantiation_level_2"));

        Map<String, Object> extensions = createExtensions();
        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(extensions);

        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(instance);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        instantiateRequestHandler.specificValidation(instance, instantiateRequest);
    }

    @Test
    public void testPositiveInstantiateVnfRequestWithWrongFormatExtensions() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);

        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(100);

        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(instance);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(instance, instantiateRequest))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(EXTENSIONS_SHOULD_BE_KEY_VALUE_PAIR);
    }

    @Test
    public void testPositiveInstantiateVnfRequestWithWrongVnfControlledScaling() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);

        Map<String, Object> extensions = createExtensions();
        extensions.put(VNF_CONTROLLED_SCALING, 100);

        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(extensions);

        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(instance);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(instance, instantiateRequest))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(VNF_CONTROLLED_SCALING_SHOULD_BE_KEY_VALUE_PAIR);
    }

    @Test
    public void testPositiveInstantiateVnfRequestWithWrongAspect() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);
        instance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel("instantiation_level_2"));

        Map<String, Object> extensions = createExtensions();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put("Aspect1", "xyz");
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(extensions);

        when(databaseInteractionService.getVnfInstance(instance.getVnfInstanceId())).thenReturn(instance);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(instance, instantiateRequest))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE);
    }

    @Test
    public void testPositiveInstantiateVnfRequestAspectNotMatchWithPolicy() {
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);
        instance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel("instantiation_level_2"));

        Map<String, Object> extensions = createExtensions();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put("Aspect2", MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(extensions);

        when(databaseInteractionService.getVnfInstance(instance.getVnfInstanceId())).thenReturn(instance);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(instance, instantiateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY);
    }

    @Test
    public void testPositiveInstantiateVnfRequestDuplicateInstance() {
        // given
        VnfInstance instance = getVnfInstance();
        instance.setVnfInstanceId(INSTANCE_ID);
        instance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel("instantiation_level_2"));

        Map<String, Object> extensions = createExtensions();
        InstantiateVnfRequest instantiateRequest = createInstantiateVnfRequestBody("my-namespace", HALL_914_CONFIG);
        instantiateRequest.setExtensions(extensions);

        // when and then
        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(instance);
        when(databaseInteractionService.getDuplicateInstances(anyString(), anyString(), anyString())).thenReturn(1);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(instance, instantiateRequest))
                .isInstanceOf(DuplicateCombinationException.class)
                .hasMessage("Duplicate combination of resource instance name, target cluster server and namespace.");
    }

    @Test
    public void testSendRequestFailedForWFSBadRequest() throws Exception {
        HelmChart helmChart = createHelmChart(1, LifecycleOperationState.PROCESSING.toString());

        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setHelmCharts(List.of(helmChart));

        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setVnfInstance(vnfInstance);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.name", "testTopology");
        additionalParams.put(NAMESPACE, "testinstantiate");

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName("hall914");
        request.setAdditionalParams(additionalParams);

        final WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage("Workflow service is unavailable or unable to process the request");
        response.setHttpStatus(HttpStatus.BAD_REQUEST);

        doReturn(response).when(workflowRoutingService).routeInstantiateRequest(any(), any(), any(InstantiateVnfRequest.class));

        when(databaseInteractionService.getClusterConfigByName(vnfInstance.getClusterName()))
                .thenReturn(Optional.of(createClusterConfig(vnfInstance.getClusterName(), null)));

        instantiateRequestHandler.sendRequest(vnfInstance, operation, request, null);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        ProblemDetails problemDetails = mapper.readValue(operation.getError(), ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .isEqualTo("Workflow service is unavailable or unable to process the request");
    }

    @Test
    public void testUpdateReplicaDetailsNoMaxReplicaParam() throws JsonProcessingException {
        HelmChart chart = createHelmChartWithReplicaDetails(ERIC_PM_BULK_REPORTER, true, false);

        Map<String, Integer> resourceDetails = new HashMap<>();
        resourceDetails.put(ERIC_PM_BULK_REPORTER, 1);

        InstantiateRequestHandler.updateChartReplicaDetails(resourceDetails, chart);

        Map<String, ReplicaDetails> chartReplicas = mapper.readValue(chart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE_REF);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getMaxReplicasCount()).isEqualTo(0);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getCurrentReplicaCount()).isEqualTo(1);
    }

    @Test
    public void testUpdateReplicaDetailsAutoScaleFalse() throws JsonProcessingException {
        HelmChart chart = createHelmChartWithReplicaDetails(ERIC_PM_BULK_REPORTER, false, true);
        Map<String, Integer> resourceDetails = new HashMap<>();
        resourceDetails.put(ERIC_PM_BULK_REPORTER, 1);

        InstantiateRequestHandler.updateChartReplicaDetails(resourceDetails, chart);

        Map<String, ReplicaDetails> chartReplicas = mapper.readValue(chart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE_REF);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getMaxReplicasCount()).isEqualTo(0);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getCurrentReplicaCount()).isEqualTo(1);
    }

    @Test
    public void testUpdateReplicaDetails() throws JsonProcessingException {
        HelmChart chart = createHelmChartWithReplicaDetails(ERIC_PM_BULK_REPORTER, true, true);
        Map<String, Integer> resourceDetails = new HashMap<>();
        resourceDetails.put(ERIC_PM_BULK_REPORTER, 1);

        InstantiateRequestHandler.updateChartReplicaDetails(resourceDetails, chart);

        Map<String, ReplicaDetails> chartReplicas = mapper.readValue(chart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE_REF);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getMaxReplicasCount()).isEqualTo(1);
        assertThat(chartReplicas.get(ERIC_PM_BULK_REPORTER).getCurrentReplicaCount()).isEqualTo(1);
    }

    @Test
    public void testCrdNamespaceForExternalCluster() throws Exception {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setClusterName("crdcluster");
        vnfInstance.setNamespace("cnf-with-crd-ns");
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(List.of(createHelmChart(1, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(2, LifecycleOperationState.COMPLETED.toString())));
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(INST_LEVEL_1));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Aspect1", 5));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("crdcluster",
                constructInstantiateAdditionalParameters("cnf-with-crd-ns"));

        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        String vnfd = readDataFromFile(getClass(), "instantiate/descriptorModelUpdatedScaling.json");
        extensionsService.setDefaultExtensions(vnfInstance, vnfd);
        request.setExtensions(createExtensions());

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");

        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(anyString())).thenReturn("multi-cluster-crd-ns");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);
        verifyUpdatedInstance(vnfInstance, 2, "cnf-with-crd-ns", "crdcluster", "multi-cluster-crd-ns");
    }

    @Test
    public void testCrdNamespaceForDefaultCluster() {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setNamespace("cnf-with-crd-ns");
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(List.of(createHelmChart(1, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(2, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(3, LifecycleOperationState.COMPLETED.toString()),
                createHelmChart(4, LifecycleOperationState.COMPLETED.toString())));
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(INST_LEVEL_1));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Aspect1", 5));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("cnf-with-crd-ns",
                constructInstantiateAdditionalParameters("cnf-with-crd-ns"));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");

        String vnfd = readDataFromFile(getClass(), "instantiate/descriptorModel.json");

        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(anyString())).thenReturn("eric-crd-ns");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);
        verifyUpdatedInstance(vnfInstance, 4, "cnf-with-crd-ns", HALL_914_CONFIG, "eric-crd-ns");
    }

    @Test
    public void testValidationFailsIfVnfInstanceAlreadyUsed() {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setVnfInstanceId(INSTANCE_ID);

        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(vnfInstance);
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(any())).thenReturn(1);

        assertThatThrownBy(() -> instantiateRequestHandler.specificValidation(vnfInstance, new InstantiateVnfRequest()))
                .isInstanceOf(OperationAlreadyInProgressException.class)
                .hasMessage(String.format(VNF_INSTANCE_IS_BEING_PROCESSED, INSTANCE_ID));
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidationCaseNoParamProvided() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", null);

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        instantiateRequestHandler.specificValidation(vnfInstance, request);

        assertThat(vnfInstance.getHelmClientVersion()).isNull();
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidationCaseInvalidVersion() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.8.1");

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(helmClientVersionValidator.validateAndGetHelmClientVersion(any())).thenThrow(IllegalArgumentException.class);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThrows(IllegalArgumentException.class,
                () -> instantiateRequestHandler.specificValidation(vnfInstance, request));
        ;
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidationLatest() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "latest");
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(helmClientVersionValidator.validateAndGetHelmClientVersion(any())).thenReturn("latest");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        instantiateRequestHandler.specificValidation(vnfInstance, request);

        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("latest");
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidationWhenExactVersionPassed() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.10");
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(helmClientVersionValidator.validateAndGetHelmClientVersion(any())).thenReturn("3.10");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        instantiateRequestHandler.specificValidation(vnfInstance, request);

        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.10");
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidation() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.8");
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(helmClientVersionValidator.validateAndGetHelmClientVersion(any())).thenReturn("3.8");
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        instantiateRequestHandler.specificValidation(vnfInstance, request);

        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");
    }

    @Test
    public void testInstantiateWithHelmClientVersionValidationSavesVersionToLifecycleOperation() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.8");

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);

        ClusterConfigFile file = new ClusterConfigFile();
        file.setName(randomAlphabetic(8));

        when(helmClientVersionValidator.validateAndGetHelmClientVersion(any())).thenReturn("3.8");

        var resultOperation = instantiateRequestHandler.persistOperation(vnfInstance,
                request,
                null,
                INSTANTIATE,
                null, "3600");

        assertThat(resultOperation.getHelmClientVersion()).isEqualTo("3.8");
    }

    @Test
    public void testInstantiateWithHelmClientNoHelmClientVersionPassed() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        String defaultClusterName = randomAlphabetic(8);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.of(defaultClusterName));
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());
        instantiateRequestHandler.specificValidation(vnfInstance, request);

        assertThat(vnfInstance.getHelmClientVersion()).isNull();
    }

    @Test
    public void testInstantiateWithDefaultClusterFileNotFound() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("helm_client_version", "3.8.1");

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.empty());
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        ClusterConfigFileNotFoundException exception = assertThrows(ClusterConfigFileNotFoundException.class,
                () -> instantiateRequestHandler.specificValidation(vnfInstance, request));
        String expectedExceptionMessage = "At least one cluster config has to be registered in order to proceed with LCM operations.";
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testUpdateScaleInfoWithInstantiationLevelsInPoliciesWithInstantiationLevelInRequest() throws Exception {
        String policies = TestUtils.readDataFromFile("com/ericsson/vnfm/orchestrator/presentation/services/vnfInstancePoliciesWithInstantiationLevels.json");
        VnfInstance vnfInstance = createInstanceForScaleInfoTest(policies);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate",
                constructInstantiateAdditionalParameters("testinstantiate"));
        request.setInstantiationLevelId("instantiation_level_1");

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "dummy-ns");

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(readDataFromFile(getClass(), "instantiate/descriptorModel.json")));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);

        verify(instantiationLevelService, times(1)).setScaleLevelForInstantiationLevel(any(), any(), any());
        verify(instantiationLevelService, never()).setScaleLevelForTargetScaleLevelInfo(any(), any(), any());
        verify(instantiationLevelService, never()).setDefaultInstantiationLevelToVnfInstance(any());
    }

    @Test
    public void testUpdateScaleInfoWithoutInstantiationLevelsInPoliciesWithTargetScaleLevelInfo() throws Exception {
        String policies = TestUtils.readDataFromFile("com/ericsson/vnfm/orchestrator/presentation/services/vnfInstancePoliciesWithoutInstantiationLevels.json");
        VnfInstance vnfInstance = createInstanceForScaleInfoTest(policies);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate",
                constructInstantiateAdditionalParameters("testinstantiate"));
        request.setTargetScaleLevelInfo(List.of(
                new ScaleInfo().aspectId("aspect-1").scaleLevel(1)
        ));
        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "dummy-ns");

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(readDataFromFile(getClass(), "instantiate/descriptorModel.json")));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);

        verify(instantiationLevelService, never()).setScaleLevelForInstantiationLevel(any(), any(), any());
        verify(instantiationLevelService, times(1)).setScaleLevelForTargetScaleLevelInfo(any(), any(), any());
        verify(instantiationLevelService, never()).setDefaultInstantiationLevelToVnfInstance(any());
    }

    @Test
    public void testUpdateScaleInfoWithInstantiationLevelsInPoliciesWithDefaultRequest() throws Exception {
        String policies = TestUtils.readDataFromFile("com/ericsson/vnfm/orchestrator/presentation/services/vnfInstancePoliciesWithInstantiationLevels.json");
        VnfInstance vnfInstance = createInstanceForScaleInfoTest(policies);

        InstantiateVnfRequest request = TestUtils.createInstantiateRequest("testinstantiate",
                constructInstantiateAdditionalParameters("testinstantiate"));

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null, INSTANTIATE, null, "3600");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("namespace", "dummy-ns");

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(readDataFromFile(getClass(), "instantiate/descriptorModel.json")));

        instantiateRequestHandler.createTempInstance(vnfInstance, request);
        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);

        verify(instantiationLevelService, never()).setScaleLevelForInstantiationLevel(any(), any(), any());
        verify(instantiationLevelService, never()).setScaleLevelForTargetScaleLevelInfo(any(), any(), any());
        verify(instantiationLevelService, times(1)).setDefaultInstantiationLevelToVnfInstance(any());
    }

    @Test
    public void testInstantiateWithSkipVerificationAndAddNodeToOssAndCMPv2EnrollmentEnabled() {
        VnfInstance vnfInstance = createVnfInstance(false);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(SKIP_VERIFICATION, true);
        additionalParams.put(CMP_V2_ENROLLMENT, true);
        additionalParams.put(ADD_NODE_TO_OSS, true);

        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(additionalParams);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(databaseInteractionService.
                getLifecycleOperationsByVnfInstance(vnfInstance)).thenReturn(List.of(operation));
        when(databaseInteractionService.getDefaultClusterName()).thenReturn(Optional.empty());
        when(packageService.getVnfd(any())).thenReturn(new JSONObject());

        assertThrows(InvalidInputException.class,
                () -> instantiateRequestHandler.specificValidation(vnfInstance, request),
                "Both parameters 'skipVerification' and 'CMPv2Enrollment' cannot be true 'addNodeToOss is true'");
    }

    private VnfInstance createInstanceForScaleInfoTest(String policies) {
        VnfInstance vnfInstance= TestUtils.getVnfInstance();
        vnfInstance.setPolicies(policies);

        List<ScaleInfoEntity> allScaleInfoEntity = List.of(
                createScaleInfoEntity(vnfInstance, "aspect-1", 0),
                createScaleInfoEntity(vnfInstance, "aspect-2", 0),
                createScaleInfoEntity(vnfInstance, "aspect-3", 0)
        );
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        return vnfInstance;
    }

    private VnfInstance createVnfInstanceForUpdate(PackageResponse packageInfo, String vnfd) throws Exception {
        Policies policies = PolicyUtility.createPolicies(new JSONObject(vnfd));

        VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(mapper.writeValueAsString(policies));
        vnfInstance.setClusterName(HALL_914_CONFIG);
        vnfInstance.setOssTopology(EMPTY_JSON);
        vnfInstance.setHelmCharts(createHelmChartsFromPackageInfo(packageInfo, vnfInstance));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Payload", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        return vnfInstance;
    }

    private void mockServicesForUpdateInstance(VnfInstance vnfInstance, String vnfd, PackageResponse packageInfo) {
        when(databaseInteractionService.getVnfInstance(INSTANCE_ID)).thenReturn(vnfInstance);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(packageInfo);
        when(clusterConfigService.getConfigFileByName(HALL_914_CONFIG))
                .thenReturn(createClusterConfig(HALL_914_CONFIG, null));
    }

    private void executeUpdateInstance(VnfInstance vnfInstance, Object request) {
        Map<String, Object> additionalParams = instantiateRequestHandler
                .formatParameters(vnfInstance, request, INSTANTIATE, null);

        LifecycleOperation operation = instantiateRequestHandler
                .persistOperation(vnfInstance, request, null,
                        INSTANTIATE, null, "3600");
        instantiateRequestHandler.createTempInstance(vnfInstance, request);

        instantiateRequestHandler.updateInstance(vnfInstance, request, INSTANTIATE, operation, additionalParams);
    }

    private void runUpdateInstanceAndAssertResults(Map<String, Object> defaultExtensions,
                                                   Map<String, Object> requestExtensions,
                                                   Map<String, Boolean> expectedCharts, String vnfdFileName) throws Exception {
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "instantiate/deployableModules/packageResponse.json"),
                PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        VnfInstance vnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(defaultExtensions));

        mockServicesForUpdateInstance(vnfInstance, vnfd, packageInfo);

        InstantiateVnfRequest request = TestUtils
                .createInstantiateRequest("testinstantiate", constructInstantiateAdditionalParameters("testinstantiate"));
        request.setExtensions(requestExtensions);

        executeUpdateInstance(vnfInstance, request);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        final Map<String, Boolean> actualCharts = vnfInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));
        final Map<String, Boolean> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));

        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);
        assertThat(actualCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);
    }

    private void verifyUpdatedInstance(VnfInstance vnfInstance, int chartSize, String namespace, String cluster, String crdNamespace) {
        assertThat(vnfInstance.getHelmCharts().size()).isEqualTo(chartSize);
        assertThat(vnfInstance.getHelmCharts()).extracting("state").containsOnlyNulls();
        assertThat(vnfInstance.getCombinedAdditionalParams()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNull();
        assertThat(vnfInstance.getInstantiateOssTopology()).contains("testTopology");
        assertThat(vnfInstance.getNamespace()).isEqualTo(namespace);
        assertThat(vnfInstance.getClusterName()).isEqualTo(cluster);
        assertThat(vnfInstance.isCleanUpResources()).isTrue();
        assertThat(vnfInstance.getCrdNamespace()).isEqualTo(crdNamespace);
    }

    private void verifyInstantiationLevelIsSet(VnfInstance vnfInstance, Map<String, Integer> scaleLevelInfoMap) {
        assertThat(vnfInstance.getInstantiationLevel()).isNull();

        for (ScaleInfoEntity scaleInfoEntity : vnfInstance.getScaleInfoEntity()) {
            String aspectId = scaleInfoEntity.getAspectId();
            int scaleLevel = scaleInfoEntity.getScaleLevel();

            assertThat(scaleLevel).isEqualTo(Optional.ofNullable(scaleLevelInfoMap.get(aspectId)).orElse(0));
        }
    }

    private Map<String, Object> constructInstantiateAdditionalParameters(String namespace) {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology", Collections.singletonMap("name", "testTopology"));
        additionalParams.put(NAMESPACE, namespace);
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        additionalParams.put(HELM_NO_HOOKS, true);
        return additionalParams;
    }

    private HelmChart createHelmChartWithReplicaDetails(final String targetName,
                                                        final boolean autoScale, final boolean setMaxParam) throws JsonProcessingException {
        ReplicaDetails replicaDetails = ReplicaDetails.builder()
                .withMinReplicasParameterName(targetName + ".minReplicas")
                .withMinReplicasCount(0)
                .withMaxReplicasParameterName(targetName + ".maxReplicas")
                .withMaxReplicasCount(0)
                .withScalingParameterName(targetName + ".replicaCounts")
                .withCurrentReplicaCount(0)
                .withAutoScalingEnabledValue(autoScale)
                .build();
        if (!setMaxParam) {
            replicaDetails.setMaxReplicasParameterName(null);
        }
        HelmChart chart = new HelmChart();
        Map<String, ReplicaDetails> replicaDetailsMap = new HashMap<>();
        replicaDetailsMap.put(targetName, replicaDetails);
        chart.setReplicaDetails(mapper.writeValueAsString(replicaDetailsMap));
        return chart;
    }

    private InstantiateVnfRequest createInstantiateVnfRequestBody(String namespace, String clusterName) {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> ossTopologyMap = new HashMap<>();
        ossTopologyMap.put("disableLdapUser", "true");
        ossTopologyMap.put("second", "false");
        additionalParams.put(NAMESPACE, namespace);
        additionalParams.put("ossTopology", ossTopologyMap);
        additionalParams.put("third", "false");

        request.setAdditionalParams(additionalParams);
        request.setClusterName(clusterName);
        return request;
    }

    private Map<String, Object> createExtensions() {
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put("Aspect1", CISM_CONTROLLED);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);
        return extensions;
    }

    private Map<String, Object> createDeployableModuleExtension() {
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_3", "enabled");
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(DEPLOYABLE_MODULES, deployableModules);
        return extensions;
    }

    private ClusterConfigFile createClusterConfig(String name, String crdNamespace) {
        ClusterConfigFile configFile = new ClusterConfigFile();
        configFile.setName(name);
        configFile.setStatus(ConfigFileStatus.NOT_IN_USE);
        configFile.setCrdNamespace(crdNamespace);
        configFile.setContent(StringUtils.EMPTY);
        configFile.setClusterServer(StringUtils.EMPTY);
        return configFile;
    }

    private VnfInstance createTempInstance(final VnfInstance vnfInstance) {
        VnfInstance tempInstance = new VnfInstance();
        BeanUtils.copyProperties(vnfInstance, tempInstance, "tempInstance");
        tempInstance.getHelmCharts().forEach(obj -> obj.setState(null));
        return tempInstance;
    }

    private static HelmChart toHelmChart(final HelmPackage helmPackage,
                                         final VnfInstance vnfInstance) {

        final var chart = new HelmChart();

        chart.setId(UUID.randomUUID().toString());
        chart.setHelmChartUrl(helmPackage.getChartUrl());
        chart.setPriority(helmPackage.getPriority());
        chart.setReleaseName("releaseName");
        chart.setHelmChartName(helmPackage.getChartName());
        chart.setHelmChartVersion(helmPackage.getChartVersion());
        chart.setHelmChartType(helmPackage.getChartType());
        chart.setHelmChartArtifactKey(helmPackage.getChartArtifactKey());
        chart.setVnfInstance(vnfInstance);

        return chart;
    }

    public static List<HelmChart> createHelmChartsFromPackageInfo(final PackageResponse packageInfo,
                                                                  final VnfInstance vnfInstance) {

        return packageInfo.getHelmPackageUrls().stream()
                .map(helmPackage -> toHelmChart(helmPackage, vnfInstance))
                .collect(toList());
    }

    private HelmChart createHelmChart(int priority, String state) {
        HelmChart helmChart = new HelmChart();
        helmChart.setId(UUID.randomUUID().toString());
        helmChart.setHelmChartName("helm_chart" + priority);
        helmChart.setPriority(priority);
        helmChart.setState(state);
        helmChart.setHelmChartType(HelmChartType.CNF);
        return helmChart;
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "instantiate/descriptorModel.json");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }

    private static <K, T> Map<K, T> createMutableMap(K key, T value) {
        Map<K, T> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static void verifyInteractionsForScaleInfoUpdate(InstantiationLevelService instantiationLevelService) {

    }
}
