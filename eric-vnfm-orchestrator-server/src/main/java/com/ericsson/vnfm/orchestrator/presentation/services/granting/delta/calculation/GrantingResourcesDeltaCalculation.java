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

import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;

public interface GrantingResourcesDeltaCalculation {

    List<ResourceDefinition> calculateResources(NodeTemplate node,
                                                Map<String, Integer> resourcesWithReplicasCount,
                                                String targetVnfdId);

    List<ResourceDefinition> getCompletelyDisabledResources(List<ResourceDefinition> resources,
                                                            Map<String, ScaleMapping> scaleMapping,
                                                            Map<String, Object> valuesYamlMap);

    List<ResourceDefinition> getPartiallyDisabledResources(List<ResourceDefinition> resources,
                                                           Map<String, ScaleMapping> scaleMapping,
                                                           Map<String, Object> valuesYamlMap);
}
