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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostSyncVnfInstancePositiveBase extends ContractTestRunner {

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() throws IOException, URISyntaxException {
        RestAssuredMockMvc.webAppContextSetup(context);
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(getVnfInstance());
        when(databaseInteractionService.persistLifecycleOperation(any())).thenReturn(new LifecycleOperation());

    }

    @BeforeAll
    public static void beforeAll() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterAll
    public static void afterAll() {
        TransactionSynchronizationManager.clear();
    }

    private VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setVnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a37");
        vnfInstance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        List<HelmChart> helmCharts = getHelmCharts(vnfInstance);
        vnfInstance.setHelmCharts(helmCharts);
        return vnfInstance;
    }

    private List<HelmChart> getHelmCharts(final VnfInstance vnfInstance) {
        HelmChart helmChart = new HelmChart();
        helmChart.setHelmChartUrl("http://test/test.tgz");
        helmChart.setPriority(1);
        helmChart.setReleaseName("release-name-2");
        helmChart.setHelmChartType(HelmChartType.CNF);
        helmChart.setVnfInstance(vnfInstance);
        List<HelmChart> charts = new ArrayList<>();
        charts.add(helmChart);
        return charts;
    }
}
