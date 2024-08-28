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

import com.ericsson.vnfm.orchestrator.messaging.operations.ChangeVnfPackageOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import org.apache.commons.lang3.StringUtils;

public class RedirectFromInstantiateToCcvp extends RedirectToAnotherOperation<ChangeVnfPackageOperation> {

    public RedirectFromInstantiateToCcvp(final ChangeVnfPackageOperation routeToOperation,
                                       final DatabaseInteractionService databaseInteractionService) {
        super(routeToOperation, databaseInteractionService);
    }

    @Override
    protected boolean shouldBeRedirected(LifecycleOperation operation) {
        return LifecycleOperationType.CHANGE_VNFPKG == operation.getLifecycleOperationType() &&
                LifecycleOperationState.PROCESSING == operation.getOperationState() &&
                StringUtils.isNotBlank(operation.getUpgradePattern());
    }

    @Override
    protected HelmReleaseOperationType getOperationType() {
        return HelmReleaseOperationType.INSTANTIATE;
    }
}
