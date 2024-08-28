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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

public interface UpgradeOperation {

    void execute(LifecycleOperation operation);

    void triggerNextStage(MessageHandlingContext<HelmReleaseLifecycleMessage> context);

    Command getService(String service);
}
