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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnCompleted;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class SetAlarmSupervision extends MessageHandler<HelmReleaseLifecycleMessage> {

    private LifecycleRequestHandler request;
    private EnmOperationEnum enmOperationEnum;

    @Override
    public void handle(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling setting Alarm Supervision");
        VnfInstance instance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        if (!LifecycleOperationType.MODIFY_INFO.equals(operation.getLifecycleOperationType())) {
            request.setAlarmSupervisionWithWarning(instance, enmOperationEnum);
        }
        if (!operation.getOperationState().equals(LifecycleOperationState.ROLLING_BACK)) {
            updateOperationAndInstanceOnCompleted(operation, InstantiationState.INSTANTIATED, instance,
                    LifecycleOperationState.COMPLETED);
        }
        passToSuccessor(getSuccessor(), context);
    }
}
