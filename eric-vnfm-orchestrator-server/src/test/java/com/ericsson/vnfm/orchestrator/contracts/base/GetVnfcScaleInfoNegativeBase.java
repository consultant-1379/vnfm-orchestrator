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

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetVnfcScaleInfoNegativeBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    //Despite it is not used here It is necessary here to mock tries to request Onboarding service
    @MockBean
    private PackageService packageService;
    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        VnfInstance vnfInstance = getVnfInstance();

        given(vnfInstanceRepository.findById(anyString(), any())).willReturn(Optional.of(vnfInstance));
    }

    private static VnfInstance getVnfInstance(){
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("5f43fb8e-1316-468a-9f9c-b375e5d82094");
        vnfInstance.setResourceDetails("{\"PL__scaled_vm\": 28, \"CL_scaled_vm\": 28, \"TL_scaled_vm\": 1}");
        String policies = "{\"allScalingAspects\":{\"ScalingAspects\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                + "\"properties\":{\"aspects\":{\"Payload\":{\"name\":\"Payload\",\"description\":\"Scale level 0-29 maps to 1-30 Payload VNFC "
                + "instances (1 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\",\"delta_2\",\"delta_3\"]}}}}},"
                + "\"allInitialDelta\":{\"Payload_InitialDelta\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\","
                + "\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"PL__scaled_vm\",\"TL_scaled_vm\"]}},"
                + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                + "\"properties\":{\"aspect\":\"Payload\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},"
                + "\"delta_2\":{\"number_of_instances\":2},\"delta_3\":{\"number_of_instances\":7}}},\"targets\":[\"PL__scaled_vm\","
                + "\"CL_scaled_vm\"]}}}";
        vnfInstance.setPolicies(policies);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);

        HelmChart chart = new HelmChart();
        chart.setVnfInstance(vnfInstance);
        chart.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/" +
                                  "spider-app-2.74.6.tgz");
        chart.setPriority(1);
        chart.setReleaseName("release_name");
        chart.setReplicaDetails("{\"PL__scaled_vm\":{\"currentReplicaCount\":28,\"scalingParameterName\":\"PL__scaled" +
                                    "_vm.replicaCount\",\"autoScalingEnabledParameterName\":\"PL__scaled_vm.autoScali" +
                                    "ngEnabled\",\"autoScalingEnabledValue\":true,\"minReplicasParameterName\":\"PL__" +
                                    "scaled_vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplicasParameterName\":\"PL_" +
                                    "_scaled_vm.minReplicas\",\"maxReplicasCount\":3},\"TL_scaled_vm\":{\"currentRepli" +
                                    "caCount\":28,\"scalingParameterName\":\"TL_scaled_vm.replicaCount\"},\"CL__scale" +
                                    "d_vm\":{\"currentReplicaCount\":28,\"scalingParameterName\":\"CL__scaled_vm.repl" +
                                    "icaCount\",\"autoScalingEnabledParameterName\":\"CL__scaled_vm.autoScalingEnable" +
                                    "d\",\"autoScalingEnabledValue\":false,\"minReplicasParameterName\":\"CL__scaled_" +
                                    "vm.maxReplicas\",\"minReplicasCount\":1,\"maxReplicasParameterName\":\"CL__scaled" +
                                    "_vm.minReplicas\",\"maxReplicasCount\":3}}");
        List<HelmChart> allHelmChart = new ArrayList<>();
        allHelmChart.add(chart);

        vnfInstance.setHelmCharts(allHelmChart);

        ScaleInfoEntity scaleInfoEntity = new ScaleInfoEntity();
        scaleInfoEntity.setVnfInstance(vnfInstance);
        scaleInfoEntity.setAspectId("Payload");
        scaleInfoEntity.setScaleLevel(5);
        List<ScaleInfoEntity> scaleInfoEntityList = new ArrayList<>();
        scaleInfoEntityList.add(scaleInfoEntity);
        vnfInstance.setScaleInfoEntity(scaleInfoEntityList);
        return vnfInstance;
    }
}
