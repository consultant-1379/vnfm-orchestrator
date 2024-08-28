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
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DeleteVnfinstanceNamespaceDetails<T> extends MessageHandler<T> {

    private final DatabaseInteractionService databaseInteractionService;

    @Override
    public void handle(final MessageHandlingContext<T> context) {
        LOGGER.info("Removing namespace details");
        VnfInstance vnfInstance = context.getVnfInstance();
        String vnfInstanceId = vnfInstance.getVnfInstanceId();
        databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstanceId);
        passToSuccessor(getSuccessor(), context);
    }
}
