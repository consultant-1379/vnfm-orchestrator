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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.OnboardingUtility.createOnboardingRetryTemplate;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.messaging.OnboardingHealth;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class EvnfmOnboardingRoutingClientTest {

    private static final String VNF_PACKAGE_ID = "43bf1225-81e1-46b4-ae10-cadea4432939";
    private static final URI EVNFM_ONBOARDING_URI = URI.create("http://localhost:8000/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,test)");

    @InjectMocks
    private EvnfmOnboardingRoutingClient evnfmOnboardingRoutingClient = new EvnfmOnboardingRoutingClient();

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OnboardingHealth onboardingHealth;

    @Spy
    private ObjectMapper objectMapper;

    private String packageDetails;

    @BeforeEach
    public void init() throws JsonProcessingException {
        mockRetryTemplateResult();

        final PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId(VNF_PACKAGE_ID);
        packageDetails = objectMapper.writeValueAsString(new PackageResponse[] { packageResponse });
    }

    @Test
    public void executeSuccess() {
        ResponseEntity<String> responseEntityPackages = ResponseEntity.ok(packageDetails);
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseEntityPackages);

        ResponseEntity<PackageResponse[]> actual = evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                                               HttpMethod.GET, PackageResponse[].class, null);

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();
        PackageResponse actualPackageResponse = actual.getBody()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void executeWithRetrySuccess() {
        ResponseEntity<String> responseEntityPackages = ResponseEntity.ok(packageDetails);
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpServerErrorException.class)
                .thenReturn(responseEntityPackages);

        ResponseEntity<PackageResponse[]> actual = evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                                               HttpMethod.GET, PackageResponse[].class, null);

        verify(restTemplate, times(2))
                .exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class));

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();
        PackageResponse actualPackageResponse = actual.getBody()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void executeWithRetryFailedInternalServerError() {
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpServerErrorException.class)
                .thenThrow(HttpServerErrorException.class)
                .thenThrow(HttpServerErrorException.class)
                .thenThrow(HttpServerErrorException.class);
        when(onboardingHealth.isUp()).thenReturn(true);

        assertThatThrownBy(() -> evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                      HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpServerErrorException.class);

        verify(restTemplate, times(4))
                .exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    public void executeWithRetryFailedHttpServiceInaccessible() {
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class)
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class)
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class)
                .thenThrow(HttpServerErrorException.ServiceUnavailable.class);
        when(onboardingHealth.isUp()).thenReturn(false);

        assertThatThrownBy(() -> evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                      HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpServiceInaccessibleException.class);

        verify(restTemplate, times(4))
                .exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    public void executeSuccessNotParsingString() throws JsonProcessingException {
        ResponseEntity<String> responseEntityPackages = ResponseEntity.ok(packageDetails);
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseEntityPackages)
                .thenReturn(responseEntityPackages)
                .thenReturn(responseEntityPackages)
                .thenReturn(responseEntityPackages);

        ResponseEntity<String> actual = evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                             HttpMethod.GET, String.class, null);

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();

        verify(objectMapper, times(0)).readValue(packageDetails, String.class);
    }

    @Test
    public void executeFailedHttpResponseParsing() {
        ResponseEntity<String> responseEntityPackages = ResponseEntity.ok("Invalid package response");

        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseEntityPackages);
        when(onboardingHealth.isUp()).thenReturn(true);

        assertThatThrownBy(() -> evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                      HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpResponseParsingException.class);
        verify(restTemplate, times(4))
                .exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    public void executeFailedHttpClientErrorExceptionWithoutRetry() {
        when(restTemplate.exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);
        when(onboardingHealth.isUp()).thenReturn(true);

        assertThatThrownBy(() -> evnfmOnboardingRoutingClient.execute(EVNFM_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                      HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        verify(restTemplate, times(1))
                .exchange(eq(EVNFM_ONBOARDING_URI), eq(HttpMethod.GET), any(), eq(String.class));
    }

    private void mockRetryTemplateResult() {
        RetryTemplate retryTemplate = createOnboardingRetryTemplate();

        ReflectionTestUtils.setField(evnfmOnboardingRoutingClient, "nfvoRetryTemplate", retryTemplate);
    }
}