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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.OperationRequestHandler;

/**
 * This class prepares to make the LCM operation request and updates the DB afterwards
 */
public interface LifeCycleManagementService {

    /**
     * Handle lifecycle operation request with values map
     * @param type
     * @param vnfInstanceId
     * @param request
     * @param requestUsername
     * @param valuesYamlMap
     * @return
     */
    String executeRequest(LifecycleOperationType type, String vnfInstanceId, Object request, String requestUsername,
                          Map<String, Object> valuesYamlMap);

    /**
     * Prepare to make a terminate request to remove resources associated with a release.
     * * the vnfInstanceId exists
     * * the vnfInstance is in a failed state
     *
     * @param vnfInstance
     * @param cleanupVnfRequest
     * @param requestUsername
     *
     * @return the occurrence id of this life cycle operation
     */
    String cleanup(VnfInstance vnfInstance, CleanupVnfRequest cleanupVnfRequest, String requestUsername);

    /**
     * Handle lifecycle operation request by instance and service with values map
     * @param service
     * @param vnfInstance
     * @param request
     * @param operation
     * @param valuesYamlMap
     * @param additionalParams
     * @return
     */
    String executeRequest(OperationRequestHandler service, VnfInstance vnfInstance, Object request, LifecycleOperation operation,
                          Map<String, Object> valuesYamlMap, Map<String, Object> additionalParams);
}
