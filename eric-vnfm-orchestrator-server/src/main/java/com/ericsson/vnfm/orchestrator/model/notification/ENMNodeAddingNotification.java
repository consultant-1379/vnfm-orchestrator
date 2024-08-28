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

import com.fasterxml.jackson.annotation.JsonProperty;

public class ENMNodeAddingNotification extends NotificationBase {

    @JsonProperty("operationState")
    private OperationState operationState;

    public ENMNodeAddingNotification(String vnfInstanceId, OperationState operationState) {
        super(vnfInstanceId, NotificationType.VNF_ENM_NODE_ADDING_NOTIFICATION);
        this.operationState = operationState;
    }

    public OperationState getOperationState() {
        return operationState;
    }

    public void setOperationState(final OperationState operationState) {
        this.operationState = operationState;
    }
}