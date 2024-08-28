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
package com.ericsson.vnfm.orchestrator.presentation.services.license;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CLUSTER_MANAGEMENT_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.LCM_OPERATIONS_LICENSE_TYPE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

@ExtendWith(MockitoExtension.class)
public class OrchestratorLimitsCalculatorTest {

    @Mock
    private VnfInstanceRepository vnfInstanceRepository;

    @Mock
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @InjectMocks
    private OrchestratorLimitsCalculator orchestratorLimitsCalculator;

    @Test
    public void testCheckOrchestratorLimitsWithEmptyAttributeForInstances() {
        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForInstances(null))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCheckOrchestratorLimitsWithTrueAttributeForInstances() {
        Object attribute = true;
        when(vnfInstanceRepository.count()).thenReturn(2L);

        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForInstances(attribute))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCheckOrchestratorLimitsWithFalseAttributeForInstances() {
        Object attribute = false;

        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForInstances(attribute))
                .doesNotThrowAnyException();
        verifyNoInteractions(vnfInstanceRepository);
    }

    @Test
    public void testCheckOrchestratorLimitsWithTrueAttributeAndFiveCnf() {
        Object attribute = true;
        when(vnfInstanceRepository.count()).thenReturn(5L);

        MissingLicensePermissionException exception =
                assertThrows(MissingLicensePermissionException.class, () -> orchestratorLimitsCalculator.checkOrchestratorLimitsForInstances(attribute));

        assertEquals(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, LCM_OPERATIONS_LICENSE_TYPE), exception.getMessage());
    }

    @Test
    public void testCheckOrchestratorLimitsWithEmptyAttributeForClusters() {
        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForClusters(null))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCheckOrchestratorLimitsWithTrueAttributeForClusters() {
        Object attribute = true;
        when(clusterConfigFileRepository.count()).thenReturn(1L);

        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForClusters(attribute))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCheckOrchestratorLimitsWithFalseAttributeForClusters() {
        Object attribute = false;

        assertThatCode(() -> orchestratorLimitsCalculator.checkOrchestratorLimitsForClusters(attribute))
                .doesNotThrowAnyException();
        verifyNoInteractions(vnfInstanceRepository);
    }

    @Test
    public void testCheckOrchestratorLimitsWithTrueAttributeAndTwoClusters() {
        Object attribute = true;
        when(clusterConfigFileRepository.count()).thenReturn(2L);

        MissingLicensePermissionException exception =
                assertThrows(MissingLicensePermissionException.class, () -> orchestratorLimitsCalculator.checkOrchestratorLimitsForClusters(attribute));

        assertEquals(String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE), exception.getMessage());
    }
}