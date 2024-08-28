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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_TYPE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.VNF_INSTANCE_KEY;

import jakarta.annotation.PostConstruct;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public abstract class AbstractLifeCycleOperationProcessor implements LifeCycleOperationProcessor<HelmReleaseLifecycleMessage> {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> completeConfiguration;

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> failConfiguration;

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> rollbackConfiguration;

    @PostConstruct
    private void configureFlows() {
        completeConfiguration = configureCompleted();
        failConfiguration = configureFailed();
        rollbackConfiguration = configureRollback();
    }

    @Override
    @Transactional
    public void completed(final HelmReleaseLifecycleMessage message) {
        completeConfiguration.getInitialStage()
                .ifPresent(handler -> handler.handle(initContext(message)));
        MDC.clear();
    }

    @Override
    @Transactional
    public void failed(final HelmReleaseLifecycleMessage message) {
        failConfiguration.getInitialStage()
                .ifPresent(handler -> handler.handle(initContext(message)));
        MDC.clear();
    }

    @Override
    @Transactional
    public void rollBack(final HelmReleaseLifecycleMessage message) {
        rollbackConfiguration.getInitialStage()
                .ifPresent(handler -> handler.handle(initContext(message)));
        MDC.clear();
    }

    /*
     * Template methods for providing actual message handling chain configurations.
     * Default configuration provides no message handling.
     */

    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        return new MessageHandlingConfiguration<>();
    }

    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        return new MessageHandlingConfiguration<>();
    }

    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureRollback() {
        return new MessageHandlingConfiguration<>();
    }

    @Override
    public MessageHandlingContext<HelmReleaseLifecycleMessage> initContext(HelmReleaseLifecycleMessage message) {
        LOGGER.info("Initializing message handling context");
        LifecycleOperation operation = databaseInteractionService.getLifecycleOperation(message.getLifecycleOperationId());
        MessageHandlingContext<HelmReleaseLifecycleMessage> context = new MessageHandlingContext<>(message);
        context.setOperation(operation);
        context.setVnfInstance(operation.getVnfInstance());

        MDC.put(VNF_INSTANCE_KEY, operation.getVnfInstance().getVnfInstanceId());
        MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, operation.getLifecycleOperationType().toString());
        MDC.put(LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY, operation.getOperationOccurrenceId());

        return context;
    }
}
