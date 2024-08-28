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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;


public class ClusterConfigServiceImplDBTest extends AbstractDbSetupTest {

    private static final String CLUSTER_FILE_NAME = "multiple-charts-failed.config";

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ClusterConfigFileRepository configFileRepository;


    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private ClusterConfigInstanceRepository clusterConfigInstanceRepository;

    @Test
    public void testClusterConfigFileAssociatedWithTwoInstancesVerifyingClusterStatusAfterEachOperation() {
        VnfInstance firstInstance = vnfInstanceRepository.findByVnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a379");
        VnfInstance secondInstance = vnfInstanceRepository.findByVnfInstanceId("e3def1ce-4cf4-477c-aab3-21c454e6a389");

        //Pre updates cluster file not in use
        verifyConfigFileStatusAtCorrectState(ConfigFileStatus.NOT_IN_USE);
        Optional<ClusterConfigFile> clusterConfigFileOptional = configFileRepository.findByName(CLUSTER_FILE_NAME);
        assertThat(clusterConfigFileOptional).isPresent();

        ClusterConfigFile clusterConfigFile = clusterConfigFileOptional.get();
        Optional<ClusterConfigInstance> byClusterConfigFileAndInstanceId = clusterConfigInstanceRepository
                .findByClusterConfigFileAndInstanceId(clusterConfigFile, firstInstance.getVnfInstanceId());
        assertThat(byClusterConfigFileAndInstanceId).isEmpty();
        Optional<ClusterConfigInstance> byClusterConfigFileAndSecondInstance = clusterConfigInstanceRepository
                .findByClusterConfigFileAndInstanceId(clusterConfigFile, firstInstance.getVnfInstanceId());
        assertThat(byClusterConfigFileAndSecondInstance).isEmpty();
        //associate first instance with cluster
        clusterConfigService
                .changeClusterConfigFileStatus(CLUSTER_FILE_NAME, firstInstance, ConfigFileStatus.IN_USE);

        //cluster status is now in use
        verifyConfigFileStatusAtCorrectState(ConfigFileStatus.IN_USE);

        //associate second instance with cluster
        clusterConfigService
                .changeClusterConfigFileStatus(CLUSTER_FILE_NAME, secondInstance, ConfigFileStatus.IN_USE);
        //cluster status is still in use
        verifyConfigFileStatusAtCorrectState(ConfigFileStatus.IN_USE);

        //fist and second instance added to new DB schema
        Optional<ClusterConfigInstance> afterUpdate = clusterConfigInstanceRepository
                .findByClusterConfigFileAndInstanceId(clusterConfigFile, firstInstance.getVnfInstanceId());
        assertThat(afterUpdate).isPresent();
        Optional<ClusterConfigInstance> afterUpdate2 = clusterConfigInstanceRepository
                .findByClusterConfigFileAndInstanceId(clusterConfigFile, secondInstance.getVnfInstanceId());
        assertThat(afterUpdate2).isPresent();

        // remove association with first instance
        clusterConfigService
                .changeClusterConfigFileStatus(CLUSTER_FILE_NAME, firstInstance, ConfigFileStatus.NOT_IN_USE);
        // cluster file still in use due to second chart
        verifyConfigFileStatusAtCorrectState(ConfigFileStatus.IN_USE);

        // remove association with second instance
        clusterConfigService
                .changeClusterConfigFileStatus(CLUSTER_FILE_NAME, secondInstance, ConfigFileStatus.NOT_IN_USE);
        //no association in with instance so cluster file is no not_in_use
        verifyConfigFileStatusAtCorrectState(ConfigFileStatus.NOT_IN_USE);
    }

    @Test
    public void testThrowErrorWhenInvalidRequestForCismCluster() {
        assertThatThrownBy(() -> clusterConfigService.getCismClusterConfigs(
                PageRequest.of(0, 0, Sort.Direction.DESC, "name")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must not be less than one");
    }

    private void verifyConfigFileStatusAtCorrectState(final ConfigFileStatus configFileStatus) {
        Optional<ClusterConfigFile> byFileNameAfterFirst = configFileRepository.findByName(CLUSTER_FILE_NAME);
        assertThat(byFileNameAfterFirst).isPresent();
        assertThat(byFileNameAfterFirst.get().getStatus()).isEqualTo(configFileStatus);
    }

}
