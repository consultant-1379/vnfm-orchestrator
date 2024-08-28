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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.DEFAULT_CLUSTER_FILE_NOT_FOUND;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_BACK_OFF_PERIOD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_KEY_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_LOCK_DURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_MAX_LOCK_ATTEMPTS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.setPriorityBeforeOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ericsson.am.shared.locks.Lock;
import com.ericsson.am.shared.locks.LockManager;
import com.ericsson.am.shared.locks.LockMode;
import com.ericsson.vnfm.orchestrator.infrastructure.annotations.StartingAndProcessingLcmOperationExceptionHandler;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ClusterConfigFileNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.CleanupRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifeCycleRequestFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.OperationRequestHandler;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LifeCycleManagementServiceImpl implements LifeCycleManagementService {

    @Lazy
    @Autowired
    LifeCycleManagementService selfInject;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private AsyncGrantingAndOrchestrationProcessorImpl asyncGrantingAndOrchestrationProcessor;

    @Autowired
    private LifeCycleRequestFactory lifeCycleRequestFactory;

    @Autowired
    private LifecycleOperationHelper lifecycleOperationHelper;

    @Autowired
    private CleanupRequestHandler cleanupRequestHandler;

    @Autowired
    private EnmMetricsExposers enmMetricsExposers;

    @Autowired
    private LockManager lockManager;

    @Override
    @Transactional
    public String executeRequest(final LifecycleOperationType type, final String vnfInstanceId, final Object request,
                                 final String requestUsername, final Map<String, Object> valuesYamlMap) {

        OperationRequestHandler service = lifeCycleRequestFactory.getService(type);

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

        Optional<Lock> maybeLock = Optional.of(type).filter(LifecycleOperationType.INSTANTIATE::equals)
                .map(unused -> {
                    String clusterConfigName = resolveClusterConfigName((InstantiateVnfRequest) request);

                    return lockManager.getLock(LockMode.SHARED, CLUSTER_CONFIG_KEY_PREFIX + clusterConfigName)
                            .withAcquireRetries(CLUSTER_CONFIG_MAX_LOCK_ATTEMPTS, CLUSTER_CONFIG_BACK_OFF_PERIOD);
                });

        maybeLock.ifPresent(lock -> lock.lock(CLUSTER_CONFIG_LOCK_DURATION, TimeUnit.SECONDS));
        try {
            LOGGER.info("Request validation");
            service.specificValidation(vnfInstance, request);

            service.updateClusterConfigStatus(vnfInstance);

            if (vnfInstance.getHelmCharts() != null) {
                setPriorityBeforeOperation(type, vnfInstance);
            }

            service.createTempInstance(vnfInstance, request);

            Map<String, Object> additionalParams = service.formatParameters(
                    vnfInstance, request, service.getType(), valuesYamlMap
            );
            String applicationTimeout = (String) additionalParams.get(APPLICATION_TIME_OUT);

            LifecycleOperation operation = service.persistOperation(
                    vnfInstance, request, requestUsername, service.getType(), valuesYamlMap, applicationTimeout
            );

            enmMetricsExposers.incrementInstantiateMetrics(type, additionalParams);
            return selfInject.executeRequest(service, vnfInstance, request, operation, valuesYamlMap, additionalParams);
        } finally {
            maybeLock.ifPresent(Lock::unlock);
        }
    }

    @Override
    @Transactional
    public String cleanup(VnfInstance vnfInstance, CleanupVnfRequest cleanupVnfRequest, String requestUsername) {

        cleanupRequestHandler.specificValidation(vnfInstance, cleanupVnfRequest);

        LifecycleOperation currentOperation = databaseInteractionService.getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
        if (lifecycleOperationHelper.checkClusterConfigNotFoundError(currentOperation)
                || lifecycleOperationHelper.checkInstantiateFailedWithRollback(currentOperation)) {
            instanceService.deleteInstanceEntity(vnfInstance.getVnfInstanceId(), true);
            return vnfInstance.getOperationOccurrenceId();
        }

        final TerminateVnfRequest cleanUpTerminateRequest = cleanupRequestHandler.createTerminateRequest(cleanupVnfRequest);
        OperationRequestHandler service = lifeCycleRequestFactory.getService(LifecycleOperationType.TERMINATE);

        Map<String, Object> valuesYamlMap = new HashMap<>();
        Map<String, Object> additionalParams = service.formatParameters(
                vnfInstance, cleanUpTerminateRequest, service.getType(), valuesYamlMap
        );
        String applicationTimeout = (String) additionalParams.get(APPLICATION_TIME_OUT);
        LifecycleOperation operation = service.persistOperation(
                vnfInstance, cleanUpTerminateRequest, requestUsername, service.getType(), valuesYamlMap, applicationTimeout
        );
        return selfInject.executeRequest(service, vnfInstance, cleanUpTerminateRequest, operation, valuesYamlMap, additionalParams);
    }

    @Override
    @StartingAndProcessingLcmOperationExceptionHandler
    public String executeRequest(OperationRequestHandler service, VnfInstance vnfInstance, Object request, LifecycleOperation operation,
                                 Map<String, Object> valuesYamlMap, Map<String, Object> additionalParams) {

        MDC.put(LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY, operation.getOperationOccurrenceId());

        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LOGGER.info("Updating VNF instance with information relevant to the operation");
        service.updateInstance(vnfInstance, request, service.getType(), operation, additionalParams);

        LOGGER.info("Processing values.yaml file");
        Map<String, Object> processedValuesYamlMap = Utility.checkAndCastObjectToMap(valuesYamlMap);
        service.processValuesYaml(processedValuesYamlMap, vnfInstance, request, operation);

        executeAfterTransactionCommits(() -> asyncGrantingAndOrchestrationProcessor
                .process(request, service, vnfInstance, operation, processedValuesYamlMap));

        MDC.clear();
        return operation.getOperationOccurrenceId();
    }

    private void executeAfterTransactionCommits(Runnable task) {
        Map<String, String> webThreadContext = MDC.getCopyOfContextMap();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                MDC.setContextMap(webThreadContext);
                task.run();
            }
        });
    }

    private String resolveClusterConfigName(InstantiateVnfRequest request) {
        return Optional.ofNullable(request.getClusterName())
                .or(() -> databaseInteractionService.getDefaultClusterName())
                .map(Utility::addConfigExtension)
                .orElseThrow(() -> new ClusterConfigFileNotFoundException(DEFAULT_CLUSTER_FILE_NOT_FOUND));
    }
}
