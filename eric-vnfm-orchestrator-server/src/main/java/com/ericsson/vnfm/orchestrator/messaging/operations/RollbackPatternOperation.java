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

import static com.ericsson.vnfm.orchestrator.messaging.routing.RoutingUtility.ROLLBACK_PATTERN;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateUsageState;
import com.ericsson.vnfm.orchestrator.messaging.handlers.rollback.SetFailureDetails;
import com.ericsson.vnfm.orchestrator.messaging.handlers.rollback.TriggerNextRollbackPatternStage;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmDowngrade;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Component
public class RollbackPatternOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private EvnfmDowngrade downgrade;

    @Override
    public Conditions getConditions() {
        return new Conditions(ROLLBACK_PATTERN, HelmReleaseLifecycleMessage.class);
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new TriggerNextRollbackPatternStage(downgrade))
                .andThen(new Persist(databaseInteractionService))
                .end();
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureFailed() {
        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new SetFailureDetails())
                .andThen(new UpdateUsageState(instanceService, false))
                .andThen(new Persist(databaseInteractionService))
                .end();
    }
}
