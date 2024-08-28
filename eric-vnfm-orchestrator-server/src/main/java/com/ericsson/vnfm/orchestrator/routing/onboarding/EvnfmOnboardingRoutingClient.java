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
package com.ericsson.vnfm.orchestrator.routing.onboarding;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.messaging.OnboardingHealth;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EvnfmOnboardingRoutingClient implements OnboardingRoutingClient {

    private static final String ONBOARDING_SERVICE_INACCESSIBLE_MESSAGE = "Onboarding service is currently inaccessible: %s";

    @Autowired
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OnboardingHealth onboardingHealth;

    @Override
    public <R, P> ResponseEntity<R> execute(final URI uri, final String acceptHeaderValue, HttpMethod httpMethod,
                                            Class<R> responseClass, P payload) {
        try {
            ResponseEntity<R> responseEntity = nfvoRetryTemplate.execute(context -> processRequest(uri, acceptHeaderValue, httpMethod,
                                                                                                   responseClass, payload));
            LOGGER.info("Completed request to EVNFM Onboarding service. URI: {}. Response status: {}", uri, responseEntity.getStatusCode());
            return responseEntity;
        } catch (Exception e) {
            LOGGER.warn("Failed request to EVNFM Onboarding service with message: {}. URI: {}", e.getMessage(), uri);
            if (onboardingHealth.isUp()) {
                LOGGER.info("EVNFM Onboarding is up. Rethrowing an exception");
                throw e;
            } else {
                throw new HttpServiceInaccessibleException(String.format(ONBOARDING_SERVICE_INACCESSIBLE_MESSAGE, uri), e);
            }
        }
    }

    private <R, P> ResponseEntity<R> processRequest(final URI uri, final String acceptHeaderValue, HttpMethod httpMethod,
                                                    Class<R> responseClass, P payload) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, acceptHeaderValue);
        ResponseEntity<String> responseEntityAsString = restTemplate.exchange(uri, httpMethod,
                                                                              new HttpEntity<>(payload, httpHeaders), String.class);
        R response = OnboardingRoutingClientUtils.parseResponseBody(responseEntityAsString.getBody(), responseClass);
        return ResponseEntity.status(responseEntityAsString.getStatusCode())
                .body(response);
    }
}
