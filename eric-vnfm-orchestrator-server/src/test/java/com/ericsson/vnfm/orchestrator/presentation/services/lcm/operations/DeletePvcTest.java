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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


@SpringBootTest(classes = DeletePvc.class)
public class DeletePvcTest {

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private EvnfmDowngrade evnfmDowngrade;

    @MockBean
    private EvnfmManualRollback evnfmManualRollback;

    @Autowired
    private DeletePvc deletePvc;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTriggerDowngradeOnError() {
        // given
        final var helmChart = new HelmChart();
        helmChart.setReleaseName("release-name-2");

        final var instance = new VnfInstance();

        final var operation = new LifecycleOperation();
        operation.setVnfInstance(instance);
        operation.setFailurePattern("[{\"release-name-1\":\"rollback\"}, {\"release-name-2\":\"delete_pvc[app=postgres,release=test]\"}]");

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Namespace namespace-1 not found in cluster cluster-1");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any(), any())).willReturn(response);

        // when
        deletePvc.execute(operation, helmChart, true);

        // then
        verify(workflowRoutingService).routeDeletePvcRequest(same(instance), eq("release-name-2"), any(), eq("app=postgres"), eq("release=test"));

        final ArgumentCaptor<MessageHandlingContext<HelmReleaseLifecycleMessage>> captor = ArgumentCaptor.forClass(MessageHandlingContext.class);
        verify(evnfmDowngrade).triggerNextStage(captor.capture());

        final var messageContext = captor.getValue();
        assertThat(messageContext.getOperation()).isSameAs(operation);
        assertThat(messageContext.getVnfInstance()).isSameAs(instance);
        assertThat(messageContext.getMessage()).satisfies(message -> {
            assertThat(message.getOperationType()).isEqualTo(HelmReleaseOperationType.DELETE_PVC);
            assertThat(message.getReleaseName()).isEqualTo("release-name-2");
            assertThat(message.getState()).isEqualTo(HelmReleaseState.COMPLETED);
        });

        verifyNoInteractions(evnfmManualRollback);
    }

    @Test
    public void shouldTriggerManualRollbackOnError() {
        // given
        final var helmChart = new HelmChart();
        helmChart.setReleaseName("release-name-2");

        final var instance = new VnfInstance();

        final var operation = new LifecycleOperation();
        operation.setVnfInstance(instance);
        operation.setFailurePattern("[{\"release-name-1\":\"rollback\"}, {\"release-name-2\":\"delete_pvc[app=postgres,release=test]\"}]");

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Namespace namespace-1 not found in cluster cluster-1");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any(), any())).willReturn(response);

        // when
        deletePvc.execute(operation, helmChart, false);

        // then
        verify(evnfmManualRollback).triggerNextStage(any());
        verifyNoInteractions(evnfmDowngrade);
    }

    @Test
    public void shouldUpdateOperationAndChartsOnError() {
        // given
        final var helmChart1 = new HelmChart();
        helmChart1.setReleaseName("release-name-1");
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName("release-name-2");

        final var instance = new VnfInstance();
        instance.setHelmCharts(List.of(helmChart1, helmChart2));

        final var operation = new LifecycleOperation();
        operation.setVnfInstance(instance);
        operation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        operation.setFailurePattern("[{\"release-name-1\":\"rollback\"}, {\"release-name-2\":\"delete_pvc[app=postgres,release=test]\"}]");

        final ResponseEntity<Object> response = ResponseEntity.badRequest().body("Some error");
        given(workflowRoutingService.routeDeletePvcRequest(any(), anyString(), any(), any())).willReturn(response);

        // when
        deletePvc.execute(operation, helmChart2, true);

        // then
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"CHANGE_VNFPKG "
                                                           + "failed for release-name-2 due to Some error\",\"instance\":\"about:blank\"}");
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);

        assertThat(helmChart1.getState()).isNull();
        assertThat(helmChart1.getDeletePvcState()).isNull();

        assertThat(helmChart2.getState()).isEqualTo(LifecycleOperationState.FAILED.toString());
        assertThat(helmChart2.getDeletePvcState()).isEqualTo(LifecycleOperationState.FAILED.toString());

        verifyNoInteractions(evnfmDowngrade);
        verifyNoInteractions(evnfmManualRollback);
    }
}
