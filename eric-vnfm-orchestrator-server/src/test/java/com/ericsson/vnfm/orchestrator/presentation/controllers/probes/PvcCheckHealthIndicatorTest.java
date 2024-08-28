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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DirtiesContext
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PvcCheckHealthIndicator.class })
public class PvcCheckHealthIndicatorTest {

    @Value("${healthCheckEnv.mountPaths.readWrite}")
    private String[] readWriteMountPaths;

    @Autowired
    private PvcCheckHealthIndicator pvcCheckHealthIndicator;

    @Test
    public void shouldPerformCheckOnEachConfiguredPath() {
        Health healthStatus = pvcCheckHealthIndicator.health();
        int mountsCount = readWriteMountPaths.length;

        assertEquals(mountsCount, healthStatus.getDetails().size());
        assertTrue(healthStatus.getDetails().keySet().containsAll(List.of(readWriteMountPaths)));
    }

}