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

import static com.ericsson.vnfm.orchestrator.model.license.Permission.CLUSTER_MANAGEMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_CLUSTERS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CLUSTER_MANAGEMENT_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_WITHOUT_LICENSE_ATTRIBUTE;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ClusterManagementLicenseInterceptor implements HandlerInterceptor {


    private static final List<String> METHODS_TO_INTERCEPT = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

    private static final Set<String> ADDING_ENTITY_PATHS = Set.of("/vnflcm/v1/clusterconfigs", "/vnflcm/v1/clusterconfigs/");

    private final LicenseConsumerService licenseConsumerService;


    private final ClusterConfigFileRepository clusterConfigFileRepository;

    private final boolean restrictedMode;

    public ClusterManagementLicenseInterceptor(final LicenseConsumerService licenseConsumerService,
                                               @Value("${orchestrator.restrictedMode:true}") final boolean restrictedMode,
                                               final ClusterConfigFileRepository clusterConfigFileRepository) {
        this.licenseConsumerService = licenseConsumerService;
        this.restrictedMode = restrictedMode;
        this.clusterConfigFileRepository = clusterConfigFileRepository;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

        if (restrictedMode && METHODS_TO_INTERCEPT.contains(request.getMethod())
                && !licenseConsumerService.getPermissions().contains(CLUSTER_MANAGEMENT)) {
            checkOrchestratorLimitsWithoutLicense(request);
        }

        return true;
    }

    private void checkOrchestratorLimitsWithoutLicense(HttpServletRequest request) {
        if (HttpMethod.DELETE.matches(request.getMethod())) {
            return;
        }

        int numberOfClusters = (int) clusterConfigFileRepository.count();

        if (HttpMethod.POST.matches(request.getMethod()) && ADDING_ENTITY_PATHS.contains(request.getRequestURI())) {
            if (numberOfClusters >= ALLOWED_NUMBER_OF_CLUSTERS) {
                throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE));
            } else {
                request.setAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE, true);
            }
        } else if (numberOfClusters > ALLOWED_NUMBER_OF_CLUSTERS) {
            throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE));
        }
    }
}
