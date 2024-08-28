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

import static java.util.Collections.emptyList;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.DEFAULT_CLUSTER_FILE_NOT_FOUND;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.REPLICA_DETAILS_MAP_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANO_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.services.InstanceService.setInstanceWithSitebasicFile;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.INSTANTIATE_OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.checkVnfInstanceForBashInjection;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.parseExtensionsFromRequest;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.setInstantiateOssTopology;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.resetCombinedValues;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologyAsMap;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologySpecificParameters;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.mergeMaps;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.copyParametersMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ClusterConfigFileNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.DeployableModulesService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class InstantiateRequestHandler extends GrantingLifecycleRequestHandler implements GrantingRequestHandler {

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private InstantiationLevelService instantiationLevelService;

    @Autowired
    private ExtensionsService extensionsService;

    @Autowired
    private ReplicaDetailsService replicaDetailsService;

    @Autowired
    private Day0ConfigurationService day0ConfigurationService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Autowired
    private ValuesFileService valuesFileService;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private DeployableModulesService deployableModulesService;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    @Autowired
    private VnfdParametersHelper vnfdParametersHelper;

    @Override
    public LifecycleOperationType getType() {
        return INSTANTIATE;
    }

    static void setOssTopology(final VnfInstance vnfInstance, Map<String, Object> additionalParams,
                               final Map<String, Object> operationValuesFileMap) {
        Map<String, PropertiesModel> ossTopologyAsMap = getOssTopologyAsMap(vnfInstance.getOssTopology(),
                                                                            PropertiesModel.class);
        Map<String, Object> ossTopologyFromFileMap = getOssTopologySpecificParameters(operationValuesFileMap);
        mergeMaps(ossTopologyAsMap, ossTopologyFromFileMap);
        Map<String, Object> ossTopologySpecificParameters = getOssTopologySpecificParameters(
                copyParametersMap(additionalParams));
        mergeMaps(ossTopologyAsMap, ossTopologySpecificParameters);
        setInstantiateOssTopology(vnfInstance, ossTopologyAsMap);
    }

    public void updateInstanceModel(final VnfInstance instance, final Map<String, Object> additionalParams) {
        if (LifeCycleManagementHelper.vnfInstanceSupportsScaling(instance)) {
            if (LifeCycleManagementHelper.manoControlledScalingParameterIsPresent(additionalParams)) {
                final Object manoControlledScaling = additionalParams.get(MANO_CONTROLLED_SCALING);
                instance.setManoControlledScaling(BooleanUtils.getBooleanValue(manoControlledScaling));
            } else {
                instance.setManoControlledScaling(false);
            }
            LOGGER.info("VNF instance {} supports scaling, setting {} to {}", instance.getVnfInstanceName(), MANO_CONTROLLED_SCALING,
                        instance.getManoControlledScaling());
        }
    }

    @SuppressWarnings("unchecked")
    private void extractAndSetVnfSensitiveInfoFromAdditionalParams(final VnfInstance instance, final Map additionalParams) {
        if (CollectionUtils.isEmpty(additionalParams)) {
            return;
        }
        Map<String, Object> day0 = day0ConfigurationService.retrieveDay0ConfigurationParams(additionalParams);
        if (CollectionUtils.isEmpty(day0)) {
            return;
        }
        try {
            String day0AsString = mapper.writeValueAsString(day0);
            cryptoUtils.setEncryptDetailsForKey(DAY0_CONFIGURATION_PREFIX, day0AsString, instance);
        } catch (JsonProcessingException e) {
            String errorMessage = String.format("Unable to parse day0 configuration for instance id %s", instance.getVnfInstanceId());
            throw new InvalidInputException(errorMessage, e);
        }
    }

    private String resolveClusterName(final InstantiateVnfRequest request) {
        String clusterNameTemp = request.getClusterName();
        if (clusterNameTemp != null) {
            return clusterNameTemp;
        }
        return databaseInteractionService.getDefaultClusterName()
                .orElseThrow(() -> new ClusterConfigFileNotFoundException(DEFAULT_CLUSTER_FILE_NOT_FOUND));
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        final InstantiateVnfRequest instantiateVnfRequest = (InstantiateVnfRequest) request;
        instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) instantiateVnfRequest.getAdditionalParams());
        instantiateVnfRequestValidatingService.validateSkipVerification((Map<?, ?>) instantiateVnfRequest.getAdditionalParams());
        instantiateVnfRequestValidatingService.validateScaleLevelInfo(instantiateVnfRequest);

        super.commonValidation(vnfInstance, InstantiationState.INSTANTIATED, LCMOperationsEnum.INSTANTIATE);

        String vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId()).toString();

        validateNetworkDataTypes(vnfd, instantiateVnfRequest);

        Map<String, Object> additionalParams = checkAndCastObjectToMap(instantiateVnfRequest.getAdditionalParams());
        String clusterName = resolveClusterName(instantiateVnfRequest);
        instantiateVnfRequest.setClusterName(clusterName);
        vnfInstance.setClusterName(clusterName);
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, vnfInstance.getVnfInstanceName(), INSTANTIATE_OPERATION);
        checkVnfInstanceForBashInjection(vnfInstance);
        instantiateVnfRequestValidatingService.validateNamespace(additionalParams);
        if (StringUtils.hasLength(instantiateVnfRequest.getInstantiationLevelId())) {
            instantiationLevelService.validateInstantiationLevelInPoliciesOfVnfInstance(vnfInstance, instantiateVnfRequest.getInstantiationLevelId());
        }

        if (instantiateVnfRequest.getExtensions() != null) {
            Map<String, Object> extensionsFromRequest = parseExtensionsFromRequest(instantiateVnfRequest.getExtensions());
            extensionsService.validateVnfControlledScalingExtension(extensionsFromRequest, vnfInstance.getPolicies());
            extensionsService.validateDeployableModulesExtension(extensionsFromRequest, vnfInstance, vnfd);
        }
        vnfInstance.setHelmClientVersion(
            lifeCycleManagementHelper.validateAndGetHelmClientVersion(additionalParams));
        synchronized (this) {
            lifeCycleManagementHelper.checkDuplicateConstraint(vnfInstance, instantiateVnfRequest);
        }
    }

    private void updateVnfTempInstance(final InstantiateVnfRequest vnfInstanceRequest,
                                       VnfInstance vnfInstance,
                                       final Map<String, Object> valuesYamlMap) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        var descriptorModel = String.valueOf(packageService.getVnfd(vnfInstance.getVnfPackageId()));

        tempInstance.setManoControlledScaling(vnfInstance.getManoControlledScaling());

        if (vnfInstanceRequest.getExtensions() != null) {
            extensionsService.updateInstanceWithExtensionsInRequest(
                    parseExtensionsFromRequest(vnfInstanceRequest.getExtensions()), tempInstance);
            Map<String, Object> extensions = convertStringToJSONObj(tempInstance.getVnfInfoModifiableAttributesExtensions());
            extensionsService.validateDeployableModulesExtension(extensions, tempInstance, descriptorModel);
        }
        if (tempInstance.getPolicies() != null) {
            updateTempInstanceWithScalingData(vnfInstanceRequest, tempInstance);
            replicaDetailsService.updateAndSetReplicaDetailsToVnfInstance(descriptorModel, tempInstance, valuesYamlMap);
        }

        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
    }

    private void updateHelmChartsWithDeployableModulesExtension(final VnfInstance vnfInstance) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        final Map<String, String> deployableModulesValues = extensionsMapper.getDeployableModulesValues(
                tempInstance.getVnfInfoModifiableAttributesExtensions());
        deployableModulesService.updateVnfInstanceHelmChartsAccordingToDeployableModulesExtension(tempInstance, deployableModulesValues);
        updateVnfInstanceHelmChartsStatusFromTempInstance(vnfInstance, tempInstance);

        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
    }

    private void updateVnfInstanceHelmChartsStatusFromTempInstance(VnfInstance vnfInstance, VnfInstance tempInstance) {
        final Map<String, HelmChart> vnfInstanceHelmCharts = vnfInstance.getHelmCharts().stream()
                .collect(Collectors.toMap(HelmChart::getId, Function.identity()));
        final List<HelmChart> tempInstanceHelmCharts = tempInstance.getHelmCharts();

        tempInstanceHelmCharts.forEach(tempHelmChart -> {
            final HelmChart helmChart = vnfInstanceHelmCharts.get(tempHelmChart.getId());
            helmChart.setChartEnabled(tempHelmChart.isChartEnabled());
        });
    }

    /**
     * Sets scaleLevel values for aspects in an updated VNF instance from the {@link InstantiateVnfRequest} object.
     *
     * @param vnfInstanceRequest {@link InstantiateVnfRequest} object
     * @param tempInstance       {@link VnfInstance} object that is being updated
     */
    private void updateTempInstanceWithScalingData(final InstantiateVnfRequest vnfInstanceRequest,
                                                   final VnfInstance tempInstance) {

        var vnfInstancePolicies = replicaDetailsMapper.getPoliciesFromVnfInstance(tempInstance);
        List<ScaleInfo> targetScaleLevelInfo = vnfInstanceRequest.getTargetScaleLevelInfo();

        if (!CollectionUtils.isEmpty(vnfInstancePolicies.getAllInstantiationLevels())) {
            String targetInstantiationLevelId = vnfInstanceRequest.getInstantiationLevelId();

            if (StringUtils.hasLength(targetInstantiationLevelId)) {
                tempInstance.setInstantiationLevel(targetInstantiationLevelId);
                instantiationLevelService.setScaleLevelForInstantiationLevel(
                        tempInstance, targetInstantiationLevelId, vnfInstancePolicies);
                return;
            }
        }
        if (!CollectionUtils.isEmpty(targetScaleLevelInfo)) {
            instantiationLevelService.setScaleLevelForTargetScaleLevelInfo(
                    tempInstance, targetScaleLevelInfo, vnfInstancePolicies);
        } else {
            instantiationLevelService.setDefaultInstantiationLevelToVnfInstance(tempInstance);
        }
    }

    /**
     * Update the instance model
     *
     * @param vnfInstance
     * @param request
     * @param operation
     * @param additionalParams
     */
    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        InstantiateVnfRequest instantiateVnfRequest = (InstantiateVnfRequest) request;
        Map<String, Object> operationValuesFileMap = convertStringToJSONObj(operation.getValuesFileParams());

        updateInstanceModel(vnfInstance, additionalParams);
        updateVnfTempInstance(instantiateVnfRequest, vnfInstance, operationValuesFileMap);
        setInstanceWithCleanUpResources(vnfInstance, additionalParams);
        LifeCycleManagementHelper.setNamespace(vnfInstance, additionalParams);
        lifeCycleManagementHelper.persistNamespaceDetails(vnfInstance);
        updateHelmChartWithVnfControlledScalingExtension(vnfInstance);
        updateHelmChartsWithDeployableModulesExtension(vnfInstance);
        helmChartHelper.resetHelmChartStates(vnfInstance.getHelmCharts());
        helmChartHelper.completeDisabledHelmCharts(vnfInstance.getHelmCharts());
        resetCombinedValues(vnfInstance);
        removeExcessAdditionalParams(additionalParams);
        updateCrdNamespace(vnfInstance);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    @Override
    public void createTempInstance(final VnfInstance vnfInstance, final Object request) {
        InstanceUtils.createTempInstance(vnfInstance);
    }

    @Override
    public void updateClusterConfigStatus(final VnfInstance vnfInstance) {
        LOGGER.info("Updating cluster config file status to IN_USE");
        clusterConfigService.changeClusterConfigFileStatus(vnfInstance.getClusterName(), vnfInstance, ConfigFileStatus.IN_USE);
    }

    private void updateCrdNamespace(VnfInstance vnfInstance) {
        String crdNamespace = databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(vnfInstance.getClusterName());
        vnfInstance.setCrdNamespace(crdNamespace);
    }

    @VisibleForTesting
    static void updateChartReplicaDetails(final Map<String, Integer> resourceDetails, final HelmChart chart) {
        Map<String, ReplicaDetails> replicaDetailsMap = parseJsonToGenericType(chart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE);
        for (final Entry<String, Integer> target : resourceDetails.entrySet()) {
            if (replicaDetailsMap.containsKey(target.getKey())) {
                int numberIn = target.getValue();
                ReplicaDetails replicaDetails = replicaDetailsMap.get(target.getKey());
                if (!Strings.isNullOrEmpty(replicaDetails.getMaxReplicasParameterName()) && replicaDetails.getAutoScalingEnabledValue()) {
                    replicaDetails.setMaxReplicasCount(numberIn);
                }
                replicaDetails.setCurrentReplicaCount(numberIn);
                replicaDetailsMap.replace(target.getKey(), replicaDetails);
            }
        }
        chart.setReplicaDetails(convertObjToJsonString(replicaDetailsMap));
    }

    @Override
    public void sendRequest(final VnfInstance vnfInstance, LifecycleOperation operation, final Object request,
                            final Path toValuesFile) {
        InstantiateVnfRequest instantiateVnfRequest = (InstantiateVnfRequest) request;
        WorkflowRoutingResponse response;
        if (toValuesFile == null || Utility.isEmptyFile(toValuesFile)) {
            response = workflowRoutingService.routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest);
        } else {
            response = workflowRoutingService.routeInstantiateRequest(vnfInstance, operation, instantiateVnfRequest, toValuesFile);
        }
        if (response != null) {
            checkAndProcessFailedError(operation, response);
        }
    }

    @Override
    public Map<String, Object> formatParameters(final VnfInstance vnfInstance, final Object request,
                                                final LifecycleOperationType type, final Map<String, Object> valuesYamlMap) {
        Map<String, Object> additionalParams;
        try {
            additionalParams = super.formatParameters(vnfInstance, request, type, valuesYamlMap);
            setInstanceWithSitebasicFile(vnfInstance, additionalParams);
            mergeParameters(vnfInstance, request, type, additionalParams, valuesYamlMap);
            lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, vnfInstance);
            addENMNodeWithEnrollment(vnfInstance, additionalParams, valuesYamlMap);
            extractAndSetVnfSensitiveInfoFromAdditionalParams(vnfInstance, additionalParams);
        } catch (Exception exe) {
            databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstance.getVnfInstanceId());
            throw exe;
        }
        return additionalParams;
    }

    private void mergeParameters(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                                 final Map<String, Object> additionalParams, final Map<String, Object> valuesYamlMap) {
        valuesFileService.mergeValuesYamlMap(valuesYamlMap, additionalParams);
        vnfdParametersHelper.mergeDefaultParameters(vnfInstance.getVnfDescriptorId(), type, additionalParams, valuesYamlMap);
        additionalParams.remove(VALUES_YAML_ADDITIONAL_PARAMETER);
        setAdditionalParams(request, additionalParams);
    }

    private void addENMNodeWithEnrollment(VnfInstance vnfInstance,
                                          Map<String, Object> additionalParams,
                                          Map<String, Object> valuesYamlMap) {
        boolean addNodeToOss = BooleanUtils.getBooleanValue(additionalParams.get(ADD_NODE_TO_OSS), false);
        boolean cmpV2Enrollment = BooleanUtils.getBooleanValue(additionalParams.get(CMP_V2_ENROLLMENT), false);

        setOssTopology(vnfInstance, additionalParams, Optional.ofNullable(valuesYamlMap).orElse(new HashMap<>()));

        if (addNodeToOss && cmpV2Enrollment) {
            cmpEnrollmentHelper.addNodeToENMWithEnrollment(vnfInstance, additionalParams);
        }
    }

    @Override
    public LifecycleOperation persistOperation(final VnfInstance vnfInstance,
                                               final Object request,
                                               final String requestUsername,
                                               final LifecycleOperationType type,
                                               final Map<String, Object> valuesYamlMap, final String applicationTimeout) {
        LifecycleOperation lifecycleOperation = super.persistOperation(vnfInstance, request,
                                                                       requestUsername, type, valuesYamlMap, applicationTimeout);

        String helmClientVersion = lifeCycleManagementHelper.validateAndGetHelmClientVersion(getAdditionalParams(request));
        lifecycleOperation.setHelmClientVersion(helmClientVersion);
        return lifecycleOperation;
    }

    @Override
    protected void doVerifyGrantingResources(LifecycleOperation operation, final Object request, final Map<String, Object> valuesYamlMap) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        InstantiateVnfRequest instantiateVnfRequest = (InstantiateVnfRequest) request;
        JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());

        GrantRequest grantRequest = new GrantRequest();
        grantRequest.setFlavourId(instantiateVnfRequest.getFlavourId());
        grantRequest.setInstantiationLevelId(vnfInstance.getInstantiationLevel());

        LOGGER.info("Starting granting resources delta calculation for Instantiate operation. PackageId = {}", vnfInstance.getVnfPackageId());
        final List<ResourceDefinition> resourceDefinitions =
                grantingResourceDefinitionCalculation
                        .calculateRel4ResourcesInUse(
                                vnfd, getVnfInstanceFromJson(vnfInstance.getTempInstance()), valuesYamlMap, vnfInstance.getVnfDescriptorId());

        fillAndExecuteGrantRequest(resourceDefinitions,
                                   emptyList(),
                                   vnfInstance,
                                   operation,
                                   grantRequest);
    }

    @Override
    protected GrantedLcmOperationType getGrantingOperationType() {
        return GrantedLcmOperationType.INSTANTIATE;
    }

    private void validateNetworkDataTypes(String vnfd, InstantiateVnfRequest instantiateVnfRequest) {
        if (!CollectionUtils.isEmpty(instantiateVnfRequest.getExtVirtualLinks())) {
            instantiateVnfRequestValidatingService.validateNetworkDataTypes(vnfd, instantiateVnfRequest);
        }
    }

    @Override
    public void processValuesYaml(final Map<String, Object> valuesYamlMap,
                                  final VnfInstance vnfInstance,
                                  final Object request,
                                  final LifecycleOperation operation) {
        valuesFileService.updateValuesYamlMapWithReplicaDetailsAndHighestPriority(valuesYamlMap, vnfInstance);
    }
}
