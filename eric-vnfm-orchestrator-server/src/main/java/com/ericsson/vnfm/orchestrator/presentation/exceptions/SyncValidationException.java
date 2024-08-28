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

import static java.util.Collections.singletonList;

import java.util.List;

import lombok.Getter;

@Getter
public class SyncValidationException extends RuntimeException {

    private final List<String> errors;

    public SyncValidationException(final String error) {
        this(singletonList(error));
    }

    public SyncValidationException(final String error, final Throwable cause) {
        super(cause);
        this.errors = singletonList(error);
    }

    public SyncValidationException(final List<String> errors) {
        this.errors = errors;
    }
}
