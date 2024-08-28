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

import static com.ericsson.vnfm.orchestrator.messaging.operations.TerminateOperation.isReleaseNameOrNamespaceNotFound;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.parsePvcLabels;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.updateChartDeletePvcState;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeletePvc implements Command {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    @Lazy
    private EvnfmDowngrade evnfmDowngrade;

    @Autowired
    @Lazy
    private EvnfmManualRollback evnfmManualRollback;

    @Override
    public CommandType getType() {
        return CommandType.DELETE_PVC;
    }

    @Override
    public void execute(final LifecycleOperation operation, final HelmChart helmChart, final boolean isDowngradeOperation) {
        LOGGER.info("Starting Delete PVC command");
        triggerDeletePvcOperation(operation, operation.getVnfInstance(), helmChart.getReleaseName(), isDowngradeOperation);
    }

    private void triggerDeletePvcOperation(final LifecycleOperation operation,
                                           final VnfInstance vnfInstance,
                                           final String releaseName,
                                           final boolean isDowngradeOperation) {

        String[] labels = parsePvcLabels(operation, releaseName);
        ResponseEntity<Object> response = workflowRoutingService
                .routeDeletePvcRequest(vnfInstance, releaseName, operation.getOperationOccurrenceId(), labels);
        if (response.getStatusCode().isError()) {
            String errorMessage = Optional.ofNullable(response.getBody()).map(Object::toString).orElse("Delete pvc operation failed.");
            if (isReleaseNameOrNamespaceNotFound(errorMessage)) {
                HelmReleaseLifecycleMessage pvcDeleteCompleted = new HelmReleaseLifecycleMessage();
                pvcDeleteCompleted.setOperationType(HelmReleaseOperationType.DELETE_PVC);
                pvcDeleteCompleted.setReleaseName(releaseName);
                pvcDeleteCompleted.setState(HelmReleaseState.COMPLETED);

                MessageHandlingContext<HelmReleaseLifecycleMessage> context = new MessageHandlingContext<>(pvcDeleteCompleted);
                context.setVnfInstance(vnfInstance);
                context.setOperation(operation);
                triggerNextStage(isDowngradeOperation, context);
            } else {
                OperationsUtils.updateOperationOnFailure(errorMessage, operation, vnfInstance, releaseName);
                updateChartDeletePvcState(vnfInstance, releaseName, LifecycleOperationState.FAILED.toString());
            }
        }
    }

    private void triggerNextStage(final boolean isDowngradeOperation,
                                  MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        if (isDowngradeOperation) {
            evnfmDowngrade.triggerNextStage(context);
        } else {
            evnfmManualRollback.triggerNextStage(context);
        }
    }
}
