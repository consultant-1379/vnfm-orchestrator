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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import java.util.Map;

public final class VnfInstanceTestUtils {

    private VnfInstanceTestUtils() {}

    public static String createTestReplicaDetails(Map<String, Integer> testReplicas) {
        final String replicaDetailsPattern = getReplicaDetailsPattern();
        StringBuilder testReplicaDetails = new StringBuilder("{");
        for(Map.Entry<String, Integer> entry : testReplicas.entrySet()) {
            String currentReplicaDetails = String.format(replicaDetailsPattern, entry.getKey(), entry.getKey(), entry.getValue());
            testReplicaDetails.append(currentReplicaDetails);
        }
        testReplicaDetails.setCharAt(testReplicaDetails.length() - 1, '}');
        return testReplicaDetails.toString();
    }

    private static String getReplicaDetailsPattern() {
        return "\"%s\":{\"minReplicasParameterName\":null,\"minReplicasCount\":null,"
                + "\"maxReplicasParameterName\":null,\"maxReplicasCount\":null,"
                + "\"scalingParameterName\":\"%s.replicaCount\","
                + "\"currentReplicaCount\":%o,\"autoScalingEnabledParameterName\":null,"
                + "\"autoScalingEnabledValue\":false},";
    }
}
