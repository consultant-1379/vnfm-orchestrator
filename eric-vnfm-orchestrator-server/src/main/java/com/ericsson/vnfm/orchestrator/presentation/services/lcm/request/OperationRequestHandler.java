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

import java.nio.file.Path;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public interface OperationRequestHandler {

    /**
     * The type of service
     *
     * @return
     */
    LifecycleOperationType getType();

    /**
     * Carry out validation which is specific to the operation
     *
     * @param vnfInstance
     * @param request
     */
    void specificValidation(VnfInstance vnfInstance, Object request);

    /**
     * Update the instance with any information relevant to the operation
     *
     * @param vnfInstance
     * @param request
     * @param type
     * @param operation
     * @param additionalParams
     */
    void updateInstance(VnfInstance vnfInstance, Object request, LifecycleOperationType type,
                        LifecycleOperation operation, Map<String, Object> additionalParams);

    /**
     * Send the request specific to the operation to the WFS & verify it's accepted.
     *
     * @param vnfInstance
     * @param operation
     * @param request
     * @param toValuesFile
     */
    void sendRequest(VnfInstance vnfInstance, LifecycleOperation operation, Object request,
                     Path toValuesFile);

    /**
     * Format additional parameters
     *
     * @param vnfInstance
     * @param type
     * @param request
     * @param valuesYamlMap
     *
     * @return
     */
    Map<String, Object> formatParameters(VnfInstance vnfInstance, Object request, LifecycleOperationType type,
                                         Map<String, Object> valuesYamlMap);

    /**
     * Create and persist the Lifecycle Operation to the database
     *
     * @param vnfInstance
     * @param request
     * @param requestUsername
     * @param type
     * @param valuesYamlMap
     *
     * @param applicationTimeout
     * @return
     */
    LifecycleOperation persistOperation(VnfInstance vnfInstance, Object request, String requestUsername,
                                        LifecycleOperationType type, Map<String, Object> valuesYamlMap, String applicationTimeout);

    /**
     * Sets the extensions and Instantiation Level in lifecycle operation in temp instance
     *
     * @param vnfInstance
     * @param operations
     */
    void setExtensionsAndInstantiationLevelInOperationInTempInstance(VnfInstance vnfInstance, LifecycleOperation operations);

    /**
     * Sets the extensions and Instantiation Level in lifecycle operation in current instance
     *
     * @param vnfInstance
     * @param operations
     */
    void setExtensionsAndInstantiationLevelInOperationInCurrentInstance(VnfInstance vnfInstance, LifecycleOperation operations);

    /**
     * Persist the Lifecycle Operation and Vnf Instance to the database after sending request to WFS
     *
     * @param vnfInstance
     * @param operation
     */
    void persistOperationAndInstanceAfterExecution(VnfInstance vnfInstance, LifecycleOperation operation);

    /**
     * Creates tempInstance
     *
     * @param vnfInstance
     * @param request
    */
    default void createTempInstance(VnfInstance vnfInstance, Object request) {
    }


    /**
     * Updated cluster config status
     *
     * @param vnfInstance
     */
    default void updateClusterConfigStatus(VnfInstance vnfInstance){

    }

    void processValuesYaml(Map<String, Object> valuesYamlMap,
                           VnfInstance vnfInstance,
                           Object request,
                           LifecycleOperation operation);
}
