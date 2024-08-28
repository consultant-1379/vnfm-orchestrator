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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ONBOARDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ENM_INTEGRATION_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;

import java.util.EnumSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

@ExtendWith(MockitoExtension.class)
public class EnmIntegrationLicenseAddDeleteNodeInterceptorTest {

    private static final EnumSet<Permission> HARDCODED_PERMISSION_SET = EnumSet
            .of(ONBOARDING, LCM_OPERATIONS, ENM_INTEGRATION, CLUSTER_MANAGEMENT);

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @Mock
    private LicenseConsumerService licenseConsumerService;

    @Mock
    private VnfInstanceRepository vnfInstanceRepository;

    private EnmIntegrationLicenseAddDeleteNodeInterceptor enmIntegrationLicenseAddDeleteNodeInterceptor;

    @Test
    public void testRestrictedModeIsEnabled() {
        enmIntegrationLicenseAddDeleteNodeInterceptor = new EnmIntegrationLicenseAddDeleteNodeInterceptor(licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);
        when(licenseConsumerService.getPermissions()).thenReturn(HARDCODED_PERMISSION_SET);

        boolean result = enmIntegrationLicenseAddDeleteNodeInterceptor.preHandle(request, response, handler);
        verify(licenseConsumerService).getPermissions();
        assertTrue(result);
    }

    @Test
    public void testRestrictedModeIsDisabled() {
        enmIntegrationLicenseAddDeleteNodeInterceptor = new EnmIntegrationLicenseAddDeleteNodeInterceptor(licenseConsumerService, Boolean.FALSE, vnfInstanceRepository);

        boolean result = enmIntegrationLicenseAddDeleteNodeInterceptor.preHandle(request, response, handler);
        verifyNoInteractions(licenseConsumerService);
        assertTrue(result);
    }

    @Test
    public void testRestrictedModeWithSixInstancesWithoutLicense() {
        enmIntegrationLicenseAddDeleteNodeInterceptor = new EnmIntegrationLicenseAddDeleteNodeInterceptor(licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);

        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(vnfInstanceRepository.count()).thenReturn(6L);


        MissingLicensePermissionException exception = assertThrows(MissingLicensePermissionException.class,
                () -> enmIntegrationLicenseAddDeleteNodeInterceptor.preHandle(request, response, handler));
        verify(licenseConsumerService).getPermissions();
        assertEquals(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, ENM_INTEGRATION_LICENSE_TYPE), exception.getMessage());
    }

    @Test
    public void testRestrictedModeWithFourInstancesWithoutLicense() {
        enmIntegrationLicenseAddDeleteNodeInterceptor = new EnmIntegrationLicenseAddDeleteNodeInterceptor(licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);

        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(vnfInstanceRepository.count()).thenReturn(5L);

        boolean result = enmIntegrationLicenseAddDeleteNodeInterceptor.preHandle(request, response, handler);
        verify(licenseConsumerService).getPermissions();
        assertTrue(result);
    }
}