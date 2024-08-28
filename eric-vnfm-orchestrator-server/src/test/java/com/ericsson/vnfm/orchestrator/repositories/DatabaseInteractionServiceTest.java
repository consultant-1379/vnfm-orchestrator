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
package com.ericsson.vnfm.orchestrator.repositories;

import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RunningLcmOperationsAmountExceededException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatabaseInteractionServiceTest {

    public static final int RUNNING_LCM_OPERATION_AMOUNT_LIMIT = 30;

    @Mock
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Mock
    private LifecycleOperationRepository lifecycleOperationRepository;
    @Mock
    private LcmOperationsConfig lcmOperationsConfig;
    @Mock
    private VnfInstanceRepository vnfInstanceRepository;
    @Mock
    private OperationsInProgressRepository operationsInProgressRepository;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DatabaseInteractionService databaseInteractionService;

    @BeforeEach
    public void setUp() throws Exception {
        when(lcmOperationsConfig.getLcmOperationsLimit()).thenReturn(Integer.MAX_VALUE);
    }

    @Test
    public void isNamespaceSetForDeletionShouldReturnTrue() {
        // given
        final var namespaceDetails = new VnfInstanceNamespaceDetails();
        namespaceDetails.setDeletionInProgress(true);

        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(anyString())).thenReturn(Optional.of(namespaceDetails));

        // when and then
        assertThat(databaseInteractionService.isNamespaceSetForDeletion("instance-id")).isTrue();
    }

    @Test
    public void isNamespaceSetForDeletionShouldReturnFalse() {
        // given
        final var namespaceDetails = new VnfInstanceNamespaceDetails();
        namespaceDetails.setDeletionInProgress(false);

        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(anyString())).thenReturn(Optional.of(namespaceDetails));

        // when and then
        assertThat(databaseInteractionService.isNamespaceSetForDeletion("instance-id")).isFalse();
    }

    @Test
    public void isNamespaceSetForDeletionShouldThrowException() {
        // given
        when(vnfInstanceNamespaceDetailsRepository.findByVnfId(anyString())).thenReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> databaseInteractionService.isNamespaceSetForDeletion("instance-id"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Instance instance-id does not exist");
    }

    @Test
    public void getNamespaceDetailsPresentShouldReturnEmptyDetailsWhenNamespaceDoesNotMatch() {
        // given
        when(vnfInstanceNamespaceDetailsRepository.findByNamespaceAndClusterServer(anyString(), anyString())).thenReturn(emptyList());

        // when and then
        assertThat(databaseInteractionService.getNamespaceDetailsPresent("namespace", "server2")).isEmpty();
    }

    @Test
    public void testPersistOperationFailsWhenRunningLcmOperationsAmountExceeded() {
        when(lifecycleOperationRepository.countByOperationStateNotIn(any()))
                .thenReturn(RUNNING_LCM_OPERATION_AMOUNT_LIMIT);
        when(lcmOperationsConfig.getLcmOperationsLimit())
                .thenReturn(RUNNING_LCM_OPERATION_AMOUNT_LIMIT);

        assertThatThrownBy(() -> databaseInteractionService.persistLifecycleOperationInProgress(new LifecycleOperation(),
                                                                                                new VnfInstance(),
                                                                                                LifecycleOperationType.SCALE))
                .isInstanceOf(RunningLcmOperationsAmountExceededException.class)
                .hasMessageContaining("Operation cannot be created due to reached global limit of concurrent LCM operations: 30");
    }

    @Test
    public void testPersistOperationSucceedWhenRunningLcmOperationsAmountNotExceeded() {
        final int amountOfRunningLcmOperationWhenOneMoreIsAllowed = RUNNING_LCM_OPERATION_AMOUNT_LIMIT - 1;
        when(lifecycleOperationRepository.countByOperationStateNotIn(any()))
                .thenReturn(amountOfRunningLcmOperationWhenOneMoreIsAllowed);
        when(lcmOperationsConfig.getLcmOperationsLimit())
                .thenReturn(RUNNING_LCM_OPERATION_AMOUNT_LIMIT);
        when(operationsInProgressRepository.findByVnfId(any()))
                .thenReturn(Optional.empty());

        databaseInteractionService.persistLifecycleOperationInProgress(new LifecycleOperation(),
                                                                       new VnfInstance(),
                                                                       LifecycleOperationType.SCALE);
    }

    @Test
    public void testDeleteTaskCallsDeleteById() {
        final ArgumentCaptor<Integer> taskIdCaptor = ArgumentCaptor.forClass(Integer.class);

        final Task task = new Task();
        task.setId(123);

        databaseInteractionService.deleteTask(task);

        verify(taskRepository).deleteById(taskIdCaptor.capture());
        assertThat(taskIdCaptor.getValue()).isEqualTo(123);
    }

    @Test
    public void testDeleteTasksCallsDeleteAllById() {
        @SuppressWarnings("unchecked") final ArgumentCaptor<Iterable<Integer>> taskIdsCaptor = ArgumentCaptor.forClass(Iterable.class);

        final Task task1 = new Task();
        task1.setId(123);
        final Task task2 = new Task();
        task2.setId(456);
        final List<Task> tasks = List.of(task1, task2);

        databaseInteractionService.deleteTasks(tasks);

        verify(taskRepository).deleteAllById(taskIdsCaptor.capture());
        assertThat(taskIdsCaptor.getValue()).containsOnly(123, 456);
    }
}
