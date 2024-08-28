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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class CryptoHealthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CryptoHealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new CryptoHealthService("crypto-host", restTemplate);
    }

    @Test
    void isUpShouldReturnTrueWhenCryptoHealthReports2xx() {
        // given
        when(restTemplate.exchange(eq("crypto-host/actuator/health"), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when and then
        assertThat(healthService.isUp()).isTrue();
    }
}
