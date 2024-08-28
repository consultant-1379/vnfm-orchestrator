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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.security.CustomX509TrustManager.TEMP_KEYSTORE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import io.fabric8.kubernetes.client.KubernetesClient;


@SpringBootTest(classes = { CertificateEventChangeDetector.class, CustomX509TrustManager.class })
@TestPropertySource(properties = { "truststore.path = ${java.home}/lib/security/cacerts" })
public class CertificateEventChangeDetectorTest {

    private static final String KEYSTORE_PASS = "changeit";

    @Autowired
    private CustomX509TrustManager customX509TrustManager;

    @MockBean
    private AbstractEnvironment environment;

    @MockBean
    private ConfigReloadProperties properties;

    @MockBean
    private KubernetesClient kubernetesClient;

    @SpyBean
    private CertificateEventChangeDetector detector;

    @Test
    public void onEvent() throws IOException, URISyntaxException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        String certs = readDataFromFile(getClass(), "change-detector-certificate.crt");
        detector.updateTrustManager(certs);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(Files.newInputStream(Paths.get(System.getProperty("java.io.tmpdir") + File.separator + TEMP_KEYSTORE)), KEYSTORE_PASS.toCharArray());

        byte[] certsBytes = Base64.getDecoder().decode(certs);
        ByteArrayInputStream bytes = new ByteArrayInputStream(certsBytes);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> x509certs = cf.generateCertificates(bytes);

        X509Certificate testCert = (X509Certificate) x509certs.iterator().next();
        String alias = ks.getCertificateAlias(testCert);
        assertFalse(Strings.isEmpty(alias), "Did not find cert in trustStore");
    }

    @Test
    public void testReSubscription() {

        //given
        doNothing().when(detector).subscribe();
        ReflectionTestUtils.setField(detector, "initialTimeout", 0L);

        //when WatchConnectionManager closes it calls this method through delegate e.g. Subscriber Impl
        detector.scheduleResubscribe();

        //then
        verify(detector, timeout(TimeUnit.SECONDS.toMillis(1)).atLeastOnce()).subscribe();
    }
}
