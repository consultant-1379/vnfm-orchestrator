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
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PodstatusPositiveBase extends ContractTestRunner {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private ClusterConfigFileRepository configFileRepository;

    @Inject
    private WebApplicationContext context;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @BeforeEach
    public void setUp() {
        VnfInstance instance = getVnfInstance();
        given(vnfInstanceRepository.findById(anyString(), any())).willReturn(Optional.of(instance));

        final ClusterConfigFile clusterConfigFile = getClusterConfigFile();
        given(configFileRepository.findByName(anyString())).willReturn(Optional.of(clusterConfigFile));

        ComponentStatusResponse componentStatusResponse = getComponentStatusResponse();
        given(workflowRoutingService.getComponentStatusRequest(instance)).willReturn(componentStatusResponse);

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @NotNull
    private static ComponentStatusResponse getComponentStatusResponse() {
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        List<VimLevelAdditionalResourceInfo> pods = new ArrayList<>();
        VimLevelAdditionalResourceInfo first = new VimLevelAdditionalResourceInfo();
        first.setName("eric-am-onboarding-service-85748b467-tg2vf");
        first.setStatus("Running");
        VimLevelAdditionalResourceInfo second = new VimLevelAdditionalResourceInfo();
        second.setName("eric-lcm-container-registry-registry-0");
        VimLevelAdditionalResourceInfo third = new VimLevelAdditionalResourceInfo();
        third.setName("eric-lcm-helm-chart-registry-75789844cb-6d566");
        pods.add(first);
        pods.add(second);
        pods.add(third);
        componentStatusResponse.setPods(pods);
        return componentStatusResponse;
    }

    @NotNull
    private static ClusterConfigFile getClusterConfigFile() {
        final ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName("hall914.config");
        clusterConfigFile.setContent("content");
        return clusterConfigFile;
    }

    @NotNull
    private static VnfInstance getVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("test");
        instance.setClusterName("default");
        List<HelmChart> allHelmChart = new ArrayList<>();
        HelmChart chart = new HelmChart();
        chart.setReleaseName("test");
        chart.setHelmChartUrl("testURL");
        chart.setId("id");
        chart.setPriority(1);
        allHelmChart.add(chart);
        instance.setHelmCharts(allHelmChart);
        return instance;
    }
}
