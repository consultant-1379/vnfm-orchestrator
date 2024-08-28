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
 * Service for working with ReplicaDetails.
 */
public interface ReplicaDetailsService {

    /**
     * Set replica details to helm charts without scale.
     *
     * @param descriptorModel - information about vnfInstance we are creating.
     * @param vnfInstance - created vnfInstance.
     */
    void setReplicaDetailsToVnfInstance(String descriptorModel, VnfInstance vnfInstance);

    /**
     * Set replica details to helm charts.
     *  @param descriptorModel - information about vnfInstance we are creating.
     * @param vnfInstance - created vnfInstance.
     * @param valuesYamlMap
     */
    void updateAndSetReplicaDetailsToVnfInstance(String descriptorModel, VnfInstance vnfInstance, Map<String, Object> valuesYamlMap);
}
