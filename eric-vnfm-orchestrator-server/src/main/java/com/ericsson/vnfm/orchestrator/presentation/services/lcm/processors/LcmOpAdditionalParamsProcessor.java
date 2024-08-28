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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Class is responsible for processing of additional params and values.yaml for LCM operations. It can process additional params with
 * simple and complex logic. It's needed for Dynamic Granting, especially for calculation of resources that user provided.
*/
public interface LcmOpAdditionalParamsProcessor {
    /**
     * Method is responsible for processing additional params for Downgrade and Self Upgrade operations.
     * 1. For Downgrade: it retrieves series of Upgrade or Self Upgrade operations which were completed before current Downgrade operation and merges
     * their parameters. In case of finding in series Self Upgrade operation it reuses logic which is described in second point.
     * 2. For Self Upgrade: it retrieves series of Upgrade or Self Upgrade operations which were completed before current Downgrade operation and
     * saves their parameters. In case of finding in series Downgrade operation it reuses which is described in first point
     * Here is implemented recursion calls to reuse logic in case of finding Downgrade or Self Upgrade operation.
     */
    void process(Map<String, Object> valuesYamlMap, String targetVnfdId, LifecycleOperation operation);

    /**
     * Method is responsible for processing additional params for LCM operation. It retrieves additional params from last successful operation and
     * set it into map.
     */
    Map<String, Object> processRaw(VnfInstance vnfInstance);
}
