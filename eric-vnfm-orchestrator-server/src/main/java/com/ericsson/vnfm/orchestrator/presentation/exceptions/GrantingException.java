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

public class GrantingException extends RuntimeException {

    public GrantingException() {
        super("NFVO reject a Granting request based on policies and available capacity");
    }

    public GrantingException(final String message) {
        super(message);
    }

    public GrantingException(Throwable e) {
        super(e);
    }

    public GrantingException(Throwable e, String message) {
        super(message, e);
    }
}
