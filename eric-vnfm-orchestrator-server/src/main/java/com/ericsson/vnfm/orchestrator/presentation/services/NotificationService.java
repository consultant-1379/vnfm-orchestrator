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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_OP_OCCS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.messaging.MessagingService;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeAddingNotification;
import com.ericsson.vnfm.orchestrator.model.notification.ENMNodeDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.NotificationBase;
import com.ericsson.vnfm.orchestrator.model.notification.NotificationLink;
import com.ericsson.vnfm.orchestrator.model.notification.OperationState;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierCreationNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierDeletionNotification;
import com.ericsson.vnfm.orchestrator.model.notification.VnfLcmOperationOccurrenceNotification;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;

@Service
public class NotificationService {

    private final MessagingService messagingService;

    @Value("${vnfm.host}")
    private String vnfmHost;

    @Autowired
    public NotificationService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void sendLifecycleOperationStateEvent(LifecycleOperation operation, String errorTitle, String errorDetails,
                                                 Integer errorCode) {
        VnfLcmOperationOccurrenceNotification notification = new VnfLcmOperationOccurrenceNotification(operation);
        if (errorDetails != null) {
            notification.withProblemDetails(errorTitle, errorDetails, errorCode, vnfmHost);
        }
        updateLccnLinks(notification);
        messagingService.sendMessage(notification);
    }

    public void sendVnfIdentifierCreationEvent(String vnfInstanceId) {
        final VnfIdentifierCreationNotification notification =
                new VnfIdentifierCreationNotification(vnfInstanceId, OperationState.COMPLETED);
        updateLccnLinks(notification);

        messagingService.sendMessage(notification);
    }

    public void sendVnfIdentifierDeletionEvent(String vnfInstanceId) {
        final VnfIdentifierDeletionNotification notification =
                new VnfIdentifierDeletionNotification(vnfInstanceId);
        updateLccnLinks(notification);

        messagingService.sendMessage(notification);
    }

    public void sendNodeEvent(String vnfInstanceId, OperationState operationState, EnmOperationEnum operation) {
        switch (operation) {
            case ADD_NODE:
                final ENMNodeAddingNotification enmNodeAddingNotification =
                        new ENMNodeAddingNotification(vnfInstanceId, operationState);
                updateLccnLinks(enmNodeAddingNotification);
                messagingService.sendMessage(enmNodeAddingNotification);
                break;
            case DELETE_NODE:
                final ENMNodeDeletionNotification enmNodeDeletionNotification =
                        new ENMNodeDeletionNotification(vnfInstanceId, operationState);
                updateLccnLinks(enmNodeDeletionNotification);
                messagingService.sendMessage(enmNodeDeletionNotification);
                break;
            default:
                break;
        }
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
