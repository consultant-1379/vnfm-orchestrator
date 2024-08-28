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

import static java.lang.String.format;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ENM_INTEGRATION_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.VALIDATION_STARTED_LOG;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * A component that checks ENM integration license permission for Add/Delete node operations.
 */
@Component
@Slf4j
public class EnmIntegrationLicenseAddDeleteNodeInterceptor implements HandlerInterceptor {

    private final LicenseConsumerService licenseConsumerService;

    private final boolean restrictedMode;

    private final VnfInstanceRepository vnfInstanceRepository;

    public EnmIntegrationLicenseAddDeleteNodeInterceptor(final LicenseConsumerService licenseConsumerService,
                                                         @Value("${orchestrator.restrictedMode:true}") final boolean restrictedMode,
                                                         final VnfInstanceRepository vnfInstanceRepository) {
        this.licenseConsumerService = licenseConsumerService;
        this.restrictedMode = restrictedMode;
        this.vnfInstanceRepository = vnfInstanceRepository;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        LOGGER.debug(format(VALIDATION_STARTED_LOG, this.getClass().getSimpleName()));

        if (restrictedMode && !licenseConsumerService.getPermissions().contains(ENM_INTEGRATION)) {
            int numberOfPackages = (int) vnfInstanceRepository.count();

            if (numberOfPackages > ALLOWED_NUMBER_OF_INSTANCES) {
                throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, ENM_INTEGRATION_LICENSE_TYPE));
            }
        }

        return true;
    }
}
