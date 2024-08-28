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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.springframework.util.CollectionUtils.isEmpty;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.getNextCnfChart;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils.parseJsonOperationAdditionalParams;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.AbstractLifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingConfiguration;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.handlers.Persist;
import com.ericsson.vnfm.orchestrator.messaging.handlers.SetAlarmSupervision;
import com.ericsson.vnfm.orchestrator.messaging.handlers.heal.PatchKMSKey;
import com.ericsson.vnfm.orchestrator.messaging.handlers.heal.RestoreFromBackup;
import com.ericsson.vnfm.orchestrator.messaging.handlers.terminate.DeleteIdentifier;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HealOperation extends AbstractLifeCycleOperationProcessor {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private InstantiateRequestHandler instantiateRequestHandler;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private InstantiateOperation instantiateOperation;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private CryptoUtils cryptoUtils;


    @Override
    public Conditions getConditions() {
        return Conditions.HEAL_OPERATION_CONDITIONS;
    }

    @Override
    public void completed(final HelmReleaseLifecycleMessage message) {
        LifecycleOperation operation = databaseInteractionService
                .getLifecycleOperation(message.getLifecycleOperationId());
        if (HelmReleaseOperationType.TERMINATE.equals(message.getOperationType())) {
            completedTerminate(operation);

        }
        if (HelmReleaseOperationType.INSTANTIATE.equals(message.getOperationType())) {
            super.completed(message);
        }
    }

    @Override
    public MessageHandlingConfiguration<HelmReleaseLifecycleMessage> configureCompleted() {
        MessageHandlingConfiguration<HelmReleaseLifecycleMessage> triggerTeardown = instantiateOperation.getAlternativeFlow();

        return new MessageHandlingConfiguration<HelmReleaseLifecycleMessage>()
                .startWith(new PatchKMSKey(cryptoUtils, utility))
                .andThenOrElse(new RestoreFromBackup(utility, ossNodeService), triggerTeardown)
                .andThenOrElse(new SetAlarmSupervision(instantiateRequestHandler, EnmOperationEnum.ENABLE_ALARM_SUPERVISION), triggerTeardown)
                .andThen(new Persist(databaseInteractionService))
                .andThen(new DeleteIdentifier(utility))
                .end();
    }

    @Override
    public void failed(final HelmReleaseLifecycleMessage message) {
        // Not applicable for Heal operation
    }

    @Override
    public void rollBack(final HelmReleaseLifecycleMessage message) {
        utility.lifecycleTimedOut(message.getLifecycleOperationId(), InstantiationState.NOT_INSTANTIATED, message.getMessage());
    }

    private void updateOperationAndModelToFailed(final LifecycleOperation operation, final String errorMessage) {
        updateOperationState(operation, LifecycleOperationState.FAILED);
        setError(errorMessage, operation);
        databaseInteractionService.persistLifecycleOperation(operation);
    }

    private void completedTerminate(final LifecycleOperation operation) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        Optional<LifecycleOperation> installOrUpgradeOperation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0);
        if (installOrUpgradeOperation.isEmpty()) {
            String errorMessage =
                    "Unable to retrieve a previous completed install or upgrade operation for VnfInstance "
                            + vnfInstance.getVnfInstanceId();
            LOGGER.error(errorMessage);
            updateOperationAndModelToFailed(operation, errorMessage);
        } else if (!StringUtils.hasLength(vnfInstance.getOssNodeProtocolFile())
                && MessageUtility.getAdditionalParams(operation).containsKey(RESTORE_BACKUP_FILE_REFERENCE)) {
            String errorMessage = "Unable to retrieve oss node protocol file for VnfInstance " + vnfInstance.getVnfInstanceId();
            LOGGER.error(errorMessage);
            updateOperationAndModelToFailed(operation, errorMessage);
        } else {
            LifecycleOperation updatedOperation = updateOperationWithInstantiateParams(operation,
                    installOrUpgradeOperation.get(), vnfInstance);
            if (updatedOperation == null) {
                String errorMessage =
                        "Unable to create the instantiate request for VnfInstance " + vnfInstance.getVnfInstanceId();
                LOGGER.error(errorMessage);
                updateOperationAndModelToFailed(operation, errorMessage);
                return;
            }
            vnfInstance.setCleanUpResources(false);
            helmChartHelper.resetHelmChartStates(vnfInstance.getHelmCharts());
            helmChartHelper.completeDisabledHelmCharts(vnfInstance.getHelmCharts());
            triggerInstantiate(updatedOperation, vnfInstance);
        }
    }

    private void triggerInstantiate(final LifecycleOperation operation, final VnfInstance vnfInstance) {
        LOGGER.info("Trigger Instantiate");
        lifeCycleManagementHelper.persistNamespaceDetails(vnfInstance);

        Optional<HelmChart> nextChart = getNextCnfChart(vnfInstance);
        if (nextChart.isPresent()) {
            HelmChart helmChart = nextChart.get();
            LOGGER.info("Next chart name is {}", helmChart.getHelmChartName());
            WorkflowRoutingResponse response = workflowRoutingService
                    .routeInstantiateRequest(helmChart.getPriority(), operation, vnfInstance);
            instantiateRequestHandler.checkAndProcessFailedError(operation, response, helmChart.getReleaseName());
        }
        databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, operation);
    }

    private LifecycleOperation updateOperationWithInstantiateParams(final LifecycleOperation currentOperation,
                                                                    final LifecycleOperation lastSuccessfulInstallUpgradeOperation,
                                                                    final VnfInstance vnfInstance) {
        InstantiateVnfRequest instantiatedVnfRequest = getInstantiateVnfRequest(lastSuccessfulInstallUpgradeOperation,
                                                                                vnfInstance);
        if (instantiatedVnfRequest == null) {
            return null;
        }
        addOtpToAdditionalParams(instantiatedVnfRequest, vnfInstance);
        currentOperation.setCombinedAdditionalParams(lastSuccessfulInstallUpgradeOperation.getCombinedAdditionalParams());
        currentOperation.setCombinedValuesFile(vnfInstance.getCombinedValuesFile());
        currentOperation.setValuesFileParams(lastSuccessfulInstallUpgradeOperation.getValuesFileParams());
        return currentOperation;
    }

    @VisibleForTesting
    public InstantiateVnfRequest getInstantiateVnfRequest(
            final LifecycleOperation lastSuccessfulInstallUpgradeOperation, final VnfInstance instance) {
        if (LifecycleOperationType.INSTANTIATE
                .equals(lastSuccessfulInstallUpgradeOperation.getLifecycleOperationType())) {
            return parseJson(lastSuccessfulInstallUpgradeOperation.getOperationParams(), InstantiateVnfRequest.class);
        } else {
            return mergeInstantiatedWithChangePackage(lastSuccessfulInstallUpgradeOperation, instance);
        }
    }

    @VisibleForTesting
    protected InstantiateVnfRequest mergeInstantiatedWithChangePackage(final LifecycleOperation lastSuccessfulUpgrade,
                                                                       final VnfInstance instance) {
        Optional<LifecycleOperation> lastSuccessfulInstallOperation = lcmOpSearchService.searchLastSuccessfulInstallOperation(instance);
        if (lastSuccessfulInstallOperation.isEmpty()) {
            return null;
        }
        InstantiateVnfRequest previousRequest = parseJson(
                lastSuccessfulInstallOperation.get().getOperationParams(), InstantiateVnfRequest.class
        );
        Map<String, Object> additionalParams = checkAndCastObjectToMap(parseJsonOperationAdditionalParams(lastSuccessfulUpgrade));
        Map<String, Object> upgradeAdditionalParams = isEmpty(additionalParams) ? Collections.emptyMap() : additionalParams;
        Map<String, Object> previousAdditionalParams = checkAndCastObjectToMap(previousRequest.getAdditionalParams());
        previousAdditionalParams.putAll(upgradeAdditionalParams);
        return previousRequest;
    }

    private static void addOtpToAdditionalParams(InstantiateVnfRequest instantiateRequest, VnfInstance vnfInstance) {
        String ossNodeProtocolFileAsString = vnfInstance.getOssNodeProtocolFile();
        if (ossNodeProtocolFileAsString != null) {
            Map<String, Object> config0Params = CMPEnrollmentHelper.createDay0ConfigurationParams(vnfInstance);
            Map<String, Object> operationParams = checkAndCastObjectToMap(instantiateRequest.getAdditionalParams());
            operationParams.putAll(config0Params);
        }
    }
}
