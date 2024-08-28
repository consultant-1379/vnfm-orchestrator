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

import static com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENABLE_ALARM_SUPERVISION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.handlers.FailOperationByTimeout;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.SetAlarmSupervision;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateTempChartForRollback;
import com.ericsson.vnfm.orchestrator.messaging.handlers.downsize.TriggerRollbackDownsize;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.DetermineFailurePath;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.DetermineUpgradeOperationType;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.DetermineUpgradePatternPresence;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.NextChartPresent;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.ReleaseTargetPackage;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.TriggerNextUpgradePatternStage;
import com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade.UpdateInstance;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmUpgrade;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.HelmChartMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Component
public class ChangeVnfPackageOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Autowired
    private ChangeVnfPackageRequestHandler request;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private HelmChartMapper helmChartMapper;

    @Autowired
    private VnfdHelper vnfdHelper;

    @Autowired
    private RollbackService rollbackService;

    @Autowired
    private EvnfmUpgrade evnfmUpgrade;

    @Override
    public Conditions getConditions() {
        return new Conditions(CHANGE_VNFPKG.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getPersistFlow();
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new DetermineUpgradePatternPresence())
                .andThenOrElse(new NextChartPresent(
                        changePackageOperationDetailsRepository, workflowRoutingService, utility, databaseInteractionService, rollbackService),
                        getUpgradeByPatternFlow())
                .andThenOrElse(new DetermineUpgradeOperationType(utility, helmChartHistoryService, instanceService,
                        changePackageOperationDetailsRepository, helmChartMapper,
                        packageService, vnfdHelper), persistFlow)
                .andThenOrElse(new SetAlarmSupervision(request, ENABLE_ALARM_SUPERVISION), persistFlow)
                .andThen(new UpdateInstance(instanceService, utility, helmChartHistoryService))
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> autoRollBackOperation = getAutoRollbackFlow();
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new DetermineFailurePath(utility,
                                                    instanceService,
                                                    changePackageOperationDetailsRepository,
                                                    packageService))
                .andThenOrElse(new UpdateTempChartForRollback(utility), autoRollBackOperation)
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureRollback() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new ReleaseTargetPackage(instanceService, false, changePackageOperationDetailsRepository,
                                                    packageService))
                .andThen(new FailOperationByTimeout(utility, InstantiationState.INSTANTIATED))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getPersistFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new Persist(databaseInteractionService))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getAutoRollbackFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new TriggerRollbackDownsize(workflowRoutingService, request))
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getUpgradeByPatternFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new TriggerNextUpgradePatternStage(evnfmUpgrade))
                .andThen(new Persist(databaseInteractionService))
                .end();
    }
}
