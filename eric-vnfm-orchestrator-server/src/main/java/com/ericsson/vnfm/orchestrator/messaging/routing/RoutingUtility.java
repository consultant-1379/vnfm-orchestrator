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
package com.ericsson.vnfm.orchestrator.messaging.routing;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoutingUtility {

    public static final String ROLLBACK_FAILURE_PATTERN = "ROLLBACK_FAILURE_PATTERN";
    public static final String ROLLBACK_PATTERN = "ROLLBACK_PATTERN";

    public static Conditions getConditions(final HelmReleaseLifecycleMessage message, LifecycleOperation operation) {
        Optional<String> failurePattern = Optional.ofNullable(operation.getFailurePattern());
        Optional<String> rollbackPattern = Optional.ofNullable(operation.getRollbackPattern());
        String operationType = failurePattern
                .map(s -> ROLLBACK_FAILURE_PATTERN)
                .or(() -> rollbackPattern.map(s -> ROLLBACK_PATTERN))
                .orElse(message.getOperationType().toString());
        return new Conditions(operationType, message.getClass());
    }
}
