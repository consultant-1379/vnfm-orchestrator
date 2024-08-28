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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static java.util.Collections.emptyList;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationStateToProcessing;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getEnabledNotCrdAndNotProcessedCnfChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.notification.OperationState;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TerminateRequestHandler extends GrantingLifecycleRequestHandler implements GrantingRequestHandler {

    private static final int MAX_RETRY_VALUE = 3;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private ScheduledThreadPoolExecutor taskExecutor;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private LcmOpAdditionalParamsProcessor lcmOpAdditionalParamsProcessor;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.TERMINATE;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        final TerminateVnfRequest terminateVnfRequest = (TerminateVnfRequest) request;
        instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) terminateVnfRequest.getAdditionalParams());

        super.commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.TERMINATE);
    }

    public void setCleanUpResources(final VnfInstance vnfInstance, final Map<String, Object> additionalParams) {
        setInstanceWithCleanUpResources(vnfInstance, additionalParams);
        if (vnfInstance.isCleanUpResources()) {
            lifeCycleManagementHelper.persistNamespaceDetails(vnfInstance);
        }
    }

    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        setCleanUpResources(vnfInstance, additionalParams);
        helmChartHelper.resetHelmChartStates(vnfInstance.getHelmCharts());
        helmChartHelper.completeDisabledHelmCharts(vnfInstance.getHelmCharts());
        operation.setHelmClientVersion(vnfInstance.getHelmClientVersion());
    }

    @Override
    public void sendRequest(final VnfInstance vnfInstance,
                            final LifecycleOperation operation,
                            final Object request,
                            final Path toValuesFile) {

        operation.setCombinedAdditionalParams(additionalParamsFrom(request));
        updateOperationStateToProcessing(operation);

        final HelmChart firstHelmChartToTerminate = getEnabledNotCrdAndNotProcessedCnfChartWithHighestPriority(vnfInstance);
        firstHelmChartToTerminate.setState(LifecycleOperationState.PROCESSING.toString());

        final String firstReleaseToTerminate = firstHelmChartToTerminate.getReleaseName();

        WorkflowRoutingResponse response = workflowRoutingService.routeTerminateRequest(vnfInstance, operation, firstReleaseToTerminate);
        checkAndProcessFailedError(operation, response, firstReleaseToTerminate);
    }

    @Override
    protected void doVerifyGrantingResources(LifecycleOperation operation, final Object request, final Map<String, Object> valuesYamlMap) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());

        LOGGER.info("Starting granting resources delta calculation for Terminate operation. Package ID: {}", vnfInstance.getVnfPackageId());

        final LifecycleOperation lastOperation = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0).orElseThrow();
        Map<String, Object> processedSourceValuesMap = lcmOpAdditionalParamsProcessor.processRaw(vnfInstance);
        lcmOpAdditionalParamsProcessor.process(processedSourceValuesMap, vnfInstance.getVnfDescriptorId(), lastOperation);
        final List<ResourceDefinition> resourceDefinitions =
                grantingResourceDefinitionCalculation
                        .calculateRel4ResourcesInUse(vnfd, vnfInstance, processedSourceValuesMap, vnfInstance.getVnfDescriptorId());

        fillAndExecuteGrantRequest(emptyList(),
                                   resourceDefinitions,
                                   vnfInstance,
                                   operation,
                                   new GrantRequest());
    }

    @Override
    protected GrantedLcmOperationType getGrantingOperationType() {
        return GrantedLcmOperationType.TERMINATE;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteNodeFromENM(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        updateOperationStateToProcessing(operation);
        databaseInteractionService.persistLifecycleOperation(operation);

        if (vnfInstance.isAddedToOss()) {
            setAlarmSupervisionWithWarning(vnfInstance, DISABLE_ALARM_SUPERVISION);
            taskExecutor.execute(() -> verifyAndDeleteFromENM(vnfInstance, operation));
        }
    }

    private void verifyAndDeleteFromENM(VnfInstance vnfInstance, final LifecycleOperation operation) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_VALUE) {
            retryCount++;
            try {
                LOGGER.info("Deleting Node from ENM. Retry attempt {}", retryCount);
                ossNodeService.deleteNodeFromENM(vnfInstance, false);
                notificationService.sendNodeEvent(vnfInstance.getVnfInstanceId(), OperationState.COMPLETED, EnmOperationEnum.DELETE_NODE);
                break;
            } catch (Exception e) {
                LOGGER.warn("Unable to delete the Node from ENM. Retry attempt {}", retryCount, e);
                setDeleteNodeFailedStatusAndMessage(operation, vnfInstance, retryCount, e.getMessage());
            }
        }
        operation.setDeleteNodeFinished(true);

        databaseInteractionService.persistLifecycleOperation(operation);
    }

    private void setDeleteNodeFailedStatusAndMessage(final LifecycleOperation operation, final VnfInstance vnfInstance, final int retryCount,
                                                     final String exceptionMessage) {
        if (retryCount == MAX_RETRY_VALUE) {
            operation.setDeleteNodeErrorMessage(exceptionMessage);
            operation.setDeleteNodeFailed(true);
            notificationService.sendNodeEvent(vnfInstance.getVnfInstanceId(), OperationState.FAILED, EnmOperationEnum.DELETE_NODE);
            LOGGER.warn("Delete Node from ENM has failed, please log onto ENM and delete the Node manually");
        }
    }

    private static String additionalParamsFrom(final Object request) {
        final TerminateVnfRequest terminateVnfRequest = (TerminateVnfRequest) request;
        final Map<String, Object> additionalParams = checkAndCastObjectToMap(terminateVnfRequest.getAdditionalParams());

        return convertObjToJsonString(additionalParams);
    }
}
