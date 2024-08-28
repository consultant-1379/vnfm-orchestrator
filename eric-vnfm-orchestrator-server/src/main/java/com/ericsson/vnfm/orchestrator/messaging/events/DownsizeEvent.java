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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractMessageProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageConverter;
import com.ericsson.vnfm.orchestrator.messaging.operations.DownsizeOperation;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;


@Component
public class DownsizeEvent extends AbstractMessageProcessor {
    @Autowired
    DownsizeOperation downsizeOperation;

    @Override
    public Conditions getConditions() {
        return new Conditions(WorkflowServiceEventType.DOWNSIZE.toString(), WorkflowServiceEventMessage.class);
    }

    @Override
    public void completed(WorkflowServiceEventMessage message) {
        HelmReleaseLifecycleMessage helmMessage = MessageConverter.convertEventToHelmMessage(message, HelmReleaseOperationType.CHANGE_VNFPKG);
        downsizeOperation.completed(helmMessage);
    }

    @Override
    public void failed(WorkflowServiceEventMessage message) {
        HelmReleaseLifecycleMessage helmMessage = MessageConverter.convertEventToHelmMessage(message, HelmReleaseOperationType.CHANGE_VNFPKG);
        downsizeOperation.failed(helmMessage);
    }
}
