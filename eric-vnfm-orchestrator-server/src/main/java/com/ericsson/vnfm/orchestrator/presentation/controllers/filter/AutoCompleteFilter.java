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
package com.ericsson.vnfm.orchestrator.presentation.controllers.filter;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PACKAGE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_NUMBER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOFTWARE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOURCE_PACKAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.ericsson.vnfm.orchestrator.model.AutoCompleteResponse;

@RequestMapping("/vnflcm/api/v1/instance/filter/autocomplete")
public interface AutoCompleteFilter {

    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<AutoCompleteResponse> getAutoCompleteValue(
            @RequestParam(value = TYPE, required = false) String type,
            @RequestParam(value = SOFTWARE_VERSION, required = false) String softwareVersion,
            @RequestParam(value = PACKAGE_VERSION, required = false) String packageVersion,
            @RequestParam(value = SOURCE_PACKAGE, required = false) String sourcePackage,
            @RequestParam(value = CLUSTER, required = false) String cluster,
            @RequestParam(value = PAGE_NUMBER, required = false) String pageNumber,
            @RequestParam(value = PAGE_SIZE, required = false) String pageSize);
}
