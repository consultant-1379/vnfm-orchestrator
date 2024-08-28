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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.downsize.AnyDownsizeFailedReleases;
import com.ericsson.vnfm.orchestrator.messaging.handlers.downsize.DownsizeNextChart;
import com.ericsson.vnfm.orchestrator.messaging.handlers.downsize.SetChartDownsizeState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.downsize.TriggerUpgradePostDownsize;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeOperationContextBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Component
public class DownsizeOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private MessageUtility messageUtility;

    @Autowired
    @Qualifier("basicChangeOperationContextBuilder")
    private ChangeOperationContextBuilder changeOperationContextBuilder;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Override
    public Conditions getConditions() {
        return new Conditions(MessagingLifecycleOperationType.DOWNSIZE.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new SetChartDownsizeState(LifecycleOperationState.COMPLETED.toString()))
                .andThen(new DownsizeNextChart(workflowRoutingService))
                .andThenOrElse(new AnyDownsizeFailedReleases(), persistFlow)
                .andThenOrElse(new TriggerUpgradePostDownsize(changeVnfPackageRequestHandler, lifeCycleManagementHelper,
                                                              changeOperationContextBuilder, helmChartHelper), persistFlow)
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getAlternativeFlow();
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new SetChartDownsizeState(LifecycleOperationState.FAILED.toString()))
                .andThen(new AnyDownsizeFailedReleases())
                .andThenOrElse(new TriggerUpgradePostDownsize(changeVnfPackageRequestHandler, lifeCycleManagementHelper,
                                                              changeOperationContextBuilder, helmChartHelper), persistFlow)
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getAlternativeFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new Persist(databaseInteractionService))
                .end();
    }
}
