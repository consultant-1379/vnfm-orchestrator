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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;


@SpringBootTest(classes = {
        Terminate.class
})
public class TerminateTest {
    @Autowired
    private Terminate terminate;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    private static final String TEST_ERROR = "Test error message";

    @Test
    public void testExecuteShouldUpdateOperationOnError() {
        WorkflowRoutingResponse negativeResponse = new WorkflowRoutingResponse();
        negativeResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        negativeResponse.setErrorMessage(TEST_ERROR);
        when(workflowRoutingService.routeTerminateRequest(any(), any(), ArgumentMatchers.anyMap(),
                                                          any())).thenReturn(negativeResponse);
        LifecycleOperation operation = new LifecycleOperation();
        VnfInstance vnfInstance = new VnfInstance();
        HelmChart helmChart = new HelmChart();
        String releaseName = "test-release";
        LifecycleOperationType instantiate = LifecycleOperationType.INSTANTIATE;
        operation.setLifecycleOperationType(instantiate);
        helmChart.setReleaseName(releaseName);
        vnfInstance.setHelmCharts(List.of(helmChart));
        operation.setVnfInstance(vnfInstance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        terminate.execute(operation, helmChart, true);
        assertEquals(LifecycleOperationState.FAILED, operation.getOperationState());
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"INSTANTIATE failed for test-release due to "
                             + "Test error message\",\"instance\":\"about:blank\"}", operation.getError());
    }
}
