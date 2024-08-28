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
package com.ericsson.vnfm.orchestrator.presentation.services.scale;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MAX_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MIN_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleOperationUtils.getScalingDataType;
import static com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleOperationUtils.isLinearScaling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.am.shared.vnfd.model.policies.VduLevelDataType;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ScaleParametersServiceImpl implements ScaleParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleParametersServiceImpl.class);

    private static final String CANT_PERFORM_SCALE_ERROR_MESSAGE = "Cannot perform scale operation as replica count is ";
    private static final String CANT_PERFORM_SCALE_FOR_TARGET = "Cannot perform scale for target %s, because the "
            + "calculated replica count is either zero or less than zero";
    private static final String ASPECT_ID_NOT_SUPPORTED =
            "Provided aspectId [%s] does not exist in VNFD. Available " + "aspectIds are: %s";
    private static final String SCALE_ASPECT_NOT_DEFINED_ERROR_MESSAGE = "Scaling Aspect %s not defined for instance %s";


    private static final TypeReference<Map<String, Integer>> RESOURCE_DETAILS_TYPE_REF = new TypeReference<>() {
    };


    @Autowired
    private VnfInstanceService vnfInstanceService;

    @Autowired
    private ObjectMapper mapper;


    public Map<String, Map<String, Integer>> getScaleParameters(VnfInstance vnfInstance,
                                                                ScaleVnfRequest scaleVnfRequest) {
        Policies policies = vnfInstanceService.getPolicies(vnfInstance);
        String aspectId = scaleVnfRequest.getAspectId();
        Optional<ScaleInfoEntity> selectedAspectScaleInfoEntity = vnfInstanceService.getScaleInfoForAspect(vnfInstance, aspectId);
        if (selectedAspectScaleInfoEntity.isPresent()) {
            Optional<ScalingAspectDataType> scalingAspectDataType = getScalingDataType(aspectId, policies);
            if (scalingAspectDataType.isEmpty()) {
                throw new IllegalArgumentException(String.format(SCALE_ASPECT_NOT_DEFINED_ERROR_MESSAGE, aspectId,
                                                                 vnfInstance.getVnfInstanceId()));
            }

            ScaleLevelValidator.validate(scalingAspectDataType.get(), selectedAspectScaleInfoEntity.get(), scaleVnfRequest);
            Map<String, Map<String, Integer>> scaleParameters = getCurrentScaleParameters(vnfInstance,
                                                                                          policies.getAllInitialDelta());
            Map<String, Map<String, Integer>> scaleRequestParametersForAspect = getScaleParametersForAspect(
                    scalingAspectDataType.get(), selectedAspectScaleInfoEntity.get().getScaleLevel(),
                    scaleVnfRequest.getNumberOfSteps(), vnfInstance,
                    scaleVnfRequest.getType());
            scaleParameters.putAll(scaleRequestParametersForAspect);
            LOGGER.info("Parameters for scaling are: {}", scaleParameters);
            return scaleParameters;
        } else {
            List<String> aspectIds = vnfInstance.getScaleInfoEntity().stream()
                    .map(scaleInfoEntity -> scaleInfoEntity.getAspectId()).collect(Collectors.toList());
            throw new IllegalArgumentException(String.format(ASPECT_ID_NOT_SUPPORTED, aspectId, aspectIds));
        }
    }

    @Override
    public int calculateNumberOfInstancesForScale(ScalingAspectDataType scalingAspectDataType,
                                                  int currentScaleLevel,
                                                  int scaleStep,
                                                  ScalingAspectDeltas scaleDelta,
                                                  ScaleVnfRequest.TypeEnum scaleOperation) {

        if (isLinearScaling(scalingAspectDataType)) {
            return calculateNumberOfInstancesForLinearScaling(scaleStep, scaleDelta);
        } else {
            return calculateNumberOfInstancesForNonLinearScaling(currentScaleLevel, scaleStep, scaleDelta, scalingAspectDataType, scaleOperation);
        }
    }

    private Map<String, Map<String, Integer>> getCurrentScaleParameters(final VnfInstance vnfInstance,
                                                                        Map<String, InitialDelta> allInitialDelta) {
        try {
            Map<String, Map<String, Integer>> currentScaleParameters = new HashMap<>();
            Map<String, Integer> resourceDetails = mapper.readValue(vnfInstance.getResourceDetails(), RESOURCE_DETAILS_TYPE_REF);
            for (Map.Entry<String, Integer> resourceDetail : resourceDetails.entrySet()) {
                Map<String, Integer> scaleParameters = getAllReplicaParameter(
                        getInitialDeltaKeyForTarget(allInitialDelta, resourceDetail.getKey()),
                        resourceDetail.getValue());
                currentScaleParameters.put(resourceDetail.getKey(), scaleParameters);
            }
            LOGGER.info("Current Scale parameters are: {}", currentScaleParameters);
            return currentScaleParameters;
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException(String.format("Json parsing failed %s", e.getMessage()), e);
        }
    }

    private static Map<String, Integer> getAllReplicaParameter(String paramName, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException(CANT_PERFORM_SCALE_ERROR_MESSAGE + count);
        }
        Map<String, Integer> replicaParameters = new HashMap<>();
        replicaParameters.put(String.format(REPLICA_PARAMETER_NAME, paramName), count);
        replicaParameters.put(String.format(MIN_REPLICA_PARAMETER_NAME, paramName), count);
        replicaParameters.put(String.format(MAX_REPLICA_PARAMETER_NAME, paramName), count);
        return replicaParameters;
    }

    private static String getInitialDeltaKeyForTarget(Map<String, InitialDelta> allInitialDelta, String target) {
        List<String> allMatchingKeys = new ArrayList<>();
        for (Map.Entry<String, InitialDelta> initialDelta : allInitialDelta.entrySet()) {
            if (Arrays.asList(initialDelta.getValue().getTargets()).contains(target)) {
                allMatchingKeys.add(initialDelta.getKey());
            }
        }
        if (allMatchingKeys.size() > 1) {
            throw new IllegalArgumentException("Scale cannot be performed because multiple initial delta defined" +
                                                       " for target " + target);
        } else if (allMatchingKeys.isEmpty()) {
            throw new IllegalArgumentException("Scale cannot be performed because no initial delta defined" +
                                                       " for target " + target);
        }
        return allMatchingKeys.get(0);
    }

    private Map<String, Map<String, Integer>> getScaleParametersForAspect(ScalingAspectDataType scalingAspectDataType,
                                                                          int currentScaleLevel, int scaleStep, VnfInstance vnfInstance,
                                                                          ScaleVnfRequest.TypeEnum scaleOperation) {
        Map<String, Map<String, Integer>> allReplicaParameters = new HashMap<>();
        Map<String, ScalingAspectDeltas> allScalingAspectDelta = scalingAspectDataType.getAllScalingAspectDelta();
        for (Map.Entry<String, ScalingAspectDeltas> scalingAspectDeltaEntry : allScalingAspectDelta.entrySet()) {
            ScalingAspectDeltas value = scalingAspectDeltaEntry.getValue();
            int numberOfInstances = calculateNumberOfInstancesForScale(scalingAspectDataType, currentScaleLevel, scaleStep, value, scaleOperation);
            Map<String, InitialDelta> allInitialDelta = value.getAllInitialDelta();
            for (Map.Entry<String, InitialDelta> initialDeltaEntry : allInitialDelta.entrySet()) {
                String[] targets = initialDeltaEntry.getValue().getTargets();
                Map<String, Integer> currentResourcesDetails = getCurrentResourcesDetails(vnfInstance);
                getReplicaParameters(targets, numberOfInstances, currentResourcesDetails, initialDeltaEntry.getKey(),
                                     scaleOperation, allReplicaParameters);
            }
        }
        LOGGER.info("Scale Parameters for aspect are: {}", allReplicaParameters);
        return allReplicaParameters;
    }

    private static int calculateNumberOfInstancesForLinearScaling(int scaleStep, ScalingAspectDeltas scaleDelta) {
        Map<String, VduLevelDataType> deltas = scaleDelta.getProperties().getDeltas();
        int delta = deltas.get(deltas.keySet().toArray()[0]).getNumberOfInstances();
        return scaleStep * delta;
    }

    private static int calculateNumberOfInstancesForNonLinearScaling(int currentScaleLevel,
                                                                     int scaleStep,
                                                                     ScalingAspectDeltas scaleDelta,
                                                                     ScalingAspectDataType scalingAspectDataType,
                                                                     ScaleVnfRequest.TypeEnum scaleOperation) {

        Map<String, VduLevelDataType> deltas = scaleDelta.getProperties().getDeltas();
        List<String> allDeltaName = scalingAspectDataType.getStepDeltas();
        int tempScaleLevel = 1;
        //This initialization is for scaleIn for scale out this will be reset in the while loop
        //This is required because for scaleIn first delta will be taken on the basis of current scale level
        //For scale-Out one would be added to find the first delta
        int nextDelta = currentScaleLevel;
        int numberOfInstanceToAdd = 0;
        while (tempScaleLevel <= scaleStep) {
            LOGGER.info("Start :: Next Delta :: {}, Number of Instance to add :: {}, temp scale level :: {}, "
                                + "current scale level :: {}", nextDelta, numberOfInstanceToAdd, tempScaleLevel, currentScaleLevel);
            //If it is scale-Out operation then the current scale level scale will be added
            if (ScaleVnfRequest.TypeEnum.OUT.equals(scaleOperation)) {
                nextDelta = currentScaleLevel + tempScaleLevel;
            }
            if (nextDelta > deltas.size()) {
                numberOfInstanceToAdd += deltas.get(allDeltaName.get(deltas.size() - 1)).getNumberOfInstances();
            } else {
                numberOfInstanceToAdd += deltas.get(allDeltaName.get(nextDelta - 1)).getNumberOfInstances();
            }
            if (ScaleVnfRequest.TypeEnum.IN.equals(scaleOperation)) {
                nextDelta = currentScaleLevel - tempScaleLevel;
            }
            tempScaleLevel++;
            LOGGER.info("Start :: Next Delta :: {}, Number of Instance to add :: {}, temp scale level :: {}, "
                                + "current scale level :: {}", nextDelta, numberOfInstanceToAdd, tempScaleLevel, currentScaleLevel);
        }
        return numberOfInstanceToAdd;
    }

    private static void getReplicaParameters(String[] targets, int numberOfInstances,
                                             Map<String, Integer> currentResourcesDetails, String initialDeltaKey,
                                             ScaleVnfRequest.TypeEnum scaleOperation, Map<String, Map<String, Integer>> allReplicaParameters) {
        for (String target : targets) {
            try {
                Map<String, Integer> replicaParameterForTarget = calculateAndGetReplicaParameterForTarget(
                        numberOfInstances, currentResourcesDetails.get(target), initialDeltaKey, scaleOperation);
                allReplicaParameters.put(target, replicaParameterForTarget);
            } catch (IllegalArgumentException iae) {
                if (iae.getMessage().contains(CANT_PERFORM_SCALE_ERROR_MESSAGE)) {
                    throw new IllegalArgumentException(String.format(CANT_PERFORM_SCALE_FOR_TARGET, target), iae);
                } else {
                    throw new IllegalArgumentException(iae.getMessage(), iae);
                }
            }
        }
    }

    private static Map<String, Integer> calculateAndGetReplicaParameterForTarget(int numberOfInstances,
                                                                                 int currentReplicaParameter, String replicaParameterName,
                                                                                 ScaleVnfRequest.TypeEnum scaleOperation) {
        if (ScaleVnfRequest.TypeEnum.OUT.equals(scaleOperation)) {
            return getAllReplicaParameter(replicaParameterName, currentReplicaParameter + numberOfInstances);
        } else {
            return getAllReplicaParameter(replicaParameterName, currentReplicaParameter - numberOfInstances);
        }
    }

    private Map<String, Integer> getCurrentResourcesDetails(VnfInstance vnfInstance) {
        try {
            String resourcesDetails = vnfInstance.getResourceDetails();
            if (StringUtils.isEmpty(resourcesDetails)) {
                throw new IllegalArgumentException(
                        "Scale cannot be done, as Resources details not updated " + "during instantiation/upgrade");
            } else {
                return mapper.readValue(resourcesDetails, Map.class);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Unable to parse resource details, Invalid value provided during instantiation/upgrade", ex);
        }
    }
}
