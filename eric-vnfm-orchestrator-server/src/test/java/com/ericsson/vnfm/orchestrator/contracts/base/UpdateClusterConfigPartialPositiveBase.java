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

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class UpdateClusterConfigPartialPositiveBase extends ContractTestRunner{

    @Autowired
    private WebApplicationContext context;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean(name = "wfsRetryTemplate")
    private RetryTemplate wfsRetryTemplate;

    @BeforeEach
    public void setUp() {
        addClusterConfig();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private ClusterConfigFile buildResultClusterConfigFile(String clusterName) {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId(UUID.randomUUID().toString());
        clusterConfigFile.setCrdNamespace("eric-crd-ns");
        clusterConfigFile.setName(clusterName);
        clusterConfigFile.setDescription("Original description");
        clusterConfigFile.setStatus(ConfigFileStatus.NOT_IN_USE);
        clusterConfigFile.setContent("");
        clusterConfigFile.setDefault(false);
        return clusterConfigFile;
    }

    private void addClusterConfig() {
        ClusterConfigFile defaultCluster = buildResultClusterConfigFile("cluster01.config");
        given(databaseInteractionService.getClusterConfigByName("cluster01.config"))
            .willReturn(Optional.of(defaultCluster));
        given(databaseInteractionService.saveClusterConfig(defaultCluster)).willReturn(defaultCluster);
        given(wfsRetryTemplate.execute(any())).willReturn(getResponseEntity());
    }

    private static ResponseEntity<ClusterServerDetailsResponse> getResponseEntity() {
        Namespace nameSpace1 =  new Namespace();
        nameSpace1.setName("cvnfm");
        nameSpace1.setUid(random(8));
        Namespace nameSpace2 =  new Namespace();
        nameSpace2.setName("kube-system");
        nameSpace2.setUid(random(8));
        ClusterServerDetailsResponse clusterServerDetailsResponse = new ClusterServerDetailsResponse();
        clusterServerDetailsResponse.setNamespaces(List.of(nameSpace1, nameSpace2));
        clusterServerDetailsResponse.setHostUrl("http://localhost:ericsson.se/k8s/cluster/anynum");
        return ResponseEntity.ok().body(clusterServerDetailsResponse);
    }
}
