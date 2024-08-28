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
package com.ericsson.vnfm.orchestrator.messaging.routing;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isAlreadyFinished;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isLifecycleOperationIdNull;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isMessageProcessorNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessageProcessor;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowServiceEventRoutingStrategy implements MessageRoutingStrategy<WorkflowServiceEventMessage> {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MessageOperationFactory messageOperationFactory;

    @Override
    public Class<WorkflowServiceEventMessage> getMessageType() {
        return WorkflowServiceEventMessage.class;
    }

    @Override
    public void routeByMessage(final WorkflowServiceEventMessage message) {
        Conditions conditions = new Conditions(message.getType().toString(), message.getClass());
        routeToMessageProcessor(message, conditions);
    }

    @Override
    public void routeByStatus(final MessageProcessor<WorkflowServiceEventMessage> messageProcessor,
                              final WorkflowServiceEventMessage message) {
        String lifecycleOperationId = message.getLifecycleOperationId();
        LifecycleOperation operation = databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
        if (isLifecycleOperationIdNull(lifecycleOperationId) || isAlreadyFinished(operation)) {
            return;
        }
        if (messageProcessor != null) {
            final WorkflowServiceEventStatus status = message.getStatus();
            switch (status) {
                case COMPLETED:
                    messageProcessor.completed(message);
                    break;
                case FAILED:
                    messageProcessor.failed(message);
                    break;
                default:
                    LOGGER.warn("Status '{}' not found", status);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void routeToMessageProcessor(WorkflowServiceEventMessage message, Conditions conditions) {
        MessageProcessor<WorkflowServiceEventMessage> messageProcessor =
                (MessageProcessor<WorkflowServiceEventMessage>) messageOperationFactory.getProcessor(conditions);
        if (isMessageProcessorNotNull(messageProcessor, conditions)) {
            routeByStatus(messageProcessor, message);
        }
    }
}
