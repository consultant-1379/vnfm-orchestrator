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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.ClusterConfigData;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;

@SpringBootTest(classes = CismClusterConfigMapperImpl.class)
public class CismClusterConfigMapperTest {

    @Autowired
    private CismClusterConfigMapper cismClusterConfigMapper;


    @Test
    public void testMapperCorrectlyMapsClusterConfig() {
        ClusterConfigFile clusterConfigFile = buildConfigFile();

        ClusterConfigData result = cismClusterConfigMapper.toInternalModel(clusterConfigFile);

        assertThat(result.getId()).isEqualTo(clusterConfigFile.getId());
        assertThat(result.getName()).isEqualTo(clusterConfigFile.getName());
        assertThat(result.getDescription()).isEqualTo(clusterConfigFile.getDescription());
        assertThat(result.getClusterData()).isEqualTo(clusterConfigFile.getContent());
        assertThat(result.getCrdNamespace()).isEqualTo(clusterConfigFile.getCrdNamespace());
        assertThat(result.getStatus()).isEqualTo(clusterConfigFile.getStatus());
        assertThat(result.getIsDefault()).isEqualTo(clusterConfigFile.isDefault());
    }

    private ClusterConfigFile buildConfigFile() {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId("dummy-id");
        clusterConfigFile.setName("dummy-name");
        clusterConfigFile.setDefault(true);
        clusterConfigFile.setDescription("Dummy config");
        clusterConfigFile.setCrdNamespace("dummy-crd-namespace");
        clusterConfigFile.setStatus(ConfigFileStatus.IN_USE);
        clusterConfigFile.setVerificationNamespaceUid("dummy-uid");
        clusterConfigFile.setClusterServer("dummyServer");
        clusterConfigFile.setContent("{dummy: content}");
        return clusterConfigFile;
    }
}
