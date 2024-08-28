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
package com.ericsson.vnfm.orchestrator.messaging.handlers;

import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.updateHelmChartsDeletePvcState;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Update the state of the chart based on the result of it's individual lifecycle operation
 */
@Slf4j
@AllArgsConstructor
public final class UpdateChartDeletePvcState extends MessageHandler<HelmReleaseLifecycleMessage> {

    private LifecycleOperationState state;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        LOGGER.info("Handling update release pvc state for {} release", message.getReleaseName());
        VnfInstance vnfInstance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        updateHelmChartsDeletePvcState(vnfInstance, operation, message.getReleaseName(), state.toString());
        passToSuccessor(getSuccessor(), context);
    }
}
