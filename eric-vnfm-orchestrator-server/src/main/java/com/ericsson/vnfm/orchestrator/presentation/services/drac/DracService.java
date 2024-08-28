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
package com.ericsson.vnfm.orchestrator.presentation.services.drac;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import static com.ericsson.am.shared.http.HttpUtility.getCurrentHttpRequest;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NOT_AUTHORIZED_FOR_NODE_TYPE_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.ROLES_AND_USERNAME_INFO_MESSAGE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotAuthorizedException;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.JwtDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RefreshScope
public class DracService {

    private final boolean enabled;
    private final Map<String, Set<String>> nodeTypeToRoles;
    private final JwtDecoder jwtDecoder;

    public DracService(@Value("${drac.enabled}") final boolean enabled,
                       @Value("${drac.config.json}") final String configJson,
                       final JwtDecoder jwtDecoder,
                       final ObjectMapper mapper) {

        this.enabled = enabled;
        this.nodeTypeToRoles = extractNodeTypeToRoles(deserializeConfig(configJson, mapper));
        this.jwtDecoder = jwtDecoder;

        LOGGER.info("DRAC configuration initialized with the following values: [enabled={}, nodeTypeToRoles={}]", this.enabled, this.nodeTypeToRoles);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void checkPermissionForNodeType(final String nodeType) {
        final var rolesByNodeType = nodeTypeToRoles.getOrDefault(nodeType, emptySet());
        final var userRoles = jwtDecoder.extractUserRoles(getCurrentHttpRequest());
        final String userName = jwtDecoder.extractUsername(getCurrentHttpRequest());
        LOGGER.debug(format(ROLES_AND_USERNAME_INFO_MESSAGE, userName, userRoles));

        if (rolesByNodeType.stream().noneMatch(userRoles::contains)) {
            throw new NotAuthorizedException(format(NOT_AUTHORIZED_FOR_NODE_TYPE_EXCEPTION, nodeType));
        }
    }

    private static DracConfig deserializeConfig(final String configJson, final ObjectMapper mapper) {
        if (isBlank(configJson)) {
            LOGGER.warn("DRAC configuration is missing");

            return DracConfig.empty();
        }

        try {
            return mapper.readValue(configJson, DracConfig.class);
        } catch (final JsonProcessingException e) {
            LOGGER.warn("Exception occurred while deserializing DRAC configuration: {}", e.getMessage(), e);

            return DracConfig.empty();
        }
    }

    private static Map<String, Set<String>> extractNodeTypeToRoles(final DracConfig dracConfig) {
        final Map<String, Set<String>> nodeTypeToRole = new HashMap<>();

        for (final var role : getFilteredRoles(dracConfig)) {
            for (final var nodeType : getFilteredNodeTypes(role)) {
                nodeTypeToRole.computeIfAbsent(nodeType, key -> new HashSet<>()).add(role.getName());
            }
        }

        return toUnmodifiableMap(nodeTypeToRole);
    }

    private static List<DracRole> getFilteredRoles(final DracConfig dracConfig) {
        return emptyIfNull(dracConfig.getRoles()).stream()
                .filter(role -> role.getName() != null)
                .filter(role -> role.getNodeTypes() != null)
                .collect(toList());
    }

    private static List<String> getFilteredNodeTypes(final DracRole role) {
        return role.getNodeTypes().stream()
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static <K, V> Map<K, Set<V>> toUnmodifiableMap(final Map<K, Set<V>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> unmodifiableSet(entry.getValue())));
    }
}
