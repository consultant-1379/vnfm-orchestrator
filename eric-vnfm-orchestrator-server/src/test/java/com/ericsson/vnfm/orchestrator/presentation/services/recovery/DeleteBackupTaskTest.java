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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.BackupsResponseDto.StatusEnum.COMPLETE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_BACKUP;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

@SpringBootTest(classes = {
        BackupsService.class,
        DatabaseInteractionService.class
})
public class DeleteBackupTaskTest {

    private static final String TEST_VNF_INSTANCE_ID = "instance-id";

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private BackupsService backupsService;

    @Test
    public void testDeleteBackupWithLatestCreationDateSuccess() {
        final BackupsResponseDto backup1 = createBackup("test1", "test1", "2024-03-07T11:58:29.831");
        final BackupsResponseDto backup2 = createBackup("test2", "test2", "2024-03-07T14:52:31.831");

        when(databaseInteractionService.getVnfInstance(TEST_VNF_INSTANCE_ID)).thenReturn(getVnfInstance());
        when(backupsService.getAllBackups(TEST_VNF_INSTANCE_ID)).thenReturn(List.of(backup1, backup2));

        Task task = prepareTask();
        TaskProcessor taskProcessor = new DeleteBackupTask(Optional.empty(), task, backupsService, databaseInteractionService);
        taskProcessor.execute();

        verify(backupsService, times(1)).deleteBackup(TEST_VNF_INSTANCE_ID, "DEFAULT", backup2.getName());
        verify(databaseInteractionService).deleteTask(task);
    }

    @Test
    public void testDeleteBackupWhenNoBackupsAvailable() {
        when(databaseInteractionService.getVnfInstance(TEST_VNF_INSTANCE_ID)).thenReturn(getVnfInstance());
        when(backupsService.getAllBackups(TEST_VNF_INSTANCE_ID)).thenReturn(Collections.emptyList());

        Task task = prepareTask();
        TaskProcessor taskProcessor = new DeleteBackupTask(Optional.empty(), task, backupsService, databaseInteractionService);
        taskProcessor.execute();

        verify(backupsService, times(0)).deleteBackup(any(), any(), any());
        verify(databaseInteractionService).deleteTask(task);
    }

    @Test
    public void testDeleteBackupWhenVnfNotInState() {
        final VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);

        when(databaseInteractionService.getVnfInstance(TEST_VNF_INSTANCE_ID)).thenReturn(vnfInstance);
        when(backupsService.getAllBackups(TEST_VNF_INSTANCE_ID)).thenReturn(Collections.emptyList());

        Task task = prepareTask();
        TaskProcessor taskProcessor = new DeleteBackupTask(Optional.empty(), task, backupsService, databaseInteractionService);
        taskProcessor.execute();

        verify(backupsService, times(0)).deleteBackup(any(), any(), any());
        verify(databaseInteractionService).deleteTask(task);
    }

    private static BackupsResponseDto createBackup(String id, String name, String date) {
        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        return new BackupsResponseDto(id,
                                      name,
                                      Date.from(localDateTime.toInstant(ZoneOffset.UTC)),
                                      COMPLETE,
                                      "DEFAULT");
    }

    private static Task prepareTask() {
        Task task = new Task();
        task.setVnfInstanceId(TEST_VNF_INSTANCE_ID);
        task.setTaskName(DELETE_BACKUP);

        return task;
    }

    private static VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(TEST_VNF_INSTANCE_ID);
        vnfInstance.setInstantiationState(INSTANTIATED);

        return vnfInstance;
    }
}
