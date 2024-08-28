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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.model.TaskName;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

@SpringBootTest(classes = {
        TaskProcessorHelper.class
})
@MockBean(classes = {
        PackageService.class,
        MessagingService.class,
        OssNodeService.class,
        DatabaseInteractionService.class,
        BackupsService.class
})
public class TaskProcessorHelperTest {

    @Autowired
    private TaskProcessorHelper taskProcessorHelper;

    @Test
    public void testGetChainEndingWithSendNotification() {
        List<Task> tasks = new ArrayList<>();

        Task sendNotification = new Task();
        sendNotification.setPriority(3);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        Task updateUsageState = new Task();
        updateUsageState.setPriority(2);
        updateUsageState.setTaskName(TaskName.UPDATE_PACKAGE_STATE);
        tasks.add(updateUsageState);

        Task deleteVnfInstance = new Task();
        deleteVnfInstance.setPriority(1);
        deleteVnfInstance.setTaskName(TaskName.DELETE_VNF_INSTANCE);
        tasks.add(deleteVnfInstance);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(DeleteVnfInstanceTask.class);
        assertThat(chain.nextProcessor.get()).isInstanceOf(UpdateUsageStateTask.class);
        assertThat(chain.nextProcessor.get().nextProcessor.get()).isInstanceOf(SendNotificationTask.class);
    }

    @Test
    public void testGetChainEndingWithDeleteNode() {
        List<Task> tasks = new ArrayList<>();

        Task deleteNode = new Task();
        deleteNode.setPriority(3);
        deleteNode.setTaskName(TaskName.DELETE_NODE);
        tasks.add(deleteNode);

        Task sendNotification = new Task();
        sendNotification.setPriority(2);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        Task deleteBackup = new Task();
        deleteBackup.setPriority(1);
        deleteBackup.setTaskName(TaskName.DELETE_BACKUP);
        tasks.add(deleteBackup);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(DeleteBackupTask.class);
        assertThat(chain.nextProcessor.get()).isInstanceOf(SendNotificationTask.class);
        assertThat(chain.nextProcessor.get().nextProcessor.get()).isInstanceOf(DeleteNodeTask.class);
    }

    @Test
    public void testGetChainEndingWithDeleteVnfInstance() {
        List<Task> tasks = new ArrayList<>();

        Task deleteVnfInstance = new Task();
        deleteVnfInstance.setPriority(3);
        deleteVnfInstance.setTaskName(TaskName.DELETE_VNF_INSTANCE);
        tasks.add(deleteVnfInstance);

        Task sendNotification = new Task();
        sendNotification.setPriority(2);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        Task deleteBackup = new Task();
        deleteBackup.setPriority(1);
        deleteBackup.setTaskName(TaskName.DELETE_BACKUP);
        tasks.add(deleteBackup);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(DeleteBackupTask.class);
        assertThat(chain.nextProcessor.get()).isInstanceOf(SendNotificationTask.class);
        assertThat(chain.nextProcessor.get().nextProcessor.get()).isInstanceOf(DeleteVnfInstanceTask.class);
    }

    @Test
    public void testGetChainEndingWithUpdateUsageState() {
        List<Task> tasks = new ArrayList<>();

        Task updateUsageState = new Task();
        updateUsageState.setPriority(3);
        updateUsageState.setTaskName(TaskName.UPDATE_PACKAGE_STATE);
        tasks.add(updateUsageState);

        Task sendNotification = new Task();
        sendNotification.setPriority(2);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        Task deleteBackup = new Task();
        deleteBackup.setPriority(1);
        deleteBackup.setTaskName(TaskName.DELETE_BACKUP);
        tasks.add(deleteBackup);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(DeleteBackupTask.class);
        assertThat(chain.nextProcessor.get()).isInstanceOf(SendNotificationTask.class);
        assertThat(chain.nextProcessor.get().nextProcessor.get()).isInstanceOf(UpdateUsageStateTask.class);
    }

    @Test
    public void testGetChainEndingWithDeleteBackup() {
        List<Task> tasks = new ArrayList<>();

        Task deleteBackup = new Task();
        deleteBackup.setPriority(3);
        deleteBackup.setTaskName(TaskName.DELETE_BACKUP);
        tasks.add(deleteBackup);

        Task sendNotification = new Task();
        sendNotification.setPriority(2);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        Task updateUsageState = new Task();
        updateUsageState.setPriority(1);
        updateUsageState.setTaskName(TaskName.UPDATE_PACKAGE_STATE);
        tasks.add(updateUsageState);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(UpdateUsageStateTask.class);
        assertThat(chain.nextProcessor.get()).isInstanceOf(SendNotificationTask.class);
        assertThat(chain.nextProcessor.get().nextProcessor.get()).isInstanceOf(DeleteBackupTask.class);
    }

    @Test
    public void testGetChainFromSingleElement() {
        List<Task> tasks = new ArrayList<>();

        Task sendNotification = new Task();
        sendNotification.setPriority(3);
        sendNotification.setTaskName(TaskName.SEND_NOTIFICATION);
        tasks.add(sendNotification);

        final TaskProcessor chain = taskProcessorHelper.getChain(tasks);

        assertThat(chain).isInstanceOf(SendNotificationTask.class);
        assertThat(chain.nextProcessor).isEmpty();
    }
}