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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}"})
@AutoConfigureStubRunner(ids = "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server")
public class UpdateClusterConfigPositiveBase extends AbstractDbSetupTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClusterConfigFileRepository configFileRepository;

    @SpyBean
    private ClusterConfigController configController;

    @BeforeEach
    public void setUp() {
        /* there is a default cluster created by test DB migrations, its isDefault flag have to be unset to make sure there is only one default
         cluster config file present after next steps */
        unsetDefaultConfigIfPresent();

        ClusterConfigFile clusterConfigFile = ClusterConfigFile.builder()
                .name("cluster01.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("Cluster config file description.")
                .crdNamespace("eric-crd-ns")
                .clusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf")
                .verificationNamespaceUid("deadbeef-6d62-497d-8de1-ffa0aea9696f")
                .isDefault(true)
                .build();

        configFileRepository.save(clusterConfigFile);

        ClusterConfigFile notDefaultClusterConfigFile = ClusterConfigFile.builder()
                .name("cluster02.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("Cluster config file description.")
                .crdNamespace("eric-crd-ns")
                .clusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf")
                .verificationNamespaceUid("ed3f3753-e0a3-4e01-8703-5fed7fffabcf")
                .isDefault(false)
                .build();

        configFileRepository.save(notDefaultClusterConfigFile);
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @AfterEach
    public void cleanup() {
        configFileRepository.findAll().stream()
                .filter(x -> x.getName().equals("cluster01.config") ||
                        x.getName().equals("cluster02.config")).forEach(configFileRepository::delete);
    }

    private void unsetDefaultConfigIfPresent() {
        configFileRepository.findByIsDefaultTrue()
                .ifPresent(configFile -> {
                    configFile.setDefault(false);
                    configFileRepository.save(configFile);
                });
    }
}
