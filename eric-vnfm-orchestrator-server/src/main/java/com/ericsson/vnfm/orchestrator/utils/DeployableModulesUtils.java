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
package com.ericsson.vnfm.orchestrator.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ericsson.am.shared.vnfd.model.nestedvnfd.DeployableModule;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DeployableModulesUtils {

    public static List<String> getDeployableModulesFromArtifact(Map<String, DeployableModule> deployableModules, String helmChartArtifact) {
        return deployableModules.entrySet().stream()
                .filter(dm -> dm.getValue().getAssociatedArtifacts().contains(helmChartArtifact))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static boolean isChartHasStateInAnyDeployableModules(List<String> associatedDeployableModules, Map<String, ?> deployableModule,
                                                                String state) {
        return associatedDeployableModules.stream()
                .map(deployableModule::get)
                .anyMatch(state::equals);
    }
}
