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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.InstantiateOperation;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.FileMerger;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        AdditionalParamsUtils.class,
        ObjectMapper.class,
        ChangeVnfPackageServiceImpl.class,
        HealOperation.class,
        OperationsUtils.class,
        VimLevelAdditionalResourceInfo.class,
        LcmOpAdditionalParamsProcessorImpl.class,
        LcmOpSearchServiceImpl.class,
        AdditionalParamsUtils.class,
        YamlFileMerger.class,
        CryptoUtils.class
})
@MockBean({ VnfInstanceRepository.class,
        HelmChartHistoryRepository.class,
        LifecycleOperationRepository.class,
        ChangePackageOperationDetailsRepository.class,
        WorkflowRoutingService.class,
        HelmChartHistoryService.class,
        InstantiateRequestHandler.class,
        InstanceService.class,
        MessageUtility.class,
        InstantiateOperation.class,
        CryptoService.class,
        OssNodeService.class,
        ReplicaDetailsMapper.class,
        DatabaseInteractionService.class,
        HelmChartHelper.class,
        PackageService.class,
        LifecycleOperationHelper.class,
        LifeCycleManagementHelper.class
})
public class LcmOpAdditionalParamsProcessorTest {
    private static final String TARGET_VNFD_ID_FOR_SCALE_MOCK = "target_vnfd_id_for_scale_mock";
    private static final String FIRST_OPERATION_OCCURRENCE_ID_MOCK = "first-operation-occurrence-id-mock";
    private static final String SECOND_OPERATION_OCCURRENCE_ID_MOCK= "second_operation_occurrence_id_mock";
    private static final String THIRD_OPERATION_OCCURRENCE_ID_MOCK= "third_operation_occurrence_id_mock";
    private static final String TARGET_VNFD_ID_FOR_UPGRADE_MOCK = "target_vnfd_id_for_upgrade_mock";
    private static final String TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK = "target_vnfd_id_for_self_upgrade_mock";
    private static final String TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK = "target_vmfd_id_for_downgrade_mock";
    private static final String TARGET_VNFD_ID_FOR_DOWNGRADE_NO_UPGRADE_IN_HISTORY_MOCK = "target_vnfd_id_for_downgrade_no_upgrade_in_history_mock";
    private static final String OPERATION_PARAMS_VALUE = "{\"additionalParams\": {\"applicationTimeOut\":360,\"skipVerification\":false}}";
    private static final String SECOND_INSTANTIATE_VALUE_FILE_PARAM = "AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nK"
            + "iBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4ev"
            + "uxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g"
            + "0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=";

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private LcmOpAdditionalParamsProcessor lcmOpAdditionalParamsProcessor;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @MockBean
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @SpyBean
    private FileMerger fileMerger;

    private Path currentValuePath;

    @BeforeEach
    public void init() throws IOException {
        currentValuePath = Files.createTempDirectory("temp_values");
    }

    @Test
    public void testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation() throws IOException {
        String expected = "applicationTimeOut: 100\ntest_VDU:\n  replicaCount: 3";

        Map<String, LifecycleOperation> testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation
                = createLifecycleOperationsDataFor_TestProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation();

        doReturn(List.of(testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation.get("secondLifecycleOperation"),
                         testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation.get("thirdLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation.get("secondLifecycleOperation"),
                         testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation.get("thirdLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        ChangePackageOperationDetails changePackageOpDetailsMock = createChangePackageOpDetailsMock();

        doReturn(Optional.of(changePackageOpDetailsMock))
                .when(changePackageOperationDetailsRepository)
                .findById(any());

        HelmChartHistoryRecord helmChartHistoryRecordMock = createHelmChartHistoryRecordMock();

        doReturn(List.of(helmChartHistoryRecordMock))
                .when(helmChartHistoryRepository)
                .findAllByLifecycleOperationIdOrderByPriorityAsc(any());

        Map<String, Object> actual = new HashMap<>();
        lcmOpAdditionalParamsProcessor.process(actual, TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK,
                testProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation.get("operation"));
        assertThat(convertYamlStringIntoMap(expected)).isEqualTo(actual);
    }

    //a test of SM-139154 will show that after scale operation the cycle will break and the processing will consider only operations, which
    // happened before scale
    @Test
    public void testProcessWithScale() throws IOException, URISyntaxException {
        Path expectedValuesFile = TestUtils.getResource("com/ericsson/vnfm/orchestrator/presentation/services/lcm/processors/valuesScale.yaml");
        Map<String, Object> expectedValuesYamlMap = convertYamlFileIntoMap(expectedValuesFile);

        final Map<String, LifecycleOperation> testProcessWithScaleData =
                createLifecycleOperationsDataMockFor_TestProcessWithScale();

        doReturn(List.of(testProcessWithScaleData.get("fourthChangeVnfpkgMock"),
                         testProcessWithScaleData.get("scaleMock"),
                         testProcessWithScaleData.get("secondChangeVnfpkgMock"),
                         testProcessWithScaleData.get("firstInstantiateMock")
                 ),
                 List.of(testProcessWithScaleData.get("scaleMock"),
                         testProcessWithScaleData.get("secondChangeVnfpkgMock"),
                         testProcessWithScaleData.get("firstInstantiateMock")))
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        doReturn(List.of(testProcessWithScaleData.get("sixthChangeVnfpkgMock"),
                         testProcessWithScaleData.get("fourthChangeVnfpkgMock"),
                         testProcessWithScaleData.get("scaleMock"),
                         testProcessWithScaleData.get("secondChangeVnfpkgMock"),
                         testProcessWithScaleData.get("firstInstantiateMock")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());
        Map<String, Object> actual = new HashMap<>();
        lcmOpAdditionalParamsProcessor.process(actual, TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                               testProcessWithScaleData.get("operation"));
        assertThat(expectedValuesYamlMap).isEqualTo(actual);
    }

    @Test
    public void testProcessForClassicUpgradeShouldReturnCurrentValuePath() {
        LifecycleOperation operation = new LifecycleOperation();
        operation.setSourceVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);

        Map<String, Object> actualValueMap = new HashMap<>();
        lcmOpAdditionalParamsProcessor.process(actualValueMap, TARGET_VNFD_ID_FOR_DOWNGRADE_NO_UPGRADE_IN_HISTORY_MOCK,
                operation
        );

        assertThat(actualValueMap).isEmpty();
        verifyNoInteractions(fileMerger);
    }

    @Test
    public void testProcessAfterSelfUpgradeReturnsSameMap() {
        Path currentValuePathShouldBeReturned = TestUtils.getResource(getClass(),
                "values3.yaml");
        Map<String, Object> currentValueMapShouldBeReturned = convertYamlFileIntoMap(currentValuePathShouldBeReturned);

        Map<String, LifecycleOperation> testProcessAfterSelfUpgradeReturnsSameFileData =
                createLifecycleOperationsDataMockFor_TestProcessAfterSelfUpgradeReturnsSameFile();

        Map<String, Object> actual = new HashMap<>(currentValueMapShouldBeReturned);
        lcmOpAdditionalParamsProcessor.process(actual, TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                testProcessAfterSelfUpgradeReturnsSameFileData.get("currentOperation")
        );
        assertThat(actual).isEqualTo(currentValueMapShouldBeReturned);
    }

    @Test
    public void testProcessSelfUpgradeInProgress() {
        Path expectedValuePath = TestUtils.getResource(getClass(), "lcmOpAdditionalParamsProcessorTestData_instantiate1selfupgrade2.yaml");
        Map<String, Object> expectedValuesYamlMap = convertYamlFileIntoMap(expectedValuePath);
        Path currentValuePath = TestUtils.getResource(getClass(), "values2.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(currentValuePath);

        Map<String, LifecycleOperation> testProcessSelfUpgradeInProgressData =
                createLifecycleOperationsDataMockFor_TestProcessSelfUpgradeInProgress();

        doReturn(List.of(testProcessSelfUpgradeInProgressData.get("firstLifecycleOperation")), Collections.emptyList())
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        doReturn(List.of(testProcessSelfUpgradeInProgressData.get("currentOperation"),
                         testProcessSelfUpgradeInProgressData.get("firstLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        lcmOpAdditionalParamsProcessor
                .process(valuesYamlMap, TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK, testProcessSelfUpgradeInProgressData.get("currentOperation"));

        assertThat(valuesYamlMap).isEqualTo(expectedValuesYamlMap);
    }

    @Test
    public void testProcessInProgressAfterSelfUpgrade() {
        Path expectedValuePath = TestUtils.getResource(getClass(), "lcmOpAdditionalParamsProcessorTestData_instantiate1seflupgrade2selfupgrade3.yaml");
        Map<String, Object> expectedValuesYamlMap = convertYamlFileIntoMap(expectedValuePath);
        Path currentValuePath = TestUtils.getResource(getClass(), "values2.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(currentValuePath);

        Map<String, LifecycleOperation> testProcessInProgressAfterSelfUpgradeData =
                createLifecycleOperationsDataMockFor_TestProcessInProgressAfterSelfUpgrade();

        doReturn(List.of(testProcessInProgressAfterSelfUpgradeData.get("thirdLifecycleOperation"),
                         testProcessInProgressAfterSelfUpgradeData.get("secondLifecycleOperation"),
                         testProcessInProgressAfterSelfUpgradeData.get("firstLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProcessInProgressAfterSelfUpgradeData.get("firstLifecycleOperation"),
                         testProcessInProgressAfterSelfUpgradeData.get("secondLifecycleOperation")),
                 Collections.emptyList())
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        lcmOpAdditionalParamsProcessor
                .process(valuesYamlMap, TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK, testProcessInProgressAfterSelfUpgradeData.get("currentOperation"));

        assertThat(valuesYamlMap).isEqualTo(expectedValuesYamlMap);
    }

    @Test
    public void testProcessThreeSelfUpgradesAfterGeneralUpgrade() {
        Path expectedValuePath = TestUtils.getResource(getClass(), "lcmOpAdditionalParamsProcessorTestData_instantiate1selfupgrade2upgrade3selfupgrade4selfupgrade5.yaml");
        Map<String, Object> expectedValuesYamlMap = convertYamlFileIntoMap(expectedValuePath);
        Path currentValuePath = TestUtils.getResource(getClass(), "values5.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(currentValuePath);

        Map<String, LifecycleOperation> testProcessThreeSelfUpgradesAfterGeneralUpgradeData =
                createLifecycleOperationsDataMockFor_TestProcessThreeSelfUpgradesAfterGeneralUpgrade();

        doReturn(List.of(testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("fifthLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("fourthLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("thirdLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("secondLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("firstLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("fourthLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("thirdLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("secondLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("firstLifecycleOperation")),
                 List.of(testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("firstLifecycleOperation"),
                         testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("secondLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        lcmOpAdditionalParamsProcessor
                .process(valuesYamlMap, TARGET_VNFD_ID_FOR_UPGRADE_MOCK, testProcessThreeSelfUpgradesAfterGeneralUpgradeData.get("currentOperation"));

        assertThat(valuesYamlMap).isEqualTo(expectedValuesYamlMap);
    }

    @Test
    public void testProcessReturnToSelfUpgradeValuesAfterRollback() throws Exception {
        Path expectedValuePath = TestUtils.getResource(getClass(), "lcmOpAdditionalParamsProcessorTestData_instantiate1selfupgrade2upgrade3selfupgrade4downgrade5.yaml");
        Map<String, Object> expectedValuesYamlMap = convertYamlFileIntoMap(expectedValuePath);
        Path currentValuePath = TestUtils.getResource(getClass(), "values5.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(currentValuePath);

        Map<String, LifecycleOperation> testProcessReturnToSelfUpgradeValuesAfterRollbackData =
                createLifecycleOperationsDataMockFor_TestProcessReturnToSelfUpgradeValuesAfterRollback();

        ChangePackageOperationDetails changePackageOpDetailsMock = createChangePackageOpDetailsMock();

        HelmChartHistoryRecord helmChartHistoryRecordMock = createHelmChartHistoryRecordMock();

        doReturn(List.of(testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("fifthLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("fourthLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("thirdLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("secondLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("firstLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("fourthLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("thirdLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("secondLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("firstLifecycleOperation")),
                 List.of(testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("fourthLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("thirdLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("secondLifecycleOperation"),
                         testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("firstLifecycleOperation")),
                 Collections.emptyList())
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        doReturn(Optional.of(changePackageOpDetailsMock)).when(changePackageOperationDetailsRepository).findById(any());

        doReturn(List.of(helmChartHistoryRecordMock)).when(helmChartHistoryRepository).findAllByLifecycleOperationIdOrderByPriorityAsc(any());

        lcmOpAdditionalParamsProcessor
                .process(valuesYamlMap, TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK, testProcessReturnToSelfUpgradeValuesAfterRollbackData.get("currentOperation"));

        assertThat(valuesYamlMap).isEqualTo(expectedValuesYamlMap);
    }

    @Test
    public void testProceedTerminateAfterInstantiate() throws Exception {
        Map<String, Object> expected = TestUtils.loadYamlToMap(TestUtils.getResource(
                "com/ericsson/vnfm/orchestrator/presentation/services/lcm/processors/values1.yaml"));

        VnfInstance vnfInstance = prepareVnfInstance();

        Map<String, LifecycleOperation> testProceedTerminateAfterInstantiateData =
                createLifecycleOperationsDataMockFor_TestProceedTerminateAfterInstantiate();

        doReturn(List.of(testProceedTerminateAfterInstantiateData.get("firstLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        Map<String, Object> actual = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testProceedTerminateAfterDowngrade() throws Exception {
        Map<String, Object> expected = TestUtils.loadYamlToMap(TestUtils.getResource(
                "com/ericsson/vnfm/orchestrator/presentation/services/lcm/processors/instantiate1upgrade2downgrade3.yaml"));

        VnfInstance vnfInstance = prepareVnfInstance();
        vnfInstance.setVnfDescriptorId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        Map<String, LifecycleOperation> testProceedTerminateAfterDowngradeData =
                createLifecycleOperationsDataMockFor_TestProceedTerminateAfterDowngrade();

        ChangePackageOperationDetails changePackageOpDetailsMock = createChangePackageOpDetailsMock();

        HelmChartHistoryRecord helmChartHistoryRecordMock = createHelmChartHistoryRecordMock();

        doReturn(List.of(testProceedTerminateAfterDowngradeData.get("firstMockLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("secondLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("thirdMockLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProceedTerminateAfterDowngradeData.get("secondLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("firstMockLifecycleOperation")),
                 List.of(testProceedTerminateAfterDowngradeData.get("thirdMockLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("secondLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("firstMockLifecycleOperation"),
                         testProceedTerminateAfterDowngradeData.get("fourthMockLifecycleOperation")),
                 Collections.emptyList())
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        doReturn(Optional.of(changePackageOpDetailsMock))
                .when(changePackageOperationDetailsRepository)
                .findById(any());

        doReturn(List.of(helmChartHistoryRecordMock))
                .when(helmChartHistoryRepository)
                .findAllByLifecycleOperationIdOrderByPriorityAsc(any());

        Map<String, Object> actual = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);
        lcmOpAdditionalParamsProcessor
                .process(actual, vnfInstance.getVnfDescriptorId(),
                         testProceedTerminateAfterDowngradeData.get("previousOperation"));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testProceedTerminateAfterSelfUpgrade() throws Exception {
        Map<String, Object> expected = TestUtils
                .loadYamlToMap(TestUtils.getResource("com/ericsson/vnfm/orchestrator"
                                                             + "/presentation/services/lcm/processors/instantiate1upgrade2selfupgrade3.yaml")
        );

        VnfInstance vnfInstance = prepareVnfInstance();
        vnfInstance.setVnfDescriptorId(TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        Map<String, LifecycleOperation> testProceedTerminateAfterSelfUpgradeData =
                createLifecycleOperationsDataMockFor_TestProceedTerminateAfterSelfUpgrade();

        doReturn(List.of(testProceedTerminateAfterSelfUpgradeData.get("firstMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("secondMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("thirdMockLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProceedTerminateAfterSelfUpgradeData.get("secondMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("thirdMockLifecycleOperation")),
                 List.of(testProceedTerminateAfterSelfUpgradeData.get("thirdMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("secondMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("firstMockLifecycleOperation"),
                         testProceedTerminateAfterSelfUpgradeData.get("fourthMockLifecycleOperation")),
                 List.of(testProceedTerminateAfterSelfUpgradeData.get("thirdMockLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        Map<String, Object> actual = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);
        lcmOpAdditionalParamsProcessor
                .process(actual, vnfInstance.getVnfDescriptorId(), testProceedTerminateAfterSelfUpgradeData.get("previousOperation"));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testProceedTerminateAfterComplexUpgradesSequence() throws Exception {
        // Sequence: Instantiate (1) , [ Self-upgrade (2, 6), Upgrade (3, 7), Self-upgrade(4, 8), Downgrade(5, 9) ] x2, Terminate (10)
        // Expected result is merged yaml file from (1), (2) and (6) operations
        Map<String, Object> expected = TestUtils.loadYamlToMap(TestUtils.getResource(
                "com/ericsson/vnfm/orchestrator/presentation/services/lcm/processors/terminateAfterComplexScenario.yaml"));

        VnfInstance vnfInstance = prepareVnfInstance();
        vnfInstance.setVnfDescriptorId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        Map<String, LifecycleOperation> testProceedTerminateAfterComplexUpgradesSequence =
                createLifecycleOperationsDataMockFor_TestProceedTerminateAfterComplexUpgradesSequence();

        ChangePackageOperationDetails changePackageOpDetailsMock = createChangePackageOpDetailsMock();

        HelmChartHistoryRecord helmChartHistoryRecordMock = createHelmChartHistoryRecordMock();

        doReturn(List.of(testProceedTerminateAfterComplexUpgradesSequence.get("firstMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("secondLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("thirdMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("fourthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("fifthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("sixthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("seventhMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("eightMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("ninthMockLifecycleOperation")))
                .when(lifecycleOperationRepository)
                .findByVnfInstanceAndTypesAndStates(any(), any(), any());

        doReturn(List.of(testProceedTerminateAfterComplexUpgradesSequence.get("thirdMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("fourthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("fifthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("sixthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("seventhMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("eightMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("ninthMockLifecycleOperation")),
                 List.of(testProceedTerminateAfterComplexUpgradesSequence.get("sixthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("seventhMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("eightMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("ninthMockLifecycleOperation")),
                 List.of(testProceedTerminateAfterComplexUpgradesSequence.get("sixthMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("seventhMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("eightMockLifecycleOperation"),
                         testProceedTerminateAfterComplexUpgradesSequence.get("ninthMockLifecycleOperation")),
                 Collections.emptyList())
                .when(lifecycleOperationRepository)
                .findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(any(), any());

        doReturn(Optional.of(changePackageOpDetailsMock))
                .when(changePackageOperationDetailsRepository)
                .findById(any());

        doReturn(List.of(helmChartHistoryRecordMock))
                .when(helmChartHistoryRepository)
                .findAllByLifecycleOperationIdOrderByPriorityAsc(any());

        Map<String, Object> actual = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);
        lcmOpAdditionalParamsProcessor
                .process(actual, vnfInstance.getVnfDescriptorId(), testProceedTerminateAfterComplexUpgradesSequence.get("previousOperation"));

        assertThat(actual).isEqualTo(expected);
    }

    private VnfInstance prepareVnfInstance() {
        final String vnfDescriptorId = "vnfDescriptorId";
        final VnfInstance vnfInstance = new VnfInstance();
        final VnfInstance tempInstance = TestUtils.getVnfInstance();
        tempInstance.setVnfDescriptorId(vnfDescriptorId);
        tempInstance.setHelmCharts(Collections.emptyList());
        vnfInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        vnfInstance.setVnfInstanceId("vnf_instance_id_mock");
        vnfInstance.setVnfDescriptorId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");
        return vnfInstance;
    }

    private LifecycleOperation createFirstInstantiateMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 16, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 16, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc5\":{\"replicaCount\":115},"
                                                           + "\"vnfc3\":{\"replicaCount\":113},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":116},\"vnfc1\":{\"replicaCount\":111},"
                                                           + "\"vnfc2\":{\"replicaCount\":112}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);

        return mockLifecycleOperation;
    }

    private LifecycleOperation createSecondInstantiateMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 16, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 16, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams(SECOND_INSTANTIATE_VALUE_FILE_PARAM);
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createSecondChangeVnfpkgMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 17, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 17, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc3\":{\"replicaCount\":223},\"eric-pm-bulk-reporter\":{\"replicaCount\":226},"
                                                           + "\"vnfc1\":{\"replicaCount\":221},"
                                                           + "\"vnfc2\":{\"replicaCount\":222}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createThirdMockChangeVnfpkg(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 28, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 28, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"eric-pm-bulk-reporter\":{\"replicaCount\":336},\"vnfc1\":{\"replicaCount\":331},"
                                                           + "\"vnfc2\":{\"replicaCount\":332}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createFourthChangeVnfpkgMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 21, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 21, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":441},\"eric-pm-bulk-reporter\":{\"replicaCount\":442}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createFifthChangeVnfpkgMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 23, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 23, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"eric-pm-bulk-reporter\":{\"replicaCount\":555}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createSixthChangeVnfpkgMock(String id, String sourceVnfdId, String targetVnfdId) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 25, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 25, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":661},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":662}}");
        mockLifecycleOperation.setSourceVnfdId(sourceVnfdId);
        mockLifecycleOperation.setTargetVnfdId(targetVnfdId);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createSeventhChangeVnfpkgMock() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 27, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 27, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":771},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":772}}\"");
        mockLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        mockLifecycleOperation.setTargetVnfdId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createEightChangeVnfpkgMock() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 29, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 29, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":881},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":882}}");
        mockLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        mockLifecycleOperation.setTargetVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createNinthChangeVnfpkgMock(String id) {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(id);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 31, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 31, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":991},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":992}}");
        mockLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        mockLifecycleOperation.setTargetVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createTenthChangeVnfpkgMock() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 33, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 33, 53));
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        mockLifecycleOperation.setOperationParams(OPERATION_PARAMS_VALUE);
        mockLifecycleOperation.setValuesFileParams("{\"vnfc1\":{\"replicaCount\":1001},"
                                                           + "\"eric-pm-bulk-reporter\":{\"replicaCount\":1002}}");
        mockLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        mockLifecycleOperation.setTargetVnfdId(TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createTerminateMock() {
        LifecycleOperation mockLifecycleOperation = new LifecycleOperation();
        mockLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        mockLifecycleOperation.setVnfInstance(prepareVnfInstance());
        mockLifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        mockLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);
        mockLifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 20, 53));
        mockLifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 35, 31));
        mockLifecycleOperation.setOperationParams(null);
        mockLifecycleOperation.setValuesFileParams(null);
        mockLifecycleOperation.setSourceVnfdId(null);
        mockLifecycleOperation.setTargetVnfdId(null);
        return mockLifecycleOperation;
    }

    private LifecycleOperation createScaleMock() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.SCALE);
        lifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 17, 55));
        lifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 17, 55));
        return lifecycleOperation;
    }

    private ChangePackageOperationDetails createChangePackageOpDetailsMock() {
        ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setOperationOccurrenceId("downgrade-74f-4673-91ee-761fd83991e6");
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        changePackageOperationDetails.setTargetOperationOccurrenceId("downgrade-cf4-477c-aab3-21c454e6a389");
        return changePackageOperationDetails;
    }

    private HelmChartHistoryRecord createHelmChartHistoryRecordMock() {
        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        helmChartHistoryRecord.setLifecycleOperationId("downgrade-74f-4673-91ee-761fd83991e5");
        helmChartHistoryRecord.setReleaseName("downgrade-test-1");
        helmChartHistoryRecord.setHelmChartUrl("https://sky.net/helm/registry/magic-chart-238.tgz");
        helmChartHistoryRecord.setState("COMPLETED");
        return helmChartHistoryRecord;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataFor_TestProcessForDowngradeShouldReturnParamsFromValuesFileOfPreviousOperation() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        operation.setOperationState(LifecycleOperationState.COMPLETED);
        operation.setStateEnteredTime(LocalDateTime.of(2022, 10,30,10, 15, 30));
        operation.setStartTime(LocalDateTime.of(2022, 10,30,10, 15, 30));
        operation.setLifecycleOperationType(LifecycleOperationType.SCALE);
        operation.setOperationParams(null);
        operation.setValuesFileParams(null);
        operation.setSourceVnfdId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");
        operation.setTargetVnfdId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");

        LifecycleOperation firstLifecycleOperation = new LifecycleOperation();
        firstLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        firstLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        firstLifecycleOperation.setStateEnteredTime(LocalDateTime.now());
        firstLifecycleOperation.setStartTime(LocalDateTime.of(2022, 10, 30, 10, 15, 40, 545209));
        firstLifecycleOperation.setOperationParams(null);
        firstLifecycleOperation.setValuesFileParams(null);
        firstLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.SCALE);
        firstLifecycleOperation.setSourceVnfdId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");
        firstLifecycleOperation.setTargetVnfdId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");

        LifecycleOperation secondLifecycleOperation = new LifecycleOperation();
        secondLifecycleOperation.setOperationOccurrenceId(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        secondLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        secondLifecycleOperation.setStateEnteredTime(LocalDateTime.now());
        secondLifecycleOperation.setStartTime(LocalDateTime.of(2022, 10, 30, 10, 15, 20, 545209));
        secondLifecycleOperation.setOperationParams("{\"additionalParams\": {\"applicationTimeOut\": 300}}");
        secondLifecycleOperation.setValuesFileParams("{\"test_VDU_2\": {\"replicaCount\": 5}}}");
        secondLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        secondLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        secondLifecycleOperation.setTargetVnfdId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");

        LifecycleOperation thirdLifecycleOperation = new LifecycleOperation();
        thirdLifecycleOperation.setOperationOccurrenceId(SECOND_OPERATION_OCCURRENCE_ID_MOCK);
        thirdLifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        thirdLifecycleOperation.setStateEnteredTime(LocalDateTime.now());
        thirdLifecycleOperation.setStartTime(LocalDateTime.of(2022, 10, 30, 10, 15, 20, 545209));
        thirdLifecycleOperation.setOperationParams("{\"additionalParams\": {\"applicationTimeOut\": 100}}");
        thirdLifecycleOperation.setValuesFileParams("{\"test_VDU\": {\"replicaCount\": 3}}}");
        thirdLifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        thirdLifecycleOperation.setSourceVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        thirdLifecycleOperation.setTargetVnfdId(TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);
        operationsMap.put("operation", operation);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdLifecycleOperation", thirdLifecycleOperation);
        return operationsMap;
    }
    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessWithScale() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation scaleMock = createScaleMock();
        LifecycleOperation operation = createSixthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                   TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                                                   TARGET_VNFD_ID_FOR_SCALE_MOCK);

        LifecycleOperation sixthChangeVnfpkgMock = createSixthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                               TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                                                               TARGET_VNFD_ID_FOR_SCALE_MOCK);

        LifecycleOperation fourthChangeVnfpkgMock = createFourthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SCALE_MOCK);

        LifecycleOperation secondChangeVnfpkgMock = createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SCALE_MOCK);

        LifecycleOperation firstInstantiateMock = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                             TARGET_VNFD_ID_FOR_SCALE_MOCK,
                                                                             TARGET_VNFD_ID_FOR_SCALE_MOCK);
        operationsMap.put("scaleMock", scaleMock);
        operationsMap.put("operation", operation);
        operationsMap.put("sixthChangeVnfpkgMock", sixthChangeVnfpkgMock);
        operationsMap.put("fourthChangeVnfpkgMock", fourthChangeVnfpkgMock);
        operationsMap.put("secondChangeVnfpkgMock", secondChangeVnfpkgMock);
        operationsMap.put("firstInstantiateMock", firstInstantiateMock);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessAfterSelfUpgradeReturnsSameFile() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation currentOperation = createSecondInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                          TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                          TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        operationsMap.put("currentOperation", currentOperation);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessSelfUpgradeInProgress() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation currentOperation = createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                           TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                           null);

        LifecycleOperation firstLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        operationsMap.put("currentOperation", currentOperation);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessInProgressAfterSelfUpgrade() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation currentOperation = createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                           TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                           null);

        LifecycleOperation firstLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation secondLifecycleOperation =  createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation thirdLifecycleOperation = createThirdMockChangeVnfpkg(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                 null);
        operationsMap.put("currentOperation", currentOperation);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdLifecycleOperation", thirdLifecycleOperation);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessThreeSelfUpgradesAfterGeneralUpgrade() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation currentOperation = createFifthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                          TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                          null);

        LifecycleOperation firstLifecycleOperation = createSecondInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation secondLifecycleOperation = createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation thirdLifecycleOperation =  createThirdMockChangeVnfpkg(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                  TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                  TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation fourthLifecycleOperation = createFourthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation fifthLifecycleOperation = createFifthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                                 null);
        operationsMap.put("currentOperation", currentOperation);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdLifecycleOperation", thirdLifecycleOperation);
        operationsMap.put("fourthLifecycleOperation", fourthLifecycleOperation);
        operationsMap.put("fifthLifecycleOperation", fifthLifecycleOperation);
        return operationsMap;
    }
    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProcessReturnToSelfUpgradeValuesAfterRollback() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation currentOperation = createFifthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                          TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                          null);

        LifecycleOperation firstLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation secondLifecycleOperation = createSecondChangeVnfpkgMock(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation thirdLifecycleOperation = createThirdMockChangeVnfpkg(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_UPGRADE_MOCK);
        thirdLifecycleOperation.setStartTime(LocalDateTime.of(2022, 10, 12, 14, 25, 10));

        LifecycleOperation fourthLifecycleOperation = createFourthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation fifthLifecycleOperation = createFifthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                 TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                                 null);
        operationsMap.put("currentOperation", currentOperation);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdLifecycleOperation", thirdLifecycleOperation);
        operationsMap.put("fourthLifecycleOperation", fourthLifecycleOperation);
        operationsMap.put("fifthLifecycleOperation", fifthLifecycleOperation);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProceedTerminateAfterInstantiate() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation firstLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);
        operationsMap.put("firstLifecycleOperation", firstLifecycleOperation);
        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProceedTerminateAfterDowngrade() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation previousOperation = createThirdMockChangeVnfpkg(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                           TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK,
                                                                           TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation firstMockLifecycleOperation = createThirdMockChangeVnfpkg(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                     TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK,
                                                                                     TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation secondLifecycleOperation = createSecondChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                   TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);

        LifecycleOperation thirdMockLifecycleOperation = createFirstInstantiateMock(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation fourthMockLifecycleOperation = createTerminateMock();

        operationsMap.put("previousOperation", previousOperation);
        operationsMap.put("firstMockLifecycleOperation", firstMockLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdMockLifecycleOperation", thirdMockLifecycleOperation);
        operationsMap.put("fourthMockLifecycleOperation", fourthMockLifecycleOperation);

        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProceedTerminateAfterSelfUpgrade() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation previousOperation = createThirdMockChangeVnfpkg(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                           TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                           TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation firstMockLifecycleOperation =  createThirdMockChangeVnfpkg(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_UPGRADE_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation secondMockLifecycleOperation = createSecondChangeVnfpkgMock(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                       TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                       TARGET_VNFD_ID_FOR_UPGRADE_MOCK);

        LifecycleOperation thirdMockLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation fourthMockLifecycleOperation = createTerminateMock();

        operationsMap.put("previousOperation", previousOperation);
        operationsMap.put("firstMockLifecycleOperation", firstMockLifecycleOperation);
        operationsMap.put("secondMockLifecycleOperation", secondMockLifecycleOperation);
        operationsMap.put("thirdMockLifecycleOperation", thirdMockLifecycleOperation);
        operationsMap.put("fourthMockLifecycleOperation", fourthMockLifecycleOperation);

        return operationsMap;
    }

    private Map<String, LifecycleOperation> createLifecycleOperationsDataMockFor_TestProceedTerminateAfterComplexUpgradesSequence() {
        Map<String, LifecycleOperation> operationsMap = new HashMap<>();
        LifecycleOperation previousOperation = createNinthChangeVnfpkgMock(SECOND_OPERATION_OCCURRENCE_ID_MOCK);
        LifecycleOperation firstMockLifecycleOperation = createTenthChangeVnfpkgMock();
        LifecycleOperation secondLifecycleOperation = createNinthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK);
        LifecycleOperation thirdMockLifecycleOperation = createEightChangeVnfpkgMock();

        LifecycleOperation fourthMockLifecycleOperation = createThirdMockChangeVnfpkg(SECOND_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation fifthMockLifecycleOperation = createSeventhChangeVnfpkgMock();

        LifecycleOperation sixthMockLifecycleOperation = createSixthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                     TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK,
                                                                                     TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);

        LifecycleOperation seventhMockLifecycleOperation = createFourthChangeVnfpkgMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                        TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                        TARGET_VNFD_ID_FOR_DOWNGRADE_MOCK);

        LifecycleOperation eightMockLifecycleOperation = createSecondChangeVnfpkgMock(THIRD_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                      TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation ninthMockLifecycleOperation = createFirstInstantiateMock(FIRST_OPERATION_OCCURRENCE_ID_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK,
                                                                                    TARGET_VNFD_ID_FOR_SELF_UPGRADE_MOCK);

        LifecycleOperation tenthMockLifecycleOperation = createTerminateMock();

        operationsMap.put("previousOperation", previousOperation);
        operationsMap.put("firstMockLifecycleOperation", firstMockLifecycleOperation);
        operationsMap.put("secondLifecycleOperation", secondLifecycleOperation);
        operationsMap.put("thirdMockLifecycleOperation", thirdMockLifecycleOperation);
        operationsMap.put("fourthMockLifecycleOperation", fourthMockLifecycleOperation);
        operationsMap.put("fifthMockLifecycleOperation", fifthMockLifecycleOperation);
        operationsMap.put("sixthMockLifecycleOperation", sixthMockLifecycleOperation);
        operationsMap.put("seventhMockLifecycleOperation", seventhMockLifecycleOperation);
        operationsMap.put("eightMockLifecycleOperation", eightMockLifecycleOperation);
        operationsMap.put("ninthMockLifecycleOperation", ninthMockLifecycleOperation);
        operationsMap.put("tenthMockLifecycleOperation", tenthMockLifecycleOperation);
        return operationsMap;
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.delete(currentValuePath);
    }
}
