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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.GrantingConstants.OS_CONTAINER_REQUIREMENT_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.GrantingConstants.VB_STORAGE_REQUIREMENT_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.ScaleMappingDeltaCalculationUtils.isContainerDisabledByScaleMapping;
import static com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.ScaleMappingDeltaCalculationUtils.isResourceCompletelyDisabledByScaleMapping;
import static com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.ScaleMappingDeltaCalculationUtils.isResourcePartiallyDisabledByScaleMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduOsContainerDeployableUnit;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinitionType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GrantingResourcesDeltaCalculationImpl implements GrantingResourcesDeltaCalculation {

    @Override
    public List<ResourceDefinition> calculateResources(final NodeTemplate node,
                                                       final Map<String, Integer> resourcesWithReplicasCount,
                                                       final String vnfdId) {
        LOGGER.info("Starting calculate resources for node template");
        final Set<String> enabledResources = resourcesWithReplicasCount.keySet();
        List<ResourceDefinition> result = buildResourceDefinitions(node, vnfdId, enabledResources);
        result = calculateResourceReplicas(result, resourcesWithReplicasCount);
        LOGGER.info("Finished calculate resources for node template: {}", convertResourceNamesToString(result));
        return result;
    }

    @Override
    public List<ResourceDefinition> getCompletelyDisabledResources(final List<ResourceDefinition> resources,
                                                                   final Map<String, ScaleMapping> scaleMapping,
                                                                   final Map<String, Object> valuesYamlMap) {
        LOGGER.info("Calculating resource definitions completely disabled in scaling mapping file");
        Map<String, List<String>> disabledContainers = getDisabledContainers(scaleMapping, valuesYamlMap);

        List<ResourceDefinition> completelyDisabledResources = resources.stream()
                .filter(resource -> Objects.nonNull(disabledContainers.get(resource.getVduId())))
                .filter(resource -> isResourceCompletelyDisabledByScaleMapping(resource, disabledContainers.get(resource.getVduId())))
                .collect(Collectors.toList());

        List<ResourceDefinition> disabledStorages = completelyDisabledResources.stream()
                .map(ResourceDefinition::getVduId)
                .distinct()
                .flatMap(oscduId -> getDisabledOscduStorageResources(oscduId, resources).stream())
                .collect(Collectors.toList());
        completelyDisabledResources.addAll(disabledStorages);

        LOGGER.info("Resources completely disabled by scaling mapping file: {}", completelyDisabledResources);
        return completelyDisabledResources;
    }

    @Override
    public List<ResourceDefinition> getPartiallyDisabledResources(final List<ResourceDefinition> resources,
                                                                  final Map<String, ScaleMapping> scaleMapping,
                                                                  final Map<String, Object> valuesYamlMap) {
        LOGGER.info("Calculating resource definitions partially disabled in scaling mapping file");
        Map<String, List<String>> disabledContainers = getDisabledContainers(scaleMapping, valuesYamlMap);

        List<ResourceDefinition> partiallyDisabledResources = resources.stream()
                .filter(resource -> Objects.nonNull(disabledContainers.get(resource.getVduId())))
                .filter(resource -> isResourcePartiallyDisabledByScaleMapping(resource, disabledContainers.get(resource.getVduId())))
                .map(resource -> resource.setResourceTemplateId(resource.getResourceTemplateId().stream()
                        .filter(id -> !disabledContainers.get(resource.getVduId()).contains(id))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());

        LOGGER.info("Resources partially disabled by scaling mapping file: {}", partiallyDisabledResources);
        return partiallyDisabledResources;
    }

    private List<ResourceDefinition> getDisabledOscduStorageResources(String disabledOscduId,
                                                                      List<ResourceDefinition> requiredResources) {
        return requiredResources.stream()
                .filter(resource -> Objects.equals(resource.getVduId(), disabledOscduId))
                .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                .collect(Collectors.toList());
    }

    private Map<String, List<String>> getDisabledContainers(Map<String, ScaleMapping> scaleMapping, Map<String, Object> valuesYamlMap) {
        return scaleMapping.entrySet().stream()
                .filter(entry -> Optional.ofNullable(entry.getValue())
                        .filter(scm -> MapUtils.isNotEmpty(scm.getContainers()))
                        .isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getContainers().entrySet().stream()
                        .filter(containerEntry -> isContainerDisabledByScaleMapping(containerEntry, valuesYamlMap))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList())))
                .entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<ResourceDefinition> buildResourceDefinitions(NodeTemplate nodeTemplate, String vnfdId, Set<String> enabledResources) {
        if (nodeTemplate == null || CollectionUtils.isEmpty(nodeTemplate.getOsContainerDeployableUnit())) {
            return Collections.emptyList();
        }

        List<ResourceDefinition> result = new ArrayList<>();

        List<VduOsContainerDeployableUnit> enabledVduNodes = nodeTemplate.getOsContainerDeployableUnit().stream()
                .filter(vdu -> enabledResources.contains(vdu.getVduComputeKey()))
                .collect(Collectors.toList());
        for (final VduOsContainerDeployableUnit vduNode : enabledVduNodes) {

            LOGGER.info("Starting to collect containers required for VDU: {}", vduNode.getName());
            Optional<ResourceDefinition> requiredContainers = getRequiredResources(vduNode,
                                                                                   OS_CONTAINER_REQUIREMENT_KEY,
                                                                                   ResourceDefinitionType.OSCONTAINER,
                                                                                   vnfdId);
            requiredContainers.ifPresent(result::add);

            LOGGER.info("Starting to collect virtual storages required for VDU: {}", vduNode.getName());
            Optional<ResourceDefinition> requiredVirtualStorages = getRequiredResources(vduNode,
                                                                                        VB_STORAGE_REQUIREMENT_KEY,
                                                                                        ResourceDefinitionType.STORAGE,
                                                                                        vnfdId);
            requiredVirtualStorages.ifPresent(result::add);
        }

        return result;
    }

    private static List<ResourceDefinition> calculateResourceReplicas(List<ResourceDefinition> resourceDefinitions,
                                                               final Map<String, Integer> resourceWithReplicasCount) {
        var definitions = new ArrayList<ResourceDefinition>();
        resourceDefinitions.forEach(
            resourceDefinition -> {
                final String osContainerAndStorageVduId = resourceDefinition.getVduId();
                // ResourceDefinitionType.STORAGE should be added as it is to ResourceDefinition
                boolean onlyOneReplica = Optional.ofNullable(osContainerAndStorageVduId)
                        .map(resourceWithReplicasCount::get)
                        .filter(count -> count != 1)
                        .isEmpty();
                if (ResourceDefinitionType.STORAGE == resourceDefinition.getType()
                        || onlyOneReplica) {
                    definitions.add(resourceDefinition);
                } else {
                    // create instances ResourceDefinition for amount of replicas
                    IntStream.range(0, resourceWithReplicasCount.get(osContainerAndStorageVduId))
                            .mapToObj(i -> ResourceDefinition.of(resourceDefinition))
                            .forEach(definitions::add);
                }
            }
        );
        return definitions;
    }

    private static Optional<ResourceDefinition> getRequiredResources(VduOsContainerDeployableUnit vdu,
                                                                     String requirementKey,
                                                                     ResourceDefinitionType type,
                                                                     String vnfdId) {
        Map<String, List<String>> requirements = vdu.getRequirements();
        List<String> requiredResources = requirements.get(requirementKey);
        if (CollectionUtils.isEmpty(requiredResources)) {
            return Optional.empty();
        }
        LOGGER.info("Building resource definition for {} : {} required for VDU: {}",
                    requirementKey, requiredResources, vdu.getName());
        ResourceDefinition resourceDefinition = new ResourceDefinition()
                .setId(UUID.randomUUID().toString())
                .setVduId(vdu.getVduComputeKey())
                .setResourceTemplateId(requiredResources)
                .setVnfdId(vnfdId)
                .setType(type);
        return Optional.ofNullable(resourceDefinition);
    }

    private static String convertResourceNamesToString(List<ResourceDefinition> resourceDefinitions) {
        return resourceDefinitions.stream()
                .map(ResourceDefinition::getResourceTemplateId)
                .flatMap(Collection::stream)
                .collect(Collectors.joining(", "));
    }
}
