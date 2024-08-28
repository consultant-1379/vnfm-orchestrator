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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.IDEMPOTENCY_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_KEY;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TestingMessageSender {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    public <T> void sendMessage(final T message) {
        try {
            Map<String, String> messageBody = prepareMessage(message);

            StringRecord record = StreamRecords
                    .string(messageBody)
                    .withStreamKey(WFS_STREAM_KEY);

            redisTemplate.opsForStream().add(record);
            LOGGER.info("Message sent, payload: {}", messageBody.get(PAYLOAD));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert object to JSON string, message sending failed: ", e);
        } catch (RedisConnectionFailureException e) {
            LOGGER.error("Failed to send message due to connection problems: ", e);
        } catch (Exception e) {
            LOGGER.error("Failed to send message due to: ", e);
        }
    }

    public <T> void sendMessageWithIdempotencyKey(final T message, String idempotencyKey) {
        try {
            Map<String, String> messageBody = prepareMessage(message, idempotencyKey);

            StringRecord record = StreamRecords
                    .string(messageBody)
                    .withStreamKey(WFS_STREAM_KEY);

            redisTemplate.opsForStream().add(record);
            LOGGER.info("Message sent, payload: {}", messageBody.get(PAYLOAD));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert object to JSON string, message sending failed: ", e);
        } catch (RedisConnectionFailureException e) {
            LOGGER.error("Failed to send message due to connection problems: ", e);
        } catch (Exception e) {
            LOGGER.error("Failed to send message due to: ", e);
        }
    }

    private <T> Map<String, String> prepareMessage(final T message) throws JsonProcessingException {
        HashMap<String, String> messageBody = new HashMap<>();
        String payload = mapper.writeValueAsString(message);
        messageBody.put(PAYLOAD, payload);
        messageBody.put(TYPE_ID, message.getClass().getName());
        return messageBody;
    }

    private <T> Map<String, String> prepareMessage(final T message, String idempotencyKey) throws JsonProcessingException {
        Map<String, String> messageBody = prepareMessage(message);
        messageBody.put(IDEMPOTENCY_KEY, idempotencyKey);
        return messageBody;
    }

}
