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

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;

@RestController
@Validated
@RequestMapping("/vnflcm/v1/operations")
public interface ResourceOperationsController {
    /**
     * Retrieve all vnf operations
     *
     * @return returns List<OperationDetails>
     */
    @GetMapping
    ResponseEntity<List<OperationDetails>> getAllOperations(@RequestHeader(value = "Accept") String accept);
}
