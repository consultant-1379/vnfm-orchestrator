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
package com.ericsson.vnfm.orchestrator.messaging.handlers.terminate;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delete the pvcs of the instance
 * This implementation goes outside the pattern as there are 3 outgoing flows of execution.
 * pvcs deleted flow ends and passes control to the pvc event handler
 * pvc deletion fails and the handling jumps to persisting the information
 * pvc deletion not required so flow continues
 */
@Slf4j
@AllArgsConstructor
public final class DeletePvc extends MessageHandler<HelmReleaseLifecycleMessage> {

    private MessageUtility utility;
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling delete pvc");
        LifecycleOperation operation = context.getOperation();
        boolean isCCVPOperation = operation.getLifecycleOperationType().equals(LifecycleOperationType.CHANGE_VNFPKG);
        VnfInstance vnfInstance = context.getVnfInstance();
        HelmReleaseLifecycleMessage message = context.getMessage();
        if (vnfInstance.isCleanUpResources() || isCCVPOperation) {
            LOGGER.info("Execute delete pvc request for release {} in cluster {}",
                        message.getReleaseName(), vnfInstance.getClusterName());
            final boolean isDeletePvcCompleted = deletePvcs(vnfInstance, operation, message);
            if (!isDeletePvcCompleted && isCCVPOperation) {
                message.setMessage(operation.getError());
                message.setState(HelmReleaseState.FAILED);
                context.setMessage(message);
                passToSuccessor(getSuccessor(), context);
            } else if (!isDeletePvcCompleted) {
                passToSuccessor(getSuccessor(), context);
            } else {
                passToSuccessor(getAlternativeSuccessor(), context);
            }
        } else {
            LOGGER.info("Skipping deleting pvc as cleanUpResources is false");
            passToSuccessor(getSuccessor(), context);
        }
    }

    private boolean deletePvcs(final VnfInstance vnfInstance, final LifecycleOperation operation,
            final HelmReleaseLifecycleMessage message) {
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, operation.getApplicationTimeout());
        return utility.triggerDeletePvcs(operation, vnfInstance, message.getReleaseName());
    }
}
