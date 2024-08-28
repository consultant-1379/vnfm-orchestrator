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

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEFAULT_TITLE_ERROR_FOR_LCM_OP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.ASPECTS_SHOULD_BE_PRESENT_IN_REPLICA_DETAILS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VDU_INITIAL_DELTA_SHOULD_BE_PRESENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_INSTANCE_IS_BEING_PROCESSED;
import static com.ericsson.vnfm.orchestrator.utils.SupportedOperationUtils.validateOperationIsSupported;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.vnfm.orchestrator.model.RequestWithAdditionalParams;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AlreadyInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationAlreadyInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

/**
 * Template Method Pattern base class for handling a lifecycle operation request.
 * Logic which is common to each operation type sits in this class.
 * Abstract methods are to be implemented by each operation's sub class
 */
@Slf4j
public abstract class LifecycleRequestHandler implements OperationRequestHandler {

    @Autowired
    protected LcmOpErrorManagementService lcmOpErrorManagementService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Value("${workflow.command.execute.defaultTimeOut}")
    private long defaultTimeout;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private ValuesFileComposer valuesFileComposer;

    @Autowired
    private UsernameCalculationService usernameCalculationService;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private VnfInstanceService vnfInstanceService;

    @Autowired
    private PackageService packageService;

    public void commonValidation(final VnfInstance vnfInstance, final InstantiationState instantiationState, LCMOperationsEnum operationType) {
        checkOperationsByInstanceNotInProgress(vnfInstance);
        checkVnfNotInState(vnfInstance, instantiationState);
        validateOperationIsSupported(vnfInstance, operationType.getOperation());
    }

    public void setExtensionsAndInstantiationLevelInOperationInTempInstance(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        setExtensionsAndInstantiationLevelInOperations(tempInstance, operation);
    }

    public void setExtensionsAndInstantiationLevelInOperationInCurrentInstance(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        setExtensionsAndInstantiationLevelInOperations(vnfInstance, operation);
    }

    public void setExtensionsAndInstantiationLevelInOperations(VnfInstance vnfInstance, LifecycleOperation operation) {
        if (!Strings.isNullOrEmpty(vnfInstance.getInstantiationLevel())) {
            operation.setInstantiationLevel(vnfInstance.getInstantiationLevel());
        }
        if (!Strings.isNullOrEmpty(vnfInstance.getVnfInfoModifiableAttributesExtensions())) {
            operation.setVnfInfoModifiableAttributesExtensions(vnfInstance.getVnfInfoModifiableAttributesExtensions());
        }
        try {
            if (!CollectionUtils.isEmpty(vnfInstance.getScaleInfoEntity())) {
                operation.setScaleInfoEntities(mapper.writeValueAsString(vnfInstance.getScaleInfoEntity()));
            }
        } catch (JsonProcessingException jpe) {
            LOGGER.warn("Unable to take backup of scale entities in lifecycle table", jpe);
        }
    }

    @Override
    public LifecycleOperation persistOperation(VnfInstance vnfInstance, Object request, String requestUsername,
                                               LifecycleOperationType type, Map<String, Object> valuesYamlMap, final String applicationTimeout) {

        LifecycleOperation operation = buildOperation(vnfInstance, request, requestUsername, type);

        if (MapUtils.isNotEmpty(valuesYamlMap)) {
            operation.setValuesFileParams(new JSONObject(valuesYamlMap).toString());
        }

        lifeCycleManagementHelper.setTimeouts(operation, applicationTimeout);

        lifeCycleManagementHelper.persistLifecycleOperationInProgress(operation, vnfInstance, type);

        vnfInstance.setOperationOccurrenceId(operation.getOperationOccurrenceId());

        return operation;
    }

    @Override
    public Map<String, Object> formatParameters(final VnfInstance vnfInstance, final Object request,
                                                final LifecycleOperationType type, final Map<String, Object> valuesYamlMap) {
        Map<String, Object> additionalParams = getAdditionalParams(request);

        final String applicationTimeout = lifeCycleManagementHelper.getApplicationTimeout(additionalParams);
        additionalParams.put(APPLICATION_TIME_OUT, applicationTimeout);

        setAdditionalParams(request, additionalParams);

        return additionalParams;
    }

    private LifecycleOperation buildOperation(final VnfInstance vnfInstance, final Object request, final String requestUsername,
                                              final LifecycleOperationType lifecycleOperationType) {
        LifecycleOperation operation = getLifecycleOperation(vnfInstance, lifecycleOperationType, request);

        operation.setUsername(requestUsername);
        operation.setVnfSoftwareVersion(vnfInstance.getVnfSoftwareVersion());
        operation.setVnfProductName(vnfInstance.getVnfProductName());
        operation.setStartTime(LocalDateTime.now());
        operation.setStateEnteredTime(LocalDateTime.now());
        operation.setSourceVnfdId(vnfInstance.getVnfDescriptorId());
        setDefaultTimeoutToOperation(operation, defaultTimeout);
        return operation;
    }

    private LifecycleOperation getLifecycleOperation(final VnfInstance vnfInstance,
                                                     final LifecycleOperationType lifecycleOperationType,
                                                     final Object request) {
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        operation.setOperationState(LifecycleOperationState.STARTING);
        operation.setLifecycleOperationType(lifecycleOperationType);
        try {
            operation.setOperationParams(mapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to save the request to lifecycle operation details", e);
        }
        return operation;
    }

    private static void checkVnfNotInState(final VnfInstance vnfInstance, final InstantiationState undesiredState) {
        if (undesiredState.equals(vnfInstance.getInstantiationState())) {
            if (undesiredState.equals(INSTANTIATED)) {
                throw new AlreadyInstantiatedException(vnfInstance);
            } else {
                throw new NotInstantiatedException(vnfInstance);
            }
        }
    }

    protected String getPoliciesAsJsonString(final Policies policies) {
        String policiesAsJsonString;
        try {
            policiesAsJsonString = mapper.writeValueAsString(policies);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to write policies object to json", e);
        }
        return policiesAsJsonString;
    }

    protected void checkAndProcessFailedError(final LifecycleOperation operation, final WorkflowRoutingResponse response) {
        if (response.getHttpStatus().isError()) {
            //5xx error means that smth went wrong with external service and this operation can be retried automatically
            if (response.getHttpStatus().is5xxServerError()) {
                throw new HttpServiceInaccessibleException(response.getErrorMessage());
            }
            lcmOpErrorManagementService.process(operation, response.getHttpStatus(), DEFAULT_TITLE_ERROR_FOR_LCM_OP, response.getErrorMessage());
        }
    }

    public void checkAndProcessFailedError(final LifecycleOperation operation,
                                           final WorkflowRoutingResponse response,
                                           final String releaseName) {

        if (response.getHttpStatus().isError()) {
            //5xx error means that smth went wrong with external service and this operation can be retried automatically
            if (response.getHttpStatus().is5xxServerError()) {
                throw new HttpServiceInaccessibleException(response.getErrorMessage());
            }
            operation.getVnfInstance().getHelmCharts().stream()
                    .filter(chart -> StringUtils.equals(releaseName, chart.getReleaseName())).findFirst()
                    .ifPresent(chart -> chart.setState(LifecycleOperationState.FAILED.toString()));
            lcmOpErrorManagementService.process(operation, response.getHttpStatus(), DEFAULT_TITLE_ERROR_FOR_LCM_OP, response.getErrorMessage());
        }
    }

    public void checkAndProcessFailedError(final LifecycleOperation operation, final ResponseEntity<Object> response,
                                           final String releaseName) {
        if (response.getStatusCode().isError()) {
            final Object body = response.getBody();
            final String errorDetail = body != null ? body.toString() : StringUtils.EMPTY;

            //5xx error means that smth went wrong with external service and this operation can be retried automatically
            if (response.getStatusCode().is5xxServerError()) {
                throw new HttpServiceInaccessibleException(errorDetail);
            }
            operation.getVnfInstance().getHelmCharts().stream()
                    .filter(chart -> StringUtils.equals(releaseName, chart.getReleaseName())).findFirst()
                    .ifPresent(chart -> chart.setDownsizeState(LifecycleOperationState.FAILED.toString()));
            lcmOpErrorManagementService.process(operation, (HttpStatus) response.getStatusCode(), DEFAULT_TITLE_ERROR_FOR_LCM_OP, errorDetail);
        }
    }

    public static boolean parameterPresent(final Map additionalParams, final String expectedParameter) {
        if (additionalParams != null) {
            @SuppressWarnings("unchecked")
            Set<String> keys = additionalParams.keySet();
            return keys.stream().anyMatch(key -> StringUtils.equalsIgnoreCase(key, expectedParameter));
        } else {
            return false;
        }
    }

    public static boolean isMapContainsNotOnlyExpectedParameters(final Map<String, Object> map,
                                                                 final List<String> expectedParameters) {
        return !CollectionUtils.isEmpty(map) && map.keySet().stream()
                .anyMatch(key -> !containsIgnoreCase(expectedParameters, key));
    }

    private static boolean containsIgnoreCase(List<String> list, String expectedParameter) {
        for (String verifiableElement : list) {
            if (StringUtils.equalsIgnoreCase(verifiableElement, expectedParameter)) {
                return true;
            }
        }
        return false;
    }

    static void setInstanceWithCleanUpResources(final VnfInstance vnfInstance, final Map additionalParams) {
        if (!CollectionUtils.isEmpty(additionalParams) && additionalParams.containsKey(CLEAN_UP_RESOURCES)) {
            vnfInstance.setCleanUpResources(BooleanUtils.getBooleanValue(additionalParams.get(CLEAN_UP_RESOURCES)));
        } else {
            LOGGER.info("CleanUpResources is not provided, setting default");
            vnfInstance.setCleanUpResources(true);
        }
    }

    public void setAlarmSupervisionWithWarning(final VnfInstance vnfInstance, EnmOperationEnum enableOrDisable) {
        try {
            ossNodeService.setAlarmSuperVisionInENM(vnfInstance, enableOrDisable);
        } catch (Exception e) {
            LOGGER.warn("{} supervision in ENM failed", enableOrDisable.getOperation(), e);
        }
    }

    public static Map<String, Object> getAdditionalParams(Object request) {
        if (request instanceof RequestWithAdditionalParams) {
            var paramsRequest = (RequestWithAdditionalParams) request;
            return checkAndCastObjectToMap(paramsRequest.getAdditionalParams());
        } else {
            return new HashMap<>();
        }
    }

    protected void updateHelmChartWithVnfControlledScalingExtension(final VnfInstance tempInstance) {
        if (!instanceSupportsScalingAndUpdatedScaleParamsPresent(tempInstance)) {
            LOGGER.debug("Skipping updating helm charts with extensions as instance doesn't support scaling and scale params are not present");
            return;
        }
        Map<String, String> vnfControlledScaling = vnfInstanceService.getVnfControlledScalingExtension(tempInstance);
        if (MapUtils.isEmpty(vnfControlledScaling)) {
            LOGGER.debug("Skipping updating helm charts with extensions as instance scale params are not present");
            return;
        }
        Map<String, ScalingAspectDataType> aspects = ScalingUtils.getScalingDetails(tempInstance.getPolicies(), mapper);
        if (CollectionUtils.isEmpty(aspects)) {
            LOGGER.debug("Skipping updating helm charts with extensions as aspects are not present");
            return;
        }

        List<HelmChart> helmCharts = tempInstance.getHelmCharts();
        for (Map.Entry<String, String> aspect : vnfControlledScaling.entrySet()) {
            if (!aspects.containsKey(aspect.getKey())) {
                continue;
            }

            final Collection<ScalingAspectDeltas> scalingAspectDeltas = getAspectDeltas(aspects, aspect.getKey());
            for (final var aspectDelta : scalingAspectDeltas) {
                Arrays.stream(aspectDelta.getTargets())
                        .forEach(target -> updateHelmCharts(helmCharts, aspect, aspectDelta, target));
            }
        }
    }

    protected void validatePreviousOperationStateCompleted(final VnfInstance vnfInstance) {
        Optional<LifecycleOperation> lastSuccessfulOperation = lcmOpSearchService.searchLastOperation(vnfInstance);
        if (lastSuccessfulOperation.isPresent() && !LifecycleOperationState.COMPLETED.equals(lastSuccessfulOperation.get().getOperationState())) {
            throw new IllegalStateException(format("Previous Lifecycle operation state: %s, but has to be COMPLETED in order to perform "
                                                           + "%s operation.", lastSuccessfulOperation.get().getOperationState(), getType()));
        }
    }

    protected VnfInstance getVnfInstanceFromJson(String vnfInstance) {
        try {
            return mapper.readValue(vnfInstance, VnfInstance.class);
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException(String.format("VnfInstance json parsing failed %s", e.getMessage()), e);
        }
    }

    private static boolean instanceSupportsScalingAndUpdatedScaleParamsPresent(final VnfInstance tempInstance) {
        if (LifeCycleManagementHelper.vnfInstanceSupportsScaling(tempInstance)) {
            return StringUtils.isNotEmpty(tempInstance.getVnfInfoModifiableAttributesExtensions());
        }
        return false;
    }

    private static Collection<ScalingAspectDeltas> getAspectDeltas(final Map<String, ScalingAspectDataType> aspects,
                                                                   final String aspectFromVnfControlledScaling) {

        final Set<ScalingAspectDeltas> aspectDeltas = aspects.get(aspectFromVnfControlledScaling)
                .getAllScalingAspectDelta()
                .values()
                .stream()
                .filter(aspectDelta -> aspectDelta.getProperties().getAspect().equals(aspectFromVnfControlledScaling))
                .collect(toSet());

        if (aspectDeltas.isEmpty()) {
            throw new IllegalArgumentException(format(ASPECTS_SHOULD_BE_PRESENT_IN_REPLICA_DETAILS, aspectFromVnfControlledScaling));
        }

        return aspectDeltas;
    }

    private void updateHelmCharts(final List<HelmChart> helmCharts,
                                  final Map.Entry<String, String> aspect,
                                  final ScalingAspectDeltas scalingAspectDelta,
                                  final String target) {
        for (HelmChart helmChart : helmCharts) {
            if (helmChart.getHelmChartType().equals(HelmChartType.CNF) && Objects.nonNull(helmChart.getReplicaDetails())) {
                Map<String, ReplicaDetails> replicateDetailsFromHelmChart =
                        replicaDetailsMapper.getReplicaDetailsFromHelmChart(helmChart);
                if (replicateDetailsFromHelmChart.containsKey(target)) {
                    ReplicaDetails replicaDetails = replicateDetailsFromHelmChart.get(target);
                    prepareManualControlledReplicaDetails(aspect, replicaDetails);
                    prepareCismControlledReplicaDetails(aspect, scalingAspectDelta, target, replicaDetails);
                    helmChart.setReplicaDetails(Utility.convertObjToJsonString(replicateDetailsFromHelmChart));
                }
            }
        }
    }

    private static void prepareManualControlledReplicaDetails(final Map.Entry<String, String> aspect, final ReplicaDetails replicaDetails) {
        if (aspect.getValue().equals(MANUAL_CONTROLLED)) {
            replicaDetails.setAutoScalingEnabledValue(false);
        }
    }

    private static void prepareCismControlledReplicaDetails(final Map.Entry<String, String> aspect,
                                                            final ScalingAspectDeltas scalingAspectDelta,
                                                            final String target,
                                                            final ReplicaDetails replicaDetails) {
        if (aspect.getValue().equals(CISM_CONTROLLED)) {
            replicaDetails.setAutoScalingEnabledValue(true);
            replicaDetails.setMaxReplicasCount(replicaDetails.getCurrentReplicaCount());
            if (replicaDetails.getMinReplicasCount() == null) {
                replicaDetails.setMinReplicasCount(calculateMinReplicaCount(scalingAspectDelta, target));
            }
        }
    }

    private static Integer calculateMinReplicaCount(final ScalingAspectDeltas scalingAspectDelta, final String target) {
        return scalingAspectDelta.getAllInitialDelta()
                .values()
                .stream()
                .filter(initialDeltaIncludeTarget(target))
                .map(getInitialDeltaNumberOfInstancesFunction())
                .findFirst()
                .orElseThrow(() -> new NotFoundException(VDU_INITIAL_DELTA_SHOULD_BE_PRESENT));
    }

    private static Function<InitialDelta, Integer> getInitialDeltaNumberOfInstancesFunction() {
        return initialDelta -> initialDelta.getProperties()
                .getInitialDelta()
                .getNumberOfInstances();
    }

    private static Predicate<InitialDelta> initialDeltaIncludeTarget(final String target) {
        return initialDelta -> Arrays.asList(initialDelta.getTargets())
                .contains(target);
    }

    public static void setAdditionalParams(Object request, final Map<String, Object> additionalParams) {
        if (request instanceof RequestWithAdditionalParams) {
            var paramsRequest = (RequestWithAdditionalParams) request;
            paramsRequest.setAdditionalParams(additionalParams);
        }
    }

    public static void removeExcessAdditionalParams(final Map<String, Object> additionalParams) {
        if (!CollectionUtils.isEmpty(additionalParams)) {
            additionalParams.remove(VALUES_YAML_ADDITIONAL_PARAMETER);
            additionalParams.keySet()
                    .removeIf(parameter -> !EVNFM_PARAMS.contains(parameter)
                            && !EVNFM_PARAMS_FOR_WFS.contains(parameter)
                            && !parameter.contains(DAY0_CONFIGURATION_PREFIX));
        }
    }

    @Override
    public void persistOperationAndInstanceAfterExecution(VnfInstance vnfInstance, LifecycleOperation operation) {
        databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
    }

    protected void checkOperationsByInstanceNotInProgress(final VnfInstance vnfInstance) {
        Integer operationsCountInProgress = databaseInteractionService.getOperationsCountNotInTerminalStatesByVnfInstance(vnfInstance);
        if (operationsCountInProgress > 0) {
            throw new OperationAlreadyInProgressException(String.format(VNF_INSTANCE_IS_BEING_PROCESSED,
                                                                        vnfInstance.getVnfInstanceId()));
        }
    }

    protected void setDefaultTimeoutToOperation(LifecycleOperation operation, long defaultTimeout) {
        operation.setApplicationTimeout(Long.toString(defaultTimeout));
        LocalDateTime defaultExpiredTime = LocalDateTime
                .now()
                .plusSeconds(defaultTimeout + 120);
        operation.setExpiredApplicationTime(defaultExpiredTime);
    }

    @Override
    public void processValuesYaml(final Map<String, Object> valuesYamlMap,
                                  final VnfInstance vnfInstance,
                                  final Object request,
                                  final LifecycleOperation operation) {
    }
}
