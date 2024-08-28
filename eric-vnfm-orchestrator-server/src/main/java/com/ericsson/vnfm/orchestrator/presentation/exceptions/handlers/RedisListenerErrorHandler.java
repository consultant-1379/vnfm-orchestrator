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
package com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.RedisListenerExceptionEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisListenerErrorHandler implements ErrorHandler, Lifecycle {

    private boolean isRunning;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void handleError(final Throwable throwable) {
        if (isRunning) {
            LOGGER.error("An error occurred in stream listener, error message: {}", throwable.getMessage());
            eventPublisher.publishEvent(new RedisListenerExceptionEvent(throwable.getMessage()));
        }
    }

    @Override
    public void start() {
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
