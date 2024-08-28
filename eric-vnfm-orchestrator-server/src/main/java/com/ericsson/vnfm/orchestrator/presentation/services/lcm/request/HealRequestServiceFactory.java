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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.parameterPresent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.entity.HealRequestType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;

@Service
public class HealRequestServiceFactory {

    private final Map<String, HealRequestService> serviceCache = new HashMap<>();

    @Autowired
    private List<HealRequestService> services;

    @PostConstruct
    public void initServiceCache() {
        for (HealRequestService service : services) {
            serviceCache.put(service.getType().toString(), service);
        }
    }

    public HealRequestService getServiceByParams(Map additionalParams) {
        if (parameterPresent(additionalParams, RESTORE_BACKUP_NAME)) {
            return getService(HealRequestType.CNA.toString());
        }
        return getService(HealRequestType.CNF.toString());
    }

    private HealRequestService getService(String type) {
        HealRequestService service = serviceCache.get(type);
        if (service == null) {
            throw new NotFoundException("Unknown Heal request : " + type);
        }
        return service;
    }
}
