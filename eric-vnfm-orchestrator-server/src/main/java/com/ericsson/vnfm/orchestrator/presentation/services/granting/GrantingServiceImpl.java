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
package com.ericsson.vnfm.orchestrator.presentation.services.granting;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.response.Grant;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GrantingServiceImpl implements GrantingService {

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private GrantingNotificationsConfig grantingNotificationsConfig;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @Override
    public ResponseEntity<Grant> executeGrantRequest(GrantRequest grantRequest) {
        LOGGER.info("Starts execution Granting request for VNF {}", grantRequest.getVnfInstanceId());
        final String grantingUrl = buildNFVOGrantingRequestUrl();
        final URI grantingUri = URI.create(grantingUrl);
        try {
            return nfvoOnboardingRoutingClient.execute(grantingUri, MediaType.APPLICATION_JSON_VALUE,
                                                HttpMethod.POST,
                                                Grant.class,
                                                grantRequest);
        } catch (RestClientResponseException e) {
            LOGGER.error("Error occurred while making HTTP request", e);

            return ResponseEntity.status(e.getRawStatusCode()).build();
        }
    }

    private String buildNFVOGrantingRequestUrl() {
        return onboardingConfig.getHost()
                + grantingNotificationsConfig.getGrantingNotificationsEndpointsConfig().getGrantUrl();
    }
}