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

import com.ericsson.vnfm.orchestrator.model.notification.TracingContextInjectorService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.*;


@Slf4j
@Component
public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TracingContextInjectorService tracingContextInjectorService;

    @Value("${idempotency.eventsDedupExpirationSeconds}")
    private Integer eventsDedupExpirationSeconds;

    @Autowired
    private IdempotencyContext idempotencyContext;


    @Override
    public void onMessage(final MapRecord<String, String, String> message) {
        Optional.ofNullable(message.getValue().get(TRACING))
                .ifPresent(tracingContextInjectorService::injectTracing);
        String jsonStr;
        try {
            jsonStr = mapper.writeValueAsString(message.getValue());
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to serialize tracing context due to {}", e.getMessage());
            return;
        }

        if (checkIsNoProcessedBefore(jsonStr)) {
            LOGGER.debug("Sent message into working queue: {} ", message.getValue());

            String messageId = UUID.randomUUID().toString();

            redisTemplate.opsForValue().set(String.format(MESSAGE_POINTER, messageId), jsonStr);
            redisTemplate.opsForList().leftPush(LOCAL_INCOMING_QUEUE, messageId);
            redisTemplate.opsForStream().acknowledge(WFS_STREAM_KEY, CONSUMER_GROUP_NAME, message.getId());
        }
    }

    private boolean checkIsNoProcessedBefore(String jsonString) {
        HashMap<String, String> messageMap;
        try {
            messageMap = mapper.readValue(jsonString, HashMap.class);
            Optional<String> idempotencyKey = Optional.ofNullable(messageMap.get(IDEMPOTENCY_KEY));
            idempotencyKey.ifPresent(idempotencyContext::setIdempotencyId);
            return checkIdempotencyKeyNotProcessedBefore(idempotencyKey, jsonString);
        } catch (JsonProcessingException e) { // NOSONAR
            LOGGER.error("Unable to parse message text due to: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkIdempotencyKeyNotProcessedBefore(Optional<String> idempotencyValueOptional, String jsonStr) {
        if (idempotencyValueOptional.isPresent()) {
            String idempotencyValue = idempotencyValueOptional.get();
            boolean isAdded = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(String.format(MUTEX_WORKFLOW_MESSAGE_DEDUPLICATION, idempotencyValue), "",
                            eventsDedupExpirationSeconds,
                            TimeUnit.SECONDS));
            if (!isAdded) {
                LOGGER.warn("Message with idempotency key {} was processed. This message {} will be ignored", idempotencyValue, jsonStr);
            }
            return isAdded;
        }
        return true;
    }
}
