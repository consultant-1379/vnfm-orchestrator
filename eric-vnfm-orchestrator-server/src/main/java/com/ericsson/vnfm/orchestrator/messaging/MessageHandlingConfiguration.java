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

import java.util.Objects;
import java.util.Optional;

public class MessageHandlingConfiguration<M> {

    private Optional<MessageHandler<M>> initialStage;

    private MessageHandler<M> lastConfiguredStage;

    /**
     * Setup initial stage of message handling chain.
     */
    public MessageHandlingConfiguration<M> startWith(MessageHandler<M> handler) {
        initialStage = Optional.of(handler);
        lastConfiguredStage = handler;
        return this;
    }

    /**
     * Defines any of non-initial stages of message handling chain.
     * If initial stage was not set - provided handler will be set as initial.
     */
    public MessageHandlingConfiguration<M> andThen(MessageHandler<M> handler) {
        return andThenOrElse(handler, null);
    }

    /**
     * Defines any of non-initial stages of message handling chain with alternative path provided.
     * Allows to create simple is-else forks using alternative successors in handlers.
     *
     * If initial stage was not set - provided handler will be set as initial
     * while alternative path just ignored.
     */
    public MessageHandlingConfiguration<M> andThenOrElse(MessageHandler<M> handler, MessageHandlingConfiguration<M> forkSubflow) {
        if (lastConfiguredStage == null) {
            return this;
        }

        if (initialStage.isPresent()) {
            lastConfiguredStage.setSuccessor(Optional.of(handler));
            Optional<MessageHandler<M>> altSuccessor =
                    Objects.nonNull(forkSubflow) ? forkSubflow.getInitialStage() : Optional.empty();
            lastConfiguredStage.setAlternativeSuccessor(altSuccessor);
            lastConfiguredStage = handler;
            return this;
        } else {
            return startWith(handler);
        }
    }

    /**
     * Allows to pass message handling to another subflow if defined.
     * Effectively makes further configuration impossible, as control will
     * be passed to another flow.
     *
     * Any configuration calls will be ignored after this one.
     */
    public MessageHandlingConfiguration<M> endWithSubflow(MessageHandlingConfiguration<M> subflow) {
        if (subflow != null && initialStage.isPresent() && lastConfiguredStage != null) {
            lastConfiguredStage.setSuccessor(subflow.getInitialStage());
            lastConfiguredStage = null;
        }
        return this;
    }

    /**
     * Marks configuration as completed. Any configuration calls will be ignored after this one.
     */
    public MessageHandlingConfiguration<M> end() {
        if (initialStage.isPresent() && lastConfiguredStage != null) {
            lastConfiguredStage = null;
        }
        return this;
    }

    /**
     * Retrieves initial stage of chain to start message processing.
     * Can be empty Optional if no real configuration was provided
     * before call of this method.
     */
    public Optional<MessageHandler<M>> getInitialStage() {
        return initialStage;
    }
}
