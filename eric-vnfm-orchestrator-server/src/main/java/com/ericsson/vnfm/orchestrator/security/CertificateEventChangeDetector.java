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
package com.ericsson.vnfm.orchestrator.security;

import static io.fabric8.kubernetes.client.Watcher.Action.ADDED;
import static io.fabric8.kubernetes.client.Watcher.Action.MODIFIED;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.kubernetes.commons.config.SecretsPropertySource;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.core.env.AbstractEnvironment;

import com.google.common.collect.Maps;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CertificateEventChangeDetector extends ChangeDetector {

    private final CustomX509TrustManager trustManager;

    public CertificateEventChangeDetector(AbstractEnvironment environment,
                                          KubernetesClient kubernetesClient,
                                          ConfigReloadProperties properties,
                                          CustomX509TrustManager trustManager) {
        super(environment, kubernetesClient, properties);
        this.trustManager = trustManager;
    }

    @Override
    public void subscribe() {
        if (properties.monitoringSecrets()) {
            LOGGER.info("NBI certificate event detector is ENABLED");

            final String detectorName = this.toString();
            kubernetesClient.secrets().watch(new Subscriber<>(detectorName) {
                @Override
                public void eventReceived(final Action action, final Secret secret) {
                    onSecret(action, secret);
                }
            });
        }
    }

    private void onSecret(final Action action, final Secret secret) {
        String sourceName = getSourceName(secret.getMetadata().getName(), secret.getMetadata().getNamespace());
        findPropertySources(SecretsPropertySource.class).forEach(item -> {

            if (itemIsNfvoWithCorrectAction(item, sourceName, action)) {
                LOGGER.info("Detected change in secrets {} adding certificates", sourceName);
                String certs = Optional.ofNullable(secret.getData()).orElseGet(Maps::newHashMap).get("tls.crt");
                if (certs != null) {
                    try {
                        updateTrustManager(certs);
                        LOGGER.info("The action secret {} accepted with is {}", sourceName, action);
                    } catch (Exception e) {
                        LOGGER.error("Failed to add certificates to trust manager", e);
                    }
                } else {
                    LOGGER.warn("No certificate were found in secret {}", sourceName);
                }
            } else {
                LOGGER.debug("Detected change in secrets, skipped due to :: {} didn't match criteria", sourceName);
            }
        });
    }

    private static boolean itemIsNfvoWithCorrectAction(SecretsPropertySource item, String sourceName, Action action) {
        var envSourceName = StringUtils.substringAfter(item.getName(), ".");
        return sourceName.equals(envSourceName) && (ADDED.equals(action) || MODIFIED.equals(action));
    }

    private static String getSourceName(String name, String namespace) {
        return "%s.%s".formatted(name, namespace);
    }

    void updateTrustManager(final String certs) throws CertificateException, KeyStoreException, IOException {
        byte[] certsBytes = Base64.getDecoder().decode(certs);
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(certsBytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> x509certs = cf.generateCertificates(bytes);
            trustManager.addCertificates(x509certs);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
