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
package com.ericsson.vnfm.orchestrator.presentation.services.packageing;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.USAGE_STATE_API;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

@Component
public class OnboardingUriProvider {

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private OnboardingConfig onboardingConfig;

    public URI getOnboardingPackagesQueryUri(String vnfIdentifier) {
        UriComponentsBuilder uriComponentsBuilder = getUriBuilderOnboarding();
        String query = String.format(onboardingConfig.getQueryValue(), vnfIdentifier);
        try {
            if (nfvoConfig.isEnabled()) {
                return new URL(uriComponentsBuilder
                                       .query(query).build().toString()).toURI();
            } else {
                return new URL(uriComponentsBuilder
                                       .queryParam("filter", query).build().toString()).toURI();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    public URI getVnfdQueryUri(String vnfPkgId) {
        try {
            return new URL(getUriBuilderOnboarding(vnfPkgId, "vnfd")
                                   .build().toString())
                    .toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    public URI getPackageSupportedOperationsQueryUri(String vnfPkgId) {
        try {
            return new URL(
                    UriComponentsBuilder.fromHttpUrl(onboardingConfig.getHost())
                            .path("/api/v1/")
                            .pathSegment("packages", vnfPkgId, "supported_operations")
                            .build().toString())
                    .toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    public URI getArtifactUri(final String vnfPackageId, final String artifactPath) {
        try {
            return new URL(getUriBuilderOnboarding(vnfPackageId, "artifacts", artifactPath)
                                   .build().toString())
                    .toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    public URI getUpdateUsageStateUri(final String vnfPackageId) {
        try {
            return new URL(getUriBuilderOnboarding(vnfPackageId, USAGE_STATE_API)
                                   .build()
                                   .toString())
                    .toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    private UriComponentsBuilder getUriBuilderOnboarding(String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(onboardingConfig.getHost())
                .path(onboardingConfig.getPath())
                .pathSegment(pathSegments);
    }
}
