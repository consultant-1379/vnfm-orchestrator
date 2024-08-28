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
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DELETING_NODE_FROM_OSS;

@Slf4j
@AllArgsConstructor
public class DeleteNodeFromENM extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final MessageUtility utility;
    private final OssNodeService ossNodeService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling delete Node from ENM");
        VnfInstance vnfInstance = context.getVnfInstance();
        if (vnfInstance.isAddedToOss()) {
            try {
                ossNodeService.deleteNodeFromENM(vnfInstance, false);
                vnfInstance.setAddedToOss(false);
            } catch (Exception e) {
                LOGGER.error("An error occurred during deleting Node from ENM", e);
                utility.saveErrorMessage("the following reason: " + e.getMessage(),
                        context.getVnfInstance().getVnfInstanceName(), DELETING_NODE_FROM_OSS, context.getOperation());
            }
        } else {
            LOGGER.info("VnfInstance [{}] is not added to OSS", vnfInstance.getVnfInstanceName());
        }
        passToSuccessor(getSuccessor(), context);
    }
}
