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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource(properties = {"spring.datasource.hikari.validationTimeout=15000"})
public class ChangeVnfPackageOperationTest extends AbstractDbSetupTest {

    @Autowired
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private RollbackOperation rollbackOperation;

    @Autowired
    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ChangedInfoRepository changedInfoRepository;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private PackageService packageService;

    @Test
    public void operationInProcessingWhichCompletesSingleHelmChart() {
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, "msg-chg-complete");
        final String lifecycleOperationId = "865e3873-6a0e-443c-9b0c-4da9d9c2ab71";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        completed.setReleaseName(priorToMessage.getVnfInstance().getVnfInstanceName());

        changeVnfPackageOperation.completed(completed);

        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();

        // Operation has been updated wih values as is successful
        VnfInstance instance = operation.getVnfInstance();
        assertThat(operation.getTargetVnfdId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(instance.getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(instance.getCombinedValuesFile());

        // Instance has been updated
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNotNull();
        assertThat(instance.getVnfDescriptorId()).isEqualTo("68aea56b-14e8-4850-b2a6-48f785042127");
        assertThat(instance.getVnfPackageId()).isEqualTo("68aea56b-14e8-4850-b2a6-48f785042127");

        // Scale levels have been reset
        List<ScaleInfoEntity> scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(0, 0, 0, 0);

        // Chart state has been set to completed.
        List<HelmChart> helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts.get(0).getState()).isEqualTo(HelmReleaseState.COMPLETED.toString());

        // HelmChartHistoryRecords were stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmCharts() {
        final String firstReleaseName = "msg-multi-chart-chg-1";
        final String secondReleaseName = "msg-multi-chart-chg-2";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "521ecc62-420d-49bd-aa7d-705dd926e6e1";
        setExpiryTimeout(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        completed.setReleaseName(firstReleaseName);

        whenWfsUpgradeRespondsWithAccepted();
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());

        changeVnfPackageOperation.completed(completed);

        // Operation is still processing
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getStateEnteredTime()).isBefore(LocalDateTime.now());

        // Scale levels have not been reset
        VnfInstance instance = operation.getVnfInstance();
        List<ScaleInfoEntity> scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(3, 4);

        // VnfPackageId and VnfDescriptorIds are not updated
        assertThat(instance.getVnfDescriptorId()).isEqualTo("rrdef1ce-4cf4-477c-aab3-21c454e6a389");
        assertThat(instance.getVnfPackageId()).isEqualTo("9392468011745350001");

        // Chart State has been set to completed;
        String tempInstanceString = instance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.PROCESSING.toString()));

        completed.setReleaseName(secondReleaseName);
        changeVnfPackageOperation.completed(completed);

        // Operation has been updated
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isBefore(operation.getStateEnteredTime());

        // Operation has been updated wih values as is successful
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfInstance.getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(vnfInstance.getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(vnfInstance.getCombinedValuesFile());

        // Scale levels have been reset
        scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(0, 0, 0, 0);

        // Chart state has been set to completed.
        helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()));

        // HelmChartHistoryRecords were stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(vnfInstance.getHelmCharts().size());

        // VnfPackageId and VnfDescriptorIds are updated
        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo("68aea56b-14e8-4850-b2a6-48f785042127");
        assertThat(vnfInstance.getVnfPackageId()).isEqualTo("68aea56b-14e8-4850-b2a6-48f785042127");
    }

    @Test
    public void operationInProcessingWhichFailsSingleHelmChart() {
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, "msg-chg-complete-a");
        failed.setMessage("Failed to upgrade");
        final String lifecycleOperationId = "837dbdb3-2240-4d3b-b840-c9deafa98c83";
        failed.setLifecycleOperationId(lifecycleOperationId);
        setExpiryTimeout(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(failed);

        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to upgrade");

        // Chart has been updated
        VnfInstance instance = operation.getVnfInstance();
        String tempInstanceString = instance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts.get(0).getState()).isEqualTo(HelmReleaseState.ROLLING_BACK.toString());

        rollbackOperation.completed(failed);

        // Operation has been updated
        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to upgrade");

        // Operation has not been updated wih values as is failed
        assertThat(operationOccurrenceId.getTargetVnfdId()).isNull();
        assertThat(operationOccurrenceId.getCombinedAdditionalParams()).isNull();
        assertThat(operationOccurrenceId.getCombinedValuesFile()).isNull();

        // Chart has been updated
        List<HelmChart> charts = helmChartRepository.findByVnfInstance(operation.getVnfInstance());
        assertThat(charts.get(0).getState()).isEqualTo(HelmReleaseState.ROLLED_BACK.toString());

        // HelmChartHistoryRecords were not stored as operation failed
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(0);
    }

    @Test
    public void operationInProcessingWhichFailsFirstOfMultipleHelmCharts() {
        final String firstReleaseName = "msg-multi-chart-chg-1st-fails-1";
        final String secondReleaseName = "msg-multi-chart-chg-1st-fails-2";
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, firstReleaseName);
        failed.setMessage("Failed to upgrade");
        final String lifecycleOperationId = "8f8f558f-ace7-40ec-bd66-8945756dde6b";
        setExpiryTimeout(lifecycleOperationId);

        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(failed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to upgrade");

        // Chart has been updated
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.ROLLING_BACK.toString()),
                        tuple(secondReleaseName, null));

        rollbackOperation.completed(failed);

        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to upgrade");

        // Operation has not been updated wih values as is failed
        assertThat(operationOccurrenceId.getTargetVnfdId()).isNull();
        assertThat(operationOccurrenceId.getCombinedAdditionalParams()).isNull();
        assertThat(operationOccurrenceId.getCombinedValuesFile()).isNull();

        List<HelmChart> instanceCharts = helmChartRepository.findByVnfInstance(operation.getVnfInstance());
        assertThat(instanceCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.ROLLED_BACK.toString()),
                        tuple(secondReleaseName, null));

        // HelmChartHistoryRecords were not stored as operation failed
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(0);
    }

    @Test
    public void operationInProcessingWhichFailsSecondOfMultipleHelmCharts() {
        final String firstReleaseName = "msg-multi-chart-chg-2nd-fails-1";
        final String secondReleaseName = "msg-multi-chart-chg-2nd-fails-2";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "cc03f37e-e692-4bc9-9b52-33f2e17c6dce";
        setExpiryTimeout(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToTest = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = priorToTest.getVnfInstance();
        vnfInstance.setTempInstance(convertObjToJsonString(vnfInstance));
        vnfInstanceRepository.save(vnfInstance);

        whenWfsUpgradeRespondsWithAccepted();
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());

        changeVnfPackageOperation.completed(completed);

        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, secondReleaseName);
        failed.setMessage("Failed to upgrade");

        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(failed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to upgrade");

        // Chart has been updated
        VnfInstance instance = operation.getVnfInstance();
        String tempInstanceString = instance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.PROCESSING.toString()),
                        tuple(secondReleaseName, HelmReleaseState.ROLLING_BACK.toString()));

        whenWfsTerminateRespondsWithAccepted();

        rollbackOperation.completed(failed);

        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to upgrade");

        // Operation has not been updated wih values as is failed
        assertThat(operationOccurrenceId.getTargetVnfdId()).isNull();
        assertThat(operationOccurrenceId.getCombinedAdditionalParams()).isNull();
        assertThat(operationOccurrenceId.getCombinedValuesFile()).isNull();

        // Chart has been updated
        instance = operationOccurrenceId.getVnfInstance();
        tempInstanceString = instance.getTempInstance();
        upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.ROLLING_BACK.toString()),
                        tuple(secondReleaseName, HelmReleaseState.ROLLED_BACK.toString()));

        // HelmChartHistoryRecords were not stored as operation failed
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(0);
    }

    @Test
    public void operationInProcessingWhichFailsWithNoErrorMessage() {
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, "msg-chg-complete");
        final String lifecycleOperationId = "ab3d12f2-8084-4975-9d33-0577aedc61b7";
        failed.setLifecycleOperationId(lifecycleOperationId);
        setExpiryTimeout(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(failed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failure event");

        // Operation has not been updated wih values as is failed
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();

        // HelmChartHistoryRecords were not stored as operation failed
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(0);
    }

    @Test
    public void testChangePackageInfoOnCompleteDowngradeSingleChart() {
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, "change-package-info-release-name");
        final String lifecycleOperationId = "87ebcbc8-474f-4673-91ee-656fd8366666";
        completed.setLifecycleOperationId(lifecycleOperationId);
        setExpiryTimeout(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime previousEnteredTime = priorToMessage.getStateEnteredTime();
        completed.setReleaseName(priorToMessage.getVnfInstance().getVnfInstanceName());

        whenOnboardingRespondsWith("change-vnfpkg/descriptor-model.json");
        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.completed(completed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(previousEnteredTime.isBefore(operation.getStateEnteredTime())).isTrue();

        VnfInstance instance = operation.getVnfInstance();
        assertThat(operation.getTargetVnfdId()).isEqualTo("e3def1ce-4236-477c-abb3-21c454e6a645");
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(instance.getCombinedAdditionalParams());

        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);

        List<HelmChart> helmCharts = tempInstance.getHelmCharts();

        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple("change-package-info-release-name", HelmReleaseState.COMPLETED.toString()));
    }

    @Test
    public void testChangePackageInfoOnCompleteDowngradeMultipleChart() {
        final String firstReleaseName = "change-package-info-release-name2";
        final String secondReleaseName = "change-package-info-release-name3";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "87ebcbc8-474f-4673-91ee-645369384739";
        setExpiryTimeout(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime previousEnteredTime = priorToMessage.getStateEnteredTime();
        completed.setReleaseName(firstReleaseName);

        whenOnboardingRespondsWith("change-vnfpkg/descriptor-model.json");
        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.completed(completed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(previousEnteredTime.isBefore(operation.getStateEnteredTime())).isTrue();

        VnfInstance instance = operation.getVnfInstance();

        assertThat(instance.getVnfDescriptorId()).isEqualTo("83dee1ce-8ab5-477c-aab3-215434e6666");
        assertThat(instance.getVnfPackageId()).isEqualTo("6408748274545356324");

        String tempInstanceString = instance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()));

        completed.setReleaseName(secondReleaseName);
        changeVnfPackageOperation.completed(completed);

        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(previousEnteredTime).isBefore(operation.getStateEnteredTime());

        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(operation.getTargetVnfdId()).isEqualTo("e3def1ce-4236-477c-abb3-21c454e6a645");

        helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()));

        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo("83dee1ce-8ab5-477c-aab3-215434e6666");
        assertThat(vnfInstance.getVnfPackageId()).isEqualTo("6408748274545356324");
    }

    @Test
    public void testChangePackageInfoOnFailedDowngradeWhenOperationIsUnfinished() {
        HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, "msg-chg-complete");
        final String lifecycleOperationId = "78finish-474f-4673-91ee-656fd8366666";
        failed.setMessage("Failed to upgrade");
        failed.setLifecycleOperationId(lifecycleOperationId);

        final PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId("d3def1ce-4cf4-477c-aab3-21cb04e6a379");
        when(packageService.getPackageInfo(anyString())).thenReturn(packageResponse);

        changeVnfPackageOperation.failed(failed);

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(lifecycleOperation).isNotNull();
        assertThat(StringUtils.isNotEmpty(lifecycleOperation.getError())).isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(lifecycleOperation.getVnfInstance().getHelmCharts().stream()
                           .allMatch(chart ->
                                             StringUtils.equals(chart.getState(), LifecycleOperationState.FAILED.toString())))
                .isTrue();

        Optional<VnfInstanceNamespaceDetails> namespaceDetails =
                databaseInteractionService.getNamespaceDetails(lifecycleOperation.getVnfInstance().getVnfInstanceId());
        assertThat(namespaceDetails).hasValueSatisfying(details -> assertThat(details.isDeletionInProgress()).isFalse());
    }

    @Test
    public void modifyOperationInProcessingWhichCompletesSingleHelmChart() {
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, "msg-chg-complete");
        final String lifecycleOperationId = "865e3873-6a0e-443c-9b0c-4da9d9c2ab7";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        completed.setReleaseName(priorToMessage.getVnfInstance().getVnfInstanceName());

        // save temp instance
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        VnfInstance tempInstance = vnfInstanceRepository.findByVnfInstanceId("865e3873-6a0e-443c-9b0c-4da9d9c2543-temp");
        VnfInstance actualIns = vnfInstanceRepository.findByVnfInstanceId(priorToMessage.getVnfInstance().getVnfInstanceId());
        actualIns.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        vnfInstanceRepository.save(actualIns);

        ChangedInfo priorToMessageChangedInfo = changedInfoRepository.findById(priorToMessage.getOperationOccurrenceId()).orElse(null);

        assertThat(priorToMessageChangedInfo).isNotNull();
        assertThat(tempInstance.getVnfInstanceDescription()).isEqualTo(priorToMessageChangedInfo.getVnfInstanceDescription());
        assertThat(tempInstance.getMetadata()).isEqualTo(priorToMessageChangedInfo.getMetadata());
        assertThat(tempInstance.getVnfPackageId()).isEqualTo(priorToMessageChangedInfo.getVnfPkgId());
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo(priorToMessage.getVnfInfoModifiableAttributesExtensions());

        changeVnfPackageOperation.completed(completed);

        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();

        // Operation has been updated wih values as is successful
        VnfInstance instance = operation.getVnfInstance();
        ChangedInfo changedInfo = changedInfoRepository.findById(operation.getOperationOccurrenceId()).orElse(null);
        assertThat(operation.getTargetVnfdId()).isEqualTo(instance.getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(instance.getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(instance.getCombinedValuesFile());
        assertThat(changedInfo).isNotNull();
        assertModifiedAttributes(instance, operation, changedInfo);

        // Instance has been updated
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);

        // Scale levels have been reset
        List<ScaleInfoEntity> scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(0, 0);

        // Chart state has been set to completed.
        List<HelmChart> helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts.get(0).getState()).isEqualTo(HelmReleaseState.COMPLETED.toString());

        // HelmChartHistoryRecords were stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void modifyOperationInProcessingWhichCompletesMultipleHelmCharts() {
        final String firstReleaseName = "msg-mod-multi-complete-1";
        final String secondReleaseName = "msg-mod-multi-complete-2";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "865e3873-6a0e-443c-9b0c-4da9d9c23434";
        setExpiryTimeout(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        completed.setReleaseName(firstReleaseName);

        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance tempInstance = vnfInstanceRepository.findByVnfInstanceId("865e3873-6a0e-443c-9b0c-4da9d9c2222-temp");
        VnfInstance actualIns = vnfInstanceRepository.findByVnfInstanceId(priorToMessage.getVnfInstance().getVnfInstanceId());
        actualIns.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        ChangedInfo priorToMessageChangedInfo = changedInfoRepository.findById(priorToMessage.getOperationOccurrenceId()).orElse(null);
        assertThat(priorToMessageChangedInfo).isNotNull();
        assertThat(tempInstance.getVnfInstanceDescription()).isEqualTo(priorToMessageChangedInfo.getVnfInstanceDescription());
        assertThat(tempInstance.getMetadata()).isEqualTo(priorToMessageChangedInfo.getMetadata());
        assertThat(tempInstance.getVnfPackageId()).isEqualTo(priorToMessageChangedInfo.getVnfPkgId());
        assertThat(tempInstance.getVnfInfoModifiableAttributesExtensions()).isEqualTo(priorToMessage.getVnfInfoModifiableAttributesExtensions());
        vnfInstanceRepository.save(actualIns);

        whenWfsUpgradeRespondsWithAccepted();
        when(packageService.getPackageInfo(any())).thenReturn(new PackageResponse());

        changeVnfPackageOperation.completed(completed);

        // Operation is still processing
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getStateEnteredTime()).isBefore(LocalDateTime.now());

        // Scale levels have not been reset
        VnfInstance instance = operation.getVnfInstance();
        List<ScaleInfoEntity> scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(3, 3);

        //  VnfDescriptorId is not updated
        assertThat(instance.getVnfDescriptorId()).isEqualTo("rrdef1ce-4cf4-477c-aab3-21c454e6a389");

        // Chart State has been set to completed;
        String tempInstanceString = instance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = upgradedInstance.getHelmCharts();
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.PROCESSING.toString()));

        completed.setReleaseName(secondReleaseName);
        changeVnfPackageOperation.completed(completed);

        // Operation has been updated
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isBefore(operation.getStateEnteredTime());

        // Operation has been updated wih values as is successful
        VnfInstance vnfInstance = operation.getVnfInstance();
        ChangedInfo changedInfo = changedInfoRepository.findById(operation.getOperationOccurrenceId()).orElse(null);
        assertThat(operation.getTargetVnfdId()).isEqualTo(vnfInstance.getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(vnfInstance.getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(vnfInstance.getCombinedValuesFile());
        assertThat(changedInfo).isNotNull();
        assertModifiedAttributes(vnfInstance, operation, changedInfo);

        // Scale levels have been reset
        scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(vnfInstance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(0, 0);

        // Chart state has been set to completed.
        helmCharts = helmChartRepository.findByVnfInstance(vnfInstance);
        assertThat(helmCharts)
                .extracting("releaseName", "state")
                .contains(
                        tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()));

        // HelmChartHistoryRecords were stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(vnfInstance.getHelmCharts().size());

        // VnfDescriptorId is not updated
        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo("rrdef1ce-4cf4-477c-aab3-21c454e6a389");
    }

    private HelmReleaseLifecycleMessage createMessage(final HelmReleaseState success, final String releaseName) {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName(releaseName);
        message.setState(success);
        message.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        message.setRevisionNumber("1");
        return message;
    }

    private void whenWfsRollbackRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("rollback");
    }

    private void whenWfsTerminateRespondsWithAccepted() {
        when(restTemplate.exchange(contains("terminate"), eq(HttpMethod.POST), any(), eq(ResourceResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));
    }

    private void whenWfsUpgradeRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("upgrade");
    }

    @SuppressWarnings("unchecked")
    private void whenWfsRespondsWithAccepted(final String urlEnd) {
        when(restTemplate.exchange(endsWith(urlEnd), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private void whenOnboardingRespondsWith(final String fileName) {
        when(packageService.getPackageInfoWithDescriptorModel(anyString())).thenReturn(readObject(fileName, PackageResponse.class));
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readDataFromFile(getClass(), fileName), targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertModifiedAttributes(VnfInstance upgradedInstance, LifecycleOperation operation, ChangedInfo changedInfo) {

        assertThat(changedInfo.getMetadata()).isEqualTo(upgradedInstance.getMetadata());
        assertThat(changedInfo.getVnfInstanceDescription()).isEqualTo(upgradedInstance.getVnfInstanceDescription());
        assertThat(changedInfo.getVnfPkgId()).isEqualTo(upgradedInstance.getVnfPackageId());
        assertThat(operation.getVnfInfoModifiableAttributesExtensions()).isEqualTo(upgradedInstance.getVnfInfoModifiableAttributesExtensions());
    }

    private void setExpiryTimeout(final String lifecycleOperationId) {
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        byOperationOccurrenceId.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        byOperationOccurrenceId.setApplicationTimeout("80");
        lifecycleOperationRepository.save(byOperationOccurrenceId);
    }
}
