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
package com.ericsson.vnfm.orchestrator.messaging.handlers.heal;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getAdditionalParams;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationAndInstanceOnCompleted;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.RESTORING_FROM_FILE_BACKUP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.LATEST;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RestoreFromBackup extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final MessageUtility utility;
    private final OssNodeService ossNodeService;

    @Override
    public void handle(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling restore from backup");
        LifecycleOperation operation = context.getOperation();
        VnfInstance vnfInstance = context.getVnfInstance();
        String backupFileRef = getAdditionalParam(operation, RESTORE_BACKUP_FILE_REFERENCE);
        String backupFileRefPassword = getAdditionalParam(operation, RESTORE_PASSWORD);
        if (StringUtils.isEmpty(backupFileRef)) {
            LOGGER.info("Skipping restore as backup file reference not provided");
            passToSuccessor(getSuccessor(), context);
        } else {
            executeRestore(context, operation, vnfInstance, backupFileRef, backupFileRefPassword);
        }
    }

    public static String getAdditionalParam(final LifecycleOperation operation, String key) {
        String backupFileRef = "";
        Map<String, Object> additionalParams = getAdditionalParams(operation);
        if (!CollectionUtils.isEmpty(additionalParams) && additionalParams.containsKey(key)) {
            backupFileRef = String.valueOf(additionalParams.get(key));
        }
        return backupFileRef;
    }

    private void executeRestore(final MessageHandlingContext<HelmReleaseLifecycleMessage> context,
                                final LifecycleOperation operation, final VnfInstance vnfInstance,
                                String backupFileRef, String backupFileRefPassword) {
        try {
            if (LATEST.equalsIgnoreCase(backupFileRef)) {
                ossNodeService.restoreBackupFromENM(vnfInstance, backupFileRef, backupFileRefPassword);
                updateOperationAndInstanceOnCompleted(operation, InstantiationState.INSTANTIATED, vnfInstance,
                                                      LifecycleOperationState.COMPLETED);
                passToSuccessor(getSuccessor(), context);
            } else {
                ossNodeService.restoreAsyncBackupFromENM(vnfInstance, operation, backupFileRef, backupFileRefPassword)
                        .thenAcceptAsync(isRestoreCompletedSuccessfully ->
                                                 passToSuccessor(isRestoreCompletedSuccessfully, operation, vnfInstance, context));
            }
        } catch (Exception exception) {
            LOGGER.error("An error occurred during restoring", exception);
            utility.saveErrorMessage("The following reason: " + exception.getMessage(),
                                     vnfInstance.getVnfInstanceName(), RESTORING_FROM_FILE_BACKUP, operation);
            passToSuccessor(getAlternativeSuccessor(), context);
        }
    }

    private void passToSuccessor(boolean isRestoreCompletedSuccessfully, final LifecycleOperation operation, final VnfInstance vnfInstance,
                                 final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        if (isRestoreCompletedSuccessfully) {
            updateOperationAndInstanceOnCompleted(operation, InstantiationState.INSTANTIATED, vnfInstance,
                                                  LifecycleOperationState.COMPLETED);
            passToSuccessor(getSuccessor(), context);
        } else {
            passToSuccessor(getAlternativeSuccessor(), context);
        }
    }
}
