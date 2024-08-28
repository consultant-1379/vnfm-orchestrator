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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_ADDING_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_CREATION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.NOTIFICATION_TYPE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.messaging.MessagingServiceImpl;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeAddingNotification;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierCreationNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfLcmOperationOccurrenceNotification;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;


@SpringBootTest(classes = {
        MessagingService.class
})
@MockBean(classes = {
        MessagingServiceImpl.class
})
public class SendNotificationTaskTest {

    @Autowired
    private MessagingService messagingService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void successAddNodeNotification() {

        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(getVnfInstance());

        Task task = prepareTask();

        TaskProcessor taskProcessor = new SendNotificationTask(Optional.empty(), task, "vnfmHost",
                                                               messagingService, databaseInteractionService);

        taskProcessor.execute();

        verify(messagingService, times(1))
                .sendMessage(any(ENMNodeAddingNotification.class));
    }

    @Test
    public void successDeleteNodeNotification() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setAddedToOss(false);

        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(vnfInstance);

        Task task = prepareTask();

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NOTIFICATION_TYPE, VNF_ENM_NODE_DELETION_NOTIFICATION);

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        TaskProcessor taskProcessor = new SendNotificationTask(Optional.empty(), task, "vnfmHost",
                                                               messagingService, databaseInteractionService);

        taskProcessor.execute();

        verify(messagingService, atLeastOnce()).sendMessage(any(ENMNodeDeletionNotification.class));
    }

    @Test
    public void successCreateVnfIdentifierNotification() {
        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(getVnfInstance());

        Task task = prepareTask();

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NOTIFICATION_TYPE, VNF_IDENTIFIER_CREATION_NOTIFICATION);

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        TaskProcessor taskProcessor = new SendNotificationTask(Optional.empty(), task, "vnfmHost",
                                                               messagingService, databaseInteractionService);

        taskProcessor.execute();

        verify(messagingService, times(1))
                .sendMessage(any(VnfIdentifierCreationNotification.class));
    }


    @Test
    public void successDeleteVnfInstanceNotification() {
        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(getVnfInstance());

        Task task = prepareTask();

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NOTIFICATION_TYPE, VNF_IDENTIFIER_DELETION_NOTIFICATION);

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        TaskProcessor taskProcessor = new SendNotificationTask(Optional.empty(), task, "vnfmHost",
                                                               messagingService, databaseInteractionService);

        taskProcessor.execute();

        verify(messagingService, times(1))
                .sendMessage(any(VnfIdentifierDeletionNotification.class));
    }

    @Test
    public void successLcmOperationNotification() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setOperationOccurrenceId("operation_id");

        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(vnfInstance);

        Task task = prepareTask();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NOTIFICATION_TYPE, VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION);

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        setError("some error", operation);

        when(databaseInteractionService.getLifecycleOperation("operation_id"))
                .thenReturn(operation);

        TaskProcessor taskProcessor = new SendNotificationTask(Optional.empty(), task, "vnfmHost",
                                                               messagingService, databaseInteractionService);

        taskProcessor.execute();

        verify(messagingService, times(1))
                .sendMessage(any(VnfLcmOperationOccurrenceNotification.class));
    }

    private Task prepareTask() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setAddedToOss(false);

        Task task = new Task();
        task.setVnfInstanceId("instance-id");
        task.setTaskName(SEND_NOTIFICATION);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NOTIFICATION_TYPE, VNF_ENM_NODE_ADDING_NOTIFICATION);

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        return task;
    }

    private VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setAddedToOss(false);

        return vnfInstance;
    }
}