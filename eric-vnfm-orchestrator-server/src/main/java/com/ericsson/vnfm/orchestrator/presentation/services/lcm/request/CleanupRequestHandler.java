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

import static java.util.regex.Pattern.compile;

import static com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc.OperationEnum.HEAL;
import static com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc.OperationEnum.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc.OperationEnum.TERMINATE;
import static com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc.OperationStateEnum.FAILED;
import static com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc.OperationStateEnum.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.DELETE_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LastOperationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ReleaseNameInUseException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;

@Component
public class CleanupRequestHandler {

    private static final Pattern RELEASE_EXISTS_ERROR_MESSAGE = compile("A resource named [\\w|-]+ already exists.*");

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private LifeCycleRequestFactory lifeCycleRequestFactory;

    @Autowired
    private VnfLcmOperationService vnfLcmOperationService;

    public void specificValidation(final VnfInstance vnfInstance, final CleanupVnfRequest cleanupVnfRequest) {
        if (cleanupVnfRequest != null) {
            instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) cleanupVnfRequest.getAdditionalParams());
        }

        checkLastOperationType(vnfInstance);

        TerminateRequestHandler service = (TerminateRequestHandler) lifeCycleRequestFactory
                .getService(LifecycleOperationType.TERMINATE);

        service.commonValidation(vnfInstance, INSTANTIATED, LCMOperationsEnum.TERMINATE);
    }

    public TerminateVnfRequest createTerminateRequest(final CleanupVnfRequest cleanupVnfRequest) {
        Map<String, Object> cleanupAdditionalParams = getAdditionalParams(cleanupVnfRequest);
        cleanupAdditionalParams.put(DELETE_IDENTIFIER, true);
        cleanupAdditionalParams.put(CLEAN_UP_RESOURCES, true);

        TerminateVnfRequest cleanUpTerminateRequest = new TerminateVnfRequest();
        cleanUpTerminateRequest.setAdditionalParams(cleanupAdditionalParams);
        cleanUpTerminateRequest.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);

        return cleanUpTerminateRequest;
    }

    private void checkLastOperationType(VnfInstance vnfInstance) {
        String operationId = vnfInstance.getOperationOccurrenceId();
        if (operationId == null) {
            throw new LastOperationException(String.format("No previous operation found for instance %s",
                                                                        vnfInstance.getVnfInstanceId()));
        }
        VnfLcmOpOcc operation = vnfLcmOperationService.getLcmOperationByOccId(operationId);
        if (isInvalidCleanupState(operation)) {
            throw new LastOperationException("Resources will not be cleaned up; " +
                    "last operation on instance was not a failed INSTANTIATE or TERMINATE");
        }
        if (operation.getError() != null && RELEASE_EXISTS_ERROR_MESSAGE.matcher(operation.getError().getDetail()).find()) {
            throw new ReleaseNameInUseException("Resources will not be cleaned up; " +
                    "instantiate failed because another release with the same name already existed");
        }
    }

    private static boolean isInvalidCleanupState(VnfLcmOpOcc operation) {
        VnfLcmOpOcc.OperationEnum lastOperationType = operation.getOperation();
        VnfLcmOpOcc.OperationStateEnum operationState = operation.getOperationState();
        if (lastOperationType.equals(INSTANTIATE)) {
            return !operationState.equals(FAILED) && !operationState.equals(ROLLED_BACK);
        } else if (lastOperationType.equals(TERMINATE) || lastOperationType.equals(HEAL)) {
            return !operationState.equals(FAILED);
        }
        return true;
    }

    private static Map<String, Object> getAdditionalParams(final CleanupVnfRequest cleanupVnfRequest) {
        if (cleanupVnfRequest == null) {
            return new HashMap<>();
        }

        return checkAndCastObjectToMap(cleanupVnfRequest.getAdditionalParams());
    }
}
