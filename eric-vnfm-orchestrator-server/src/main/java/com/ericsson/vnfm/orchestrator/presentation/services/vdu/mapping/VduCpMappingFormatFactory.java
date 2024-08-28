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
package com.ericsson.vnfm.orchestrator.presentation.services.vdu.mapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.VduCpMappingFormat;

@Component
public class VduCpMappingFormatFactory {

    private final Map<VduCpMappingFormat, VduCpMappingFormatHandler> serviceCache;

    public VduCpMappingFormatFactory(List<VduCpMappingFormatHandler> services) {
        serviceCache = services.stream()
                .collect(Collectors.toMap(VduCpMappingFormatHandler::getType, Function.identity()));
    }

    public VduCpMappingFormatHandler getService(VduCpMappingFormat type) {
        return Optional.ofNullable(serviceCache.get(type))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown VduCp mapping format: %s", type)));
    }
}
