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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;

public interface GrantingRequestHandler {
    /**
     * Creates and sends granting request for Rel 4 packages
     *
     * @param operation
     * @param request
     * */
    void verifyGrantingResources(LifecycleOperation operation, Object request, Map<String, Object> valuesYamlMap);
}
