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
package com.ericsson.vnfm.orchestrator.messaging.handlers;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FailOperationByTimeout extends MessageHandler<HelmReleaseLifecycleMessage> {

    private MessageUtility utility;

    private InstantiationState instantiationState;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        LOGGER.info("Failing operation due to timeout");
        utility.lifecycleTimedOut(message.getLifecycleOperationId(), instantiationState, message.getMessage());
        passToSuccessor(getSuccessor(), context);
    }
}
