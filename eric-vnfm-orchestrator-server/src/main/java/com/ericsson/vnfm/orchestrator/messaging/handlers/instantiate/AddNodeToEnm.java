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
package com.ericsson.vnfm.orchestrator.messaging.handlers.instantiate;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getAdditionalParams;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ADDING_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ENABLE_SUPERVISIONS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;

@Slf4j
@AllArgsConstructor
public class AddNodeToEnm extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final MessageUtility utility;
    private final OssNodeService ossNodeService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling adding Node to ENM");
        Map<String, Object> additionalParams = getAdditionalParams(context.getOperation());
        boolean addNodeToOss = BooleanUtils.getBooleanValue(additionalParams.get(ADD_NODE_TO_OSS), false);
        boolean cmpV2Enrollment = BooleanUtils.getBooleanValue(additionalParams.get(CMP_V2_ENROLLMENT), false);

        boolean executionSuccessful = true;

        if (addNodeToOss && !LifecycleOperationType.HEAL.equals(context.getOperation().getLifecycleOperationType())) {
            if (!cmpV2Enrollment && !context.getVnfInstance().isAddedToOss()) {
                executionSuccessful = addNodeToENM(context);
            }
            if (executionSuccessful && context.getVnfInstance().isAddedToOss()) {
                executionSuccessful = enableSupervisions(context);
            }
        } else {
            LOGGER.info("Skipping adding Node to ENM");
        }
        passToSuccessor(executionSuccessful ? getSuccessor() : getAlternativeSuccessor(), context);
    }

    private boolean addNodeToENM(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        try {
            ossNodeService.addNode(context.getVnfInstance());
            return true;
        } catch (Exception e) {
            LOGGER.error("An error occurred during adding Node to ENM", e);
            utility.saveErrorMessage("the following reason: " + e.getMessage(),
                    context.getVnfInstance().getVnfInstanceName(), ADDING_NODE_TO_OSS, context.getOperation());
            return false;
        }
    }

    private boolean enableSupervisions(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        try {
            ossNodeService.enableSupervisionsInENM(context.getVnfInstance());
            return true;
        } catch (Exception e) {
            LOGGER.error("An error occurred during enabling supervisions in ENM", e);
            utility.saveErrorMessage("the following reason: " + e.getMessage(),
                    context.getVnfInstance().getVnfInstanceName(), ENABLE_SUPERVISIONS, context.getOperation());
            ossNodeService.deleteNodeFromENM(context.getVnfInstance(), false);
            return false;
        }
    }

}