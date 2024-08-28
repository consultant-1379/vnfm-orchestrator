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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;

import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ChangeVnfPackageServiceImpl.class,
        OperationsUtils.class,
        ObjectMapper.class})
@MockBean(classes = {
        HealOperation.class,
        HelmChartHistoryService.class,
        ReplicaDetailsMapper.class,
        RetryTemplate.class,
        LifecycleOperationRepository.class
})
public class ChangePackageInfoServiceTest {

    private static final String VNF_DESCRIPTOR_ID = "any-source-descriptor-id";
    private static final String VNFD_ID = "any-destination-descriptor-id";
    private static final String VNF_INSTANCE_ID = UUID.randomUUID().toString();

    private static final String RANDOM_STRING = UUID.randomUUID().toString();

    @Autowired
    private ChangeVnfPackageService changePackageInfoService;

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @MockBean
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @MockBean
    private LcmOpSearchService lcmOpSearchService;

    @Test
    public void testCheckInstanceOperationHistoryQualifiesForDowngrade() {
        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        when(lcmOpSearchService.searchAllCompleted(any(VnfInstance.class))).thenReturn(lifecycleOperationList());
        when(changePackageOperationDetailsRepository.findById(anyString())).thenReturn(Optional.of(changePackageOperationDetails()));
        when(helmChartHistoryRepository.findAllByLifecycleOperationIdOrderByPriorityAsc(anyString())).thenReturn(List.of(helmChartHistoryRecord));
        VnfInstance vnfInstance = createVnfInstance();

        assertThat(changePackageInfoService.getSuitableTargetDowngradeOperationFromVnfInstance(
                VNFD_ID, vnfInstance)).isPresent();
    }

    @Test
    public void testCheckInstanceOperationHistoryQualifiesForDowngradeShouldBePresent() {
        VnfInstance vnfInstance = createVnfInstance();

        assertThat(changePackageInfoService.getSuitableTargetDowngradeOperationFromVnfInstance(
                VNFD_ID, vnfInstance)).isNotPresent();
    }

    @Test
    public void testCheckInstanceOperationHistoryQualifiesForDowngradeNoChangeInTargetVersion() {
        VnfInstance vnfInstance = createVnfInstance();

        assertThat(changePackageInfoService.getSuitableTargetDowngradeOperationFromVnfInstance(
                VNFD_ID, vnfInstance)).isNotPresent();
    }

    @Test
    public void testCheckInstanceOperationHistoryQualifiesForDowngradeNoValuesSetForTargetAndSourceVnfdInOpHistory() {
        VnfInstance vnfInstance = createVnfInstance();
        assertThat(changePackageInfoService.getSuitableTargetDowngradeOperationFromVnfInstance(
                VNFD_ID, vnfInstance)).isNotPresent();
    }

    @Test
    public void testCheckInstanceOperationHistoryQualifiesForDowngradeDowngradeAlreadyOccurred() {
        VnfInstance vnfInstance = createVnfInstance();
        assertThat(changePackageInfoService.getSuitableTargetDowngradeOperationFromVnfInstance(
                VNFD_ID, vnfInstance)).isNotPresent();
    }

    @Test
    public void testIsDowngradeOperationShouldReturnTrue() {
        HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        final LifecycleOperation currentOperation = getLifecycleoperation();
        List<LifecycleOperation> allBeforeCurrentOperations = lifecycleOperationListBeforeCurrent();

        when(lcmOpSearchService.searchAllBefore(currentOperation)).thenReturn(allBeforeCurrentOperations);
        when(changePackageOperationDetailsRepository.findById(anyString())).thenReturn(Optional.of(changePackageOperationDetails()));
        when(helmChartHistoryRepository.findAllByLifecycleOperationIdOrderByPriorityAsc(anyString())).thenReturn(List.of(helmChartHistoryRecord));
        boolean actual = changePackageInfoService.isDowngrade(VNFD_ID, currentOperation);

        assertThat(actual).isTrue();
    }

    @Test
    public void testIsDowngradeOperationShouldReturnFalse() {
        boolean actual = changePackageInfoService.isDowngrade(VNFD_ID, getLifecycleoperation());
        assertThat(actual).isFalse();
    }

    private VnfInstance createVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId(VNF_INSTANCE_ID);
        instance.setVnfDescriptorId(VNF_DESCRIPTOR_ID);
        return instance;
    }

    private LifecycleOperation getLifecycleoperation() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation.setVnfInstance(createVnfInstance());
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.SCALE);
        lifecycleOperation.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation.setSourceVnfdId(VNF_DESCRIPTOR_ID);
        lifecycleOperation.setTargetVnfdId(VNF_DESCRIPTOR_ID);
        return lifecycleOperation;
    }

    private List<LifecycleOperation> lifecycleOperationListBeforeCurrent() {
        LifecycleOperation lifecycleOperation1 = new LifecycleOperation();
        lifecycleOperation1.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation1.setVnfInstance(createVnfInstance());
        lifecycleOperation1.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation1.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation1.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation1.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperation1.setOperationParams("{\"additionalParams\":{\"applicationTimeOut\":300}}");
        lifecycleOperation1.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation1.setSourceVnfdId(VNFD_ID);
        lifecycleOperation1.setTargetVnfdId(VNF_DESCRIPTOR_ID);

        LifecycleOperation lifecycleOperation2 = new LifecycleOperation();
        lifecycleOperation2.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation2.setVnfInstance(createVnfInstance());
        lifecycleOperation2.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation2.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation2.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation2.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperation2.setOperationParams("{\"additionalParams\":{\"applicationTimeOut\":100}}");
        lifecycleOperation2.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation2.setSourceVnfdId(VNFD_ID);
        lifecycleOperation2.setTargetVnfdId(VNFD_ID);

        return List.of(lifecycleOperation1, lifecycleOperation2);
    }
    private List<LifecycleOperation> lifecycleOperationList() {
        LifecycleOperation lifecycleOperation1 = new LifecycleOperation();
        lifecycleOperation1.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation1.setVnfInstance(createVnfInstance());
        lifecycleOperation1.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation1.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation1.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation1.setLifecycleOperationType(LifecycleOperationType.SCALE);
        lifecycleOperation1.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation1.setSourceVnfdId(VNF_DESCRIPTOR_ID);
        lifecycleOperation1.setTargetVnfdId(VNF_DESCRIPTOR_ID);

        LifecycleOperation lifecycleOperation2 = new LifecycleOperation();
        lifecycleOperation2.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation2.setVnfInstance(createVnfInstance());
        lifecycleOperation2.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation2.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation2.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation2.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        lifecycleOperation2.setOperationParams("{\"additionalParams\":{\"applicationTimeOut\":300}}");
        lifecycleOperation2.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation2.setSourceVnfdId(VNFD_ID);
        lifecycleOperation2.setTargetVnfdId(VNF_DESCRIPTOR_ID);

        LifecycleOperation lifecycleOperation3 = new LifecycleOperation();
        lifecycleOperation3.setOperationOccurrenceId(RANDOM_STRING);
        lifecycleOperation3.setVnfInstance(createVnfInstance());
        lifecycleOperation3.setOperationState(LifecycleOperationState.COMPLETED);
        lifecycleOperation3.setStateEnteredTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation3.setStartTime(LocalDateTime.of(2022, 1, 1, 1, 1));
        lifecycleOperation3.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperation3.setOperationParams("{\"additionalParams\":{\"applicationTimeOut\":100}}");
        lifecycleOperation3.setCancelMode(CancelModeType.FORCEFUL);
        lifecycleOperation3.setSourceVnfdId(VNFD_ID);
        lifecycleOperation3.setTargetVnfdId(VNFD_ID);

        return List.of(lifecycleOperation1, lifecycleOperation2, lifecycleOperation3);
    }

    private ChangePackageOperationDetails changePackageOperationDetails() {
        ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        return changePackageOperationDetails;
    }
}
