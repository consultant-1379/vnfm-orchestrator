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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TerminateLcmOpErrorProcessor extends DefaultLcmOpErrorProcessor {
    @Override
    public void process(final LifecycleOperation operation, final HttpStatus status, final String errorTitle, final String errorDetail) {
        super.process(operation, status, errorTitle, errorDetail);
        LOGGER.info("Started releasing namespace deletion");
        databaseInteractionService.releaseNamespaceDeletion(operation);
    }

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.TERMINATE;
    }
}
