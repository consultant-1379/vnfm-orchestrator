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
package com.ericsson.vnfm.orchestrator.scheduler;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import static com.ericsson.vnfm.orchestrator.TestUtils.getVnfInstance;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLING_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.STARTING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.MODIFY_INFO;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SCALE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SYNC;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

@RunWith(SpringRunner.class)
public class CheckApplicationTimeoutTest extends AbstractDbSetupTest {
    @Autowired
    private CheckApplicationTimeout checkApplicationTimeout;
    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;
    @Autowired
    private DatabaseInteractionService databaseInteractionService;
    @MockBean
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;
    @Captor
    private ArgumentCaptor<VnfInstanceNamespaceDetails> savedVnfInstanceNamespaceDetails;

    @Test
    public void shouldFailHealOperationByTimeout() {
        VnfInstance vnfInstance = getVnfInstance();
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, HEAL, ROLLING_BACK));

        checkApplicationTimeout.checkForApplicationOut();

        var afterCheckApplicationTimeout = databaseInteractionService.getLifecycleOperation(lifecycleOperation.getOperationOccurrenceId());

        assertThat(afterCheckApplicationTimeout.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(afterCheckApplicationTimeout.getVnfInstance().getHelmCharts().get(0).getState()).isEqualTo("FAILED");
        assertThat(afterCheckApplicationTimeout.getVnfInstance().getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
    }

    @Test
    public void shouldFailSyncOperationByTimeout() throws JsonProcessingException {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setTempInstance(new ObjectMapper().writeValueAsString(vnfInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, SYNC, STARTING));

        checkApplicationTimeout.checkForApplicationOut();

        var afterCheckApplicationTimeout = databaseInteractionService.getLifecycleOperation(lifecycleOperation.getOperationOccurrenceId());

        assertThat(afterCheckApplicationTimeout.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(afterCheckApplicationTimeout.getVnfInstance().getInstantiationState()).isEqualTo(INSTANTIATED);
    }

    @Test
    public void checkFlagDeletionInProgressAfterTimeout() {
        VnfInstance vnfInstance = getVnfInstance();
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        VnfInstanceNamespaceDetails vnfInstanceNamespaceDetails = new VnfInstanceNamespaceDetails();
        vnfInstanceNamespaceDetails.setVnfId(vnfInstance.getVnfInstanceId());
        vnfInstanceNamespaceDetails.setDeletionInProgress(true);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, INSTANTIATE, PROCESSING));

        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstance.getVnfInstanceId())).thenReturn(Optional.of(vnfInstanceNamespaceDetails));

        checkApplicationTimeout.checkForApplicationOut();

        verify(vnfInstanceNamespaceDetailsRepository).save(savedVnfInstanceNamespaceDetails.capture());

        var afterCheckApplicationTimeout = databaseInteractionService.getLifecycleOperation(lifecycleOperation.getOperationOccurrenceId());

        assertThat(afterCheckApplicationTimeout.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(savedVnfInstanceNamespaceDetails.getValue().isDeletionInProgress()).isFalse();
    }

    @Test
    public void shouldFailModifyVnfOperationByTimeout() {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, MODIFY_INFO, ROLLING_BACK));

        checkApplicationTimeout.checkForApplicationOut();

        var afterCheckApplicationTimeout = databaseInteractionService.getLifecycleOperation(lifecycleOperation.getOperationOccurrenceId());
        assertThat(afterCheckApplicationTimeout.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(afterCheckApplicationTimeout.getVnfInstance().getInstantiationState()).isEqualTo(INSTANTIATED);
    }

    @Test
    public void shouldHaveStateFailedAfterCheckApplicationTimeoutInstantiateTest() {
        VnfInstance vnfInstance = getVnfInstance();

        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, INSTANTIATE, PROCESSING));

        var stateBeforeCheckForTimeout = lifecycleOperation.getOperationState();

        checkApplicationTimeout.checkForApplicationOut();

        var operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperation.getOperationOccurrenceId());

        var stateAfterCheckForTimeout = operation.getOperationState();

        assertThat(stateBeforeCheckForTimeout).isNotEqualTo(stateAfterCheckForTimeout);
        assertThat(stateAfterCheckForTimeout).isEqualTo(FAILED);
        assertThat(operation.getVnfInstance().getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(operation.getError()).contains(lifecycleOperation.getOperationOccurrenceId() + " failed due to timeout");
    }

    @Test
    public void shouldHaveStateFailedAfterCheckApplicationTimeoutTerminateTest() {
        VnfInstance vnfInstance = getVnfInstance();

        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, TERMINATE, PROCESSING));

        var stateBeforeCheckForTimeout = lifecycleOperation.getOperationState();

        checkApplicationTimeout.checkForApplicationOut();

        var operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperation.getOperationOccurrenceId());

        var stateAfterCheckForTimeout = operation.getOperationState();

        assertThat(stateBeforeCheckForTimeout).isNotEqualTo(stateAfterCheckForTimeout);
        assertThat(stateAfterCheckForTimeout).isEqualTo(FAILED);
        assertThat(operation.getVnfInstance().getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(operation.getError()).contains(lifecycleOperation.getOperationOccurrenceId() + " failed due to timeout");
    }

    @Test
    public void shouldHaveStateFailedAfterCheckApplicationTimeoutUpgradeTest() throws JsonProcessingException {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setTempInstance(new ObjectMapper().writeValueAsString(vnfInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, CHANGE_VNFPKG, PROCESSING));

        var stateBeforeCheckForTimeout = lifecycleOperation.getOperationState();

        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(anyString()))
                .thenReturn(Optional.of(getVnfInstanceNamespaceDetails()));

        checkApplicationTimeout.checkForApplicationOut();

        var operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperation.getOperationOccurrenceId());

        var stateAfterCheckForTimeout = operation.getOperationState();

        assertThat(stateBeforeCheckForTimeout).isNotEqualTo(stateAfterCheckForTimeout);
        assertThat(stateAfterCheckForTimeout).isEqualTo(FAILED);
        assertThat(operation.getVnfInstance().getInstantiationState()).isEqualTo(INSTANTIATED);
        assertThat(operation.getError()).contains(lifecycleOperation.getOperationOccurrenceId() + " failed due to timeout");
    }

    @Test
    public void shouldHaveStateFailedAfterCheckApplicationTimeoutScaleTest() throws JsonProcessingException {
        VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setTempInstance(new ObjectMapper().writeValueAsString(vnfInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LifecycleOperation lifecycleOperation =
                databaseInteractionService.persistLifecycleOperation(createLifecycleOperation(vnfInstance, SCALE, PROCESSING));

        var stateBeforeCheckForTimeout = lifecycleOperation.getOperationState();

        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(anyString()))
                .thenReturn(Optional.of(getVnfInstanceNamespaceDetails()));

        checkApplicationTimeout.checkForApplicationOut();

        var operation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperation.getOperationOccurrenceId());

        var stateAfterCheckForTimeout = operation.getOperationState();

        assertThat(stateBeforeCheckForTimeout).isNotEqualTo(stateAfterCheckForTimeout);
        assertThat(stateAfterCheckForTimeout).isEqualTo(FAILED);
        assertThat(operation.getVnfInstance().getInstantiationState()).isEqualTo(INSTANTIATED);
        assertThat(operation.getError()).contains(lifecycleOperation.getOperationOccurrenceId() + " failed due to timeout");
    }

    private static LifecycleOperation createLifecycleOperation(final VnfInstance vnfInstance,
                                                               final LifecycleOperationType operationType,
                                                               final LifecycleOperationState operationState) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setOperationState(operationState);
        lifecycleOperation.setLifecycleOperationType(operationType);
        lifecycleOperation.setSourceVnfdId("package-id-21cb04e7b489");
        lifecycleOperation.setTargetVnfdId("package-id-21cb04e7b489");
        lifecycleOperation.setStateEnteredTime(LocalDateTime.now().minusHours(1));
        lifecycleOperation.setStartTime(LocalDateTime.now().minusHours(1));
        lifecycleOperation.setExpiredApplicationTime(LocalDateTime.now().minusHours(1));
        return lifecycleOperation;
    }

    private static VnfInstanceNamespaceDetails getVnfInstanceNamespaceDetails() {
        VnfInstanceNamespaceDetails details = new VnfInstanceNamespaceDetails();
        details.setNamespace("testchangepackage");
        details.setId("upgrade-21cb04e6a378");
        details.setVnfId("upgrade-21cb04e6a378");
        details.setDeletionInProgress(false);
        return details;
    }

}
