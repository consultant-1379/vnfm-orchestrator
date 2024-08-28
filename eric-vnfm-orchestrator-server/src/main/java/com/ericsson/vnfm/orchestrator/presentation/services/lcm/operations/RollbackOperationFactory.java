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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;

@Service
public class RollbackOperationFactory {

    private final Map<String, RollbackOperation> serviceCache = new HashMap<>();

    @Autowired
    private List<RollbackOperation> services;

    @PostConstruct
    public void initServiceCache() {
        for (RollbackOperation service : services) {
            serviceCache.put(service.getType().toString(), service);
        }
    }

    public RollbackOperation getServiceByPattern(List<MutablePair<String, String>> helmChartCommandPairs) {
        if (CollectionUtils.isEmpty(helmChartCommandPairs)) {
            return getService(RollbackType.AUTO_ROLLBACK.toString());
        }
        return getService(RollbackType.MANUAL_ROLLBACK.toString());
    }

    private RollbackOperation getService(String type) {
        RollbackOperation service = serviceCache.get(type);
        if (service == null) {
            throw new NotFoundException("Unknown Rollback type detected : " + type);
        }
        return service;
    }
}
