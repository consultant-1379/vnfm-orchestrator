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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.INVALID_PARAMETER_NAME_PROVIDED_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PACKAGE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOFTWARE_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.SOURCE_PACKAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.SOFTWARE_PACKAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNFD_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PRODUCT_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_SOFTWARE_VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceViewRepository;

@Service
public class AutoCompleteService {

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private VnfInstanceViewRepository vnfInstanceViewRepository;

    @Async
    public CompletableFuture<List<String>> getAutoCompleteResponse(String parameterName, String value, int pageNumber,
                                                                   int pageSize) {
        List<String> values;
        Pageable page;
        if (value != null) {
            switch (parameterName) {
                case TYPE:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by(VNF_PRODUCT_NAME).ascending());
                    values =  vnfInstanceRepository.findDistinctVnfProductName(value, page);
                    break;
                case SOFTWARE_VERSION:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by(VNF_SOFTWARE_VERSION).ascending());
                    values = vnfInstanceRepository.findDistinctVnfSoftwareVersion(value, page);
                    break;
                case PACKAGE_VERSION:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by(VNFD_VERSION).ascending());
                    values = vnfInstanceRepository.findDistinctVnfdVersion(value, page);
                    break;
                case CLUSTER:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by(CLUSTER_NAME).ascending());
                    values = vnfInstanceRepository.findDistinctClusterName(value, page);
                    break;
                case SOURCE_PACKAGE:
                    page = PageRequest.of(pageNumber, pageSize, Sort.by(SOFTWARE_PACKAGE).ascending());
                    values = vnfInstanceViewRepository.findDistinctSoftwarePackage(value, page);
                    break;
                default:
                    throw new InvalidInputException(String.format(INVALID_PARAMETER_NAME_PROVIDED_ERROR_MESSAGE,
                            parameterName));
            }
        } else {
            values = new ArrayList<>();
        }
        return CompletableFuture.completedFuture(values);
    }
}
