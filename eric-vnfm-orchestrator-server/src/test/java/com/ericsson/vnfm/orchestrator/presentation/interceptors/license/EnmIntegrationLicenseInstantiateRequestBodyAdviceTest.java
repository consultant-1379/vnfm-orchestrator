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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.model.license.Permission.ONBOARDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class EnmIntegrationLicenseInstantiateRequestBodyAdviceTest {

    private static final EnumSet<Permission> HARDCODED_PERMISSION_SET = EnumSet
            .of(ONBOARDING, LCM_OPERATIONS, ENM_INTEGRATION, CLUSTER_MANAGEMENT);

    private ObjectMapper mapper;

    private InstantiateVnfRequest body;

    private HttpInputMessage inputMessage;

    private MethodParameter parameter;

    private Type targetType;

    private Class<? extends HttpMessageConverter<?>> converterType;

    @Mock
    private LicenseConsumerService licenseConsumerService;

    @Mock
    private VnfInstanceRepository vnfInstanceRepository;

    private EnmIntegrationLicenseInstantiateRequestBodyAdvice enmIntegrationLicenseInstantiateRequestBodyAdvice;

    @Test
    public void testRestrictedModeIsEnabled() {
        enmIntegrationLicenseInstantiateRequestBodyAdvice =
                new EnmIntegrationLicenseInstantiateRequestBodyAdvice(mapper, licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);
        when(licenseConsumerService.getPermissions()).thenReturn(HARDCODED_PERMISSION_SET);
        final InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(Map.of(ADD_NODE_TO_OSS, Boolean.TRUE));

        Object result = enmIntegrationLicenseInstantiateRequestBodyAdvice.afterBodyRead(request, inputMessage, parameter, targetType, converterType);
        verify(licenseConsumerService).getPermissions();
        assertEquals(result, request);
    }

    @Test
    public void testRestrictedModeIsDisabled() {
        enmIntegrationLicenseInstantiateRequestBodyAdvice =
                new EnmIntegrationLicenseInstantiateRequestBodyAdvice(mapper, licenseConsumerService, Boolean.FALSE, vnfInstanceRepository);
        Object result = enmIntegrationLicenseInstantiateRequestBodyAdvice.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
        verifyNoInteractions(licenseConsumerService);
        assertEquals(result, body);
    }

    @Test
    public void testRestrictedModeWithFiveCnfWithoutLicense() {
        enmIntegrationLicenseInstantiateRequestBodyAdvice =
                new EnmIntegrationLicenseInstantiateRequestBodyAdvice(mapper, licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);
        final InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(Map.of(ADD_NODE_TO_OSS, Boolean.TRUE));

        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(vnfInstanceRepository.count()).thenReturn(6L);

        MissingLicensePermissionException exception = assertThrows(MissingLicensePermissionException.class,
                () -> enmIntegrationLicenseInstantiateRequestBodyAdvice.afterBodyRead(request, inputMessage, parameter, targetType, converterType));
        verify(licenseConsumerService).getPermissions();
        assertEquals(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, CommonConstants.ENM_INTEGRATION_LICENSE_TYPE), exception.getMessage());
    }

    @Test
    public void testRestrictedModeWithFourInstancesWithoutLicense() {
        enmIntegrationLicenseInstantiateRequestBodyAdvice =
                new EnmIntegrationLicenseInstantiateRequestBodyAdvice(mapper, licenseConsumerService, Boolean.TRUE, vnfInstanceRepository);
        final InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setAdditionalParams(Map.of(ADD_NODE_TO_OSS, Boolean.TRUE));

        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.noneOf(Permission.class));
        when(vnfInstanceRepository.count()).thenReturn(4L);

        Object result = enmIntegrationLicenseInstantiateRequestBodyAdvice.afterBodyRead(request, inputMessage, parameter, targetType, converterType);
        verify(licenseConsumerService).getPermissions();
        assertEquals(result, request);
    }
}
