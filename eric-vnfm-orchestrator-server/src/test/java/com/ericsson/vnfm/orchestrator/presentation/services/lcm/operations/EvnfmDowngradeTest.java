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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_HELM_CLIENT_VERSION;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType.CNF;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        EvnfmDowngrade.class,
        CcvpPatternCommandFactory.class,
        Rollback.class,
        Terminate.class,
        MessageUtility.class,
        ObjectMapper.class,
        LifeCycleManagementHelper.class,
        WorkflowRoutingServicePassThrough.class,
        WorkflowRequestBodyBuilder.class,
        ReplicaDetailsMapper.class,
        CryptoUtils.class
})
@MockBean(classes = {
        DatabaseInteractionService.class,
        ScaleInfoRepository.class,
        HelmChartHistoryService.class,
        InstanceService.class,
        ChangePackageOperationDetailsRepository.class,
        ScaleService.class,
        CryptoService.class,
        ValuesFileService.class,
        RetryTemplate.class,
        HelmClientVersionValidator.class,
        RollbackService.class
})
public class EvnfmDowngradeTest {

    @Autowired
    private EvnfmDowngrade downgrade;
    @SpyBean
    private Rollback rollback;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private ClusterConfigServiceImpl clusterConfigService;
    @MockBean(name = "wfsRoutingRetryTemplate")
    private RetryTemplate wfsRoutingRetryTemplate;
    @Test
    public void testExecuteDowngradeFirstRollback() {
        VnfInstance sourceVnfInstance = getSourceTestVnfInstance();
        sourceVnfInstance.setHelmCharts(helmChartsList());
        LifecycleOperation lifecycleOperation = new LifecycleOperation();

        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        lifecycleOperation.setRollbackPattern("[{\"rollback-operation-1\": \"rollback\"},"
                                                      + "{\"rollback-operation-2\":\"rollback\"}]");

        VnfInstance targetVnfInstance = getTargetTestVnfInstance();
        targetVnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setState(""));
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));

        Mockito.doNothing().when(rollback).execute(Mockito.any(), Mockito.any(), Mockito.eq(true));
        downgrade.execute(lifecycleOperation);

        VnfInstance tempInstanceAfterTriggeringFirstDowngrade = Utility.parseJson(
                sourceVnfInstance.getTempInstance(), VnfInstance.class);
        HelmChart rollingBackChart1 = sourceVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("rollback-operation-1")).findFirst()
                .orElse(null);
        assertThat(rollingBackChart1).isNotNull();
        assertThat(rollingBackChart1.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        HelmChart rollingBackChart2 = tempInstanceAfterTriggeringFirstDowngrade.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("rollback-operation-2")).findFirst()
                .orElse(null);
        assertThat(rollingBackChart2).isNotNull();
        assertThat(rollingBackChart2.getState()).isEmpty();
    }

    @Test
    public void testExecuteDowngradeFirstTerminate() {
        String lifecycleId = randomAlphanumeric(12);
        VnfInstance sourceVnfInstance = getSourceTestVnfInstance();
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationState(LifecycleOperationState.FAILED);
        lifecycleOperation.setHelmClientVersion(DUMMY_HELM_CLIENT_VERSION);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now());
        lifecycleOperation.setOperationOccurrenceId(lifecycleId);
        lifecycleOperation.setRollbackPattern("[{\"rollback-operation-2\": \"delete\"},"
                                                      + "{\"rollback-operation-1\":\"rollback\"}]");
        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        VnfInstance targetVnfInstance = getTargetTestVnfInstance();
        targetVnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setState(""));
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));
        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(DUMMY_CLUSTER_NAME);
        clusterConfigFile.setContent(randomAlphabetic(12));
        whenWfsResourcesRespondsWithAccepted();
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(clusterConfigFile);
        downgrade.execute(lifecycleOperation);

        VnfInstance tempInstanceAfterTriggeringFirstDowngrade = Utility.parseJson(
                sourceVnfInstance.getTempInstance(), VnfInstance.class);
        HelmChart rollingBackChart1 = tempInstanceAfterTriggeringFirstDowngrade.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("rollback-operation-1")).findFirst()
                .orElse(null);
        assertThat(rollingBackChart1).isNotNull();
        assertThat(rollingBackChart1.getState()).isEmpty();

        HelmChart rollingBackChart2 = sourceVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("rollback-operation-2")).findFirst()
                .orElse(null);
        assertThat(rollingBackChart2).isNotNull();
        assertThat(rollingBackChart2.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
    }
    private VnfInstance getSourceTestVnfInstance() {
        VnfInstance sourceInstance = new VnfInstance();
        sourceInstance.setClusterName(DUMMY_CLUSTER_NAME);
        sourceInstance.setNamespace(DUMMY_NAMESPACE);
        sourceInstance.setHelmCharts(helmChartsList());
        VnfInstance tempVnfInstance = new VnfInstance();
        tempVnfInstance.setHelmCharts(helmChartsList());
        sourceInstance.setTempInstance(Utility.convertObjToJsonString(tempVnfInstance));
        return sourceInstance;
    }

    private VnfInstance getTargetTestVnfInstance() {
        VnfInstance targetInstance = new VnfInstance();
        targetInstance.setClusterName(DUMMY_CLUSTER_NAME);
        targetInstance.setNamespace(DUMMY_NAMESPACE);
        targetInstance.setHelmCharts(helmChartsList());
        return targetInstance;
    }

    private List<HelmChart> helmChartsList() {
        HelmChart chart1 = new HelmChart();
        chart1.setId("chart1");
        chart1.setHelmChartType(CNF);
        chart1.setHelmChartArtifactKey("helm_package1");
        chart1.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        chart1.setReleaseName("rollback-operation-1");
        chart1.setPriority(0);
        chart1.setDownsizeState("COMPLETED");

        HelmChart chart2 = new HelmChart();
        chart2.setId("chart2");
        chart2.setHelmChartType(CNF);
        chart2.setHelmChartArtifactKey("helm_package2");
        chart2.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        chart2.setReleaseName("rollback-operation-2");
        chart2.setPriority(1);
        chart2.setDownsizeState("FAILED");
        return List.of(chart1, chart2);
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted() {
        when(restTemplate.exchange(contains("resources"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }
}
