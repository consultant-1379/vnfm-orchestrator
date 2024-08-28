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

public class HttpResponseParsingException extends RuntimeException {

    private static final long serialVersionUID = -7129351777235563581L;

    public HttpResponseParsingException(String message) {
        super(message);
    }

    public HttpResponseParsingException(String message, final Exception e) {
        super(message, e);
    }
}
