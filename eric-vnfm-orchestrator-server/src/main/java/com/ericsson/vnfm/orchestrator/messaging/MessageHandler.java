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
package com.ericsson.vnfm.orchestrator.messaging;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

/**
 * Chain of Responsibility design pattern
 * Extensions of this class can be used to handle
 * {@link com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage} messages.
 * Each implementation contains logic to execute based on the message and then decides whether to call its successive
 * handler
 */
@Getter
@Setter
public abstract class MessageHandler<M> {

    private Optional<MessageHandler<M>> successor = Optional.empty();
    private Optional<MessageHandler<M>> alternativeSuccessor = Optional.empty();

    /**
     * Handle this message
     */
    public abstract void handle(MessageHandlingContext<M> context);

    protected void passToSuccessor(Optional<MessageHandler<M>> actualSuccessor,
                                          MessageHandlingContext<M> context) {
        actualSuccessor.ifPresent(handler -> handler.handle(context));
    }
}
