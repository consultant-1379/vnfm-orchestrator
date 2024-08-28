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

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends RuntimeException {
    private final String service;

    public ServiceUnavailableException(final String service, final String message, final Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public ServiceUnavailableException(final String service, final String message) {
        super(message);
        this.service = service;
    }
}
