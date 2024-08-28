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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsEndpointsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.URILink;
import com.ericsson.vnfm.orchestrator.model.granting.ResponseStructureLinks;
import com.ericsson.vnfm.orchestrator.model.granting.StructureLinks;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.response.Grant;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SSLCertificateException;
import com.ericsson.vnfm.orchestrator.routing.onboarding.NfvoOnboardingRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
public class GrantingServiceTest {

    private final String NFVO_ONBOARDING_URL = "https://eo210x70.athtem.eei.ericsson.se";
    private final String GRANTING_URL = "/ecm_service/grant/v1/grants";
    private final URI GRANTING_URI = URI.create(NFVO_ONBOARDING_URL + GRANTING_URL);

    @SpyBean
    @InjectMocks
    GrantingServiceImpl grantingService = new GrantingServiceImpl();

    @Mock
    private NfvoOnboardingRoutingClient nfvoOnboardingRoutingClient;

    private Grant grant;

    @BeforeEach
    public void init() throws JsonProcessingException {
        mockGrantingNotificationEndpointsConfigResult();
        mockOnboardingConfigResult();
        grant = buildGrantResponseOnlyMandatoryFields();
    }

    @Test
    public void testExecuteGrantRequestSuccess() {
        var request = buildGrantRequestOnlyMandatoryFields();

        when(nfvoOnboardingRoutingClient.execute(GRANTING_URI, APPLICATION_JSON_VALUE, HttpMethod.POST, Grant.class, request))
                .thenReturn(ResponseEntity.ok(grant));

        var result = grantingService.executeGrantRequest(request);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
    }

    @Test
    public void testExecuteGrantRequestFailedForbidden() {
        var request = buildGrantRequestOnlyMandatoryFields();

        when(nfvoOnboardingRoutingClient.execute(GRANTING_URI, APPLICATION_JSON_VALUE, HttpMethod.POST, Grant.class, request))
                .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, StringUtils.EMPTY, new HttpHeaders(), null,
                                                           StandardCharsets.UTF_8));

        var result = grantingService.executeGrantRequest(request);

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getBody()).isNull();
    }

    @Test
    public void testExecuteGrantRequestFailedServiceUnavailable() {
        var request = buildGrantRequestOnlyMandatoryFields();

        when(nfvoOnboardingRoutingClient.execute(GRANTING_URI, APPLICATION_JSON_VALUE, HttpMethod.POST, Grant.class, request))
                .thenThrow(HttpClientErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, StringUtils.EMPTY, new HttpHeaders(), null,
                                                           StandardCharsets.UTF_8));

        var result = grantingService.executeGrantRequest(request);

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(result.getBody()).isNull();
    }

    @Test
    public void testExecuteGrantRequestFailedSslCertificateException() {
        var request = buildGrantRequestOnlyMandatoryFields();

        when(nfvoOnboardingRoutingClient.execute(any(), eq(APPLICATION_JSON_VALUE), eq(HttpMethod.POST), eq(Grant.class), eq(request)))
                .thenThrow(SSLCertificateException.class);

        assertThatThrownBy(() -> grantingService.executeGrantRequest(request))
                .isInstanceOf(SSLCertificateException.class);
    }

    private GrantRequest buildGrantRequestOnlyMandatoryFields() {
        GrantRequest request = new GrantRequest();

        request.setVnfInstanceId("vnfInstanceId-1");
        request.setVnfLcmOpOccId("vnfLcmOpOccId-1");
        request.setOperation(GrantedLcmOperationType.INSTANTIATE);
        request.setLinks(buildLinks());

        return request;
    }

    private Grant buildGrantResponseOnlyMandatoryFields() {
        Grant response = new Grant();

        response.setId("grantResponseId-1");
        response.setVnfInstanceId("vnfInstanceId-1");
        response.setVnfLcmOpOccId("vnfLcmOpOccId-1");
        response.setLinks(buildResponseLinks());

        return response;
    }

    private StructureLinks buildLinks() {
        StructureLinks structureLinks = new StructureLinks();

        URILink vnfInstanceLink = new URILink();
        vnfInstanceLink.setHref("/vnfInstanceLink-1");
        structureLinks.setVnfInstance(vnfInstanceLink);

        URILink vnfLcmOppOccLink = new URILink();
        vnfLcmOppOccLink.setHref("/vnfLcmOppOccLink-1");
        structureLinks.setVnfLcmOpOcc(vnfLcmOppOccLink);

        return structureLinks;
    }

    private ResponseStructureLinks buildResponseLinks() {
        ResponseStructureLinks structureLinks = new ResponseStructureLinks();

        URILink vnfInstanceLink = new URILink();
        vnfInstanceLink.setHref("/selfLink-1");
        structureLinks.setSelf(vnfInstanceLink);
        structureLinks.setVnfInstance(buildLinks().getVnfInstance());
        structureLinks.setVnfLcmOpOcc(buildLinks().getVnfLcmOpOcc());

        return structureLinks;
    }

    private void mockGrantingNotificationEndpointsConfigResult() {
        GrantingNotificationsEndpointsConfig grantingNotificationsEndpointsConfig = new GrantingNotificationsEndpointsConfig();
        grantingNotificationsEndpointsConfig.setGrantUrl(GRANTING_URL);

        ReflectionTestUtils.setField(grantingService,
                                     "grantingNotificationsConfig",
                                     new GrantingNotificationsConfig(true, grantingNotificationsEndpointsConfig));
    }

    private void mockOnboardingConfigResult() {
        var onboardingConfig = new OnboardingConfig();
        onboardingConfig.setHost(NFVO_ONBOARDING_URL);
        ReflectionTestUtils.setField(grantingService, "onboardingConfig", onboardingConfig);
    }
}
