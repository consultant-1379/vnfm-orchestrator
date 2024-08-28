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

import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.FAILED;
import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.messaging.routing.RoutingUtility.getConditions;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.INSTANTIATE;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState.COMPLETED;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


@TestPropertySource(properties = "redis.listener.enabled=true")
public class RedisStreamListenerTest extends AbstractEndToEndTest {

    @Test
    public void operationCompleteWhichFails() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.FAILED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "71aa2228-96b5-4ce6-9996-8aa994dd841a";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        testingMessageSender.sendMessage(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isEqualTo(operation.getStateEnteredTime());
    }

    @Test
    public void operationCompleteWhichFailsWithIdempotencyKey() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.FAILED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "71aa2228-96b5-4ce6-9996-8aa994dd841a";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        testingMessageSender.sendMessageWithIdempotencyKey(completed, UUID.randomUUID().toString());
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isEqualTo(operation.getStateEnteredTime());
    }

    @Test
    public void operationFailedWhichCompletes() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(COMPLETED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "ecf7d304-972b-4324-9342-3a7095f7d194";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        testingMessageSender.sendMessage(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(prior).isEqualTo(operation.getStateEnteredTime());
    }

    @Test
    public void operationFailedWhichCompletesSentTwiceWithDifferentStates() {
        String idempotencyKey = UUID.randomUUID().toString();
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(COMPLETED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "ecf7d304-972b-4324-9342-3a7095f7d194";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        testingMessageSender.sendMessageWithIdempotencyKey(completed, idempotencyKey);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(prior).isEqualTo(operation.getStateEnteredTime());

        final HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        completed.setState(FAILED);
        completed.setOperationType(INSTANTIATE);
        completed.setLifecycleOperationId(lifecycleOperationId);

        testingMessageSender.sendMessageWithIdempotencyKey(failed, idempotencyKey);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(prior).isEqualTo(operation.getStateEnteredTime());
    }

    @Test
    public void shouldGetTheCorrectOperationTypeForRollback() {
        HelmReleaseLifecycleMessage helmReleaseLifecycleMessage = new HelmReleaseLifecycleMessage();
        helmReleaseLifecycleMessage.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        Conditions conditions = getConditions(helmReleaseLifecycleMessage, lifecycleOperation);
        assertThat(conditions.getOperationTypeName()).isEqualTo("INSTANTIATE");

        lifecycleOperation.setRollbackPattern("rollback pattern");
        conditions = getConditions(helmReleaseLifecycleMessage, lifecycleOperation);
        assertThat(conditions.getOperationTypeName()).isEqualTo("ROLLBACK_PATTERN");

        lifecycleOperation.setRollbackPattern(null);
        lifecycleOperation.setFailurePattern("rollback failure pattern");
        conditions = getConditions(helmReleaseLifecycleMessage, lifecycleOperation);
        assertThat(conditions.getOperationTypeName()).isEqualTo("ROLLBACK_FAILURE_PATTERN");
    }
}
