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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockPropertySource;

import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.github.tomakehurst.wiremock.WireMockServer;

@TestConfiguration
public class LifecycleManagementServiceTestConfig {

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public WireMockServer mockServer() {
        WireMockServer mockServer = new WireMockServer(wireMockConfig().dynamicPort());
        mockServer.start();
        return mockServer;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowRoutingServicePassThrough workflowRoutingService(
            WireMockServer mockServer, ConfigurableApplicationContext applicationContext) {
        MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
        MockPropertySource mockEnvVars = new MockPropertySource().withProperty("workflow.host", "localhost:" + mockServer.port());
        propertySources.addFirst(mockEnvVars);
        return new WorkflowRoutingServicePassThrough();
    }
}
