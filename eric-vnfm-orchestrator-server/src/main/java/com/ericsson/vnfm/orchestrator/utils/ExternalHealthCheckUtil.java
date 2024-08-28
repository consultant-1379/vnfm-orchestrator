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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExternalHealthCheckUtil {

    private ExternalHealthCheckUtil() { }

    public static boolean checkHealth(String healthCheckUrl, RestTemplate restTemplate, String serviceName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString());
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result;
        try {
            result = restTemplate.exchange(healthCheckUrl, HttpMethod.GET, request, String.class);
        } catch (RestClientException e) {
            LOGGER.error("health check for {} failed with the message: {}", serviceName, e.getMessage());
            return false;
        }
        LOGGER.debug("Health check status {}", result.getStatusCode().is2xxSuccessful());
        return result.getStatusCode().is2xxSuccessful();
    }

}
