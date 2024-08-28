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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.presentation.services.ValidateNamespaceServiceImpl;

@RestController
public class ValidateNamespaceControllerImpl implements ValidateNamespaceController {

    @Autowired
    private ValidateNamespaceServiceImpl validateNamespaceService;

    @Override
    public ResponseEntity<Void> validateNamespace(
            @PathVariable String clusterName,
            @PathVariable String namespace) {
        validateNamespaceService.validateNamespace(namespace, clusterName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
