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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoHealthService;

@DirtiesContext
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { CryptoServiceHealthIndicator.class, CryptoHealthService.class})
public class CryptoServiceHealthIndicatorTest {

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private CryptoHealthService cryptoHealthService;

    @Autowired
    private CryptoServiceHealthIndicator cryptoServiceHealthIndicator;

    @Test
    public void testHealthUpWhenCryptoIsUp() {
        Mockito.when(cryptoHealthService.isUp()).thenReturn(true);

        assertEquals(Status.UP, cryptoServiceHealthIndicator.health().getStatus());
    }

    @Test
    public void testHealthDownWhenCryptoIsDown() {
        Mockito.when(cryptoHealthService.isUp()).thenReturn(false);

        assertEquals(Status.DOWN, cryptoServiceHealthIndicator.health().getStatus());
    }

}