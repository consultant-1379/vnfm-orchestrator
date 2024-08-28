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
package com.ericsson.vnfm.orchestrator.messaging.handlers.downsize;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationOnRollingBack;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getFirstChartToDownsizeDuringAutoRollback;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TriggerRollbackDownsize extends MessageHandler<HelmReleaseLifecycleMessage> {

    private WorkflowRoutingService workflowRoutingService;
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("AutoRollback on release: {}", context.getMessage().getReleaseName());

        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        VnfInstance oldInstanceWithChartsToDownsize = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        updateOperationOnRollingBack(operation);

        triggerDownsize(context.getMessage().getReleaseName(), oldInstanceWithChartsToDownsize, operation);

        vnfInstance.setTempInstance(convertObjToJsonString(oldInstanceWithChartsToDownsize));

        passToSuccessor(getSuccessor(), context);
    }

    public void triggerDownsize(final String currentReleaseName, final VnfInstance tempInstance, final LifecycleOperation operation) {
        final Optional<HelmChart> firstChartToDownsize = getFirstChartToDownsizeDuringAutoRollback(currentReleaseName, tempInstance, operation);
        if (firstChartToDownsize.isPresent()) {
            firstChartToDownsize.get().setDownsizeState(LifecycleOperationState.PROCESSING.toString());

            final ResponseEntity<Object> responseEntity = workflowRoutingService.routeDownsizeRequest(tempInstance,
                                                                                                      operation,
                                                                                                      currentReleaseName);
            changeVnfPackageRequestHandler.checkAndProcessFailedError(operation, responseEntity, currentReleaseName);
        }
    }
}
