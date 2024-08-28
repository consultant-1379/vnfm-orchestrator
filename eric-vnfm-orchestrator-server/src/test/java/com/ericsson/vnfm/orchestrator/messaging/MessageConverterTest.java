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
package com.ericsson.vnfm.orchestrator.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.messaging.MessageConverter.convertEventToHelmMessage;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.DELETE_PVC;

import org.junit.jupiter.api.Test;

import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;

public class MessageConverterTest {

    private final String LIFECYCLE_OPERATION_ID = "test-id";
    private final String TEST_RELEASE_NAME = "some name";
    private final String TEST_MESSAGE = "some name";


    @Test
    public void convertEventToHelmMessageTestWithCompletedState() {
        //given
        WorkflowServiceEventMessage workflowServiceEventMessage = createWorkflowServiceEventMessage();
        workflowServiceEventMessage.setStatus(WorkflowServiceEventStatus.COMPLETED);

        //when
        HelmReleaseLifecycleMessage helmReleaseLifecycleMessage =
              convertEventToHelmMessage(workflowServiceEventMessage, DELETE_PVC);

        //then
        commonAssertion(helmReleaseLifecycleMessage);
        assertThat(helmReleaseLifecycleMessage.getState()).isNotNull().isEqualTo(HelmReleaseState.COMPLETED);
    }

    @Test
    public void convertEventToHelmMessageTestWithFailedState() {
        //given
        WorkflowServiceEventMessage workflowServiceEventMessage = createWorkflowServiceEventMessage();
        workflowServiceEventMessage.setStatus(WorkflowServiceEventStatus.FAILED);

        //when
        HelmReleaseLifecycleMessage helmReleaseLifecycleMessage =
              convertEventToHelmMessage(workflowServiceEventMessage, DELETE_PVC);

        //then
        commonAssertion(helmReleaseLifecycleMessage);
        assertThat(helmReleaseLifecycleMessage.getState()).isNotNull().isEqualTo(HelmReleaseState.FAILED);
    }

    public void commonAssertion(HelmReleaseLifecycleMessage helmReleaseLifecycleMessage) {
        assertThat(helmReleaseLifecycleMessage.getReleaseName()).isNotNull().isEqualTo(TEST_RELEASE_NAME);
        assertThat(helmReleaseLifecycleMessage.getMessage()).isNotNull().isEqualTo(TEST_MESSAGE);
        assertThat(helmReleaseLifecycleMessage.getLifecycleOperationId()).isNotNull().isEqualTo(LIFECYCLE_OPERATION_ID);
        assertThat(helmReleaseLifecycleMessage.getOperationType()).isNotNull().isEqualTo(DELETE_PVC);
        assertThat(helmReleaseLifecycleMessage.getRevisionNumber()).isNull();
    }

    public WorkflowServiceEventMessage createWorkflowServiceEventMessage () {
        WorkflowServiceEventMessage workflowServiceEventMessage = new WorkflowServiceEventMessage();
        workflowServiceEventMessage.setReleaseName(TEST_RELEASE_NAME);
        workflowServiceEventMessage.setMessage(TEST_MESSAGE);
        workflowServiceEventMessage.setLifecycleOperationId(LIFECYCLE_OPERATION_ID);
        return workflowServiceEventMessage;
    }
}