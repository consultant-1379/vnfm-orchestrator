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
package com.ericsson.vnfm.orchestrator.presentation.services.recovery;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

@Component
public class TaskProcessorHelper {

    @Value("${vnfm.host}")
    private String vnfmHost;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private BackupsService backupsService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Async("compensatingTaskAsyncExecutor")
    public void buildChainAndExecute(List<Task> tasks) {
        final TaskProcessor chain = getChain(tasks);
        chain.execute();
    }

    protected TaskProcessor getChain(List<Task> tasks) {
        List<Task> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(Task::getPriority).reversed())
                .collect(Collectors.toList());

        TaskProcessor taskProcessor = getChainEnd(sortedTasks.get(0));

        for (int i = 1; i < sortedTasks.size(); i++) {
            switch (sortedTasks.get(i).getTaskName()) {
                case SEND_NOTIFICATION: // NOSONAR
                    taskProcessor = new SendNotificationTask(Optional.ofNullable(taskProcessor),
                                                             sortedTasks.get(i),
                                                             vnfmHost,
                                                             messagingService,
                                                             databaseInteractionService);
                    break;
                case DELETE_NODE:
                    taskProcessor = new DeleteNodeTask(Optional.ofNullable(taskProcessor),
                                                       sortedTasks.get(i),
                                                       ossNodeService,
                                                       databaseInteractionService);
                    break;
                case DELETE_VNF_INSTANCE:
                    taskProcessor = new DeleteVnfInstanceTask(Optional.ofNullable(taskProcessor),
                                                              sortedTasks.get(i),
                                                              databaseInteractionService);
                    break;
                case UPDATE_PACKAGE_STATE:
                    taskProcessor = new UpdateUsageStateTask(Optional.ofNullable(taskProcessor),
                                                             sortedTasks.get(i),
                                                             packageService,
                                                             databaseInteractionService);
                    break;
                case DELETE_BACKUP:
                    taskProcessor = new DeleteBackupTask(Optional.ofNullable(taskProcessor),
                                                         sortedTasks.get(i),
                                                         backupsService,
                                                         databaseInteractionService);
                    break;
                default:
                    break;
            }
        }
        return taskProcessor;
    }

    private TaskProcessor getChainEnd(Task task) {
        return switch (task.getTaskName()) {
            case SEND_NOTIFICATION -> new SendNotificationTask(Optional.empty(),
                                                               task,
                                                               vnfmHost,
                                                               messagingService,
                                                               databaseInteractionService);
            case DELETE_NODE -> new DeleteNodeTask(Optional.empty(),
                                                   task,
                                                   ossNodeService,
                                                   databaseInteractionService);
            case DELETE_VNF_INSTANCE -> new DeleteVnfInstanceTask(Optional.empty(),
                                                                  task,
                                                                  databaseInteractionService);
            case UPDATE_PACKAGE_STATE -> new UpdateUsageStateTask(Optional.empty(),
                                                                  task,
                                                                  packageService,
                                                                  databaseInteractionService);
            case DELETE_BACKUP -> new DeleteBackupTask(Optional.empty(),
                                                       task,
                                                       backupsService,
                                                       databaseInteractionService);
            default -> null;
        };
    }
}
