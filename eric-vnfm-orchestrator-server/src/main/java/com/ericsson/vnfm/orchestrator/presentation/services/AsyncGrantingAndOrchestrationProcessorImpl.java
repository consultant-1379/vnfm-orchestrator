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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.HEAL;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.writeMapToValuesFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.infrastructure.annotations.StartingAndProcessingLcmOperationExceptionHandler;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.entity.CheckpointType;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationStage;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.GrantingRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.OperationRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.TerminateRequestHandler;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AsyncGrantingAndOrchestrationProcessorImpl implements AsyncGrantingAndOrchestrationProcessor {

    @Autowired
    private GrantingNotificationsConfig grantingNotificationsConfig;

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    @Lazy
    private AsyncGrantingAndOrchestrationProcessorImpl asyncGrantingAndOrchestrationProcessor;

    @Value("${instance}")
    private String instanceName;

    @Value("${recovery.operationStage.validTimeMinutes}")
    private Integer operationStageValidTimeMinutes;

    @Async
    @Override
    @StartingAndProcessingLcmOperationExceptionHandler
    public void process(Object request, OperationRequestHandler service, VnfInstance vnfInstance, LifecycleOperation operation,
                        Map<String, Object> valuesYamlMap) {
        final LifecycleOperationStage lifecycleOperationStage = operation.getLifecycleOperationStage();

        if (lifecycleOperationStage == null) {
            final LifecycleOperationStage newLifecycleOperationStage = new LifecycleOperationStage();
            newLifecycleOperationStage.setOperationId(operation.getOperationOccurrenceId());
            newLifecycleOperationStage.setOwner(instanceName);
            newLifecycleOperationStage.setCheckpoint(CheckpointType.INIT);
            newLifecycleOperationStage.setOwnedSince(LocalDateTime.now());
            LocalDateTime validUtil = LocalDateTime.now().plusMinutes(operationStageValidTimeMinutes);
            newLifecycleOperationStage.setValidUntil(validUtil);
            newLifecycleOperationStage.setLifecycleOperation(operation);

            operation.setLifecycleOperationStage(newLifecycleOperationStage);
            databaseInteractionService.persistLifecycleOperation(operation);
        }

        if (CheckpointType.INIT.equals(operation.getLifecycleOperationStage().getCheckpoint())) {
            asyncGrantingAndOrchestrationProcessor.requestGrantIfRequired(service, operation, request, valuesYamlMap);
        }

        if (CheckpointType.GRANTED.equals(operation.getLifecycleOperationStage().getCheckpoint())) {
            asyncGrantingAndOrchestrationProcessor.sendToWFS(request, service, vnfInstance, operation, valuesYamlMap);
        }
    }

    @Transactional
    public void requestGrantIfRequired(final OperationRequestHandler service,
                                       LifecycleOperation operation,
                                       final Object request,
                                       final Map<String, Object> valuesYamlMap) {
        if (service instanceof GrantingRequestHandler grantingRequestHandler) {
            if (nfvoConfig.isEnabled() && grantingNotificationsConfig.isGrantSupported()) {
                LOGGER.info("Processing granting request");
                grantingRequestHandler.verifyGrantingResources(operation, request, valuesYamlMap);
            } else {
                LOGGER.info("Granting not supported since condition(s) not satisfied: NFVO.enable={}, GrantSupported={}",
                            nfvoConfig.isEnabled(), grantingNotificationsConfig.isGrantSupported());
            }
        }

        LOGGER.info("The status of the checkpoint changes to {}", CheckpointType.GRANTED);
        LifecycleOperationStage lifecycleOperationStage = operation.getLifecycleOperationStage();
        lifecycleOperationStage.setOwner(instanceName);
        LocalDateTime validUtil = LocalDateTime.now().plusMinutes(operationStageValidTimeMinutes);
        lifecycleOperationStage.setValidUntil(validUtil);
        lifecycleOperationStage.setCheckpoint(CheckpointType.GRANTED);
        lifecycleOperationStage.setOwnedSince(LocalDateTime.now());

        databaseInteractionService.persistLifecycleOperation(operation);
    }

    @Transactional
    public void sendToWFS(final Object request,
                          final OperationRequestHandler service,
                          final VnfInstance vnfInstance,
                          final LifecycleOperation operation,
                          final Map<String, Object> valuesYamlMap) {
        if (service instanceof TerminateRequestHandler) {
            ((TerminateRequestHandler) service).deleteNodeFromENM(vnfInstance, operation);
        }

        Path valuePath = writeMapToValuesFile(valuesYamlMap);

        LOGGER.info("Sending request to Workflow service");
        service.sendRequest(vnfInstance, operation, request, valuePath);
        checkOperationError(operation);

        deleteFile(valuePath);

        LOGGER.info("Setting extension and instantiation level");
        updateExtensionsAndInstantiationLevelInOperation(service, vnfInstance, operation);

        LOGGER.info("Remove lifecycle operation stage");
        operation.setLifecycleOperationStage(null);

        LOGGER.info("Persisting operation after execution");
        service.persistOperationAndInstanceAfterExecution(vnfInstance, operation);
    }

    private void checkOperationError(LifecycleOperation operation) {
        if (List.of(FAILED, ROLLED_BACK).contains(operation.getOperationState())) {
            LifecycleOperationType type = operation.getLifecycleOperationType();
            if (List.of(INSTANTIATE, TERMINATE, HEAL).contains(type)) {
                databaseInteractionService.releaseNamespaceDeletion(operation);
            }
        }
    }

    private void updateExtensionsAndInstantiationLevelInOperation(final OperationRequestHandler service,
                                                                  final VnfInstance vnfInstance,
                                                                  final LifecycleOperation operation) {
        if (List.of(FAILED, ROLLED_BACK).contains(operation.getOperationState())) {
            LOGGER.debug("Operation is failed or rolled backed, skipping extension and instantiation level update");
            return;
        }

        if (Strings.isNullOrEmpty(vnfInstance.getTempInstance())) {
            service.setExtensionsAndInstantiationLevelInOperationInCurrentInstance(vnfInstance, operation);
        } else {
            service.setExtensionsAndInstantiationLevelInOperationInTempInstance(vnfInstance, operation);
        }
    }
}
