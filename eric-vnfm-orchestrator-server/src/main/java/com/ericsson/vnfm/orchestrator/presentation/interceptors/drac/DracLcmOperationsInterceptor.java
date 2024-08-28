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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.drac;

import static java.lang.String.format;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VNF_LCM_OP_OCC_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.DRAC_DISABLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.EMPTY_NODE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.VALIDATION_STARTED_LOG;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.services.drac.DracService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * A component that checks if user is authorized to perform LCM operations (lifecycle operation based) taking into account Domain Role Access Control
 * configuration.
 */
@Component
@Slf4j
public class DracLcmOperationsInterceptor implements HandlerInterceptor {

    private static final List<String> METHODS_TO_INTERCEPT = Arrays.asList("POST", "PATCH", "DELETE", "PUT");

    private final DatabaseInteractionService databaseInteractionService;
    private final DracService dracService;

    public DracLcmOperationsInterceptor(final DatabaseInteractionService databaseInteractionService, final DracService dracService) {
        this.databaseInteractionService = databaseInteractionService;
        this.dracService = dracService;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        LOGGER.debug(format(VALIDATION_STARTED_LOG, this.getClass().getSimpleName()));

        if (!dracService.isEnabled()) {
            LOGGER.debug(DRAC_DISABLED);
            return true;
        }

        if (METHODS_TO_INTERCEPT.contains(request.getMethod())) {
            final var nodeType = lookupNodeType(request);
            if (nodeType.isEmpty()) {
                LOGGER.debug(EMPTY_NODE_TYPE);
                return true;
            }

            dracService.checkPermissionForNodeType(nodeType.get());
        }

        return true;
    }

    private Optional<String> lookupNodeType(final HttpServletRequest request) {
        return getOperationOccurrenceIdFromPath(request)
                .flatMap(this::getLifecycleOperation)
                .map(LifecycleOperation::getVnfProductName);
    }

    private static Optional<String> getOperationOccurrenceIdFromPath(final HttpServletRequest request) {
        return Optional.ofNullable(getUriTemplateVariables(request).get(VNF_LCM_OP_OCC_ID));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getUriTemplateVariables(final HttpServletRequest request) {
        return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    private Optional<LifecycleOperation> getLifecycleOperation(final String lifecycleOperationId) {
        try {
            return Optional.ofNullable(databaseInteractionService.getLifecycleOperation(lifecycleOperationId));
        } catch (final DataAccessException e) {
            LOGGER.error("Error occurred while getting LCM operation from database", e);

            return Optional.empty();
        }
    }
}
