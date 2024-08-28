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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultReplicaDetailsCalculationService {

    @Autowired
    private VnfInstanceService vnfInstanceService;

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private ReplicaDetailsBuilder replicaDetailsBuilder;

    public Map<String, ReplicaDetails> calculate(final VnfInstance vnfInstance) {
        LOGGER.info("Setting default Replica details");
        Map<String, Integer> currentReplicaCounts =
                replicaCountCalculationService.calculate(vnfInstance);
        Map<String, ReplicaDetails> replicaDetailsMap = replicaDetailsBuilder.buildDefaultReplicaDetails(vnfInstance, currentReplicaCounts);
        return Optional.ofNullable(replicaDetailsMap)
                .filter(MapUtils::isNotEmpty)
                .map(replicaDetails -> updateMinAndMaxReplicaIfExtensionIsPresent(vnfInstance, replicaDetails))
                .orElse(new HashMap<>());
    }

    private Map<String, ReplicaDetails> updateMinAndMaxReplicaIfExtensionIsPresent(final VnfInstance vnfInstance,
                                                            Map<String, ReplicaDetails> replicaDetailsMap) {
        if (vnfInstanceService.isVnfControlledScalingExtensionPresent(vnfInstance)) {
            replicaDetailsMap.forEach((target, replicaDetails) -> {
                replicaDetails.setMaxReplicasCount(replicaDetails.getCurrentReplicaCount());
                int minReplicaCount = calculateMinReplicasCount(vnfInstance, target, replicaDetails);
                replicaDetails.setMinReplicasCount(minReplicaCount);
            });
        }
        return replicaDetailsMap;
    }

    private Integer calculateMinReplicasCount(VnfInstance vnfInstance, String replicaName, ReplicaDetails replicaDetails) {
        return replicaDetails.getAutoScalingEnabledValue().equals(Boolean.TRUE) ?
                scaleService.getMinReplicasCountFromVduInitialDelta(vnfInstance, replicaName) : replicaDetails.getMinReplicasCount();
    }
}
