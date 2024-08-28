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

import static com.ericsson.vnfm.orchestrator.messaging.handlers.heal.RestoreFromBackup.getAdditionalParam;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY_POST_STRING;

import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PatchKMSKey extends MessageHandler<HelmReleaseLifecycleMessage> {

    private static final String UNABLE_TO_DECRYPT_DATA_ERROR_MESSAGE = "unable to decrypt the data for instance name " +
            "%s due to %s";

    private final CryptoUtils cryptoUtils;

    private final MessageUtility utility;

    @Override
    public void handle(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling patching of kms unseal key data");
        VnfInstance vnfInstance = context.getVnfInstance();
        LifecycleOperation operation = context.getOperation();
        if (StringUtils.isEmpty(getAdditionalParam(operation, RESTORE_BACKUP_FILE_REFERENCE))) {
            LOGGER.info("Skipping patching of kms unseal key data as backup file reference not provided");
            passToSuccessor(getSuccessor(), context);
        } else {
            String decryptDetailsForKey = null;
            try {
                decryptDetailsForKey = cryptoUtils.getDecryptDetailsForKey(KMS_UNSEAL_KEY, vnfInstance);
                LOGGER.info("KMS key retrieved for vnf instance {}", vnfInstance.getVnfInstanceName());
            } catch (Exception ex) {
                LOGGER.error("An error occurred during patching", ex);
                appendError(String.format(UNABLE_TO_DECRYPT_DATA_ERROR_MESSAGE, vnfInstance.getVnfInstanceName(),
                        ex.getMessage()), operation);
                passToSuccessor(getAlternativeSuccessor(), context);
            }
            boolean patchSecret = utility
                    .triggerPatchSecret(KMS_UNSEAL_KEY_POST_STRING, KMS_UNSEAL_KEY, decryptDetailsForKey, operation,
                            vnfInstance);
            passToNextOperation(context, patchSecret);
        }
    }

    private void passToNextOperation(final MessageHandlingContext<HelmReleaseLifecycleMessage> context,
            final boolean patchSecret) {
        if (patchSecret) {
            passToSuccessor(getSuccessor(), context);
        } else {
            passToSuccessor(getAlternativeSuccessor(), context);
        }
    }
}
