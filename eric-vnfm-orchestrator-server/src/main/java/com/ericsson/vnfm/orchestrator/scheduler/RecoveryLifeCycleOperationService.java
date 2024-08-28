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

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.SyncVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationStage;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.AsyncGrantingAndOrchestrationProcessorImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifeCycleRequestFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.OperationRequestHandler;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.UNABLE_TO_PARSE_JSON_CAUSE_FORMAT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_INCOMING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_WORKING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MUTEX_STAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MUTEX_WORKING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WORKING_QUEUE_TIMEOUTS;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;


@Slf4j
@Component
@ConditionalOnProperty(name = "redis.listener.enabled")
public class RecoveryLifeCycleOperationService {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private LifecycleOperationStageRepository lifecycleOperationStageRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AsyncGrantingAndOrchestrationProcessorImpl asyncGrantingAndOrchestrationProcessor;

    @Autowired
    private LifeCycleRequestFactory lifeCycleRequestFactory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final EnumMap<LifecycleOperationType, Class<?>> REQUEST_CLASS_BY_OPERATION = new EnumMap<>(LifecycleOperationType.class);

    private RecoveryLifeCycleOperationService recoveryService;

    @Value("${recovery.workingQueue.validTimeMinutes}")
    private Integer operationEventValidTimeMinutes;

    @Value("${recovery.operationStage.validTimeMinutes}")
    private Integer operationStageValidTimeMinutes;

    @PostConstruct
    public void init() {
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.INSTANTIATE, InstantiateVnfRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.SCALE, ScaleVnfRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.TERMINATE, TerminateVnfRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.HEAL, HealVnfRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.MODIFY_INFO, VnfInfoModificationRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.CHANGE_VNFPKG, ChangeCurrentVnfPkgRequest.class);
        REQUEST_CLASS_BY_OPERATION.put(LifecycleOperationType.SYNC, SyncVnfRequest.class);

        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.recoveryService = this;
    }

    @Scheduled(initialDelay = 50000, fixedRate = 10000)
    public void checkStageTimeouts() {
        lifecycleOperationStageRepository.findExpiredLifecycleOperationStage(LocalDateTime.now())
                .stream()
                .filter(stage -> Boolean.TRUE.equals(redisTemplate.opsForValue()
                        .setIfAbsent(String.format(MUTEX_STAGE, stage.getOperationId()), "", 3, TimeUnit.SECONDS)))
                .forEach(recoveryService::prepareAndProcessOperationInDB);
    }

    @Scheduled(initialDelay = 50000, fixedRate = 10000)
    public void checkWorkingQueueTimeouts() {
        List<String> expiredOperations = redisTemplate.<String, String>opsForHash().entries(WORKING_QUEUE_TIMEOUTS)
                .entrySet().stream()
                .filter(entry -> Instant.now().isAfter(
                                Instant.ofEpochSecond(Long.parseLong(entry.getValue()))
                        )
                )
                .map(Map.Entry::getKey)
                .toList();

        if (!expiredOperations.isEmpty() && Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(MUTEX_WORKING_QUEUE, "", 2, TimeUnit.SECONDS))) {
            expiredOperations.forEach(this::recoveryOperationInRedis);
            redisTemplate.opsForValue().getAndDelete(MUTEX_WORKING_QUEUE);
        }
    }

    @Scheduled(initialDelay = 50000, fixedRate = 10000)
    public void searchLostMessage() {
        Map<String, String> messagesWithTimeout = redisTemplate.<String, String>opsForHash()
                .entries(WORKING_QUEUE_TIMEOUTS);
        Optional.ofNullable(redisTemplate.<String, String>opsForList().range(LOCAL_WORKING_QUEUE, 0, -1))
                .ifPresent(queue -> queue.stream()
                        .filter(messageId -> !messagesWithTimeout.containsKey(messageId))
                        .forEach(messageId -> {
                            Long validUntil = Instant.now().plus(operationEventValidTimeMinutes, ChronoUnit.MINUTES).getEpochSecond();
                            redisTemplate.<String, String>opsForHash().put(WORKING_QUEUE_TIMEOUTS, messageId, String.valueOf(validUntil));
                        }));
    }

    @Transactional
    public void prepareAndProcessOperationInDB(LifecycleOperationStage stage) {
        LOGGER.info("Recovery lifecycle operation with id: {} ", stage.getOperationId());

        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(stage.getOperationId());

        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        OperationRequestHandler service = lifeCycleRequestFactory.getService(lifecycleOperation.getLifecycleOperationType());
        Object operationParam = prepareObjectRequest(lifecycleOperation);

        Map<String, Object> processedValuesYamlMap = new HashMap<>(convertStringToJSONObj(lifecycleOperation.getValuesFileParams()));
        service.processValuesYaml(processedValuesYamlMap, vnfInstance, operationParam, lifecycleOperation);

        asyncGrantingAndOrchestrationProcessor.process(operationParam, service, vnfInstance, lifecycleOperation, processedValuesYamlMap);

        stage.setValidUntil(LocalDateTime.now().plusMinutes(operationStageValidTimeMinutes));
        lifecycleOperationStageRepository.save(stage);
        redisTemplate.opsForValue().getAndDelete(String.format(MUTEX_STAGE, stage.getOperationId()));
    }

    private static Object prepareObjectRequest(LifecycleOperation operation) {
        String request = operation.getOperationParams();
        try {
            return MAPPER.readValue(request, REQUEST_CLASS_BY_OPERATION.get(operation.getLifecycleOperationType()));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_JSON_CAUSE_FORMAT, request, e.getMessage()), e);
        }
    }

    private void recoveryOperationInRedis(String expiredTimeoutsKey) {
        LOGGER.info("Recovering message with id: {} ", expiredTimeoutsKey);
        redisTemplate.opsForList().remove(LOCAL_WORKING_QUEUE, 1, expiredTimeoutsKey);
        redisTemplate.opsForHash().delete(WORKING_QUEUE_TIMEOUTS, expiredTimeoutsKey);
        redisTemplate.opsForList().leftPush(LOCAL_INCOMING_QUEUE, expiredTimeoutsKey);
    }
}
