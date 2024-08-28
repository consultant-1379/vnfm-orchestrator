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
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.NUMBER_EXPRESSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PACKAGE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_NUMBER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOFTWARE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOURCE_PACKAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.model.AutoCompleteResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.AutoCompleteService;

@RestController
public class AutoCompleteFilterImpl implements AutoCompleteFilter {

    @Autowired
    private AutoCompleteService autoCompleteService;

    @Override
    @SuppressWarnings("squid:S1067")
    public ResponseEntity<AutoCompleteResponse> getAutoCompleteValue(
            @RequestParam(value = TYPE, required = false) String type,
            @RequestParam(value = SOFTWARE_VERSION, required = false) String softwareVersion,
            @RequestParam(value = PACKAGE_VERSION, required = false) String packageVersion,
            @RequestParam(value = SOURCE_PACKAGE, required = false) String sourcePackage,
            @RequestParam(value = CLUSTER, required = false) String cluster,
            @RequestParam(value = PAGE_NUMBER, required = false, defaultValue = "0") String pageNumber,
            @RequestParam(value = PAGE_SIZE, required = false, defaultValue = "5") String pageSize) {
        int pgSize = matchInt(pageSize, PAGE_SIZE);
        int pgNumber = matchInt(pageNumber, PAGE_NUMBER);

        CompletableFuture<List<String>> allType;
        CompletableFuture<List<String>> allSoftwareVersion;
        CompletableFuture<List<String>> allPackageVersion;
        CompletableFuture<List<String>> allSourcePackage;
        CompletableFuture<List<String>> allCluster;
        if (type == null && softwareVersion == null && packageVersion == null && sourcePackage == null &&
                cluster == null) {
            allType = autoCompleteService.getAutoCompleteResponse(TYPE, "", pgNumber, pgSize);
            allSoftwareVersion = autoCompleteService.getAutoCompleteResponse(SOFTWARE_VERSION, "", pgNumber, pgSize);
            allPackageVersion = autoCompleteService.getAutoCompleteResponse(PACKAGE_VERSION, "", pgNumber, pgSize);
            allSourcePackage = autoCompleteService.getAutoCompleteResponse(SOURCE_PACKAGE, "", pgNumber, pgSize);
            allCluster = autoCompleteService.getAutoCompleteResponse(CLUSTER, "", pgNumber, pgSize);
        } else {
            allType = autoCompleteService.getAutoCompleteResponse(TYPE, type, pgNumber, pgSize);
            allSoftwareVersion = autoCompleteService.getAutoCompleteResponse(SOFTWARE_VERSION, softwareVersion, pgNumber, pgSize);
            allPackageVersion = autoCompleteService.getAutoCompleteResponse(PACKAGE_VERSION, packageVersion, pgNumber, pgSize);
            allSourcePackage = autoCompleteService.getAutoCompleteResponse(SOURCE_PACKAGE, sourcePackage, pgNumber, pgSize);
            allCluster = autoCompleteService.getAutoCompleteResponse(CLUSTER, cluster, pgNumber, pgSize);
        }
        CompletableFuture.allOf(allType, allSoftwareVersion, allPackageVersion, allSourcePackage, allCluster).join();
        AutoCompleteResponse autoCompleteResponse = new AutoCompleteResponse();
        try {
            autoCompleteResponse.setType(allType.get());
            autoCompleteResponse.setPackageVersion(allPackageVersion.get());
            autoCompleteResponse.setSoftwareVersion(allSoftwareVersion.get());
            autoCompleteResponse.setSourcePackage(allSourcePackage.get());
            autoCompleteResponse.setCluster(allCluster.get());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new InternalRuntimeException(ie);
        } catch (Exception ex) {
            throw new InternalRuntimeException(ex);
        }

        return new ResponseEntity<>(autoCompleteResponse, HttpStatus.OK);
    }

    private int matchInt(String value, String paramName) {
        if (NUMBER_EXPRESSION.matcher(value).find()) {
            return Integer.parseInt(value);
        } else {
            throw new InvalidInputException(String.format("%s only supports number value", paramName));
        }
    }
}
