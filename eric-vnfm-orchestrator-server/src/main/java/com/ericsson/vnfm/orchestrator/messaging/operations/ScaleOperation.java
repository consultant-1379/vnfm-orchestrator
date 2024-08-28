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
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType;
import com.ericsson.vnfm.orchestrator.messaging.handlers.FailOperationByTimeout;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateTempChartForRollback;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateTempInstanceChartState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.scale.NextChartAndRetryCheck;
import com.ericsson.vnfm.orchestrator.messaging.handlers.scale.NextChartPresent;
import com.ericsson.vnfm.orchestrator.messaging.handlers.scale.UpdateScaleInfo;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Component
public class ScaleOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @Autowired
    private MessageUtility utility;

    @Override
    public Conditions getConditions() {
        return new Conditions(MessagingLifecycleOperationType.SCALE.toString(), HelmReleaseLifecycleMessage.class);
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getPersistFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new UpdateTempInstanceChartState(LifecycleOperationState.COMPLETED))
                .andThen(new NextChartPresent(workflowRoutingService, utility))
                .andThenOrElse(new UpdateScaleInfo(helmChartHistoryService), persistFlow)
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> persistFlow = getPersistFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new NextChartAndRetryCheck(workflowRoutingService,
                                                      utility, scaleService))
                .andThenOrElse(new UpdateTempChartForRollback(utility), persistFlow)
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    protected MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureRollback() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new FailOperationByTimeout(utility, InstantiationState.INSTANTIATED))
                .end();
    }

    private MessageHandlingConfiguration<HelmReleaseLifecycleMessage> getPersistFlow() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new Persist(databaseInteractionService))
                .end();
    }
}
