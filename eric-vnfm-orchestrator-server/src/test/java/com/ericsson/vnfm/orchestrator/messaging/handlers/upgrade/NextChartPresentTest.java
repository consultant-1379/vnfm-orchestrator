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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


@SpringBootTest(classes = NextChartPresent.class)
public class NextChartPresentTest {

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private MessageUtility messageUtility;

    @MockBean
    private RollbackService rollbackService;

    @Autowired
    private NextChartPresent nextChartPresent;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void shouldUpdateChartForRollback() {
        // given
        final var message = new HelmReleaseLifecycleMessage();
        final String releaseName = "releaseName";
        message.setReleaseName(releaseName);

        final var helmChart1 = new HelmChart();
        helmChart1.setState(HelmReleaseState.COMPLETED.toString());
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName(releaseName);
        helmChart2.setHelmChartType(HelmChartType.CNF);

        final var tempInstance = new VnfInstance();
        tempInstance.setHelmCharts(List.of(helmChart1, helmChart2));
        tempInstance.setTerminatedHelmCharts(Collections.emptyList());

        final var instance = new VnfInstance();
        instance.setTempInstance(Utility.convertObjToJsonString(tempInstance));

        final var context = new MessageHandlingContext<>(message);
        context.setVnfInstance(instance);
        context.setOperation(new LifecycleOperation());

        given(changePackageOperationDetailsRepository.findById(any())).willReturn(Optional.empty());

        final var workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.BAD_GATEWAY);
        given(workflowRoutingService.routeChangePackageInfoRequest(anyInt(), any(), any())).willReturn(workflowRoutingResponse);

        // when
        nextChartPresent.handle(context);

        // then
        verify(rollbackService).rollbackChart(any(VnfInstance.class), any(), any(), any());
    }

    @Test
    public void shouldRouteToTerminateWhenChartIsDisabled() {
        // given
        final String operationOccurenceId = "wm8fcbc8-rd45-4673";
        final var message = new HelmReleaseLifecycleMessage();

        final var helmChart1 = new TerminatedHelmChart();
        helmChart1.setChartEnabled(false);
        helmChart1.setReleaseName("terminate-chart-release-1");
        helmChart1.setOperationOccurrenceId(operationOccurenceId);

        final var helmChart2 = new HelmChart();
        helmChart2.setChartEnabled(false);
        helmChart2.setReleaseName("ccvp-chart-release-1");
        helmChart2.setState(LifecycleOperationState.COMPLETED.toString());

        final var tempInstance = new VnfInstance();
        tempInstance.setHelmCharts(List.of(helmChart2));
        tempInstance.setTerminatedHelmCharts(List.of(helmChart1));

        final var instance = new VnfInstance();
        instance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        instance.setHelmCharts(List.of(helmChart2));
        instance.setDeployableModulesSupported(true);

        final LifecycleOperation ccvpOperation = new LifecycleOperation();
        ccvpOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        ccvpOperation.setVnfInstance(instance);
        ccvpOperation.setOperationOccurrenceId(operationOccurenceId);

        final var context = new MessageHandlingContext<>(message);
        context.setVnfInstance(instance);
        context.setOperation(ccvpOperation);

        final var workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeTerminateRequest(any(), any(), any())).thenReturn(workflowRoutingResponse);

        given(changePackageOperationDetailsRepository.findById(any())).willReturn(Optional.empty());

        final var terminateReleaseNameCaptor = ArgumentCaptor.forClass(String.class);

        // when
        nextChartPresent.handle(context);

        // then
        verify(workflowRoutingService).routeTerminateRequest(any(), any(), terminateReleaseNameCaptor.capture());
        String helmChartTerminateReleaseName = terminateReleaseNameCaptor.getValue();
        assertEquals(helmChart1.getReleaseName(), helmChartTerminateReleaseName);
    }

    @Test
    public void shouldRouteToCCVPWhenChartIsEnabled() {
        // given
        final var message = new HelmReleaseLifecycleMessage();

        final var helmChart1 = new HelmChart();
        helmChart1.setChartEnabled(true);
        helmChart1.setReleaseName("upgrade-chart-release-1");

        final var tempInstance = new VnfInstance();
        tempInstance.setHelmCharts(List.of(helmChart1));

        final var instance = new VnfInstance();
        instance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        instance.setHelmCharts(List.of(helmChart1));
        instance.setDeployableModulesSupported(true);

        final LifecycleOperation ccvpOperation = new LifecycleOperation();
        ccvpOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        ccvpOperation.setVnfInstance(instance);
        ccvpOperation.setOperationOccurrenceId("wm8fcbc8-rd45-4673");

        final var context = new MessageHandlingContext<>(message);
        context.setVnfInstance(instance);
        context.setOperation(ccvpOperation);

        final var workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeChangePackageInfoRequest(anyInt(), any(), any())).thenReturn(workflowRoutingResponse);

        given(changePackageOperationDetailsRepository.findById(any())).willReturn(Optional.empty());

        // when
        nextChartPresent.handle(context);

        // then
        verify(workflowRoutingService).routeChangePackageInfoRequest(anyInt(), any(), any());
    }
}
