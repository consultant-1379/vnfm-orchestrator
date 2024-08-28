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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevels;
import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevelsDataInfo;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.VduInstantiationLevels;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Service for working with InstantiationLevels.
 */
public interface InstantiationLevelService {

    /**
     * Set default instantiationLevel of VnfInstance.
     *
     * @param vnfInstance - created vnfInstance.
     */
    void setDefaultInstantiationLevelToVnfInstance(VnfInstance vnfInstance);

    /**
     * Returns InstantiationLevelsDataInfo object matching given level.
     *
     * @param policies Policies object
     * @param level Instantiation level
     * @return InstantiationLevelsDataInfo if present in Policies empty if not present
     */
    Optional<InstantiationLevelsDataInfo> getMatchingInstantiationLevel(Policies policies, String level);

    /**
     * Returns the default Instantiation level from Policies object
     *
     * @param vnfInstancePolicies Policies object
     * @return Default Instantiation level
     */
    String getDefaultInstantiationLevelFromPolicies(Policies vnfInstancePolicies);

    /**
     * Returns VduInstantiationLevels object matching given level
     *
     * @param policies Policies object
     * @param level Instantiation level
     * @return List of VduInstantiationLevels objects
     */
    List<VduInstantiationLevels> getMatchingVduInstantiationLevels(Policies policies, String level);

    /**
     * Sets Instantiation level from request or the default level from policies
     *
     *  @param vnfInstance vnf instance to instantiate
     * @param level request
     */
    void validateInstantiationLevelInPoliciesOfVnfInstance(VnfInstance vnfInstance, String level);

    /**
     * Sets the scale level based on the instantiation level provided
     *
     * @param vnfInstance Instance to update
     * @param instantiationLevel Instantiation level in request or default level from vnfd
     * @param vnfInstancePolicies Policies of vnfInstance
     */
    void setScaleLevelForInstantiationLevel(VnfInstance vnfInstance,
                                            String instantiationLevel,
                                            Policies vnfInstancePolicies);

    /**
     * Populates scaleLevel values for aspects from the "targetScaleLevelInfo" request param. If the scaleLevel value
     * is not present in the request, the default value will be set.
     *
     * @param vnfInstance Instance to update
     * @param targetScaleLevelInfo targetScaleLevelInfo from the request
     * @param vnfInstancePolicies Policies of vnfInstance
     */
    void setScaleLevelForTargetScaleLevelInfo(VnfInstance vnfInstance,
                                              List<ScaleInfo> targetScaleLevelInfo,
                                              Policies vnfInstancePolicies);

    /**
     * Returns all instantiation levels from provided vnfInstance
     *
     * @param vnfInstance Instance to get levels from
     * @return Map of all instantiation levels
     */
    Map<String, InstantiationLevels> getInstantiationLevelsFromPolicies(VnfInstance vnfInstance);
}
