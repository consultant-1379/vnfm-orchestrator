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
package com.ericsson.vnfm.orchestrator.messaging;

import static com.ericsson.vnfm.orchestrator.messaging.operations.TerminateOperation.isReleaseNameOrNamespaceNotFound;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.appendError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.DELETE_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils.parseJsonOperationAdditionalParams;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.updateHelmChartsDeletePvcState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DeleteNamespaceException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Rollback;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Enums;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MessageUtility {
    private static final String REQUEST_FAILED_DUE_TO_TIMEOUT = "Marking lifecycle operation %s as failed due to timeout";

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private Rollback rollback;

    @Autowired
    private RollbackService rollbackService;

    public static void updateScaleInfoInInstance(VnfInstance instance, List<ScaleInfoEntity> scaleInfoEntities) {
        for (ScaleInfoEntity currentScaleInfo : instance.getScaleInfoEntity()) {
            for (ScaleInfoEntity requiredScaleInfo : scaleInfoEntities) {
                if (currentScaleInfo.getAspectId().equals(requiredScaleInfo.getAspectId())) {
                    currentScaleInfo.setScaleLevel(requiredScaleInfo.getScaleLevel());
                    break;
                }
            }
        }
    }

    public static void updateHelmChartInInstanceAfterScale(VnfInstance instance, List<HelmChart> helmChartList) {
        for (HelmChart tempChart : helmChartList) {
            for (HelmChart currentChart : instance.getHelmCharts()) {
                if (tempChart.getHelmChartUrl().equals(currentChart.getHelmChartUrl())) {
                    currentChart.setRevisionNumber(tempChart.getRevisionNumber());
                    currentChart.setReplicaDetails(tempChart.getReplicaDetails());
                    currentChart.setState(tempChart.getState());
                }
            }
        }
    }

    public static List<HelmChart> addInstance(VnfInstance instance, List<HelmChart> helmChartList) {
        List<HelmChart> helmCharts = new ArrayList<>();
        for (HelmChart chart : helmChartList) {
            chart.setVnfInstance(instance);
            helmCharts.add(chart);
        }
        return helmCharts;
    }

    public static List<TerminatedHelmChart> addInstanceToTerminateHelmChart(VnfInstance instance, List<TerminatedHelmChart> helmChartList) {
        List<TerminatedHelmChart> helmCharts = new ArrayList<>();
        for (TerminatedHelmChart chart : helmChartList) {
            chart.setVnfInstance(instance);
            helmCharts.add(chart);
        }
        instance.setTerminatedHelmCharts(helmCharts);
        return helmCharts;
    }

    private void updateInstanceOnFail(final LifecycleOperation operation, InstantiationState instantiationState) {
        VnfInstance vnfInstance = operation.getVnfInstance();

        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        for (HelmChart chart : helmCharts) {
            chart.setState(FAILED.toString());
        }

        vnfInstance.setTempInstance(null);
        vnfInstance.setInstantiationState(instantiationState);

        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    private static boolean isFinished(final LifecycleOperation operation) {
        final LifecycleOperationState operationState = operation.getOperationState();
        return operationState == LifecycleOperationState.COMPLETED || operationState == LifecycleOperationState.FAILED
                || operationState == LifecycleOperationState.ROLLED_BACK;
    }

    public static Optional<HelmChart> getNextChart(final VnfInstance vnfInstance) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        return helmCharts.stream().filter(obj -> obj.getState() == null)
                .sorted(Comparator.comparing(HelmChart::getPriority)).findFirst();
    }

    public static Optional<HelmChart> getNextCnfChart(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> chart.getState() == null)
                .min(Comparator.comparing(HelmChart::getPriority));
    }

    public static Optional<HelmChart> getCurrentlyProcessingChart(final VnfInstance vnfInstance) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        return helmCharts.stream().filter(obj -> HelmReleaseState.PROCESSING.toString().equalsIgnoreCase(obj.getState()))
                .sorted(Comparator.comparing(HelmChart::getPriority)).findFirst();
    }

    public static String formatRollbackErrorMessage(String errorMsg, String releaseName) {
        return String.format("Rollback failed for %s due to %s", releaseName, errorMsg);
    }

    public static List<HelmChart> getCompletedHelmCharts(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> COMPLETED.toString().equals(helmChart.getState())).collect(Collectors.toList());
    }

    public static List<HelmChart> getFailedHelmCharts(final VnfInstance vnfInstance) {
        return vnfInstance.getHelmCharts().stream()
                .filter(helmChart -> FAILED.toString().equals(helmChart.getState())).collect(Collectors.toList());
    }

    @Transactional
    public void lifecycleTimedOut(String operationOccurrenceId, InstantiationState instantiationState, String errorMessage) {
        updateOperationOnFail(operationOccurrenceId, errorMessage, String.format(REQUEST_FAILED_DUE_TO_TIMEOUT, operationOccurrenceId),
                              instantiationState, FAILED);
    }

    @Transactional
    public void updateOperationOnFail(String operationOccurrenceId, String errorMessage, String operationFailDetails,
                                      InstantiationState instantiationState, LifecycleOperationState state) {
        LOGGER.error(operationFailDetails);
        LifecycleOperation operation = getOperation(operationOccurrenceId);
        if (isFinished(operation)) {
            LOGGER.info("Operation {} is already failed, ignoring this event.", operationOccurrenceId);
            return;
        }
        updateOperation(errorMessage, operation, state);
        updateInstanceOnFail(operation, instantiationState);
        updateNamespaceDetails(operation);
    }

    public void updateNamespaceDetails(final LifecycleOperation operation) {
        Optional<VnfInstanceNamespaceDetails> namespaceDetails = databaseInteractionService
                .getNamespaceDetails(operation.getVnfInstance().getVnfInstanceId());
        namespaceDetails.ifPresent(vnfInstanceNamespaceDetails -> {
            vnfInstanceNamespaceDetails.setDeletionInProgress(false);
            databaseInteractionService.persistNamespaceDetails(vnfInstanceNamespaceDetails);
        });
    }

    @Transactional
    public void updateFailedOperationAndChartByReleaseName(String operationOccurrenceId, String errorMessage, String operationFailDetails,
                                                           LifecycleOperationState operationState, LifecycleOperationState chartState,
                                                           final String failedReleaseName) {
        LOGGER.error(operationFailDetails);
        LifecycleOperation operation = getOperation(operationOccurrenceId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        if (isFinished(operation)) {
            LOGGER.info("Operation {} is already failed, ignoring this event.", operationOccurrenceId);
            return;
        }
        updateOperation(errorMessage, operation, operationState);
        updateFailedChartForTempInstance(vnfInstance, failedReleaseName, chartState);
    }

    private void updateFailedChartForTempInstance(VnfInstance vnfInstance, final String failedReleaseName,
                                                  LifecycleOperationState chartState) {
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> helmCharts = tempInstance.getHelmCharts();
        helmCharts.stream()
                .filter(chart -> chart.getReleaseName().equalsIgnoreCase(failedReleaseName))
                .findFirst()
                .or(() -> tempInstance.getTerminatedHelmCharts()
                            .stream()
                            .filter(chart -> chart.getReleaseName().equalsIgnoreCase(failedReleaseName))
                            .findFirst()
                            .map(HelmChartUtils::toHelmChart))
                .ifPresent(o -> o.setState(chartState.toString()));
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    public static boolean isDowngradeOperation(Optional<ChangePackageOperationDetails> changePackageOperationDetails) {
        return changePackageOperationDetails.isPresent() &&
                changePackageOperationDetails.get().getChangePackageOperationSubtype() == ChangePackageOperationSubtype.DOWNGRADE;
    }

    public static boolean isAutorollbackAndDownsizeAllowedOperation(LifecycleOperation operation) {
        return operation.isAutoRollbackAllowed() && operation.isDownsizeAllowed() && operation.getLifecycleOperationType().equals(
                LifecycleOperationType.CHANGE_VNFPKG);
    }

    public void updateOperation(final String message, final LifecycleOperation operation, LifecycleOperationState state) {
        updateOperationState(operation, state);
        setError(message != null ? message : "Failure event did not contain an error message", operation);
        databaseInteractionService.persistLifecycleOperation(operation);
    }

    public static void updateOperationOnRollingBack(LifecycleOperation operation) {
        operation.setAutoRollbackAllowed(false);
        updateOperationState(operation, LifecycleOperationState.ROLLING_BACK);
        operation.setTargetVnfdId(operation.getSourceVnfdId());
    }

    public void updateChart(final LifecycleOperation operation, final String releaseName, final String state,
                            final String revisionNumber) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        databaseInteractionService.saveVnfInstanceToDB(updateChartStateAndRevisionNumber(vnfInstance, releaseName, state, revisionNumber));
    }

    public VnfInstance updateChartStateAndRevisionNumber(final VnfInstance vnfInstance, final String releaseName,
                                                         final String state, final String revisionNumber) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        helmCharts.stream().filter(obj -> obj.getReleaseName().equalsIgnoreCase(releaseName)).findFirst()
                .ifPresent(o -> {
                    o.setState(state);
                    o.setRevisionNumber(revisionNumber);
                });
        return vnfInstance;
    }

    public static VnfInstance updateChartStateIncreaseRetryCount(final VnfInstance vnfInstance,
                                                                 final String releaseName,
                                                                 final String state,
                                                                 final int priority) {

        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        LOGGER.info("Updating state to {} and increasing retry count for release {}", state, releaseName);
        helmCharts.stream().filter(obj -> obj.getPriority() == priority).findFirst()
                .ifPresent(o -> {
                    o.setState(state);
                    o.setRetryCount(o.getRetryCount() + 1);
                });
        return vnfInstance;
    }

    public VnfInstance updateChartState(final VnfInstance vnfInstance, final String releaseName, final String state) {
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        LOGGER.info("Updating state to {} for release {}", state, releaseName);
        helmCharts.stream().filter(obj -> obj.getReleaseName().equalsIgnoreCase(releaseName)).findFirst()
                .ifPresent(o -> o.setState(state));
        return vnfInstance;
    }

    public void saveErrorMessage(final HelmReleaseLifecycleMessage message, final LifecycleOperation operation) {
        saveErrorMessage(message.getMessage(), message.getReleaseName(), message.getOperationType().toString(),
                         operation);
    }

    public void saveErrorMessage(final String message, final String releaseName, final String type,
                                 final LifecycleOperation operation) {
        final String errorMessage = message != null ? message : "Failure event did not contain an error message";
        setError(String.format("%s for %s failed with %s.", type, releaseName, errorMessage), operation);
    }

    public void updateTempChartForRollback(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        HelmReleaseLifecycleMessage message = context.getMessage();
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        VnfInstance upgradedInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        operation.setHelmClientVersion(instance.getHelmClientVersion());
        saveErrorMessage(message, operation);
        final String releaseName = message.getReleaseName();
        HelmChartUtils.setCompletedChartsStateToProcessing(upgradedInstance, operation);
        upgradedInstance = updateChartState(upgradedInstance, releaseName, LifecycleOperationState.ROLLING_BACK.toString());

        rollbackService.rollbackChart(instance, upgradedInstance, operation, releaseName);
    }

    public void updateChartForRollback(VnfInstance actualInstance, HelmReleaseLifecycleMessage message, VnfInstance tempInstance) {
        LifecycleOperation operation = getOperation(message.getLifecycleOperationId());
        saveErrorMessage(message, operation);
        VnfInstance updatedTempInstance = updateChartState(tempInstance, message.getReleaseName(), LifecycleOperationState.ROLLING_BACK.toString());
        actualInstance.setTempInstance(Utility.convertObjToJsonString(updatedTempInstance));
        HelmChartBaseEntity chart = actualInstance.getHelmCharts().stream()
                .filter(helmChart -> helmChart.getHelmChartType() != HelmChartType.CRD)
                .filter(obj -> obj.getReleaseName().equalsIgnoreCase(message.getReleaseName()))
                .findFirst()
                .orElse(null);
        if (chart == null) {
            chart = actualInstance.getTerminatedHelmCharts().stream()
                    .filter(helmChart -> helmChart.getHelmChartType() != HelmChartType.CRD)
                    .filter(obj -> obj.getReleaseName().equalsIgnoreCase(message.getReleaseName()))
                    .findFirst()
                    .orElse(null);
        }
        String revisionNumber = chart != null ? chart.getRevisionNumber() : "0";
        triggerRollbackOperation(operation, message.getReleaseName(), revisionNumber, actualInstance);
    }

    public void triggerRollbackOperation(LifecycleOperation operation, String releaseName, String revisionNumber,
                                         final VnfInstance actualInstance) {
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, operation.getApplicationTimeout());
        rollback.triggerRollbackOperation(operation, releaseName, revisionNumber, actualInstance, false);
    }

    public boolean triggerDeleteNamespace(final LifecycleOperation operation, final String namespace,
                                          final String clusterName, final String releaseName, final String applicationTimeout) {
        try {
            workflowRoutingService.routeDeleteNamespace(namespace, clusterName, releaseName, applicationTimeout,
                                                        operation.getOperationOccurrenceId());
            return true;
        } catch (DeleteNamespaceException e) {
            String errorMessage = e.getMessage();
            LOGGER.error("failed to delete namespace {}", namespace, e);
            appendError(errorMessage, operation);
            updateOperationState(operation, LifecycleOperationState.FAILED);
            return false;
        }
    }

    public boolean triggerPatchSecret(final String secretName, final String key, final String keyContents,
                                      final LifecycleOperation operation, final VnfInstance vnfInstance) {
        ResponseEntity<Object> response = workflowRoutingService
                .routeToEvnfmWfsForPatchingSecrets(secretName, key, keyContents, vnfInstance.getClusterName(),
                                                   vnfInstance.getNamespace());
        var responseBody = response.getBody();
        if (response.getStatusCode().isError() && responseBody != null) {
            String errorMessage = responseBody.toString();
            appendError(errorMessage, operation);
            databaseInteractionService.persistLifecycleOperation(operation);
            return false;
        }
        return true;
    }

    public boolean triggerDeletePvcs(final LifecycleOperation operation, final VnfInstance vnfInstance, final String releaseName) {
        ResponseEntity<Object> response = workflowRoutingService
                .routeDeletePvcRequest(vnfInstance, releaseName, operation.getOperationOccurrenceId());
        var responseBody = response.getBody();
        if (response.getStatusCode().isError() && responseBody != null) {
            String errorMessage = responseBody.toString();
            if (isReleaseNameOrNamespaceNotFound(errorMessage)
                    && !LifecycleOperationType.CHANGE_VNFPKG.equals(operation.getLifecycleOperationType())) {
                operation.setOperationState(COMPLETED);
                return true;
            } else {
                updateHelmChartsDeletePvcState(vnfInstance, operation, releaseName, FAILED.toString());
                appendError(errorMessage, operation);
                return false;
            }
        }
        return true;
    }

    public void updateInstanceOnChangeVnfPackageOperation(VnfInstance sourceInstance, VnfInstance targetInstance,
                                                          LifecycleOperation operation, LifecycleOperationState lifecycleOperationState) {
        targetInstance.setVnfInstanceId(sourceInstance.getVnfInstanceId());
        if (targetInstance.getVnfInstanceDescription() == null) {
            targetInstance.setVnfInstanceDescription(sourceInstance.getVnfInstanceDescription());
        }
        targetInstance.getHelmCharts().forEach(chart -> chart.setState(lifecycleOperationState.toString()));
        targetInstance.setHelmCharts(addInstance(targetInstance, targetInstance.getHelmCharts()));
        targetInstance.setAllOperations(sourceInstance.getAllOperations());
        targetInstance.setTerminatedHelmCharts(addInstanceToTerminateHelmChart(targetInstance, targetInstance.getTerminatedHelmCharts()));
        if (Strings.isNotBlank(targetInstance.getInstantiationLevel())) {
            sourceInstance.setInstantiationLevel(targetInstance.getInstantiationLevel());
        }
        if (Strings.isNotBlank(targetInstance.getVnfInfoModifiableAttributesExtensions())) {
            sourceInstance.setVnfInfoModifiableAttributesExtensions(targetInstance.getVnfInfoModifiableAttributesExtensions());
        }
        targetInstance.setOperationOccurrenceId(operation.getOperationOccurrenceId());
        targetInstance.getAllOperations().stream()
                .filter(operationOcc -> operationOcc.getOperationOccurrenceId().equals(operation.getOperationOccurrenceId()))
                .findFirst()
                .ifPresent(upgradeOperation -> upgradeOperation.setVnfInstance(targetInstance));
    }

    public void updateUnusedInstance(VnfInstance unusedInstance, String operationTargetPackageId) {
        LOGGER.info("Updating instance with source pckg id {}, target pckg id {}", unusedInstance.getVnfPackageId(), operationTargetPackageId);
        VnfInstance savedInstance = databaseInteractionService.getVnfInstance(unusedInstance.getVnfInstanceId());
        instanceService.removeHelmChartEntriesFromInstance(savedInstance);
        instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(unusedInstance.getVnfPackageId(),
                                                                                         operationTargetPackageId,
                                                                                         unusedInstance.getVnfPackageId(),
                                                                                         unusedInstance.getVnfInstanceId(),
                                                                                         false);
    }

    public void updateInstanceWithHelmCharts(VnfInstance upgradedInstance, VnfInstance sourceInstance) {
        List<HelmChart> sourceHelmCharts = sourceInstance.getHelmCharts();
        List<HelmChart> chartsToRemove = new ArrayList<>();
        for (HelmChart tempChart : sourceHelmCharts) {
            if (upgradedInstance.getHelmCharts().stream()
                    .noneMatch(chart -> chart.getHelmChartUrl().equals(tempChart.getHelmChartUrl()))) {
                upgradedInstance.getHelmCharts().add(tempChart);
                chartsToRemove.add(tempChart);
            }
        }
        sourceInstance.getHelmCharts().removeAll(chartsToRemove);
    }

    public void deleteIdentifier(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        try {
            if (StringUtils.isNotEmpty(operation.getOperationParams())) {
                TerminateVnfRequest terminateVnfRequest = mapper.readValue(operation.getOperationParams(),
                                                                           TerminateVnfRequest.class);
                Map<String, Object> additionalParams = checkAndCastObjectToMap(terminateVnfRequest.getAdditionalParams());
                if (!CollectionUtils.isEmpty(additionalParams)) {
                    executeDeleteIdentifier(vnfInstance, additionalParams);
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to delete the identifier for vnfInstance : {}",
                         vnfInstance.getVnfInstanceId(), e);
        }
    }

    private void executeDeleteIdentifier(final VnfInstance instance, final Map additionalParams) {
        if (additionalParams.containsKey(DELETE_IDENTIFIER)
                && Boolean.TRUE.equals(Boolean.parseBoolean(additionalParams.get(DELETE_IDENTIFIER).toString()))) {
            instanceService.deleteInstanceEntity(instance.getVnfInstanceId(), false);
        }
    }

    public static boolean isAnyChartProcessing(VnfInstance vnfInstance) {
        return vnfInstance
                .getHelmCharts()
                .stream()
                .anyMatch(chart -> StringUtils.equals(LifecycleOperationState.PROCESSING.toString(), chart.getState()));
    }

    public static void updateOperationAndInstanceOnCompleted(final LifecycleOperation operation, final InstantiationState state,
                                                             final VnfInstance vnfInstance, LifecycleOperationState operationState) {
        updateOperationState(operation, operationState);
        vnfInstance.setInstantiationState(state);
    }

    public static void updateOperationAndInstanceOnFailure(final LifecycleOperation operation, final InstantiationState state,
                                                             final VnfInstance vnfInstance, LifecycleOperationState operationState) {
        updateOperationState(operation, operationState);
        vnfInstance.setInstantiationState(state);
    }

    public static Map<String, Object> getAdditionalParams(LifecycleOperation operation) {
        return Optional.ofNullable(operation.getOperationParams())
                .filter(StringUtils::isNotEmpty)
                .map(operationParamsAsString -> {
                    var additionalParams = parseJsonOperationAdditionalParams(operation);
                    return checkAndCastObjectToMap(additionalParams);
                }).orElseGet(HashMap::new);
    }

    public static boolean isLifecycleOperationIdNull(String lifecycleOperationId) {
        if (StringUtils.isEmpty(lifecycleOperationId)) {
            LOGGER.info("Lifecycle operation Id was null, message will be ignored");
            return true;
        }
        return false;
    }

    public static <M> boolean isMessageProcessorNotNull(MessageProcessor<M> messageProcessor, Conditions conditions) {
        if (Objects.isNull(messageProcessor)) {
            LOGGER.info("Can not resolve message processor for conditions: {}, message will be ignored", conditions);
            return false;
        }
        return true;
    }

    public static boolean isAlreadyFinished(final LifecycleOperation operation) {
        final LifecycleOperationState operationState = operation.getOperationState();
        if (operationState == LifecycleOperationState.COMPLETED
                || operationState == LifecycleOperationState.FAILED
                || operationState == LifecycleOperationState.ROLLED_BACK) {
            LOGGER.info("Operation {} is already {}, message will be ignored", operation.getOperationOccurrenceId(),
                        operationState);
            return true;
        }
        return false;
    }

    public LifecycleOperation getOperation(final String lifecycleOperationId) {
        return databaseInteractionService.getLifecycleOperation(lifecycleOperationId);
    }

    public static LifecycleOperationState mapHelmReleaseStateToLifeCycleOperationState(final HelmReleaseState helmReleaseState) {
        return Enums.getIfPresent(LifecycleOperationState.class, helmReleaseState.name()).orNull();
    }

    public void updateScaleInfo(final VnfInstance instance, final VnfInstance tempInstance) {
        var tempInstanceScaleInfoEntities = tempInstance.getScaleInfoEntity();
        if (!CollectionUtils.isEmpty(tempInstanceScaleInfoEntities)) {
            tempInstanceScaleInfoEntities.forEach(scaleInfoEntity -> scaleInfoEntity.setVnfInstance(instance));
        }
    }

    public void updateReplicaDetails(final VnfInstance instance, final VnfInstance tempInstance) {
        for (HelmChart chartTemp : tempInstance.getHelmCharts()) {
            instance.getHelmCharts().stream()
                    .filter(chartCurrent -> chartTemp.getReleaseName().equals(chartCurrent.getReleaseName()))
                    .findFirst()
                    .ifPresent(chartCurrent -> chartCurrent.setReplicaDetails(chartTemp.getReplicaDetails()));
        }
    }
}
