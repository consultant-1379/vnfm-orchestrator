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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public interface HttpRoutingClient {
    /**
     * Executes any REST call.
     * @param headers http headers
     * @param url url which will be called
     * @param httpMethod http method
     * @param <T> request body, can be any object (dto)
     * @param <V> response type.
     * @return
     */
    <T, V> T executeHttpRequest(HttpHeaders headers, String url, HttpMethod httpMethod, V requestBody, Class<T> requestDtoClass);
}
