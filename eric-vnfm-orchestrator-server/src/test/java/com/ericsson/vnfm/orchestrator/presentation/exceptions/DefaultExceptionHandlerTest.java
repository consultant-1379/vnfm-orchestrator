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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.org.yaml.snakeyaml.error.YAMLException;

import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.DefaultExceptionHandler;

public class DefaultExceptionHandlerTest {

    DefaultExceptionHandler defaultExceptionHandler = new DefaultExceptionHandler();

    @Test
    public void handleAllTest() {
        String error = "This is a test";
        try {
            throw new YAMLException(error);
        } catch(Exception e) {
            ResponseEntity<ProblemDetails> objectResponseEntity = defaultExceptionHandler.handleAll(e, null);
            String title = "Error occurred YAMLException";
            assertThat(objectResponseEntity.getBody().getTitle()).isEqualTo(title);
            assertThat(objectResponseEntity.getBody().getDetail()).isEqualTo(error);
        }
    }

}