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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReplicaCountCalculationServiceTest extends ReplicaDetailsTestCommon {

    @Mock
    private ReplicaDetailsMapper replicaDetailsMapper;

    @InjectMocks
    private ReplicaCountCalculationService replicaCountCalculationService = new ReplicaCountCalculationServiceImpl();

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        super.setUp();
        mockReplicaDetailsMapperMethods();
    }

    @Test
    public void calculateFromInitialDelta() {
        Map<String, Integer> expected = buildExpectedReplicaCounts();

        Map<String, Integer> actual = replicaCountCalculationService.calculateFromVduInitialDelta(vnfInstanceRel4);

        assertEquals(expected, actual);
    }

    @Test
    public void calculate() {
        Map<String, Integer> expected = Map.of("test-cnf-vnfc3", 1,
                                               "test-cnf-vnfc4", 1, "test-cnf-vnfc5", 1, "eric-pm-bulk-reporter", 2);

        Map<String, Integer> actual = replicaCountCalculationService.calculate(vnfInstanceRel4);

        assertEquals(expected, actual);
    }

    @Test
    public void calculateForNonScalableWithoutInstantiationLevel() {
        Map<String, Integer> expected = Map.of("test-cnf-vnfc3", 0,
                                               "test-cnf-vnfc4", 1, "test-cnf-vnfc5", 1, "eric-pm-bulk-reporter", 2);

        Map<String, Integer> actual = replicaCountCalculationService.calculate(vnfInstanceRel4WithoutInstantiationLevel);

        assertEquals(expected, actual);
    }

    @Test
    public void calculateForNonScalableWithoutDefaultAndInstantiationLevel() {
        Map<String, Integer> expected = Map.of("test-cnf-vnfc4", 1, "test-cnf-vnfc5", 1, "eric-pm-bulk-reporter", 2);

        Map<String, Integer> actual = replicaCountCalculationService.calculate(vnfInstanceRel4WithoutDefaultAndInstantiationLevel);

        assertEquals(expected, actual);
    }

    @Test
    public void getResourceDetails() {
        String expected = buildExpectedResourceDetails();

        String actual = replicaCountCalculationService.getResourceDetails(vnfInstanceRel4);

        assertEquals(expected, actual);
    }

    private void mockReplicaDetailsMapperMethods() {
        Set<String> scalableVdus = buildMockedScalableVdus();

        when(replicaDetailsMapper.getScalableVdusNames(vnfInstanceRel4)).thenReturn(scalableVdus);
        when(replicaDetailsMapper.getScalableVdusNames(vnfInstanceRel4WithoutInstantiationLevel)).thenReturn(scalableVdus);
        when(replicaDetailsMapper.getScalableVdusNames(vnfInstanceRel4WithoutDefaultAndInstantiationLevel)).thenReturn(scalableVdus);
        when(replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstanceRel4)).thenReturn(policies);
        when(replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstanceRel4WithoutInstantiationLevel)).thenReturn(policies);
        when(replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstanceRel4WithoutDefaultAndInstantiationLevel))
                .thenReturn(policiesWithoutDefaultLevel);
    }

    private String buildExpectedResourceDetails() {
        return "{\"test-cnf-vnfc3\":1,\"test-cnf-vnfc4\":1,\"test-cnf-vnfc5\":1,\"eric-pm-bulk-reporter\":2}";
    }

    private Set<String> buildMockedScalableVdus() {
        return Set.of("eric-pm-bulk-reporter", "test-cnf", "test-cnf-vnfc4", "test-cnf-vnfc5");
    }
}