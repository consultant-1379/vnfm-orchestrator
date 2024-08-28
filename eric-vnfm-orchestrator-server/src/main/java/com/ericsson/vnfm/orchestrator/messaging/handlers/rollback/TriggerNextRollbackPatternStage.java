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
package com.ericsson.vnfm.orchestrator.messaging.handlers.rollback;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.RollbackOperation;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TriggerNextRollbackPatternStage extends MessageHandler<HelmReleaseLifecycleMessage> {

    private RollbackOperation rollbackOperation;

    @Override
    public void handle(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Triggering next rollback pattern stage");
        rollbackOperation.triggerNextStage(context);
        passToSuccessor(getSuccessor(), context);
    }
}
