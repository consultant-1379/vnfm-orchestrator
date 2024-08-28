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
package com.ericsson.vnfm.orchestrator.presentation.controllers.probes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoHealthService;

@Component
public class CryptoServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private CryptoHealthService cryptoHealthService;

    @Override
    public Health health() {
        Health.Builder health = new Health.Builder().up();

        if (cryptoHealthService.isUp()) {
            health.withDetail("Crypto service status", Status.UP);
        } else {
            health.down().withDetail("Crypto service status", Status.DOWN);
        }

        return health.build();
    }
}
