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
package com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;

import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.ericsson.am.shared.http.HttpUtility;

public final class ExceptionHandlersUtils {

    private static final String VNF_INTANCE_ID_PATH_VARIABLE_NAME = "vnfInstanceId";
    private static final String RESOURCE_ID_PATH_VARIABLE_NAME = "resourceId";

    private ExceptionHandlersUtils() {

    }

    public static URI getInstanceUri(WebRequest request) {
        String vnfInstanceId = extractPathVariable(request);
        if (StringUtils.isNotEmpty(vnfInstanceId)) {
            return URI.create(HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + vnfInstanceId);
        }
        return URI.create(TYPE_BLANK);
    }

    private static String extractPathVariable(WebRequest request) {
        if (request instanceof final ServletWebRequest servletWebRequest) {
            Object pathVariablesObj = servletWebRequest.getAttribute(
                    URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                    SCOPE_REQUEST
            );

            if (pathVariablesObj instanceof Map) {
                Map<String, String> pathVariables = (Map<String, String>) pathVariablesObj;
                return extractPathVariable(pathVariables);
            }
        }
        return null;
    }

    private static String extractPathVariable(Map<String, String> pathVariables) {
        if (pathVariables.containsKey(VNF_INTANCE_ID_PATH_VARIABLE_NAME)) {
            return pathVariables.get(VNF_INTANCE_ID_PATH_VARIABLE_NAME);
        } else if (pathVariables.containsKey(RESOURCE_ID_PATH_VARIABLE_NAME)) {
            return pathVariables.get(RESOURCE_ID_PATH_VARIABLE_NAME);
        }
        return null;
    }
}
