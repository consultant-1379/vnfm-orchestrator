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
package com.ericsson.vnfm.orchestrator.presentation.services.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.license.Permission;

@ExtendWith(MockitoExtension.class)
public class LicenseConsumerServiceImplTest {

    private static final EnumSet<Permission> NO_PERMISSIONS = EnumSet.noneOf(Permission.class);

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private RetryTemplate retryTemplate;

    @InjectMocks
    private LicenseConsumerServiceImpl licenseConsumerService;

    @Test
    @SuppressWarnings("unchecked")
    public void fetchPermissionsTestWithRestException() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).willThrow(RestClientException.class);
        assertThatThrownBy(() -> licenseConsumerService.fetchPermissions()).isInstanceOf(RestClientException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fetchPermissionsTestWithNull() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .willReturn(new ResponseEntity<Permission[]>(HttpStatus.OK));

        assertThat(licenseConsumerService.fetchPermissions()).isEqualTo(NO_PERMISSIONS);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fetchPermissionsTestWithEmptyBody() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class))).
                willReturn(new ResponseEntity<>(new Permission[]{}, HttpStatus.OK));

        assertThat(licenseConsumerService.fetchPermissions()).isEqualTo(NO_PERMISSIONS);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fetchPermissionsTestWithSuccess() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .willReturn(new ResponseEntity<>(new Permission[]{LCM_OPERATIONS, ENM_INTEGRATION}, HttpStatus.ACCEPTED));

        assertThat(licenseConsumerService.fetchPermissions()).isEqualTo(EnumSet.of(LCM_OPERATIONS, ENM_INTEGRATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPermissionsTestWithSuccess() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .willReturn(new ResponseEntity<>(new Permission[]{LCM_OPERATIONS, ENM_INTEGRATION},
                                                 HttpStatus.ACCEPTED));

        assertThat(licenseConsumerService.getPermissions()).isEqualTo(EnumSet.of(LCM_OPERATIONS, ENM_INTEGRATION));
    }

    @Test
    public void getPermissionsTestWithException() {
        given(restTemplate.getForEntity(any(String.class), any(Class.class)))
                .willThrow(new RestClientException("Unable to retrieve license manager data due to: 500"));
        assertThat(licenseConsumerService.getPermissions()).isEqualTo(EnumSet.noneOf(Permission.class));
    }
}
