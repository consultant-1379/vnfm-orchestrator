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
package com.ericsson.vnfm.orchestrator.model.sync;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor (access = AccessLevel.PRIVATE)
public final class ActualScaleValues {
    private static final ActualScaleValues EMPTY = new ActualScaleValues(null, null, null, null);

    private final Integer replicaCount;
    private final Boolean autoScalingEnabled;
    private final Integer minReplicaCount;
    private final Integer maxReplicaCount;

    public static ActualScaleValues create(final Integer replicaCount,
                                           final Boolean autoScalingEnabled,
                                           final Integer minReplicaCount,
                                           final Integer maxReplicaCount) {

        return new ActualScaleValues(replicaCount, autoScalingEnabled, minReplicaCount, maxReplicaCount);
    }

    public static ActualScaleValues empty() {
        return EMPTY;
    }
}
