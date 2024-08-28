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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import com.ericsson.am.shared.vnfd.ScalingMapUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.am.shared.vnfd.model.policies.VduScalingAspectDeltasProperties;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScalingAspectsHelper {

    private static final TypeReference<Map<String, ScalingAspectDataType>> SCALING_ASPECTS_TYPE_REF = new TypeReference<>() {
    };

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MappingFileService mappingFileService;

    @Autowired
    private PackageService packageService;

    public void disableScalingAspectsForDisabledVdus(ResourceResponse vnfResource, VnfInstance vnfInstance) {
        if (vnfResource.getScalingInfo() != null) {
            Map<String, ScalingAspectDataType> scalingAspects = mapper
                    .convertValue(vnfResource.getScalingInfo(), SCALING_ASPECTS_TYPE_REF);

            List<String> scalingAspectsKeysWithAllDisabledVdus = getScalingAspectsKeysWithAllDisabledVdus(vnfInstance);
            if (!scalingAspectsKeysWithAllDisabledVdus.isEmpty()) {
                scalingAspects.entrySet().stream()
                        .filter(aspect -> scalingAspectsKeysWithAllDisabledVdus.contains(aspect.getKey()))
                        .forEach(aspect -> aspect.getValue().setEnabled(false));
                vnfResource.setScalingInfo(scalingAspects);
            }
        }
    }

    private List<String> getScalingAspectsKeysWithAllDisabledVdus(VnfInstance vnfInstance) {
        JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());
        Set<String> disabledVduNames = getDisabledVduNames(vnfInstance, vnfd.toString());

        try {
            Policies policies = mapper.readValue(vnfInstance.getPolicies(), Policies.class);
            return getScalingAspectsKeysByTargetVduNames(policies, disabledVduNames);
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException("Couldn't parse policies", e);
        }
    }

    private Set<String> getDisabledVduNames(VnfInstance vnfInstance, String vnfd) {
        NodeTemplate nodeTemplate = ReplicaDetailsUtility.getNodeTemplate(vnfd);
        Set<String> disabledArtifacts = HelmChartUtils.getAllDisabledArtifactsKeys(vnfInstance);
        Optional<String> scalingMappingFilePath = mappingFileService.getScaleMapFilePathFromDescriptorModel(vnfd);

        if (scalingMappingFilePath.isPresent()) {
            Map<String, ScaleMapping> scalingMapping = mappingFileService
                    .getMappingFile(scalingMappingFilePath.get(), vnfInstance);

            return ScalingMapUtility.mapArtifactsToVduNames(scalingMapping).entrySet().stream()
                    .filter(entry -> disabledArtifacts.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());
        } else {
            return VnfdUtility.createMapHelmPackageTargets(nodeTemplate).entrySet().stream()
                    .filter(entry -> disabledArtifacts.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());
        }
    }

    private List<String> getScalingAspectsKeysByTargetVduNames(Policies policies, Set<String> targetVduNames) {
        if (policies.getAllScalingAspectDelta() != null) {
            return policies.getAllScalingAspectDelta().values().stream()
                    .filter(aspectDelta -> Optional.ofNullable(aspectDelta)
                            .map(ScalingAspectDeltas::getTargets)
                            .isPresent())
                    .filter(aspectDelta -> targetVduNames.containsAll(Arrays.asList(aspectDelta.getTargets())))
                    .map(ScalingAspectDeltas::getProperties)
                    .filter(Objects::nonNull)
                    .map(VduScalingAspectDeltasProperties::getAspect)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
