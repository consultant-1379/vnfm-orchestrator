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

@Getter
public class HelmVersionsException extends RuntimeException {

    private final transient HttpStatus status;

    public HelmVersionsException(final String message, Throwable t, HttpStatus status) {
        super(message, t);
        this.status = status;
    }

    public HelmVersionsException(final String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
