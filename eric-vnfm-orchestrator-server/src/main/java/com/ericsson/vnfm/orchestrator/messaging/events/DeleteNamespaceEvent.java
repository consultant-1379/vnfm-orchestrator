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
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.TERMINATE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractMessageProcessor;
import com.ericsson.vnfm.orchestrator.messaging.operations.DeleteNamespaceOperation;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

@Component
public class DeleteNamespaceEvent extends AbstractMessageProcessor {

    @Autowired
    private DeleteNamespaceOperation deleteNamespaceOperation;

    @Override
    public Conditions getConditions() {
        return new Conditions(WorkflowServiceEventType.DELETE_NAMESPACE.toString(), WorkflowServiceEventMessage.class);
    }

    @Override
    public void completed(final WorkflowServiceEventMessage message) {
        HelmReleaseLifecycleMessage helmMessage = convertEventToHelmMessage(message, TERMINATE);
        deleteNamespaceOperation.completed(helmMessage);
    }

    @Override
    public void failed(final WorkflowServiceEventMessage message) {
        HelmReleaseLifecycleMessage helmMessage = convertEventToHelmMessage(message, TERMINATE);
        deleteNamespaceOperation.failed(helmMessage);
    }
}
