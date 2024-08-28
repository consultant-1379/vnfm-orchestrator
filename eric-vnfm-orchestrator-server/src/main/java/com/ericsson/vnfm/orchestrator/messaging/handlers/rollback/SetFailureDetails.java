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
package com.ericsson.vnfm.orchestrator.messaging.handlers.rollback;

import java.util.LinkedHashMap;
import java.util.List;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.RollbackPatternUtility.getPattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetFailureDetails extends MessageHandler<HelmReleaseLifecycleMessage>  {

    @Override
    public void handle(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Setting failure details");
        LifecycleOperation operation = context.getOperation();
        HelmReleaseLifecycleMessage message = context.getMessage();
        List<LinkedHashMap<String, String>> patternList = getPattern(operation);
        int nextStage = OperationsUtils.getCurrentStage(patternList, message);
        String errorMessage =
                message.getMessage() != null ? message.getMessage() : "Failure event did not contain an error message.";
        String formattedErrorMessage = String
                .format("Stage %s executing %s for %s failed with %s.", nextStage, message.getOperationType(),
                        message.getReleaseName(), errorMessage);
        appendError(formattedErrorMessage, operation);
        updateOperationState(operation, LifecycleOperationState.FAILED);
        passToSuccessor(getSuccessor(), context);
    }
}
