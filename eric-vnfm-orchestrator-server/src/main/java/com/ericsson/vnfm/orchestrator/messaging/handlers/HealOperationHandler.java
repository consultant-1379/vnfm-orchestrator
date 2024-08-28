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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.mapHelmReleaseStateToLifeCycleOperationState;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnCompleted;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HealOperationHandler implements HealHandler {

    private HealOperation healOperation;

    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        HelmReleaseLifecycleMessage message = context.getMessage();

        routeToHealHandler(message, operation, instance);
    }

    public static boolean isHealForCNA(final LifecycleOperation operation) {
        return MessageUtility.getAdditionalParams(operation).containsKey(RESTORE_BACKUP_NAME);
    }


    private void routeToHealHandler(final HelmReleaseLifecycleMessage message,
                                    final LifecycleOperation operation,
                                    final VnfInstance instance) {
        if (isHealForCNA(operation) && HelmReleaseOperationType.INSTANTIATE.equals(message.getOperationType())) {
            LifecycleOperationState lifecycleOperationState = mapHelmReleaseStateToLifeCycleOperationState(message.getState());
            InstantiationState instantiationState = lifecycleOperationState.equals(LifecycleOperationState.FAILED)
                    ? NOT_INSTANTIATED :
                    INSTANTIATED;
            updateOperationAndInstanceOnCompleted(operation, instantiationState, instance,
                    lifecycleOperationState);
        } else {
            healOperation.completed(message);
        }
    }
}
