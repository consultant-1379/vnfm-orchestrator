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

import static com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType.INSTANTIATE;

import com.ericsson.vnfm.orchestrator.messaging.handlers.RedirectFromInstantiateToCcvp;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate.DeleteNodeFromENM;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate.AddNodeToEnm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.handlers.ChangeClusterConfigFileState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RedirectFromInstantiateToRollback;
import com.ericsson.vnfm.orchestrator.messaging.handlers.ReleasesStillProcessing;
import com.ericsson.vnfm.orchestrator.messaging.handlers.RouteToHeal;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateChartState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate.HandleKmsKey;
import com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate.InstantiateNextChart;
import com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate.TriggerTeardown;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteIdentifier;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InstantiateOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private EnmTopologyService enmTopologyService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MessageUtility utility;

    @Autowired
    @Lazy
    private HealOperation healOperation;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private RollbackOperation rollbackOperation;

    @Autowired
    @Lazy
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Override
    public Conditions getConditions() {
        return new Conditions(INSTANTIATE.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> triggerTeardown = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartState(LifecycleOperationState.COMPLETED))
                .andThen(new InstantiateNextChart(workflowRoutingService, utility, databaseInteractionService))
                .andThenOrElse(new RedirectFromInstantiateToRollback(rollbackOperation, databaseInteractionService), triggerTeardown)
                .andThenOrElse(new RedirectFromInstantiateToCcvp(changeVnfPackageOperation, databaseInteractionService), triggerTeardown)
                .andThenOrElse(new HandleKmsKey(mapper, workflowRoutingService, cryptoUtils), triggerTeardown)
                .andThenOrElse(new AddNodeToEnm(utility, ossNodeService), triggerTeardown)
                .andThenOrElse(new RouteToHeal(healOperation, helmChartHistoryService, replicaCountCalculationService), triggerTeardown)
                .andThenOrElse(new ChangeClusterConfigFileState<>(clusterConfigService, ConfigFileStatus.IN_USE), triggerTeardown)
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility)).end();
    }

    MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getAlternativeFlow() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> triggerTeardownNotRequired = getTriggerTeardownAlternativeFlow();
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getPersistFlow();
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> deleteNodeFlow = getDeleteNodeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new TriggerTeardown(workflowRoutingService, lifeCycleManagementHelper))
                .andThenOrElse(new ReleasesStillProcessing(), triggerTeardownNotRequired)
                .andThenOrElse(new DeleteNodeFromENM(utility, ossNodeService), deleteNodeFlow)
                .andThenOrElse(new Persist(databaseInteractionService), persistFlow)
                .andThen(new DeleteIdentifier(utility))
                .end();
    }

    MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getTriggerTeardownAlternativeFlow() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> deleteNodeFlow = getDeleteNodeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new ReleasesStillProcessing())
                .andThenOrElse(new DeleteNodeFromENM(utility, ossNodeService), deleteNodeFlow)
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility))
                .end();
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {

        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistAndDeleteIdentifierSubflow = getAlternativePersistFlow();
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> deleteNodeFlow = getDeleteNodeFlow();
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getPersistFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateChartState(LifecycleOperationState.FAILED))
                .andThen(new RedirectFromInstantiateToRollback(rollbackOperation, databaseInteractionService))
                .andThen(new RedirectFromInstantiateToCcvp(changeVnfPackageOperation, databaseInteractionService))
                .andThen(new TriggerTeardown(workflowRoutingService, lifeCycleManagementHelper))
                .andThenOrElse(new ReleasesStillProcessing(), persistAndDeleteIdentifierSubflow)
                .andThenOrElse(new DeleteNodeFromENM(utility, ossNodeService), deleteNodeFlow)
                .andThenOrElse(new RouteToHeal(healOperation, helmChartHistoryService, replicaCountCalculationService), persistFlow)
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility)).end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getAlternativePersistFlow() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> deleteNodeFlow = getDeleteNodeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new ReleasesStillProcessing())
                .andThenOrElse(new DeleteNodeFromENM(utility, ossNodeService), deleteNodeFlow)
                .andThen(new RouteToHeal(healOperation, helmChartHistoryService, replicaCountCalculationService))
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility)).end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getDeleteNodeFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new DeleteNodeFromENM(utility, ossNodeService))
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility)).end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getPersistFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility)).end();
    }

    @Override
    public void rollBack(HelmReleaseLifecycleMessage message) {
        utility.lifecycleTimedOut(message.getLifecycleOperationId(), InstantiationState.NOT_INSTANTIATED, message.getMessage());
    }
}

