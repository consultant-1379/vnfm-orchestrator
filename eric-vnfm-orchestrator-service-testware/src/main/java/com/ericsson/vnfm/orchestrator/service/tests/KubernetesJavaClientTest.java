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
package com.ericsson.vnfm.orchestrator.service.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import static com.ericsson.vnfm.orchestrator.service.tests.utils.Utils.delay;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.util.Config;
import io.qameta.allure.Epic;

public class KubernetesJavaClientTest {

    private static final Logger LOGGER = getLogger(KubernetesJavaClientTest.class);

    private static final String NAMESPACE = System.getProperty("namespace");

    private static final String SECRET_NAME = "eric-eo-evnfm-nfvo";
    private static final String CONFIG_MAP_NAME = "eric-eo-evnfm-nfvo-config";
    private static final String ECM_ADMIN = "ecmAdmin";
    private static final String NFVO_USERNAME = "nfvo.username";
    private static final String NFVO_PASSWORD = "nfvo.password";
    private static final String CLOUD_ADMIN_123 = "CloudAdmin123";
    private static final String ECM = "ECM";
    private static final String NFVO_TENANT_ID = "nfvo.tenantId";
    private static final String TLS_CRT = "tls.crt";
    private static final String SOME_DATA = "some-data";

    private static CoreV1Api coreV1Api = null;

    @BeforeAll
    public static void setup() throws IOException {
        ApiClient client;
        try {
            client = Config.defaultClient();    //CI flow sets $KUBECONFIG
        } catch (IOException e) {
            throw new IOException("Issue creating kubernetes java client, check $KUBECONFIG is set", e);
        }
        coreV1Api = new CoreV1Api(client);
    }

    @AfterAll
    public static void teardown() {
        deleteSecret();
        deleteConfigMap();
        delay(3000);    // Need to allow time for service to read config update and restart

        checkResourcesDoNotExistInNamespace();
    }


    @Epic("SLES Update")
    @Test
    @DisplayName("Check Kubernetes Java client works with SLES image")
    public void testClusterConnection() {

        LOGGER.info("Checking secret and configMap are not present in namespace before starting test");
        checkResourcesDoNotExistInNamespace();

        createSecret();
        createConfigMap();
        delay(3000);    // Need to allow time for service to read config update and restart

        LOGGER.info("Checking secret and configMap are present as expected");
        checkResourcesExistInNamespace();
    }

    private static void createSecret() {
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(SECRET_NAME);

        Map<String, String> secretData = new HashMap<>();
        secretData.put(NFVO_USERNAME, ECM_ADMIN);
        secretData.put(NFVO_PASSWORD, CLOUD_ADMIN_123);
        secretData.put(NFVO_TENANT_ID, ECM);
        secretData.put(TLS_CRT, "");

        V1Secret secret = new V1Secret().type("generic")
                .metadata(v1ObjectMeta)
                .stringData(secretData);

        try {
            LOGGER.info("Creating NFVO secret");
            coreV1Api.createNamespacedSecret(NAMESPACE, secret).execute();
        } catch (ApiException e) {
            LOGGER.error("Failed to create secret", e);
        }
    }

    private static void deleteSecret() {
        try {
            LOGGER.info("Deleting NFVO secret");
            coreV1Api.deleteNamespacedSecret(SECRET_NAME, NAMESPACE).execute();
        } catch (ApiException e) {
            LOGGER.error("Failed to delete secret", e);
        }
    }

    private static void createConfigMap() {
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(CONFIG_MAP_NAME);

        Map<String, String> data = new HashMap<>();
        data.put("application.yaml", SOME_DATA);

        V1ConfigMap configMap = new V1ConfigMap()
                .metadata(v1ObjectMeta)
                .data(data);
        try {
            LOGGER.info("Creating NFVO configmap");
            coreV1Api.createNamespacedConfigMap(NAMESPACE, configMap).execute();
        } catch (ApiException e) {
            LOGGER.error("Failed to create configmap", e);
        }
    }

    private static void deleteConfigMap() {
        try {
            LOGGER.info("Deleting NFVO configmap");
            coreV1Api.deleteNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE).execute();
        } catch (ApiException e) {
            LOGGER.error("Failed to delete configmap", e);
        }
    }

    private static void checkResourcesExistInNamespace() {
        assertThat(isConfigMapPresentInNamespace(CONFIG_MAP_NAME))
                .withFailMessage("Configmap missing in namespace when it is expected to be present")
                .isTrue();
        assertThat(isSecretPresentInNamespace(SECRET_NAME))
                .withFailMessage("Secret missing in namespace when it is expected to be present")
                .isTrue();
    }

    private static void checkResourcesDoNotExistInNamespace() {
        assertThat(isConfigMapPresentInNamespace(CONFIG_MAP_NAME))
                .withFailMessage("Configmap is present in namespace when it shouldn't be")
                .isFalse();
        assertThat(isSecretPresentInNamespace(SECRET_NAME))
                .withFailMessage("Secret is present in namespace when it shouldn't be")
                .isFalse();
    }

    private static boolean isSecretPresentInNamespace(final String secretName) {
        try {
            V1SecretList secretList =
                    coreV1Api.listNamespacedSecret(NAMESPACE).allowWatchBookmarks(false).watch(false).execute();
            LOGGER.info("Check if secret exists in namespace");
            for (V1Secret item : secretList.getItems()) {
                if (item.getMetadata().getName().equals(secretName)) {
                    return true;
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Failed to get secret", e);
        }
        return false;
    }

    private static boolean isConfigMapPresentInNamespace(final String configMapName) {
        try {
            V1ConfigMapList configMapList =
                    coreV1Api.listNamespacedConfigMap(NAMESPACE).allowWatchBookmarks(false).watch(false).execute();
            LOGGER.info("Check if configMap exists in namespace");
            for (V1ConfigMap item : configMapList.getItems()) {
                if (item.getMetadata().getName().equals(configMapName)) {
                    return true;
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Failed to get configMap", e);
        }
        return false;
    }
}
