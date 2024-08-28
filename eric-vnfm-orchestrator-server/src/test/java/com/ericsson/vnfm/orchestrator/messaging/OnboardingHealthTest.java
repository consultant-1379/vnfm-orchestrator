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
package com.ericsson.vnfm.orchestrator.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;


@SpringBootTest(classes = { OnboardingHealth.class, OnboardingConfig.class })
public class OnboardingHealthTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private OnboardingHealth onboardingHealth;

    @Test
    public void shouldReturnFalseWhenExceptionOccurs() {
        // given
        given(restTemplate.exchange(any(), any(), any(), eq(String.class))).willThrow(new RuntimeException());

        // when and then
        assertThat(onboardingHealth.isUp()).isFalse();
    }

    @Test
    public void shouldReturnFalseWhenStatusIsNot2xx() {
        // given
        given(restTemplate.exchange(any(), any(), any(), eq(String.class))).willReturn(ResponseEntity.badRequest().build());

        // when and then
        assertThat(onboardingHealth.isUp()).isFalse();
    }

    @Test
    public void shouldReturnTrueWhenStatusIs2xx() {
        // given
        given(restTemplate.exchange(any(), any(), any(), eq(String.class))).willReturn(ResponseEntity.ok().build());

        // when and then
        assertThat(onboardingHealth.isUp()).isTrue();
    }
}
