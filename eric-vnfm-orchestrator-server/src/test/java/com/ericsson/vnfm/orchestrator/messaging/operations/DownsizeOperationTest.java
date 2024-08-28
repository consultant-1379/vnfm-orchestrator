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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class DownsizeOperationTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @Autowired
    private DownsizeOperation downsizeOperation;

    @Test
    public void testDownsizeOperationSuccessForAllCharts() {
        final String lifecycleOperationId = "wm8fcbc8-rd1f-4673-oper-downsize0001";

        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.COMPLETED);
        firstChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("multiple-charts-downsize-1");

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.COMPLETED);
        secondChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("multiple-charts-downsize-2");

        whenWfsRespondsWithAccepted();

        downsizeOperation.completed(secondChart);
        VnfInstance vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), secondChart.getReleaseName(), LifecycleOperationState.COMPLETED.toString());

        verify(workflowRoutingService).routeDownsizeRequest(any(), any(), eq("multiple-charts-downsize-1"));

        downsizeOperation.completed(firstChart);
        vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), firstChart.getReleaseName(), LifecycleOperationState.COMPLETED.toString());

        verify(changeVnfPackageRequestHandler).sendPostDownsizeRequest(any(), any());
    }

    @Test
    public void testDownsizeOperationPassFirstChartFailSecondChart() {
        final String lifecycleOperationId = "wm8fcbc8-rd1f-4673-oper-downsize0002";

        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("multiple-charts-downsize-fail-second-1");
        firstChart.setMessage("Unable to scale down ReplicaSets or StatefulSets in namespace multiple-charts-downsize-fail-second with release name "
                                  + "multiple-charts-downsize-fail-second-1 due to DownsizeOperationTestError");

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.COMPLETED);
        secondChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("multiple-charts-downsize-fail-second-2");

        whenWfsRespondsWithAccepted();

        downsizeOperation.completed(secondChart);
        VnfInstance vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), secondChart.getReleaseName(), LifecycleOperationState.COMPLETED.toString());
        assertThat(vnfInstance.getNamespace()).isNotNull();
        assertThat(vnfInstance.getClusterName()).isNotNull();

        verify(workflowRoutingService).routeDownsizeRequest(any(), any(), eq("multiple-charts-downsize-fail-second-1"));

        downsizeOperation.failed(firstChart);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), firstChart.getReleaseName(), LifecycleOperationState.FAILED.toString());
        waitForOperationToHaveStateOf(lifecycleOperationId, LifecycleOperationState.ROLLING_BACK);

        verify(changeVnfPackageRequestHandler).sendPostDownsizeRequest(any(), any());
    }

    @Test
    public void testDownsizeOperationFailFirstChart() {
        final String lifecycleOperationId = "wm8fcbc8-rd1f-4673-oper-downsize0003";

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.FAILED);
        secondChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("multiple-charts-downsize-fail-first-2");
        secondChart.setMessage("Unable to scale down ReplicaSets or StatefulSets in namespace multiple-charts-downsize-fail-first with release name "
                                      + "multiple-charts-downsize-fail-first-2 due to DownsizeOperationTestError");

        downsizeOperation.failed(secondChart);
        VnfInstance vnfInstance = getVnfInstance(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), secondChart.getReleaseName(), LifecycleOperationState.FAILED.toString());
        assertThat(vnfInstance.getNamespace()).isNotNull();
        assertThat(vnfInstance.getClusterName()).isNotNull();
        waitForOperationToHaveStateOf(lifecycleOperationId, LifecycleOperationState.ROLLING_BACK);

        verifyNoInteractions(workflowRoutingService);
        verify(changeVnfPackageRequestHandler).sendPostDownsizeRequest(any(), any());
    }

    @Test
    public void testDownsizeOperationForCrds() {
        final String lifecycleOperationId = "wm8fcbc8-rd1f-4674-crd-downsize0001";

        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.COMPLETED);
        firstChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("multiple-charts-downsize-1");

        downsizeOperation.completed(firstChart);

        VnfInstance vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), firstChart.getReleaseName(), null);

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.COMPLETED);
        secondChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("multiple-charts-downsize-2");

        downsizeOperation.completed(secondChart);

        vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), secondChart.getReleaseName(), null);

        final HelmReleaseLifecycleMessage thirdChart = new HelmReleaseLifecycleMessage();
        thirdChart.setState(HelmReleaseState.COMPLETED);
        thirdChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        thirdChart.setLifecycleOperationId(lifecycleOperationId);
        thirdChart.setReleaseName("multiple-charts-downsize-3");

        downsizeOperation.completed(thirdChart);

        vnfInstance = verifyStates(lifecycleOperationId);
        waitForChartToHaveDownsizeStateOf(vnfInstance.getVnfInstanceId(), thirdChart.getReleaseName(), LifecycleOperationState.COMPLETED.toString());
    }

    private void whenWfsRespondsWithAccepted() {
        when(workflowRoutingService.routeDownsizeRequest(any(), any(), any())).thenReturn(ResponseEntity.accepted().build());
    }

    private void waitForChartToHaveDownsizeStateOf(String vnfInstanceId, String helmReleaseName, String expectedState) {
        await().atMost(10, TimeUnit.SECONDS).until(helmChartHaveDownsizeState(vnfInstanceId, helmReleaseName, expectedState));
    }

    private void waitForOperationToHaveStateOf(String lifecycleOperationId, LifecycleOperationState expectedState) {
        await().atMost(10, TimeUnit.SECONDS).until(operationReachesState(lifecycleOperationId, expectedState));
    }

    private Callable<Boolean> helmChartHaveDownsizeState(String vnfInstanceId, String helmReleaseName, String expectedState) {
        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            return instance.getHelmCharts().stream()
                    .filter(chart -> chart.getReleaseName().equals(helmReleaseName))
                    .allMatch(chart -> StringUtils.equals(expectedState, chart.getDownsizeState()));
        };
    }

    private VnfInstance verifyStates(String lifecycleOperationId) {
        VnfInstance vnfInstance = getVnfInstance(lifecycleOperationId);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        waitForOperationToHaveStateOf(lifecycleOperationId, LifecycleOperationState.PROCESSING);
        return vnfInstance;
    }

    private VnfInstance getVnfInstance(String lifecycleOperationId) {
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        return lifecycleOperation.getVnfInstance();
    }

    private Callable<Boolean> operationReachesState(final String lifeCycleOperationId,
                                                    final LifecycleOperationState expectedState) {
        return () -> {
            LifecycleOperation operation =
                    lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
            return expectedState.equals(operation.getOperationState());
        };
    }
}
