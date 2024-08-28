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

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpdateUsageState extends MessageHandler<HelmReleaseLifecycleMessage> {

    private static final String UPDATE_USAGE_STATE_FAILED_MESSAGE = "Update usage state api failed. Flow will continue to update operation state.";

    protected final InstanceService instanceService;
    private final boolean inUse;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling update usage state");
        try {
            VnfInstance instance = context.getVnfInstance();
            String targetPackageId = getTargetPackageId(context);

            instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                             targetPackageId,
                                                                                             targetPackageId,
                                                                                             instance.getVnfInstanceId(),
                                                                                             inUse);
        } catch (Exception e) {
            LOGGER.warn(UPDATE_USAGE_STATE_FAILED_MESSAGE, e);
        }

        passToSuccessor(getSuccessor(), context);
    }

    protected String getTargetPackageId(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        VnfInstance instance = context.getVnfInstance();
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        return tempInstance.getVnfPackageId();
    }
}
