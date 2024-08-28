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
package com.ericsson.vnfm.orchestrator.model.notification;

public enum NotificationType {
    VNF_IDENTIFIER_CREATION_NOTIFICATION("VnfIdentifierCreationNotification"),
    VNF_IDENTIFIER_DELETION_NOTIFICATION("VnfIdentifierDeletionNotification"),
    VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION("VnfLcmOperationOccurrenceNotification"),
    VNF_ENM_NODE_ADDING_NOTIFICATION("VnfEnmNodeAddingNotification"),
    VNF_ENM_NODE_DELETION_NOTIFICATION("VnfEnmNodeDeletionNotification");

    private final String notification;

    NotificationType(String notification) {
        this.notification = notification;
    }

    @Override
    public String toString() {
        return this.notification;
    }
}
