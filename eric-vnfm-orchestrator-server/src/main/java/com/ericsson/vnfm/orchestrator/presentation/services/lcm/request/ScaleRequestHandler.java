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

import static java.lang.Boolean.TRUE;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.updateCombinedValuesEntity;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScaleRequestHandler extends GrantingLifecycleRequestHandler implements GrantingRequestHandler {

    private static final String ONLY_NON_SCALABLE_VDUS_IN_RESOURCES = "Resources contain only non-scalable VDUs";
    private static final int DEFAULT_NUMBER_OF_STEPS = 1;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private LcmOpAdditionalParamsProcessor lcmOpAdditionalParamsProcessor;

    @Autowired
    private PackageService packageService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.SCALE;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        final ScaleVnfRequest scaleVnfRequest = validateStepsOrSetDefaultValue((ScaleVnfRequest) request);
        instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) scaleVnfRequest.getAdditionalParams());
        super.commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.SCALE);
        checkAndCastObjectToMap(scaleVnfRequest.getAdditionalParams());
        scaleService.validateScaleRequestAndPolicies(vnfInstance, scaleVnfRequest);
    }
    @Override
    public Map<String, Object> formatParameters(final VnfInstance vnfInstance,
                                                final Object request,
                                                final LifecycleOperationType type,
                                                final Map<String, Object> valuesYamlMap) {

        final Map<String, Object> additionalParams = super.formatParameters(vnfInstance, request, type, valuesYamlMap);
        setDefaultHelmNoHooks(additionalParams);

        return additionalParams;
    }

    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        try {
            Map<String, Object> operationValuesFileMap = convertStringToJSONObj(operation.getValuesFileParams());
            updateTempInstance(vnfInstance, operationValuesFileMap, additionalParams);
        } catch (JsonProcessingException ex) {
            throw new InternalRuntimeException(ex);
        }
    }

    @Override
    public void processValuesYaml(@NotNull final Map<String, Object> valuesYamlMap,
                                  final VnfInstance vnfInstance,
                                  final Object request,
                                  final LifecycleOperation operation) {
        Map<String, Object> processedValuesYamlMap = processValuesYaml(vnfInstance, operation);
        valuesYamlMap.clear();
        valuesYamlMap.putAll(processedValuesYamlMap);

        if (vnfInstance.isRel4()) {
            checkAtLeastOneVduInResourcesIsScalable(vnfInstance, valuesYamlMap);
        }
    }

    private Map<String, Object> processValuesYaml(final VnfInstance vnfInstance,
                                                  final LifecycleOperation operation) {
        final String targetVnfdId = vnfInstance.getVnfDescriptorId();
        Map<String, Object> valuesYamlMap = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);
        lcmOpAdditionalParamsProcessor.process(valuesYamlMap, targetVnfdId, operation);
        return valuesYamlMap;
    }

    @Override
    public void createTempInstance(final VnfInstance vnfInstance, final Object request) {
        scaleService.createTempInstance(vnfInstance, (ScaleVnfRequest) request);
    }

    @Override
    public void sendRequest(final VnfInstance vnfInstance, final LifecycleOperation operation, final Object request,
                            final Path toValuesFile) {
        ScaleVnfRequest scaleVnfRequest = (ScaleVnfRequest) request;
        WorkflowRoutingResponse response = workflowRoutingService
                .routeScaleRequest(vnfInstance, operation, scaleVnfRequest);
        checkAndProcessFailedError(operation, response);
    }

    @Override
    protected void doVerifyGrantingResources(LifecycleOperation operation, final Object request, final Map<String, Object> valuesYamlMap) {
        VnfInstance vnfInstance = operation.getVnfInstance();

        JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());

        LOGGER.info("Starting granting resources delta calculation for Scale operation. Package ID: {}", vnfInstance.getVnfPackageId());
        LOGGER.info("Starting calculate current resource definitions for Scale operation");
        final Map<String, Object> valuesYamlMapCopy = new HashMap<>(valuesYamlMap);
        final List<ResourceDefinition> currentResourceDefinitions =
                grantingResourceDefinitionCalculation
                        .calculateRel4ResourcesInUse(vnfd, vnfInstance, valuesYamlMapCopy, vnfInstance.getVnfDescriptorId());
        LOGGER.info("Starting calculate target resource definitions for Scale operation");
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        final List<ResourceDefinition> targetResourceDefinitions =
                grantingResourceDefinitionCalculation
                        .calculateRel4ResourcesInUse(vnfd, tempInstance, valuesYamlMapCopy, tempInstance.getVnfDescriptorId());

        fillAndExecuteGrantRequest(targetResourceDefinitions, currentResourceDefinitions, vnfInstance, operation, new GrantRequest());
    }

    @Override
    protected GrantedLcmOperationType getGrantingOperationType() {
        return GrantedLcmOperationType.SCALE;
    }

    private static ScaleVnfRequest validateStepsOrSetDefaultValue(final ScaleVnfRequest scaleVnfRequest) {
        if (scaleVnfRequest.getNumberOfSteps() == null) {
            LOGGER.debug("Number of steps is not provided, setting default");
            scaleVnfRequest.setNumberOfSteps(DEFAULT_NUMBER_OF_STEPS);
        } else if (scaleVnfRequest.getNumberOfSteps() <= 0) {
            throw new IllegalArgumentException("Invalid scale step provided, Scale step should be a positive integer");
        }

        return scaleVnfRequest;
    }

    private static void setDefaultHelmNoHooks(final Map<String, Object> additionalParams) {
        if (!additionalParams.containsKey(HELM_NO_HOOKS)) {
            additionalParams.put(HELM_NO_HOOKS, TRUE.toString());
        }
    }

    private void updateTempInstance(VnfInstance instance, final Map<String, Object> valuesYamlMap,
                                    final Map<String, Object> additionalParams) throws JsonProcessingException {
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        updateCombinedValuesEntity(tempInstance, valuesYamlMap, additionalParams);
        scaleService.removeHelmChartFromTempInstance(instance, tempInstance);
        helmChartHelper.completeDisabledHelmCharts(tempInstance.getHelmCharts());
        instance.setTempInstance(convertObjToJsonString(tempInstance));
    }

    public void checkAtLeastOneVduInResourcesIsScalable(final VnfInstance vnfInstance,
                                                        final Map<String, Object> valuesYamlMap) {
        final JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());
        final VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        final List<ResourceDefinition> resourceDefinitions =
                grantingResourceDefinitionCalculation
                        .calculateRel4ResourcesInUse(vnfd, tempInstance, valuesYamlMap, null);

        checkAtLeastOneVduInResourcesIsScalable(resourceDefinitions, vnfInstance);
    }

    private void checkAtLeastOneVduInResourcesIsScalable(List<ResourceDefinition> resources, VnfInstance vnfInstance) {
        Set<String> scalableVdusNames = replicaDetailsMapper.getScalableVdusNames(vnfInstance);
        boolean areAllVdusNonScalable = resources.stream()
                .map(ResourceDefinition::getVduId)
                .noneMatch(scalableVdusNames::contains);

        if (areAllVdusNonScalable) {
            throw new OperationNotSupportedException(LifecycleOperationType.SCALE.toString(), vnfInstance.getVnfPackageId(),
                                                     ONLY_NON_SCALABLE_VDUS_IN_RESOURCES);
        }
    }
}