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
package com.ericsson.vnfm.orchestrator.presentation.controllers.internal.operations;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourceOperationsServiceImpl;

@RestController
public class ResourceOperationsControllerImpl implements ResourceOperationsController {

    @Autowired
    private ResourceOperationsServiceImpl allOperationsService;

    @Override
    public ResponseEntity<List<OperationDetails>> getAllOperations(@RequestHeader(value = "Accept") final String accept) {
        return new ResponseEntity<>(allOperationsService.getAllOperationDetails(), HttpStatus.OK);
    }
}
