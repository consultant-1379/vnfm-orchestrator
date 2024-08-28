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
package com.ericsson.vnfm.orchestrator.aspect;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;

import java.util.Arrays;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotificationAspectException;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class NotificationAspect {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private NotificationService notificationService;

    @Around("com.ericsson.vnfm.orchestrator.aspect.PointcutConfig.persistOperationExecution()")
    public Object around(final ProceedingJoinPoint pjp) {
        // Variable indicating whether a notification will be sent
        boolean isNotificationSendingExpected = false;

        final Optional<LifecycleOperation> currentOperation = getFirstObjectByClass(pjp.getArgs(), LifecycleOperation.class);

        // LifecycleOperation is one of the parameters of the methods on which the annotation is added. If this parameter is absent or == null the
        // notification cannot be sent
        if (currentOperation.isEmpty()) {
            LOGGER.error("LifecycleOperation parameter not found notification cannot be sent");
            return startProceedingJoinPoint(pjp, null);
        }

        final String operationOccurrenceId = currentOperation.get().getOperationOccurrenceId();

        // If the operationOccurrenceId == null, it indicates that the operation has just been created, in which case a notification should be sent
        if (operationOccurrenceId == null) {
            LOGGER.info("Sending notification of operation {} create and in state {}",
                         currentOperation.get().getLifecycleOperationType(),
                         currentOperation.get().getOperationState());
            isNotificationSendingExpected = true;
        } else {
            final LifecycleOperation previousOperationCondition =
                    databaseInteractionService.getCommittedLifecycleOperation(operationOccurrenceId);
            // If the operation status has changed, it is necessary to send a notification
            if (hasOperationStateChanged(currentOperation.get(), previousOperationCondition)) {
                isNotificationSendingExpected = true;
                LOGGER.info("Sending notification of operation {} changing state from {} to {}",
                             currentOperation.get().getLifecycleOperationType(),
                             previousOperationCondition.getOperationState(),
                             currentOperation.get().getOperationState());
            }
        }

        final Object proceedObject = startProceedingJoinPoint(pjp, currentOperation.get());

        if (isNotificationSendingExpected) {
            notificationService.sendLifecycleOperationStateEvent(currentOperation.get(),
                                                                 null,
                                                                 currentOperation.get().getError(),
                                                                 INTERNAL_SERVER_ERROR.value());

            databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(currentOperation.get().getVnfInstance().getVnfInstanceId(),
                                                                           SEND_NOTIFICATION);
        }

        return proceedObject;
    }

    private Object startProceedingJoinPoint(final ProceedingJoinPoint pjp, final LifecycleOperation operation) {
        try {
            return pjp.proceed();
        } catch (Throwable throwable) {
            String message;
            if (operation == null) {
                message = String.format("NotificationAspect Failed: Unable to proceeding join point, due to %s", throwable.getMessage());
            } else {
                message = String.format("NotificationAspect Failed: Unable to proceeding join point, operation %s, due to %s",
                                        operation.getOperationOccurrenceId(), throwable.getMessage());
            }

            throw new NotificationAspectException(message, throwable);
        }
    }

    private boolean hasOperationStateChanged(final LifecycleOperation currentOperation, final LifecycleOperation previousOperationCondition) {
        return previousOperationCondition != null && previousOperationCondition.getOperationState() != null
                && !previousOperationCondition.getOperationState()
                .equals(currentOperation.getOperationState());
    }

    private <T> Optional<T> getFirstObjectByClass(final Object[] args, final Class<T> cls) {
        return Arrays.stream(args).filter(arg -> arg != null && cls == arg.getClass()).map(cls::cast).findFirst();
    }
}
