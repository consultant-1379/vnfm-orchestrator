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
import static org.assertj.core.api.Assertions.tuple;
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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.messaging.DeleteNodeCheckTask;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import brave.Tracing;


@SpringBootTest(classes = {
        DeletePvcOperation.class,
        DeleteNodeCheckTask.class,
})
@MockBean({
        Tracing.class,
        LifeCycleManagementHelper.class,
        ClusterConfigService.class,
        HealOperation.class,
        RollbackOperation.class,
})
public class DeletePvcOperationTest {

    @Autowired
    private DeletePvcOperation deletePvcOperation;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private MessageUtility messageUtility;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @MockBean
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Captor
    private ArgumentCaptor<LifecycleOperation> operationCaptor;

    @Test
    public void deletePvcOperationSuccessForFirstChartWhenAnotherExist() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-delete-pvc-1", HelmReleaseState.COMPLETED, PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-delete-pvc-2", null);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        whenWfsRespondsWithAccepted();

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("multiple-charts-delete-pvc-1");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(PROCESSING);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts())
                    .extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState, HelmChartBaseEntity::getDeletePvcState)
                    .contains(tuple("multiple-charts-delete-pvc-1", COMPLETED.toString(), COMPLETED.toString()),
                              tuple("multiple-charts-delete-pvc-2", PROCESSING.toString(), null));
        });

        verifyNoMoreInteractions(databaseInteractionService);

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationSuccessForCcvpOperation() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-delete-pvc-1", HelmReleaseState.COMPLETED, PROCESSING);
        final VnfInstance existingInstance = createInstance(List.of(firstChart));
        final VnfInstance tempInstance = createTempInstance(List.of(firstChart));
        existingInstance.setTempInstance(convertObjToJsonString(tempInstance));

        final LifecycleOperation existingOperation = createCcvpOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("multiple-charts-delete-pvc-1");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(PROCESSING);
        final VnfInstance vnfInstance = parseJson(updatedOperation.getVnfInstance().getTempInstance(), VnfInstance.class);
        assertThat(vnfInstance).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getTerminatedHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getDeletePvcState)
                    .contains(tuple("multiple-charts-delete-pvc-1", COMPLETED.toString()));
        });

        verify(changeVnfPackageOperation).completed(any());

        verifyNoMoreInteractions(databaseInteractionService);
        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationFailedForCcvpOperation() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-delete-pvc-1", HelmReleaseState.COMPLETED, PROCESSING);
        final VnfInstance existingInstance = createInstance(List.of(firstChart));
        final VnfInstance tempInstance = createTempInstance(List.of(firstChart));
        existingInstance.setTempInstance(convertObjToJsonString(tempInstance));

        final LifecycleOperation existingOperation = createCcvpOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createFailedMessage("multiple-charts-delete-pvc-1",
                                                                          "Failed to delete PVCs");

        deletePvcOperation.failed(completed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(PROCESSING);
        final VnfInstance vnfInstance = parseJson(updatedOperation.getVnfInstance().getTempInstance(), VnfInstance.class);
        assertThat(vnfInstance).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getTerminatedHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getDeletePvcState)
                    .contains(tuple("multiple-charts-delete-pvc-1", FAILED.toString()));
        });

        verify(changeVnfPackageOperation).failed(any());

        verifyNoMoreInteractions(databaseInteractionService);
        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationSuccessForSecondChartLast() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-delete-pvc-1", HelmReleaseState.COMPLETED, COMPLETED);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-delete-pvc-2", HelmReleaseState.COMPLETED, PROCESSING);
        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("multiple-charts-delete-pvc-2");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState, HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(tuple(COMPLETED.toString(), COMPLETED.toString()));
        });

        verify(messageUtility).deleteIdentifier(same(updatedOperation.getVnfInstance()), same(updatedOperation));
    }

    @Test
    public void deletePvcOperationFailedForFirstChartWhenAnotherExist() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-delete-pvc-failed-1", HelmReleaseState.COMPLETED, PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-delete-pvc-failed-2", null);
        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("multiple-charts-delete-pvc-failed-1", "Failed to delete PVCs");

        deletePvcOperation.failed(failed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"TERMINATE for "
                                   + "multiple-charts-delete-pvc-failed-1 failed with Failed to delete PVCs.\",\"instance\":\"about:blank\"}");
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getHelmCharts())
                    .extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState, HelmChartBaseEntity::getDeletePvcState)
                    .contains(tuple("multiple-charts-delete-pvc-failed-1", COMPLETED.toString(), FAILED.toString()));
        });

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationSuccessDeleteNamespace() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "delete-pvc-namespace-1", HelmReleaseState.COMPLETED, COMPLETED);
        HelmChart secondChart = createChart("chart-id-2", 2, "delete-pvc-namespace-2", HelmReleaseState.COMPLETED, PROCESSING);
        final VnfInstance existingInstance = createInstance(List.of(firstChart,secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        when(databaseInteractionService.isNamespaceSetForDeletion(anyString())).thenReturn(true);

        when(messageUtility.triggerDeleteNamespace(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-pvc-namespace-2");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(PROCESSING);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState, HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(tuple(COMPLETED.toString(), COMPLETED.toString()));
        });

        verify(messageUtility).triggerDeleteNamespace(same(updatedOperation),
                                                      eq("namespace"),
                                                      eq("cluster-name"),
                                                      eq("delete-pvc-namespace-2"),
                                                      any());

        verifyNoMoreInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationSuccessAddNodeSuccess() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-success-add-node-1", HelmReleaseState.COMPLETED);
        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);
        when(databaseInteractionService.findHelmChartsByVnfInstance(any(VnfInstance.class))).thenReturn(List.of(chart));

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-pvc-success-add-node-1");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(COMPLETED.toString());
        });

        verify(messageUtility).deleteIdentifier(same(updatedOperation.getVnfInstance()), same(updatedOperation));

        verifyNoMoreInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationSuccessAddNodeFailed() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-failed-add-node-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(true);
        existingOperation.setDeleteNodeErrorMessage("ssh connection issue");
        setError("Adding node to OSS for instantiate-failed-add-node failed with the following reason: " +
                                           "OSS topology parameters not found in request.", existingOperation);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-pvc-failed-add-node-1");

        deletePvcOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("Adding node to OSS");
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(COMPLETED.toString());
        });

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationFailedAddNodeSuccess() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-fails-delete-node-success-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-pvc-fails-delete-node-success-1", "timed out");

        deletePvcOperation.failed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("timed out");
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.isDeleteNodeFailed()).isFalse();
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(FAILED.toString());
        });

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void deletePvcOperationFailedAddNodeFailed() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-failed-add-node-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(true);
        existingOperation.setDeleteNodeErrorMessage("delete node failure");

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-pvc-failed-add-node-1", "timed out");

        deletePvcOperation.completed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError())
                .contains("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                  + "\"detail\":\"Delete Node from ENM failure, please delete the Node from ENM manually: delete node failure. \","
                                  + "\"instance\":\"about:blank\"}");
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(COMPLETED.toString());
        });

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void operationCompletesBeforeDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-complete-before-delete-node-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-pvc-complete-before-delete-node-1");

        deletePvcOperation.completed(completed);

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
        operationAfterDeleteNode.setDeleteNodeFailed(true);
        operationAfterDeleteNode.setDeleteNodeErrorMessage("delete node failure");

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operationAfterDeleteNode);

        // when
        deleteNodeCheckTask.run();

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNodeCheck = operationCaptor.getValue();
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError()).containsOnlyOnce("delete node failure");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(COMPLETED.toString());
        });
    }

    @Test
    public void operationCompletesBeforeDeleteNodeSucceeds() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-complete-before-delete-node-success-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("delete-pvc-complete-before-delete-node-success-1");

        deletePvcOperation.completed(completed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(COMPLETED.toString());
        });
    }

    @Test
    public void operationFailsBeforeDeleteNodeSucceeds() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-fails-before-delete-node-success-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-pvc-fails-before-delete-node-success-1", "timed out");

        deletePvcOperation.failed(failed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError()).containsOnlyOnce("timed out");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(FAILED.toString());
        });
    }

    @Test
    public void operationFailsBeforeDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-fails-before-delete-node-fails-1", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("delete-pvc-fails-before-delete-node-fails-1",
                                                                       "timed out waiting for condition");

        deletePvcOperation.failed(failed);

        // then
        verify(databaseInteractionService, times(0)).persistVnfInstanceAndOperation(any(), any());

        final var firstTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledThreadPoolExecutor).schedule(firstTaskCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));
        final var firstDeleteNodeCheckTask = firstTaskCaptor.getValue();

        reset(databaseInteractionService, scheduledThreadPoolExecutor);

        // simulate first delete node check

        // given
        final LifecycleOperation firstOperationAfterDeleteNode = createOperation(existingInstance);
        firstOperationAfterDeleteNode.setDeleteNodeFinished(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(firstOperationAfterDeleteNode);

        // when
        firstDeleteNodeCheckTask.run();

        // then
        verify(databaseInteractionService, times(0)).persistVnfInstanceAndOperation(any(), any());

        final var secondTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduledThreadPoolExecutor).schedule(secondTaskCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));
        final var secondDeleteNodeCheckTask = secondTaskCaptor.getValue();

        reset(databaseInteractionService);

        // simulate second delete node check

        // given
        final LifecycleOperation secondOperationAfterDeleteNode = createOperation(existingInstance);
        secondOperationAfterDeleteNode.setDeleteNodeFinished(true);
        secondOperationAfterDeleteNode.setDeleteNodeFailed(true);
        secondOperationAfterDeleteNode.setDeleteNodeErrorMessage("delete node failure");

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(secondOperationAfterDeleteNode);

        // when
        secondDeleteNodeCheckTask.run();

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNodeCheck = operationCaptor.getValue();
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError())
                .containsOnlyOnce("timed out waiting for condition")
                .containsOnlyOnce("delete node failure");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getDeletePvcState)
                    .containsOnly(FAILED.toString());
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

    private static VnfInstance createTempInstance(final List<HelmChart> charts) {
        List<TerminatedHelmChart> terminatedHelmCharts = new ArrayList<>();
        for (HelmChart chart : charts) {
            final TerminatedHelmChart terminatedHelmChart = TerminatedHelmChart.builder()
                    .id(chart.getId())
                    .priority(chart.getPriority())
                    .releaseName(chart.getReleaseName())
                    .state(chart.getState())
                    .deletePvcState(chart.getDeletePvcState())
                    .build();
            terminatedHelmCharts.add(terminatedHelmChart);
        }
        final var instance = new VnfInstance();
        instance.setTerminatedHelmCharts(terminatedHelmCharts);
        instance.setClusterName("cluster-name");
        instance.setNamespace("namespace");
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("instance-id");
        instance.setCrdNamespace("crd-namespace");

        return instance;
    }

    private static HelmChart createChart(final String id,
                                         final Integer priority,
                                         final String releaseName,
                                         final HelmReleaseState state,
                                         final LifecycleOperationState deletePvcState) {

        final var chart = createChart(id, priority, releaseName, state);
        chart.setDeletePvcState(deletePvcState.toString());

        return chart;
    }

    private static HelmChart createChart(final String id, final Integer priority, final String releaseName, final HelmReleaseState state) {
        final var chart = new HelmChart();
        chart.setId(id);
        chart.setPriority(priority);
        chart.setReleaseName(releaseName);
        chart.setState(state != null ? state.toString() : null);

        return chart;
    }

    private static LifecycleOperation createOperation(final VnfInstance instance) {
        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);
        operation.setOperationState(PROCESSING);
        operation.setVnfInstance(instance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusMinutes(10));
        operation.setOperationOccurrenceId("operation-id");

        return operation;
    }

    private static LifecycleOperation createCcvpOperation(final VnfInstance instance) {
        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        operation.setOperationState(PROCESSING);
        operation.setVnfInstance(instance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusMinutes(10));
        operation.setOperationOccurrenceId("operation-id");

        return operation;
    }

    private static HelmReleaseLifecycleMessage createCompletedMessage(final String releaseName) {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setOperationType(HelmReleaseOperationType.DELETE_PVC);
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

    private void whenWfsRespondsWithAccepted() {
        final WorkflowRoutingResponse wfsResponse = new WorkflowRoutingResponse();
        wfsResponse.setHttpStatus(HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeTerminateRequest(any(), any(), any())).thenReturn(wfsResponse);
    }
}
