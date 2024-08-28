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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;

@ExtendWith(MockitoExtension.class)
public class ScaleMappingReplicaDetailsCalculationServiceTest extends ReplicaDetailsTestCommon {

    @Mock
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Mock
    private ReplicaDetailsBuilder replicaDetailsBuilder;

    @Mock
    private ReplicaDetailsMapper replicaDetailsMapper;

    @InjectMocks
    private ScaleMappingReplicaDetailsCalculationService replicaDetailsCalculationService =
            new ScaleMappingReplicaDetailsCalculationService();

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        super.setUp();
    }

    @Test
    public void calculateWithoutValuesFile() {
        Map<String, ReplicaDetails> expected = buildExpectedDefaultReplicaDetails();
        Map<String, Integer> replicaCounts = new HashMap<>();
        replicaCounts.put("test-cnf", 2);
        replicaCounts.put("test-cnf-vnfc3", 3);

        when(replicaCountCalculationService.calculate(vnfInstanceRel4)).thenReturn(replicaCounts);
        when(replicaDetailsBuilder.buildReplicaDetailsFromScalingMapping(vnfInstanceRel4, scaleMappingMap, replicaCounts))
                .thenReturn(expected);

        final Map<String, ReplicaDetails> actual = replicaDetailsCalculationService.calculate(vnfd, scaleMappingMap, vnfInstanceRel4, null);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
        verify(replicaDetailsBuilder, times(1))
                .buildReplicaDetailsFromScalingMapping(vnfInstanceRel4, scaleMappingMap, replicaCounts);
    }

    @Test
    public void calculateWithValuesFile() {
        Map<String, ReplicaDetails> expected = buildExpectedEnhancedDefaultReplicaDetails();
        Map<String, Integer> expectedReplicaCounts = Map.of("test-cnf", 2, // not overridden by values
                                                            "test-cnf-vnfc0", 4, // not overridden by values, autoscaling=true but maxReplicas=null
                                                            "test-cnf-vnfc1", 7, // overridden by values despite autoscaling=null
                                                            "test-cnf-vnfc3", 6); // overridden by values when autoscaling=true and replicaCount!=null
        Map<String, Integer> replicaCounts = new HashMap<>(Map.of("test-cnf", 2,
                                                                  "test-cnf-vnfc0", 4,
                                                                  "test-cnf-vnfc1", 3,
                                                                  "test-cnf-vnfc3", 3));
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(toValuesFile);

        when(replicaDetailsMapper.getScalableVdusNames(vnfInstanceRel4)).thenReturn(Set.of("eric-pm-bulk-reporter"));
        when(replicaCountCalculationService.calculate(vnfInstanceRel4)).thenReturn(replicaCounts);
        when(replicaDetailsBuilder.buildReplicaDetailsFromScalingMapping(vnfInstanceRel4, scaleMappingMap, replicaCounts))
                .thenReturn(expected);

        final Map<String, ReplicaDetails> actual = replicaDetailsCalculationService.calculate(vnfd, scaleMappingMap, vnfInstanceRel4, valuesYamlMap);
        assertSame(expected, actual);
        assertEquals(expectedReplicaCounts, replicaCounts);
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
                .build());
        replicaDetailsMap.put("test-cnf-vnfc0", ReplicaDetails.builder()
                .withCurrentReplicaCount(4)
                .build());
        replicaDetailsMap.put("test-cnf-vnfc1", ReplicaDetails.builder()
                .withCurrentReplicaCount(7)
                .build());
        replicaDetailsMap.put("test-cnf-vnfc3", ReplicaDetails.builder()
                .withCurrentReplicaCount(6)
                .build());
        return replicaDetailsMap;
    }
}