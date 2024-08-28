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
package com.ericsson.vnfm.orchestrator.messaging.handlers.terminate;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import com.ericsson.vnfm.orchestrator.messaging.DeleteNodeCheckTask;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.TraceRunnableWrapper;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import brave.Tracing;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * If the node was added to ENM this handler checks if the deletion has completed, if not it kicks it off into a
 * separate thread.
 */
@Slf4j
@AllArgsConstructor
public final class DeleteNodeHandler extends MessageHandler<HelmReleaseLifecycleMessage> {

    private static final String ERROR_MESSAGE = "Delete Node from ENM failure, please delete the Node from ENM "
            + "manually: %s. ";

    private ApplicationContext applicationContext;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private Tracing tracing;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling delete node");
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        boolean callSuccessor = true;
        if (deleteNodeFromENM(operation, instance)) {
            callSuccessor = updateOperationOrSchedule(operation, instance);
        } else {
            if (operation.getOperationState().equals(LifecycleOperationState.COMPLETED)) {
                instance.setClusterName(null);
                instance.setNamespace(null);
            }
        }
        if (callSuccessor) {
            passToSuccessor(getSuccessor(), context);
        }
    }

    private static boolean deleteNodeFromENM(final LifecycleOperation operation, final VnfInstance vnfInstance) {
        return operation.isDeleteNodeFinished() || vnfInstance.isAddedToOss();
    }

    private boolean updateOperationOrSchedule(final LifecycleOperation operation, final VnfInstance vnfInstance) {
        LOGGER.info("Delete Node from ENM is part of Operation");
        if (operation.isDeleteNodeFinished()) {
            LOGGER.info("Delete Node from ENM has finished");
            if (operation.isDeleteNodeFailed()) {
                LOGGER.info("Delete Node from ENM has failed");
                setDeleteNodeErrorMessage(operation);
                updateOperationState(operation, LifecycleOperationState.FAILED);
                vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
            } else {
                if (operation.getOperationState().equals(LifecycleOperationState.COMPLETED)) {
                    vnfInstance.setClusterName(null);
                    vnfInstance.setNamespace(null);
                }
            }
            return true;
        } else {
            LOGGER.info("Delete Node from ENM has not finished, scheduling task");
            DeleteNodeCheckTask deleteNodeCheckTask = applicationContext
                    .getBean(DeleteNodeCheckTask.class, operation, vnfInstance);
            Runnable decoratedTask = new TraceRunnableWrapper(deleteNodeCheckTask, tracing.currentTraceContext())
                    .parentContext(MDC.getCopyOfContextMap()).wrap();
            scheduledThreadPoolExecutor.schedule(decoratedTask, 10, TimeUnit.SECONDS);
            return false;
        }
    }

    private static void setDeleteNodeErrorMessage(final LifecycleOperation operation) {
        if (operation.isDeleteNodeFailed()) {
            final String deleteNodeErrorMessage = String
                    .format(ERROR_MESSAGE, operation.getDeleteNodeErrorMessage());
            appendError(deleteNodeErrorMessage, operation);
        }
    }
}
