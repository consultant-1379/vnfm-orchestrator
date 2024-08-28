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

import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OnboardingRoutingClientUtils {

    private static final String FAILED_TO_PARSE_ONBOARDING_RESPONSE = "Failed to parse Onboarding response from string to %s";

    public <T> T parseResponseBody(String responseBody, Class<T> responseClass) {
        try {
            if (responseClass.equals(String.class)) {
                return responseClass.cast(responseBody);
            } else {
                return new ObjectMapper().readValue(responseBody, responseClass);
            }
        } catch (JsonProcessingException e) {
            throw new HttpResponseParsingException(String.format(FAILED_TO_PARSE_ONBOARDING_RESPONSE, responseClass.getSimpleName()), e);
        }
    }
}
