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
package com.ericsson.vnfm.orchestrator.messaging.handlers;

import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedirectFromInstantiateToRollback extends RedirectToAnotherOperation<RollbackOperation> {

    public RedirectFromInstantiateToRollback(final RollbackOperation routeToOperation,
                                             final DatabaseInteractionService databaseInteractionService) {
        super(routeToOperation, databaseInteractionService);
    }

    @Override
    protected boolean shouldBeRedirected(final LifecycleOperation operation) {
        return operation.getLifecycleOperationType().equals(LifecycleOperationType.CHANGE_VNFPKG) &&
                operation.getOperationState().equals(LifecycleOperationState.ROLLING_BACK);
    }

    @Override
    protected HelmReleaseOperationType getOperationType() {
        return HelmReleaseOperationType.ROLLBACK;
    }
}
