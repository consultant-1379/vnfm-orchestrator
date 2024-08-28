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
package com.ericsson.vnfm.orchestrator.messaging.events;

import static com.ericsson.vnfm.orchestrator.messaging.MessageConverter.convertEventToHelmMessage;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.DELETE_PVC;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.TERMINATE;

import com.ericsson.vnfm.orchestrator.messaging.operations.ChangeVnfPackageOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractMessageProcessor;
import com.ericsson.vnfm.orchestrator.messaging.operations.DeletePvcOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackFailureOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackPatternOperation;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

@Component
public class DeletePvcEvent extends AbstractMessageProcessor {

    @Autowired
    private DeletePvcOperation deletePvcOperation;

    @Autowired
    private RollbackFailureOperation rollbackFailureOperation;

    @Autowired
    private RollbackPatternOperation rollbackPatternOperation;

    @Autowired
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Override
    public Conditions getConditions() {
        return new Conditions(WorkflowServiceEventType.DELETE_PVC.toString(), WorkflowServiceEventMessage.class);
    }

    @Override
    public void completed(final WorkflowServiceEventMessage message) {
        String operationId = message.getLifecycleOperationId();
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(operationId);
        HelmReleaseLifecycleMessage helmMessage = convertEventToHelmMessage(message, DELETE_PVC);
        executeCompleted(lifecycleOperation, helmMessage);
    }

    @Override
    public void failed(final WorkflowServiceEventMessage message) {
        String operationId = message.getLifecycleOperationId();
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(operationId);
        HelmReleaseLifecycleMessage helmMessage = convertEventToHelmMessage(message, DELETE_PVC);
        executeFailed(lifecycleOperation, helmMessage);

    }

    private void executeCompleted(LifecycleOperation operation, final HelmReleaseLifecycleMessage helmMessage) {
        if (operation.getFailurePattern() != null) {
            rollbackFailureOperation.completed(helmMessage);
        } else if (operation.getRollbackPattern() != null) {
            rollbackPatternOperation.completed(helmMessage);
        } else if (operation.getUpgradePattern() != null) {
            changeVnfPackageOperation.completed(helmMessage);
        } else {
            helmMessage.setOperationType(TERMINATE);
            deletePvcOperation.completed(helmMessage);
        }
    }

    private void executeFailed(LifecycleOperation operation, final HelmReleaseLifecycleMessage helmMessage) {
        if (operation.getFailurePattern() != null) {
            rollbackFailureOperation.failed(helmMessage);
        } else if (operation.getRollbackPattern() != null) {
            rollbackPatternOperation.failed(helmMessage);
        } else if (operation.getUpgradePattern() != null) {
            changeVnfPackageOperation.failed(helmMessage);
        } else {
            helmMessage.setOperationType(TERMINATE);
            deletePvcOperation.failed(helmMessage);
        }
    }
}
