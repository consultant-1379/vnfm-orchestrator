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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.events.DeletePvcEvent;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;


@SpringBootTest (classes = DeletePvcEvent.class)
public class DeletePvcEventTest {

    private final String LIFECYCLE_OPERATION_ID = "test-id";

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private RollbackFailureOperation rollbackFailureOperation;

    @MockBean
    private RollbackPatternOperation rollbackPatternOperation;

    @MockBean
    private DeletePvcOperation deletePvcOperation;

    @MockBean
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Autowired
    private DeletePvcEvent deletePvcEvent;

    @Test
    public void testCompletedForRollbackFailure() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setFailurePattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.completed(createWorkflowServiceEventMessage());

        //then
        verify(rollbackFailureOperation, times(1)).completed(any());
    }

    @Test
    public void testCompletedForRollbackPattern() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setRollbackPattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.completed(createWorkflowServiceEventMessage());

        //then
        verify(rollbackPatternOperation, times(1)).completed(any());
    }

    @Test
    public void testCompletedForUpgradePattern() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setUpgradePattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.completed(createWorkflowServiceEventMessage());

        //then
        verify(changeVnfPackageOperation, times(1)).completed(any());
    }

    @Test
    public void testCompletedForTerminate() {
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(new LifecycleOperation());
        deletePvcEvent.completed(createWorkflowServiceEventMessage());

        verify(deletePvcOperation, times(1)).completed(any());
    }

    @Test
    public void testFailedForRollbackFailure() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setFailurePattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.failed(createWorkflowServiceEventMessage());

        //then
        verify(rollbackFailureOperation, times(1)).failed(any());
    }

    @Test
    public void testFailedForRollbackPattern() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setRollbackPattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.failed(createWorkflowServiceEventMessage());

        //then
        verify(rollbackPatternOperation, times(1)).failed(any());
    }

    @Test
    public void testFailedForUpgradePattern() {
        //given
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setUpgradePattern("some pattern");

        //when
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(lifecycleOperation);
        deletePvcEvent.failed(createWorkflowServiceEventMessage());

        //then
        verify(changeVnfPackageOperation, times(1)).failed(any());
    }

    @Test
    public void testFailedForTerminate() {
        when(databaseInteractionService.getLifecycleOperation(LIFECYCLE_OPERATION_ID)).thenReturn(new LifecycleOperation());
        deletePvcEvent.failed(createWorkflowServiceEventMessage());

        verify(deletePvcOperation, times(1)).failed(any());
    }

    private WorkflowServiceEventMessage createWorkflowServiceEventMessage() {
        final WorkflowServiceEventMessage workflowServiceEventMessage = new WorkflowServiceEventMessage();
        workflowServiceEventMessage.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        workflowServiceEventMessage.setStatus(WorkflowServiceEventStatus.COMPLETED);
        return workflowServiceEventMessage;
    }

}

