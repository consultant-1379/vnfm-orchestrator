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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.util.Map;
import java.util.Optional;

import org.slf4j.MDC;

import brave.propagation.CurrentTraceContext;

public final class TraceRunnableWrapper {
    private final Runnable runnable;
    private Map<String, String> parentContext;

    public TraceRunnableWrapper(Runnable runnable, CurrentTraceContext traceContext) {
        this.runnable = Optional.ofNullable(traceContext)
                .map(item -> item.wrap(runnable))
                .orElse(runnable);
    }

    public TraceRunnableWrapper parentContext(Map<String, String> parentContext) {
        this.parentContext = parentContext;
        return this;
    }

    public Runnable wrap() {
        clearOrSetContext(parentContext);
        return new TracedRunnable(runnable);
    }

    private static void clearOrSetContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }

    /**
     * Use this Runnable type to :
     * - forward trace context information (correlation-id, username etc)
     * - log trace context information (correlation-id, username etc)
     */
    private static final class TracedRunnable implements Runnable {
        private final Runnable runnable;

        private TracedRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        }

    }
}

