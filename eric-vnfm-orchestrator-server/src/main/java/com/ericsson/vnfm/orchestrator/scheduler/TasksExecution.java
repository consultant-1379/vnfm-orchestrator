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
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.presentation.services.recovery.TaskProcessorHelper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TasksExecution {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private TaskProcessorHelper taskProcessorHelper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void executeTasks() {
        LOGGER.debug("Starting process of tasks execution at {}", LocalDateTime.now());
        List<Task> availableTasks = databaseInteractionService.getAvailableTasksForExecution(LocalDateTime.now());

        if (CollectionUtils.isNotEmpty(availableTasks)) {
            LOGGER.info("Updating 'last update time' field of tasks");
            updateLastUpdateTimeOfTasks(availableTasks);

            Map<String, List<Task>> groupedTasksByVnfInstanceId = groupTasksByVnfInstanceId(availableTasks);

            LOGGER.info("Executing of tasks");
            groupedTasksByVnfInstanceId.values().forEach(t -> taskProcessorHelper.buildChainAndExecute(t));
        }
    }

    private Map<String, List<Task>> groupTasksByVnfInstanceId(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(Task::getVnfInstanceId));
    }

    private void updateLastUpdateTimeOfTasks(List<Task> tasks) {
        tasks.forEach(t -> t.setLastUpdateTime(LocalDateTime.now()));
        databaseInteractionService.saveTasksInExistingTransaction(tasks);
    }
}
