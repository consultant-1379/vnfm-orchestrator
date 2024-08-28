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
package com.ericsson.vnfm.orchestrator.messaging;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OnboardingHealth {
    private static final String ONBOARDING_ACTUATOR_HEALTH_URL = "%s/actuator/health";

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private RestTemplate restTemplate;

    public boolean isUp() {
        String onboardingHealthCheckUrl = String.format(ONBOARDING_ACTUATOR_HEALTH_URL, onboardingConfig.getHost());
        URI onboardingHealthCheckUri = URI.create(onboardingHealthCheckUrl);
        LOGGER.debug("Started checking health of Onboarding service {}", onboardingHealthCheckUrl);

        ResponseEntity<String> result;

        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString());
            result = restTemplate.exchange(onboardingHealthCheckUri, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
        } catch (final Exception e) {
            LOGGER.warn("Health check for Onboarding service failed", e);
            return false;
        }

        boolean isSuccessful = result.getStatusCode().is2xxSuccessful();
        if (!isSuccessful) {
            LOGGER.warn("Health check status for Onboarding service is {}", isSuccessful);
        } else {
            LOGGER.debug("Health check status for Onboarding service is {}", isSuccessful);
        }
        return isSuccessful;
    }
}
