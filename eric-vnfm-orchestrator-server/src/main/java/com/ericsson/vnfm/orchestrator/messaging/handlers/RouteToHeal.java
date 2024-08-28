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
import static com.ericsson.vnfm.orchestrator.messaging.handlers.HealOperationHandler.isHealForCNA;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;

import java.util.EnumMap;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.operations.HealOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;

public final class RouteToHeal extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final Map<LifecycleOperationType, HealHandler> handlerMap = new EnumMap<>(LifecycleOperationType.class);

    public RouteToHeal(HealOperation healOperation,
                       HelmChartHistoryService helmChartHistoryService,
                       ReplicaCountCalculationService replicaCountCalculationService) {

        HealHandler healMessageHandler = new HealOperationHandler(healOperation);
        HealHandler terminateMessageHandler = new TerminateOperationHandler();
        HealHandler instantiateMessageHandler = new InstantiateOperationHandler(helmChartHistoryService, replicaCountCalculationService);

        handlerMap.put(HEAL, healMessageHandler);
        handlerMap.put(TERMINATE, terminateMessageHandler);
        handlerMap.put(INSTANTIATE, instantiateMessageHandler);
    }

    public RouteToHeal(final HealOperation healOperation) {

        HealHandler healMessageHandler = new HealOperationHandler(healOperation);
        HealHandler terminateMessageHandler = new TerminateOperationHandler();
        HealHandler instantiateMessageHandler = new InstantiateOperationHandler();

        handlerMap.put(HEAL, healMessageHandler);
        handlerMap.put(TERMINATE, terminateMessageHandler);
        handlerMap.put(INSTANTIATE, instantiateMessageHandler);
    }

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        HelmReleaseLifecycleMessage message = context.getMessage();

        if (FAILED.toString().equals(message.getState().toString())) {
            updateOperationAndInstanceOnCompleted(operation, NOT_INSTANTIATED, instance, FAILED);
            instance.setCleanUpResources(false);
        } else {
            handlerMap.get(operation.getLifecycleOperationType()).handle(context);
        }
        if (isNotHealForCNA(context, operation)) {
            return;
        }
        passToSuccessor(getSuccessor(), context);
    }

    private static boolean isNotHealForCNA(MessageHandlingContext<HelmReleaseLifecycleMessage> context,
                                           LifecycleOperation operation) {
        return HEAL.equals(operation.getLifecycleOperationType()) &&
                !(isHealForCNA(operation) && HelmReleaseOperationType.INSTANTIATE.equals(context.getMessage().getOperationType()));
    }
}
