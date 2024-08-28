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

import com.ericsson.vnfm.orchestrator.presentation.exceptions.RedisListenerExceptionEvent;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.CONSUMER_GROUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_KEY;

@Slf4j
@Component
@ConditionalOnProperty(name = "redis.listener.enabled")
public class MessageListenerContainerRestarter implements Lifecycle {

    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;

    @Autowired
    private StreamListener<String, MapRecord<String, String, String>> streamListener;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${redis.consumer.name}")
    private String consumerName;

    private boolean isRunning;

    @EventListener
    public void restartStreamListenerOnError(RedisListenerExceptionEvent exceptionEvent) {
        if (!isRunning) {
            return;
        }
        LOGGER.info("Exception occurred in stream listener: {}, restarting listener", exceptionEvent.getSource());

        listenerContainer.stop();
        listenerContainer.receive(
                Consumer.from(CONSUMER_GROUP_NAME, consumerName),
                StreamOffset.create(WFS_STREAM_KEY, ReadOffset.lastConsumed()),
                streamListener);

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

        listenerContainer.start();
        LOGGER.info("Listener restarted");
    }

    @Override
    public void start() {
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
