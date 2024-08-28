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
package com.ericsson.vnfm.orchestrator.messaging.handlers.scale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


@SpringBootTest(classes = NextChartAndRetryCheck.class)
public class NextChartAndRetryCheckTest {

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private MessageUtility messageUtility;

    @MockBean
    private ScaleService scaleService;

    @Autowired
    private NextChartAndRetryCheck nextChartAndRetryCheck;

    @Test
    public void shouldUpdateChartForRollback() {
        // given
        final var message = new HelmReleaseLifecycleMessage();
        message.setMessage("Error occurred");
        message.setReleaseName("release-name-2");

        final var helmChart1 = new HelmChart();
        helmChart1.setState(HelmReleaseState.COMPLETED.toString());
        final var helmChart2 = new HelmChart();
        helmChart2.setState(HelmReleaseState.PROCESSING.toString());
        helmChart2.setRetryCount(1);
        helmChart2.setPriority(1);

        final var tempInstance = new VnfInstance();
        tempInstance.setHelmCharts(List.of(helmChart1, helmChart2));

        final var instance = new VnfInstance();
        instance.setTempInstance(Utility.convertObjToJsonString(tempInstance));

        final var context = new MessageHandlingContext<>(message);
        context.setVnfInstance(instance);
        context.setOperation(new LifecycleOperation());

        given(scaleService.getScaleRetryAttempts()).willReturn(5);

        final var workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.BAD_GATEWAY);
        given(workflowRoutingService.routeScaleRequest(anyInt(), any(), any())).willReturn(workflowRoutingResponse);

        // when
        nextChartAndRetryCheck.handle(context);

        // then
        verify(messageUtility).updateChartForRollback(same(instance), same(message), any());
    }
}
