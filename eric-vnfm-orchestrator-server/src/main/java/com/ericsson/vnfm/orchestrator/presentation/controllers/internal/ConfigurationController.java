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
package com.ericsson.vnfm.orchestrator.presentation.controllers.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.api.ConfigurationsApi;
import com.ericsson.vnfm.orchestrator.model.EvnfmProductConfiguration;
import com.ericsson.vnfm.orchestrator.presentation.services.configurations.ConfigurationService;

@RestController
@Validated
@RequestMapping("/info/v1/")
public class ConfigurationController implements ConfigurationsApi {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public ResponseEntity<EvnfmProductConfiguration> configurationsGet() {
        EvnfmProductConfiguration configuration = configurationService.getConfiguration();
        return new ResponseEntity<>(configuration, HttpStatus.OK);
    }

}