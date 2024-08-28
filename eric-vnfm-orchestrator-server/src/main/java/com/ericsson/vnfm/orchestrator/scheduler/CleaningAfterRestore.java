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

import com.ericsson.vnfm.orchestrator.presentation.services.LifecycleOperationFailingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import com.ericsson.vnfm.orchestrator.repositories.SettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_INCOMING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_WORKING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MESSAGE_POINTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MUTEX_RESTORE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WORKING_QUEUE_TIMEOUTS;

@Slf4j
@Component
@ConditionalOnProperty(name = "redis.listener.enabled")
public class CleaningAfterRestore {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private LifecycleOperationFailingService lifecycleOperationFailingService;

    @Autowired
    private LifecycleOperationStageRepository lifecycleOperationStageRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final CountDownLatch latch = new CountDownLatch(1);

    private static final String RESTORE_KEY = "restore";
    private static final String RESTORE_EXPECTED_VALUE = "completed";

    public static final String RESTORE_ERROR_MESSAGE = "Lifecycle operation %s failed since the operation was in progress" +
            " after the restore";


    @EventListener(ApplicationReadyEvent.class)
    public void init() throws InterruptedException {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                performOperations();
            } catch (Exception e) {
                LOGGER.error("An error occurred while trying to clean up operations after restore: ", e);
            }
        }, 0, 4, TimeUnit.SECONDS);

        if (!latch.await(20, TimeUnit.SECONDS)) {
            settingsRepository.findById(RESTORE_KEY)
                    .ifPresent(settingsRepository::delete);

            LOGGER.error("Timeout expired for falling operations after restore");
        }

        scheduledExecutor.shutdown();
    }

    private void performOperations() {
        if (isRestoreCompleted()) {
            boolean lock = false;
            try {
                lock = Boolean.TRUE.equals(redisTemplate.opsForValue()
                        .setIfAbsent(MUTEX_RESTORE, "", 4, TimeUnit.SECONDS));

                if (lock && isRestoreCompleted()) {

                    LOGGER.info("Init failing operations after completed restore");
                    clearOperationQueue();

                    lifecycleOperationRepository.findAllInProgress().forEach(operation ->
                            Optional.ofNullable(databaseInteractionService.getLifecycleOperation(operation))
                                    .ifPresent(op -> {
                                        op.setLifecycleOperationStage(null);
                                        databaseInteractionService.persistLifecycleOperation(op);
                                        lifecycleOperationFailingService.initFailing(op, RESTORE_ERROR_MESSAGE);
                                    }));

                    settingsRepository.deleteById(RESTORE_KEY);

                    LOGGER.info("Failing operations was successful");
                    latch.countDown();
                }
            } finally {
                if (lock) redisTemplate.opsForValue().getAndDelete(MUTEX_RESTORE);
            }
        } else {
            latch.countDown();
        }
    }

    private boolean isRestoreCompleted() {
        return settingsRepository.findById(RESTORE_KEY)
                .map(value -> Objects.equals(value.getValue(), RESTORE_EXPECTED_VALUE)).orElse(false);
    }

    private void clearOperationQueue() {
        redisTemplate.delete(LOCAL_WORKING_QUEUE);
        redisTemplate.delete(LOCAL_INCOMING_QUEUE);
        redisTemplate.delete(MESSAGE_POINTER);
        redisTemplate.delete(WORKING_QUEUE_TIMEOUTS);

        LOGGER.info("Cleaning operation queues was successful");
    }

}

