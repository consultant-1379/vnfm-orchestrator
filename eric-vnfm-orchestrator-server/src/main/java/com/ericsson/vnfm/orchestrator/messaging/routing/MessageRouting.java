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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MessageRouting {

    private final Map<Class<?>, MessageRoutingStrategy> messageRoutingStrategyCache = new HashMap<>(2);

    @Autowired
    private List<MessageRoutingStrategy<?>> strategies;

    @PostConstruct
    public void initStrategies() {
        for (MessageRoutingStrategy<?> strategy : strategies) {
            messageRoutingStrategyCache.put(strategy.getMessageType(), strategy);
        }
    }

    /**
     * Routes message to the processor resolved by message content
     *
     * @param message message to process
     * @param <M>     message type
     */
    public <M> void routeToMessageProcessor(M message) {
        MessageRoutingStrategy<M> strategy = getRoutingStrategy(message);
        if (Objects.nonNull(strategy)) {
            strategy.routeByMessage(message);
        }
    }

    @SuppressWarnings("unchecked")
    public <M> MessageRoutingStrategy<M> getRoutingStrategy(M message) {
        return messageRoutingStrategyCache.get(message.getClass());
    }
}
