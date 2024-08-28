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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isAutorollbackAndDownsizeAllowedOperation;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLING_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.MODIFY_INFO;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DetermineFailurePath extends MessageHandler<HelmReleaseLifecycleMessage> {

    private static final String OPERATION_FAILED_MESSAGE = "Operation with id %s and release name %s failed due to %s";

    private final MessageUtility utility;
    private final InstanceService instanceService;
    private final ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;
    private final PackageService packageService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        VnfInstance instance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        String operationOccurrenceId = operation.getOperationOccurrenceId();
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository
                .findById(operationOccurrenceId);
        LOGGER.debug("Auto rollback set to : {}", operation.isAutoRollbackAllowed());

        if (isAutorollbackAndDownsizeAllowedOperation(operation)) {
            // trigger rollback with downsize before it
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            final boolean isDowngrade = isDowngradeOperation(changePackageOperationDetails);
            final String failedReleaseName = context.getMessage().getReleaseName();
            if ((isDowngrade || operation.isDownsizeAllowed() || !operation.isAutoRollbackAllowed())
                    && MODIFY_INFO != operation.getLifecycleOperationType()) {
                skipAutoRollback(isDowngrade, instance, message, operation, failedReleaseName);
            } else {
                passToSuccessor(getSuccessor(), context);
            }
        }
    }

    private void skipAutoRollback(final boolean isDowngrade, VnfInstance instance, final HelmReleaseLifecycleMessage message,
                                  final LifecycleOperation operation, final String failedReleaseName) {
        LOGGER.info("Skipping auto rollback");
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        String errorLogMessage = String.format(OPERATION_FAILED_MESSAGE, operation.getOperationOccurrenceId(),
                                               message.getReleaseName(), message.getMessage());

        String targetPackageId = isDowngrade ?
                packageService.getPackageInfo(operation.getTargetVnfdId()).getId() :
                tempInstance.getVnfPackageId();

        if (isNotFailedAutoRollback(operation)) {
            // Autorollback was not triggered
            utility.updateFailedOperationAndChartByReleaseName(operation.getOperationOccurrenceId(),
                                                               message.getMessage(),
                                                               errorLogMessage,
                                                               LifecycleOperationState.FAILED_TEMP,
                                                               LifecycleOperationState.FAILED,
                                                               failedReleaseName);
        } else {
            try {
                instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                                 targetPackageId,
                                                                                                 targetPackageId,
                                                                                                 tempInstance.getVnfInstanceId(), false);
            } catch (Exception e) {
                LOGGER.warn("Update usage state failed due to: {}. Flow will continue to update chart and operation state.", e.getMessage(), e);
            }
            utility.updateOperationOnFail(operation.getOperationOccurrenceId(), message.getMessage(), errorLogMessage,
                                          InstantiationState.INSTANTIATED,  LifecycleOperationState.FAILED);
        }
    }

    // AutorollbackAllowed updated from true to false in case Rollback with Downsize.
    // For details, check TriggerRollbackDownsize.java
    private static boolean isNotFailedAutoRollback(LifecycleOperation operation) {
        return !operation.isAutoRollbackAllowed()
                && ROLLING_BACK != operation.getOperationState();
    }

}
