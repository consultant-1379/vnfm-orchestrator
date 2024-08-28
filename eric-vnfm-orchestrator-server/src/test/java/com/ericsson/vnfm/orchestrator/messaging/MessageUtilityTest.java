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
package com.ericsson.vnfm.orchestrator.messaging;

import static java.util.Collections.singletonList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DeleteNamespaceException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Rollback;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class MessageUtilityTest {

    @Mock
    private DatabaseInteractionService databaseInteractionService;

    @Mock
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Mock
    private Rollback rollback;

    @Mock
    private WorkflowRoutingService workflowRoutingService;

    @Mock
    private InstanceService instanceService;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private MessageUtility messageUtility;

    @Test
    public void updateFailedOperationAndChartByReleaseNameShouldNotUpdateInstanceAndOperationWhenOperationIsFinished() {
        // given
        final var lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationState(LifecycleOperationState.ROLLED_BACK);

        given(databaseInteractionService.getLifecycleOperation(anyString())).willReturn(lifecycleOperation);

        // when
        messageUtility.updateFailedOperationAndChartByReleaseName("operation-id-1",
                                                                  "error message",
                                                                  "fail details",
                                                                  LifecycleOperationState.FAILED,
                                                                  LifecycleOperationState.FAILED,
                                                                  "failed-release-1");

        // then
        verify(databaseInteractionService, never()).persistLifecycleOperation(any());
        verify(databaseInteractionService, never()).saveVnfInstanceToDB(any());
    }

    @Test
    public void updateChartForRollbackShouldModifyChartStateAndTriggerRollback() {
        // given
        final var helmChart1 = new HelmChart();
        helmChart1.setReleaseName("release-name-1");
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName("release-name-2");
        helmChart2.setRevisionNumber("2");
        final var charts = List.of(helmChart1, helmChart2);

        final var tempInstance = new VnfInstance();
        tempInstance.setHelmCharts(charts);

        final var instance = new VnfInstance();
        instance.setHelmCharts(charts);
        instance.setTempInstance(convertObjToJsonString(tempInstance));

        final var message = new HelmReleaseLifecycleMessage();
        message.setMessage("Error occurred");
        message.setReleaseName("release-name-2");
        message.setOperationType(HelmReleaseOperationType.SCALE);
        message.setLifecycleOperationId("operation-id-1");

        final var operation = new LifecycleOperation();
        operation.setApplicationTimeout("1234");
        given(databaseInteractionService.getLifecycleOperation(anyString())).willReturn(operation);

        // when
        messageUtility.updateChartForRollback(instance, message, tempInstance);

        // then
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"SCALE for "
                                                           + "release-name-2 failed with Error occurred.\",\"instance\":\"about:blank\"}");

        final var modifiedTempInstance = Utility.parseJson(instance.getTempInstance(), VnfInstance.class);
        assertThat(modifiedTempInstance.getHelmCharts())
                .extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                .contains(tuple("release-name-2", HelmReleaseState.ROLLING_BACK.toString()));

        verify(lifeCycleManagementHelper).setExpiredTimeoutAndPersist(same(operation), eq("1234"));
        verify(rollback).triggerRollbackOperation(same(operation), eq("release-name-2"), eq("2"), same(instance), eq(false));
    }

    @Test
    public void triggerDeleteNamespaceShouldSetErrorMessageWhenDeletionFails() {
        // given
        final var operation = new LifecycleOperation();
        operation.setOperationOccurrenceId("occurrence-id-1");

        doThrow(new DeleteNamespaceException("Deletion failed", HttpStatus.INTERNAL_SERVER_ERROR))
                .when(workflowRoutingService).routeDeleteNamespace(anyString(), anyString(), anyString(), anyString(), anyString());

        // when
        messageUtility.triggerDeleteNamespace(operation, "namespace-1", "cluster-1", "release-name-1", "1234");

        // then
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Deletion "
                                                           + "failed\",\"instance\":\"about:blank\"}");
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(operation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), byLessThan(5L, ChronoUnit.SECONDS));
    }

    @Test
    public void triggerPatchSecretShouldSetErrorMessageWhenPatchingFails() {
        // given
        final var operation = new LifecycleOperation();
        operation.setOperationOccurrenceId("occurrence-id-1");

        final var instance = new VnfInstance();
        instance.setClusterName("cluster-1");
        instance.setNamespace("namespace-1");

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Error occurred");
        given(workflowRoutingService.routeToEvnfmWfsForPatchingSecrets(anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(response);

        // when
        messageUtility.triggerPatchSecret("secret-name-1", "key-1", "contents-1", operation, instance);

        // then
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Error "
                                                           + "occurred\",\"instance\":\"about:blank\"}");

        verify(databaseInteractionService).persistLifecycleOperation(same(operation));
    }

    @Test
    public void triggerDeletePvcsShouldSetOperationCompletedWhenNamespaceNotFound() {
        // given
        final var helmChart = new HelmChart();
        helmChart.setReleaseName("release-name-2");

        final var instance = new VnfInstance();
        instance.setHelmCharts(singletonList(helmChart));

        final var operation = new LifecycleOperation();

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Namespace namespace-1 not found in cluster cluster-1");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any())).willReturn(response);

        // when
        final var result = messageUtility.triggerDeletePvcs(operation, instance, "release-name-2");

        // then
        assertThat(result).isTrue();
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
    }

    @Test
    public void triggerDeletePvcsShouldSetChartDeletePvcStateWhenErrorOccurs() {
        // given
        final var helmChart1 = new HelmChart();
        helmChart1.setReleaseName("release-name-1");
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName("release-name-2");

        final var instance = new VnfInstance();
        instance.setHelmCharts(List.of(helmChart1, helmChart2));

        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Some error");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any(), any())).willReturn(response);

        // when
        final var result = messageUtility.triggerDeletePvcs(operation, instance, "release-name-2");

        // then
        assertThat(result).isFalse();
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Some error\","
                                                           + "\"instance\":\"about:blank\"}");

        assertThat(helmChart1.getDeletePvcState()).isNull();
        assertThat(helmChart2.getDeletePvcState()).isEqualTo(LifecycleOperationState.FAILED.toString());
    }

    @Test
    public void triggerDeletePvcsForCcvpShouldSetChartDeletePvcStateForTerminatedChartsWhenErrorOccurs() {
        // given
        final var terminatedHelmChart1 = new TerminatedHelmChart();
        terminatedHelmChart1.setReleaseName("release-name-1");
        final var terminatedHelmChart2 = new TerminatedHelmChart();
        terminatedHelmChart2.setReleaseName("release-name-2");
        final var tempInstance = new VnfInstance();
        tempInstance.setTerminatedHelmCharts(List.of(terminatedHelmChart1, terminatedHelmChart2));

        final var helmChart1 = new HelmChart();
        helmChart1.setReleaseName("release-name-1");
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName("release-name-2");

        final var instance = new VnfInstance();
        instance.setHelmCharts(List.of(helmChart1, helmChart2));
        instance.setTempInstance(convertObjToJsonString(tempInstance));

        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Some error");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any(), any())).willReturn(response);

        // when
        final var result = messageUtility.triggerDeletePvcs(operation, instance, "release-name-2");

        // then
        assertThat(result).isFalse();
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Some error\","
                                                           + "\"instance\":\"about:blank\"}");

        final VnfInstance actualTempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        final TerminatedHelmChart actualTerminatedHelmChart1 = actualTempInstance.getTerminatedHelmCharts().stream()
                .filter(terminatedHelmChart -> terminatedHelmChart.getReleaseName().equals("release-name-1"))
                .findFirst()
                .orElseThrow();
        final TerminatedHelmChart actualTerminatedHelmChart2 = actualTempInstance.getTerminatedHelmCharts().stream()
                .filter(terminatedHelmChart -> terminatedHelmChart.getReleaseName().equals("release-name-2"))
                .findFirst()
                .orElseThrow();

        assertThat(actualTerminatedHelmChart1.getDeletePvcState()).isNull();
        assertThat(actualTerminatedHelmChart2.getDeletePvcState()).isEqualTo(LifecycleOperationState.FAILED.toString());
    }

    @Test
    public void isLifecycleOperationIdNullShouldReturnTrueWhenIdIsNull() {
        assertThat(MessageUtility.isLifecycleOperationIdNull(null)).isTrue();
    }

    @Test
    public void isMessageProcessorNotNullShouldReturnFalseWhenProcessorIsNull() {
        assertThat(MessageUtility.isMessageProcessorNotNull(null, null)).isFalse();
    }

    @Test
    public void updateOperationOnFailShouldNotUpdateIfAlreadyFinished() {
        for (final var state : EnumSet.of(LifecycleOperationState.COMPLETED, LifecycleOperationState.FAILED, LifecycleOperationState.ROLLED_BACK)) {
            // given
            final var operation = new LifecycleOperation();
            operation.setOperationState(state);

            when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operation);

            // when
            messageUtility.updateOperationOnFail("occurrence-id", "error message", "fail details",
                                                 InstantiationState.INSTANTIATED, LifecycleOperationState.FAILED);

            // then
            verify(databaseInteractionService).getLifecycleOperation(eq("occurrence-id"));
            verifyNoMoreInteractions(databaseInteractionService);

            reset(databaseInteractionService);
        }
    }

    @Test
    public void updateOperationOnFailShouldUpdateIfNotFinished() {
        // given
        final var chart1 = new HelmChart();
        chart1.setState(LifecycleOperationState.PROCESSING.toString());
        final var chart2 = new HelmChart();
        chart2.setState(LifecycleOperationState.COMPLETED.toString());

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setHelmCharts(List.of(chart1, chart2));

        final var operation = new LifecycleOperation();
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setVnfInstance(instance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operation);

        final var namespaceDetails = new VnfInstanceNamespaceDetails();
        namespaceDetails.setDeletionInProgress(true);

        when(databaseInteractionService.getNamespaceDetails(anyString())).thenReturn(Optional.of(namespaceDetails));

        // when
        messageUtility.updateOperationOnFail("occurrence-id", "error message", "fail details",
                                             InstantiationState.INSTANTIATED, LifecycleOperationState.FAILED);

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("occurrence-id"));

        final var operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistLifecycleOperation(operationCaptor.capture());
        assertThat(operationCaptor.getValue()).satisfies(actualOperation -> {
            assertThat(actualOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
            assertThat(actualOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
            assertThat(actualOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"error"
                                                                     + " message\",\"instance\":\"about:blank\"}");
        });

        final var instanceCaptor = ArgumentCaptor.forClass(VnfInstance.class);
        verify(databaseInteractionService).saveVnfInstanceToDB(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getHelmCharts())
                .extracting(HelmChart::getState)
                .containsOnly(LifecycleOperationState.FAILED.toString());

        verify(databaseInteractionService).getNamespaceDetails(eq("instance-id"));
        final var namespaceDetailsCaptor = ArgumentCaptor.forClass(VnfInstanceNamespaceDetails.class);
        verify(databaseInteractionService).persistNamespaceDetails(namespaceDetailsCaptor.capture());
        assertThat(namespaceDetailsCaptor.getValue().isDeletionInProgress()).isFalse();
    }

    @Test
    public void lifecycleTimedOutShouldUpdateOperation() {
        // given
        final var chart = new HelmChart();
        chart.setState(LifecycleOperationState.PROCESSING.toString());

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setHelmCharts(List.of(chart));
        instance.setTempInstance("{}");

        final var operation = new LifecycleOperation();
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setVnfInstance(instance);

        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(operation);

        when(databaseInteractionService.getNamespaceDetails(anyString())).thenReturn(Optional.empty());

        // when
        messageUtility.lifecycleTimedOut("occurrence-id", InstantiationState.NOT_INSTANTIATED, "error message");

        // then
        verify(databaseInteractionService).getLifecycleOperation(eq("occurrence-id"));

        final var operationCaptor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistLifecycleOperation(operationCaptor.capture());
        assertThat(operationCaptor.getValue()).satisfies(actualOperation -> {
            assertThat(actualOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
            assertThat(actualOperation.getStateEnteredTime()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
            assertThat(actualOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"error"
                                                                     + " message\",\"instance\":\"about:blank\"}");
        });

        final var instanceCaptor = ArgumentCaptor.forClass(VnfInstance.class);
        verify(databaseInteractionService).saveVnfInstanceToDB(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue()).satisfies(actualInstance -> {
            assertThat(actualInstance.getHelmCharts())
                    .extracting(HelmChartBaseEntity::getState)
                    .containsOnly(LifecycleOperationState.FAILED.toString());
            assertThat(actualInstance.getTempInstance()).isNull();
        });
    }

    @Test
    public void deleteIdentifierShouldDeleteInstance() {
        // given
        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");

        final var operation = new LifecycleOperation();
        operation.setOperationParams("{\"additionalParams\": {\"deleteIdentifier\": true}}");

        // when
        messageUtility.deleteIdentifier(instance, operation);

        // then
        verify(instanceService).deleteInstanceEntity(eq("instance-id"), eq(false));
    }
}
