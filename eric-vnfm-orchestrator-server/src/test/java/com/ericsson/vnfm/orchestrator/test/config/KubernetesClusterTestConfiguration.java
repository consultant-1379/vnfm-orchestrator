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
package com.ericsson.vnfm.orchestrator.test.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.fabric8.kubernetes.client.Config;

/**
 * This test configuration ensures that the K8s cluster configuration is set via system variables in tests so that migrations do not depend on K8s
 * cluster configuration located elsewhere (for example at $HOME/.kube/config).
 */
@TestConfiguration
public class KubernetesClusterTestConfiguration {

    /**
     * Sets system variables that {@link db.migration.V60_1__UploadDefaultConfigFile} is interested in (through K8s client) before migrations are
     * executed.
     */
    @Bean
    public FlywayConfigurationCustomizer dummyFlywayConfigurationCustomizer() {
        return config -> {
            System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, "http://k8s-master");
            System.setProperty(Config.KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY, "token");
        };
    }
}
