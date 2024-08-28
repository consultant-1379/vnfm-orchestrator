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
package com.ericsson.vnfm.orchestrator.scheduler;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.CONSUMER_GROUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_KEY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "redis.listener.enabled")
public class MessageListenerContainerStarter {

    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createConsumerGroupAndStartListenerContainer() {
        try {
            redisTemplate.opsForStream().createGroup(WFS_STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP_NAME);
            LOGGER.info("Created consumer group: {}", CONSUMER_GROUP_NAME);
        } catch (RedisSystemException e) {
            if (Utility.hasRedisBusyException(e)) {
                LOGGER.error("Consumer group {} already exists", CONSUMER_GROUP_NAME, e);
            } else {
                LOGGER.error("Unable to create Consumer group: {}, due to: {}", CONSUMER_GROUP_NAME, e.getMessage(), e);
            }
        }

        LOGGER.info("Starting message listener");
        listenerContainer.start();
        LOGGER.info(("Message listener started"));
    }
}
