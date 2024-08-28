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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;

import lombok.Getter;

@Getter
class ParameterNameResolver {

    private static final String REPLICA_COUNT_SUFFIX = ".replicaCount";
    private static final String MIN_REPLICAS_SUFFIX = "minReplicas";
    private static final String MAX_REPLICAS_SUFFIX = "maxReplicas";
    private static final String AUTOSCALING_ENABLED_SUFFIX = "autoscaling.enabled";

    private final String scalingParameterName;
    private final String minReplicasParameterName;
    private final String maxReplicasParameterName;
    private final String autoScalingEnabledParameterName;

    ParameterNameResolver(final ReplicaDetails replicaDetails) {
        this.scalingParameterName = replicaDetails.getScalingParameterName();
        final var targetName = targetNameFromScalingParameterName(scalingParameterName);
        this.minReplicasParameterName = paramNameOrDefault(replicaDetails.getMinReplicasParameterName(), targetName, MIN_REPLICAS_SUFFIX);
        this.maxReplicasParameterName = paramNameOrDefault(replicaDetails.getMaxReplicasParameterName(), targetName, MAX_REPLICAS_SUFFIX);
        this.autoScalingEnabledParameterName =
                paramNameOrDefault(replicaDetails.getAutoScalingEnabledParameterName(), targetName, AUTOSCALING_ENABLED_SUFFIX);
    }

    private static String targetNameFromScalingParameterName(String scalingParameterName) {
        return StringUtils.substringBefore(scalingParameterName, REPLICA_COUNT_SUFFIX);
    }

    private static String paramNameOrDefault(String paramName, String targetName, String defaultSuffix) {
        return StringUtils.defaultIfBlank(paramName, defaultParamName(targetName, defaultSuffix));
    }

    private static String defaultParamName(String targetName, String defaultSuffix) {
        return StringUtils.isNotBlank(targetName) ? String.format("%s.%s", targetName, defaultSuffix) : null;
    }
}
