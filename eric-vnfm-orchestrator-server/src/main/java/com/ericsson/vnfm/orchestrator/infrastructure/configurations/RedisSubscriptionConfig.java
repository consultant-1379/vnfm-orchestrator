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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ErrorHandler;

import java.time.Duration;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.CONSUMER_GROUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_KEY;

@Configuration
public class RedisSubscriptionConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer(
            final RedisConnectionFactory connectionFactory,
            final ErrorHandler errorHandler,
            final StreamListener<String, MapRecord<String, String, String>> streamListener,
            @Value("${redis.consumer.name}") String consumerName) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer
                        .StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(500L))
                        .errorHandler(errorHandler)
                        .executor(createTaskExecutor())
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        listenerContainer.receive(
                Consumer.from(CONSUMER_GROUP_NAME, consumerName),
                StreamOffset.create(WFS_STREAM_KEY, ReadOffset.lastConsumed()),
                streamListener);

        return listenerContainer;
    }

    private static ThreadPoolTaskExecutor createTaskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setThreadNamePrefix("RedisReceiver-");
        taskExecutor.initialize();

        return taskExecutor;
    }
}

