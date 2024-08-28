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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomX509TrustManager implements X509TrustManager {
    public static final String TEMP_KEYSTORE = "tempStore.jks";

    private Set<Certificate> addedCerts;
    private X509TrustManager trustManager;

    @Value("${truststore.path}")
    private String trustStorePath;

    private String tempTrustStorePath;

    @Value("${truststore.pass}")
    private String trustStorePass;

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

    @PostConstruct
    private void instantiateNewStore() {
        addedCerts = new HashSet<>();
        tempTrustStorePath = System.getProperty("java.io.tmpdir") + File.separator + TEMP_KEYSTORE;
        KeyStore ts = loadKeyStore(trustStorePath, trustStorePass);
        if (ts != null) {
            saveKeyStore(ts);
        }
    }

    private static KeyStore loadKeyStore(String keyStorePath, String keyStorePass) {
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            LOGGER.error("Failed to get Keystore instance", e);
        }
        try (InputStream inputStream = java.nio.file.Files.newInputStream(Paths.get(keyStorePath))) {
            if (trustStore != null) {
                trustStore.load(inputStream, keyStorePass.toCharArray());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load keystore file", e);
        }
        return trustStore;
    }

    private void saveKeyStore(KeyStore trustStore) {
        try (OutputStream outputStream = java.nio.file.Files.newOutputStream(Paths.get(tempTrustStorePath))) {
            trustStore.store(outputStream, trustStorePass.toCharArray());
        } catch (Exception e) {
            LOGGER.error("Failed to save keystore file", e);
        }
        LOGGER.info("Start reloading trust manager {} ", trustStorePath);

        reloadTrustManager(trustStore);
    }

    private void reloadTrustManager(KeyStore trustStore) {
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to get TrustManagerFactory instance", e);
        } catch (KeyStoreException e) {
            LOGGER.error("Failed to initialize the truststore", e);
        }

        if (tmf != null) {
            TrustManager[] tms = tmf.getTrustManagers();
            for (final TrustManager tm : tms) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;

                    LOGGER.info("Trust manager instance has been reloaded");
                }
            }
        }
    }

    public void addCertificates(Collection<? extends Certificate> certs) throws KeyStoreException {
        KeyStore trustStore = loadKeyStore(tempTrustStorePath, trustStorePass);
        if (trustStore != null) {
            for (Certificate cert : certs) {
                if (addedCerts.add(cert)) {
                    trustStore.setCertificateEntry(String.valueOf(UUID.randomUUID()), cert);
                } else {
                    LOGGER.warn("Duplicate certificate was accepted but not added");
                }
            }
            saveKeyStore(trustStore);

            LOGGER.info("Certificates are added and accepted successfully");

        }
    }
}