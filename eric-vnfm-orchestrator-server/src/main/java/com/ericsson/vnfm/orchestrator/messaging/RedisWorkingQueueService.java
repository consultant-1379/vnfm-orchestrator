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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_INCOMING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_WORKING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MESSAGE_POINTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TRACING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WORKING_QUEUE_TIMEOUTS;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.messaging.routing.MessageRouting;
import com.ericsson.vnfm.orchestrator.model.notification.TracingContextInjectorService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty("redis.listener.enabled")
public class RedisWorkingQueueService implements Runnable {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TracingContextInjectorService tracingContextInjectorService;

    @Autowired
    private MessageRouting messageRouting;

    @Autowired
    private IdempotencyContext idempotencyContext;

    @Value("${recovery.workingQueue.validTimeMinutes}")
    private Integer operationEventValidTimeMinutes;

    private final ExecutorService executorService;

    public RedisWorkingQueueService() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        executorService.submit(this);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public void run() {
        LOGGER.info("Incoming messages handling thread started with working queue {}", LOCAL_WORKING_QUEUE);
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        HashOperations<String, String, String> hashOperation = redisTemplate.opsForHash();

        do {
            try {
                handleMessage(listOperations, valueOperations, hashOperation);
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (RuntimeException e) { // NOSONAR
                Throwable t = e.getCause();
                while (t != null && t.getCause() != null && t.getCause() != t) {
                    t = t.getCause();
                }
                if (t instanceof InterruptedException || Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } while (true);
    }

    private void handleMessage(ListOperations<String, String> listOperations, ValueOperations<String, String> valueOperations,
                               HashOperations<String, String, String> hashOperation) {
        String messageId = listOperations.move(LOCAL_INCOMING_QUEUE, RedisListCommands.Direction.RIGHT,
                LOCAL_WORKING_QUEUE, RedisListCommands.Direction.LEFT, Duration.of(10, ChronoUnit.SECONDS));

        if (messageId != null) {
            Long validUntil = Instant.now().plus(operationEventValidTimeMinutes, ChronoUnit.MINUTES).getEpochSecond();
            hashOperation.put(WORKING_QUEUE_TIMEOUTS, messageId, String.valueOf(validUntil));

            String jsonStr = valueOperations.get(String.format(MESSAGE_POINTER, messageId));
            HashMap<String, String> messageMap;
            try {

                messageMap = mapper.readValue(jsonStr, HashMap.class);
            } catch (JsonProcessingException e) {
                LOGGER.error("Unable to parse message text due to: {}", e.getMessage());
                return;
            }
            Optional.ofNullable(messageMap.get(TRACING))
                    .ifPresent(tracingContextInjectorService::injectTracing);
            Optional<String> typeId = Optional.ofNullable(messageMap.get(TYPE_ID));
            if (checkTypeIdIsPresent(typeId, jsonStr)) {
                String payload = messageMap.get(PAYLOAD);
                routeMessage(payload, typeId.get());
            }

            listOperations.remove(LOCAL_WORKING_QUEUE, 1, messageId);
            hashOperation.delete(WORKING_QUEUE_TIMEOUTS, messageId);
            valueOperations.getAndDelete(String.format(MESSAGE_POINTER, messageId));
            idempotencyContext.clear();
        }
    }

    private boolean checkTypeIdIsPresent(Optional<String> typeIdOptional, String jsonStr) {
        if (typeIdOptional.isPresent()) {
            return true;
        } else {
            LOGGER.info("Message typeId not found. This message {} will be ignored", jsonStr);
            return false;
        }
    }


    private void routeMessage(final String body, final Object typeId) {
        Optional<Class<?>> messageClass = getMessageClass(typeId.toString());
        if (messageClass.isPresent()) {
            try {
                messageRouting.routeToMessageProcessor(Utility.parseJson(body, messageClass.get()));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to deserialize message: {}, message will not be requeued.", body, e);
            }
        } else {
            LOGGER.debug("Message type unknown for {} and will be ignored.", typeId);
        }
    }

    private static Optional<Class<?>> getMessageClass(final String typeName) {
        if (typeName.equals(HelmReleaseLifecycleMessage.class.getName())) {
            return Optional.of(HelmReleaseLifecycleMessage.class);
        } else if (typeName.equals(WorkflowServiceEventMessage.class.getName())) {
            return Optional.of(WorkflowServiceEventMessage.class);
        } else {
            LOGGER.debug("Message type unknown for {} and will be ignored.", typeName);
        }
        return Optional.empty();
    }
}
