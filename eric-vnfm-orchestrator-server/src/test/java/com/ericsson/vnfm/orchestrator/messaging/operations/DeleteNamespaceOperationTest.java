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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.DeleteNodeCheckTask;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import brave.Tracing;


@SpringBootTest(classes = {
        DeleteNamespaceOperation.class,
        DeleteNodeCheckTask.class })
@MockBean({
        Tracing.class,
        ClusterConfigService.class,
        HealOperation.class })
public class DeleteNamespaceOperationTest {

    @Autowired
    private DeleteNamespaceOperation deleteNamespaceOperation;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private MessageUtility messageUtility;

    @MockBean
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Captor
    private ArgumentCaptor<LifecycleOperation> operationCaptor;

    @Test
    public void testDeleteNamespaceOperationSuccess() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-namespace-success-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage firstChart = createCompletedMessage("delete-namespace-success-1");

        deleteNamespaceOperation.completed(firstChart);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                    .containsOnly(LifecycleOperationState.COMPLETED.toString());
        });

        verify(messageUtility).deleteIdentifier(same(updatedOperation.getVnfInstance()), same(updatedOperation));
    }

    @Test
    public void testDeleteNamespaceOperationFailed() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-namespace-failed-1", HelmReleaseState.COMPLETED);
        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-namespace-failed-1",
                                                                       "Unable to delete the namespace not-found with the following ERROR Not Found");

        deleteNamespaceOperation.failed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperation.getError())
                .contains("TERMINATE for delete-namespace-failed-1 failed with " +
                                  "Unable to delete the namespace not-found with the following ERROR Not Found");
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        });

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void testDeleteNamespaceOperationNamespaceNotFound() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-namespace-not-found-1", HelmReleaseState.COMPLETED);
        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-namespace-not-found-1",
                                                                       "Namespace delete-namespace not found in cluster delete-namespace");

        deleteNamespaceOperation.failed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNamespace = operationCaptor.getValue();

        verify(messageUtility).deleteIdentifier(same(updatedOperationAfterDeleteNamespace.getVnfInstance()),
                                                same(updatedOperationAfterDeleteNamespace));

        verifyNoMoreInteractions(messageUtility);
    }

    @Test
    public void operationCompletesAndDeleteNodeIsSuccessful() {
        // terminate chart

        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-namespace-complete-delete-node-success-1", HelmReleaseState.COMPLETED);
        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(false);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-namespace-complete-delete-node-success-1");

        deleteNamespaceOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNamespace = operationCaptor.getValue();
        assertThat(updatedOperationAfterDeleteNamespace.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(updatedOperationAfterDeleteNamespace.getError()).isNull();
        assertThat(updatedOperationAfterDeleteNamespace.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNamespace.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
        });
    }

    @Test
    public void operationCompletesBeforeDeleteNodeSucceeds() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-namespace-complete-before-delete-node-success-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-namespace-complete-before-delete-node-success-1");

        deleteNamespaceOperation.completed(completed);

        // then
        verify(databaseInteractionService, times(0)).persistVnfInstanceAndOperation(any(), any());

        final var taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledThreadPoolExecutor).schedule(taskCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));
        final var deleteNodeCheckTask = taskCaptor.getValue();

        reset(databaseInteractionService);

        // simulate delete node check

        // given
        final LifecycleOperation operationAfterDeleteNode = createOperation(existingInstance);
        operationAfterDeleteNode.setDeleteNodeFinished(true);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operationAfterDeleteNode);

        // when
        deleteNodeCheckTask.run();

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNodeCheck = operationCaptor.getValue();
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                    .containsOnly(LifecycleOperationState.COMPLETED.toString());
        });
    }

    private static VnfInstance createInstance(final List<HelmChart> charts) {
        final var instance = new VnfInstance();
        instance.setHelmCharts(charts);
        instance.setClusterName("cluster-name");
        instance.setNamespace("namespace");
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("instance-id");
        instance.setCrdNamespace("crd-namespace");

        return instance;
    }

    private static HelmChart createChart(final String id, final Integer priority, final String releaseName, final HelmReleaseState state) {
        final var chart = new HelmChart();
        chart.setId(id);
        chart.setPriority(priority);
        chart.setReleaseName(releaseName);
        chart.setState(state.toString());

        return chart;
    }

    private static LifecycleOperation createOperation(final VnfInstance instance) {
        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setVnfInstance(instance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusMinutes(10));
        operation.setOperationOccurrenceId("operation-id");

        return operation;
    }

    private static HelmReleaseLifecycleMessage createCompletedMessage(final String releaseName) {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        message.setState(HelmReleaseState.COMPLETED);
        message.setReleaseName(releaseName);
        message.setLifecycleOperationId("operation-id");

        return message;
    }

    private static HelmReleaseLifecycleMessage createFailedMessage(final String releaseName, final String errorMessage) {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setOperationType(HelmReleaseOperationType.TERMINATE);
        message.setState(HelmReleaseState.FAILED);
        message.setReleaseName(releaseName);
        message.setLifecycleOperationId("operation-id");
        message.setMessage(errorMessage);

        return message;
    }
}
