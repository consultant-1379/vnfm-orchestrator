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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.parsePvcLabels;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;

public class OperationsUtilsTest {

    @Mock
    private DatabaseInteractionService databaseInteractionService;

    @InjectMocks
    private OperationsUtils operationsUtils = new OperationsUtils();

    private static final List<LinkedHashMap<String, String>> listOfMaps = getListOfMaps();
    private static final List<LinkedHashMap<String, String>> listOfMapsSameRelease = getListOfMapsSameRelease();

    @Test
    public void shouldGetFirstElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-1");
        message.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(0);
    }

    @Test
    public void shouldGetSecondElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-2");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(1);
    }

    @Test
    public void shouldGetThirdElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-3");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(2);
    }

    @Test
    public void shouldGetFourthElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-4");
        message.setOperationType(HelmReleaseOperationType.ROLLBACK);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(3);
    }

    @Test
    public void shouldGetFifthElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-5");
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(4);
    }

    @Test
    public void shouldGetSixthElementAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-5");
        message.setOperationType(HelmReleaseOperationType.DELETE_PVC);
        assertThat(OperationsUtils.getCurrentStage(listOfMaps, message)).isEqualTo(5);
    }

    @Test
    public void shouldGetAllElementsAsFailedStageTest() {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName("my-release-1");
        message.setOperationType(HelmReleaseOperationType.ROLLBACK);
        assertThat(OperationsUtils.getCurrentStage(listOfMapsSameRelease, message)).isEqualTo(0);
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMapsSameRelease, message)).isEqualTo(1);
        message.setOperationType(HelmReleaseOperationType.DELETE_PVC);
        assertThat(OperationsUtils.getCurrentStage(listOfMapsSameRelease, message)).isEqualTo(2);
        message.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        assertThat(OperationsUtils.getCurrentStage(listOfMapsSameRelease, message)).isEqualTo(3);
    }

    private static List<LinkedHashMap<String, String>> getListOfMaps() {
        LinkedHashMap<String, String> first = getLinkedHashMap("my-release-1", "install");
        LinkedHashMap<String, String> second = getLinkedHashMap("my-release-2", "delete");
        LinkedHashMap<String, String> third = getLinkedHashMap("my-release-3", "delete_pvc");
        LinkedHashMap<String, String> fourth = getLinkedHashMap("my-release-4", "rollback");
        LinkedHashMap<String, String> fifth = getLinkedHashMap("my-release-5", "delete");
        LinkedHashMap<String, String> sixth = getLinkedHashMap("my-release-5", "delete_pvc[]");
        return Arrays.asList(first, second, third, fourth, fifth, sixth);
    }

    private static List<LinkedHashMap<String, String>> getListOfMapsSameRelease() {
        LinkedHashMap<String, String> first = getLinkedHashMap("my-release-1", "rollback");
        LinkedHashMap<String, String> second = getLinkedHashMap("my-release-1", "delete");
        LinkedHashMap<String, String> third = getLinkedHashMap("my-release-1", "delete_pvc");
        LinkedHashMap<String, String> fourth = getLinkedHashMap("my-release-1", "install");
        return Arrays.asList(first, second, third, fourth);
    }

    private static LinkedHashMap<String, String> getLinkedHashMap(final String releaseName, final String commandType) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(releaseName, commandType);
        return map;
    }

    @Test
    public void shouldParsePvcLabels() {
        LifecycleOperation operation = new LifecycleOperation();
        operation.setFailurePattern(
                "[{\"test-release\":\"rollback\"},"
                        + "{\"test-release\":\"delete\"},"
                        + "{\"test-release\":\"delete_pvc\"}]");
        String[] labels = parsePvcLabels(operation, "test-release");
        assertThat(labels).isEmpty();
        operation.setFailurePattern(
                "[{\"test-release\":\"rollback\"},"
                        + "{\"test-release\":\"delete\"},"
                        + "{\"test-release\":\"delete_pvc[]\"}]");
        labels = parsePvcLabels(operation, "test-release");
        assertThat(labels).isEmpty();

        operation.setFailurePattern(
                "[{\"test-release\":\"rollback\"},"
                        + "{\"test-release\":\"delete\"},"
                        + "{\"test-release\":\"delete_pvc[app=postgres, app.kubernetes.io/instance=test]\"}]");
        labels = parsePvcLabels(operation, "test-release");
        assertThat(labels).isNotEmpty().hasSize(2);
        assertThat(labels).contains("app=postgres", "app.kubernetes.io/instance=test");

        operation.setFailurePattern(
                "[{\"test-release\":\"rollback\"},"
                        + "{\"test-release\":\"delete_pvc[app=postgres]\"},"
                        + "{\"test-release\":\"delete_pvc[release=test]\"}]");
        labels = parsePvcLabels(operation, "test-release");
        assertThat(labels).isNotEmpty().hasSize(2);
        assertThat(labels).contains("app=postgres", "release=test");

        operation.setFailurePattern(
                "[{\"test-release\":\"delete_pvc[app=postgres,release1=test,release2=]\"},"
                        + "{\"test-release\":\"delete_pvc[app=postgres]\"},"
                        + "{\"test-release-another\":\"delete_pvc[release2=test2]\"}]");
        labels = parsePvcLabels(operation, "test-release");
        assertThat(labels).isNotEmpty().hasSize(3);
        assertThat(labels).contains("app=postgres", "release1=test", "release2=");
        assertThat(labels).doesNotContain("release2=test2");
    }

    @Test
    public void shouldValidatePattern() {
        assertThat(OperationsUtils.isValidPattern(listOfMapsSameRelease)).isTrue();
    }

    @Test
    public void shouldNotValidatePattern() {
        assertThat(OperationsUtils.isValidPattern(listOfMaps)).isFalse();
    }

    private LifecycleOperation createOperation(LifecycleOperationType type, LifecycleOperationState state,
                                               LocalDateTime localDateTime, String operationOccurrenceId) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setLifecycleOperationType(type);
        lifecycleOperation.setOperationState(state);
        lifecycleOperation.setOperationOccurrenceId(operationOccurrenceId);
        lifecycleOperation.setStateEnteredTime(localDateTime);
        return lifecycleOperation;
    }
}
