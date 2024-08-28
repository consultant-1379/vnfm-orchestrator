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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ONBOARDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CLUSTER_MANAGEMENT_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_WITHOUT_LICENSE_ATTRIBUTE;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;

import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClusterManagementLicenseInterceptorTest {

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
    private ClusterConfigFileRepository clusterConfigFileRepository;

    private ClusterManagementLicenseInterceptor clusterManagementLicenseInterceptor;

    @BeforeEach
    public void setUp() {
        when(request.getRequestURI()).thenReturn("/vnflcm/v1/clusterconfigs");
    }

    @Test
    public void testRestrictedModeIsEnabled() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.TRUE, clusterConfigFileRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(licenseConsumerService.getPermissions()).thenReturn(HARDCODED_PERMISSION_SET);

        boolean result = clusterManagementLicenseInterceptor.preHandle(request, response, handler);
        verify(licenseConsumerService).getPermissions();
        assertTrue(result);
    }

    @Test
    public void testRestrictedModeIsDisabled() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.FALSE, clusterConfigFileRepository);

        boolean result = clusterManagementLicenseInterceptor.preHandle(request, response, handler);
        verifyNoInteractions(licenseConsumerService);
        assertTrue(result);
    }

    @Test
    public void testRestrictedModeWithoutLicenseDeleteRequest() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.TRUE, clusterConfigFileRepository);

        when(request.getMethod()).thenReturn(HttpMethod.DELETE.name());
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));

        boolean result = clusterManagementLicenseInterceptor.preHandle(request, response, handler);

        verify(licenseConsumerService).getPermissions();
        verifyNoInteractions(clusterConfigFileRepository);
        assertTrue(result);
    }

    @Test
    public void testRestrictedModeWithTwoClustersWithoutLicensePostRequest() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.TRUE, clusterConfigFileRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(clusterConfigFileRepository.count()).thenReturn(2L);

        MissingLicensePermissionException exception = assertThrows(MissingLicensePermissionException.class,
                () -> clusterManagementLicenseInterceptor.preHandle(request, response, handler));
        verify(licenseConsumerService).getPermissions();
        assertEquals(String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE), exception.getMessage());
    }

    @Test
    public void testRestrictedModeWithOneClustersWithoutLicensePostRequest() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.TRUE, clusterConfigFileRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(clusterConfigFileRepository.count()).thenReturn(1L);

        assertThatCode(() -> clusterManagementLicenseInterceptor.preHandle(request, response, handler))
                .doesNotThrowAnyException();
        verify(licenseConsumerService).getPermissions();
        verify(request, times(1)).setAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE, true);
    }

    @Test
    public void testRestrictedModeWithOneClustersWithoutLicensePostRequestWithSlash() {
        clusterManagementLicenseInterceptor =
                new ClusterManagementLicenseInterceptor(licenseConsumerService, Boolean.TRUE, clusterConfigFileRepository);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getRequestURI()).thenReturn("/vnflcm/v1/clusterconfigs/");
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(clusterConfigFileRepository.count()).thenReturn(1L);

        assertThatCode(() -> clusterManagementLicenseInterceptor.preHandle(request, response, handler))
                .doesNotThrowAnyException();
        verify(licenseConsumerService).getPermissions();
        verify(request, times(1)).setAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE, true);
    }
}
