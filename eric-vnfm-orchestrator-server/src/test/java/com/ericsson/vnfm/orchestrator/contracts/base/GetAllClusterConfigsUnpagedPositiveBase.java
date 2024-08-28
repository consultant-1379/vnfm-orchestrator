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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllClusterConfigsUnpagedPositiveBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @BeforeEach
    public void initMocks() {
        RestAssuredMockMvc.webAppContextSetup(context);

        given(clusterConfigFileRepository.findAll(eq(Pageable.unpaged()))).willAnswer(
                invocationOnMock -> getClusterConfigs(invocationOnMock.getArgument(0))
        );
    }

    private ClusterConfigFile createClusterConfig(String id, String name, ConfigFileStatus status, String description, boolean isDefault) {
        final ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId(id);
        clusterConfigFile.setName(name);
        clusterConfigFile.setStatus(status);
        clusterConfigFile.setDescription(description);
        clusterConfigFile.setCrdNamespace("eric-crd-ns");
        clusterConfigFile.setDefault(isDefault);

        return clusterConfigFile;
    }

    private List<ClusterConfigFile> populateClusterConfigs() {
        List<ClusterConfigFile> configs = new ArrayList<>();
        configs.add(createClusterConfig("default", "default.config", ConfigFileStatus.IN_USE, "Default cluster config file", true));
        for (int i = 0; i < 17; i++) {
            String clusterName = "cluster" + i + ".config";
            String description = "Description for config file " + i;
            configs.add(createClusterConfig(String.valueOf(i), clusterName, ConfigFileStatus.NOT_IN_USE, description, false));
        }

        return configs;
    }

    private Page<ClusterConfigFile> getClusterConfigs(Pageable pageable) {
        return new PageImpl<>(populateClusterConfigs(), pageable, 18);
    }
}
