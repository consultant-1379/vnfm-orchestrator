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

public class LifecycleInProgressException extends RuntimeException {
    private static final long serialVersionUID = -8723755491480615520L;

    public LifecycleInProgressException(String message) {
        super(message);
    }

    public LifecycleInProgressException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
