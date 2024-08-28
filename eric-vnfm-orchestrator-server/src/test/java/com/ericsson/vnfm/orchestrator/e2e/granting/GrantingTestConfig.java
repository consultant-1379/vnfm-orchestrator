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
package com.ericsson.vnfm.orchestrator.e2e.granting;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.ericsson.vnfm.orchestrator.infrastructure.model.RetryProperties;
import com.github.tomakehurst.wiremock.WireMockServer;

@TestConfiguration
public class GrantingTestConfig {

    @Bean(destroyMethod = "stop")
    @Qualifier("nfvoMockServer")
    public WireMockServer nfvoMockServer() {
        WireMockServer mockServer = new WireMockServer(wireMockConfig().dynamicPort());
        mockServer.start();
        return mockServer;
    }

    @Bean
    @Primary
    public RetryProperties testRetryProperties() {
        RetryProperties retryProperties = new RetryProperties();
        retryProperties.setReadTimeout(12000L);
        return retryProperties;
    }
}
