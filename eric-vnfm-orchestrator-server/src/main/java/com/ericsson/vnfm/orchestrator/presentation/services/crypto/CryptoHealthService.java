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
package com.ericsson.vnfm.orchestrator.presentation.services.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.utils.ExternalHealthCheckUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CryptoHealthService {

    private static final String CRYPTO_ACTUATOR_HEALTH_URL = "%s/actuator/health";

    private final String cryptoHost;

    private final RestTemplate restTemplate;

    public CryptoHealthService(@Value("${crypto.host}") String cryptoHost, RestTemplate restTemplate) {
        this.cryptoHost = cryptoHost;
        this.restTemplate = restTemplate;
    }

    public boolean isUp() {
        String healthCheckUrl = String.format(CRYPTO_ACTUATOR_HEALTH_URL, cryptoHost);
        LOGGER.debug("Checking health of crypto service for url {}", healthCheckUrl);
        return ExternalHealthCheckUtil.checkHealth(healthCheckUrl, restTemplate, "Crypto service");
    }
}
