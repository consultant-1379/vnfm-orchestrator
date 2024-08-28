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

import static java.time.LocalDateTime.now;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;

import java.util.List;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.InstantiateOperation;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    YamlFileMerger.class,
    AdditionalParamsUtils.class,
    Rollback.class,
    Upgrade.class,
    CcvpPatternCommandFactory.class,
    EvnfmManualRollback.class,
    ReplicaDetailsMapper.class,
    OperationsUtils.class,
    ObjectMapper.class,
    HelmChartHistoryServiceImpl.class,
    LcmOpSearchServiceImpl.class,
    HealOperation.class,
    LifecycleOperationHelper.class,
    WorkflowRequestBodyBuilder.class,
    HelmChartHelper.class,
    CryptoUtils.class
})
@MockBean(classes = {
    InstantiateRequestHandler.class,
    InstantiateOperation.class,
    OssNodeService.class,
    HelmChartRepository.class,
    MessageUtility.class,
    InstanceService.class,
    RestTemplate.class,
    HelmChartHistoryRepository.class,
    ChangePackageOperationDetailsRepository.class,
    LifecycleOperationRepository.class,
    ValuesFileService.class,
    ScaleServiceImpl.class,
    CryptoService.class,
    ClusterConfigServiceImpl.class,
    LifeCycleManagementHelper.class
})
public class EvnfmManualRollbackTest {
    @Autowired
    private EvnfmManualRollback evnfmManualRollback;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean
    private PackageService packageService;
    @MockBean
    private WorkflowRoutingService workflowRoutingService;
    @Captor
    private ArgumentCaptor<String> revisionCaptor;
    @Captor
    private ArgumentCaptor<LifecycleOperation> lifecycleOperationArgumentCaptor;
    @Captor
    private ArgumentCaptor<HelmChart> helmChartArgumentCaptor;
    private VnfInstance sourceVnfInstance;
    private VnfInstance targetVnfInstance;
    private LifecycleOperation lifecycleOperation;
    private WorkflowRoutingResponse workflowRoutingResponse;

    @BeforeEach
    public void setUp() throws Exception {
        List<HelmChart> helmChartsListTarget = getDoubleHelmCharts(
            "dummy-target-id-1",
            "dummy-target-id-2",
            "FAILED"
        );
        targetVnfInstance = createInstanceWith("dummy-target-instance-id",
                                           "upgrade-failed-single",
                                           "multipleChartsFailed",
                                           "testrollback",
                                               helmChartsListTarget);
        targetVnfInstance.setPolicies(readDataFromFile(getClass(), "evnfm-manual-rollback/policies.json"));

        List<HelmChart> helmChartsListSource = getDoubleHelmCharts(
            "dummy-source-id-1",
            "dummy-source-id-2",
            "COMPLETED"
        );
        sourceVnfInstance = createInstanceWith("dummy-source-instance-id",
                                           "rollback-test-source",
                                           "delete-namespace",
                                           "rollback-namespace",
                                               helmChartsListSource);
        sourceVnfInstance.setTempInstance(Utility.convertObjToJsonString(targetVnfInstance));

        lifecycleOperation = createLifecycleOperation();

        workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.OK);
    }

    @Test
    public void shouldExecuteRollbackOfFailedChart() {
        lifecycleOperation.setFailurePattern("[{\"rollback-operation-1\": \"rollback\"},"
                                                 + "{\"rollback-operation-2\":\"rollback\"}]");
        when(workflowRoutingService.routeRollbackRequest(eq(sourceVnfInstance), eq(lifecycleOperation), any(), any()))
            .thenReturn(workflowRoutingResponse);

        evnfmManualRollback.execute(lifecycleOperation);

        verify(databaseInteractionService).persistVnfInstanceAndOperation(any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());
        verify(workflowRoutingService).routeRollbackRequest(eq(sourceVnfInstance), eq(lifecycleOperation), any(), revisionCaptor.capture());

        LifecycleOperation actualLifecycleOperation = lifecycleOperationArgumentCaptor.getValue();

        assertThat(actualLifecycleOperation.getFailurePattern()).isEqualTo("[{\"rollback-operation-1\": \"rollback\"},"
                                                                               + "{\"rollback-operation-2\":\"rollback\"}]");
        assertThat(actualLifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(revisionCaptor.getValue()).isEqualTo(null);
    }

    @Test
    public void shouldExecuteUpgradeOfFailedChart() {
        lifecycleOperation.setFailurePattern("[{\"rollback-operation-1\": \"upgrade\"},"
                                                 + "{\"rollback-operation-2\":\"rollback\"}]");
        sourceVnfInstance.setCombinedValuesFile(readDataFromFile(getClass(), "evnfm-manual-rollback/combined-values-file.json"));
        JSONObject vnfd = new JSONObject(readDataFromFile(getClass(), "evnfm-manual-rollback/vnfd.json"));
        List<HelmChart> helmCharts = sourceVnfInstance.getHelmCharts();
        List<HelmChart> tempCharts = targetVnfInstance.getHelmCharts();
        assertThat(helmCharts).isNotSameAs(tempCharts);

        when(packageService.getVnfd(any())).thenReturn(vnfd);
        when(workflowRoutingService.routeToEvnfmWfsFakeUpgrade(any(), any(), any(), any(), any())).thenReturn(workflowRoutingResponse);

        evnfmManualRollback.execute(lifecycleOperation);

        verify(databaseInteractionService).persistVnfInstanceAndOperation(any(VnfInstance.class), lifecycleOperationArgumentCaptor.capture());
        verify(workflowRoutingService).routeToEvnfmWfsFakeUpgrade(any(), any(), helmChartArgumentCaptor.capture(), any(), any());
        LifecycleOperation actualLifecycleOperation = lifecycleOperationArgumentCaptor.getValue();
        assertThat(actualLifecycleOperation.getFailurePattern()).isEqualTo("[{\"rollback-operation-1\": \"upgrade\"},"
                                                                               + "{\"rollback-operation-2\":\"rollback\"}]");
        assertThat(actualLifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(helmChartArgumentCaptor.getValue()).isEqualTo(helmCharts.get(0));
    }

    private static List<HelmChart> getDoubleHelmCharts(final String idOne, final String idTwo, final String state) {
        HelmChart ch1 = new HelmChart();
        ch1.setId(idOne);
        ch1.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.73.7.tgz");
        ch1.setPriority(0);
        ch1.setReleaseName("rollback-operation-1");
        ch1.setState("COMPLETED");
        ch1.setHelmChartArtifactKey("helm_package1");
        HelmChart ch2 = new HelmChart();
        ch2.setId(idTwo);
        ch2.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.73.7.tgz");
        ch2.setPriority(1);
        ch2.setReleaseName("rollback-operation-2");
        ch2.setState(state);
        ch2.setHelmChartArtifactKey("helm_package2");
        return List.of(ch1, ch2);
    }

    private VnfInstance createInstanceWith(String instanceId,
                                           String instanceName,
                                           String clusterName,
                                           String nameSpace,
                                           List<HelmChart> helmChartsList) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(instanceId);
        vnfInstance.setVnfInstanceName(instanceName);
        vnfInstance.setVnfInstanceDescription("vnfInstanceDescription");
        vnfInstance.setVnfDescriptorId("d3def1ce-4cf4-477c-aab3-21cb04e6a379");
        vnfInstance.setVnfPackageId("9392468011745350001");
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setClusterName(clusterName);
        vnfInstance.setNamespace(nameSpace);
        vnfInstance.setHelmCharts(helmChartsList);
        return vnfInstance;
    }
    private LifecycleOperation createLifecycleOperation() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId("oo104bc1-474f-4673-91ee-656fd831212");
        lifecycleOperation.setVnfInstance(sourceVnfInstance);
        lifecycleOperation.setOperationState(LifecycleOperationState.ROLLING_BACK);
        lifecycleOperation.setStartTime(now());
        lifecycleOperation.setStateEnteredTime(now());
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperation.setOperationParams("{}");
        lifecycleOperation.setTargetVnfdId("adff854f-7dc2-4e04-8287-cbed94bd202");
        return lifecycleOperation;
    }
}
