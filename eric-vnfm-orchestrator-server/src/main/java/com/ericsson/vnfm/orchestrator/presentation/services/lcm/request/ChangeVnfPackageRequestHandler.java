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

import static java.util.stream.Collectors.toMap;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_DM_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.parseExtensionsFromRequest;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationStateToProcessing;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getFirstChartToDownsizeDuringCCVP;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getNotProcessedHelmChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.setPriorityBeforeOperation;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.toTerminateHelmChart;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkPackageOperationalState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.writeMapToValuesFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevels;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.CcvpPatternTransformer;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeOperationContextBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.DeployableModulesService;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmReleaseNameGenerator;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmDowngrade;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmUpgrade;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChangeVnfPackageRequestHandler extends GrantingLifecycleRequestHandler implements GrantingRequestHandler {

    private static final TypeReference<List<ScaleInfoEntity>> SCALE_INFO_ENTITY_LIST_REF = new TypeReference<>() {
    };

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private InstantiationLevelService instantiationLevelService;

    @Autowired
    private ReplicaDetailsService replicaDetailsService;

    @Autowired
    private EvnfmDowngrade evnfmDowngrade;

    @Autowired
    private EvnfmUpgrade evnfmUpgrade;

    @Autowired
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @Autowired
    private ValuesFileService valuesService;

    @Autowired
    private LcmOpAdditionalParamsProcessor lcmOpAdditionalParamsProcessor;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private ChangeVnfPackageService changeVnfPackageService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private VnfdHelper vnfdHelper;

    @Autowired
    private ExtensionsService extensionsService;

    @Autowired
    @Qualifier("enhancedChangeOperationContextBuilder")
    private ChangeOperationContextBuilder changeOperationContextBuilder;

    @Autowired
    private DeployableModulesService deployableModulesService;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Autowired
    private CcvpPatternTransformer ccvpPatternTransformer;

    @Autowired
    private VnfdParametersHelper vnfdParametersHelper;

    private void persistChangeVnfPackageOperationDetails(final ChangeOperationContext context) {
        ChangePackageOperationDetails operationDetails = new ChangePackageOperationDetails();
        operationDetails.setOperationOccurrenceId(context.getOperation().getOperationOccurrenceId());
        operationDetails.setChangePackageOperationSubtype(context.getChangePackageOperationSubtype());
        operationDetails.setTargetOperationOccurrenceId(context.getTargetOperationOccurrenceId());

        changePackageOperationDetailsRepository.save(operationDetails);

        final LifecycleOperation operation = context.getOperation();
        operation.setTargetVnfdId(context.getTargetVnfdId());
        operation.setSourceVnfdId(context.getSourceVnfInstance().getVnfDescriptorId());
        operation.setDownsizeAllowed(context.isDownsize());
        operation.setAutoRollbackAllowed(context.isAutoRollbackAllowed());

        databaseInteractionService.persistLifecycleOperation(operation);
    }

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.CHANGE_VNFPKG;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = (ChangeCurrentVnfPkgRequest) request;
        instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) changeCurrentVnfPkgRequest.getAdditionalParams());
        super.commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.CHANGE_VNFPKG);
        checkAndCastObjectToMap(changeCurrentVnfPkgRequest.getAdditionalParams());
    }

    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        var tempVnfInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        updateBroEndpointUrl(additionalParams, tempVnfInstance);
        updateVnfInstanceHelmChartsWithDeployableModulesExtension(vnfInstance, tempVnfInstance, additionalParams);
        updateTempInstanceHelmCharts(vnfInstance, tempVnfInstance, operation);
        helmChartHelper.completeDisabledHelmCharts(tempVnfInstance.getHelmCharts());
        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));
    }

    @Override
    public void createTempInstance(final VnfInstance vnfInstance, final Object request) {
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = (ChangeCurrentVnfPkgRequest) request;

        final PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(changeCurrentVnfPkgRequest.getVnfdId());
        checkPackageOperationalState(targetPackageInfo);
        final Optional<Policies> targetPolicies = vnfdHelper.getVnfdScalingInformation(new JSONObject(targetPackageInfo.getDescriptorModel()));

        VnfInstance tempInstance = instanceService.createTempInstanceForUpgradeOperation(vnfInstance, targetPackageInfo, targetPolicies);

        if (targetPolicies.isPresent()) {
            Map<String, Object> extensionsFromRequest = null;
            if (changeCurrentVnfPkgRequest.getExtensions() != null) {
                extensionsFromRequest = parseExtensionsFromRequest(changeCurrentVnfPkgRequest.getExtensions());
            }
            instanceService.setExtensions(tempInstance,
                                          targetPackageInfo.getDescriptorModel(),
                                          extensionsFromRequest,
                                          vnfInstance.getVnfDescriptorId());

            addInstantiationLevelInfoToTempVnfInstance(tempInstance, vnfInstance, targetPolicies.get());
        }

        instanceService.setHelmClientVersion(tempInstance, getAdditionalParams(request));
        instanceService.setIsDeployableModulesSupported(tempInstance, targetPackageInfo);
        if (tempInstance.getHelmCharts() != null) {
            setPriorityBeforeOperation(LifecycleOperationType.CHANGE_VNFPKG, tempInstance);
        }
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
    }

    private void addInstantiationLevelInfoToTempVnfInstance(final VnfInstance tempInstance,
                                                            final VnfInstance vnfInstance,
                                                            final Policies policies) {
        if (StringUtils.isEmpty(vnfInstance.getInstantiationLevel())) {
            LOGGER.info("Instantiation level is not provided, setting default");
            instantiationLevelService.setDefaultInstantiationLevelToVnfInstance(tempInstance);
            return;
        }

        if (instanceContainsInstantiationLevels(tempInstance)) {
            validateAndSetInstantiationLevel(tempInstance, vnfInstance, policies);
        } else {
            tempInstance.setInstantiationLevel(null);
        }
    }

    @Override
    public void sendRequest(final VnfInstance sourceVnfInstance, final LifecycleOperation operation, final Object request,
                            final Path valuesFile) {
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = (ChangeCurrentVnfPkgRequest) request;
        final ChangeOperationContext context = changeOperationContextBuilder.build(sourceVnfInstance, operation, changeCurrentVnfPkgRequest);
        persistChangeVnfPackageOperationDetails(context);

        boolean isScalingAspectPresentInTempInstanceAndCurrentInstance = isScalingAspectPresentInBothVnfInstances(context);
        boolean collectedPersistScaleInfoAndRemoveFromAdditionalParams =
                collectPersistScaleInfoValueItAndRemoveFromAdditionalParams(context.getAdditionalParams());

        final VnfInstance tempVnfInstance = context.getTempInstance();

        LOGGER.info("Source Package id {}, target package id {}", sourceVnfInstance.getVnfPackageId(), context.getTempInstance().getVnfPackageId());

        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        if (isScalingAspectPresentInTempInstanceAndCurrentInstance && !context.isDowngrade()) {
            String descriptorModel = context.getSourcePackageInfo().getDescriptorModel();
            updateReplicaDetailsForTempInstance(sourceVnfInstance,
                                                tempVnfInstance,
                                                collectedPersistScaleInfoAndRemoveFromAdditionalParams,
                                                descriptorModel,
                                                valuesYamlMap);
        }

        setCombinedValuesToVnfInstance(tempVnfInstance, context.getAdditionalParams());
        setAlarmSupervisionWithWarning(sourceVnfInstance, DISABLE_ALARM_SUPERVISION);
        setDetailsOnDowngrade(context);

        sourceVnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));

        // We should always lock target package. Note that downgrade runs upgrade flow on current one.
        instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(sourceVnfInstance.getVnfPackageId(),
                                                                                         context.getTargetPackageInfo().getId(),
                                                                                         context.getTargetPackageInfo().getId(),
                                                                                         sourceVnfInstance.getVnfInstanceId(),
                                                                                         true);

        if (context.isDowngrade()) {
            ccvpPatternTransformer.saveRollbackPatternInOperationForDowngradeCcvp(context);

            rollbackValuesFile(context);

            evnfmDowngrade.execute(context.getOperation());
            sourceVnfInstance.setTempInstance(convertObjToJsonString(context.getTempInstance()));
        } else {
            ccvpPatternTransformer.saveUpgradePatternInOperationCcvp(context);
            if (context.isDownsize()) {
                updateOperationStateToProcessing(operation);

                final HelmChart firstHelmChartToDownsize = getFirstChartToDownsizeDuringCCVP(sourceVnfInstance);
                firstHelmChartToDownsize.setDownsizeState(LifecycleOperationState.PROCESSING.toString());

                final String firstReleaseToDownsize = firstHelmChartToDownsize.getReleaseName();

                ResponseEntity<Object> responseEntity = workflowRoutingService.routeDownsizeRequest(sourceVnfInstance,
                                                                                                    context.getOperation(),
                                                                                                    firstReleaseToDownsize);
                sourceVnfInstance.setTempInstance(convertObjToJsonString(context.getTempInstance()));
                checkAndProcessFailedError(operation, responseEntity, firstReleaseToDownsize);
            } else {
                routeChangePackageInfoRequestByDeployableModules(valuesFile, context);
            }
        }
    }

    private void routeChangePackageInfoRequestByDeployableModules(Path valuesFile, ChangeOperationContext context) {
        VnfInstance sourceInstance = context.getSourceVnfInstance();
        VnfInstance upgradedInstance = context.getTempInstance();
        LifecycleOperation operation = context.getOperation();

        if (operation.getUpgradePattern() == null) {
            HelmChart helmChartToUpgrade = getNotProcessedHelmChartWithHighestPriority(upgradedInstance);
            WorkflowRoutingResponse response = routeChangePackageInfoRequest(valuesFile, context, helmChartToUpgrade.getPriority());
            sourceInstance.setTempInstance(convertObjToJsonString(upgradedInstance));
            checkAndProcessFailedError(operation, response);
        } else {
            operation.setVnfInstance(sourceInstance);
            evnfmUpgrade.execute(operation);
        }
    }

    private void updateTempInstanceHelmCharts(final VnfInstance vnfInstance,
                                              final VnfInstance tempVnfInstance,
                                              final LifecycleOperation operation) {
        updateTempInstanceTerminatedHelmChartsWithSourceHelmCharts(vnfInstance, tempVnfInstance, operation);
        updateTempInstanceTerminatedHelmChartsWithDisabledCharts(vnfInstance, tempVnfInstance, operation);
        updateTempInstanceHelmChartsToUpgrade(tempVnfInstance, operation);

        updateTempInstanceHelmChartsReleaseNames(vnfInstance, tempVnfInstance, operation);
    }

    private void updateTempInstanceTerminatedHelmChartsWithDisabledCharts(final VnfInstance vnfInstance,
                                                                          final VnfInstance tempInstance,
                                                                          final LifecycleOperation lifecycleOperation) {
        Map<String, HelmChart> enabledHelmChartsInSourceMap = vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.isChartEnabled() && helmChart.getHelmChartType() != HelmChartType.CRD)
                .collect(toMap(HelmChart::getHelmChartName, Function.identity()));

        Map<String, HelmChart> disabledHelmChartsInTargetMap = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> !helmChart.isChartEnabled() && helmChart.getHelmChartType() != HelmChartType.CRD)
                .collect(toMap(HelmChart::getHelmChartName, Function.identity()));

        List<HelmChart> helmChartsToTerminate = new ArrayList<>();
        for (Map.Entry<String, HelmChart> helmChartEntry: disabledHelmChartsInTargetMap.entrySet()) {
            boolean isHelmChartInstantiated = enabledHelmChartsInSourceMap.containsKey(helmChartEntry.getKey()) &&
                    enabledHelmChartsInSourceMap.get(helmChartEntry.getKey()).isChartEnabled();
            if (isHelmChartInstantiated) {
                helmChartsToTerminate.add(helmChartEntry.getValue());
            }
        }
        List<TerminatedHelmChart> terminatedHelmCharts = helmChartsToTerminate.stream()
                .map(helmChart ->  toTerminateHelmChart(helmChart, lifecycleOperation))
                .toList();
        tempInstance.getTerminatedHelmCharts().addAll(terminatedHelmCharts);
    }

    private void updateTempInstanceTerminatedHelmChartsWithSourceHelmCharts(final VnfInstance vnfInstance,
                                                                            final VnfInstance tempInstance,
                                                                            final LifecycleOperation lifecycleOperation) {
        Map<String, HelmChart> helmChartsInTargetMap = tempInstance.getHelmCharts().stream()
                .collect(toMap(HelmChart::getHelmChartName, Function.identity()));
        Map<String, HelmChart> enabledHelmChartsInSourceMap = vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.isChartEnabled() && helmChart.getHelmChartType() != HelmChartType.CRD)
                .collect(toMap(HelmChart::getHelmChartName, Function.identity()));

        List<TerminatedHelmChart> notPresentHelmChartsInTarget =
                Maps.difference(enabledHelmChartsInSourceMap, helmChartsInTargetMap).entriesOnlyOnLeft().values().stream()
                        .map(helmChart ->  toTerminateHelmChart(helmChart, lifecycleOperation))
                        .toList();
        tempInstance.getTerminatedHelmCharts().addAll(notPresentHelmChartsInTarget);
    }

    private void updateTempInstanceHelmChartsToUpgrade(final VnfInstance tempInstance,
                                                       final LifecycleOperation lifecycleOperation) {
        Map<String, TerminatedHelmChart> helmChartsToTerminateMap = tempInstance.getTerminatedHelmCharts().stream()
                .filter(helmChart -> helmChart.getOperationOccurrenceId().equals(lifecycleOperation.getOperationOccurrenceId()))
                .collect(toMap(TerminatedHelmChart::getHelmChartName, Function.identity()));

        List<HelmChart> helmChartsToUpgrade = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> !helmChartsToTerminateMap.containsKey(helmChart.getHelmChartName()))
                .collect(Collectors.toList());
        tempInstance.setHelmCharts(helmChartsToUpgrade);
    }

    private void updateTempInstanceHelmChartsReleaseNames(final VnfInstance vnfInstance, final VnfInstance tempInstance,
                                                          final LifecycleOperation operation) {
        Map<String, HelmChart> cnfHelmChartsInSourceMap = vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.getHelmChartType() != HelmChartType.CRD)
                .collect(toMap(HelmChart::getHelmChartName, Function.identity()));

        List<TerminatedHelmChart> terminatedHelmCharts = tempInstance.getTerminatedHelmCharts().stream()
                .filter(helmChart -> helmChart.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId())
                        && helmChart.getReleaseName() == null)
                .toList();
        terminatedHelmCharts.forEach(helmChart -> {
            String sourceReleaseName = cnfHelmChartsInSourceMap.get(helmChart.getHelmChartName()).getReleaseName();
            helmChart.setReleaseName(sourceReleaseName);
        });

        List<HelmChart> upgradedHelmCharts = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> cnfHelmChartsInSourceMap.containsKey(helmChart.getHelmChartName()) && helmChart.getReleaseName() == null)
                .toList();
        upgradedHelmCharts.forEach(helmChart -> {
            String sourceReleaseName = cnfHelmChartsInSourceMap.get(helmChart.getHelmChartName()).getReleaseName();
            helmChart.setReleaseName(sourceReleaseName);
        });

        List<HelmChart> newHelmCharts = tempInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.getReleaseName() == null)
                .sorted(Comparator.comparing(HelmChartBaseEntity::getPriority))
                .toList();

        newHelmCharts.forEach(helmChart -> {
            var previouslyTerminatedHelmChart = tempInstance.getTerminatedHelmCharts().stream()
                    .filter(terminatedHelmChart -> helmChart.getHelmChartName().equals(terminatedHelmChart.getHelmChartName()))
                    .findFirst();
            if (previouslyTerminatedHelmChart.isPresent()) {
                helmChart.setReleaseName(previouslyTerminatedHelmChart.get().getReleaseName());
            } else {
                final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(tempInstance, vnfInstance.getHelmCharts());
                helmChart.setReleaseName(helmReleaseNameGenerator.generateNextReleaseName());
            }
        });
    }

    private void updateVnfInstanceHelmChartsWithDeployableModulesExtension(final VnfInstance vnfInstance, final VnfInstance tempInstance,
                                                                           final Map<String, Object> additionalParams) {
        Map<String, String> targetDeployableModules = extensionsMapper.getDeployableModulesValues(
                tempInstance.getVnfInfoModifiableAttributesExtensions());
        if (MapUtils.isEmpty(targetDeployableModules)) {
            return;
        }

        final Map<String, String> sourceDeployableModules = extensionsMapper.getDeployableModulesValues(
                vnfInstance.getVnfInfoModifiableAttributesExtensions());
        boolean isPersistDmConfig = parsePersistDmConfigValueFromAdditionalParams(additionalParams);
        if (isPersistDmConfig && MapUtils.isNotEmpty(sourceDeployableModules)) {
            updateDeployableModuleValues(sourceDeployableModules, targetDeployableModules);
            updateTempInstanceExtensionsWithNewDeployableModules(tempInstance, targetDeployableModules);
        }
        deployableModulesService.updateVnfInstanceHelmChartsAccordingToDeployableModulesExtension(tempInstance, targetDeployableModules);
    }

    private void updateDeployableModuleValues(final Map<String, String> sourceDeployableModules, final Map<String, String> targetDeployableModules) {
        targetDeployableModules.entrySet().stream()
                .filter(deployableModule -> sourceDeployableModules.containsKey(deployableModule.getKey()))
                .forEach(deployableModule -> targetDeployableModules.replace(deployableModule.getKey(), deployableModule.getValue(),
                                                                             sourceDeployableModules.get(deployableModule.getKey())));
    }

    private void updateTempInstanceExtensionsWithNewDeployableModules(VnfInstance tempInstance, Map<String, String> updatedDeployableModules) {
        final Map<String, Map<String, String>> extensions = extensionsMapper.getVnfInfoModifiableAttributesExtensions(
                tempInstance.getVnfInfoModifiableAttributesExtensions());
        extensions.replace(DEPLOYABLE_MODULES, updatedDeployableModules);
        tempInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
    }

    private void rollbackValuesFile(final ChangeOperationContext context) {
        final VnfInstance vnfInstance = context.getSourceVnfInstance();
        Optional<LifecycleOperation> operationToRollback = lcmOpSearchService
                .searchLastCompletedInstallOrUpgradeOperationForRollbackTo(vnfInstance);

        operationToRollback
                .ifPresent(lifecycleOperation -> {
                    VnfInstance tempInstance = context.getTempInstance();
                    tempInstance.setCombinedValuesFile(lifecycleOperation.getCombinedValuesFile());
                    rollbackBroEndpointIfNeeded(tempInstance);
                    vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
                });
    }

    private void rollbackBroEndpointIfNeeded(final VnfInstance tempInstance) {
        Map<String, Object> valuesMap = Utility.convertStringToJSONObj(tempInstance.getCombinedValuesFile());
        updateBroEndpointUrl(valuesMap, tempInstance);
    }

    private void updateBroEndpointUrl(Map<String, Object> valuesYamlMap, final VnfInstance vnfInstanceTemp) {
        lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, vnfInstanceTemp);
    }

    private void setDetailsOnDowngrade(final ChangeOperationContext context) {
        if (context.isDowngrade()) {
            LifecycleOperation targetLifecycleOperation = databaseInteractionService.getLifecycleOperation(
                    context.getTargetOperationOccurrenceId());
            if (!Strings.isNullOrEmpty(targetLifecycleOperation.getInstantiationLevel())) {
                context.getTempInstance().setInstantiationLevel(targetLifecycleOperation.getInstantiationLevel());
            }
            if (!Strings.isNullOrEmpty(targetLifecycleOperation.getScaleInfoEntities())) {
                List<ScaleInfoEntity> scaleInfoEntities = Optional.of(targetLifecycleOperation.getScaleInfoEntities())
                        .map(json -> parseJsonToGenericType(json, SCALE_INFO_ENTITY_LIST_REF))
                        .orElseGet(ArrayList::new);
                context.getTempInstance().setScaleInfoEntity(scaleInfoEntities);
            }
            context.getTempInstance().setHelmClientVersion(targetLifecycleOperation.getHelmClientVersion());
            context.getOperation().setHelmClientVersion(targetLifecycleOperation.getHelmClientVersion());
            List<HelmChartHistoryRecord> helmChartHistoryRecords =
                    helmChartHistoryRepository.findAllByLifecycleOperationIdOrderByPriorityAsc(context.getTargetOperationOccurrenceId());
            context.getTempInstance()
                    .getHelmCharts()
                    .forEach(helmChart -> helmChartHistoryRecords.stream()
                            .filter(helmChartHistoryRecord -> helmChartHistoryRecord.getReleaseName().equals(helmChart.getReleaseName()))
                            .findFirst()
                            .ifPresent(helmChartHistoryRecord -> helmChart.setReplicaDetails(helmChartHistoryRecord.getReplicaDetails())));
        }
    }

    public void sendPostDownsizeRequest(final ChangeOperationContext context, final Path valuesFile) {
        routeChangePackageInfoRequestByDeployableModules(valuesFile, context);
    }

    private WorkflowRoutingResponse routeChangePackageInfoRequest(final Path toValuesFile,
                                                                  final ChangeOperationContext context,
                                                                  final int priority) {
        Map<String, Object> yamlValueMap = convertYamlFileIntoMap(toValuesFile);
        valuesService.updateValuesMapWithReplicaDetailsFromTempInstance(yamlValueMap, context.getTempInstance(), priority);
        final WorkflowRoutingResponse response;
        if (MapUtils.isEmpty(yamlValueMap)) {
            response = workflowRoutingService.routeChangePackageInfoRequest(context, priority);
        } else {
            Path tempValuePath = writeMapToValuesFile(yamlValueMap, toValuesFile);
            response = workflowRoutingService.routeChangePackageInfoRequest(context, tempValuePath, priority);
        }
        return response;
    }

    @Override
    public Map<String, Object> formatParameters(final VnfInstance vnfInstance, final Object request,
                                                final LifecycleOperationType type, Map<String, Object> valuesYamlMap) {
        Map<String, Object> additionalParams = super.formatParameters(vnfInstance, request, type, valuesYamlMap);
        mergeParameters(vnfInstance, request, type, additionalParams, valuesYamlMap);
        return additionalParams;
    }

    private void mergeParameters(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                                 final Map<String, Object> additionalParams, final Map<String, Object> valuesYamlMap) {
        valuesService.mergeValuesYamlMap(valuesYamlMap, additionalParams);
        mergeDefaultParameters(vnfInstance, request, type, additionalParams, valuesYamlMap);
        additionalParams.remove(VALUES_YAML_ADDITIONAL_PARAMETER);
        setAdditionalParams(request, additionalParams);
    }

    private void mergeDefaultParameters(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                                        final Map<String, Object> additionalParams, final Map<String, Object> valuesYamlMap) {
        String targetVnfdId = ((ChangeCurrentVnfPkgRequest) request).getVnfdId();
        boolean isDowngradeOperationPresent = changeVnfPackageService
                .getSuitableTargetDowngradeOperationFromVnfInstance(targetVnfdId, vnfInstance)
                .isPresent();

        if (isDowngradeOperationPresent) {
            vnfdParametersHelper.mergeDefaultDowngradeParameters(vnfInstance, request, type, additionalParams, valuesYamlMap);
        } else {
            vnfdParametersHelper.mergeDefaultParameters(targetVnfdId, type, additionalParams, valuesYamlMap);
        }
    }

    @Override
    public LifecycleOperation persistOperation(final VnfInstance vnfInstance,
                                               final Object request,
                                               final String requestUsername,
                                               final LifecycleOperationType type,
                                               Map<String, Object> valuesYamlMap, final String applicationTimeout) {
        LifecycleOperation lifecycleOperation = super.persistOperation(vnfInstance, request,
                                                                       requestUsername, type, valuesYamlMap, applicationTimeout);

        String helmClientVersion = lifeCycleManagementHelper.validateAndGetHelmClientVersion(getAdditionalParams(request));
        lifecycleOperation.setHelmClientVersion(helmClientVersion);
        return lifecycleOperation;
    }

    @Override
    public void verifyGrantingResources(final LifecycleOperation operation, final Object request, final Map<String, Object> valuesYamlMap) {
        // VNFD.isRel4 validation skipped because not applicable for CCVP
        doVerifyGrantingResources(operation, request, valuesYamlMap);
    }

    @Override
    protected void doVerifyGrantingResources(LifecycleOperation operation, final Object request, Map<String, Object> valuesYamlMap) {
        VnfInstance sourceInstance = operation.getVnfInstance();
        String sourceVnfPackageId = sourceInstance.getVnfPackageId();
        JSONObject sourceVnfd = packageService.getVnfd(sourceVnfPackageId);
        String sourceVnfdId = sourceInstance.getVnfDescriptorId();

        ChangeCurrentVnfPkgRequest ccvpRequest = (ChangeCurrentVnfPkgRequest) request;

        VnfInstance tempInstance = parseJson(sourceInstance.getTempInstance(), VnfInstance.class);
        String targetVnfPackageId = tempInstance.getVnfPackageId();
        String targetVnfdId = ccvpRequest.getVnfdId();
        JSONObject targetVnfd = packageService.getVnfd(targetVnfPackageId);
        if (!VnfdUtility.isRel4Vnfd(sourceVnfd) && !VnfdUtility.isRel4Vnfd(targetVnfd)) {
            LOGGER.info("Granting not supported since instance is not rel4");
            return;
        }

        VnfDescriptorDetails descriptorDetails = VnfdUtility.buildVnfDescriptorDetails(targetVnfd);
        String flavourId = descriptorDetails.getDefaultFlavour().getId();
        GrantRequest grantRequest = new GrantRequest();
        grantRequest.setFlavourId(flavourId);
        grantRequest.setDstVnfdId(targetVnfdId);

        Map<String, Object> additionalParams = getAdditionalParams(request);
        boolean persistScaleInfo = BooleanUtils.getBooleanValue(additionalParams.getOrDefault(PERSIST_SCALE_INFO, true), true);
        updateReplicaDetailsForTempInstance(sourceInstance, tempInstance, persistScaleInfo, targetVnfd.toString(), valuesYamlMap);

        LOGGER.info("Starting calculate ADD resource definitions for ChangePackage operation. PackageId = {}", targetVnfPackageId);
        Map<String, Object> processedTargetValuesMap = new HashMap<>(valuesYamlMap);
        final List<ResourceDefinition> addResources =
                grantingResourceDefinitionCalculation.calculateRel4ResourcesInUse(targetVnfd, tempInstance, processedTargetValuesMap, targetVnfdId);

        LOGGER.info("Starting calculate REMOVE resource definitions for ChangePackage operation. PackageId = {}", sourceVnfPackageId);
        final LifecycleOperation lastOperation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(sourceInstance, 0).orElseThrow();
        Map<String, Object> processedSourceValuesMap = lcmOpAdditionalParamsProcessor.processRaw(sourceInstance);
        lcmOpAdditionalParamsProcessor.process(
                processedSourceValuesMap,
                sourceInstance.getVnfDescriptorId(),
                lastOperation);

        final List<ResourceDefinition> removeResources =
                grantingResourceDefinitionCalculation.calculateRel4ResourcesInUse(sourceVnfd, sourceInstance, processedSourceValuesMap, sourceVnfdId);

        fillAndExecuteGrantRequest(addResources,
                                   removeResources,
                                   sourceInstance,
                                   operation,
                                   grantRequest);
    }

    @Override
    protected GrantedLcmOperationType getGrantingOperationType() {
        return GrantedLcmOperationType.CHANGE_VNFPKG;
    }

    @Override
    public void processValuesYaml(final Map<String, Object> valuesYamlMap, final VnfInstance vnfInstance,
                                  final Object request,
                                  final LifecycleOperation operation) {
        final String targetVnfdId = ((ChangeCurrentVnfPkgRequest) request).getVnfdId();
        lcmOpAdditionalParamsProcessor.process(valuesYamlMap, targetVnfdId, operation);
    }

    private void validateAndSetInstantiationLevel(final VnfInstance tempInstance,
                                                  final VnfInstance vnfInstance,
                                                  final Policies policies) {
        instantiationLevelService.validateInstantiationLevelInPoliciesOfVnfInstance(tempInstance, vnfInstance.getInstantiationLevel());
        tempInstance.setInstantiationLevel(vnfInstance.getInstantiationLevel());
        instantiationLevelService.setScaleLevelForInstantiationLevel(tempInstance, vnfInstance.getInstantiationLevel(), policies);
    }

    private void updateReplicaDetailsForTempInstance(VnfInstance sourceVnfInstance, VnfInstance targetVnfInstance, boolean persistScaleInfo,
                                                     String targetVnfd,
                                                     Map<String, Object> valuesYamlMap) {
        setScaleLevelToTempInstance(sourceVnfInstance, targetVnfInstance, persistScaleInfo);
        replicaDetailsService.updateAndSetReplicaDetailsToVnfInstance(
                targetVnfd, targetVnfInstance, valuesYamlMap);
        if (persistScaleInfo) {
            updateScalingModes(sourceVnfInstance, targetVnfInstance);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateScalingModes(final VnfInstance sourceVnfInstance, final VnfInstance targetVnfInstance) {
        Map<String, Object> currentVnfControlledScaling = (Map<String, Object>) convertStringToJSONObj(
                sourceVnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        Map<String, Object> targetVnfControlledScaling = (Map<String, Object>) convertStringToJSONObj(
                targetVnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);

        Map<String, Object> currentExtensions = getCommonExtensions(currentVnfControlledScaling, targetVnfControlledScaling);

        extensionsService.updateInstanceWithExtensionsInRequest(currentExtensions, targetVnfInstance);

        updateHelmChartWithVnfControlledScalingExtension(targetVnfInstance);
    }

    private static Map<String, Object> getCommonExtensions(final Map<String, Object> currentVnfControlledScaling,
                                                            final Map<String, Object> targetVnfControlledScaling) {
        Map<String, Object> extensions = new HashMap<>();

        Map<String, Object> vnfScaling = new HashMap<>();

        if (MapUtils.isNotEmpty(currentVnfControlledScaling) && MapUtils.isNotEmpty(targetVnfControlledScaling)) {
            vnfScaling = currentVnfControlledScaling.entrySet().stream()
                    .filter(entry -> targetVnfControlledScaling.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        if (MapUtils.isNotEmpty(vnfScaling)) {
            extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);
        }
        return extensions;
    }

    private void setCombinedValuesToVnfInstance(VnfInstance tempVnfInstance, Map<String, Object> additionalParams) {
        try {
            tempVnfInstance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        } catch (JsonProcessingException e) {
            String message = String.format("Unable to convert the java object %s to json", additionalParams);
            throw new IllegalStateException(message, e);
        }
    }

    private static boolean collectPersistScaleInfoValueItAndRemoveFromAdditionalParams(Map<String, Object> additionalParams) {
        boolean persistScaleInfo = false;
        try {
            if (additionalParams.containsKey(PERSIST_SCALE_INFO)) {
                persistScaleInfo = BooleanUtils.getBooleanValue(additionalParams.get(PERSIST_SCALE_INFO));
                additionalParams.remove(PERSIST_SCALE_INFO);
            } else {
                persistScaleInfo = true;
            }
        } catch (Exception ex) {
            LOGGER.warn("Setting value of {} to false, unable to fetch {} value", PERSIST_SCALE_INFO, PERSIST_SCALE_INFO, ex);
        }
        return persistScaleInfo;
    }

    private static boolean parsePersistDmConfigValueFromAdditionalParams(Map<String, Object> additionalParams) {
        if (MapUtils.isEmpty(additionalParams)) {
            return false;
        }

        return BooleanUtils.getBooleanValue(additionalParams.getOrDefault(PERSIST_DM_CONFIG, false), false);
    }

    private boolean isScalingAspectPresentInBothVnfInstances(ChangeOperationContext context) {
        try {
            Optional<Policies> vnfInstancePolicy = vnfdHelper.getVnfdScalingInformation(
                    new JSONObject(context.getSourcePackageInfo().getDescriptorModel()));
            Optional<Policies> tempVnfInstancePolicy = vnfdHelper.getVnfdScalingInformation(
                    new JSONObject(context.getTargetPackageInfo().getDescriptorModel()));

            if (vnfInstancePolicy.isEmpty() || tempVnfInstancePolicy.isEmpty()) {
                return false;
            }
            return MapUtils.isNotEmpty(vnfInstancePolicy.get().getAllScalingAspects()) && MapUtils
                    .isNotEmpty(tempVnfInstancePolicy.get().getAllScalingAspects());
        } catch (Exception ex) {
            LOGGER.error("Exception occurred while casting policy", ex);
            return false;
        }
    }

    private void setScaleLevelToTempInstance(final VnfInstance sourceVnfInstance, final VnfInstance tempVnfInstance, final boolean persistScaleInfo) {
        if (instanceContainsInstantiationLevels(tempVnfInstance) && persistScaleInfo) {
            setTempInstanceScaleLevelToHigherValueOfMatchingAspects(sourceVnfInstance, tempVnfInstance);
        }
        if (!instanceContainsInstantiationLevels(sourceVnfInstance) && !instanceContainsInstantiationLevels(tempVnfInstance)) {
            if (persistScaleInfo) {
                setScaleEntityCurrentAndTargetNoLevels(sourceVnfInstance, tempVnfInstance);
            } else {
                setTempInstanceScaleLevelToZero(tempVnfInstance);
            }
        }
    }

    private static void setTempInstanceScaleLevelToHigherValueOfMatchingAspects(final VnfInstance vnfInstance, final VnfInstance vnfInstanceTemp) {
        for (ScaleInfoEntity scaleInfoEntity : vnfInstance.getScaleInfoEntity()) {
            for (ScaleInfoEntity tempScaleInfoEntity : vnfInstanceTemp.getScaleInfoEntity()) {
                if (scaleInfoEntity.getAspectId().equals(tempScaleInfoEntity.getAspectId())) {
                    tempScaleInfoEntity.setScaleLevel(Math.max(scaleInfoEntity.getScaleLevel(), tempScaleInfoEntity.getScaleLevel()));
                }
            }
        }
    }

    private static void setScaleEntityCurrentAndTargetNoLevels(final VnfInstance vnfInstance, final VnfInstance vnfInstanceTemp) {
        setTempInstanceScaleLevelToZero(vnfInstanceTemp);
        for (ScaleInfoEntity scaleInfoEntity : vnfInstance.getScaleInfoEntity()) {
            for (ScaleInfoEntity tempScaleInfoEntity : vnfInstanceTemp.getScaleInfoEntity()) {
                if (scaleInfoEntity.getAspectId().equals(tempScaleInfoEntity.getAspectId())) {
                    tempScaleInfoEntity.setScaleLevel(scaleInfoEntity.getScaleLevel());
                }
            }
        }
    }

    private static void setTempInstanceScaleLevelToZero(final VnfInstance vnfInstanceTemp) {
        for (ScaleInfoEntity scaleInfo : vnfInstanceTemp.getScaleInfoEntity()) {
            scaleInfo.setScaleLevel(0);
        }
    }

    private boolean instanceContainsInstantiationLevels(final VnfInstance instance) {
        Map<String, InstantiationLevels> allInstantiationLevels = instantiationLevelService.getInstantiationLevelsFromPolicies(instance);
        return MapUtils.isNotEmpty(allInstantiationLevels);
    }
}
