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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteBackupTask extends TaskProcessor {

    private final Task task;
    private final BackupsService backupsService;
    private final DatabaseInteractionService databaseInteractionService;

    protected DeleteBackupTask(final Optional<TaskProcessor> nextProcessor, Task task,
                               BackupsService backupsService,
                               DatabaseInteractionService databaseInteractionService) {
        super(nextProcessor);
        this.task = task;
        this.backupsService = backupsService;
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    public void execute() {
        String vnfInstanceId = task.getVnfInstanceId();
        LOGGER.info(VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME, "Delete Backup", vnfInstanceId);

        try {
            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
            checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);

            List<BackupsResponseDto> allBackups = backupsService.getAllBackups(vnfInstanceId);
            if (!allBackups.isEmpty()) {
                BackupsResponseDto backupToDelete = Collections.max(allBackups, Comparator.comparing(BackupsResponseDto::getCreationTime));
                backupsService.deleteBackup(vnfInstanceId, backupToDelete.getScope(), backupToDelete.getName());
            }
            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Delete Backup", vnfInstanceId);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Delete Backup Task", e);
        } finally {
            databaseInteractionService.deleteTask(task);
            nextProcessor.ifPresent(TaskProcessor::execute);
        }
    }
}
