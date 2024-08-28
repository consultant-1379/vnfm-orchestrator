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
package com.ericsson.vnfm.orchestrator.utils;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_BACKUP;
import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_VNF_INSTANCE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_ADDING_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_CREATION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.NOTIFICATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.time.LocalDateTime;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public final class TaskUtils {
    public static final String PACKAGE_ID = "packageId";
    private static final long MAXIMUM_REQUEST_PROCESSING_TIME = 5L;

    private TaskUtils() {

    }

    public static List<Task> prepareRecovery(final RequestNameEnum requestName, VnfInstance vnfInstance) {
        switch (requestName) {
            case CREATE_VNF_IDENTIFIER:
                return createTasksForIdentifierOperations(vnfInstance, VNF_IDENTIFIER_CREATION_NOTIFICATION);
            case DELETE_VNF_IDENTIFIER:
                return createTasksForIdentifierOperations(vnfInstance, VNF_IDENTIFIER_DELETION_NOTIFICATION);
            case FAIL_LCM_OPP:
                return createTasksForFailLifecycleOpp(vnfInstance, VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION);
            case DELETE_BACKUP, CREATE_BACKUP:
                return createTasksForBackupOperations(vnfInstance.getVnfInstanceId());
            default:
                return List.of();
        }
    }

    public static List<Task> prepareOssNodeRecovery(final RequestNameEnum requestName,
                                                    VnfInstance vnfInstance,
                                                    Map<String, Object> ossTopologyAttributes,
                                                    LocalDateTime performAtTime) {
        switch (requestName) {
            case DELETE_NODE:
                return createTasksForNodeOperations(vnfInstance, VNF_ENM_NODE_DELETION_NOTIFICATION, ossTopologyAttributes, performAtTime);
            case ADD_NODE:
                return createTasksForNodeOperations(vnfInstance, VNF_ENM_NODE_ADDING_NOTIFICATION, ossTopologyAttributes, performAtTime);
            default:
                return Collections.emptyList();
        }
    }

    private static LocalDateTime calculatePerformAtTime() {
        return LocalDateTime.now().plusSeconds(2 * MAXIMUM_REQUEST_PROCESSING_TIME);
    }

    private static List<Task> createTasksForFailLifecycleOpp(final VnfInstance vnfInstance, String notificationType) {
        List<Task> tasks = new ArrayList<>();
        Map<String, Object> additionalParams = new HashMap<>();

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        if (!vnfInstance.getVnfPackageId().equals(tempInstance.getVnfPackageId())) {
            Task task = new Task();
            task.setTaskName(UPDATE_PACKAGE_STATE);
            task.setVnfInstanceId(vnfInstance.getVnfInstanceId());
            task.setPerformAtTime(calculatePerformAtTime());
            task.setLastUpdateTime(LocalDateTime.now());
            task.setPriority(1);

            additionalParams.put(PACKAGE_ID, tempInstance.getVnfPackageId());
            task.setAdditionalParams(convertObjToJsonString(additionalParams));

            tasks.add(task);
        }

        Task task = new Task();
        task.setTaskName(SEND_NOTIFICATION);
        task.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        task.setPerformAtTime(calculatePerformAtTime());
        task.setLastUpdateTime(LocalDateTime.now());
        task.setPriority(2);

        additionalParams.put(NOTIFICATION_TYPE, notificationType);
        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(task);

        return tasks;
    }

    private static List<Task> createTasksForIdentifierOperations(final VnfInstance vnfInstance, String notificationType) {
        List<Task> tasks = new ArrayList<>();

        Map<String, Object> additionalParams = new HashMap<>();

        Task deleteVnfInstanceTask = new Task();
        deleteVnfInstanceTask.setTaskName(DELETE_VNF_INSTANCE);
        deleteVnfInstanceTask.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        deleteVnfInstanceTask.setPerformAtTime(calculatePerformAtTime());
        deleteVnfInstanceTask.setLastUpdateTime(LocalDateTime.now());
        deleteVnfInstanceTask.setPriority(1);
        deleteVnfInstanceTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(deleteVnfInstanceTask);

        Task updatePackageStateTask = new Task();
        updatePackageStateTask.setTaskName(UPDATE_PACKAGE_STATE);
        updatePackageStateTask.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        updatePackageStateTask.setPerformAtTime(calculatePerformAtTime());
        updatePackageStateTask.setLastUpdateTime(LocalDateTime.now());
        updatePackageStateTask.setPriority(2);

        additionalParams.put(PACKAGE_ID, vnfInstance.getVnfPackageId());
        updatePackageStateTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(updatePackageStateTask);

        Task sendNotificationTask = new Task();
        sendNotificationTask.setTaskName(SEND_NOTIFICATION);
        sendNotificationTask.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        sendNotificationTask.setPerformAtTime(calculatePerformAtTime());
        sendNotificationTask.setLastUpdateTime(LocalDateTime.now());
        sendNotificationTask.setPriority(3);

        additionalParams.put(NOTIFICATION_TYPE, notificationType);
        sendNotificationTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(sendNotificationTask);

        return tasks;
    }

    private static List<Task> createTasksForNodeOperations(final VnfInstance vnfInstance, String notificationType,
                                                           Map<String, Object> ossTopologyAttributes, LocalDateTime performAtTime) {
        List<Task> tasks = new ArrayList<>();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(MANAGED_ELEMENT_ID, ossTopologyAttributes.get(MANAGED_ELEMENT_ID));

        Task deleteNodeTask = new Task();
        deleteNodeTask.setTaskName(DELETE_NODE);
        deleteNodeTask.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        deleteNodeTask.setPerformAtTime(performAtTime);
        deleteNodeTask.setLastUpdateTime(LocalDateTime.now());
        deleteNodeTask.setPriority(1);
        deleteNodeTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(deleteNodeTask);

        Task sendNotificationTask = new Task();
        sendNotificationTask.setTaskName(SEND_NOTIFICATION);
        sendNotificationTask.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        sendNotificationTask.setPerformAtTime(performAtTime);
        sendNotificationTask.setLastUpdateTime(LocalDateTime.now());
        sendNotificationTask.setPriority(2);

        additionalParams.put(NOTIFICATION_TYPE, notificationType);
        sendNotificationTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(sendNotificationTask);

        return tasks;
    }

    private static List<Task> createTasksForBackupOperations(final String vnfInstanceId) {
        List<Task> tasks = new ArrayList<>();
        Map<String, Object> additionalParams = new HashMap<>();

        Task deleteNodeTask = new Task();
        deleteNodeTask.setTaskName(DELETE_BACKUP);
        deleteNodeTask.setVnfInstanceId(vnfInstanceId);
        deleteNodeTask.setPerformAtTime(LocalDateTime.now().plus(20L, ChronoUnit.MILLIS));
        deleteNodeTask.setLastUpdateTime(LocalDateTime.now());
        deleteNodeTask.setPriority(1);
        deleteNodeTask.setAdditionalParams(convertObjToJsonString(additionalParams));

        tasks.add(deleteNodeTask);

        return tasks;
    }
}
