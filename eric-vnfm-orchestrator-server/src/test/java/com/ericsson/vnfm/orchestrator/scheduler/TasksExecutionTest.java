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

import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareOssNodeRecovery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_VNF_INSTANCE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.notification.OperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.notification.OperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.CREATE_VNF_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.DELETE_VNF_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.FAIL_LCM_OPP;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareRecovery;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.RedisTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.model.TaskName;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeAddingNotification;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.NotificationBase;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierCreationNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfLcmOperationOccurrenceNotification;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.recovery.TaskProcessorHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.RequestNameEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.TimeoutUtils;

@SpringBootTest(classes = {
        TasksExecution.class,
        TaskProcessorHelper.class,
        InstanceService.class,
        IdempotencyContext.class
})
@MockBean(classes = {
        EnmTopologyService.class,
        RestoreBackupFromEnm.class,
        EnrollmentInfoService.class,
        EnrollmentInfoService.class,
        NotificationService.class,
        NfvoConfig.class,
        VnfInstanceQuery.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ReplicaCountCalculationService.class,
        ObjectMapper.class,
        ExtensionsService.class,
        PackageService.class,
        AdditionalAttributesHelper.class,
        VnfInstanceMapper.class,
        LifeCycleManagementHelper.class,
        LcmOpSearchService.class,
        ChangePackageOperationDetailsService.class,
        ChangeVnfPackageService.class,
        EnmMetricsExposers.class,
        BackupsService.class,
        RedisTemplate.class
})
public class TasksExecutionTest {

    @Autowired
    private TasksExecution tasksExecution;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private MessagingService messagingService;

    @MockBean
    private SshHelper sshHelper;

    @SpyBean
    private OssNodeService ossNodeService;

    @Captor
    private ArgumentCaptor<? extends NotificationBase> message;

    @Captor
    private ArgumentCaptor<VnfInstance> instance;

    @Test
    public void executeTasksForCreateIdentifierFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        List<Task> tasks = prepareRecovery(CREATE_VNF_IDENTIFIER, vnfInstance);

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteVnfIdentifierTask verification
        verify(databaseInteractionService).deleteVnfInstance(vnfInstance);
        verify(databaseInteractionService).deleteAllClusterConfigInstancesByInstanceId("createVnfInstanceId");
        verify(databaseInteractionService).deleteInstanceDetailsByVnfInstanceId("createVnfInstanceId");

        // UpdatePackageStateTask verification
        verify(packageService).updateUsageState("packageId", "createVnfInstanceId", false);

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());

        VnfIdentifierCreationNotification notification = (VnfIdentifierCreationNotification) message.getValue();

        assertThat(notification.getOperationState()).isEqualTo(FAILED);
    }

    @Test
    public void executeTasksForDeleteIdentifierFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        List<Task> tasks = prepareRecovery(DELETE_VNF_IDENTIFIER, vnfInstance);

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteVnfIdentifierTask verification
        verify(databaseInteractionService).deleteVnfInstance(vnfInstance);
        verify(databaseInteractionService).deleteAllClusterConfigInstancesByInstanceId("createVnfInstanceId");
        verify(databaseInteractionService).deleteInstanceDetailsByVnfInstanceId("createVnfInstanceId");
        verifyTaskDeletion(tasks, DELETE_VNF_INSTANCE);

        // UpdatePackageStateTask verification
        verify(packageService).updateUsageState("packageId", "createVnfInstanceId", false);
        verifyTaskDeletion(tasks, UPDATE_PACKAGE_STATE);

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());
        verifyTaskDeletion(tasks, SEND_NOTIFICATION);

        NotificationBase notification =  message.getValue();

        assertThat(notification).isInstanceOf(VnfIdentifierDeletionNotification.class);
    }

    @Test
    public void executeTasksForDeleteIdentifierFlowIdentifierAlreadyDeleted() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        List<Task> tasks = prepareRecovery(DELETE_VNF_IDENTIFIER, vnfInstance);

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenThrow(NotFoundException.class);

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteVnfIdentifierTask verification
        verify(databaseInteractionService, times(0)).deleteVnfInstance(any());
        verify(databaseInteractionService).deleteAllClusterConfigInstancesByInstanceId("createVnfInstanceId");
        verify(databaseInteractionService).deleteInstanceDetailsByVnfInstanceId("createVnfInstanceId");
        verifyTaskDeletion(tasks, DELETE_VNF_INSTANCE);

        // UpdatePackageStateTask verification
        verify(packageService).updateUsageState("packageId", "createVnfInstanceId", false);
        verifyTaskDeletion(tasks, UPDATE_PACKAGE_STATE);

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());
        verifyTaskDeletion(tasks, SEND_NOTIFICATION);

        NotificationBase notification =  message.getValue();

        assertThat(notification).isInstanceOf(VnfIdentifierDeletionNotification.class);
    }

    @Test
    public void executeTasksForAddingNodeFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        vnfInstance.setOssTopology(getOssTopologyString());
        vnfInstance.setAddNodeOssTopology(getAddNodeOssTopologyString());
        vnfInstance.setAddedToOss(true);
        List<Task> tasks = prepareOssNodeRecovery(ADD_NODE, vnfInstance, convertOssTopologyStringToMap(getAddNodeOssTopologyString()),
                LocalDateTime.now().plus(40L, ChronoUnit.MILLIS));

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        doReturn(true).when(ossNodeService).checkNodePresent(any());

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteNodeTask verification
        verify(databaseInteractionService).saveVnfInstanceToDB(instance.capture());
        vnfInstance = instance.getValue();
        assertThat(vnfInstance.isAddedToOss()).isFalse();

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());

        ENMNodeAddingNotification notification = (ENMNodeAddingNotification) message.getValue();

        assertThat(notification.getOperationState()).isEqualTo(FAILED);
    }

    @Test
    public void executeTasksForDeleteNodeSuccessFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        vnfInstance.setOssTopology(getOssTopologyString());
        vnfInstance.setAddNodeOssTopology(getAddNodeOssTopologyString());
        vnfInstance.setAddedToOss(true);
        List<Task> tasks = prepareOssNodeRecovery(RequestNameEnum.DELETE_NODE, vnfInstance, convertOssTopologyStringToMap(getAddNodeOssTopologyString()),
                LocalDateTime.now().plus(40L, ChronoUnit.MILLIS));

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        doReturn(true).when(ossNodeService).checkNodePresent(any());

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteNodeTask verification
        verify(databaseInteractionService).saveVnfInstanceToDB(instance.capture());
        vnfInstance = instance.getValue();
        assertThat(vnfInstance.isAddedToOss()).isFalse();
        verifyTaskDeletion(tasks, TaskName.DELETE_NODE);

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());
        verifyTaskDeletion(tasks, SEND_NOTIFICATION);

        ENMNodeDeletionNotification notification = (ENMNodeDeletionNotification) message.getValue();

        assertThat(notification.getOperationState()).isEqualTo(COMPLETED);
    }

    @Test
    public void executeTasksForDeleteNodeFlowNodeAlreadyDeleted() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        vnfInstance.setOssTopology(getOssTopologyString());
        vnfInstance.setAddNodeOssTopology(getAddNodeOssTopologyString());
        vnfInstance.setAddedToOss(false);
        List<Task> tasks = prepareOssNodeRecovery(RequestNameEnum.DELETE_NODE, vnfInstance, convertOssTopologyStringToMap(getAddNodeOssTopologyString()),
                LocalDateTime.now().plus(40L, ChronoUnit.MILLIS));;

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        doReturn(false).when(ossNodeService).checkNodePresent(any());

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteNodeTask verification
        verifyTaskDeletion(tasks, TaskName.DELETE_NODE);

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());
        verifyTaskDeletion(tasks, SEND_NOTIFICATION);

        ENMNodeDeletionNotification notification = (ENMNodeDeletionNotification) message.getValue();

        assertThat(notification.getOperationState()).isEqualTo(COMPLETED);
    }

    @Test
    public void executeTasksForDeleteNodeFailedFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        vnfInstance.setOssTopology(getOssTopologyString());
        vnfInstance.setAddNodeOssTopology(getAddNodeOssTopologyString());
        vnfInstance.setAddedToOss(true);
        List<Task> tasks = prepareOssNodeRecovery(RequestNameEnum.DELETE_NODE, vnfInstance, convertOssTopologyStringToMap(getAddNodeOssTopologyString()),
                LocalDateTime.now().plus(40L, ChronoUnit.MILLIS));

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(1));
        doReturn(true).when(ossNodeService).checkNodePresent(any());

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // DeleteNodeTask verification
        verify(ossNodeService).deleteNodeFromENM(instance.capture(), eq(false));
        vnfInstance = instance.getValue();
        assertThat(vnfInstance.isAddedToOss()).isTrue();

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());

        ENMNodeDeletionNotification notification = (ENMNodeDeletionNotification) message.getValue();

        assertThat(notification.getOperationState()).isEqualTo(FAILED);
    }

    @Test
    public void executeTasksForFailLifecycleOperationSuccessFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");
        vnfInstance.setTempInstance(convertObjToJsonString(vnfInstance));

        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        operation.setOperationState(LifecycleOperationState.FAILED);

        List<Task> tasks = prepareRecovery(FAIL_LCM_OPP, vnfInstance);

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(databaseInteractionService.getLifecycleOperation(any())).thenReturn(operation);

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());

        VnfLcmOperationOccurrenceNotification notification = (VnfLcmOperationOccurrenceNotification) message.getValue();

        assertThat(notification.getError()).isNull();
        assertThat(notification.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    @Test
    public void executeTasksForFailLifecycleOperationWithUpdateUsageStateSuccessFlow() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setVnfInstanceId("createVnfInstanceId");
        vnfInstance.setVnfPackageId("packageId");

        VnfInstance tempVnfInstance = new VnfInstance();
        tempVnfInstance.setVnfPackageId("packageId2");
        vnfInstance.setTempInstance(convertObjToJsonString(tempVnfInstance));

        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        operation.setOperationState(LifecycleOperationState.FAILED);

        List<Task> tasks = prepareRecovery(FAIL_LCM_OPP, vnfInstance);

        when(databaseInteractionService.getAvailableTasksForExecution(any())).thenReturn(tasks);
        when(databaseInteractionService.getVnfInstance("createVnfInstanceId")).thenReturn(vnfInstance);
        when(databaseInteractionService.getLifecycleOperation(any())).thenReturn(operation);

        tasksExecution.executeTasks();
        awaitLastTaskDeletion(tasks);

        // Tasks update verification
        verify(databaseInteractionService).saveTasksInExistingTransaction(same(tasks));

        // UpdatePackageStateTask verification
        verify(packageService).updateUsageState("packageId2", "createVnfInstanceId", false);


        // SendNotificationTask verification
        verify(messagingService).sendMessage(message.capture());

        VnfLcmOperationOccurrenceNotification notification = (VnfLcmOperationOccurrenceNotification) message.getValue();

        assertThat(notification.getError()).isNull();
        assertThat(notification.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    private void awaitLastTaskDeletion(List<Task> tasks) {
        Task lastTask = tasks.stream()
                .max(Comparator.comparingInt(Task::getPriority))
                .get();
        await().untilAsserted(() -> verify(databaseInteractionService).deleteTask(lastTask));
    }

    private void verifyTaskDeletion(List<Task> tasks, TaskName taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .get();
        await().untilAsserted(() -> verify(databaseInteractionService).deleteTask(task));
    }

    private SshResponse createSshResponse(int exitStatus) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setOutput("{\"delete_node_data\": {\"commandOutput\": {\n"
                                      + "        \"return-value\": \"action-id-ref\"\n"
                                      + "      }}}");
        sshResponse.setExitStatus(exitStatus);
        return  sshResponse;
    }

    private Map<String, Object> convertOssTopologyStringToMap(String ossTopology) {
        return new JSONObject(ossTopology).toMap();
    }

    private String getOssTopologyString() {
        return """
                {
                "managedElementId": "test-node",
                "networkElementType": "Shared-CNF"
               }
               """;
    }

    private String getAddNodeOssTopologyString() {
        return """
                {
                "managedElementId": "test-node",
                "networkElementType": "Shared-CNF",
                "networkElementVersion": "1.0.0",
                "nodeIpAddress": "10.203.101.100",
                "networkElementUsername": "user",
                "networkElementPassword": "passwd"
               }
               """;
    }
}