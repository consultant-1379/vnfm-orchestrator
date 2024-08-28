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

public class InvalidFileException extends RuntimeException {
    private static final long serialVersionUID = -8229511314589850107L;

    public InvalidFileException(final String message) {
        super(message);
    }

    public InvalidFileException(final String message, final Exception e) {
        super(message, e);
    }
}
