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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.loadYamlToMap;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoJson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.InstantiateOperation;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        Instantiate.class,
        AdditionalParamsUtils.class,
        YamlUtility.class,
        ObjectMapper.class,
        OperationsUtils.class,
        HealOperation.class,
        YamlFileMerger.class,
        CryptoUtils.class
})
@MockBean(classes = {
        ChangeVnfPackageServiceImpl.class,
        InstantiateRequestHandler.class,
        ReplicaDetailsMapper.class,
        MessageUtility.class,
        InstantiateOperation.class,
        CryptoService.class,
        OssNodeService.class,
        DatabaseInteractionService.class,
        HelmChartHelper.class,
        LifecycleOperationHelper.class,
        LifeCycleManagementHelper.class
})
public class InstantiateTest {
    private static final String SOURCE_DESCRIPTOR_ID = "2ce9484e-85e5-49b7-ac97-445379754e37";
    private static final String DESTINATION_DESCRIPTOR_ID = "36ff67a9-0de4-48f9-97a3-4b0661670934";

    private LifecycleOperation instantiateOperation;
    private LifecycleOperation rollbackOperation;

    private HelmChart helmChart;

    @Autowired
    private Instantiate instantiate;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private LcmOpSearchService lcmOpSearchService;

    @MockBean
    private HelmChartHistoryService helmChartHistoryService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> additionalParamsCaptor;

    @Captor
    private ArgumentCaptor<Path> valuesFileCaptor;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        VnfInstance originalInstance = TestUtils.getVnfInstance();
        originalInstance.setVnfDescriptorId(SOURCE_DESCRIPTOR_ID);

        VnfInstance tempInstance = TestUtils.getVnfInstance();
        tempInstance.setVnfDescriptorId(DESTINATION_DESCRIPTOR_ID);

        helmChart = TestUtils.getHelmChart("test-url", "unit-test-release", originalInstance, 1, HelmChartType.CNF);
        helmChart.setReplicaDetails("test");
        originalInstance.getHelmCharts().add(helmChart);
        tempInstance.getHelmCharts().add(helmChart);

        originalInstance.setTempInstance(convertObjToJsonString(tempInstance));

        originalInstance.setCombinedAdditionalParams("{\"original-instance-param\": \"original-instance-value\"}");

        instantiateOperation = new LifecycleOperation();
        instantiateOperation.setOperationParams(readDataFromFile(getClass(), "instantiate-request.json"));
        instantiateOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        instantiateOperation.setValuesFileParams("{\"instantiate-values-file-param\": \"instantiate-value\"}");
        instantiateOperation.setVnfInstance(originalInstance);
        instantiateOperation.setOperationState(LifecycleOperationState.COMPLETED);
        instantiateOperation.setStateEnteredTime(LocalDateTime.now().minusHours(5));

        rollbackOperation = new LifecycleOperation();
        rollbackOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        String rollbackAdditionalParams = readDataFromFile(getClass(), "rollback-additional-params.json");
        rollbackOperation.setValuesFileParams(rollbackAdditionalParams);
        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest =
                TestUtils.createUpgradeRequest(originalInstance.getVnfInstanceId(), convertStringToJSONObj(rollbackAdditionalParams));
        rollbackOperation.setOperationParams(convertObjToJsonString(changeCurrentVnfPkgRequest));
        rollbackOperation.setVnfInstance(originalInstance);
        rollbackOperation.setOperationState(LifecycleOperationState.PROCESSING);
        rollbackOperation.setStateEnteredTime(LocalDateTime.now().minusHours(1));

        when(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(any())).thenReturn(List.of());

        when(packageService.getVnfd(ArgumentMatchers.anyString()))
                .thenReturn(convertYamlFileIntoJson(getResource(getClass(), "valid-vnfd.yaml")));

        WorkflowRoutingResponse workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.ACCEPTED);
        when(workflowRoutingService.routeInstantiateRequest(any(), any(), any(HelmChart.class), any(), any()))
                .thenReturn(workflowRoutingResponse);
    }

    @Test
    public void testInstantiateParamsAreCorrectWhenRollbackPattern() throws IOException {
        // given
        when(lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(any(), eq(1)))
                .thenReturn(Optional.of(instantiateOperation));

        // when
        instantiate.execute(rollbackOperation, helmChart, true);

        // then
        verify(workflowRoutingService).routeInstantiateRequest(any(),
                                                               any(),
                                                               any(HelmChart.class),
                                                               additionalParamsCaptor.capture(),
                                                               valuesFileCaptor.capture());

        final Map<String, Object> additionalParams = additionalParamsCaptor.getValue();

        assertThat(additionalParams).doesNotContainKey(UPGRADE_FAILED_VNFD_KEY);

        final Map<String, Object> valueFileParams = loadYamlToMap(valuesFileCaptor.getValue());

        assertThat(valueFileParams)
                .hasEntrySatisfying("spring", valueSpring ->
                        assertThat(valueSpring).asInstanceOf(MAP)
                                .hasEntrySatisfying("data", valueData ->
                                        assertThat(valueData).asInstanceOf(MAP).contains(
                                                entry("password", "password-value"),
                                                entry("login", "login-value")))
                                .contains(
                                        entry("profile", "global-static-instance"),
                                        entry("logs", "enabled")))
                .contains(
                        entry("original-instance-param", "original-instance-value"),
                        entry("host", "localhost"),
                        entry("version", "global-static-version"),
                        entry("username", "global-static-username"),
                        entry("instantiate-parameter", "instantiate-value"),
                        entry("instantiate-values-file-param", "instantiate-value"));
    }

    @Test
    public void testInstantiateParamsAreCorrectWhenFailurePattern() throws IOException {
        // given
        when(lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(any(), eq(0)))
                .thenReturn(Optional.of(instantiateOperation));

        // when
        instantiate.execute(rollbackOperation, helmChart, false);

        // then
        verify(workflowRoutingService).routeInstantiateRequest(any(),
                                                               any(),
                                                               any(HelmChart.class),
                                                               additionalParamsCaptor.capture(),
                                                               valuesFileCaptor.capture());

        final Map<String, Object> additionalParams = additionalParamsCaptor.getValue();

        assertThat(additionalParams).doesNotContainKey(UPGRADE_FAILED_VNFD_KEY);

        final Map<String, Object> valueFileParams = loadYamlToMap(valuesFileCaptor.getValue());

        assertThat(valueFileParams)
                .hasEntrySatisfying("spring", valueSpring ->
                        assertThat(valueSpring).asInstanceOf(MAP)
                                .hasEntrySatisfying("data", valueData ->
                                        assertThat(valueData).asInstanceOf(MAP).contains(
                                                entry("password", "password-value-initial"),
                                                entry("login", "login-value-initial")))
                                .contains(
                                        entry("profile", "global-static-instance"),
                                        entry("logs", "disabled")))
                .contains(
                        entry("original-instance-param", "original-instance-value"),
                        entry("host", "localhost-initial"),
                        entry("version", "global-static-version"),
                        entry("username", "global-static-username"),
                        entry("instantiate-parameter", "instantiate-value"),
                        entry("instantiate-values-file-param", "instantiate-value"),
                        entry("upgrade-failed-param", "upgrade-failed-value"));
    }
}
