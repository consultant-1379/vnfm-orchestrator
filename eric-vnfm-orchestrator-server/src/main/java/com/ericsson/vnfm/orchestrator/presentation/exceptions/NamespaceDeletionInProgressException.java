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

public class NamespaceDeletionInProgressException extends RuntimeException {

    private static final long serialVersionUID = 3696180690566978485L;

    public NamespaceDeletionInProgressException(final String message) {
        super(message);
    }
}
