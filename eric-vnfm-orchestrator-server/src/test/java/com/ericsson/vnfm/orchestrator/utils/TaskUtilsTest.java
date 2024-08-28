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

import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareOssNodeRecovery;
import static java.time.temporal.ChronoUnit.MILLIS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_VNF_INSTANCE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.CREATE_VNF_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.FAIL_LCM_OPP;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareRecovery;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class TaskUtilsTest {

    @Test
    public void createTasksForFailLifecycleOppWithoutUpdatePackageStateTask() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setVnfPackageId("package1");
        vnfInstance.setTempInstance(convertObjToJsonString(vnfInstance));

        List<Task> tasks = prepareRecovery(FAIL_LCM_OPP, vnfInstance);

        final Optional<Task> updateUsageStateTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(UPDATE_PACKAGE_STATE))
                .findFirst();

        final Optional<Task> sendNotificationTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(SEND_NOTIFICATION))
                .findFirst();

        assertThat(tasks.size()).isEqualTo(1);

        assertThat(updateUsageStateTask).isEmpty();
        assertThat(sendNotificationTask).isNotEmpty();
        assertThat(sendNotificationTask.get().getAdditionalParams()).contains("\"notificationType\":\"VnfLcmOperationOccurrenceNotification\"");
        assertThat(sendNotificationTask.get().getVnfInstanceId()).isNotNull();
        assertThat(sendNotificationTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(sendNotificationTask.get().getPriority()).isEqualTo(2);
    }

    @Test
    public void createTasksForFailLifecycleOppWithUpdatePackageStateTask() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setVnfPackageId("package1");
        VnfInstance tempInstance = new VnfInstance();
        tempInstance.setVnfPackageId("package2");
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));

        List<Task> tasks = prepareRecovery(FAIL_LCM_OPP, vnfInstance);

        final Optional<Task> updateUsageStateTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(UPDATE_PACKAGE_STATE))
                .findFirst();

        final Optional<Task> sendNotificationTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(SEND_NOTIFICATION))
                .findFirst();

        assertThat(tasks.size()).isEqualTo(2);

        assertThat(updateUsageStateTask).isNotEmpty();
        assertThat(updateUsageStateTask.get().getAdditionalParams()).contains("\"packageId\":\"package2\"");
        assertThat(updateUsageStateTask.get().getVnfInstanceId()).isNotNull();
        assertThat(updateUsageStateTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(updateUsageStateTask.get().getPriority()).isEqualTo(1);

        assertThat(sendNotificationTask).isNotEmpty();
        assertThat(sendNotificationTask.get().getAdditionalParams()).contains("\"notificationType\":\"VnfLcmOperationOccurrenceNotification\"");
        assertThat(sendNotificationTask.get().getVnfInstanceId()).isNotNull();
        assertThat(sendNotificationTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(sendNotificationTask.get().getPriority()).isEqualTo(2);
    }

    @Test
    public void createTasksForIdentifierOperations() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setVnfPackageId("package1");

        List<Task> tasks = prepareRecovery(CREATE_VNF_IDENTIFIER, vnfInstance);

        final Optional<Task> deleteIdentifierTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(DELETE_VNF_INSTANCE))
                .findFirst();

        final Optional<Task> updateUsageStateTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(UPDATE_PACKAGE_STATE))
                .findFirst();

        final Optional<Task> sendNotificationTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(SEND_NOTIFICATION))
                .findFirst();

        assertThat(tasks.size()).isEqualTo(3);

        assertThat(deleteIdentifierTask).isNotEmpty();
        assertThat(deleteIdentifierTask.get().getVnfInstanceId()).isNotNull();
        assertThat(deleteIdentifierTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(deleteIdentifierTask.get().getPriority()).isEqualTo(1);

        assertThat(updateUsageStateTask).isNotEmpty();
        assertThat(updateUsageStateTask.get().getAdditionalParams()).contains("\"packageId\":\"package1\"");
        assertThat(updateUsageStateTask.get().getVnfInstanceId()).isNotNull();
        assertThat(updateUsageStateTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(updateUsageStateTask.get().getPriority()).isEqualTo(2);

        assertThat(sendNotificationTask).isNotEmpty();
        assertThat(sendNotificationTask.get().getAdditionalParams()).contains("\"notificationType\":\"VnfIdentifierCreationNotification\"");
        assertThat(sendNotificationTask.get().getVnfInstanceId()).isNotNull();
        assertThat(sendNotificationTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(10), within(500, MILLIS));
        assertThat(sendNotificationTask.get().getPriority()).isEqualTo(3);
    }

    @Test
    public void createTasksForNodeOperations() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");

        List<Task> tasks = prepareOssNodeRecovery(ADD_NODE, vnfInstance, Map.of("managedElementId", "test-node"),
                LocalDateTime.now().plusSeconds(40L));

        final Optional<Task> deleteNodeTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(DELETE_NODE))
                .findFirst();

        final Optional<Task> sendNotificationTask = tasks.stream()
                .filter(x -> x.getTaskName().equals(SEND_NOTIFICATION))
                .findFirst();

        assertThat(tasks.size()).isEqualTo(2);

        assertThat(deleteNodeTask).isNotEmpty();
        assertThat(deleteNodeTask.get().getVnfInstanceId()).isNotNull();
        assertThat(deleteNodeTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(40), within(600, MILLIS));
        assertThat(deleteNodeTask.get().getPriority()).isEqualTo(1);

        assertThat(sendNotificationTask).isNotEmpty();
        assertThat(sendNotificationTask.get().getAdditionalParams()).contains("\"notificationType\":\"VnfEnmNodeAddingNotification\"");
        assertThat(sendNotificationTask.get().getVnfInstanceId()).isNotNull();
        assertThat(sendNotificationTask.get().getPerformAtTime()).isCloseTo(LocalDateTime.now().plusSeconds(40), within(500, MILLIS));
        assertThat(sendNotificationTask.get().getPriority()).isEqualTo(2);
    }
}
