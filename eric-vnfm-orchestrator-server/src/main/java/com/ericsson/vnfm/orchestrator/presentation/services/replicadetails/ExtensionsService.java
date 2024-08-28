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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Service for working with Extensions.
 */
public interface ExtensionsService {

    /**
     * Validate Vnf Controlled Scaling extension provided in request
     *
     * @param extensions extensions from request
     * @param policies   Policies as string to validate the extensions against
     */
    void validateVnfControlledScalingExtension(Map<String, Object> extensions, String policies);

    /**
     * Validate Deployable Modules extension provided in request
     *
     * @param extensions extensions from request
     * @param vnfInstance vnfInstance
     * @param vnfd vnfd
     */
    void validateDeployableModulesExtension(Map<String, Object> extensions, VnfInstance vnfInstance, String vnfd);

    /**
     * Set the default extensions defined in the vnfd
     *
     * @param vnfInstance instance to set extensions
     * @param descriptorModel json string of the vnf descriptor
     */
    void setDefaultExtensions(VnfInstance vnfInstance, String descriptorModel);

    /**
     * Method will combine extensions provided in a request object with current extensions
     * The request extensions will override the current extensions where aspects are matching
     *
     * @param requestExtensions extensions provided in request
     * @param instance current instance in which extensions will be updated
     */
    void updateInstanceWithExtensionsInRequest(Map<String, Object> requestExtensions, VnfInstance instance);
}
