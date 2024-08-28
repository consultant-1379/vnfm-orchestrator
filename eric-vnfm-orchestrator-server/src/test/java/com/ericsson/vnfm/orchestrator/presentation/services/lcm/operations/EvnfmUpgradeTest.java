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

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_HELM_CLIENT_VERSION;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType.CNF;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        EvnfmUpgrade.class,
        CcvpPatternCommandFactory.class,
        Rollback.class,
        Upgrade.class,
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
public class EvnfmUpgradeTest {

    @Autowired
    private EvnfmUpgrade upgrade;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private ClusterConfigServiceImpl clusterConfigService;

    @MockBean(name = "wfsRoutingRetryTemplate")
    private RetryTemplate wfsRoutingRetryTemplate;

    @MockBean
    private AdditionalParamsUtils additionalParamsUtils;

    @MockBean
    private OperationsUtils operationsUtils;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void mockAdditionalParamsFetching() {
        when(additionalParamsUtils.mergeAdditionalParams(any())).thenReturn(StringUtils.EMPTY);
        when(additionalParamsUtils.convertAdditionalParamsToMap(anyString())).thenReturn(Collections.emptyMap());
        when(operationsUtils.getAdditionalParamsFromLastOperation(any(VnfInstance.class), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(operationsUtils.getScaleParamsLastOperation(any(VnfInstance.class), any(HelmChart.class), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(packageService.getVnfd(any())).thenReturn(getVnfd());
    }

    @Test
    public void testExecuteUpgradeFirstUpgrade() {
        VnfInstance sourceVnfInstance = getSourceTestVnfInstance();
        sourceVnfInstance.setHelmCharts(helmChartsList());
        String lifecycleId = randomAlphanumeric(12);

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        lifecycleOperation.setUpgradePattern("[{\"upgrade-operation-1\": \"upgrade\"},"
                + "{\"upgrade-operation-2\":\"rollback\"}]");
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setHelmClientVersion(DUMMY_HELM_CLIENT_VERSION);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now());
        lifecycleOperation.setOperationOccurrenceId(lifecycleId);

        VnfInstance targetVnfInstance = getTargetTestVnfInstance();
        sourceVnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setState(""));
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(DUMMY_CLUSTER_NAME);
        clusterConfigFile.setContent(randomAlphabetic(12));

        whenWfsResourcesRespondsWithAccepted();
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(clusterConfigFile);

        upgrade.execute(lifecycleOperation);

        VnfInstance tempInstanceAfterTriggeringFirstUpgrade = Utility.parseJson(
                sourceVnfInstance.getTempInstance(), VnfInstance.class);
        HelmChart upgradingChart1 = tempInstanceAfterTriggeringFirstUpgrade.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-1")).findFirst()
                .orElse(null);
        assertThat(upgradingChart1).isNotNull();
        assertThat(upgradingChart1.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());

        HelmChart upgradingChart2 = sourceVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-2")).findFirst()
                .orElse(null);
        assertThat(upgradingChart2).isNotNull();
        assertThat(upgradingChart2.getState()).isEmpty();
    }

    @Test
    public void testExecuteUpgradeFirstTerminate() {
        VnfInstance sourceVnfInstance = getSourceTestVnfInstance();
        sourceVnfInstance.setHelmCharts(helmChartsList());
        String lifecycleId = randomAlphanumeric(12);

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        lifecycleOperation.setUpgradePattern("[{\"upgrade-operation-2\": \"delete\"},"
                + "{\"upgrade-operation-1\":\"upgrade\"}]");
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setHelmClientVersion(DUMMY_HELM_CLIENT_VERSION);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now());
        lifecycleOperation.setOperationOccurrenceId(lifecycleId);

        VnfInstance targetVnfInstance = getTargetTestVnfInstance();
        targetVnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setState(""));
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(DUMMY_CLUSTER_NAME);
        clusterConfigFile.setContent(randomAlphabetic(12));

        whenWfsResourcesRespondsWithAccepted();
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(clusterConfigFile);

        upgrade.execute(lifecycleOperation);

        VnfInstance tempInstanceAfterTriggeringFirstTerminate = Utility.parseJson(
                sourceVnfInstance.getTempInstance(), VnfInstance.class);
        HelmChart upgradingChart1 = tempInstanceAfterTriggeringFirstTerminate.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-1")).findFirst()
                .orElse(null);
        assertThat(upgradingChart1).isNotNull();
        assertThat(upgradingChart1.getState()).isEmpty();

        HelmChart upgradingChart2 = sourceVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-2")).findFirst()
                .orElse(null);
        assertThat(upgradingChart2).isNotNull();
        assertThat(upgradingChart2.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
    }

    @Test
    public void testExecuteUpgradeFirstUpgradeForDisabledChart() {
        VnfInstance sourceVnfInstance = getSourceTestVnfInstance();
        sourceVnfInstance.setHelmCharts(helmChartsList());
        String lifecycleId = randomAlphanumeric(12);

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        lifecycleOperation.setUpgradePattern("[{\"upgrade-operation-1\": \"upgrade\"},"
                + "{\"upgrade-operation-2\":\"upgrade\"}]");
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setHelmClientVersion(DUMMY_HELM_CLIENT_VERSION);
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now());
        lifecycleOperation.setOperationOccurrenceId(lifecycleId);

        VnfInstance targetVnfInstance = getTargetTestVnfInstance();
        targetVnfInstance.setTerminatedHelmCharts(Collections.singletonList(getTerminatedHelmChart(lifecycleId)));
        targetVnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setState(""));
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));

        ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setName(DUMMY_CLUSTER_NAME);
        clusterConfigFile.setContent(randomAlphabetic(12));

        whenWfsResourcesRespondsWithAccepted();
        when(clusterConfigService.getOrDefaultConfigFileByName(anyString())).thenReturn(clusterConfigFile);

        upgrade.execute(lifecycleOperation);

        VnfInstance tempInstanceAfterTriggeringFirstUpgrade = Utility.parseJson(
                sourceVnfInstance.getTempInstance(), VnfInstance.class);
        HelmChart upgradingChart1 = targetVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-1")).findFirst()
                .orElse(null);
        assertThat(upgradingChart1).isNotNull();
        assertThat(upgradingChart1.getState()).isEmpty();

        HelmChart upgradingChart2 = tempInstanceAfterTriggeringFirstUpgrade.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName().equals("upgrade-operation-2")).findFirst()
                .orElse(null);
        assertThat(upgradingChart2).isNotNull();
        assertThat(upgradingChart2.getState()).isEqualTo(LifecycleOperationState.PROCESSING.toString());
    }

    private VnfInstance getSourceTestVnfInstance() {
        VnfInstance sourceInstance = new VnfInstance();
        sourceInstance.setVnfDescriptorId("2ce9484e-85e5-49b7-ac97-445379754e37");
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
        targetInstance.setVnfDescriptorId("36ff67a9-0de4-48f9-97a3-4b0661670934");
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
        chart1.setReleaseName("upgrade-operation-1");
        chart1.setPriority(0);
        chart1.setDownsizeState("COMPLETED");

        HelmChart chart2 = new HelmChart();
        chart2.setId("chart2");
        chart2.setHelmChartType(CNF);
        chart2.setHelmChartArtifactKey("helm_package2");
        chart2.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/test-scale-chart-1.0.1.tgz");
        chart2.setReleaseName("upgrade-operation-2");
        chart2.setPriority(1);
        chart2.setDownsizeState("COMPLETED");
        return List.of(chart1, chart2);
    }

    private TerminatedHelmChart getTerminatedHelmChart(String lifecycleOperationOccurenceId) {
        TerminatedHelmChart chart1 = new TerminatedHelmChart();
        chart1.setOperationOccurrenceId(lifecycleOperationOccurenceId);
        chart1.setId("chart1Terminated");
        chart1.setHelmChartType(CNF);
        chart1.setHelmChartArtifactKey("helm_package1");
        chart1.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        chart1.setReleaseName("upgrade-operation-1");
        chart1.setPriority(0);
        return chart1;
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted() {
        when(restTemplate.exchange(contains("resources"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private JSONObject getVnfd() {
        Path vnfdPath = TestUtils.getResource(getClass(), "valid-vnfd.yaml");
        Map<String, Object> vnfdFileMap = YamlUtility.convertYamlFileIntoMap(vnfdPath);
        return new JSONObject(vnfdFileMap);
    }
}
