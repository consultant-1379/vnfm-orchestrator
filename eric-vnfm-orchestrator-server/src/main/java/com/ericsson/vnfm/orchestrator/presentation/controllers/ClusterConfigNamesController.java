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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.api.ClusterConfigNamesApi;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/vnflcm/v1")
@AllArgsConstructor
public class ClusterConfigNamesController implements ClusterConfigNamesApi {

    @Autowired
    private final ClusterConfigService configService;

    @Override
    public ResponseEntity<List<String>> getAllClusterConfigNames() {
        List<String> names = configService.getAllClusterConfigNames();

        return new ResponseEntity<>(names, HttpStatus.OK);
    }
}
