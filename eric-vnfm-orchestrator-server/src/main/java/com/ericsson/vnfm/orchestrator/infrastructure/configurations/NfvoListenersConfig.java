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

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;

import com.ericsson.vnfm.orchestrator.security.CertificateEventChangeDetector;
import com.ericsson.vnfm.orchestrator.security.ChangeDetector;
import com.ericsson.vnfm.orchestrator.security.CustomX509TrustManager;

import io.fabric8.kubernetes.client.KubernetesClient;
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
public class NfvoListenersConfig {

    @Bean
    public ChangeDetector nfvoSecretChangeDetector(AbstractEnvironment environment,
                                                   KubernetesClient client,
                                                   ConfigReloadProperties reloadProperties,
                                                   CustomX509TrustManager trustManager) {
        return new CertificateEventChangeDetector(environment, client, reloadProperties, trustManager);
    }
}
