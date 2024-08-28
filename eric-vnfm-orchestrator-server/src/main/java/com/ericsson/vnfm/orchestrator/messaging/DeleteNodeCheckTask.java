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
package com.ericsson.vnfm.orchestrator.messaging;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.TraceRunnableWrapper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteNodeCheckTask implements Runnable {

    private static final String ERROR_MESSAGE = "Delete Node from ENM failure, please delete the Node from ENM "
            + "manually:\"%s\"";

    @Autowired
    private ScheduledThreadPoolExecutor taskExecutor;

    @Autowired
    private Tracing tracing;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MessageUtility utility;

    private LifecycleOperation operation;

    private VnfInstance vnfInstance;

    public DeleteNodeCheckTask(final LifecycleOperation operation, final VnfInstance vnfInstance) {
        this.operation = operation;
        this.vnfInstance = vnfInstance;
    }

    @Override
    public void run() {
        LifecycleOperation latestOperation = databaseInteractionService.getLifecycleOperation(
                operation.getOperationOccurrenceId());

        if (latestOperation.isDeleteNodeFinished()) {
            LOGGER.info("Delete Node from ENM has finished");
            if (latestOperation.isDeleteNodeFailed()) {
                LOGGER.info("Delete Node from ENM has failed");
                final String deleteNodeErrorMessage = String.format(ERROR_MESSAGE,
                        latestOperation.getDeleteNodeErrorMessage());
                latestOperation.setOperationState(LifecycleOperationState.FAILED);
                setError(operation.getError(), latestOperation);
                appendError(deleteNodeErrorMessage, latestOperation);
            } else {
                setError(operation.getError(), latestOperation);
                setLatestOperationState(latestOperation);
            }
            latestOperation.setStateEnteredTime(LocalDateTime.now());
            databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, latestOperation);
            if (latestOperation
                    .getOperationState()
                    .equals(LifecycleOperationState.COMPLETED)) {
                vnfInstance.setNamespace(null);
                vnfInstance.setClusterName(null);
                databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstance.getVnfInstanceId());
                databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
                utility.deleteIdentifier(vnfInstance, operation);
            }
        } else {
            scheduleTask();
        }
    }

    private void setLatestOperationState(final LifecycleOperation latestOperation) {
        if (operationHasNotTimedOut(latestOperation)) {
            latestOperation.setOperationState(operation.getOperationState());
        }
    }

    private static boolean operationHasNotTimedOut(final LifecycleOperation latestOperation) {
        return !latestOperation
                .getOperationState()
                .equals(LifecycleOperationState.FAILED);
    }

    private void scheduleTask() {
        Runnable decorateTask = new TraceRunnableWrapper(this, tracing.currentTraceContext())
                .parentContext(MDC.getCopyOfContextMap())
                .wrap();
        taskExecutor.schedule(decorateTask, 10, TimeUnit.SECONDS);
    }
}
