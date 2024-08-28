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
package com.ericsson.vnfm.orchestrator.routing;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestTemplateHttpRoutingClient implements HttpRoutingClient {

    private static final String SERVICE_INACCESSIBLE_MESSAGE = "Requested service is currently inaccessible: %s";
    private static final String UNABLE_TO_PARSE_RESPONSE_MESSAGE = "Unable to parse response json. Error message: %s";
    private static final String UNKNOWN_EXCEPTION = "Unable to retrieve exception from BRO endpoint.";
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public <T, V> T executeHttpRequest(final HttpHeaders headers,
                                       final String url,
                                       final HttpMethod httpMethod,
                                       final V requestBody,
                                       final Class<T> responseDtoClass) {
        HttpEntity<V> request = new HttpEntity<>(requestBody, headers);
        try {
            final ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, request, String.class);
            return mapSuccessResponse(response.getBody(), responseDtoClass);
        } catch (ResourceAccessException e) {
            throw new HttpServiceInaccessibleException(String.format(SERVICE_INACCESSIBLE_MESSAGE, url), e);
        } catch (HttpClientErrorException e) {
            throw new HttpClientException(getErrorMessage(e.getResponseBodyAsString()), e);
        }
    }

    private <T> T mapSuccessResponse(String responseBody, Class<T> mappingClass) {
        try {
            return mapper.readValue(responseBody, mappingClass);
        } catch (JsonProcessingException e) {
            throw new HttpResponseParsingException(String.format(UNABLE_TO_PARSE_RESPONSE_MESSAGE, e.getMessage()), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String getErrorMessage(String responseBodyAsString) {
        Map<String, String> map;
        String message;
        try {
            map = mapper.readValue(responseBodyAsString, Map.class);
            message = map.get("message");
        } catch (JsonProcessingException e) {
            LOGGER.warn("An error occurred during parsing error message", e);
            return "Error message can`t be parsed: " + responseBodyAsString;
        }
        return message == null ? UNKNOWN_EXCEPTION : message;
    }
}
