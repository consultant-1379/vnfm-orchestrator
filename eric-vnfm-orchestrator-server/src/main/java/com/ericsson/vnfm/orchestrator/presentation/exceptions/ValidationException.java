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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

    private final String title;
    private final HttpStatus responseHttpStatus;

    public ValidationException(String exceptionMessage, String title, HttpStatus responseHttpStatus) {
        super(exceptionMessage);
        this.title = title;
        this.responseHttpStatus = responseHttpStatus;
    }

    public ValidationException(String exceptionMessage, String title, HttpStatus responseHttpStatus, final Throwable cause) {
        super(exceptionMessage, cause);
        this.title = title;
        this.responseHttpStatus = responseHttpStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("title", title)
                .append("responseHttpStatus", responseHttpStatus)
                .toString();
    }
}
