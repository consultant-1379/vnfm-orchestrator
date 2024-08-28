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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.VduInstantiationLevels;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReplicaCountCalculationServiceImpl implements ReplicaCountCalculationService {

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Override
    public Map<String, Integer> calculateFromVduInitialDelta(VnfInstance vnfInstance) {
        final Map<String, InitialDelta> initialDeltaMap = Optional.ofNullable(vnfInstance.getPolicies())
                .map(policies -> replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance))
                .map(Policies::getAllInitialDelta)
                .filter(allInitialDelta -> !CollectionUtils.isEmpty(allInitialDelta))
                .orElse(new HashMap<>());

        Map<String, Integer> resourceParameters = new HashMap<>();
        for (Map.Entry<String, InitialDelta> parameterEntry : initialDeltaMap.entrySet()) {
            InitialDelta value = parameterEntry.getValue();
            String[] targets = value.getTargets();
            int initialDelta = value.getProperties().getInitialDelta().getNumberOfInstances();
            for (String target : targets) {
                resourceParameters.put(target, initialDelta);
            }
        }
        return resourceParameters;
    }

    @Override
    public Map<String, Integer> calculate(VnfInstance vnfInstance) {
        return vnfInstance.isRel4() ? calculateFromVduInstantiationLevels(vnfInstance) : calculateFromVduInitialDelta(vnfInstance);
    }

    @Override
    public String getResourceDetails(VnfInstance vnfInstance) {
        Map<String, Integer> resourcesDetails = calculate(vnfInstance);
        if (resourcesDetails.isEmpty()) {
            return StringUtils.EMPTY;
        }
        JSONObject json = new JSONObject(resourcesDetails);
        return json.toString();
    }

    private Map<String, Integer> calculateFromVduInstantiationLevels(VnfInstance vnfInstance) {
        final Set<String> scalableVdusNames = replicaDetailsMapper.getScalableVdusNames(vnfInstance);
        final Map<String, Integer> initialDeltaReplicas = calculateFromVduInitialDelta(vnfInstance);
        final Map<String, Integer> scalableReplicas = initialDeltaReplicas.entrySet().stream()
                .filter(vdu -> scalableVdusNames.contains(vdu.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<String, Integer> nonScalableReplicas = calculateForNonScalableVdus(vnfInstance);

        Map<String, Integer> targetNamesCurrentReplicaCount = new HashMap<>();
        targetNamesCurrentReplicaCount.putAll(scalableReplicas);
        targetNamesCurrentReplicaCount.putAll(nonScalableReplicas);

        return targetNamesCurrentReplicaCount;
    }

    private Map<String, Integer> calculateForNonScalableVdus(final VnfInstance vnfInstance) {
        if (Strings.isNullOrEmpty(vnfInstance.getPolicies())) {
            return Collections.emptyMap();
        }

        final Policies policies = replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance);
        final String targetInstantiationLevel = Optional.ofNullable(vnfInstance.getInstantiationLevel())
                .orElseGet(() -> policies.getAllInstantiationLevels().values().stream()
                        .map(instantiationLevel -> instantiationLevel.getProperties().getDefaultLevel())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(""));
        if (targetInstantiationLevel.isBlank()) {
            return Collections.emptyMap();
        }

        final Map<String, Integer> vdusWithReplicas = policies.getAllVduInstantiationLevels().entrySet()
                .stream()
                .flatMap(vduInstantiationLevel -> Arrays.stream(vduInstantiationLevel.getValue().getTargets())
                        .map(target -> Collections.singletonMap(vduInstantiationLevel.getKey(),
                                                                new VduInstantiationLevels()
                                                                        .setProperties(vduInstantiationLevel.getValue().getProperties())
                                                                        .setType(vduInstantiationLevel.getValue().getType())
                                                                        .setTargets(new String[] {target}))
                                .entrySet().iterator().next()
                        ))
                .collect(Collectors.toMap(entry -> entry.getValue().getTargets()[0],
                    entry -> entry.getValue().getProperties().getInstantiationLevels().entrySet()
                                .stream()
                                .filter(instantiationLevel -> targetInstantiationLevel.equals(instantiationLevel.getKey()))
                                .map(instantiationLevel -> instantiationLevel.getValue().getNumberOfInstances())
                                .findAny().orElse(0)));

        // remove scalable VDU-s from result, since such calculation approach is inapplicable for them
        final Set<String> scalableVdus = replicaDetailsMapper.getScalableVdusNames(vnfInstance);
        scalableVdus.forEach(vdusWithReplicas::remove);

        return vdusWithReplicas;
    }
}
