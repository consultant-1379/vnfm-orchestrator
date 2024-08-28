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

import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;

/**
 * Basic interface for message processing after message is received from the MB.
 *
 * @param <M> message type
 */
public interface MessageProcessor<M> {

    /**
     * Returns array of objects used as conditions for this processor
     * during processor resolution.
     *
     * @implNote Order-independent, but has to contain only immutable or effectively-immutable objects.
     */
    Conditions getConditions();

    /**
     * Processes message for successfully completed event
     *
     * @param message the message from MB payload
     */
    void completed(M message);

    /**
     * Processes message for failed event
     *
     * @param message the message from MB payload
     */
    void failed(M message);

    /**
     * Defines the only consistent way of context initialization for message provided
     *
     * @param message source of the context content
     * @return context associated with provided message
     */
    MessageHandlingContext<M> initContext(M message);
}
