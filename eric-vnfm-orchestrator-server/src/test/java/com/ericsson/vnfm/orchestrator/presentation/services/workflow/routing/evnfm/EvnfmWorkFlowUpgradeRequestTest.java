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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        WorkflowRoutingServicePassThrough.class,
        ObjectMapper.class,
        CryptoUtils.class
})
@MockBean(classes = {
        DatabaseInteractionService.class,
        ValuesFileService.class,
        ScaleService.class,
        YamlMapFactoryBean.class,
        CryptoService.class,
        WorkflowRequestBodyBuilder.class,
        RestTemplate.class,
        ReplicaDetailsMapper.class
})
@MockBean(classes = RetryTemplate.class, name = "wfsRoutingRetryTemplate")
public class EvnfmWorkFlowUpgradeRequestTest {
    @Autowired
    private WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    @Test
    public void testEvnfmWorkflowUpgradeRequestCreatedCorrectlyByPriority() {
        String operationOccurrenceId = UUID.randomUUID().toString();

        HelmChart chart1 = new HelmChart();
        chart1.setPriority(1);
        HelmChart chart2 = new HelmChart();
        chart2.setPriority(2);
        VnfInstance tempVnfInstance = new VnfInstance();
        tempVnfInstance.setHelmCharts(List.of(chart1, chart2));
        tempVnfInstance.setCombinedAdditionalParams("{\"skipVerification\":true,\"skipJobVerification\":true,\"applicationTimeOut\":79,"
                + "\"overrideGlobalRegistry\":true,\"pvcTimeOut\":40}");
        tempVnfInstance.setOverrideGlobalRegistry(true);
        tempVnfInstance.setNamespace("testchangepackage");
        tempVnfInstance.setHelmCharts(List.of(chart1, chart2));

        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceName("change-package-bug");
        instance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        instance.setClusterName("changepackage-1266");
        instance.setNamespace("testchangepackage");
        instance.setOperationOccurrenceId(operationOccurrenceId);

        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationState(PROCESSING);
        operation.setVnfInstance(instance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setApplicationTimeout("80");

        VnfInstance upgradedInstance = tempVnfInstance;
        operation.setCombinedAdditionalParams(upgradedInstance.getCombinedAdditionalParams());
        final EvnfmWorkFlowUpgradeRequest requestBody = workflowRoutingServicePassThrough
                .getEvnfmWorkFlowUpgradeRequest(operation, 1, upgradedInstance);

        assertThat(requestBody.getApplicationTimeOut()).isEqualTo("79");
        assertThat(requestBody.isOverrideGlobalRegistry()).isTrue();
        assertThat(requestBody.isSkipJobVerification()).isTrue();
        assertThat(requestBody.isSkipVerification()).isTrue();
        assertThat(requestBody.getClusterName()).isEqualTo("changepackage-1266");
        assertThat(requestBody.getLifecycleOperationId()).isEqualTo(operationOccurrenceId);
        assertThat(requestBody.getNamespace()).isEqualTo("testchangepackage");
        assertThat(requestBody.getState()).isEqualTo("PROCESSING");
        assertThat(requestBody.getChartType()).isEqualTo(HelmChartType.CNF);
        assertThat(requestBody.getChartVersion()).isNull();
    }

    @Test
    public void testEvnfmWorkflowUpgradeRequestCreatedCorrectlyForCRDs() {

        VnfInstance instance = new VnfInstance();
        instance.setCrdNamespace("eric-crd-ns");
        instance.setNamespace("cnf-with-crd-ns");
        instance.setHelmCharts(getHelmCharts());

        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationState(PROCESSING);
        operation.setVnfInstance(instance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));

        final EvnfmWorkFlowUpgradeRequest firstCrdRequest = workflowRoutingServicePassThrough
                .getEvnfmWorkFlowUpgradeRequest(operation, 1, instance);
        TestChartParams firstCrdChart = new TestChartParams(HelmChartType.CRD,
                "eric-crd-ns",
                "5.3.1",
                "https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-5.3.1.tgz");
        validateUpgradeRequestBodyForChart(firstCrdChart, firstCrdRequest);

        final EvnfmWorkFlowUpgradeRequest secondCrdRequest = workflowRoutingServicePassThrough
                .getEvnfmWorkFlowUpgradeRequest(operation, 2, instance);
        TestChartParams secondCrdChart = new TestChartParams(HelmChartType.CRD,
                "eric-crd-ns",
                "2.74.7",
                "https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-sip-tls-2.5.1.tgz");
        validateUpgradeRequestBodyForChart(secondCrdChart, secondCrdRequest);

        final EvnfmWorkFlowUpgradeRequest firstCnfRequest = workflowRoutingServicePassThrough
                .getEvnfmWorkFlowUpgradeRequest(operation, 3, instance);
        TestChartParams firstCnfChart = new TestChartParams(HelmChartType.CNF,
                "cnf-with-crd-ns",
                null,
                "https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz");
        validateUpgradeRequestBodyForChart(firstCnfChart, firstCnfRequest);

        final EvnfmWorkFlowUpgradeRequest secondCnfRequest = workflowRoutingServicePassThrough
                .getEvnfmWorkFlowUpgradeRequest(operation, 4, instance);
        TestChartParams secondCnfChart = new TestChartParams(HelmChartType.CNF,
                "cnf-with-crd-ns",
                null,
                "https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/test-scale-1.0.0.tgz");
        validateUpgradeRequestBodyForChart(secondCnfChart, secondCnfRequest);

    }

    @Test
    public void testEvnfmWorkflowUpgradeRequestWithFilteringAdditionalParameters() {
        Map<String, Object> inputAdditionalParameters = new HashMap<>();
        for (String value : EVNFM_PARAMS) {
            inputAdditionalParameters.put(value, true);
        }
        for (String value : EVNFM_PARAMS_FOR_WFS) {
            inputAdditionalParameters.put(value, true);
        }

        EvnfmWorkFlowUpgradeRequest upgradeRequest = new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder("", inputAdditionalParameters).build();
        Map<String, Object> outputAdditionalParams = upgradeRequest.getAdditionalParams();

        for (String value : EVNFM_PARAMS) {
            assertThat(outputAdditionalParams.containsKey(value)).isFalse();
        }
        for (String value : EVNFM_PARAMS_FOR_WFS) {
            assertThat(outputAdditionalParams.containsKey(value)).isTrue();
        }
    }

    private static List<HelmChart> getHelmCharts() {
        var chart1 = new HelmChart();
        chart1.setHelmChartType(HelmChartType.CRD);
        chart1.setPriority(1);
        chart1.setHelmChartVersion("5.3.1");
        chart1.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-5.3.1.tgz");

        var chart2 = new HelmChart();
        chart2.setHelmChartType(HelmChartType.CRD);
        chart2.setPriority(2);
        chart2.setHelmChartVersion("2.74.7");
        chart2.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-sip-tls-2.5.1.tgz");

        var chart3 = new HelmChart();
        chart3.setHelmChartType(HelmChartType.CNF);
        chart3.setPriority(3);
        chart3.setHelmChartVersion("2.74.8");
        chart3.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz");

        var chart4 = new HelmChart();
        chart4.setHelmChartType(HelmChartType.CNF);
        chart4.setPriority(4);
        chart4.setHelmChartVersion("1.0.0");
        chart4.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/test-scale-1.0.0.tgz");

        List<HelmChart> helmChartList = List.of(chart1, chart2, chart3, chart4);
        return helmChartList;
    }

    private static void validateUpgradeRequestBodyForChart(TestChartParams expectedParams, EvnfmWorkFlowUpgradeRequest requestBody) {
        assertThat(requestBody.getChartType()).isEqualTo(expectedParams.getHelmChartType());
        assertThat(requestBody.getNamespace()).isEqualTo(expectedParams.getNamespace());
        assertThat(requestBody.getChartVersion()).isEqualTo(expectedParams.getChartVersion());
        assertThat(requestBody.getChartUrl()).isEqualTo(expectedParams.getChartUrl());
    }

    private class TestChartParams {
        private HelmChartType helmChartType;
        private String namespace;
        private String chartVersion;
        private String chartUrl;

        TestChartParams(HelmChartType helmChartType, String namespace, String chartVersion, String chartUrl) {
            this.helmChartType = helmChartType;
            this.chartVersion = chartVersion;
            this.namespace = namespace;
            this.chartUrl = chartUrl;
        }

        public HelmChartType getHelmChartType() {
            return helmChartType;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getChartVersion() {
            return chartVersion;
        }

        public String getChartUrl() {
            return chartUrl;
        }
    }
}
