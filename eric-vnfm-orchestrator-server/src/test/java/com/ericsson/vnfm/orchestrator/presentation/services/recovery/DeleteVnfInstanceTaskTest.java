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
package com.ericsson.vnfm.orchestrator.presentation.services.recovery;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_VNF_INSTANCE;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.TaskRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;


@SpringBootTest(classes = {
        DatabaseInteractionService.class
})
@MockBean(classes = {
        LifecycleOperationRepository.class,
        ClusterConfigFileRepository.class,
        ClusterConfigInstanceRepository.class,
        VnfInstanceNamespaceDetailsRepository.class,
        ScaleInfoRepository.class,
        ChangedInfoRepository.class,
        VnfResourceViewRepository.class,
        HelmChartRepository.class,
        PartialSelectionQueryExecutor.class,
        TaskRepository.class,
        LcmOperationsConfig.class,
        LifecycleOperationStageRepository.class
})
public class DeleteVnfInstanceTaskTest {

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LcmOperationsConfig lcmOperationsConfig;

    @BeforeEach
    public void setUp() throws Exception {
        when(lcmOperationsConfig.getLcmOperationsLimit()).thenReturn(Integer.MAX_VALUE);
    }

    @Test
    public void testDeleteVnfInstanceFailedOperationInProgress() {
        when(vnfInstanceRepository.findById("instance-id")).thenReturn(getVnfInstance());
        when(operationsInProgressRepository.findByVnfId(any())).thenReturn(Optional.of(new OperationInProgress()));

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteVnfInstanceTask(Optional.empty(), task, databaseInteractionService);
        taskProcessor.execute();

        verify(databaseInteractionService, times(0)).deleteVnfInstance(any());
        verify(databaseInteractionService).deleteTask(task);
    }

    @Test
    public void testDeleteVnfInstanceFailedInstanceInInstantiatedState() {
        when(vnfInstanceRepository.findById("instance-id")).thenReturn(getVnfInstance());
        when(operationsInProgressRepository.findByVnfId(any())).thenReturn(Optional.empty());

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteVnfInstanceTask(Optional.empty(), task, databaseInteractionService);
        taskProcessor.execute();

        verify(databaseInteractionService, times(0)).deleteVnfInstance(any());
        verify(databaseInteractionService).deleteTask(task);
    }

    @Test
    public void testDeleteVnfInstanceSuccess() {
        Optional<VnfInstance> vnfInstance = getVnfInstance();
        vnfInstance.get().setInstantiationState(NOT_INSTANTIATED);

        when(vnfInstanceRepository.findById("instance-id")).thenReturn(vnfInstance);
        when(operationsInProgressRepository.findByVnfId(any())).thenReturn(Optional.empty());

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteVnfInstanceTask(Optional.empty(), task, databaseInteractionService);

        taskProcessor.execute();
        assertThatNoException();
    }

    @Test
    public void testDeleteVnfInstanceSuccessWithVnfInstanceAlreadyDeleted() {
        when(vnfInstanceRepository.findById("instance-id")).thenThrow(NotFoundException.class);

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteVnfInstanceTask(Optional.empty(), task, databaseInteractionService);

        taskProcessor.execute();
        assertThatNoException();
        verify(databaseInteractionService, times(1)).deleteAllClusterConfigInstancesByInstanceId("instance-id");
        verify(databaseInteractionService, times(1)).deleteInstanceDetailsByVnfInstanceId("instance-id");
    }

    private Task prepareTask() {
        Task task = new Task();
        task.setVnfInstanceId("instance-id");
        task.setTaskName(DELETE_VNF_INSTANCE);

        return task;
    }

    private Optional<VnfInstance> getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setInstantiationState(INSTANTIATED);

        return Optional.of(vnfInstance);
    }
}