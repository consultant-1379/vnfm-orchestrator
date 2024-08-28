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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.FAILED_OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.resolveTimeOut;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delete the namespace the instance was in
 * This implementation goes outside the pattern as there are two outgoing flows of execution.
 * namespace is deleted and normal flow continues
 * namespace deletion fails and the handling jumps to persisting the information
 */
@Slf4j
@AllArgsConstructor
public final class DeleteNamespace extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final MessageUtility utility;
    private final DatabaseInteractionService databaseInteractionService;

    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling delete namespace");
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        String releaseName = context.getMessage().getReleaseName();

        if (isNamespaceSetForDeletion(vnfInstance)) {
            if (!deleteNamespace(vnfInstance, operation, releaseName)) {
                String deleteNamespaceError = String.format("Deleting the namespace %s has failed", vnfInstance.getNamespace());
                utility.updateOperationOnFail(operation.getOperationOccurrenceId(),
                                              deleteNamespaceError,
                                              String.format(FAILED_OPERATION,
                                                            operation.getOperationOccurrenceId(),
                                                            deleteNamespaceError), InstantiationState.NOT_INSTANTIATED,
                                              LifecycleOperationState.FAILED);
            }
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            LOGGER.info("Skipping deleting namespace as namespace is not set for deletion");
            passToSuccessor(getSuccessor(), context);
        }
    }

    private boolean deleteNamespace(final VnfInstance vnfInstance, final LifecycleOperation operation, final String releaseName) {
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, operation.getApplicationTimeout());
        return utility.triggerDeleteNamespace(operation, vnfInstance.getNamespace(), vnfInstance.getClusterName(), releaseName,
                                              resolveTimeOut(operation));
    }

    private boolean isNamespaceSetForDeletion(VnfInstance vnfInstance) {
        return !vnfInstance.getNamespace().equals(vnfInstance.getCrdNamespace()) &&
                databaseInteractionService.isNamespaceSetForDeletion(vnfInstance.getVnfInstanceId());
    }
}
