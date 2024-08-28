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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

@Component
public class LifeCycleRequestFactory {

    private final Map<LifecycleOperationType, OperationRequestHandler> serviceCache;

    public LifeCycleRequestFactory(List<OperationRequestHandler> services) {
        serviceCache = services.stream()
                .collect(Collectors.toMap(OperationRequestHandler::getType, Function.identity()));
    }

    public OperationRequestHandler getService(LifecycleOperationType type) {
        return Optional.ofNullable(serviceCache.get(type))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown lifecycle request: %s", type)));
    }
}
