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

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageConverter {

    public static HelmReleaseLifecycleMessage convertEventToHelmMessage(WorkflowServiceEventMessage event,
                                                                        HelmReleaseOperationType helmReleaseOperationType) {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName(event.getReleaseName());
        message.setMessage(event.getMessage());
        message.setLifecycleOperationId(event.getLifecycleOperationId());
        message.setOperationType(helmReleaseOperationType);
        message.setRevisionNumber(null);
        if (event.getStatus().toString().equals(LifecycleOperationState.COMPLETED.toString())) {
            message.setState(HelmReleaseState.COMPLETED);
        } else {
            message.setState(HelmReleaseState.FAILED);
        }

        return message;
    }
}
