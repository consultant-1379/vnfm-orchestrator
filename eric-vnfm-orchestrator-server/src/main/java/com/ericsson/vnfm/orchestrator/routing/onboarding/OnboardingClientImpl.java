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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SSLCertificateException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OnboardingClientImpl implements OnboardingClient {

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    @Qualifier("evnfmOnboardingRoutingClient")
    private OnboardingRoutingClient evnfmOnboardingRoutingClient;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @Override
    public <T> Optional<T> get(final URI uri, final String acceptHeaderValue, final Class<T> responseClass) {
        boolean nfvoEnabled = nfvoConfig.isEnabled();
        return get(uri, acceptHeaderValue, responseClass, nfvoEnabled);
    }

    @Override
    public <T> Optional<T> getSmallstack(final URI uri, final String acceptHeaderValue, final Class<T> responseClass) {
        return get(uri, acceptHeaderValue, responseClass, false);
    }

    @Override
    public <T> void put(final URI uri, T payload) {
        try {
            if (nfvoConfig.isEnabled()) {
                nfvoOnboardingRoutingClient.execute(uri, MediaType.APPLICATION_JSON_VALUE, HttpMethod.PUT, String.class, payload);
            } else {
                evnfmOnboardingRoutingClient.execute(uri, MediaType.APPLICATION_JSON_VALUE, HttpMethod.PUT, String.class, payload);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new PackageDetailsNotFoundException(
                    String.format("Package by URI %s is not found", uri), e);
        } catch (HttpServiceInaccessibleException | SSLCertificateException | HttpServerErrorException.InternalServerError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalRuntimeException(String.format("Failed request to EVNFM Onboarding service due to: %s", e.getMessage()), e);
        }
    }

    private <T> Optional<T> get(final URI uri, final String acceptHeaderValue, final Class<T> responseClass, boolean nfvoEnabled) {
        ResponseEntity<T> response;
        try {
            if (nfvoEnabled) {
                response = nfvoOnboardingRoutingClient.execute(uri, acceptHeaderValue, HttpMethod.GET, responseClass, null);
            } else {
                response = evnfmOnboardingRoutingClient.execute(uri, acceptHeaderValue, HttpMethod.GET, responseClass, null);
            }
        } catch (HttpClientErrorException.NotFound e) { // NOSONAR
            LOGGER.warn("No resource found in Onboarding service by URI: {}", uri);
            return Optional.empty();
        } catch (HttpServiceInaccessibleException | SSLCertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalRuntimeException(String.format("Internal exception occurred in Onboarding service by calling URI: %s", uri), e);
        }
        T body = response.getBody();

        return Optional.ofNullable(body);
    }
}
