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

import static com.ericsson.vnfm.orchestrator.model.license.Permission.LCM_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.LCM_OPERATIONS_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_WITHOUT_LICENSE_ATTRIBUTE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.VALIDATION_STARTED_LOG;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LcmOperationsLicenseInterceptor implements HandlerInterceptor {

    private static final List<String> METHODS_TO_INTERCEPT = Arrays.asList("POST", "PATCH", "DELETE", "PUT");

    private static final Set<String> ADDING_ENTITY_PATHS = Set.of("/vnflcm/v1/vnf_instances", "/vnflcm/v1/vnf_instances/");

    private static final String VNF_INSTANCE_PATH_REGEX = "/vnflcm/v1/vnf_instances/[\\w-]+";
    private static final Pattern DELETE_INSTANCE_PATH_PATTERN = Pattern.compile("^" + VNF_INSTANCE_PATH_REGEX + "/?$");
    private static final Pattern TERMINATE_INSTANCE_PATH_PATTERN = Pattern.compile("^" + VNF_INSTANCE_PATH_REGEX + "/terminate$");
    private static final Pattern CLEANUP_INSTANCE_PATH_PATTERN = Pattern.compile("^" + VNF_INSTANCE_PATH_REGEX + "/cleanup$");

    private final LicenseConsumerService licenseConsumerService;

    private final VnfInstanceRepository vnfInstanceRepository;

    private final boolean restrictedMode;

    public LcmOperationsLicenseInterceptor(final LicenseConsumerService licenseConsumerService,
                                           @Value("${orchestrator.restrictedMode:true}") final boolean restrictedMode,
                                           final VnfInstanceRepository vnfInstanceRepository) {
        this.licenseConsumerService = licenseConsumerService;
        this.restrictedMode = restrictedMode;
        this.vnfInstanceRepository = vnfInstanceRepository;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) {
        LOGGER.debug(format(VALIDATION_STARTED_LOG, this.getClass().getSimpleName()));

        if (restrictedMode && METHODS_TO_INTERCEPT.contains(request.getMethod())) {
            EnumSet<Permission> allowedPrivileges = licenseConsumerService.getPermissions();

            if (!allowedPrivileges.contains(LCM_OPERATIONS)) {
                checkOrchestratorLimitsWithoutLicense(request);
            }
        }
        return true;
    }

    public void checkOrchestratorLimitsWithoutLicense(HttpServletRequest request) {
        if (isDeleteResourceRequest(request)) {
            return;
        }

        int numberOfPackages = (int) vnfInstanceRepository.count();

        if (HttpMethod.POST.matches(request.getMethod()) && ADDING_ENTITY_PATHS.contains(request.getRequestURI())) {
            if (numberOfPackages >= ALLOWED_NUMBER_OF_INSTANCES) {
                throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, LCM_OPERATIONS_LICENSE_TYPE));
            } else {
                request.setAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE, true);
            }
        } else if (numberOfPackages > ALLOWED_NUMBER_OF_INSTANCES) {
            throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, LCM_OPERATIONS_LICENSE_TYPE));
        }
    }

    private static boolean isDeleteResourceRequest(final HttpServletRequest request) {
        final String method = request.getMethod();
        final String requestURI = request.getRequestURI();

        final boolean isDeleteInstanceRequest = HttpMethod.DELETE.matches(method)
                && DELETE_INSTANCE_PATH_PATTERN.matcher(requestURI).matches();
        final boolean isTerminateInstanceRequest = HttpMethod.POST.matches(method)
                && TERMINATE_INSTANCE_PATH_PATTERN.matcher(requestURI).matches();
        final boolean isCleanupInstanceRequest = HttpMethod.POST.matches(method)
                && CLEANUP_INSTANCE_PATH_PATTERN.matcher(requestURI).matches();

        return isDeleteInstanceRequest || isTerminateInstanceRequest || isCleanupInstanceRequest;
    }
}
