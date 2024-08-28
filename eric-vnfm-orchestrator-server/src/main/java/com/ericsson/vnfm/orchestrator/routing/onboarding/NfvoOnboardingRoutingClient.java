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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AuthenticationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SSLCertificateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Service
public class NfvoOnboardingRoutingClient implements OnboardingRoutingClient {

    private static final String FAILED_TO_CONVERT_PAYLOAD_TO_STRING = "Failed to convert payload to string";
    private static final String NFVO_AUTH_PATH = "/ecm_service/tokens";
    private String nfvoToken;

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public <R, P> ResponseEntity<R> execute(final URI uri, final String acceptHeaderValue, HttpMethod httpMethod, Class<R> responseClass, P payload) {
        ResponseEntity<R> responseEntity;
        if (StringUtils.isNotEmpty(nfvoToken)) {
            try {
                responseEntity = nfvoRetryTemplate.execute(context ->
                        processRequest(uri, acceptHeaderValue, httpMethod, responseClass, payload));
            } catch (AuthenticationException e) { // NOSONAR
                responseEntity = callNvfoWithAuthorizationRequest(uri, responseClass, payload, acceptHeaderValue, httpMethod);
            }
        } else {
            responseEntity = callNvfoWithAuthorizationRequest(uri, responseClass, payload, acceptHeaderValue, httpMethod);
        }
        return responseEntity;
    }

    private <R, P> ResponseEntity<R> callNvfoWithAuthorizationRequest(URI uri, Class<R> responseClass, P payload,
                                                                      String acceptHeaderValue, HttpMethod httpMethod) {
        requestNfvoToken();
        return nfvoRetryTemplate.execute(context ->
                processRequest(uri, acceptHeaderValue, httpMethod, responseClass, payload));
    }

    private <R, P> ResponseEntity<R> processRequest(URI uri, String acceptHeaderValue, HttpMethod httpMethod, Class<R> responseClass, P payload) {
        try {
            Request request = buildRequest(uri, acceptHeaderValue, httpMethod, payload);

            LOGGER.info("Started request to NFVO by URI {} with body: {}", uri, payload);
            Response response = okHttpClient.newCall(request).execute();
            final HttpStatus httpStatus = HttpStatus.valueOf(response.code());
            LOGGER.info("Completed request to NFVO. URI: {} with body: {}. Response status: {}", uri, payload, httpStatus);
            if (HttpStatus.UNAUTHORIZED.isSameCodeAs(httpStatus)) {
                LOGGER.info("Authentication to NFVO failed. Need to request new token");
                throw new AuthenticationException("Authentication to NFVO failed");
            }
            final String responseBodyString = extractBodyAsString(response);
            if (httpStatus.is2xxSuccessful()) {
                if (responseBodyString.isBlank()) {
                    return ResponseEntity.status(httpStatus).build();
                } else {
                    R body = OnboardingRoutingClientUtils.parseResponseBody(responseBodyString, responseClass);
                    return ResponseEntity.status(httpStatus).body(body);
                }
            } else if (httpStatus.is4xxClientError()) {
                throw HttpClientErrorException.create(
                        httpStatus, httpStatus.name(), new HttpHeaders(),
                        responseBodyString.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            } else {
                throw HttpServerErrorException.create(
                        httpStatus, httpStatus.name(), new HttpHeaders(),
                        responseBodyString.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
        } catch (SSLException e) {
            throw new SSLCertificateException(
                    "Invalid SSL certificates: Failed to establish secure connection to " + uri.getHost(), e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private <P> Request buildRequest(URI uri, String acceptHeaderValue, HttpMethod httpMethod, P payload) {
        try {
            RequestBody requestBody = createRequestBodyFromPayload(payload);
            return new Request.Builder()
                    .url(uri.toURL())
                    .method(httpMethod.name(), requestBody)
                    .addHeader(HttpHeaders.ACCEPT, acceptHeaderValue)
                    .addHeader("cache-control", "no-cache")
                    .addHeader("AuthToken", this.nfvoToken)
                    .addHeader("tenantId", nfvoConfig.getTenantId())
                    .build();
        } catch (MalformedURLException e) {
            throw new HttpClientException(e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new HttpResponseParsingException(FAILED_TO_CONVERT_PAYLOAD_TO_STRING, e);
        }
    }

    private String extractBodyAsString(Response response) throws IOException {
        return response.body() == null ? "" : response.body().string();
    }

    private <P> RequestBody createRequestBodyFromPayload(P payload) throws JsonProcessingException {
        return payload != null ? RequestBody.create(objectMapper.writeValueAsString(payload),
                                             okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE)) : null;
    }

    private void requestNfvoToken() throws AuthenticationException {
        URI authUri = buildUriForNfvoAuthRequest();
        LOGGER.info("Started request to NFVO Onboarding service for authentification token. URI: {}", authUri);
        Request request = buildAuthRequest(authUri, MediaType.APPLICATION_JSON_VALUE, HttpMethod.POST);
        String body;
        try {
            Response response = nfvoRetryTemplate.execute(context -> okHttpClient.newCall(request).execute());
            final HttpStatus httpStatus = HttpStatus.valueOf(response.code());
            body = extractBodyAsString(response);
            if (HttpStatus.UNAUTHORIZED.isSameCodeAs(httpStatus)) {
                LOGGER.info("Authentication to NFVO failed");
                throw new AuthenticationException("Authentication to NFVO failed");
            } else if (httpStatus.is4xxClientError()) {
                throw HttpClientErrorException.create(
                        httpStatus, httpStatus.name(), new HttpHeaders(),
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            } else if (httpStatus.is5xxServerError()) {
                throw HttpServerErrorException.create(
                        httpStatus, httpStatus.name(), new HttpHeaders(),
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
        } catch (SSLException e) {
            LOGGER.error("Insecure connection:: Failed to login to NFVO due to reason: ", e);
            throw new SSLCertificateException(
                    "Invalid SSL certificates:: Failed to establish secure connection to " + authUri.getHost());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        String token;
        try {
            token = objectMapper.readTree(body).path("status").path("credentials").asText();
        } catch (JsonProcessingException e) {
            LOGGER.error("Json parsing error", e);
            throw new IllegalArgumentException("Unable to parse NFVO token: " + e.getMessage());
        }
        this.nfvoToken = token;
        LOGGER.info("Completed request to NFVO Onboarding service for authentification token. URI: {}", authUri);
    }

    private Request buildAuthRequest(URI uri, String acceptHeaderValue, HttpMethod httpMethod) {
        String credentials = Credentials.basic(nfvoConfig.getUsername(), nfvoConfig.getPassword());
        try {
            return new Request.Builder()
                    .url(uri.toURL())
                    .method(httpMethod.name(), RequestBody.create(new byte[0]))
                    .addHeader(HttpHeaders.ACCEPT, acceptHeaderValue)
                    .addHeader("cache-control", "no-cache")
                    .addHeader(HttpHeaders.AUTHORIZATION, credentials)
                    .addHeader("tenantId", nfvoConfig.getTenantId())
                    .build();
        } catch (MalformedURLException e) {
            throw new HttpClientException(e.getMessage(), e);
        }
    }

    private URI buildUriForNfvoAuthRequest() {
        return UriComponentsBuilder
                .fromHttpUrl(onboardingConfig.getHost())
                .path(NFVO_AUTH_PATH)
                .build().toUri();
    }
}
