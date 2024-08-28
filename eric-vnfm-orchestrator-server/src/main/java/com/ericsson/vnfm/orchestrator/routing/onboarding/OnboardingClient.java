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
import java.util.Optional;

public interface OnboardingClient {
    <T> Optional<T> get(URI uri, String acceptHeaderValue, Class<T> responseClass);

    <T> Optional<T> getSmallstack(URI uri, String acceptHeaderValue, Class<T> responseClass);

    <T> void put(URI uri, T payload);
}
