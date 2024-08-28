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
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
class TransformingRunnable implements Runnable {

    private final BlockingQueue<FieldsTransformingTask> incoming;
    private final BlockingQueue<FieldsTransformingTask> outgoing;
    private final Consumer<FieldsTransformingTask> transformer;

    @SuppressWarnings("java:S2189")
    @Override
    public void run() {
        do {
            try {
                FieldsTransformingTask task = incoming.take();
                processTask(task);
                outgoing.put(task);
            } catch (InterruptedException e) {
                LOGGER.warn("Transforming worker thread interrupted, exiting.");
                Thread.currentThread().interrupt();
                return;
            }
        } while (true);
    }

    private void processTask(FieldsTransformingTask task) {
        try {
            transformer.accept(task);
            task.setSucceeded(true);
        } catch (Exception e) {
            LOGGER.error("Exception thrown from data transformer", e);
            task.setSucceeded(false);
            task.setFailureCause(e);
        }
    }
}
