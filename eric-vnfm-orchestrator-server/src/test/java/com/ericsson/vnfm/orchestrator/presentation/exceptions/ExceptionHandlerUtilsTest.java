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
package com.ericsson.vnfm.orchestrator.presentation.exceptions;

import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ExceptionHandlersUtils;

public class ExceptionHandlerUtilsTest {

    private static final String EVNFM_HOST = "http://vnfm.ews.gic.ericsson.se";

    @Test
    public void getInstanceUriTest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setServletPath("/vnflcm/v1/vnf_instances/7ec28fbf-03f7-4ed6-a4b2-1d8c478159ab/instantiate");
        WebRequest webRequest = new ServletWebRequest(httpRequest);

        Map<String, String> uriTemplateVariables = new HashMap<>();
        uriTemplateVariables.put("vnfInstanceId", "7ec28fbf-03f7-4ed6-a4b2-1d8c478159ab");
        httpRequest.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

        URI instanceURI;
        try (MockedStatic<HttpUtility> mockedStatic = Mockito.mockStatic(HttpUtility.class)) {
            // Set up the behavior of the mocked method
            mockedStatic.when(HttpUtility::getHostUrl).thenReturn(EVNFM_HOST);

            instanceURI = ExceptionHandlersUtils.getInstanceUri(webRequest);
        }

        Assertions.assertThat(instanceURI.toString()).isEqualTo(EVNFM_HOST + LCM_VNF_INSTANCES + "7ec28fbf-03f7-4ed6-a4b2-1d8c478159ab");
    }

    @Test
    public void getResourceUriTest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setServletPath("/api/v1/resources/9ec28fbf-03f7-4ed6-a4b2-1d8c478159ab/downgradeInfo/");
        WebRequest webRequest = new ServletWebRequest(httpRequest);

        Map<String, String> uriTemplateVariables = new HashMap<>();
        uriTemplateVariables.put("resourceId", "9ec28fbf-03f7-4ed6-a4b2-1d8c478159ab");
        httpRequest.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

        URI instanceURI;
        try (MockedStatic<HttpUtility> mockedStatic = Mockito.mockStatic(HttpUtility.class)) {
            // Set up the behavior of the mocked method
            mockedStatic.when(HttpUtility::getHostUrl).thenReturn(EVNFM_HOST);

            instanceURI = ExceptionHandlersUtils.getInstanceUri(webRequest);
        }

        Assertions.assertThat(instanceURI.toString()).isEqualTo(EVNFM_HOST + LCM_VNF_INSTANCES + "9ec28fbf-03f7-4ed6-a4b2-1d8c478159ab");
    }

    @Test
    public void getEmptyInstanceUriTest() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setServletPath("/vnflcm/v1/vnf_instances");
        WebRequest webRequest = new ServletWebRequest(httpRequest);

        URI instanceURI;
        try (MockedStatic<HttpUtility> mockedStatic = Mockito.mockStatic(HttpUtility.class)) {
            // Set up the behavior of the mocked method
            mockedStatic.when(HttpUtility::getHostUrl).thenReturn(EVNFM_HOST);

            instanceURI = ExceptionHandlersUtils.getInstanceUri(webRequest);
        }

        Assertions.assertThat(instanceURI.toString()).isEqualTo(TYPE_BLANK);
    }
}
