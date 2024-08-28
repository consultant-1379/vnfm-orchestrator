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

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RestTemplateHttpRoutingClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private RestTemplateHttpRoutingClient client;

    @Test
    void executeHttpRequestWhenSuccessful() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":\"some-id\",\"count\":\"10\"}"));

        // when
        final Response response = client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class);

        // then
        assertThat(response).isEqualTo(new Response("some-id", 10));
    }

    @Test
    void executeHttpRequestWhenInvalidResponseJson() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{"));

        // when and then
        assertThatException().isThrownBy(() -> client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class))
                .isInstanceOf(HttpResponseParsingException.class)
                .withMessageStartingWith("Unable to parse response json. Error message: Unexpected end-of-input");
    }

    @Test
    void executeHttpRequestWhenIOErrorOccurs() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("I/O error occurred"));

        // when and then
        assertThatException().isThrownBy(() -> client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class))
                .isInstanceOf(HttpServiceInaccessibleException.class)
                .withMessageStartingWith("Requested service is currently inaccessible: url");
    }

    @Test
    void executeHttpRequestWhen4xxErrorOccursAndResponseHasMessage() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenThrow(getHttpClientErrorException("{\"message\":\"Request is malformed\"}"));

        // when and then
        assertThatException().isThrownBy(() -> client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class))
                .isInstanceOf(HttpClientException.class)
                .withMessageStartingWith("Request is malformed");
    }

    @Test
    void executeHttpRequestWhen4xxErrorOccursAndResponseHasNoMessage() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenThrow(getHttpClientErrorException("{}"));

        // when and then
        assertThatException().isThrownBy(() -> client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class))
                .isInstanceOf(HttpClientException.class)
                .withMessageStartingWith("Unable to retrieve exception from BRO endpoint.");
    }

    @Test
    void executeHttpRequestWhen4xxErrorOccursAndInvalidResponseJsonMessage() {
        // given
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenThrow(getHttpClientErrorException("{"));

        // when and then
        assertThatException().isThrownBy(() -> client.executeHttpRequest(HttpHeaders.EMPTY, "url", POST, "{}", Response.class))
                .isInstanceOf(HttpClientException.class)
                .withMessageStartingWith("Error message can`t be parsed: {");
    }

    private static HttpClientErrorException getHttpClientErrorException(final String responseBody) {
        return HttpClientErrorException.create(BAD_REQUEST, "Malformed request", HttpHeaders.EMPTY, responseBody.getBytes(UTF_8), UTF_8);
    }

    private record Response(String id, Integer count) {
    }
}
