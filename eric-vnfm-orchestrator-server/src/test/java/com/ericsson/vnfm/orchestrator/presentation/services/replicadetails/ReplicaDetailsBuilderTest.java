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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReplicaDetailsBuilderTest extends ReplicaDetailsTestCommon {

    @Mock
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Mock
    private ScaleService scaleService;

    @InjectMocks
    private ReplicaDetailsBuilder replicaDetailsBuilder = new ReplicaDetailsBuilder();

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        super.setUp();
        mockReplicaDetailsMapperMethods();
    }

    @Test
    public void buildDefaultReplicaDetails() {
        Map<String, Integer> replicaCounts = buildExpectedReplicaCounts();
        Map<String, ReplicaDetails> expected = buildExpectedReplicaDetails(false);

        Map<String, ReplicaDetails> actual = replicaDetailsBuilder
                .buildDefaultReplicaDetails(vnfInstanceRel4, replicaCounts);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    public void buildReplicaDetailsFromScalingMapping() {
        Map<String, Integer> replicaCounts = buildExpectedReplicaCounts();
        Map<String, ReplicaDetails> expected = buildExpectedReplicaDetails(true);

        final Map<String, ReplicaDetails> actual = replicaDetailsBuilder
                .buildReplicaDetailsFromScalingMapping(vnfInstanceRel4, scaleMappingMap, replicaCounts);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    private void mockReplicaDetailsMapperMethods() {
        when(replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstanceRel4)).thenReturn(policies);
        when(scaleService.getMinReplicasCountFromVduInitialDelta(eq(vnfInstanceRel4), any())).thenReturn(1);
    }
}