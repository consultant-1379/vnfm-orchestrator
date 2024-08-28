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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.lcm;

import static com.ericsson.am.shared.vnfd.VnfdUtility.isRel4Vnfd;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeType;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourcesDeltaCalculationImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GrantingResourceDefinitionCalculationImpl implements GrantingResourceDefinitionCalculation {

    @Autowired
    protected ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private GrantingResourcesDeltaCalculationImpl deltaCalculation;

    @Autowired
    private MappingFileService mappingFileService;

    @Override
    public List<ResourceDefinition> calculateRel4ResourcesInUse(final JSONObject vnfd, final VnfInstance vnfInstance,
                                                                final Map<String, Object> valuesYamlMap, final String vnfdId) {
        return calculateRel4ResourcesInUse(vnfd, vnfInstance, vnfInstance.getHelmCharts(), valuesYamlMap, vnfdId);
    }

    private List<ResourceDefinition> calculateRel4ResourcesInUse(final JSONObject vnfd, final VnfInstance vnfInstance,
                                                                 final List<HelmChart> helmCharts, final Map<String, Object> valuesYamlMap,
                                                                 final String vnfdId) {
        if (isRel4Vnfd(vnfd)) {
            Optional<String> scalingMappingFilePath = mappingFileService
                    .getScaleMapFilePathFromDescriptorModel(vnfd.toString());
            final Map<String, Integer> resourcesWithReplicas = replicaDetailsMapper.getReplicaCountFromHelmCharts(helmCharts);
            if (scalingMappingFilePath.isPresent() && MapUtils.isNotEmpty(valuesYamlMap)) {
                Map<String, ScaleMapping> scalingMap =
                        mappingFileService.getMappingFile(scalingMappingFilePath.get(), vnfInstance);

                return calculateForScalingMappingFile(vnfd, resourcesWithReplicas, scalingMap, valuesYamlMap, vnfdId);
            } else {
                return calculate(vnfd, resourcesWithReplicas, vnfdId);
            }
        } else {
            LOGGER.debug("Skip building resource definitions since VNFD is not rel4");
            return Collections.emptyList();
        }
    }

    @Override
    public List<ResourceDefinition> calculate(final JSONObject currentVnfd,
                                              final Map<String, Integer> currentResourcesWithReplicasCount,
                                              final String vnfdId) {
        LOGGER.info("Building resource definitions for rel4 VNFD");
        NodeType currentNodeType = ReplicaDetailsUtility.getNodeType(currentVnfd.toString());
        NodeTemplate currentNodeTemplate = NodeTemplateUtility.createNodeTemplate(currentNodeType, currentVnfd);
        return deltaCalculation.calculateResources(currentNodeTemplate, currentResourcesWithReplicasCount, vnfdId);
    }

    @Override
    public List<ResourceDefinition> calculateForScalingMappingFile(final JSONObject currentVnfd,
                                                                   final Map<String, Integer> currentResourcesWithReplicasCount,
                                                                   final Map<String, ScaleMapping> scaleMapping,
                                                                   final Map<String, Object> valuesYamlMap,
                                                                   final String vnfdId) {
        LOGGER.info("Building resource definitions for rel4 scaling mapping file");
        NodeType currentNodeType = ReplicaDetailsUtility.getNodeType(currentVnfd.toString());
        NodeTemplate currentNodeTemplate = NodeTemplateUtility.createNodeTemplate(currentNodeType, currentVnfd);
        List<ResourceDefinition> requiredResources =
                deltaCalculation.calculateResources(currentNodeTemplate, currentResourcesWithReplicasCount, vnfdId);

        List<ResourceDefinition> completelyDisabledResources =
                deltaCalculation.getCompletelyDisabledResources(requiredResources, scaleMapping, valuesYamlMap);
        List<ResourceDefinition> partiallyDisabledResources =
                deltaCalculation.getPartiallyDisabledResources(requiredResources, scaleMapping, valuesYamlMap);

        partiallyDisabledResources
                .forEach(resource -> requiredResources.stream()
                        .filter(requiredRes -> Objects.equals(resource.getId(), requiredRes.getId()))
                        .forEach(requiredRes -> requiredRes.setResourceTemplateId(resource.getResourceTemplateId())));
        requiredResources.removeAll(completelyDisabledResources);
        return requiredResources;
    }
}
