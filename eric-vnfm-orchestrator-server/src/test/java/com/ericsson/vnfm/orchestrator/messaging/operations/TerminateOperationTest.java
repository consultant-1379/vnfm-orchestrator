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
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.toTerminateHelmChart;

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
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Tracing;

@SpringBootTest(classes = {
        TerminateOperation.class,
        DeleteNodeCheckTask.class
})
@MockBean({
        Tracing.class,
        LifeCycleManagementHelper.class,
        ClusterConfigService.class,
        HealOperation.class,
        RollbackOperation.class
})
public class TerminateOperationTest {

    @Autowired
    private TerminateOperation terminateOperation;

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
    public void testTerminateOperationSuccessForFirstChart() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-first-success-1", HelmReleaseState.PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-first-success-2", null);
        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        whenWfsRespondsWithAccepted();

        // when
        final HelmReleaseLifecycleMessage firstChartCompleted = createCompletedMessage("multiple-charts-first-success-1");

        terminateOperation.completed(firstChartCompleted);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-first-success-1", LifecycleOperationState.COMPLETED.toString()),
                              tuple("multiple-charts-first-success-2", LifecycleOperationState.PROCESSING.toString()));
        });

        verify(workflowRoutingService).routeTerminateRequest(same(existingInstance), same(existingOperation), eq("multiple-charts-first-success-2"));

        verifyNoMoreInteractions(databaseInteractionService, workflowRoutingService);
    }

    @Test
    public void testTerminateOperationSuccessForAllCharts() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-1", HelmReleaseState.PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-2", null);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        whenWfsRespondsWithAccepted();

        // terminate first chart

        // when
        final HelmReleaseLifecycleMessage firstChartCompleted = createCompletedMessage("multiple-charts-1");

        terminateOperation.completed(firstChartCompleted);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterFirst = operationCaptor.getValue();
        assertThat(updatedOperationAfterFirst.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-1", LifecycleOperationState.COMPLETED.toString()),
                              tuple("multiple-charts-2", LifecycleOperationState.PROCESSING.toString()));
        });

        verify(workflowRoutingService).routeTerminateRequest(same(existingInstance), same(existingOperation), eq("multiple-charts-2"));

        verifyNoMoreInteractions(databaseInteractionService, workflowRoutingService);

        reset(databaseInteractionService, workflowRoutingService);

        // terminate second chart

        // given
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(updatedOperationAfterFirst);

        // when
        final HelmReleaseLifecycleMessage secondChartCompleted = createCompletedMessage("multiple-charts-2");

        terminateOperation.completed(secondChartCompleted);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterSecond = operationCaptor.getValue();
        assertThat(updatedOperationAfterSecond.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(updatedOperationAfterSecond.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNull();
            assertThat(updatedInstance.getClusterName()).isNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-1", LifecycleOperationState.COMPLETED.toString()),
                              tuple("multiple-charts-2", LifecycleOperationState.COMPLETED.toString()));
        });

        verifyNoInteractions(workflowRoutingService);

        verify(messageUtility).deleteIdentifier(same(updatedOperationAfterSecond.getVnfInstance()), same(updatedOperationAfterSecond));
    }

    @Test
    public void testTerminateOperationFailedForFirstChart() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-failed-first-1", HelmReleaseState.PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-failed-first-2", null);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage firstChartFailed = createFailedMessage("multiple-charts-failed-first-1", "Helm/kubectl command timedOut");

        terminateOperation.failed(firstChartFailed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterFirst = operationCaptor.getValue();
        assertThat(updatedOperationAfterFirst.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperationAfterFirst.getError())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                   + "\"detail\":\"TERMINATE for multiple-charts-failed-first-1 failed with Helm/kubectl command timedOut.\","
                                   + "\"instance\":\"about:blank\"}");
        assertThat(updatedOperationAfterFirst.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-failed-first-1", LifecycleOperationState.FAILED.toString()),
                              tuple("multiple-charts-failed-first-2", null));
        });

        verifyNoInteractions(workflowRoutingService);
    }

    @Test
    public void testTerminateOperationFailedForSecondChartAfterFirstChartSuccess() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "multiple-charts-failed-second-1", HelmReleaseState.PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "multiple-charts-failed-second-2", null);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        whenWfsRespondsWithAccepted();

        // terminate first chart

        // when
        final HelmReleaseLifecycleMessage firstChartCompleted = createCompletedMessage("multiple-charts-failed-second-1");

        terminateOperation.completed(firstChartCompleted);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterFirst = operationCaptor.getValue();
        assertThat(updatedOperationAfterFirst.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperationAfterFirst.getError()).isNull();
        assertThat(updatedOperationAfterFirst.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-failed-second-1", LifecycleOperationState.COMPLETED.toString()),
                              tuple("multiple-charts-failed-second-2", LifecycleOperationState.PROCESSING.toString()));
        });

        verify(workflowRoutingService).routeTerminateRequest(same(existingInstance), same(existingOperation), eq("multiple-charts-failed-second-2"));

        verifyNoMoreInteractions(databaseInteractionService, workflowRoutingService);

        reset(databaseInteractionService, workflowRoutingService);

        // terminate second chart

        // given
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(updatedOperationAfterFirst);

        // when
        final HelmReleaseLifecycleMessage secondChartFailed = createFailedMessage("multiple-charts-failed-second-2", "Helm/kubectl command timedOut");

        terminateOperation.failed(secondChartFailed);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterSecond = operationCaptor.getValue();
        assertThat(updatedOperationAfterSecond.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperationAfterSecond.getError())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                   + "\"detail\":\"TERMINATE for multiple-charts-failed-second-2 failed with Helm/kubectl command timedOut.\","
                                   + "\"instance\":\"about:blank\"}");
        assertThat(updatedOperationAfterSecond.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("multiple-charts-failed-second-1", LifecycleOperationState.COMPLETED.toString()),
                              tuple("multiple-charts-failed-second-2", LifecycleOperationState.FAILED.toString()));
        });

        verifyNoInteractions(messageUtility, workflowRoutingService);
    }

    @Test
    public void testTerminateCompletedOperationFailedAddNode() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "instantiate-failed-add-node-1", HelmReleaseState.COMPLETED);
        HelmChart secondChart = createChart("chart-id-2", 2, "instantiate-failed-add-node-2", HelmReleaseState.COMPLETED);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final var existingOperation = new LifecycleOperation();
        existingOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        existingOperation.setOperationState(LifecycleOperationState.COMPLETED);
        existingOperation.setVnfInstance(existingInstance);
        setError("Adding node to OSS for instantiate-failed-add-node failed with the following reason: OSS topology parameters not found in request.",
                 existingOperation);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage firstChartCompleted = createCompletedMessage("test-release");

        terminateOperation.completed(firstChartCompleted);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(updatedOperation.getError()).contains("Adding node to OSS");
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationFailedWithTimeOut() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-complete-after-delete-node-succeeds-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setOperationType(HelmReleaseOperationType.TERMINATE);
        firstChart.setLifecycleOperationId("operation-id");
        firstChart.setMessage("Lifecycle operation TERMINATE failed due to timeout");

        terminateOperation.rollBack(firstChart);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));

        verify(messageUtility).lifecycleTimedOut(eq("operation-id"),
                                                 eq(InstantiationState.NOT_INSTANTIATED),
                                                 eq("Lifecycle operation TERMINATE failed due to timeout"));

        verifyNoMoreInteractions(databaseInteractionService);
    }

    @Test
    public void testTerminateOperationDeleteNamespaceSuccess() {
        // given
        HelmChart firstChart = createChart("chart-id-1", 1, "delete-namespace-1", HelmReleaseState.PROCESSING);
        HelmChart secondChart = createChart("chart-id-2", 2, "delete-namespace-2", null);

        final VnfInstance existingInstance = createInstance(List.of(firstChart, secondChart));

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        whenWfsRespondsWithAccepted();

        // terminate first chart

        // when
        final HelmReleaseLifecycleMessage firstChartCompleted = createCompletedMessage("delete-namespace-1");

        terminateOperation.completed(firstChartCompleted);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        reset(databaseInteractionService);

        // terminate second chart

        // given
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operationCaptor.getValue());

        when(databaseInteractionService.isNamespaceSetForDeletion(anyString())).thenReturn(true);

        when(messageUtility.triggerDeleteNamespace(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage secondChartCompleted = createCompletedMessage("delete-namespace-2");

        terminateOperation.completed(secondChartCompleted);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterSecond = operationCaptor.getValue();
        assertThat(updatedOperationAfterSecond.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperationAfterSecond.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                    .containsOnly(LifecycleOperationState.COMPLETED.toString());
        });

        verify(messageUtility).triggerDeleteNamespace(same(updatedOperationAfterSecond),
                                                      eq("namespace"),
                                                      eq("cluster-name"),
                                                      eq("delete-namespace-2"),
                                                      any());

        verifyNoMoreInteractions(messageUtility);
    }

    @Test
    public void testTerminateOperationDeletePvcSuccess() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "delete-pvc-success-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setCleanUpResources(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);
        when(messageUtility.triggerDeletePvcs(any(), any(), anyString())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage firstChart = createCompletedMessage("delete-pvc-success-1");

        terminateOperation.completed(firstChart);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("operation-id"));
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                    .contains(tuple("delete-pvc-success-1", LifecycleOperationState.COMPLETED.toString()));
        });

        verify(messageUtility).triggerDeletePvcs(same(updatedOperation), same(updatedOperation.getVnfInstance()), eq("delete-pvc-success-1"));

        verifyNoMoreInteractions(databaseInteractionService, messageUtility);
    }

    @Test
    public void operationCompletesAndDeleteNodeIsSuccessful() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-complete-delete-node-success-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(false);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        when(databaseInteractionService.isNamespaceSetForDeletion(anyString())).thenReturn(true);

        when(messageUtility.triggerDeleteNamespace(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-complete-delete-node-success-1");

        terminateOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperationAfterDeleteNamespace = operationCaptor.getValue();
        assertThat(updatedOperationAfterDeleteNamespace.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperationAfterDeleteNamespace.getError()).isNull();
        assertThat(updatedOperationAfterDeleteNamespace.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
            assertThat(updatedInstance.getHelmCharts()).extracting(HelmChartBaseEntity::getState)
                    .containsOnly(LifecycleOperationState.COMPLETED.toString());
        });
    }

    @Test
    public void operationCompletesAndDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-completed-delete-node-fails-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(false);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(true);
        existingOperation.setDeleteNodeErrorMessage("failure");

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-completed-delete-node-fails-1");

        terminateOperation.completed(completed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperation.getError()).contains("failure");
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationFailsAndDeleteNodeIsSuccessful() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-failed-delete-node-success-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(false);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("terminate-failed-delete-node-success-1", "timed out");

        terminateOperation.failed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperation.getError()).contains("timed out");
        assertThat(updatedOperation.isDeleteNodeFailed()).isFalse();
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationFailsAndDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-failed-delete-node-failed-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(false);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(true);
        existingOperation.setDeleteNodeFailed(true);
        existingOperation.setDeleteNodeErrorMessage("delete node failure");

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("terminate-failed-delete-node-failed-1", "timed out");

        terminateOperation.failed(failed);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(same(existingInstance), operationCaptor.capture());

        final var updatedOperation = operationCaptor.getValue();
        assertThat(updatedOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperation.getError()).contains("timed out", "delete node failure");
        assertThat(updatedOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperation.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationCompletesBeforeDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-complete-after-delete-node-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-complete-after-delete-node-1");

        terminateOperation.completed(completed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError()).containsOnlyOnce("delete node failure");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationCompletesBeforeDeleteNodeSucceeds() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-complete-after-delete-node-succeeds-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        when(databaseInteractionService.isNamespaceSetForDeletion(anyString())).thenReturn(true);

        when(messageUtility.triggerDeleteNamespace(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-complete-after-delete-node-succeeds-1");

        terminateOperation.completed(completed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationFailsBeforeDeleteNodeSucceeds() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-fails-before-delete-node-succeeds-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("terminate-fails-before-delete-node-succeeds-1", "timed out");

        terminateOperation.failed(failed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError()).containsOnlyOnce("timed out");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void operationFailsBeforeDeleteNodeFails() {
        // given
        HelmChart chart = createChart("chart-id-1", 1, "terminate-fails-after-delete-node-fails-1", HelmReleaseState.PROCESSING);

        final VnfInstance existingInstance = createInstance(List.of(chart));
        existingInstance.setAddedToOss(true);

        final LifecycleOperation existingOperation = createOperation(existingInstance);
        existingOperation.setDeleteNodeFinished(false);
        existingOperation.setDeleteNodeFailed(false);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(existingOperation);

        // when
        final HelmReleaseLifecycleMessage failed = createFailedMessage("terminate-fails-after-delete-node-fails-1",
                                                                       "timed out waiting for condition");

        terminateOperation.failed(failed);

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
        assertThat(updatedOperationAfterDeleteNodeCheck.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(updatedOperationAfterDeleteNodeCheck.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                                      + "\"detail\":\"TERMINATE for "
                                                                                      + "terminate-fails-after-delete-node-fails-1 failed with "
                                                                                      + "timed out waiting for condition.\\nDelete Node from ENM "
                                                                                      + "failure, please delete the Node from ENM "
                                                                                      + "manually:\\\"delete node failure\\\"\",\"instance\":\"about:blank\"}");
        assertThat(updatedOperationAfterDeleteNodeCheck.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(updatedOperationAfterDeleteNodeCheck.getVnfInstance()).satisfies(updatedInstance -> {
            assertThat(updatedInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
            assertThat(updatedInstance.getNamespace()).isNotNull();
            assertThat(updatedInstance.getClusterName()).isNotNull();
        });
    }

    @Test
    public void shouldNotRouteToCompletedCCVPWhenTerminateIsExecutedAndDeletePvcCompleted() throws Exception {
        // given
        HelmChart chartToTerminate = createChart("chart-id-1", 1, "terminate-complete-for-ccvp-1", HelmReleaseState.PROCESSING);
        final VnfInstance existingInstance = createInstance(new ArrayList<>());
        existingInstance.setDeployableModulesSupported(true);

        final LifecycleOperation ccvpOperation = createOperation(existingInstance);
        ccvpOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);

        VnfInstance tempInstance = createInstance(new ArrayList<>());
        TerminatedHelmChart terminatedHelmChart = toTerminateHelmChart(chartToTerminate, ccvpOperation);
        tempInstance.setTerminatedHelmCharts(List.of(terminatedHelmChart));

        ObjectMapper objectMapper = new ObjectMapper();
        existingInstance.setTempInstance(objectMapper.writeValueAsString(tempInstance));

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(ccvpOperation);
        when(messageUtility.triggerDeletePvcs(any(), any(), any())).thenReturn(true);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-complete-for-ccvp-1");

        terminateOperation.completed(completed);

        // then
        assertThat(completed.getOperationType()).isEqualTo(HelmReleaseOperationType.TERMINATE);
        verify(databaseInteractionService).persistVnfInstanceAndOperation(any(VnfInstance.class), any(LifecycleOperation.class));
        verify(changeVnfPackageOperation, times(0)).completed(any());
    }

    @Test
    public void shouldRouteToFailedCCVPWhenTerminateIsExecutedAndDeletePvcFailed() throws Exception {
        // given
        HelmChart chartToTerminate = createChart("chart-id-1", 1, "terminate-complete-for-ccvp-1", HelmReleaseState.PROCESSING);
        final VnfInstance existingInstance = createInstance(new ArrayList<>());
        existingInstance.setDeployableModulesSupported(true);

        final LifecycleOperation ccvpOperation = createOperation(existingInstance);
        ccvpOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);

        VnfInstance tempInstance = createInstance(new ArrayList<>());
        TerminatedHelmChart terminatedHelmChart = toTerminateHelmChart(chartToTerminate, ccvpOperation);
        tempInstance.setTerminatedHelmCharts(List.of(terminatedHelmChart));

        ObjectMapper objectMapper = new ObjectMapper();
        existingInstance.setTempInstance(objectMapper.writeValueAsString(tempInstance));

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(ccvpOperation);
        when(messageUtility.triggerDeletePvcs(any(), any(), any())).thenReturn(false);

        // when
        final HelmReleaseLifecycleMessage completed = createCompletedMessage("terminate-complete-for-ccvp-1");

        terminateOperation.completed(completed);

        // then
        assertThat(completed.getOperationType()).isEqualTo(HelmReleaseOperationType.CHANGE_VNFPKG);
        verify(databaseInteractionService).persistVnfInstanceAndOperation(any(VnfInstance.class), any(LifecycleOperation.class));
        verify(changeVnfPackageOperation, times(1)).failed(any());
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
        chart.setState(state != null ? state.toString() : null);

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

    private void whenWfsRespondsWithAccepted() {
        final WorkflowRoutingResponse wfsResponse = new WorkflowRoutingResponse();
        wfsResponse.setHttpStatus(HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeTerminateRequest(any(), any(), any())).thenReturn(wfsResponse);
    }
}
