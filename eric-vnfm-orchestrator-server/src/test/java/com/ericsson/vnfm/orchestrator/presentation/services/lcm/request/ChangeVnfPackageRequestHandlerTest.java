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
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.createScaleInfoEntity;
import static com.ericsson.vnfm.orchestrator.TestUtils.createUpgradeRequest;
import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.TestUtils.getVnfInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.LEVEL_ID_NOT_PRESENT_IN_VNFD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_DM_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandlerTest.createHelmChartsFromPackageInfo;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmUpgrade;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Upgrade;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.OperationalState;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ChangeVnfPackageRequestHandlerTest extends AbstractDbSetupTest {

    public static final String INSTANCE_ID = "instanceID";
    private static final String HALL_914_CONFIG = "hall914.config";

    @SpyBean
    private VnfInstanceRepository vnfInstanceRepositorySpy;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @SpyBean
    private InstanceService instanceService;

    @SpyBean
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @SpyBean
    private ChangeVnfPackageService changeVnfPackageService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private VnfdParametersHelper vnfdParametersHelper;

    @SpyBean
    private ExtensionsService extensionsService;

    @MockBean
    private PackageService packageService;

    @Autowired
    private ValuesFileService valuesFileService;

    @SpyBean
    private EvnfmUpgrade evnfmUpgrade;

    @SpyBean
    private Upgrade upgrade;

    @BeforeEach
    public void setup() {
        when(workflowRoutingService.getHelmVersionsRequest()).thenReturn(getHelmVersionsResponse());
    }

    @Test
    public void testDefaultValuesFromUpgradeAddedWhenUpgrade() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("skipVerification", true);

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest =
                TestUtils.createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a", additionalParameters);
        changeCurrentVnfPkgRequest.setExtensions(createExtensions());

        HashMap<String, Object> additionalParametersBefore = new HashMap<>(additionalParameters);
        changeVnfPackageRequestHandler.formatParameters(vnfInstance, changeCurrentVnfPkgRequest, CHANGE_VNFPKG, new HashMap<>());

        verify(vnfdParametersHelper).mergeDefaultParameters(anyString(), any(), any(), any());
        assertThat(additionalParameters.size()).isGreaterThan(additionalParametersBefore.size());
        assertThat(additionalParameters).containsEntry("test.default.upgrade.property", "defaultUpgradeValue");
        assertThat(additionalParameters).doesNotContainKey("test.rollback.param");
    }

    @Test
    public void testDefaultValuesFromRollbackPolicyAddedWhenDowngrade() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("skipVerification", true);

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest =
                TestUtils.createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a", additionalParameters);
        changeCurrentVnfPkgRequest.setExtensions(createExtensions());

        doReturn(Optional.of(LifecycleOperation.builder().build()))
                .when(changeVnfPackageService).getSuitableTargetDowngradeOperationFromVnfInstance(any(), any());

        HashMap<String, Object> additionalParametersBefore = new HashMap<>(additionalParameters);
        changeVnfPackageRequestHandler.formatParameters(vnfInstance, changeCurrentVnfPkgRequest, CHANGE_VNFPKG, new HashMap<>());

        verify(vnfdParametersHelper, times(0)).mergeDefaultParameters(anyString(), any(), any(), any());
        assertThat(additionalParameters.size()).isGreaterThan(additionalParametersBefore.size());
        assertThat(additionalParameters).containsEntry("test.rollback.param", "defaultRollbackValue");
        assertThat(additionalParameters).doesNotContainKey("test.default.upgrade.property");
    }

    @Test
    public void testDefaultValuesHaveLowerPriorityThenValuesYaml() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createPackageResponse());

        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("skipVerification", true);

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest =
                TestUtils.createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a", additionalParameters);
        changeCurrentVnfPkgRequest.setExtensions(createExtensions());

        Map<String, Object> valuesYaml =
                createMutableMap("test",
                        createMutableMap("default",
                                createMutableMap("upgrade",
                                        createMutableMap("property", "propertyFromValuesYaml"))));

        HashMap<String, Object> additionalParametersBefore = new HashMap<>(additionalParameters);

        changeVnfPackageRequestHandler.formatParameters(vnfInstance, changeCurrentVnfPkgRequest, CHANGE_VNFPKG, valuesYaml);

        verify(vnfdParametersHelper).mergeDefaultParameters(anyString(), any(), any(), any());
        assertThat(additionalParameters.size()).isGreaterThan(additionalParametersBefore.size());
        assertThat(additionalParameters).doesNotContainKey("test.default.upgrade.property");
    }

    @Test
    public void testSpecificValidationExtensionsInRequestShouldSetExtensionsToTempInstance() {
        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        VnfInstance vnfInstance = createRequestAndCallSpecificValidation("", true, "UPDATED-SCALING");

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"CISMControlled\","
                                                                                              + "\"Payload_2\":\"CISMControlled\"}}");
        assertThat(tempInstance.getInstantiationLevel()).isEqualTo(TestUtils.INST_LEVEL_1);
    }

    @Test
    public void testSpecificValidationNoScalingPoliciesShouldNotSetExtensionsAndInstantiationLevelToTempInstance() {
        whenOnboardingRespondsWithVnfd("UPDATED-SCALING", "change-vnfpkg/updated-scaling-topology-template-vnfd.json");
        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-topology-template-descriptor-model.json");

        VnfInstance vnfInstance = createRequestAndCallSpecificValidation("", true, "UPDATED-SCALING");

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions())
                .isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        assertThat(tempInstance.getInstantiationLevel()).isNullOrEmpty();
    }

    @Test
    public void testSpecificValidationNoExtensionsInRequestShouldSetDefaultExtensionToTempInstance() {
        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        VnfInstance vnfInstance = createRequestAndCallSpecificValidation("", false, "UPDATED-SCALING");

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getInstantiationLevel()).isEqualTo(TestUtils.INST_LEVEL_1);
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\","
                                                                                              + "\"Payload_2\":\"CISMControlled\"}}");
    }

    @Test
    public void testSpecificValidationCurrentInstanceHasInstantiationLevelAndIsDefinedInTargetShouldSetCurrentLevelToTarget() {
        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        VnfInstance vnfInstance = createRequestAndCallSpecificValidation(TestUtils.INST_LEVEL_2, false, "UPDATED-SCALING");

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance.getInstantiationLevel()).isEqualTo("instantiation_level_2");
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\","
                                                                                              + "\"Payload_2\":\"CISMControlled\"}}");
    }

    @Test
    public void testSpecificValidationCurrentInstanceHasInstantiationLevelAndIsNotDefinedInTargetShouldError() {
        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        assertThatThrownBy(() -> createRequestAndCallSpecificValidation("invalid_instantiationLevel", false, "UPDATED-SCALING"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining(String.format(LEVEL_ID_NOT_PRESENT_IN_VNFD, "invalid_instantiationLevel"));
    }

    @Test
    public void testCreateTempInstanceWithDisabledPackageShouldRaiseError() {
        final String vnfdId = "multi-helm-chart-disabled";
        whenOnboardingRespondsWithDescriptor(vnfdId, "change-vnfpkg/vnfPackageInDisabledState.json");

        assertThatThrownBy(() -> createRequestAndCallSpecificValidation("", false, vnfdId))
                .isInstanceOf(UnprocessablePackageException.class)
                .hasMessageContaining(String.format("Package %s rejected due to its %s state", vnfdId, OperationalState.DISABLED));
    }

    @Test
    public void testSendRequestWithUpgradePatternExecutesUpgradePatternFlow() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();

        //target package
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("UPGRADE-PATTERN", changePackageAdditionalParameter);

        VnfInstance vnfInstance = getVnfInstance();
        HelmChart helmChart1 = getHelmChart("sample-helm1.tgz", "sample-helm1", vnfInstance, 1, HelmChartType.CNF);
        helmChart1.setHelmChartArtifactKey("helm_package1");
        HelmChart helmChart2 = getHelmChart("sample-helm2.tgz", "sample-helm2", vnfInstance, 1, HelmChartType.CNF);
        helmChart2.setHelmChartArtifactKey("helm_package2");
        vnfInstance.setHelmCharts(List.of(helmChart1, helmChart2));

        // set policies and scale levels to current instance
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_1));
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setInstantiationLevel(TestUtils.INST_LEVEL_1);
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"CISMControlled\", \"Payload_2"
                + "\":\"ManualControlled\"}}");

        vnfInstance = vnfInstanceRepository.save(vnfInstance);

        whenOnboardingRespondsWithDescriptor("UPGRADE-PATTERN", "change-vnfpkg/upgrade-pattern-descriptor-model.json");

        changeVnfPackageRequestHandler.specificValidation(vnfInstance, changeCurrentVnfPkgRequest);
        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance,
                changeCurrentVnfPkgRequest, null,
                LifecycleOperationType.CHANGE_VNFPKG,
                null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                null, HttpStatus.ACCEPTED);

        when(workflowRoutingService.routeToEvnfmWfsUpgrade(any(LifecycleOperation.class), any(HelmChart.class), any(), any()))
                .thenReturn(response);
        whenOnboardingRespondsWithVnfd("UPGRADE-PATTERN", "change-vnfpkg/upgrade-pattern-vnfd.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/upgrade-pattern-vnfd.json");
        whenOnboardingRespondsWithMappingFile("UPGRADE-PATTERN", "change-vnfpkg/updated-scaling-mapping-file.json");

        changeVnfPackageRequestHandler.updateInstance(vnfInstance, changeCurrentVnfPkgRequest, LifecycleOperationType.CHANGE_VNFPKG, operation, null);
        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        verify(evnfmUpgrade, times(1)).execute(any());
        verify(upgrade, times(1)).execute(any(), any(), anyBoolean());
        verify(workflowRoutingService, times(0))
                .routeChangePackageInfoRequest(any(ChangeOperationContext.class), anyInt());
    }

    @Test
    public void testSendRequestWithoutUpgradePatternExecutesLegacyFlow() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();

        //target package
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("UPGRADE-PATTERN", changePackageAdditionalParameter);

        VnfInstance vnfInstance = getVnfInstance();
        HelmChart helmChart1 = getHelmChart("sample-helm1.tgz", "sample-helm1", vnfInstance, 1, HelmChartType.CNF);
        helmChart1.setHelmChartArtifactKey("helm_package1");
        HelmChart helmChart2 = getHelmChart("sample-helm2.tgz", "sample-helm2", vnfInstance, 1, HelmChartType.CNF);
        helmChart2.setHelmChartArtifactKey("helm_package2");
        vnfInstance.setHelmCharts(List.of(helmChart1, helmChart2));

        // set policies and scale levels to current instance
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_1));
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setInstantiationLevel(TestUtils.INST_LEVEL_1);
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"CISMControlled\", \"Payload_2"
                + "\":\"ManualControlled\"}}");

        vnfInstance = vnfInstanceRepository.save(vnfInstance);

        whenOnboardingRespondsWithDescriptor("UPGRADE-PATTERN", "change-vnfpkg/upgrade-pattern-descriptor-model.json");

        changeVnfPackageRequestHandler.specificValidation(vnfInstance, changeCurrentVnfPkgRequest);
        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance,
                changeCurrentVnfPkgRequest, null,
                LifecycleOperationType.CHANGE_VNFPKG,
                null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                null, HttpStatus.ACCEPTED);

        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);
        whenOnboardingRespondsWithVnfd("UPGRADE-PATTERN", "change-vnfpkg/upgrade-pattern-vnfd-1.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/upgrade-pattern-vnfd-1.json");
        whenOnboardingRespondsWithMappingFile("UPGRADE-PATTERN", "change-vnfpkg/updated-scaling-mapping-file.json");

        changeVnfPackageRequestHandler.updateInstance(vnfInstance, changeCurrentVnfPkgRequest, LifecycleOperationType.CHANGE_VNFPKG, operation, null);
        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        verify(evnfmUpgrade, times(0)).execute(any());
        verify(workflowRoutingService, times(1))
                .routeChangePackageInfoRequest(any(ChangeOperationContext.class), any(), anyInt());
    }

    @Test
    public void testSendRequestSetsReplicaDetailsToHelmChartsCurrentAndTargetHaveInstantiationLevelsAndDifferentScalingModes() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();

        //target package
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("UPDATED-SCALING", changePackageAdditionalParameter);

        VnfInstance vnfInstance = getVnfInstance();

        // set policies and scale levels to current instance
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_1));
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setInstantiationLevel(TestUtils.INST_LEVEL_1);
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"CISMControlled\", \"Payload_2"
                                                                     + "\":\"ManualControlled\"}}");

        vnfInstance = vnfInstanceRepository.save(vnfInstance);

        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        changeVnfPackageRequestHandler.specificValidation(vnfInstance, changeCurrentVnfPkgRequest);
        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance,
                                                                                       changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG,
                                                                                       null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);

        whenOnboardingRespondsWithVnfd("UPDATED-SCALING", "change-vnfpkg/updated-scaling-vnfd.json");
        whenOnboardingRespondsWithMappingFile("UPDATED-SCALING", "change-vnfpkg/updated-scaling-mapping-file.json");

        changeVnfPackageRequestHandler.updateInstance(vnfInstance, changeCurrentVnfPkgRequest, LifecycleOperationType.CHANGE_VNFPKG, operation, null);

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> helmCharts = tempInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, "sample-helm1");
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails.size()).isEqualTo(1);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get(TestUtils.CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(23);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(23);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(3);

        HelmChart packageTwo = getHelmChartByName(helmCharts, "sample-helm2");

        final Map<String, ReplicaDetails> pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails.size()).isEqualTo(2);

        ReplicaDetails plScaledVm = pkg2ReplicaDetails.get(TestUtils.PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(21);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(21);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails tlScaledVm = pkg2ReplicaDetails.get(TestUtils.TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(13);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(13);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRequestSetsReplicaDetailsToHelmChartsCurrentHasLessAspects() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                     changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setPolicies(
                "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                        + "\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level 0-29 maps to 1-30 Payload "
                        + "VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}}}},"
                        + "\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance"
                        + " per scale step)\\n\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}}}}},"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}},"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}");
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\", \"Payload_2"
                                                                     + "\":\"ManualControlled\"}}");

        VnfInstance tempVnfInstance = getVnfInstance();
        tempVnfInstance.setVnfInstanceId(INSTANCE_ID);
        tempVnfInstance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                                            + "\"properties\":{\"aspects\":{\"Payload_3\":{\"name\":\"Payload_3\",\"description\":\"Scale level "
                                            + "maps to one extra instance for delta_3, two extra instances for delta_4\",\"max_scale_level\":5,"
                                            + "\"step_deltas\":[\"delta_1\",\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}}}},\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level "
                                            + "maps to one extra instance\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}}}},\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale "
                                            + "level maps to one extra instance\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"TL_scaled_vm\"]}}}}}}}}},"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}},"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]}}}},"
                                            + "\"allVnfPackageChangePolicy\":{},\"allVduInstantiationLevels\":{},"
                                            + "\"allInstantiationLevels\":{\"InstantiationLevels\":{\"type\":\"tosca.policies.nfv"
                                            + ".InstantiationLevels\",\"properties\":{\"levels\":{\"instantiation_level_3\":{\"scale_info"
                                            + "\":{\"Payload_3\":{\"scale_level\":2}},\"description\":\"Payload_3 has scale level 2. Impacts "
                                            + "JL_scaled_vm.\"},\"instantiation_level_2\":{\"scale_info\":{\"Payload_2\":{\"scale_level\":3}},"
                                            + "\"description\":\"Payload_2 has scale level 3. Impacts TL_scaled_vm.\"},"
                                            + "\"instantiation_level_1\":{\"scale_info\":{\"Payload\":{\"scale_level\":4}},"
                                            + "\"description\":\"Payload has scale level 4. Impacts PL__scaled_vm, CL_scaled_vm.\"}},"
                                            + "\"default_level\":\"instantiation_level_1\"}}}}");
        tempVnfInstance.setInstantiationLevel("instantiation_level_1");
        List<ScaleInfoEntity> tempAllScaleInfoEntity = new ArrayList<>();
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 2));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 5));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_3, 4));
        tempVnfInstance.setScaleInfoEntity(tempAllScaleInfoEntity);
        tempVnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\", \"Payload_2"
                                                                      + "\":\"CISMControlled\", \"Payload_3\":\"ManualControlled\"}}");


        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("targetVnfdId", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/test-vnfd.json");


        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempAfterUpdate = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        final Map<String, Map<String, String>> extensionsAfterUpgrade = readExtensions(tempAfterUpdate);
        assertThat(extensionsAfterUpgrade).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, MANUAL_CONTROLLED),
                        entry(PAYLOAD_2, MANUAL_CONTROLLED),
                        entry(PAYLOAD_3, MANUAL_CONTROLLED)));

        for (ScaleInfoEntity scaleInfoEntity : tempAfterUpdate.getScaleInfoEntity()) {
            if (TestUtils.PAYLOAD.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if (TestUtils.PAYLOAD_2.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if (TestUtils.PAYLOAD_3.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(4);
            }
        }

        HelmChart packageOne = tempAfterUpdate.getHelmCharts().get(0);
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails.size()).isEqualTo(4);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get(TestUtils.PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(21);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(21);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(21);

        ReplicaDetails jlScaledVm = pkg1ReplicaDetails.get(TestUtils.JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(8);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(8);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(8);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get(TestUtils.CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(23);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(23);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(23);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get(TestUtils.TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(6);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(6);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRequestSetsReplicaDetailsToHelmChartsCurrentHasMoreAspects() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                     changePackageAdditionalParameter);

        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                                            + "\"properties\":{\"aspects\":{\"Payload_3\":{\"name\":\"Payload_3\",\"description\":\"Scale level "
                                            + "maps to one extra instance for delta_3, two extra instances for delta_4\",\"max_scale_level\":5,"
                                            + "\"step_deltas\":[\"delta_1\",\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}}}},\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level "
                                            + "maps to one extra instance\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}}}},\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale "
                                            + "level maps to one extra instance\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"TL_scaled_vm\"]}}}}}}}}},"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}},"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]}}}}}");
        List<ScaleInfoEntity> tempAllScaleInfoEntity = new ArrayList<>();
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 2));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 5));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_3, 4));
        vnfInstance.setScaleInfoEntity(tempAllScaleInfoEntity);
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\", \"Payload_2"
                                                                         + "\":\"CISMControlled\", \"Payload_3\":\"ManualControlled\"}}");

        VnfInstance tempVnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        tempVnfInstance.setPolicies(
                "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                        + "\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level 0-29 maps to 1-30 Payload "
                        + "VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}}}},"
                        + "\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance"
                        + " per scale step)\\n\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}}}}},"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}},"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}},"
                        + "\"allVnfPackageChangePolicy\":{},\"allVduInstantiationLevels\":{},"
                        + "\"allInstantiationLevels\":{\"InstantiationLevels\":{\"type\":\"tosca.policies.nfv"
                        + ".InstantiationLevels\",\"properties\":{\"levels\":{\"instantiation_level_3\":{\"scale_info"
                        + "\":{\"Payload_3\":{\"scale_level\":2}},\"description\":\"Payload_3 has scale level 2. Impacts "
                        + "JL_scaled_vm.\"},\"instantiation_level_2\":{\"scale_info\":{\"Payload_2\":{\"scale_level\":3}},"
                        + "\"description\":\"Payload_2 has scale level 3. Impacts TL_scaled_vm.\"},"
                        + "\"instantiation_level_1\":{\"scale_info\":{\"Payload\":{\"scale_level\":4}},"
                        + "\"description\":\"Payload has scale level 4. Impacts PL__scaled_vm, CL_scaled_vm.\"}},"
                        + "\"default_level\":\"instantiation_level_1\"}}}}");
        tempVnfInstance.setInstantiationLevel("instantiation_level_1");
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        tempVnfInstance.setScaleInfoEntity(allScaleInfoEntity);
        tempVnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\", \"Payload_2"
                                                                     + "\":\"ManualControlled\"}}");

        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("targetVnfdId", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/test-vnfd.json");


        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempAfterUpdate = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        final Map<String, Map<String, String>> extensionsAfterUpgrade = readExtensions(tempAfterUpdate);
        assertThat(extensionsAfterUpgrade).hasEntrySatisfying(
                VNF_CONTROLLED_SCALING,
                scaling -> assertThat(scaling).containsOnly(
                        entry(PAYLOAD, MANUAL_CONTROLLED),
                        entry(PAYLOAD_2, CISM_CONTROLLED)));

        for (ScaleInfoEntity scaleInfoEntity : tempAfterUpdate.getScaleInfoEntity()) {
            if (TestUtils.PAYLOAD.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if (TestUtils.PAYLOAD_2.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
        }

        HelmChart packageOne = tempAfterUpdate.getHelmCharts().get(0);
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails.size()).isEqualTo(3);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get(TestUtils.PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(41);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(41);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(41);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get(TestUtils.CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(23);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(23);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(23);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get(TestUtils.TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(21);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(21);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(21);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRequestSetsReplicaDetailsToHelmChartsCurrentAndTargetWithNoInstantiationLevels() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                               changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setPolicies(
                "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                        + "\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level 0-29 maps to 1-30 Payload "
                        + "VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}}}},"
                        + "\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance"
                        + " per scale step)\\n\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}}}}},"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}},"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}");
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        VnfInstance tempInstance = getVnfInstance();
        tempInstance.setVnfInstanceId("targetVnfdId");
        tempInstance.setPolicies(
                "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                        + "\"properties\":{\"aspects\":{\"Payload_3\":{\"name\":\"Payload_3\",\"description\":\"Scale level maps to one extra "
                        + "instance for delta_3, two extra instances for delta_4\",\"max_scale_level\":5,\"step_deltas\":[\"delta_1\",\"delta_2\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1},"
                        + "\"delta_2\":{\"number_of_instances\":2}}},\"targets\":[\"JL_scaled_vm\"],"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}}}}},"
                        + "\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level maps to one extra instance\",\"max_scale_level\":10,"
                        + "\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                        + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                        + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]}}}}},"
                        + "\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale level maps to one extra instance\",\"max_scale_level\":5,"
                        + "\"step_deltas\":[\"delta_2\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies"
                        + ".nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                        + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]}}}}}}}}},"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]},"
                        + "\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}},"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                        + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1},"
                        + "\"delta_2\":{\"number_of_instances\":2}}},\"targets\":[\"JL_scaled_vm\"],"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},"
                        + "\"targets\":[\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]}}}}}");

        List<ScaleInfoEntity> tempAllScaleInfoEntity = new ArrayList<>();
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 3));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 7));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_3, 2));
        tempInstance.setScaleInfoEntity(tempAllScaleInfoEntity);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("targetVnfdId", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/test-vnfd.json");

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempAfterUpdate = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        for (ScaleInfoEntity scaleInfoEntity : tempAfterUpdate.getScaleInfoEntity()) {
            if ("Payload".equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if ("Payload_2".equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(3);
            }
            if ("Payload_3".equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(0);
            }
        }

        HelmChart packageOne = tempAfterUpdate.getHelmCharts().get(0);
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails.size()).isEqualTo(4);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get(TestUtils.PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(21);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(21);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(21);

        ReplicaDetails jlScaledVm = pkg1ReplicaDetails.get(TestUtils.JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get(TestUtils.CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(23);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(23);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(23);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get(TestUtils.TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(4);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRequestSetsReplicaDetailsToHelmChartsCurrentHasNoLevelsTargetHasLevels() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                               changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setPolicies(
                "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                        + "\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level 0-29 maps to 1-30 Payload "
                        + "VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}}}},"
                        + "\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance"
                        + " per scale step)\\n\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}}}}},"
                        + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},"
                        + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                        + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}},"
                        + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]},\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                        + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]}}},"
                        + "\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                        + "\"properties\":{\"aspect\":\"Payload_2\",\"deltas\":{\"delta_2\":{\"number_of_instances\":4}}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca"
                        + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                        + "\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}}}}}");
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 5));
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 3));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        VnfInstance tempVnfInstance = getVnfInstance();
        tempVnfInstance.setVnfInstanceId(INSTANCE_ID);
        tempVnfInstance.setPolicies("{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                                            + "\"properties\":{\"aspects\":{\"Payload_3\":{\"name\":\"Payload_3\",\"description\":\"Scale level "
                                            + "maps to one extra instance for delta_3, two extra instances for delta_4\",\"max_scale_level\":5,"
                                            + "\"step_deltas\":[\"delta_1\",\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}}}},\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level "
                                            + "maps to one extra instance\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}}}},\"Payload_2\":{\"name\":\"Payload_2\",\"description\":\"Scale "
                                            + "level maps to one extra instance\",\"max_scale_level\":5,\"step_deltas\":[\"delta_2\"],"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"TL_scaled_vm\"]}}}}}}}}},"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},\"targets\":[\"CL_scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\"]},"
                                            + "\"Payload_InitialDelta_3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"JL_scaled_vm\"]}},"
                                            + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"PL__scaled_vm\","
                                            + "\"CL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":3}},"
                                            + "\"targets\":[\"CL_scaled_vm\"]},\"Payload_InitialDelta_2\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"PL__scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_3\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_3\","
                                            + "\"deltas\":{\"delta_1\":{\"number_of_instances\":1},\"delta_2\":{\"number_of_instances\":2}}},"
                                            + "\"targets\":[\"JL_scaled_vm\"],\"allInitialDelta\":{\"Payload_InitialDelta_3\":{\"type\":\"tosca"
                                            + ".policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},"
                                            + "\"targets\":[\"JL_scaled_vm\"]}}},\"Payload_ScalingAspectDeltas_1\":{\"type\":\"tosca.policies.nfv"
                                            + ".VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Payload_2\","
                                            + "\"deltas\":{\"delta_2\":{\"number_of_instances\":1}}},\"targets\":[\"TL_scaled_vm\"],"
                                            + "\"allInitialDelta\":{\"Payload_InitialDelta_1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                                            + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"TL_scaled_vm\"]}}}},"
                                            + "\"allVnfPackageChangePolicy\":{},\"allVduInstantiationLevels\":{},"
                                            + "\"allInstantiationLevels\":{\"InstantiationLevels\":{\"type\":\"tosca.policies.nfv"
                                            + ".InstantiationLevels\",\"properties\":{\"levels\":{\"instantiation_level_3\":{\"scale_info"
                                            + "\":{\"Payload_3\":{\"scale_level\":2}},\"description\":\"Payload_3 has scale level 2. Impacts "
                                            + "JL_scaled_vm.\"},\"instantiation_level_2\":{\"scale_info\":{\"Payload_2\":{\"scale_level\":3}},"
                                            + "\"description\":\"Payload_2 has scale level 3. Impacts TL_scaled_vm.\"},"
                                            + "\"instantiation_level_1\":{\"scale_info\":{\"Payload\":{\"scale_level\":4}},"
                                            + "\"description\":\"Payload has scale level 4. Impacts PL__scaled_vm, CL_scaled_vm.\"}},"
                                            + "\"default_level\":\"instantiation_level_1\"}}}}");
        tempVnfInstance.setInstantiationLevel("instantiation_level_1");
        List<ScaleInfoEntity> tempAllScaleInfoEntity = new ArrayList<>();
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD, 2));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_2, 5));
        tempAllScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, TestUtils.PAYLOAD_3, 4));
        tempVnfInstance.setScaleInfoEntity(tempAllScaleInfoEntity);

        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("targetVnfdId", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithVnfd("dummy-package-id", "change-vnfpkg/test-vnfd.json");

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempAfterUpdate = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        for (ScaleInfoEntity scaleInfoEntity : tempAfterUpdate.getScaleInfoEntity()) {
            if (TestUtils.PAYLOAD.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if (TestUtils.PAYLOAD_2.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(5);
            }
            if (TestUtils.PAYLOAD_3.equals(scaleInfoEntity.getAspectId())) {
                assertThat(scaleInfoEntity.getScaleLevel()).isEqualTo(4);
            }
        }

        HelmChart packageOne = tempAfterUpdate.getHelmCharts().get(0);
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails.size()).isEqualTo(4);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get(TestUtils.PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(21);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(21);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(21);

        ReplicaDetails jlScaledVm = pkg1ReplicaDetails.get(TestUtils.JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(8);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(8);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(8);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get(TestUtils.CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(23);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(23);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(23);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get(TestUtils.TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(6);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(6);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(6);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackPatternSavedInTheOperation() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("isAutoRollbackAllowed", false);
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a",
                                                                                               changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        HelmChart chart1 = getHelmChart("test", "release-1", vnfInstance, 1, HelmChartType.CNF);
        chart1.setHelmChartArtifactKey("helm_package1");
        HelmChart chart2 = getHelmChart("test", "release-2", vnfInstance, 1, HelmChartType.CNF);
        chart2.setHelmChartArtifactKey("helm_package2");
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(chart1);
        helmCharts.add(chart2);
        vnfInstance.setHelmCharts(helmCharts);

        VnfInstance tempInstance = getVnfInstance();
        tempInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        tempInstance.setHelmCharts(helmCharts);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperationInstantiate = new LifecycleOperation();
        lifecycleOperationInstantiate.setVnfInstance(vnfInstance);
        lifecycleOperationInstantiate.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationInstantiate.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperationInstantiate.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setTargetVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setInstantiationLevel("instantiation_level_2");
        lifecycleOperationInstantiate.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperationInstantiate.setStartTime(LocalDateTime.now());
        lifecycleOperationInstantiate = databaseInteractionService.persistLifecycleOperation(lifecycleOperationInstantiate);

        LifecycleOperation lifecycleOperationUpgrade = new LifecycleOperation();
        lifecycleOperationUpgrade.setVnfInstance(vnfInstance);
        lifecycleOperationUpgrade.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationUpgrade.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperationUpgrade.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationUpgrade.setTargetVnfdId("multi-rollback-477c-aab3-21cb04e6a378");
        lifecycleOperationUpgrade.setInstantiationLevel("instantiation_level_2");
        lifecycleOperationUpgrade.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperationUpgrade.setStartTime(LocalDateTime.now());
        lifecycleOperationUpgrade = databaseInteractionService.persistLifecycleOperation(lifecycleOperationUpgrade);

        vnfInstance.setAllOperations(Arrays.asList(lifecycleOperationInstantiate, lifecycleOperationUpgrade));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
        ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        changePackageOperationDetails.setOperationOccurrenceId(lifecycleOperationUpgrade.getOperationOccurrenceId());
        changePackageOperationDetailsRepository.save(changePackageOperationDetails);

        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        helmChartHistoryRecord.setLifecycleOperationId(lifecycleOperationInstantiate.getOperationOccurrenceId());
        helmChartHistoryRecord.setHelmChartUrl("helm/url");
        helmChartHistoryRecord.setReleaseName("release-1");
        helmChartHistoryRepository.save(helmChartHistoryRecord);

        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeRollbackRequest(any(), any(), any(), any())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("multi-rollback-477c-aab3-21cb04e6a378", "change-vnfpkg/multi-rollback-descriptor-model-1.json");
        whenOnboardingRespondsWithDescriptor("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-descriptor-model-2.json");
        whenOnboardingRespondsWithVnfd("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-vnfd-1.json");

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);
        LifecycleOperation afterRollback = databaseInteractionService.getLifecycleOperation(operation.getOperationOccurrenceId());
        assertThat(afterRollback.getRollbackPattern()).isNotNull().isEqualTo("[{\"release-1\":\"rollback\"},{\"release-2\":\"delete\"},"
                                                                                     + "{\"release-2\":\"delete_pvc\"},"
                                                                                     + "{\"release-2\":\"install\"}]");
    }

    public void testUpgradePatternSavedInOperation() {

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVnfInstanceValuesFileUpdatedAfterRollback() {
        String combinedValueFile = "{initialParams: value, bro_endpoint_url: \"http://bro.endpoint.url\"}";
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("isAutoRollbackAllowed", false);
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a",
                                                                                               changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        vnfInstance.setBroEndpointUrl("http://upgrade.bro.endpoint.url");
        HelmChart chart1 = getHelmChart("test", "release-1", vnfInstance, 1, HelmChartType.CNF);
        chart1.setHelmChartArtifactKey("helm_package1");
        HelmChart chart2 = getHelmChart("test", "release-2", vnfInstance, 1, HelmChartType.CNF);
        chart2.setHelmChartArtifactKey("helm_package2");
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(chart1);
        helmCharts.add(chart2);
        vnfInstance.setHelmCharts(helmCharts);

        VnfInstance tempInstance = getVnfInstance();
        tempInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        tempInstance.setHelmCharts(helmCharts);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperationInstantiate = new LifecycleOperation();
        lifecycleOperationInstantiate.setVnfInstance(vnfInstance);
        lifecycleOperationInstantiate.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationInstantiate.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperationInstantiate.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setTargetVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setInstantiationLevel("instantiation_level_2");
        lifecycleOperationInstantiate.setStateEnteredTime(LocalDateTime.of(2022, 4, 14, 10, 30, 40));
        lifecycleOperationInstantiate.setStartTime(LocalDateTime.now());
        lifecycleOperationInstantiate.setCombinedValuesFile(combinedValueFile);
        lifecycleOperationInstantiate = databaseInteractionService.persistLifecycleOperation(lifecycleOperationInstantiate);

        LifecycleOperation lifecycleOperationUpgrade = new LifecycleOperation();
        lifecycleOperationUpgrade.setVnfInstance(vnfInstance);
        lifecycleOperationUpgrade.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationUpgrade.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperationUpgrade.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationUpgrade.setTargetVnfdId("multi-rollback-477c-aab3-21cb04e6a378");
        lifecycleOperationUpgrade.setInstantiationLevel("instantiation_level_2");
        lifecycleOperationUpgrade.setStateEnteredTime(LocalDateTime.of(2022, 4, 15, 10, 30, 40));
        lifecycleOperationUpgrade.setStartTime(LocalDateTime.now());
        lifecycleOperationUpgrade = databaseInteractionService.persistLifecycleOperation(lifecycleOperationUpgrade);

        vnfInstance.setAllOperations(Arrays.asList(lifecycleOperationInstantiate, lifecycleOperationUpgrade));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
        ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        changePackageOperationDetails.setOperationOccurrenceId(lifecycleOperationUpgrade.getOperationOccurrenceId());
        changePackageOperationDetailsRepository.save(changePackageOperationDetails);

        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        helmChartHistoryRecord.setLifecycleOperationId(lifecycleOperationInstantiate.getOperationOccurrenceId());
        helmChartHistoryRecord.setHelmChartUrl("helm/url");
        helmChartHistoryRecord.setReleaseName("release-1");
        helmChartHistoryRepository.save(helmChartHistoryRecord);

        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeRollbackRequest(any(), any(), any(), any())).thenReturn(response);

        whenOnboardingRespondsWithVnfd("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-vnfd-1.json");
        whenOnboardingRespondsWithDescriptor("multi-rollback-477c-aab3-21cb04e6a378", "change-vnfpkg/multi-rollback-descriptor-model-1.json");
        whenOnboardingRespondsWithDescriptor("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-descriptor-model-2.json");

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);

        VnfInstance tempInstanceAfterRollback = Utility.parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertEquals(combinedValueFile, tempInstanceAfterRollback.getCombinedValuesFile());
        assertEquals("http://bro.endpoint.url", tempInstanceAfterRollback.getBroEndpointUrl());
    }

    @Test
    public void testSupportedOperationsInRequestShouldSetSupportedOperationsToTempInstanceForItsVnfdId() {
        VnfInstance vnfInstance = createRequestWithSupportedOperationsAndCallSpecificValidation("", true);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertEquals(8, tempInstance.getSupportedOperations().size());
    }

    @Test
    public void testUpdateInstanceResetExtensionsIfNoTargetExtensions() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("test-vnfdid", changePackageAdditionalParameter);

        whenOnboardingRespondsWithDescriptor("test-vnfdid", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "change-vnfpkg/test-descriptor-model.json");

        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance).isNotNull();
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isNull();
        verify(extensionsService, times(1)).setDefaultExtensions(any(), any());
        verify(extensionsService, times(0)).validateVnfControlledScalingExtension(any(), any());
    }

    @Test
    public void testUpdateInstanceSetInstLevelToNullIfNoTargetLevels() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        vnfInstance.setInstantiationLevel(TestUtils.INST_LEVEL_2);
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("test-vnfdid", changePackageAdditionalParameter);

        whenOnboardingRespondsWithDescriptor("test-vnfdid", "change-vnfpkg/test-descriptor-model.json");
        whenOnboardingRespondsWithDescriptor("d3def1ce-4cf4-477c-aab3-21cb04e6a378", "change-vnfpkg/test-descriptor-model.json");

        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstance).isNotNull();
        assertThat(tempInstance.getInstantiationLevel()).isNull();
    }

    @Test
    public void testUpdateInstanceWithHelmClientVersion() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("helm_client_version", "3.8");
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = TestUtils.createUpgradeRequest("UPDATED-SCALING", changePackageAdditionalParameter);

        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        assertThat(parseJson(vnfInstance.getTempInstance(), VnfInstance.class).getHelmClientVersion()).isEqualTo("3.8");
    }

    @Test
    public void testUpdateInstanceWithHelmClientVersionSetsNullWhenNoValueProvided() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = TestUtils.createUpgradeRequest("UPDATED-SCALING", changePackageAdditionalParameter);

        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);

        assertThat(parseJson(vnfInstance.getTempInstance(), VnfInstance.class).getHelmClientVersion()).isNull();
    }

    @Test
    public void testUpdateInstanceWithHelmClientVersionThrowsExceptionWhenVersionIsInvalid() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setInstantiationLevel("");
        vnfInstance.setClusterName("hall914.config");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("helm_client_version", "3.7");

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = TestUtils.createUpgradeRequest("UPDATED-SCALING", changePackageAdditionalParameter);
        String expectedExceptionMessage = "Helm version 3.7 is not supported, available options: [3.8, 3.10, latest]";

        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest));
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPersistOperationWithHelmClientVersionSetsDataToLifecycleOperation() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("helm_client_version", "latest");
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                     changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();

        VnfInstance tempVnfInstance = getVnfInstance();
        tempVnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);
        LifecycleOperation operation = changeVnfPackageRequestHandler.persistOperation(vnfInstance, changeCurrentVnfPkgRequest, null,
                                                                                       LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        assertThat(operation.getHelmClientVersion()).isNotEmpty().isEqualTo("latest");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPersistOperationWithHelmClientVersionDoesNotSetInvalidDataToLifecycleOperation() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("helm_client_version", "3.7");
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("targetVnfdId",
                                                                                     changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();

        VnfInstance tempVnfInstance = getVnfInstance();
        tempVnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
        vnfInstance = vnfInstanceRepository.save(vnfInstance);

        final VnfInstance finalVnfInstance = vnfInstance;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()
            -> changeVnfPackageRequestHandler.persistOperation(finalVnfInstance, changeCurrentVnfPkgRequest, null,
                                                               LifecycleOperationType.CHANGE_VNFPKG, null, "3600"));
        String expectedExceptionMessage = "Helm version 3.7 is not supported, available options: [3.8, 3.10, latest]";
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWhenRollbackHelmClientVersionIsEqualToInitialData() {
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        changePackageAdditionalParameter.put("isAutoRollbackAllowed", false);
        changePackageAdditionalParameter.put("helm_client_version", "latest");

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("multi-rollback-4cf4-477c-aab3-21cb04e6a",
                                                                                     changePackageAdditionalParameter);
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfDescriptorId("multi-rollback-477c-aab3-21cb04e6a378");
        vnfInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        HelmChart chart1 = getHelmChart("test", "release-1", vnfInstance, 1, HelmChartType.CNF);
        chart1.setHelmChartArtifactKey("helm_package1");
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(chart1);
        vnfInstance.setHelmCharts(helmCharts);
        VnfInstance tempInstance = getVnfInstance();
        tempInstance.setVnfPackageId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        tempInstance.setHelmCharts(helmCharts);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperationInstantiate = new LifecycleOperation();
        lifecycleOperationInstantiate.setVnfInstance(vnfInstance);
        lifecycleOperationInstantiate.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationInstantiate.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperationInstantiate.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setTargetVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationInstantiate.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperationInstantiate.setStartTime(LocalDateTime.now());
        lifecycleOperationInstantiate = databaseInteractionService.persistLifecycleOperation(lifecycleOperationInstantiate);
        assertThat(lifecycleOperationInstantiate.getHelmClientVersion()).isNull();

        LifecycleOperation lifecycleOperationUpgrade = new LifecycleOperation();
        lifecycleOperationUpgrade.setVnfInstance(vnfInstance);
        lifecycleOperationUpgrade.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperationUpgrade.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperationUpgrade.setSourceVnfdId("multi-rollback-4cf4-477c-aab3-21cb04e6a");
        lifecycleOperationUpgrade.setTargetVnfdId("multi-rollback-477c-aab3-21cb04e6a378");
        lifecycleOperationUpgrade.setInstantiationLevel("instantiation_level_2");
        lifecycleOperationUpgrade.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperationUpgrade.setStartTime(LocalDateTime.now());
        lifecycleOperationUpgrade = databaseInteractionService.persistLifecycleOperation(lifecycleOperationUpgrade);
        assertThat(lifecycleOperationUpgrade.getHelmClientVersion()).isNull();

        vnfInstance.setAllOperations(Arrays.asList(lifecycleOperationInstantiate, lifecycleOperationUpgrade));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
        ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        changePackageOperationDetails.setOperationOccurrenceId(lifecycleOperationUpgrade.getOperationOccurrenceId());
        changePackageOperationDetailsRepository.save(changePackageOperationDetails);

        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        helmChartHistoryRecord.setLifecycleOperationId(lifecycleOperationInstantiate.getOperationOccurrenceId());
        helmChartHistoryRecord.setHelmChartUrl("helm/url");
        helmChartHistoryRecord.setReleaseName("release-1");
        helmChartHistoryRepository.save(helmChartHistoryRecord);
        LifecycleOperation operation =
            changeVnfPackageRequestHandler.persistOperation(vnfInstance,
                                                            changeCurrentVnfPkgRequest, null,
                                                            LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        assertThat(operation.getHelmClientVersion()).isEqualTo("latest");

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID,
                                                                    null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeRollbackRequest(any(), any(), any(), any())).thenReturn(response);

        whenOnboardingRespondsWithDescriptor("multi-rollback-477c-aab3-21cb04e6a378", "change-vnfpkg/multi-rollback-descriptor-model-1.json");
        whenOnboardingRespondsWithDescriptor("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-descriptor-model-2.json");
        whenOnboardingRespondsWithVnfd("multi-rollback-4cf4-477c-aab3-21cb04e6a", "change-vnfpkg/multi-rollback-vnfd-1.json");

        changeVnfPackageRequestHandler.sendRequest(vnfInstance, operation, changeCurrentVnfPkgRequest, null);
        LifecycleOperation afterRollback = databaseInteractionService.getLifecycleOperation(operation.getOperationOccurrenceId());

        assertThat(afterRollback.getHelmClientVersion()).isNull();
    }

    @Test
    public void testUpdateInstanceWithDeployableModules() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "enabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "disabled");
        defaultExtensions.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_test_scale", "enabled");
        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("test_scale_chart", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm3", false);

        runUpdateInstanceAndAssertResults(defaultExtensions, requestExtensions, expectedCharts,
                                          expectedTerminatedCharts, vnfdFileName, Collections.emptyMap());
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigTrueAndSameDeployableModules() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "disabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "enabled");
        defaultExtensions.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_test_scale", "enabled");
        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm1", false);
        expectedTerminatedCharts.put("test_scale_chart", false);

        runUpdateInstanceAndAssertResults(defaultExtensions, requestExtensions, expectedCharts,
                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigTrueAndNewDeployableModuleInTarget() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "disabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "enabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_test_scale", "enabled");
        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm1", false);

        runUpdateInstanceAndAssertResults(defaultExtensions, requestExtensions, expectedCharts,
                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigTrueAndTargetInstanceNotContainDeployableModules() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModelWithoutDMConfig.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "disabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "enabled");

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();

        runUpdateInstanceAndAssertResultsWithUpdateChartsInSourceInstance(defaultExtensions, expectedCharts,
                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigFalseAndTargetInstanceNotContainDeployableModules() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModelWithoutDMConfig.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "disabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "enabled");

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();

        runUpdateInstanceAndAssertResultsWithUpdateChartsInSourceInstance(defaultExtensions, expectedCharts,
                                                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigTrueAndSourceInstanceNotContainDeployableModules() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> defaultExtensions = new HashMap<>();

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", false);
        expectedCharts.put("test_scale_chart", false);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();

        runUpdateInstanceAndAssertResultsWithUpdateChartsInSourceInstance(defaultExtensions, expectedCharts,
                                                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    @Test
    public void testUpdateInstanceWithDeployableModulesPersistDMConfigTrueAndDifferentSetOfPackagesInTargetInstance() throws Exception {
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModelTargetWithDifferentDeployableModules.json";
        final Map<String, String> defaultExtensions = new HashMap<>();
        defaultExtensions.put("deployable_module_1", "disabled");
        defaultExtensions.put("deployable_module_2", "enabled");
        defaultExtensions.put("deployable_module_3", "enabled");
        defaultExtensions.put("deployable_module_test_scale", "enabled");

        Map<String, Object> additionalParams = Map.of(PERSIST_DM_CONFIG, true);

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", true);
        final Map<String, Boolean> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm2", false);

        runUpdateInstanceAndAssertResults(defaultExtensions, null, expectedCharts,
                                          expectedTerminatedCharts, vnfdFileName, additionalParams);
    }

    private void runUpdateInstanceAndAssertResultsWithUpdateChartsInSourceInstance(Map<String, String> sourceDeployableModules,
                                                                                   Map<String, Boolean> expectedCharts,
                                                                                   Map<String, Boolean> expectedTerminatedCharts,
                                                                                   String vnfdFileName,
                                                                                   Map<String, Object> additionalParams) throws Exception {
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                       PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        sourceVnfInstance.setDeployableModulesSupported(true);
        sourceVnfInstance.getHelmCharts().stream()
                        .filter(helmChart -> expectedCharts.containsKey(helmChart.getHelmChartName()))
                        .forEach(helmChart -> helmChart.setChartEnabled(expectedCharts.get(helmChart.getHelmChartName())));
        sourceVnfInstance.getHelmCharts().stream()
                .filter(helmChart -> expectedTerminatedCharts.containsKey(helmChart.getHelmChartName()))
                .forEach(helmChart -> helmChart.setChartEnabled(expectedTerminatedCharts.get(helmChart.getHelmChartName())));

        runUpdateInstanceAndAssertResults(sourceDeployableModules, null, expectedCharts, expectedTerminatedCharts, vnfdFileName,
                                          additionalParams, sourceVnfInstance);
    }

    private void runUpdateInstanceAndAssertResults(Map<String, String> sourceDeployableModules, Map<String, Object> requestExtensions,
                                                   Map<String, Boolean> expectedCharts, Map<String, Boolean> expectedTerminatedCharts,
                                                   String vnfdFileName, Map<String, Object> additionalParams) throws Exception {
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                       PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(packageInfo, vnfd);
        runUpdateInstanceAndAssertResults(sourceDeployableModules, requestExtensions, expectedCharts, expectedTerminatedCharts, vnfdFileName,
                                          additionalParams, sourceVnfInstance);
    }

    private void runUpdateInstanceAndAssertResults(Map<String, String> sourceDeployableModules, Map<String, Object> requestExtensions,
                                                   Map<String, Boolean> expectedCharts, Map<String, Boolean> expectedTerminatedCharts,
                                                   String vnfdFileName, Map<String, Object> additionalParams,
                                                   VnfInstance sourceVnfInstance) throws Exception {
        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse packageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                       PackageResponse.class);
        packageInfo.setDescriptorModel(vnfd);

        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(packageInfo);

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(packageInfo.getId());
        request.setExtensions(requestExtensions);
        request.setAdditionalParams(additionalParams);

        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");

        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG,
                                                      operation, additionalParams);

        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, Boolean> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);
        final Map<String, Boolean> actualTerminatedTempCharts = tempInstance.getTerminatedHelmCharts().stream()
                .collect(Collectors.toMap(TerminatedHelmChart::getHelmChartName, TerminatedHelmChart::isChartEnabled));
        assertThat(actualTerminatedTempCharts).containsExactlyInAnyOrderEntriesOf(expectedTerminatedCharts);
    }

    @Test
    public void shouldThrownExceptionWhenNoEnabledHelmChartPresent() throws Exception {
        // given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_1", "disabled");
        requestDeployableModules.put("deployable_module_2", "disabled");
        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));

        HelmChart sourceEnabledChart = getHelmChart("test", "release-1", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledChart.setHelmChartArtifactKey("source_helm_package1");
        sourceEnabledChart.setHelmChartName("source_helm_package1");
        sourceEnabledChart.setChartEnabled(true);
        HelmChart sourceDisabledHelmChart1 = getHelmChart("sample-helm1", "sample-helm1-1", sourceVnfInstance, 2, HelmChartType.CNF);
        sourceDisabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceDisabledHelmChart1.setHelmChartName("sample-helm1");
        sourceDisabledHelmChart1.setChartEnabled(false);
        HelmChart sourceDisabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 2, HelmChartType.CNF);
        sourceDisabledHelmChart2.setHelmChartArtifactKey("sample-helm2");
        sourceDisabledHelmChart2.setHelmChartName("sample-helm2");
        sourceDisabledHelmChart2.setChartEnabled(false);
        HelmChart sourceDisabledHelmChart3 = getHelmChart("sample-helm3", "sample-helm3-1", sourceVnfInstance, 3, HelmChartType.CNF);
        sourceDisabledHelmChart3.setHelmChartArtifactKey("sample-helm3");
        sourceDisabledHelmChart3.setHelmChartName("sample-helm3");
        sourceDisabledHelmChart3.setChartEnabled(false);
        HelmChart sourceDisabledHelmChart4 = getHelmChart("test_scale_chart", "test_scale_chart", sourceVnfInstance, 4, HelmChartType.CNF);
        sourceDisabledHelmChart4.setHelmChartArtifactKey("test_scale_chart");
        sourceDisabledHelmChart4.setHelmChartName("test_scale_chart");
        sourceDisabledHelmChart4.setChartEnabled(false);

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledChart, sourceDisabledHelmChart1, sourceDisabledHelmChart2, sourceDisabledHelmChart3,
                                                sourceDisabledHelmChart4));

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID, null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeTerminateRequest(any(), any(), anyString())).thenReturn(response);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        final VnfInstance savedSourceVnfInstance = sourceVnfInstance;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> changeVnfPackageRequestHandler.updateInstance(savedSourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG, operation, null));
        String expectedExceptionMessage = "No enabled Helm Charts present";
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void shouldUpgradeEnabledHelmChartWhenPresentInSourceAndPresentInTargetAndSkipNotInstalledWithDeployableModules() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_test_scale", "disabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));

        HelmChart sourceEnabledHelmChart1 = getHelmChart("sample-helm1", "sample-helm1-1", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceEnabledHelmChart1.setHelmChartName("sample-helm1");
        sourceEnabledHelmChart1.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 2, HelmChartType.CNF);
        sourceEnabledHelmChart2.setHelmChartArtifactKey("sample-helm2");
        sourceEnabledHelmChart2.setHelmChartName("sample-helm2");
        sourceEnabledHelmChart2.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart3 = getHelmChart("sample-helm3", "sample-helm3-1", sourceVnfInstance, 3, HelmChartType.CNF);
        sourceEnabledHelmChart3.setHelmChartArtifactKey("sample-helm3");
        sourceEnabledHelmChart3.setHelmChartName("sample-helm3");
        sourceEnabledHelmChart3.setChartEnabled(false);
        HelmChart sourceDisabledHelmChart4 = getHelmChart("test_scale_chart", "test_scale_chart", sourceVnfInstance, 4, HelmChartType.CNF);
        sourceDisabledHelmChart4.setHelmChartArtifactKey("test_scale_chart");
        sourceDisabledHelmChart4.setHelmChartName("test_scale_chart");
        sourceDisabledHelmChart4.setChartEnabled(false);

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart1, sourceEnabledHelmChart2, sourceEnabledHelmChart3,
                                                sourceDisabledHelmChart4));

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", false);
        expectedCharts.put("test_scale_chart", false);

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID, null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG,
                                                      operation, Collections.emptyMap());
        final var helmChartPriorityCaptor = ArgumentCaptor.forClass(int.class);
        changeVnfPackageRequestHandler.sendRequest(sourceVnfInstance, operation, request, null);

        //then
        verify(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), helmChartPriorityCaptor.capture());

        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, Boolean> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        int helmChartPriorityToUpgrade = helmChartPriorityCaptor.getValue();
        assertEquals(helmChartPriorityToUpgrade, sourceEnabledHelmChart1.getPriority());

        List<TerminatedHelmChart> helmChartsToTerminate = tempInstance.getTerminatedHelmCharts();
        assertThat(helmChartsToTerminate).isEmpty();

        HelmChart skippedHelmChartForProcessing = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> sourceDisabledHelmChart4.getHelmChartName().equals(helmChart.getHelmChartName()))
                .findFirst().get();
        assertThat(skippedHelmChartForProcessing.getState()).isNotNull();
    }

    @Test
    public void shouldUpgradeHelmChartWhenDisabledInSourceAndEnabledInTargetWithDeployableModules() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_3", "enabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));

        HelmChart sourceEnabledHelmChart1 = getHelmChart("sample-helm1", "sample-helm1-1", sourceVnfInstance, 3, HelmChartType.CNF);
        sourceEnabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceEnabledHelmChart1.setHelmChartName("sample-helm1");
        sourceEnabledHelmChart1.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 2, HelmChartType.CNF);
        sourceEnabledHelmChart2.setHelmChartArtifactKey("sample-helm2");
        sourceEnabledHelmChart2.setHelmChartName("sample-helm2");
        sourceEnabledHelmChart2.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart3 = getHelmChart("sample-helm3", "sample-helm3-1", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledHelmChart3.setHelmChartArtifactKey("sample-helm3");
        sourceEnabledHelmChart3.setHelmChartName("sample-helm3");
        sourceEnabledHelmChart3.setChartEnabled(false);
        HelmChart sourceDisabledHelmChart4 = getHelmChart("test_scale_chart", "test_scale_chart", sourceVnfInstance, 4, HelmChartType.CNF);
        sourceDisabledHelmChart4.setHelmChartArtifactKey("test_scale_chart");
        sourceDisabledHelmChart4.setHelmChartName("test_scale_chart");
        sourceDisabledHelmChart4.setChartEnabled(false);

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart1, sourceEnabledHelmChart2, sourceEnabledHelmChart3,
                                                sourceDisabledHelmChart4));

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", false);

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID, null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG,
                                                      operation, Collections.emptyMap());
        final var helmChartPriorityCaptor = ArgumentCaptor.forClass(int.class);
        changeVnfPackageRequestHandler.sendRequest(sourceVnfInstance, operation, request, null);

        //then
        verify(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), helmChartPriorityCaptor.capture());

        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, Boolean> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        int helmChartPriorityToUpgrade = helmChartPriorityCaptor.getValue();
        assertEquals(helmChartPriorityToUpgrade, sourceEnabledHelmChart3.getPriority());

        List<TerminatedHelmChart> helmChartsToTerminate = tempInstance.getTerminatedHelmCharts();
        assertThat(helmChartsToTerminate).isEmpty();

        HelmChart skippedHelmChartForProcessing = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> sourceDisabledHelmChart4.getHelmChartName().equals(helmChart.getHelmChartName()))
                .findFirst().get();
        assertThat(skippedHelmChartForProcessing.getState()).isNotNull();
    }

    @Test
    public void shouldUpgradeHelmChartWhenNotPresentInSourceAndEnabledInTargetWithDeployableModules() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_3", "enabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));

        HelmChart sourceEnabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 2, HelmChartType.CNF);
        sourceEnabledHelmChart2.setHelmChartArtifactKey("sample-helm2");
        sourceEnabledHelmChart2.setHelmChartName("sample-helm2");
        sourceEnabledHelmChart2.setChartEnabled(true);
        HelmChart sourceDisabledHelmChart4 = getHelmChart("test_scale_chart", "test_scale_chart", sourceVnfInstance, 4, HelmChartType.CNF);
        sourceDisabledHelmChart4.setHelmChartArtifactKey("test_scale_chart");
        sourceDisabledHelmChart4.setHelmChartName("test_scale_chart");
        sourceDisabledHelmChart4.setChartEnabled(false);

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart2, sourceDisabledHelmChart4));

        final Map<String, Boolean> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", true);
        expectedCharts.put("sample-helm2", true);
        expectedCharts.put("sample-helm3", true);
        expectedCharts.put("test_scale_chart", false);

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID, null, HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG,
                                                      operation, Collections.emptyMap());
        final var helmChartPriorityCaptor = ArgumentCaptor.forClass(int.class);
        changeVnfPackageRequestHandler.sendRequest(sourceVnfInstance, operation, request, null);

        //then
        verify(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), helmChartPriorityCaptor.capture());

        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, Boolean> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::isChartEnabled));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        int helmChartPriorityToUpgrade = helmChartPriorityCaptor.getValue();
        assertEquals(1, helmChartPriorityToUpgrade);

        List<TerminatedHelmChart> helmChartsToTerminate = tempInstance.getTerminatedHelmCharts();
        assertThat(helmChartsToTerminate).isEmpty();

        HelmChart skippedHelmChartForProcessing = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> sourceDisabledHelmChart4.getHelmChartName().equals(helmChart.getHelmChartName()))
                .findFirst().get();
        assertThat(skippedHelmChartForProcessing.getState()).isNotNull();
    }

    @Test
    public void shouldUpdateReleaseNameWhenUpgradeHelmChart() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_1", "disabled");
        requestDeployableModules.put("deployable_module_test_scale", "enabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
        HelmChart sourceEnabledHelmChart1 = getHelmChart("sample-helm1", "sample-helm1-1", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledHelmChart1.setHelmChartName("sample-helm1");
        sourceEnabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceEnabledHelmChart1.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 3, HelmChartType.CNF);
        sourceEnabledHelmChart2.setHelmChartName("sample-helm2");
        sourceEnabledHelmChart2.setHelmChartArtifactKey("sample-helm2");

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart1, sourceEnabledHelmChart2));

        final Map<String, String> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm2", "sample-helm2-1");
        expectedCharts.put("sample-helm3", "dummy-instance-name-1");
        expectedCharts.put("test_scale_chart", "dummy-instance-name-2");

        final Map<String, String> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm1", "sample-helm1-1");

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG, operation, null);

        //then
        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, String> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::getReleaseName));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        final Map<String, String> actualTerminatedTempCharts = tempInstance.getTerminatedHelmCharts().stream()
                .collect(Collectors.toMap(TerminatedHelmChart::getHelmChartName, TerminatedHelmChart::getReleaseName));
        assertThat(actualTerminatedTempCharts).containsExactlyInAnyOrderEntriesOf(expectedTerminatedCharts);
    }

    @Test
    public void shouldUpdateReleaseNameWithStartSuffixWhenUpgradeHelmChart() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        Map<String, String> requestDeployableModules = new HashMap<>();
        requestDeployableModules.put("deployable_module_1", "disabled");
        requestDeployableModules.put("deployable_module_test_scale", "enabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(requestDeployableModules);

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
        HelmChart sourceEnabledHelmChart1 = getHelmChart("sample-helm1", "dummy-instance-name-10", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledHelmChart1.setHelmChartName("sample-helm1");
        sourceEnabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceEnabledHelmChart1.setChartEnabled(true);
        HelmChart sourceEnabledHelmChart2 = getHelmChart("sample-helm2", "sample-helm2-1", sourceVnfInstance, 3, HelmChartType.CNF);
        sourceEnabledHelmChart2.setHelmChartName("sample-helm2");
        sourceEnabledHelmChart2.setHelmChartArtifactKey("sample-helm2");

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart1, sourceEnabledHelmChart2));

        final Map<String, String> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm2", "sample-helm2-1");
        expectedCharts.put("sample-helm3", "dummy-instance-name-11");
        expectedCharts.put("test_scale_chart", "dummy-instance-name-12");

        final Map<String, String> expectedTerminatedCharts = new HashMap<>();
        expectedTerminatedCharts.put("sample-helm1", "dummy-instance-name-10");

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG, operation, null);

        //then
        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, String> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::getReleaseName));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        final Map<String, String> actualTerminatedTempCharts = tempInstance.getTerminatedHelmCharts().stream()
                .collect(Collectors.toMap(TerminatedHelmChart::getHelmChartName, TerminatedHelmChart::getReleaseName));
        assertThat(actualTerminatedTempCharts).containsExactlyInAnyOrderEntriesOf(expectedTerminatedCharts);
    }

    @Test
    public void shouldCreateTempInstanceWithAppropriatePriorityFromOperationChartPriority() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModelWithPriority.json";
        String targetPackageResponseFileName = "change-vnfpkg/deployableModules/packageResponseForPriority.json";

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), targetPackageResponseFileName),
                                                             PackageResponse.class);
        targetPackageInfo.setOperationalState(OperationalState.ENABLED);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);

        //when
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final Map<String, Integer> chartPriorityMap = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class)
                .getHelmCharts().stream().collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::getPriority));
        assertEquals( Integer.valueOf(3), chartPriorityMap.get("sample-helm1"));
        assertEquals( Integer.valueOf(1), chartPriorityMap.get("sample-helm2"));
        assertEquals( Integer.valueOf(2), chartPriorityMap.get("sample-helm3"));
    }

    @Test
    public void shouldUpdateReleaseNameWithStartSuffixForNewChartWhenUpgradeHelmChartWithFirstNotGeneratedSuffix() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";
        final Map<String, String> sourceDeployableModules = new HashMap<>();
        sourceDeployableModules.put("deployable_module_1", "enabled");
        sourceDeployableModules.put("deployable_module_2", "enabled");
        sourceDeployableModules.put("deployable_module_3", "disabled");
        sourceDeployableModules.put("deployable_module_test_scale", "disabled");

        final Map<String, Object> requestExtensions = createExtensionsWithDeployableModules(new HashMap<>());

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        Map<String, Map<String, String>> extensions = Map.of(DEPLOYABLE_MODULES, sourceDeployableModules);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
        HelmChart sourceEnabledHelmChart1 = getHelmChart("sample-helm1", "dummy-instance-name", sourceVnfInstance, 1, HelmChartType.CNF);
        sourceEnabledHelmChart1.setHelmChartName("sample-helm1");
        sourceEnabledHelmChart1.setHelmChartArtifactKey("sample-helm1");
        sourceEnabledHelmChart1.setChartEnabled(true);

        sourceVnfInstance.setHelmCharts(List.of(sourceEnabledHelmChart1));

        final Map<String, String> expectedCharts = new HashMap<>();
        expectedCharts.put("sample-helm1", "dummy-instance-name");
        expectedCharts.put("sample-helm2", "dummy-instance-name-2");
        expectedCharts.put("sample-helm3", "dummy-instance-name-3");
        expectedCharts.put("test_scale_chart", "dummy-instance-name-4");

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(requestExtensions);

        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG, operation, null);

        //then
        VnfInstance tempInstance = parseJson(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, String> actualTempCharts = tempInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getHelmChartName, HelmChart::getReleaseName));
        assertThat(actualTempCharts).containsExactlyInAnyOrderEntriesOf(expectedCharts);

        final Map<String, String> actualTerminatedTempCharts = tempInstance.getTerminatedHelmCharts().stream()
                .collect(Collectors.toMap(TerminatedHelmChart::getHelmChartName, TerminatedHelmChart::getReleaseName));
        assertThat(actualTerminatedTempCharts).isEmpty();
    }

    @Test
    public void shouldResetTempInstanceWhenFailedToRouteRequestToWfs() throws Exception {
        //given
        String vnfdFileName = "change-vnfpkg/deployableModules/descriptorModel.json";

        String vnfd = readDataFromFile(getClass(), vnfdFileName);
        PackageResponse targetPackageInfo = mapper.readValue(readDataFromFile(getClass(), "change-vnfpkg/deployableModules/packageResponse.json"),
                                                             PackageResponse.class);
        targetPackageInfo.setDescriptorModel(vnfd);

        VnfInstance sourceVnfInstance = createVnfInstanceForUpdate(targetPackageInfo, vnfd);
        sourceVnfInstance.setVnfInfoModifiableAttributesExtensions("{}");

        HelmChart chart1 = getHelmChart("test", "release-1", sourceVnfInstance, 1, HelmChartType.CNF);
        chart1.setHelmChartArtifactKey("helm_package1");

        sourceVnfInstance.setHelmCharts(List.of(chart1));

        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(targetPackageInfo.getId());
        request.setExtensions(new HashMap<>());

        WorkflowRoutingResponse response = TestUtils.createResponse(INSTANCE_ID, "Failed to send", HttpStatus.BAD_REQUEST);
        when(workflowRoutingService.routeChangePackageInfoRequest(any(), any(), anyInt())).thenReturn(response);
        when(packageService.getVnfd(any())).thenReturn(new JSONObject(vnfd));
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(targetPackageInfo);
        doReturn(Collections.emptyList()).when(instanceService).getHelmChartCommandUpgradePattern(any(), any());

        //when
        sourceVnfInstance = databaseInteractionService.saveVnfInstanceToDB(sourceVnfInstance);
        changeVnfPackageRequestHandler.createTempInstance(sourceVnfInstance, request);
        final LifecycleOperation operation = changeVnfPackageRequestHandler
                .persistOperation(sourceVnfInstance, request, null, LifecycleOperationType.CHANGE_VNFPKG, null, "3600");
        changeVnfPackageRequestHandler.updateInstance(sourceVnfInstance, request, LifecycleOperationType.CHANGE_VNFPKG,
                                                      operation, Collections.emptyMap());
        changeVnfPackageRequestHandler.sendRequest(sourceVnfInstance, operation, request, null);

        //then
        verify(workflowRoutingService).routeChangePackageInfoRequest(any(), any(), anyInt());
        assertNull(sourceVnfInstance.getTempInstance());
        assertEquals(LifecycleOperationState.ROLLED_BACK, operation.getOperationState());
    }

    private VnfInstance createVnfInstanceForUpdate(PackageResponse packageInfo, String vnfd) throws Exception {
        Policies policies = PolicyUtility.createPolicies(new JSONObject(vnfd));

        VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setPolicies(mapper.writeValueAsString(policies));
        vnfInstance.setClusterName(HALL_914_CONFIG);
        vnfInstance.setOssTopology("{}");
        vnfInstance.setHelmCharts(createHelmChartsFromPackageInfo(packageInfo, vnfInstance));
        List<ScaleInfoEntity> allScaleInfoEntity = new ArrayList<>();
        allScaleInfoEntity.add(createScaleInfoEntity(vnfInstance, "Payload", 2));
        vnfInstance.setScaleInfoEntity(allScaleInfoEntity);

        return vnfInstance;
    }

    private VnfInstance createRequestAndCallSpecificValidation(final String currentInstanceLevel, boolean setExtensions, final String vnfdId) {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel(currentInstanceLevel);
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));
        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = TestUtils.createUpgradeRequest(vnfdId, changePackageAdditionalParameter);
        changeCurrentVnfPkgRequest.setExtensions(setExtensions ? createExtensions() : null);
        changeVnfPackageRequestHandler.specificValidation(vnfInstance, changeCurrentVnfPkgRequest);
        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);
        return vnfInstance;
    }

    private VnfInstance createRequestWithSupportedOperationsAndCallSpecificValidation(final String currentInstanceLevel, boolean setExtensions) {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setInstantiationLevel(currentInstanceLevel);
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\"}}");
        vnfInstance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        when(vnfInstanceRepositorySpy.findById(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstance));

        whenOnboardingRespondsWithDescriptor("UPDATED-SCALING", "change-vnfpkg/updated-scaling-descriptor-model.json");
        whenOnboardingRespondsWithSupportedOperations("UPDATED-SCALING", "change-vnfpkg/supported-operations.json");

        Map<String, Object> changePackageAdditionalParameter = createAdditionalParameter();
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = createUpgradeRequest("UPDATED-SCALING", changePackageAdditionalParameter);
        changeCurrentVnfPkgRequest.setExtensions(setExtensions ? createExtensions() : null);
        changeVnfPackageRequestHandler.specificValidation(vnfInstance, changeCurrentVnfPkgRequest);
        changeVnfPackageRequestHandler.createTempInstance(vnfInstance, changeCurrentVnfPkgRequest);
        return vnfInstance;
    }

    private static Map<String, Object> createExtensions() {
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put(TestUtils.PAYLOAD, CISM_CONTROLLED);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);
        return extensions;
    }

    private static Map<String, Object> createExtensionsWithDeployableModules(Map<String, String> deployableModules) {
        final Map<String, Object> extensions = createExtensions();
        extensions.put(DEPLOYABLE_MODULES, deployableModules);
        return extensions;
    }

    private Map<String, Object> createAdditionalParameter() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("skipVerification", true);
        additionalParameter.put("applicationTimeOut", "340");
        additionalParameter.put("skipJobVerification", true);
        additionalParameter.put("overrideGlobalRegistry", "true");
        return additionalParameter;
    }

    private PackageResponse createPackageResponse() {
        final String vnfdString = TestUtils.readDataFromFile(getClass(), "change-vnfpkg/multi-rollback-vnfd-1.json");
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }

    private void whenOnboardingRespondsWithDescriptor(final String packageId, final String fileName) {
        when(packageService.getPackageInfoWithDescriptorModel(eq(packageId))).thenReturn(readObject(fileName, PackageResponse.class));
    }

    private void whenOnboardingRespondsWithVnfd(final String vnfdId, final String fileName) {
        when(packageService.getVnfd(eq(vnfdId))).thenReturn(new JSONObject(readFile(fileName)));
    }

    private void whenOnboardingRespondsWithMappingFile(final String vnfdId, final String fileName) {
        when(packageService.getScalingMapping(eq(vnfdId), anyString())).thenReturn(readObject(fileName, new TypeReference<>() {
        }));
    }

    private void whenOnboardingRespondsWithSupportedOperations(final String packageId, final String fileName) {
        when(packageService.getSupportedOperations(eq(packageId))).thenReturn(readObject(fileName, new TypeReference<>() {
        }));
    }

    private HelmVersionsResponse getHelmVersionsResponse() {
        List<String> helmVersions = Arrays.asList("3.8", "3.10", "latest");

        HelmVersionsResponse helmVersionsResponse = new HelmVersionsResponse();
        helmVersionsResponse.setHelmVersions(helmVersions);

        return helmVersionsResponse;
    }

    private static Map<String, Map<String, String>> readExtensions(VnfInstance instance) {
        return parseJsonToGenericType(
                instance.getVnfInfoModifiableAttributesExtensions(),
                new TypeReference<>() {
                });
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readFile(fileName), targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T readObject(final String fileName, final TypeReference<T> targetType) {
        try {
            return mapper.readValue(readFile(fileName), targetType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }

    private static <K, T> Map<K, T> createMutableMap(K key, T value) {
        Map<K, T> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
