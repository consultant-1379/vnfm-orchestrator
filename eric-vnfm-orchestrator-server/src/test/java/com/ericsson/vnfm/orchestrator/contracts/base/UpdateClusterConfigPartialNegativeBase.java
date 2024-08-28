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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ClusterConfigPatchRequest;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ApplicationExceptionHandler;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class UpdateClusterConfigPartialNegativeBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @SpyBean
    private ClusterConfigController configController;

    @MockBean
    private ClusterConfigFileRepository configFileRepository;

    @BeforeEach
    public void setUp() {
        mockDefaultClusterConfig();
        mockServiceUnavailableCase();
        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(configController)
                                                   .setControllerAdvice(new ApplicationExceptionHandler()));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void mockServiceUnavailableCase() {
        ClusterConfigPatchRequest parameters = new ClusterConfigPatchRequest();
        parameters.setDescription(JsonNullable.of("Description for service unavailable error"));

        doReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE)).when(configController)
                .updateClusterConfigPartiallyByName(eq("cluster503ForUpdate.config"), anyString(), anyString(), eq(parameters), eq(false));
    }

    private ClusterConfigFile buildResultClusterConfigFile(String clusterName) {
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId(UUID.randomUUID().toString());
        clusterConfigFile.setCrdNamespace("eric-crd-ns");
        clusterConfigFile.setName(clusterName);
        clusterConfigFile.setDescription("Original description");
        clusterConfigFile.setStatus(ConfigFileStatus.NOT_IN_USE);
        clusterConfigFile.setContent("");
        clusterConfigFile.setDefault(true);
        return clusterConfigFile;
    }

    private void mockDefaultClusterConfig() {
        ClusterConfigFile defaultCluster = buildResultClusterConfigFile("cluster01.config");
        when(configFileRepository.findByName("cluster02.config")).thenReturn(Optional.of(defaultCluster));
        when(configFileRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultCluster));
    }
}
