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
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HelmReleaseRoutingStrategy implements MessageRoutingStrategy<HelmReleaseLifecycleMessage> {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MessageOperationFactory messageOperationFactory;

    @Override
    public Class<HelmReleaseLifecycleMessage> getMessageType() {
        return HelmReleaseLifecycleMessage.class;
    }

    @Override
    public void routeByMessage(final HelmReleaseLifecycleMessage message) {
        LifecycleOperation operation = databaseInteractionService
                .getLifecycleOperationPartial(message.getLifecycleOperationId());
        Conditions conditions = RoutingUtility.getConditions(message, operation);
        routeToMessageProcessor(message, conditions);
    }

    @Override
    public void routeByStatus(final MessageProcessor<HelmReleaseLifecycleMessage> messageProcessor,
                              final HelmReleaseLifecycleMessage message) {
        String lifecycleOperationId = message.getLifecycleOperationId();
        LifecycleOperation operation = databaseInteractionService.getLifecycleOperationPartial(lifecycleOperationId);
        if (isLifecycleOperationIdNull(lifecycleOperationId) || isAlreadyFinished(operation)) {
            return;
        }
        HelmReleaseState helmState = message.getState();
        switch (helmState) {
            case COMPLETED:
                messageProcessor.completed(message);
                break;
            case FAILED:
            case PROCESSING:
                messageProcessor.failed(message);
                break;
            default:
                LOGGER.warn("State '{}' not found", helmState);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void routeToMessageProcessor(HelmReleaseLifecycleMessage message, Conditions conditions) {
        MessageProcessor<HelmReleaseLifecycleMessage> messageProcessor =
                (MessageProcessor<HelmReleaseLifecycleMessage>) messageOperationFactory.getProcessor(conditions);
        if (isMessageProcessorNotNull(messageProcessor, conditions)) {
            routeByStatus(messageProcessor, message);
        }
    }
}
