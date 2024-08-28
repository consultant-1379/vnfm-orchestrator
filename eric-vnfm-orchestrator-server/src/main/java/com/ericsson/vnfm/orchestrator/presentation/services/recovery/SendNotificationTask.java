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
package com.ericsson.vnfm.orchestrator.presentation.services.recovery;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_ADDING_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_ENM_NODE_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_CREATION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_IDENTIFIER_DELETION_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Notification.VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_OP_OCCS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.NOTIFICATION_TYPE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeAddingNotification;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.NotificationBase;
import com.ericsson.vnfm.orchestrator.model.notification.NotificationLink;
import com.ericsson.vnfm.orchestrator.model.notification.OperationState;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierCreationNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfLcmOperationOccurrenceNotification;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendNotificationTask extends TaskProcessor {

    private String vnfmHost;

    private MessagingService messagingService;

    private DatabaseInteractionService databaseInteractionService;

    private Task task;

    protected SendNotificationTask(final Optional<TaskProcessor> nextProcessor, Task task,
                                   String vnfmHost, MessagingService messagingService,
                                   DatabaseInteractionService databaseInteractionService) {
        super(nextProcessor);
        this.task = task;
        this.vnfmHost = vnfmHost;
        this.messagingService = messagingService;
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    public void execute() {
        final Map<String, Object> additionalParams = parseJsonToGenericType(task.getAdditionalParams(), new TypeReference<>() { });
        final String notificationType = String.valueOf(additionalParams.get(NOTIFICATION_TYPE));

        try {
            final String vnfInstanceId = task.getVnfInstanceId();
            final NotificationBase notification = switch (notificationType) {
                case VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION -> createOperationNotification(vnfInstanceId);
                case VNF_IDENTIFIER_CREATION_NOTIFICATION -> createIdentifierCreationNotification(vnfInstanceId);
                case VNF_IDENTIFIER_DELETION_NOTIFICATION -> createIdentifierDeletionNotification(vnfInstanceId);
                case VNF_ENM_NODE_ADDING_NOTIFICATION -> createEnmNodeAddingNotification(vnfInstanceId);
                case VNF_ENM_NODE_DELETION_NOTIFICATION -> createEnmNodeDeletionNotification(vnfInstanceId);
                default -> throw new InternalRuntimeException("Unknown notification type " + notificationType);
            };

            messagingService.sendMessage(notification);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Send Notification Task", e);
        } finally {
            databaseInteractionService.deleteTask(task);
            nextProcessor.ifPresent(TaskProcessor::execute);
        }
    }

    private VnfIdentifierCreationNotification createIdentifierCreationNotification(final String vnfInstanceId) {
        final VnfIdentifierCreationNotification notification =
                new VnfIdentifierCreationNotification(vnfInstanceId, OperationState.FAILED);
        updateLccnLinks(notification);
        return notification;
    }

    private VnfIdentifierDeletionNotification createIdentifierDeletionNotification(final String vnfInstanceId) {
        final VnfIdentifierDeletionNotification vnfIdentifierDeletionNotification =
                new VnfIdentifierDeletionNotification(vnfInstanceId);
        updateLccnLinks(vnfIdentifierDeletionNotification);
        return vnfIdentifierDeletionNotification;
    }

    private ENMNodeAddingNotification createEnmNodeAddingNotification(final String vnfInstanceId) {
        final ENMNodeAddingNotification enmNodeAddingNotification =
                new ENMNodeAddingNotification(vnfInstanceId, OperationState.FAILED);
        updateLccnLinks(enmNodeAddingNotification);
        return enmNodeAddingNotification;
    }

    private ENMNodeDeletionNotification createEnmNodeDeletionNotification(final String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        OperationState operationState = BooleanUtils.isFalse(vnfInstance.isAddedToOss()) ?
                OperationState.COMPLETED : OperationState.FAILED;

        final ENMNodeDeletionNotification enmNodeDeletionNotification =
                new ENMNodeDeletionNotification(vnfInstanceId, operationState);
        updateLccnLinks(enmNodeDeletionNotification);
        return enmNodeDeletionNotification;
    }

    private VnfLcmOperationOccurrenceNotification createOperationNotification(final String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        LifecycleOperation operation = databaseInteractionService.getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
        VnfLcmOperationOccurrenceNotification operationOccurrenceNotification = new VnfLcmOperationOccurrenceNotification(operation);
        if (operation.getError() != null) {
            operationOccurrenceNotification.withProblemDetails(null, operation.getError(), INTERNAL_SERVER_ERROR.value(), vnfmHost);
        }
        updateLccnLinks(operationOccurrenceNotification);
        return operationOccurrenceNotification;
    }

    private void updateLccnLinks(NotificationBase notification) {
        URI vnfInstanceURI = URI.create("https://" + vnfmHost + LCM_VNF_INSTANCES + notification.getVnfInstanceId());
        notification.getLinks().setVnfInstance(new NotificationLink(vnfInstanceURI));
    }

    private void updateLccnLinks(VnfLcmOperationOccurrenceNotification notification) {
        updateLccnLinks((NotificationBase) notification);
        URI vnfLcmOpOccURI = URI.create("https://" + vnfmHost + LCM_OP_OCCS + notification.getVnfLcmOpOccId());
        notification.getLinks().setVnfLcmOpOcc(new NotificationLink(vnfLcmOpOccURI));
    }
}
