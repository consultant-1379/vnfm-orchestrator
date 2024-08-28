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

import java.time.LocalDateTime;
import java.util.List;

import com.ericsson.vnfm.orchestrator.presentation.services.LifecycleOperationFailingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CheckApplicationTimeout {

    public static final String TIME_OUT_ERROR_MESSAGE = "Lifecycle operation %s failed due to timeout";

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private LifecycleOperationFailingService lifecycleOperationFailingService;

    @Scheduled(initialDelay = 50000, fixedDelay = 4000)
    public void checkForApplicationOut() {
        LOGGER.debug("Starting application timeout scheduler at {}", LocalDateTime.now());
        List<String> operationIds =
                lifecycleOperationRepository.findProgressExpiredOperation(LocalDateTime.now(), PageRequest.of(0, 25));
        if (!operationIds.isEmpty()) {
            for (String operationId : operationIds) {
                LOGGER.debug("Acquiring lock on lifecycle id {}", operationId);
                LifecycleOperation lockedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
                lifecycleOperationFailingService.initFailing(lockedOperation, TIME_OUT_ERROR_MESSAGE);
                LOGGER.debug("Releasing lock on lifecycle id {}", operationId);
            }
        }
        LOGGER.debug("Ending application timeout scheduler at {}", LocalDateTime.now());
    }
}
