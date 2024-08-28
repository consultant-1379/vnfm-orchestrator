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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {
        "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}", "spring.flyway.locations = classpath:db/migration" })
@AutoConfigureStubRunner(ids = "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server")
public class RegisterClusterConfigNegativeBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @Autowired
    private ClusterConfigFileRepository configFileRepository;

    @Autowired
    private ClusterConfigService configService;

    @SpyBean
    private ClusterConfigController configController;

    private List<String> tempConfigFileIds = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        ClusterConfigFile clusterConfigFile = ClusterConfigFile.builder()
                .name("withNameDuplication.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("")
                .crdNamespace("eric-crd-ns")
                .clusterServer("")
                .verificationNamespaceUid("f679a151-5eac-43b8-94ce-88e4a33df694")
                .isDefault(false)
                .build();
        final ClusterConfigFile savedFile = configFileRepository.save(clusterConfigFile);
        tempConfigFileIds.add(savedFile.getId());
        setUpServiceUnavailableResponseCase();
        setUpCredentialsDuplicateResponseCase();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @AfterEach
    public void cleanResources() {
        tempConfigFileIds.forEach(configFileRepository::deleteById);
    }

    private void setUpServiceUnavailableResponseCase() {
        doReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE)).when(configController)
                .registerClusterConfigFile(anyString(), anyString(), anyString(), any(), eq("Description for service unavailable error"), any(),
                                           any());
    }

    private void setUpCredentialsDuplicateResponseCase() {
        ClusterConfigFile clusterConfigFile = ClusterConfigFile.builder()
                .name("withCredentialsDuplicationCase.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("")
                .crdNamespace("eric-crd-ns")
                .clusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf")
                .verificationNamespaceUid("f679a151-5eac-43b8-94ce-88e4a33df694")
                .isDefault(false)
                .build();
        final ClusterConfigFile savedFile = configFileRepository.save(clusterConfigFile);
        tempConfigFileIds.add(savedFile.getId());
    }
}
