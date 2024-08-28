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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;

public final class TupleToLifecycleOperationMapper {
    private TupleToLifecycleOperationMapper() {

    }

    public static <T> LifecycleOperation map(Tuple tuple, Join<T, LifecycleOperation> join) {
        return LifecycleOperation.builder()
                .operationOccurrenceId(tuple.get(join.get("operationOccurrenceId")))
                .operationState(tuple.get(join.get("operationState")))
                .stateEnteredTime(tuple.get(join.get("stateEnteredTime")))
                .startTime(tuple.get(join.get("startTime")))
                .grantId(tuple.get(join.get("grantId")))
                .lifecycleOperationType(tuple.get(join.get("lifecycleOperationType")))
                .automaticInvocation(tuple.get(join.get("automaticInvocation")))
                .cancelPending(tuple.get(join.get("cancelPending")))
                .cancelMode(tuple.get(join.get("cancelMode")))
                .error(tuple.get(join.get("error")))
                .vnfSoftwareVersion(tuple.get(join.get("vnfSoftwareVersion")))
                .vnfProductName(tuple.get(join.get("vnfProductName")))
                .combinedAdditionalParams(tuple.get(join.get("combinedAdditionalParams")))
                .resourceDetails(tuple.get(join.get("resourceDetails")))
                .scaleInfoEntities(tuple.get(join.get("scaleInfoEntities")))
                .sourceVnfdId(tuple.get(join.get("sourceVnfdId")))
                .targetVnfdId(tuple.get(join.get("targetVnfdId")))
                .deleteNodeFailed(tuple.get(join.get("deleteNodeFailed")))
                .deleteNodeErrorMessage(tuple.get(join.get("deleteNodeErrorMessage")))
                .deleteNodeFinished(tuple.get(join.get("deleteNodeFinished")))
                .applicationTimeout(tuple.get(join.get("applicationTimeout")))
                .expiredApplicationTime(tuple.get(join.get("expiredApplicationTime")))
                .setAlarmSupervisionErrorMessage(tuple.get(join.get("setAlarmSupervisionErrorMessage")))
                .downsizeAllowed(tuple.get(join.get("downsizeAllowed")))
                .isAutoRollbackAllowed(tuple.get(join.get("isAutoRollbackAllowed")))
                .failurePattern(tuple.get(join.get("failurePattern")))
                .vnfInfoModifiableAttributesExtensions(tuple.get(join.get("vnfInfoModifiableAttributesExtensions")))
                .instantiationLevel(tuple.get(join.get("instantiationLevel")))
                .rollbackPattern(tuple.get(join.get("rollbackPattern")))
                .username(tuple.get(join.get("username")))
                .helmClientVersion(tuple.get(join.get("helmClientVersion")))
                .build();
    }

    public static LifecycleOperation map(Tuple tuple, Root<LifecycleOperation> root) {
        return LifecycleOperation.builder()
                .operationOccurrenceId(tuple.get(root.get("operationOccurrenceId")))
                .operationState(tuple.get(root.get("operationState")))
                .stateEnteredTime(tuple.get(root.get("stateEnteredTime")))
                .startTime(tuple.get(root.get("startTime")))
                .grantId(tuple.get(root.get("grantId")))
                .lifecycleOperationType(tuple.get(root.get("lifecycleOperationType")))
                .automaticInvocation(tuple.get(root.get("automaticInvocation")))
                .cancelPending(tuple.get(root.get("cancelPending")))
                .cancelMode(tuple.get(root.get("cancelMode")))
                .error(tuple.get(root.get("error")))
                .vnfSoftwareVersion(tuple.get(root.get("vnfSoftwareVersion")))
                .vnfProductName(tuple.get(root.get("vnfProductName")))
                .combinedAdditionalParams(tuple.get(root.get("combinedAdditionalParams")))
                .resourceDetails(tuple.get(root.get("resourceDetails")))
                .scaleInfoEntities(tuple.get(root.get("scaleInfoEntities")))
                .sourceVnfdId(tuple.get(root.get("sourceVnfdId")))
                .targetVnfdId(tuple.get(root.get("targetVnfdId")))
                .deleteNodeFailed(tuple.get(root.get("deleteNodeFailed")))
                .deleteNodeErrorMessage(tuple.get(root.get("deleteNodeErrorMessage")))
                .deleteNodeFinished(tuple.get(root.get("deleteNodeFinished")))
                .applicationTimeout(tuple.get(root.get("applicationTimeout")))
                .expiredApplicationTime(tuple.get(root.get("expiredApplicationTime")))
                .setAlarmSupervisionErrorMessage(tuple.get(root.get("setAlarmSupervisionErrorMessage")))
                .downsizeAllowed(tuple.get(root.get("downsizeAllowed")))
                .isAutoRollbackAllowed(tuple.get(root.get("isAutoRollbackAllowed")))
                .failurePattern(tuple.get(root.get("failurePattern")))
                .vnfInfoModifiableAttributesExtensions(tuple.get(root.get("vnfInfoModifiableAttributesExtensions")))
                .instantiationLevel(tuple.get(root.get("instantiationLevel")))
                .rollbackPattern(tuple.get(root.get("rollbackPattern")))
                .username(tuple.get(root.get("username")))
                .helmClientVersion(tuple.get(root.get("helmClientVersion")))
                .build();
    }
}
