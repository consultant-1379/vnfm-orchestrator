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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.UsageStateRequest;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;

@ExtendWith(MockitoExtension.class)
public class OnboardingClientTest {

    private static final String VNF_PACKAGE_ID = "43bf1225-81e1-46b4-ae10-cadea4432939";
    private static final String VNF_INSTANCE_ID = "43bf1225-81e1-46b4-ae10-cadea4432941";
    private static final URI EVNFM_ONBOARDING_URI = URI.create("http://localhost:8000/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,test)");
    private static final URI NFVO_ONBOARDING_URI = URI.create("http://localhost:9000/ecm_service/SOL003/vnfpkgm/v1/vnf_packages?vnfdId.eq=test");


    @InjectMocks
    private OnboardingClient onboardingClient = new OnboardingClientImpl();

    @Mock
    private OnboardingRoutingClient evnfmOnboardingRoutingClient;
    @Mock
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;
    @Mock
    private NfvoConfig nfvoConfig;

    private PackageResponse[] expectedPackageResponses;
    private UsageStateRequest usageStateRequestPayload;

    @BeforeEach
    public void init() {
        final PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId(VNF_PACKAGE_ID);
        expectedPackageResponses = new PackageResponse[] { packageResponse };

        usageStateRequestPayload = new UsageStateRequest();
        usageStateRequestPayload.setVnfId(VNF_INSTANCE_ID);
        usageStateRequestPayload.setInUse(true);
    }

    @Test
    public void getSmallstackSuccess() {
        ResponseEntity<PackageResponse[]> responseEntityPackageResponse = ResponseEntity.ok(expectedPackageResponses);

        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.GET, PackageResponse[].class, null))
                .thenReturn(responseEntityPackageResponse);

        Optional<PackageResponse[]> actual = onboardingClient.get(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class);

        assertThat(actual).isPresent();
        PackageResponse actualPackageResponse = actual.get()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void getSmallstackFailedNotFound() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.GET, PackageResponse[].class, null))
                .thenThrow(HttpClientErrorException.NotFound.class);

        Optional<PackageResponse[]> actual = onboardingClient.get(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class);

        assertThat(actual).isEmpty();
    }

    @Test
    public void getSmallstackFailedInternalServerError() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.GET, PackageResponse[].class, null))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> onboardingClient.get(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void getSmallstackFailedHttpServiceInaccessible() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.GET, PackageResponse[].class, null))
                .thenThrow(HttpServiceInaccessibleException.class);

        assertThatThrownBy(() -> onboardingClient.get(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class))
                .isInstanceOf(HttpServiceInaccessibleException.class);
    }

    @Test
    public void getFullstackSuccess() {
        ResponseEntity<PackageResponse[]> responseEntityPackageResponse = ResponseEntity.ok(expectedPackageResponses);

        when(nfvoConfig.isEnabled()).thenReturn(true);
        when(nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                 HttpMethod.GET, PackageResponse[].class, null))
                .thenReturn(responseEntityPackageResponse);

        Optional<PackageResponse[]> actual = onboardingClient.get(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class);

        assertThat(actual).isPresent();
        PackageResponse actualPackageResponse = actual.get()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void getFullstackFailedNotFound() {
        when(nfvoConfig.isEnabled()).thenReturn(true);
        when(nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                 HttpMethod.GET, PackageResponse[].class, null))
                .thenThrow(HttpClientErrorException.NotFound.class);

        Optional<PackageResponse[]> actual = onboardingClient.get(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class);

        assertThat(actual).isEmpty();
    }

    @Test
    public void getFullstackFailedInternalServerError() {

        when(nfvoConfig.isEnabled()).thenReturn(true);
        when(nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                 HttpMethod.GET, PackageResponse[].class, null))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> onboardingClient.get(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void getSmallstack() {
        ResponseEntity<PackageResponse[]> responseEntityPackageResponse = ResponseEntity.ok(expectedPackageResponses);

        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.GET, PackageResponse[].class, null))
                .thenReturn(responseEntityPackageResponse);

        Optional<PackageResponse[]> actual = onboardingClient.getSmallstack(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE, PackageResponse[].class);

        verify(nfvoOnboardingRoutingClient, times(0))
                .execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                         HttpMethod.GET, PackageResponse[].class, null);

        assertThat(actual).isPresent();
        PackageResponse actualPackageResponse = actual.get()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void putSmallstackSuccess() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.PUT, String.class, usageStateRequestPayload))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).build());

        onboardingClient.put(EVNFM_ONBOARDING_URI, usageStateRequestPayload);

        verify(evnfmOnboardingRoutingClient, times(1))
                .execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                         HttpMethod.PUT, String.class, usageStateRequestPayload);
    }

    @Test
    public void putFullstackSuccess() {
        when(nfvoConfig.isEnabled()).thenReturn(true);
        when(nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.PUT, String.class, usageStateRequestPayload))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).build());

        onboardingClient.put(NFVO_ONBOARDING_URI, usageStateRequestPayload);

        verify(nfvoOnboardingRoutingClient, times(1))
                .execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                         HttpMethod.PUT, String.class, usageStateRequestPayload);
    }

    @Test
    public void putSmallstackFailedNotFound() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.PUT, String.class, usageStateRequestPayload))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThatThrownBy(() -> onboardingClient.put(EVNFM_ONBOARDING_URI, usageStateRequestPayload))
                .isInstanceOf(PackageDetailsNotFoundException.class);
    }

    @Test
    public void putSmallstackFailedInternalServerError() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.PUT, String.class, usageStateRequestPayload))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> onboardingClient.put(EVNFM_ONBOARDING_URI, usageStateRequestPayload))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void putSmallstackFailedHttpServiceInaccessible() {
        when(nfvoConfig.isEnabled()).thenReturn(false);
        when(evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                  HttpMethod.PUT, String.class, usageStateRequestPayload))
                .thenThrow(HttpServiceInaccessibleException.class);

        assertThatThrownBy(() -> onboardingClient.put(EVNFM_ONBOARDING_URI, usageStateRequestPayload))
                .isInstanceOf(HttpServiceInaccessibleException.class);
    }
}