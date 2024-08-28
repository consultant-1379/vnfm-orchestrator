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
package com.ericsson.vnfm.orchestrator.presentation.controllers.validatenamespace;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/vnflcm/v1/validateNamespace/{clusterName}/{namespace}")
public interface ValidateNamespaceController {

    /**
     * Validates namespace field in UI
     *
     * Will not not return anything if namespace is valid
     *
     * Will throw error with message otherwise
     */
    @GetMapping
    ResponseEntity<Void> validateNamespace(@PathVariable String clusterName, @PathVariable String namespace);
}


