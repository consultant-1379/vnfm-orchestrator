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

import static org.mockito.BDDMockito.given;

import java.util.List;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EvnfmProductInfoConfig;
import com.ericsson.vnfm.orchestrator.model.ProductDependency;
import com.ericsson.vnfm.orchestrator.presentation.services.configurations.ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.kubernetes.KubernetesService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetConfigurationsPositiveBase extends ContractTestRunner {

    @Inject
    private WebApplicationContext context;

    @Autowired
    private ConfigurationService configurationService;

    @MockBean
    private KubernetesService kubernetesService;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        final EvnfmProductInfoConfig evnfmProductInfoConfig = new EvnfmProductInfoConfig();
        evnfmProductInfoConfig.setName("EVNFM");
        evnfmProductInfoConfig.setVersion("2.17.0-56");

        ProductDependency wfs = new ProductDependency();
        wfs.setName("eric-am-common-wfs");
        wfs.setVersion("1.76.0+1");

        ProductDependency onboarding = new ProductDependency();
        onboarding.setName("eric-am-onboarding-service");
        onboarding.setVersion("1.47.0+1");

        ProductDependency docker = new ProductDependency();
        docker.setName("eric-lcm-container-registry");
        docker.setVersion("4.4.0+32");

        ProductDependency helm = new ProductDependency();
        helm.setName("eric-lcm-helm-chart-registry");
        helm.setVersion("3.5.0+16");

        ProductDependency ui = new ProductDependency();
        ui.setName("eric-am-common-wfs-ui");
        ui.setVersion("0.84.0+1");

        ProductDependency db = new ProductDependency();
        db.setName("application-manager-postgres");
        db.setVersion("8.7.0+66");

        ProductDependency vnfm = new ProductDependency();
        vnfm.setName("eric-vnfm-orchestrator-service");
        vnfm.setVersion("0.168.0+1");

        ProductDependency mb = new ProductDependency();
        mb.setName("eric-eo-evnfm-mb");
        mb.setVersion("0.17.0+1");

        evnfmProductInfoConfig.setDependencies(List.of(wfs, onboarding, docker, helm, ui, db, vnfm, mb));

        ReflectionTestUtils.setField(configurationService, "evnfmProductInfoConfig", evnfmProductInfoConfig);

        given(kubernetesService.getPodNames()).willReturn(List.of("eric-am-onboarding-service"));

    }
}
