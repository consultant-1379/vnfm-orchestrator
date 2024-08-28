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
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.messaging.MessageProcessor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageOperationFactory {

    private final Map<Conditions, MessageProcessor<?>> messageProcessorCache = new HashMap<>();

    @Autowired
    private List<MessageProcessor<?>> messageProcessors;

    @PostConstruct
    public void initServiceCache() {
        for (MessageProcessor<?> processor : messageProcessors) {
            messageProcessorCache.put(processor.getConditions(), processor);
        }
    }

    public MessageProcessor<?> getProcessor(Conditions conditions) {
        MessageProcessor<?> messageProcessor = messageProcessorCache.get(conditions);
        if (messageProcessor == null) {
            LOGGER.info("Can not resolve message processor for conditions : {}, message will be ignored", conditions);
        }
        return messageProcessor;
    }
}
