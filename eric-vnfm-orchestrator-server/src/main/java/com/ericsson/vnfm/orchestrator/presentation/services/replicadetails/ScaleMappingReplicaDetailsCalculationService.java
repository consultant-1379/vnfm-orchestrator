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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.ScalingMapUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ScaleMappingReplicaDetailsCalculationService {

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private ReplicaDetailsBuilder replicaDetailsBuilder;

    public Map<String, ReplicaDetails> calculate(final String descriptorModel, Map<String, ScaleMapping> scaleMappingMap,
                                                 final VnfInstance vnfInstance,
                                                 final Map<String, Object> valuesYamlMap) {
        LOGGER.info("Calculating custom replica details for Extensions and Instantiation levels.");
        final NodeTemplate nodeTemplate = ReplicaDetailsUtility.getNodeTemplate(descriptorModel);
        ScalingMapUtility.validateScalingMap(scaleMappingMap, nodeTemplate);
        Map<String, Integer> currentReplicaCounts = replicaCountCalculationService.calculate(vnfInstance);

        if (MapUtils.isNotEmpty(valuesYamlMap)) {
            updateNonScalableVduReplicaDetailsAccordingToValuesFile(vnfInstance, currentReplicaCounts, scaleMappingMap, valuesYamlMap);
        }

        return replicaDetailsBuilder.buildReplicaDetailsFromScalingMapping(vnfInstance, scaleMappingMap, currentReplicaCounts);
    }

    private void updateNonScalableVduReplicaDetailsAccordingToValuesFile(VnfInstance vnfInstance,
                                                                         Map<String, Integer> resourcesWithReplicaCount,
                                                                         Map<String, ScaleMapping> scaleMapping,
                                                                         Map<String, Object> valuesYamlMap) {
        Set<String> scalableVdus = replicaDetailsMapper.getScalableVdusNames(vnfInstance);

        scaleMapping.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && !scalableVdus.contains(entry.getKey()))
                .forEach(entry -> updateReplicaCountIfExistForNonScalableVdus(resourcesWithReplicaCount, entry, valuesYamlMap));
    }

    private void updateReplicaCountIfExistForNonScalableVdus(Map<String, Integer> resourcesWithReplicaCount,
                                                             Map.Entry<String, ScaleMapping> scaleMappingEntry,
                                                             Map<String, Object> valuesYamlMap) {

        final String target = scaleMappingEntry.getKey();
        final ScaleMapping targetScaleMapping = scaleMappingEntry.getValue();

        final Boolean autoScalingEnabled = getPropertyFromValues(targetScaleMapping.getAutoScalingEnabled(),
                                                                 Boolean.class,
                                                                 valuesYamlMap);
        final Integer autoScalingMaxReplicas = getPropertyFromValues(targetScaleMapping.getAutoScalingMaxReplicasName(),
                                                                     Integer.class,
                                                                     valuesYamlMap);
        final Integer scalingParameter = getPropertyFromValues(targetScaleMapping.getScalingParameterName(),
                                                               Integer.class,
                                                               valuesYamlMap);

        if (BooleanUtils.isTrue(autoScalingEnabled)) {
            if (autoScalingMaxReplicas != null) {
                resourcesWithReplicaCount.replace(target, autoScalingMaxReplicas);
            }
        } else if (scalingParameter != null) {
            resourcesWithReplicaCount.replace(target, scalingParameter);
        }
    }

    private static <T> T getPropertyFromValues(final String propertyName, final Class<T> type, final Map<String, Object> valuesYamlMap) {
        if (StringUtils.isBlank(propertyName)) {
            return null;
        }

        return ValuesFileService.getPropertyFromValuesYamlMap(valuesYamlMap, propertyName, type);
    }
}