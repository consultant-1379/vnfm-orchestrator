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

import static com.ericsson.vnfm.orchestrator.messaging.operations.TerminateOperation.REGEX_NAMESPACE_NOT_FOUND;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.REGEX_RELEASE_NOT_FOUND;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType;
import com.ericsson.vnfm.orchestrator.messaging.handlers.AnyFailedReleases;
import com.ericsson.vnfm.orchestrator.messaging.handlers.ChangeClusterConfigFileState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.DeleteVnfinstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.ReleasesStillProcessing;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RouteToHeal;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateChartDeletePvcState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateChartState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteIdentifier;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteNodeHandler;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeleteNamespaceOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    private Tracing tracing;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private HealOperation healOperation;

    @Override
    public Conditions getConditions() {
        return new Conditions(MessagingLifecycleOperationType.DELETE_NAMESPACE.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistAndDeleteIdentifierSubflow = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartDeletePvcState(LifecycleOperationState.COMPLETED))
                .andThenOrElse(new DeleteVnfinstanceNamespaceDetails<>(databaseInteractionService), persistAndDeleteIdentifierSubflow)
                .andThenOrElse(new RouteToHeal(healOperation), persistAndDeleteIdentifierSubflow)
                .andThen(new ChangeClusterConfigFileState<>(clusterConfigService, ConfigFileStatus.NOT_IN_USE))
                .endWithSubflow(persistAndDeleteIdentifierSubflow);
    }

    @Override
    public void failed(final HelmReleaseLifecycleMessage message) {
        if (message.getMessage() != null && isTerminateOperationCompleted(message.getMessage())) {
            LOGGER.info("Terminate operation has completed in the background. Error message from WFS : {} ",
                        message.getMessage());
            message.setState(HelmReleaseState.COMPLETED);
            completed(message);
            return;
        }
        getFailConfiguration().getInitialStage()
                .ifPresent(handler -> handler.handle(initContext(message)));
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistAndDeleteIdentifierSubflow = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartState(LifecycleOperationState.FAILED))
                .andThen(new ReleasesStillProcessing())
                .andThen(new AnyFailedReleases())
                .andThenOrElse(new DeleteNodeHandler(applicationContext, scheduledThreadPoolExecutor, tracing), persistAndDeleteIdentifierSubflow)
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getAlternativeFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new DeleteNodeHandler(applicationContext, scheduledThreadPoolExecutor, tracing))
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility))
                .end();
    }

    private static boolean isTerminateOperationCompleted(String errorMessage) {
        return REGEX_RELEASE_NOT_FOUND.matcher(errorMessage).matches() ||
                REGEX_NAMESPACE_NOT_FOUND.matcher(errorMessage).matches();
    }
}
