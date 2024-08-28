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
package com.ericsson.vnfm.orchestrator.presentation.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public class SyncFailedException extends RuntimeException {
    @Getter
    final HttpStatus status;

    public SyncFailedException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

    public SyncFailedException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
