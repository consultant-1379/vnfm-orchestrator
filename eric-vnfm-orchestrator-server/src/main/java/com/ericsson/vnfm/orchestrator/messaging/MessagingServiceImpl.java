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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ETSI_STREAM_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TRACING;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.notification.NotificationBase;
import com.ericsson.vnfm.orchestrator.model.notification.TracingContextInjectorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessagingServiceImpl implements MessagingService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    @Qualifier("redisRetryTemplate")
    private RetryTemplate retryTemplate;

    @Value("${notifications.enabled}")
    private Boolean isNotificationEnabled;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TracingContextInjectorService tracingContextInjectorService;

    @Override
    @Async
    public <T extends NotificationBase> void sendMessage(T message) {
        if (Boolean.TRUE.equals(isNotificationEnabled)) {
            try {
                String jsonString = mapper.writeValueAsString(message);

                Map<String, String> mappedPayloadWithTraceCtx = new HashMap<>();
                mappedPayloadWithTraceCtx.put(PAYLOAD, jsonString);
                mappedPayloadWithTraceCtx.put(TRACING, getTracingContext());

                StringRecord stringRecord = StreamRecords
                        .string(mappedPayloadWithTraceCtx)
                        .withStreamKey(ETSI_STREAM_KEY);

                retryTemplate.execute(context -> redisTemplate.opsForStream().add(stringRecord));
                LOGGER.debug("String record with id:" + stringRecord.getId() + " is added to redis with content:[" + stringRecord.getValue() + "]");
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to convert object to JSON string, message sending failed", e);
            } catch (RedisConnectionFailureException e) {
                LOGGER.error("Failed to send message due to connection problems", e);
            } catch (Exception e) {
                LOGGER.error("Failed to send message", e);
            }
        }
    }

    private String getTracingContext() {
        return tracingContextInjectorService.createNewTraceContext();
    }
}
