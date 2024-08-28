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
package com.ericsson.vnfm.orchestrator.utils;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerServiceImpl;
import com.ericsson.vnfm.orchestrator.test.config.KubernetesClusterTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.EnumSet;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Import(KubernetesClusterTestConfiguration.class)
@AutoConfigureObservability
public abstract class AbstractDbSetupTest {

    @SpyBean
    public UsernameCalculationService usernameCalculationService;

    @MockBean
    protected LicenseConsumerServiceImpl licenseConsumerService;

    @Autowired
    protected LcmOperationsConfig lcmOperationsConfig;

    @BeforeEach
    public void initMocks() {
        doReturn("E2E_USERNAME").when(usernameCalculationService).calculateUsername();
        given(licenseConsumerService.getPermissions()).willReturn(EnumSet.allOf(Permission.class));
        lcmOperationsConfig.setLcmOperationsLimit(Integer.MAX_VALUE);
    }

    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(
            "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/postgres").asCompatibleSubstituteFor("postgres"));

    static {
        postgreSQLContainer.start();
        System.setProperty("DB_URL", postgreSQLContainer.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgreSQLContainer.getUsername());
        System.setProperty("DB_PASSWORD", postgreSQLContainer.getPassword());
    }
}
