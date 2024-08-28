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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MAX_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MIN_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.REPLICA_PARAMETER_NAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReplicaDetailsBuilder {

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Autowired
    private ScaleService scaleService;

    public Map<String, ReplicaDetails> buildDefaultReplicaDetails(final VnfInstance vnfInstance, final Map<String, Integer> currentReplicaCounts) {
        Map<String, String> targetAndReplicas = getTargetsFromInitialDeltas(vnfInstance);
        if (MapUtils.isNotEmpty(targetAndReplicas)) {
            return currentReplicaCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, targetAndReplicaEntry ->
                            buildReplicaDetails(vnfInstance, targetAndReplicas, targetAndReplicaEntry.getKey(), targetAndReplicaEntry.getValue())));
        }
        return new HashMap<>();
    }

    public Map<String, ReplicaDetails> buildReplicaDetailsFromScalingMapping(final VnfInstance vnfInstance,
                                                                             final Map<String, ScaleMapping> scalingMappingMap,
                                                                             final Map<String, Integer> currentReplicaCounts) {
        return scalingMappingMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, scaleMapping ->
                        buildReplicaDetails(vnfInstance, scaleMapping.getKey(), scaleMapping.getValue(), currentReplicaCounts)));
    }

    private String getScalingParameterName(final ScaleMapping scaleMapping) {
        return Optional.ofNullable(scaleMapping.getScalingParameterName())
                .filter(StringUtils::isNotEmpty)
                .orElseThrow(() -> new InvalidInputException("Scaling-Parameter-Name has to be present"));
    }

    private boolean isAutoScalingEnabled(final VnfInstance vnfInstance, final String targetName) {
        if (StringUtils.isEmpty(vnfInstance.getVnfInfoModifiableAttributesExtensions())) {
            return vnfInstance.getManoControlledScaling() == null || !vnfInstance.getManoControlledScaling();
        }
        Map<String, String> vnfControlledScalingValues =
                extensionsMapper.getVnfControlledScalingValues(vnfInstance.getVnfInfoModifiableAttributesExtensions());
        if (MapUtils.isEmpty(vnfControlledScalingValues)) {
            return vnfInstance.getManoControlledScaling() == null || !vnfInstance.getManoControlledScaling();
        }
        Optional<String> aspectFromScalingAspectDelta = getAspectFromScalingAspectDelta(vnfInstance, targetName);

        String aspectValue = null;
        if (aspectFromScalingAspectDelta.isPresent()) {
            aspectValue = vnfControlledScalingValues.get(aspectFromScalingAspectDelta.get());
        }

        return StringUtils.isNotEmpty(aspectValue) && aspectValue.equals(CISM_CONTROLLED);
    }

    private Optional<String> getAspectFromScalingAspectDelta(final VnfInstance vnfInstance, final String targetName) {
        return replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance)
                .getAllScalingAspectDelta().values().stream()
                .filter(scalingAspectDeltas -> Arrays.asList(scalingAspectDeltas.getTargets()).contains(targetName))
                .map(scalingAspectDeltas -> scalingAspectDeltas.getProperties().getAspect())
                .findFirst();
    }

    private ReplicaDetails buildReplicaDetails(VnfInstance vnfInstance, Map<String, String> targetAndReplicas, String targetName,
                                               Integer replicaCount) {
        return ReplicaDetails.builder()
                .withMinReplicasParameterName(String.format(MIN_REPLICA_PARAMETER_NAME, targetAndReplicas.get(targetName)))
                .withMinReplicasCount(isAutoScalingEnabled(vnfInstance, targetName) ? null : replicaCount)
                .withMaxReplicasParameterName(String.format(MAX_REPLICA_PARAMETER_NAME, targetAndReplicas.get(targetName)))
                .withMaxReplicasCount(isAutoScalingEnabled(vnfInstance, targetName) ? null : replicaCount)
                .withScalingParameterName(String.format(REPLICA_PARAMETER_NAME, targetAndReplicas.get(targetName)))
                .withCurrentReplicaCount(replicaCount)
                .withAutoScalingEnabledParameterName(null)
                .withAutoScalingEnabledValue(isAutoScalingEnabled(vnfInstance, targetName))
                .build();
    }

    private ReplicaDetails buildReplicaDetails(final VnfInstance vnfInstance, final String targetName,
                                                                             final ScaleMapping scaleMapping,
                                                                             final Map<String, Integer> targetNamesCurrentReplicaCount) {
        var autoScalingEnabled = isAutoScalingEnabled(vnfInstance, targetName);
        var maxReplicasCount = autoScalingEnabled ? targetNamesCurrentReplicaCount.get(targetName) : null;
        var minReplicasCount = autoScalingEnabled ? scaleService
                .getMinReplicasCountFromVduInitialDelta(vnfInstance, targetName) : null;

        return ReplicaDetails.builder()
                .withMinReplicasParameterName(scaleMapping.getAutoScalingMinReplicasName())
                .withMinReplicasCount(minReplicasCount)
                .withMaxReplicasParameterName(scaleMapping.getAutoScalingMaxReplicasName())
                .withMaxReplicasCount(maxReplicasCount)
                .withScalingParameterName(getScalingParameterName(scaleMapping))
                .withCurrentReplicaCount(targetNamesCurrentReplicaCount.get(targetName))
                .withAutoScalingEnabledParameterName(scaleMapping.getAutoScalingEnabled())
                .withAutoScalingEnabledValue(autoScalingEnabled)
                .build();
    }

    private Map<String, String> getTargetsFromInitialDeltas(final VnfInstance vnfInstance) {
        Policies policies = replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance);
        return Optional.ofNullable(policies.getAllInitialDelta())
                .map(initialDelta -> initialDelta.entrySet().stream()
                        .map(this::getTargetsFromInitialDelta)
                        .reduce(new HashMap<>(), Utility::putAll))
                .orElse(new HashMap<>());
    }

    private Map<String, String> getTargetsFromInitialDelta(Map.Entry<String, InitialDelta> delta) {
        return Arrays.stream(delta.getValue().getTargets()).collect(
                Collectors.toMap(Function.identity(), target -> delta.getKey()));
    }
}
