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

import com.ericsson.vnfm.orchestrator.filters.ClusterConfigQuery;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class GetClusterConfigsByPageAndSizePositiveBase extends ContractTestRunner{
    @MockBean
    private ClusterConfigQuery clusterConfigQuery;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws IOException {
        Pageable pageable = PageRequest.of(1, 2);
        Page<ClusterConfigFile> page = new PageImpl<>(getConfigFilesByPage(), pageable, 9l);

        given(clusterConfigQuery.getPageWithFilter(any(), any())).willReturn(page);
        given(databaseInteractionService.getClusterConfigs(any())).willReturn(page);

    RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<ClusterConfigFile> getConfigFilesByPage() {
        List<ClusterConfigFile> configsList = new ArrayList<>();
        final ClusterConfigFile clusterConfigFile1 = new ClusterConfigFile();
        clusterConfigFile1.setId("cbb3a411-d98b-4a41-876a-b0642fe923cf");
        clusterConfigFile1.setName("cluster002.config");
        clusterConfigFile1.setStatus(ConfigFileStatus.NOT_IN_USE);
        clusterConfigFile1.setDescription("Cluster 002 config file");
        clusterConfigFile1.setCrdNamespace("eric-crd-ns");
        clusterConfigFile1.setDefault(true);
        configsList.add(clusterConfigFile1);

        final ClusterConfigFile clusterConfigFile2 = new ClusterConfigFile();
        clusterConfigFile2.setName("cluster003.config");
        clusterConfigFile2.setId("dee3a711-d98b-4a41-123a-b0642fe923kg");
        clusterConfigFile2.setStatus(ConfigFileStatus.NOT_IN_USE);
        clusterConfigFile2.setDescription("Cluster 003 config file");
        clusterConfigFile2.setCrdNamespace("eric-crd-ns");
        clusterConfigFile1.setDefault(false);
        configsList.add(clusterConfigFile2);
        return configsList;
    }

}