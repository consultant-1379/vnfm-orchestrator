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
package com.ericsson.vnfm.orchestrator.messaging.handlers.downsize;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnFailure;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Objects;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Have any releases failed their individual operation
 */
@Slf4j
public final class AnyDownsizeFailedReleases extends MessageHandler<HelmReleaseLifecycleMessage> {

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling any downsize failed releases");

        final LifecycleOperation operation = context.getOperation();
        final VnfInstance instance = context.getVnfInstance();

        if (isAnyChartDownsizeFailed(instance, operation)) {
            LOGGER.warn("There are downsize failed Helm charts for VNF Instance");

            if (operation.isAutoRollbackAllowed()) {
                passToSuccessor(getSuccessor(), context);
            } else {
                updateOperationAndInstanceOnFailure(operation, InstantiationState.NOT_INSTANTIATED, instance, LifecycleOperationState.FAILED);

                passToSuccessor(getAlternativeSuccessor(), context);
            }
        } else {
            LOGGER.info("There are no downsize failed Helm charts for VNF Instance");

            passToSuccessor(getSuccessor(), context);
        }
    }

    private static boolean isAnyChartDownsizeFailed(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        final boolean downsizeDuringRollbackAfterCCVPFailure = isDownsizeDuringRollbackAfterCCVPFailure(operation);

        final VnfInstance targetInstance = downsizeDuringRollbackAfterCCVPFailure ?
                parseJson(vnfInstance.getTempInstance(), VnfInstance.class) :
                vnfInstance;

        return targetInstance.getHelmCharts().stream()
                .anyMatch(chart -> Objects.equals(chart.getDownsizeState(), LifecycleOperationState.FAILED.toString()));
    }

    private static boolean isDownsizeDuringRollbackAfterCCVPFailure(final LifecycleOperation operation) {
        return Objects.equals(operation.getOperationState(), LifecycleOperationState.ROLLING_BACK);
    }
}
