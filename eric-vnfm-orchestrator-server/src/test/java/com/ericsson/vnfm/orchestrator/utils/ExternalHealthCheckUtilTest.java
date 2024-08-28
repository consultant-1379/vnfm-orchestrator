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
package com.ericsson.vnfm.orchestrator.utils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
public class ExternalHealthCheckUtilTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<String> responseEntity;

    private static final String TEST_HEALTH_CHECK_URL = "actuator/health";

    @Test
    public void testHealthCheckIs2xxSuccessful() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                   ArgumentMatchers.<Class<String>>any()))
                .thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        boolean result = ExternalHealthCheckUtil.checkHealth(TEST_HEALTH_CHECK_URL, restTemplate, "testServiceName");

        verify(responseEntity, times(2)).getStatusCode();
        Assertions.assertTrue(result);
    }

    @Test
    public void testHealthCheckIsFailed() {
        when(restTemplate.exchange(anyString(), ArgumentMatchers.any(HttpMethod.class), ArgumentMatchers.any(HttpEntity.class),
                                   ArgumentMatchers.<Class<String>>any()))
                .thenThrow(RestClientException.class);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        boolean result = ExternalHealthCheckUtil.checkHealth(TEST_HEALTH_CHECK_URL, restTemplate, "testServiceName");

        verifyNoInteractions(responseEntity);
        Assertions.assertFalse(result);
    }

}