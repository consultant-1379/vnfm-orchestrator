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

public interface ReplicaCountCalculationService {
    Map<String, Integer> calculateFromVduInitialDelta(VnfInstance vnfInstance);

    Map<String, Integer> calculate(VnfInstance vnfInstance);

    String getResourceDetails(VnfInstance vnfInstance);
}
