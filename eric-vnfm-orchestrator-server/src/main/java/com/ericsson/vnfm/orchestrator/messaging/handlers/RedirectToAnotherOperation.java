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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public abstract class RedirectToAnotherOperation<T extends AbstractLifeCycleOperationProcessor> extends MessageHandler<HelmReleaseLifecycleMessage> {

    private T routeToOperation;
    protected DatabaseInteractionService databaseInteractionService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        if (shouldBeRedirected(operation)) {
            onRedirect(message, operation, vnfInstance);
            message.setOperationType(getOperationType());

            HelmReleaseState state = message.getState();
            LOGGER.info("Routing {} chart processing to {} operation", message.getReleaseName(), getOperationType());
            if (state.toString().equals(COMPLETED.toString())) {
                routeToOperation.completed(message);
            } else {
                routeToOperation.failed(message);
            }
        } else {
            passToSuccessor(getSuccessor(), context);
        }
    }

    protected void onRedirect(final HelmReleaseLifecycleMessage message, // NOSONAR
                              final LifecycleOperation operation,
                              final VnfInstance vnfInstance) {

        databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
    }

    protected abstract boolean shouldBeRedirected(LifecycleOperation operation);

    protected abstract HelmReleaseOperationType getOperationType();
}
