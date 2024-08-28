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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ApplicationExceptionHandler;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {
        "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}", "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server")
public class UpdateClusterConfigNegativeBase extends AbstractDbSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private ClusterConfigFileRepository configFileRepository;

    @SpyBean
    private ClusterConfigController configController;

    @BeforeEach
    public void setUp() {
        ClusterConfigFile defaultClusterConfigFile = ClusterConfigFile.builder()
                .name("cluster503ForUpdate.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("Cluster config file description.")
                .crdNamespace("eric-crd-ns")
                .clusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/sgrhhf")
                .verificationNamespaceUid("deadbeef-6d62-497d-8de1-ffa0aea9696f")
                .isDefault(true)
                .build();

        ClusterConfigFile invalidDescriptionConfig = ClusterConfigFile.builder()
                .name("invalidDescription.config")
                .content("")
                .status(ConfigFileStatus.NOT_IN_USE)
                .description("Cluster config file description.")
                .crdNamespace("eric-crd-ns")
                .clusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/invalidDescription")
                .verificationNamespaceUid("ed3f3753-e0a3-4e01-8703-5fed7fffabcf")
                .isDefault(false)
                .build();

        given(configFileRepository.findByName("notExisting.config")).willReturn(Optional.empty());
        given(configFileRepository.findByName("cluster503ForUpdate.config")).willReturn(Optional.of(defaultClusterConfigFile));
        given(configFileRepository.findByName("invalidDescription.config")).willReturn(Optional.of(invalidDescriptionConfig));
        given(configFileRepository.findByIsDefaultTrue()).willReturn(Optional.of(defaultClusterConfigFile));

        setUpServiceUnavailableResponseCase();

        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(configController).setControllerAdvice(new ApplicationExceptionHandler()));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void setUpServiceUnavailableResponseCase() {
        doReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE)).when(configController)
                .updateClusterConfigByName(any(), anyString(), anyString(), any(), any(), eq("Description for service unavailable error"), any());
    }
}
