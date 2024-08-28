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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static com.ericsson.vnfm.orchestrator.utils.OnboardingUtility.createOnboardingRetryTemplate;

import java.io.IOException;
import java.net.URI;
import javax.net.ssl.SSLException;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.shaded.org.apache.commons.lang3.reflect.FieldUtils;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AuthenticationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SSLCertificateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ExtendWith(MockitoExtension.class)
public class NfvoOnboardingRoutingClientTest {

    private static final String VNF_PACKAGE_ID = "43bf1225-81e1-46b4-ae10-cadea4432939";
    private static final URI NFVO_ONBOARDING_URI = URI.create("http://localhost:9000/ecm_service/SOL003/vnfpkgm/v1/vnf_packages?vnfdId.eq=test");

    @InjectMocks
    private NfvoOnboardingRoutingClient nfvoOnboardingRoutingClient = new NfvoOnboardingRoutingClient();

    @Mock
    private OnboardingConfig onboardingConfig;

    @Mock
    private OkHttpClient okHttpClient;
    @Spy
    private ObjectMapper objectMapper;
    @Mock
    private Call callMock;

    private PackageResponse packageResponse;
    private String packageDetails;

    @BeforeEach
    public void init() throws JsonProcessingException, IllegalAccessException {
        mockRetryTemplateResult();
        mockNfvoConfigResult();

        FieldUtils.writeField(nfvoOnboardingRoutingClient, "nfvoToken", "token", true);

        packageResponse = new PackageResponse();
        packageResponse.setId(VNF_PACKAGE_ID);
        packageDetails = objectMapper.writeValueAsString(new PackageResponse[] { packageResponse });
    }

    @Test
    public void executeSuccess() throws IOException {
        Response response = generateResponse(HttpStatus.OK.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute()).thenReturn(response);

        ResponseEntity<PackageResponse[]> actual = nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                                        HttpMethod.GET, PackageResponse[].class, null);

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();
        PackageResponse actualPackageResponse = actual.getBody()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void executeSuccessWithRetry() throws IOException {
        Response responseOk = generateResponse(HttpStatus.OK.value());
        Response responseInternalServerError = generateResponse(HttpStatus.INTERNAL_SERVER_ERROR.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute())
                .thenReturn(responseInternalServerError)
                .thenReturn(responseOk);

        ResponseEntity<PackageResponse[]> actual = nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                                       HttpMethod.GET, PackageResponse[].class, null);

        verify(callMock, times(2))
                .execute();

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();
        PackageResponse actualPackageResponse = actual.getBody()[0];
        assertThat(actualPackageResponse.getId()).isEqualTo(VNF_PACKAGE_ID);
    }

    @Test
    public void executeWithRetryFailedInternalServerError() throws IOException {
        Response responseInternalServerError = generateResponse(HttpStatus.INTERNAL_SERVER_ERROR.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute())
                .thenReturn(responseInternalServerError);

        assertThatThrownBy(() -> nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                     HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpServerErrorException.class);

        verify(callMock, times(4)).execute();
    }

    @Test
    public void executeSuccessNotParsingString() throws IOException {
        Response response = generateResponse(HttpStatus.OK.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute()).thenReturn(response);

        ResponseEntity<String> actual = nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                            HttpMethod.GET, String.class, null);

        assertThat(actual.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(actual.getBody()).isNotEmpty();

        verify(objectMapper, times(0)).readValue(packageDetails, String.class);
    }

    @Test
    public void executeFailedHttpClientErrorExceptionWithoutRetry() throws IOException {
        Response response = generateResponse(HttpStatus.NOT_FOUND.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute()).thenReturn(response);

        assertThatThrownBy(() -> nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                     HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        verify(callMock, times(1)).execute();
    }

    @Test
    public void executeWithPayloadPostSuccess() throws IOException {
        Response response = generateResponse(HttpStatus.CREATED.value());

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute()).thenReturn(response);

        ResponseEntity<PackageResponse[]> actual = nfvoOnboardingRoutingClient
                .execute(NFVO_ONBOARDING_URI, APPLICATION_JSON_VALUE, HttpMethod.POST,
                         PackageResponse[].class, packageResponse);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).hasSize(1);

        verify(callMock, times(1)).execute();
    }

    @Test
    public void executeResponseWithoutBodySuccess() throws IOException {
        Response responseWithoutBody = generateResponse(HttpStatus.CREATED.value(), null);

        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute()).thenReturn(responseWithoutBody);

        ResponseEntity<PackageResponse[]> actual = nfvoOnboardingRoutingClient
                .execute(NFVO_ONBOARDING_URI, APPLICATION_JSON_VALUE, HttpMethod.POST,
                         PackageResponse[].class, packageResponse);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getBody()).isNullOrEmpty();

        verify(callMock, times(1)).execute();
    }

    @Test
    public void executeFailedCertificatesException() throws IOException {
        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute())
                .thenThrow(SSLException.class);

        assertThatThrownBy(() -> nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                     HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(SSLCertificateException.class);

        verify(callMock, times(4)).execute();
    }

    @Test
    public void executeFailedInputOutputException() {
        URI malformedUri = URI.create("https://domain:-2");

        assertThatThrownBy(() -> nfvoOnboardingRoutingClient.execute(malformedUri, MediaType.APPLICATION_JSON_VALUE,
                                                                     HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(HttpClientException.class);
    }

    @Test
    public void executeFailedMalformedException() throws IOException {
        when(okHttpClient.newCall((any())))
                .thenReturn(callMock);
        when(callMock.execute())
                .thenThrow(IOException.class);

        assertThatThrownBy(() -> nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, MediaType.APPLICATION_JSON_VALUE,
                                                                     HttpMethod.GET, PackageResponse[].class, null))
                .isInstanceOf(IllegalStateException.class);

        verify(callMock, times(4)).execute();
    }

    @Test
    public void testNfvoOnbordingRequestRenewTokenSucceeds() throws IllegalAccessException, IOException {
        Response authResponse = buildAuthResponse();
        Response expected = generateResponse(HttpStatus.OK.value());
        when(okHttpClient.newCall(any()))
                .thenReturn(callMock);
        when(callMock.execute())
                .thenThrow(new AuthenticationException("Auth failed"))
                .thenReturn(authResponse)
                .thenReturn(expected);
        final ResponseEntity<PackageResponse[]> actual = nfvoOnboardingRoutingClient.execute(NFVO_ONBOARDING_URI, APPLICATION_JSON_VALUE,
                                                                                              HttpMethod.GET, PackageResponse[].class, null);

        assertEquals(expected.code(), actual.getStatusCode().value());

        verify(callMock, times(3)).execute();
    }

    private void mockRetryTemplateResult() {
        RetryTemplate retryTemplate = createOnboardingRetryTemplate();

        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, "nfvoRetryTemplate", retryTemplate);
    }

    private void mockNfvoConfigResult() {
        var nfvoConfig = new NfvoConfig();
        nfvoConfig.setEnabled(true);
        nfvoConfig.setTenantId("ECM");
        nfvoConfig.setUsername("ecmadmin");
        nfvoConfig.setPassword("ecmadminPassword-1");

        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, "nfvoConfig", nfvoConfig);
    }

    @NotNull
    private Response generateResponse(final int responseCode) {
        return generateResponse(responseCode, getResponseBody());
    }

    @NotNull
    private Response generateResponse(final int responseCode, final ResponseBody responseBody) {
        return new Response.Builder()
                .request(new Request.Builder()
                                 .url("http://localhost:9000/ecm_service/SOL003/vnfpkgm/v1/vnf_packages?vnfdId.eq=test")
                                 .build())
                .protocol(Protocol.HTTP_2)
                .message("")
                .code(responseCode)
                .body(responseBody).build();
    }

    private Response buildAuthResponse() {
        ResponseBody responseBody = ResponseBody.create("{\n"
                                                                + "    \"status\": {\n"
                                                                + "        \"credentials\": \"nA_n2RyvubV8TtG3M2j1pSmzYBw"
                                                                +
                                                                ".*AAJTSQACMDIAAlNLABxER2ZrNnIyeVhNNnFGS2RaWnF6eUlFcFN2dGM9AAR0eXBlAANDVFMAAlMxAAIwMQ..*\",\n"
                                                                + "        \"reqStatus\": \"SUCCESS\",\n"
                                                                + "        \"msgs\": [\n"
                                                                + "            {\n"
                                                                + "                \"msgCode\": \"ECMSE103\",\n"
                                                                + "                \"msgText\": \"User %A1% was authenticated successfully.\",\n"
                                                                + "                \"msgValues\": [\n"
                                                                + "                    \"ecmadmin\"\n"
                                                                + "                ]\n"
                                                                + "            }\n"
                                                                + "        ]\n"
                                                                + "    }\n"
                                                                + "}", okhttp3.MediaType.parse("application/json"));
        return new Response.Builder()
                .request(new Request.Builder()
                                 .url("http://localhost:9000/ecm_service/tokens")
                                 .build())
                .body(responseBody)
                .code(200)
                .message("Message")
                .protocol(Protocol.HTTP_2)
                .build();
    }

    private ResponseBody getResponseBody() {
        return ResponseBody.create(okhttp3.MediaType.parse(APPLICATION_JSON_VALUE), packageDetails);
    }
}