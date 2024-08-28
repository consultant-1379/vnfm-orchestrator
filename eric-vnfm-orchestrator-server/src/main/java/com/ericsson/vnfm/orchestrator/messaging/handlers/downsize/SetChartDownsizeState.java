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

import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getCnfChartWithName;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Objects;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class SetChartDownsizeState extends MessageHandler<HelmReleaseLifecycleMessage> {

    private String downsizeState;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        final HelmReleaseLifecycleMessage message = context.getMessage();

        LOGGER.info("Handling setting charts downsize state to {} with {} release name and {} message",
                    downsizeState,
                    message.getReleaseName(),
                    message.getMessage());

        final boolean downsizeDuringRollbackAfterCCVPFailure = isDownsizeDuringRollbackAfterCCVPFailure(context.getOperation());

        final VnfInstance vnfInstance = context.getVnfInstance();
        final VnfInstance targetInstance = downsizeDuringRollbackAfterCCVPFailure ?
                parseJson(vnfInstance.getTempInstance(), VnfInstance.class) :
                vnfInstance;

        getCnfChartWithName(targetInstance, message.getReleaseName())
                .ifPresent(helmChart -> helmChart.setDownsizeState(downsizeState));

        if (downsizeDuringRollbackAfterCCVPFailure) {
            vnfInstance.setTempInstance(convertObjToJsonString(targetInstance));
        }

        passToSuccessor(getSuccessor(), context);
    }

    private static boolean isDownsizeDuringRollbackAfterCCVPFailure(final LifecycleOperation operation) {
        return Objects.equals(operation.getOperationState(), LifecycleOperationState.ROLLING_BACK);
    }
}
