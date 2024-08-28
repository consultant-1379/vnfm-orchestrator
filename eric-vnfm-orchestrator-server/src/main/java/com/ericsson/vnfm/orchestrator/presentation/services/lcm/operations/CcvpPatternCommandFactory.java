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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.DELETE_PVC;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.INSTALL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;

@Service
public class CcvpPatternCommandFactory {

    private final Map<String, Command> serviceCache = new HashMap<>();

    @Autowired
    private List<Command> services;

    @PostConstruct
    public void initServiceCache() {
        for (Command service : services) {
            serviceCache.put(service.getType().toString(), service);
        }
    }

    public Command getService(String type) {
        String operationType = getOperationType(type);
        Command service = serviceCache.get(operationType);
        if (service == null) {
            throw new NotFoundException("Unknown CCVP command type detected : " + type);
        }
        return service;
    }

    private static String getOperationType(String type) {
        if (StringUtils.containsIgnoreCase(type, DELETE_PVC)) {
            return CommandType.DELETE_PVC.name();
        } else if (DELETE.equalsIgnoreCase(type)) {
            return CommandType.TERMINATE.name();
        } else if (INSTALL.equalsIgnoreCase(type)) {
            return CommandType.INSTANTIATE.name();
        }
        return type;
    }
}
