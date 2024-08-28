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

import org.json.JSONObject;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;

public interface GrantingResourceDefinitionCalculation {

    List<ResourceDefinition> calculateRel4ResourcesInUse(JSONObject vnfd, VnfInstance vnfInstance,
                                                         Map<String, Object> valuesYamlMap, String vnfdId);

    List<ResourceDefinition> calculate(JSONObject vnfd,
                                       Map<String, Integer> newResourcesWithReplicasCount,
                                       String vnfdId);

    List<ResourceDefinition> calculateForScalingMappingFile(JSONObject vnfd,
                                                            Map<String, Integer> currentResourcesWithReplicasCount,
                                                            Map<String, ScaleMapping> scaleMapping,
                                                            Map<String, Object> valuesYamlMap,
                                                            String vnfdId);
}
