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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.EnumSet;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persist all the updates to the instance and operation which have been made by all handlers in handling the message
 */
@Slf4j
@AllArgsConstructor
public final class Persist extends MessageHandler<HelmReleaseLifecycleMessage> {

    private static final EnumSet<LifecycleOperationState> COMPLETED_OPERATION_STATES = EnumSet.of(COMPLETED, FAILED);
    private static final EnumSet<LifecycleOperationType> RELEASE_NAMESPACE_OPERATION_TYPES = EnumSet.of(INSTANTIATE, TERMINATE, HEAL);

    private DatabaseInteractionService databaseInteractionService;

    @Override
    @Transactional
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling persisting data");
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        updateNamespaceDeletionOnOperationFinished(operation, instance);
        updateVnfInstanceOnInstantiate(operation, instance);
        clearTempInstanceOnCompletion(operation, instance);
        databaseInteractionService.persistVnfInstanceAndOperation(instance, operation);
        passToSuccessor(getSuccessor(), context);
    }

    private void updateVnfInstanceOnInstantiate(final LifecycleOperation operation, final VnfInstance instance) {
        LifecycleOperationType lifecycleOperationType = operation.getLifecycleOperationType();
        LifecycleOperationState operationState = operation.getOperationState();
        if (instance.getTempInstance() != null &&
                (lifecycleOperationType == INSTANTIATE && operationState == COMPLETED)) {
            VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
            updateScaleInfo(instance, tempInstance);
            updateReplicaDetails(instance, tempInstance);
            updateInstantiationLevelIdFromTempInstance(instance, tempInstance);
            updateAttributesExtensionsFromTempInstance(instance, tempInstance);
        }
    }

    private void updateScaleInfo(final VnfInstance instance, final VnfInstance tempInstance) {
        var tempInstanceScaleInfoEntities = tempInstance.getScaleInfoEntity();
        tempInstanceScaleInfoEntities.forEach(scaleInfoEntity -> scaleInfoEntity.setVnfInstance(instance));
        var scaleInfoEntities = this.databaseInteractionService.saveScaleInfo(tempInstanceScaleInfoEntities);
        instance.setScaleInfoEntity(scaleInfoEntities);
    }

    private static void updateReplicaDetails(final VnfInstance instance, final VnfInstance tempInstance) {
        for (HelmChart chartTemp : tempInstance.getHelmCharts()) {
            instance.getHelmCharts().stream()
                    .filter(chartCurrent -> chartTemp.getReleaseName().equals(chartCurrent.getReleaseName()))
                    .findFirst()
                    .ifPresent(chartCurrent -> chartCurrent.setReplicaDetails(chartTemp.getReplicaDetails()));
        }
    }

    private static void updateInstantiationLevelIdFromTempInstance(final VnfInstance instance, final VnfInstance tempInstance) {
        if (tempInstance != null) {
            instance.setInstantiationLevel(tempInstance.getInstantiationLevel());
        }
    }

    private static void updateAttributesExtensionsFromTempInstance(final VnfInstance instance, final VnfInstance tempInstance) {
        if (tempInstance != null) {
            instance.setVnfInfoModifiableAttributesExtensions(tempInstance.getVnfInfoModifiableAttributesExtensions());
        }
    }

    private void clearTempInstanceOnCompletion(final LifecycleOperation operation, final VnfInstance instance) {
        if (isOperationFinished(operation) || isOperationRolledBack(operation)) {
            instance.setTempInstance(null);
        }
    }

    private void updateNamespaceDeletionOnOperationFinished(final LifecycleOperation operation, final VnfInstance instance) {
        if (isOperationToReleaseNamespace(operation) && isOperationFinished(operation)) {
            Optional<VnfInstanceNamespaceDetails> namespaceDetails = databaseInteractionService
                    .getNamespaceDetails(instance.getVnfInstanceId());
            namespaceDetails.ifPresent(vnfInstanceNamespaceDetails -> {
                LOGGER.info("Unmark namespace {} deletion", vnfInstanceNamespaceDetails.getNamespace());
                vnfInstanceNamespaceDetails.setDeletionInProgress(false);
                databaseInteractionService.persistNamespaceDetails(vnfInstanceNamespaceDetails);
            });
        }
    }

    private static boolean isOperationToReleaseNamespace(LifecycleOperation operation) {
        return RELEASE_NAMESPACE_OPERATION_TYPES.contains(operation.getLifecycleOperationType());
    }

    private static boolean isOperationFinished(LifecycleOperation operation) {
        return COMPLETED_OPERATION_STATES.contains(operation.getOperationState());
    }

    private static boolean isOperationRolledBack(final LifecycleOperation operation) {
        return operation.getOperationState() == ROLLED_BACK;
    }
}
