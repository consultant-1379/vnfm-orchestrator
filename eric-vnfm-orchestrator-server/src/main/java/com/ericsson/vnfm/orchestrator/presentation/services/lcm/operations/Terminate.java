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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getAdditionalParams;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.resolveTimeOut;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Terminate implements Command {

    @Autowired
    WorkflowRoutingService workflowRoutingService;

    @Override
    public CommandType getType() {
        return CommandType.TERMINATE;
    }

    @Override
    public void execute(final LifecycleOperation operation, final HelmChart helmChart, final boolean isDowngradeOperation) {
        LOGGER.info("Starting Terminate command");
        triggerTerminateOperation(operation, operation.getVnfInstance(), helmChart.getReleaseName());
    }

    private void triggerTerminateOperation(LifecycleOperation operation, VnfInstance vnfInstance, String releaseName) {
        Map<String, Object> additionalParams = getAdditionalParams(operation);
        additionalParams.put(SKIP_JOB_VERIFICATION, true);
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        WorkflowRoutingResponse response = workflowRoutingService
                .routeTerminateRequest(vnfInstance, operation, additionalParams, releaseName);
        if (response.getHttpStatus().isError()) {
            OperationsUtils.updateOperationOnFailure(response.getErrorMessage(), operation, vnfInstance, releaseName);
        }
    }
}

