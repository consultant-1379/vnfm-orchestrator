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

import com.ericsson.vnfm.orchestrator.messaging.MessageProcessor;

/**
 * Defines main methods of message routing strategies for specific message type
 */
public interface MessageRoutingStrategy<M> {

    /**
     * Routes MQ message to message processor using only message itself
     *
     * @param message message to route
     */
    void routeByMessage(M message);

    /**
     * Provides class of message this strategy associated with
     */
    Class<M> getMessageType();

    void routeToMessageProcessor(M message, Conditions conditions);

    /**
     * Routes message to message processor provided
     */
    void routeByStatus(MessageProcessor<M> messageProcessor, M message);
}
