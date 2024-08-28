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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnFailure;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Have any releases failed their individual operation
 */
@Slf4j
public final class AnyFailedReleases extends MessageHandler<HelmReleaseLifecycleMessage> {

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling any failed releases");
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        if (isAnyChartFailed(instance)) {
            LOGGER.warn("There are failed Helm charts for VNF Instance");
            if (operation.isAutoRollbackAllowed() && operation.getLifecycleOperationType().equals(LifecycleOperationType.CHANGE_VNFPKG)) {
                passToSuccessor(getSuccessor(), context);
            } else {
                updateOperationAndInstance(operation, instance);
                passToSuccessor(getAlternativeSuccessor(), context);
            }
        } else {
            LOGGER.info("There are no failed Helm charts for VNF Instance");
            passToSuccessor(getSuccessor(), context);
        }
    }

    private static boolean isAnyChartFailed(VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream().anyMatch(chart -> StringUtils
                .equalsAny(LifecycleOperationState.FAILED.toString(), chart.getState(), chart.getDeletePvcState()));
    }

    private static void updateOperationAndInstance(LifecycleOperation operation, VnfInstance instance) {
        if (HEAL.equals(operation.getLifecycleOperationType())) {
            updateOperationAndInstanceOnFailure(operation, InstantiationState.INSTANTIATED, instance, LifecycleOperationState.FAILED);
        } else {
            updateOperationAndInstanceOnFailure(operation, InstantiationState.NOT_INSTANTIATED, instance, LifecycleOperationState.FAILED);
        }
    }
}
