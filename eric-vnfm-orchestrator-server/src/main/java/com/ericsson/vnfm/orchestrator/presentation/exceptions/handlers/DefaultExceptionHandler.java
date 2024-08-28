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
package com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ExceptionHandlersUtils.getInstanceUri;

import java.net.URI;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.ericsson.vnfm.orchestrator.model.ProblemDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@Order()
public class DefaultExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleAll(Throwable throwable, WebRequest request) {
        String simpleName = throwable.getClass().getSimpleName();
        LOGGER.error("{} occurred: {}", simpleName, throwable.getMessage(), throwable);
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle("Error occurred " + simpleName);
        problemDetails.setType(URI.create(TYPE_BLANK));
        problemDetails.setStatus(HttpStatus.BAD_REQUEST.value());
        problemDetails.setInstance(getInstanceUri(request));
        problemDetails.setDetail(throwable.getMessage());
        return new ResponseEntity<>(problemDetails, HttpStatus.BAD_REQUEST);
    }
}
