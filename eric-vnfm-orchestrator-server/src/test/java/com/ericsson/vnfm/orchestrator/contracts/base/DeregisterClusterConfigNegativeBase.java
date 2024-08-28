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

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractRedisSetupTest;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@TestPropertySource(properties = {"spring.flyway.enabled = false"})
public class DeregisterClusterConfigNegativeBase extends AbstractRedisSetupTest {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private ClusterConfigFileRepository configFileRepository;

    @SpyBean
    private ClusterConfigController configController;

    @BeforeEach
    public void setUp() {
        given(configFileRepository.findByName("notExisting.config")).willReturn(Optional.empty());
        ClusterConfigFile configUsedByDefault = new ClusterConfigFile();
        configUsedByDefault.setDefault(true);
        given(configFileRepository.findByName("configUsedByDefault.config")).willReturn(Optional.of(configUsedByDefault));
        ClusterConfigFile clusterConfigFileStatusInUse = new ClusterConfigFile();
        clusterConfigFileStatusInUse.setStatus(ConfigFileStatus.IN_USE);
        given(configFileRepository.findByName("clusterConfigInUse.config"))
                .willReturn(Optional.of(clusterConfigFileStatusInUse));

        setUpServiceUnavailableResponseCase();

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void setUpServiceUnavailableResponseCase() {
        doReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE)).when(configController)
                .deregisterClusterConfigByName(eq("cluster503ForDeregister.config"), anyString(), anyString());
    }
}
