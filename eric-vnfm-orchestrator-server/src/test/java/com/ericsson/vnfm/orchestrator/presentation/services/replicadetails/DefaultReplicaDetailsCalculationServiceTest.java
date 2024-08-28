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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;

@ExtendWith(MockitoExtension.class)
public class DefaultReplicaDetailsCalculationServiceTest {

    @Mock
    private VnfInstanceService vnfInstanceService;

    @Mock
    private ScaleService scaleService;

    @Mock
    private ReplicaDetailsBuilder replicaDetailsBuilder;

    @Mock
    private ReplicaCountCalculationService replicaCountCalculationService;

    private final VnfInstance vnfInstance = new VnfInstance();

    @InjectMocks
    private DefaultReplicaDetailsCalculationService replicaDetailsCalculationService = new DefaultReplicaDetailsCalculationService();

    @Test
    public void calculate() {
        Map<String, ReplicaDetails> replicaDetailsMap = buildExpectedDefaultReplicaDetails();
        Map<String, ReplicaDetails> expected = buildExpectedEnhancedDefaultReplicaDetails();
        Map<String, Integer> replicaCounts = Map.of("test-cnf", 2, "test-cnf-vnfc3", 3);

        when(replicaCountCalculationService.calculate(vnfInstance)).thenReturn(replicaCounts);
        when(vnfInstanceService.isVnfControlledScalingExtensionPresent(vnfInstance)).thenReturn(Boolean.TRUE);
        when(scaleService.getMinReplicasCountFromVduInitialDelta(eq(vnfInstance), any())).thenReturn(1);
        when(replicaDetailsBuilder.buildDefaultReplicaDetails(vnfInstance, replicaCounts)).thenReturn(replicaDetailsMap);

        final Map<String, ReplicaDetails> actual = replicaDetailsCalculationService.calculate(vnfInstance);
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    private Map<String, ReplicaDetails> buildExpectedDefaultReplicaDetails() {
        Map<String, ReplicaDetails> replicaDetailsMap = new HashMap<>();
        replicaDetailsMap.put("test-cnf", ReplicaDetails.builder()
                .withCurrentReplicaCount(2)
                .withAutoScalingEnabledValue(Boolean.TRUE)
                .build());
        replicaDetailsMap.put("test-cnf-vnfc3", ReplicaDetails.builder()
                .withCurrentReplicaCount(3)
                .withAutoScalingEnabledValue(Boolean.TRUE)
                .build());
        return replicaDetailsMap;
    }

    private Map<String, ReplicaDetails> buildExpectedEnhancedDefaultReplicaDetails() {
        Map<String, ReplicaDetails> replicaDetailsMap = new HashMap<>();
        replicaDetailsMap.put("test-cnf", ReplicaDetails.builder()
                .withCurrentReplicaCount(2)
                .withMinReplicasCount(1)
                .withMaxReplicasCount(2)
                .withAutoScalingEnabledValue(Boolean.TRUE)
                .build());
        replicaDetailsMap.put("test-cnf-vnfc3", ReplicaDetails.builder()
                .withCurrentReplicaCount(3)
                .withMinReplicasCount(1)
                .withMaxReplicasCount(3)
                .withAutoScalingEnabledValue(Boolean.TRUE)
                .build());
        return replicaDetailsMap;
    }
}