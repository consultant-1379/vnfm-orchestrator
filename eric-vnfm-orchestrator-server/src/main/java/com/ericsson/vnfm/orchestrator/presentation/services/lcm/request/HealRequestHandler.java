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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.NodeTypeUtility;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
public final class HealRequestHandler extends LifecycleRequestHandler {

    private static final List<String> SUPPORTED_CAUSES = Arrays.asList("full restore");

    @Autowired
    private TerminateRequestHandler terminateRequestHandler;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private HelmChartHelper helmChartHelper;

    @Autowired
    private HealRequestServiceFactory healRequestServiceFactory;

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.HEAL;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        super.commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.HEAL);
        HealVnfRequest healVnfRequest = (HealVnfRequest) request;
        validateHealConfiguration(vnfInstance, healVnfRequest.getCause());

        Map<String, Object> additionalParams = checkAndCastObjectToMap(healVnfRequest.getAdditionalParams());
        commonAdditionalParamsValidation(additionalParams);

        HealRequestService service = healRequestServiceFactory.getServiceByParams(additionalParams);
        service.specificValidation(vnfInstance, healVnfRequest);
    }

    void validateHealConfiguration(final VnfInstance instance, final String cause) {
        JSONObject vnfd = packageService.getVnfd(instance.getVnfPackageId());
        if (vnfd != null) {
            NodeTypeUtility.validateHealCauseIsSupported(vnfd, cause);
        }

        if (cause == null || !SUPPORTED_CAUSES.contains(cause.toLowerCase())) {
            throw new InvalidInputException(
                    String.format("Cause type [%s] is not supported. Only %s is supported", cause, SUPPORTED_CAUSES));
        }
    }

    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        helmChartHelper.resetHelmChartStates(vnfInstance.getHelmCharts());
        operation.setHelmClientVersion(vnfInstance.getHelmClientVersion());
    }

    @Override
    public void sendRequest(final VnfInstance vnfInstance, final LifecycleOperation operation, final Object request,
                            final Path toValuesFile) {
        try {
            TerminateVnfRequest terminateVnfRequest = new TerminateVnfRequest();
            final HealVnfRequest healVnfRequest = (HealVnfRequest) request;

            Map<String, Object> paramsForTerminate = getAdditionalParamsForTerminate(healVnfRequest);
            terminateVnfRequest.setAdditionalParams(paramsForTerminate);

            HealRequestService service = healRequestServiceFactory.getServiceByParams(paramsForTerminate);
            terminateVnfRequest = service.prepareTerminateRequest(terminateVnfRequest, terminateRequestHandler, vnfInstance);

            terminateRequestHandler.setCleanUpResources(vnfInstance, paramsForTerminate);
            terminateRequestHandler.sendRequest(vnfInstance, operation, terminateVnfRequest, toValuesFile);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            setError(errorMessage, operation);
            updateOperationState(operation, LifecycleOperationState.FAILED);
            databaseInteractionService.persistLifecycleOperation(operation);
            databaseInteractionService.releaseNamespaceDeletion(operation);
            throw e;
        }
    }

    private static Map<String, Object> getAdditionalParamsForTerminate(HealVnfRequest healVnfRequest) {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, true);

        final Map<String, Object> healVnfRequestAdditionalParams = checkAndCastObjectToMap(healVnfRequest.getAdditionalParams());
        additionalParams.putAll(healVnfRequestAdditionalParams);

        return additionalParams;
    }

    private static void commonAdditionalParamsValidation(Map additionalParams) {
        if (parameterPresent(additionalParams, RESTORE_BACKUP_NAME) && parameterPresent(additionalParams, RESTORE_BACKUP_FILE_REFERENCE)) {
            String errorMessage = String.format("%s and %s can not be present in the same HEAL Request",
                                                RESTORE_BACKUP_NAME, RESTORE_BACKUP_FILE_REFERENCE);
            throw new InvalidHealRequestException(errorMessage);
        }
    }
}
