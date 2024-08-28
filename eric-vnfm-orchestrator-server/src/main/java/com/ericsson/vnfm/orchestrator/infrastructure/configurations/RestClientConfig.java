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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import brave.Tracing;
import brave.spring.web.TracingClientHttpRequestInterceptor;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.infrastructure.model.RetryProperties;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.security.CustomX509TrustManager;

import okhttp3.OkHttpClient;

@Configuration
public class RestClientConfig {

    private RetryProperties retryProperties;

    @Autowired
    public RestClientConfig(final RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder, Tracing tracing) {
        RestTemplate restTemplate = builder.build();
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(TracingClientHttpRequestInterceptor.create(tracing));
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    @Bean
    @Qualifier("licenseRestTemplate")
    public RestTemplate licenseRestTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofMillis(retryProperties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(retryProperties.getDefaultProperties().getRequestTimeout()))
                .build();
    }

    @Bean
    @Qualifier("noSslRestTemplate")
    public RestTemplate noSslRestTemplate(RestTemplateBuilder builder) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        var sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        var csf = new SSLConnectionSocketFactory(sslContext);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(csf)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy())
                .setConnectionManager(connectionManager)
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setHttpClient(httpClient);
        return builder.requestFactory(() -> requestFactory).build();
    }

    @Bean
    @Qualifier("nfvoRestConfiguration")
    public RestTemplate nfvoRestConfiguration(CustomX509TrustManager trustManager) {

        SSLSocketFactory sslSocketFactory;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {
                trustManager
            }, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new InternalRuntimeException("Unable to process SSL context", e);
        }

        final OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();

        final OkHttp3ClientHttpRequestFactory okHttp3ClientHttpRequestFactory =
                new OkHttp3ClientHttpRequestFactory(okHttpClient);
        return new RestTemplate(okHttp3ClientHttpRequestFactory);
    }

    @Bean
    public OkHttpClient okHttpClient(CustomX509TrustManager trustManager, RetryProperties retryProperties) {
        SSLSocketFactory sslSocketFactory;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManager}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new InternalRuntimeException("Unable to process SSL context", e);
        }

        return new OkHttpClient().newBuilder()
                .readTimeout(Duration.ofMillis(retryProperties.getReadTimeout()))
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();
    }
}
