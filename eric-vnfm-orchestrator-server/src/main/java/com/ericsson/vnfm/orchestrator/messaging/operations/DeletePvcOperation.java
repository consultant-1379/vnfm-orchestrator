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
import com.ericsson.vnfm.orchestrator.messaging.handlers.FailOperationByTimeout;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RedirectFromTerminateToCcvp;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RedirectFromTerminateToRollback;
import com.ericsson.vnfm.orchestrator.messaging.handlers.ReleasesStillProcessing;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RouteToHeal;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateChartDeletePvcState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteIdentifier;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteNamespace;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteNodeHandler;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.TerminateNextChart;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeletePvcOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private Tracing tracing;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private HealOperation healOperation;

    @Autowired
    private RollbackOperation rollbackOperation;

    @Autowired
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Override
    public Conditions getConditions() {
        return new Conditions(MessagingLifecycleOperationType.DELETE_PVC.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistAndDeleteIdentifierSubflow = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartDeletePvcState(LifecycleOperationState.COMPLETED))
                .andThenOrElse(new RedirectFromTerminateToCcvp(changeVnfPackageOperation, databaseInteractionService),
                               persistAndDeleteIdentifierSubflow)
                .andThen(new RedirectFromTerminateToRollback(rollbackOperation, databaseInteractionService))
                .andThen(new TerminateNextChart(workflowRoutingService, databaseInteractionService))
                .andThen(new AnyFailedReleases())
                .andThenOrElse(new DeleteNamespace(utility, databaseInteractionService, lifeCycleManagementHelper),
                               persistAndDeleteIdentifierSubflow)
                .andThenOrElse(new RouteToHeal(healOperation), persistAndDeleteIdentifierSubflow)
                .andThenOrElse(new ChangeClusterConfigFileState<>(clusterConfigService, ConfigFileStatus.NOT_IN_USE),
                               persistAndDeleteIdentifierSubflow)
                .andThen(new DeleteVnfinstanceNamespaceDetails<>(databaseInteractionService))
                .endWithSubflow(persistAndDeleteIdentifierSubflow);
    }

    @Override
    public void failed(final HelmReleaseLifecycleMessage message) {
        if (message.getMessage() != null && isTerminateOperationCompleted(message.getMessage())) {
            LOGGER.info("Terminate operation has completed in the background. Error message from WFS : {} ",
                        message.getMessage());
            completed(message);
            return;
        }
        getFailConfiguration().getInitialStage()
                .ifPresent(handler -> handler.handle(initContext(message)));
    }

    private static boolean isTerminateOperationCompleted(String errorMessage) {
        return REGEX_RELEASE_NOT_FOUND.matcher(errorMessage).matches();
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {

        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistAndDeleteIdentifierSubflow = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartDeletePvcState(LifecycleOperationState.FAILED))
                .andThenOrElse(new RedirectFromTerminateToCcvp(changeVnfPackageOperation, databaseInteractionService),
                               persistAndDeleteIdentifierSubflow)
                .andThen(new RedirectFromTerminateToRollback(rollbackOperation, databaseInteractionService))
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
                .andThen(new DeleteIdentifier(utility)).end();
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureRollback() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new FailOperationByTimeout(utility, InstantiationState.NOT_INSTANTIATED))
                .end();
    }
}

