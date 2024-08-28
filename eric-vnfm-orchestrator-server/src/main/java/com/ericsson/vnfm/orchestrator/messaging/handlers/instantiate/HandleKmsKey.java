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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.KMS_UNSEAL_KEY_POST_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.RETRIEVE_UNSEAL_KEY;
import static com.ericsson.vnfm.orchestrator.utils.SupportedOperationUtils.isOperationSupported;

import java.util.Map;
import java.util.Optional;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretAttribute;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public final class HandleKmsKey extends MessageHandler<HelmReleaseLifecycleMessage> {

    public static final String UNSEAL_KEY_NOT_CREATED_ERROR_MESSAGE = "KMS unseal key not created for instance name %s";
    public static final String UNSEAL_KEY_DATA_NULL = "KMS unseal key data is null for instance name %s";
    public static final String UNABLE_TO_FETCH_UNSEAL_KEY_ERROR_MESSAGE = "Unable to fetch KMS unseal key data for " +
            "instance name %s, due to %s";
    public static final String UNABLE_TO_ENCRYPT_DATA_ERROR_MESSAGE = "unable to encrypt the data for instance name " +
            "%s due to %s";

    private final ObjectMapper mapper;
    private final WorkflowRoutingService workflowRoutingService;
    private final CryptoUtils cryptoUtils;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling persisting of kms unseal key data");
        LifecycleOperation operation = context.getOperation();
        Map<String, Object> additionalParameters = MessageUtility.getAdditionalParams(operation);

        Optional<String> additionalParamsRetrieveUnsealKey =
                additionalParameters.keySet().stream().filter(key -> key.equalsIgnoreCase(RETRIEVE_UNSEAL_KEY)).findAny();
        if (additionalParamsRetrieveUnsealKey.isPresent() &&
                "false".equalsIgnoreCase(additionalParameters.get(RETRIEVE_UNSEAL_KEY).toString())) {
            LOGGER.info("Skipping persisting of kms unseal key data");
            passToSuccessor(getSuccessor(), context);
        } else {
            if (isOperationSupported(context.getVnfInstance(), LCMOperationsEnum.HEAL.getOperation()) && operation.getLifecycleOperationType()
                    .equals(LifecycleOperationType.INSTANTIATE)) {
                fetchAndSetKMSUnsealKey(context);
            } else {
                LOGGER.info("Skipping persisting of kms unseal key data as Heal is not supported");
                passToSuccessor(getSuccessor(), context);
            }
        }
    }

    private void fetchAndSetKMSUnsealKey(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        String clusterName = instance.getClusterName();
        String namespace = instance.getNamespace();
        LOGGER.info("Heal interface is supported. This is a vDU node. Hence, getting the kms unseal key in " +
                            "cluster {} and namespace {}", clusterName, namespace);
        WorkflowSecretResponse workflowSecretResponse = null;
        try {
            workflowSecretResponse = workflowRoutingService
                    .routeToEvnfmWfsForGettingAllSecrets(clusterName, namespace);
        } catch (Exception ex) {
            LOGGER.error("An error occurred during fetching KMS key", ex);
            appendError(String.format(UNABLE_TO_FETCH_UNSEAL_KEY_ERROR_MESSAGE, instance.getVnfInstanceName(),
                                          ex.getMessage()), operation);
            passToSuccessor(getAlternativeSuccessor(), context);
        }
        if (workflowSecretResponse != null) {
            processAndSetKMSKey(context, workflowSecretResponse);
        } else {
            LOGGER.error("Failed to fetch and set KMS key, KMS unseal key data is null");
            appendError(String.format(UNSEAL_KEY_DATA_NULL, instance.getVnfInstanceName()), operation);
            passToSuccessor(getAlternativeSuccessor(), context);
        }
    }

    private void processAndSetKMSKey(MessageHandlingContext<HelmReleaseLifecycleMessage> context, WorkflowSecretResponse workflowSecretResponse) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        Map<String, WorkflowSecretAttribute> allSecrets = workflowSecretResponse.getAllSecrets();
        String secretName = getUnsealKeySecretName(allSecrets);
        if (Strings.isNullOrEmpty(secretName)) {
            LOGGER.error("Failed to set KMS key, KMS unseal key not created");
            appendError(String.format(UNSEAL_KEY_NOT_CREATED_ERROR_MESSAGE, instance.getVnfInstanceName()), operation);
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            setKMSKeyInVNFInstance(allSecrets, secretName, context);
        }
    }

    private void setKMSKeyInVNFInstance(Map<String, WorkflowSecretAttribute> allSecrets, String secretName,
                                        MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        String jsonKMSUnsealKeyData = getTheJsonKMSUnsealKey(allSecrets.get(secretName).getData());
        if (Strings.isNullOrEmpty(jsonKMSUnsealKeyData)) {
            LOGGER.error("Failed to set KMS key, KMS unseal key data is not valid");
            appendError(String.format(UNSEAL_KEY_DATA_NULL, instance.getVnfInstanceName()), operation);
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            try {
                cryptoUtils.setEncryptDetailsForKey(KMS_UNSEAL_KEY, jsonKMSUnsealKeyData, instance);
                LOGGER.info("KMS key set in the vnf instance {}", instance.getVnfInstanceName());
                passToSuccessor(getSuccessor(), context);
            } catch (Exception ex) {
                LOGGER.error("An error occurred during setting KMS key", ex);
                appendError(String.format(UNABLE_TO_ENCRYPT_DATA_ERROR_MESSAGE, instance.getVnfInstanceName(), ex
                        .getMessage()), operation);
                passToSuccessor(getAlternativeSuccessor(), context);
            }
        }
    }

    private String getTheJsonKMSUnsealKey(Map<String, String> data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception ex) {
            LOGGER.warn("Unable to convert unseal key data to json", ex);
            return null;
        }
    }

    private static String getUnsealKeySecretName(Map<String, WorkflowSecretAttribute> allSecrets) {
        if (allSecrets == null || allSecrets.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, WorkflowSecretAttribute> entry : allSecrets.entrySet()) {
            String secretName = entry.getKey();
            if (secretName.contains(KMS_UNSEAL_KEY_POST_STRING)) {
                return secretName;
            }
        }
        return null;
    }
}
