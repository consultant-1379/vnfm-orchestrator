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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DetermineUpgradePatternPresence extends MessageHandler<HelmReleaseLifecycleMessage> {

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        LOGGER.info("Determining upgrade pattern presence for operation: {}", operation.getOperationOccurrenceId());
        if (StringUtils.isNotBlank(operation.getUpgradePattern())) {
            LOGGER.info("Upgrade pattern for operation: {} present. Performing upgrade according to pattern",
                    operation.getOperationOccurrenceId());
            passToSuccessor(getAlternativeSuccessor(), context);
            return;
        }
        LOGGER.info("Upgrade pattern for operation: {} absent. Performing default upgrade",
                operation.getOperationOccurrenceId());
        passToSuccessor(getSuccessor(), context);
    }
}
