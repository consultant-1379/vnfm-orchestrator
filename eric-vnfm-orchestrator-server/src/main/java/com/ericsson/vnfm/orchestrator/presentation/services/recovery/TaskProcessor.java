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
package com.ericsson.vnfm.orchestrator.presentation.services.recovery;

import java.util.Optional;

public abstract class TaskProcessor {
    Optional<TaskProcessor> nextProcessor;

    protected TaskProcessor(final Optional<TaskProcessor> nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public abstract void execute();
}
