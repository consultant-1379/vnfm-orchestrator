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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.ScaleMappingContainerDetails;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinitionType;

public final class ScaleMappingTestUtils {

    public static Map<String, ScaleMapping> getScalingMapping(boolean isValid) {
        Map<String, ScaleMapping> result = new HashMap<>();

        ScaleMapping vnfc1 = isValid ?
                getValidScalingMappingForOscdu("vnfc1") : getInvalidScalingMappingForOscdu("vnfc1");
        ScaleMapping vnfc2 = isValid ?
                getValidScalingMappingForOscdu("vnfc2") : getInvalidScalingMappingForOscdu("vnfc2");
        ScaleMapping vnfc3 = isValid ?
                getValidScalingMappingForOscdu("test_cnf") : getInvalidScalingMappingForOscdu("test_cnf");

        result.put("test-cnf-vnfc1", vnfc1);
        result.put("test-cnf-vnfc2", vnfc2);
        result.put("test-cnf", vnfc3);

        return result;
    }

    public static List<ResourceDefinition> getOscduResourceInstances(List<ResourceDefinition> resources, String oscduId,
                                                       ResourceDefinitionType type) {
        return resources.stream()
                .filter(resource -> Objects.equals(resource.getVduId(), oscduId))
                .filter(resource -> type == resource.getType())
                .collect(Collectors.toList());
    }

    private static ScaleMapping getValidScalingMappingForOscdu(String oscduId) {
        ScaleMapping vnfcScaleMapping = new ScaleMapping();
        vnfcScaleMapping.setScalingParameterName(oscduId + ".replicaCount");
        vnfcScaleMapping.setAutoScalingEnabled(oscduId + ".autoscaling.enabled");
        vnfcScaleMapping.setAutoScalingMaxReplicasName(oscduId + ".maxReplicas");
        vnfcScaleMapping.setMciopName("helm_package1");

        ScaleMappingContainerDetails vnfcContainer = new ScaleMappingContainerDetails();
        vnfcContainer.setDeploymentAllowed(oscduId + ".enabled");

        Map<String, ScaleMappingContainerDetails> vnfcContainersMap = new HashMap<>();
        vnfcContainersMap.put(oscduId + "_container", vnfcContainer);

        vnfcScaleMapping.setContainers(vnfcContainersMap);
        return vnfcScaleMapping;
    }

    private static ScaleMapping getInvalidScalingMappingForOscdu(String oscduId) {
        ScaleMapping vnfcScaleMapping = new ScaleMapping();
        vnfcScaleMapping.setScalingParameterName("dummy");
        vnfcScaleMapping.setAutoScalingEnabled("dummy");
        vnfcScaleMapping.setAutoScalingMaxReplicasName("dummy");

        ScaleMappingContainerDetails vnfcContainer = new ScaleMappingContainerDetails();
        vnfcContainer.setDeploymentAllowed("dummy");

        Map<String, ScaleMappingContainerDetails> vnfcContainersMap = new HashMap<>();
        vnfcContainersMap.put(oscduId + "_container", vnfcContainer);

        vnfcScaleMapping.setContainers(vnfcContainersMap);
        return vnfcScaleMapping;
    }
}
