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
package com.ericsson.vnfm.orchestrator.model.notification;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;

import java.net.URI;

import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VnfLcmOperationOccurrenceNotification extends NotificationBase {
    private NotificationStatus notificationStatus;
    private LifecycleOperationState operationState;
    private LifecycleOperationType operation;
    private boolean isAutomaticInvocation;
    private NotificationVerbosity verbosity;
    private String vnfLcmOpOccId;
    private ProblemDetails error;

    public VnfLcmOperationOccurrenceNotification(LifecycleOperation lifecycleOperation) {
        super(lifecycleOperation.getVnfInstance().getVnfInstanceId(), NotificationType.VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION);
        notificationStatus = lifecycleOperation.getOperationState() == LifecycleOperationState.STARTING ?
                NotificationStatus.START : NotificationStatus.RESULT;
        operation = lifecycleOperation.getLifecycleOperationType();
        operationState = lifecycleOperation.getOperationState();
        isAutomaticInvocation = false;
        verbosity = NotificationVerbosity.SHORT;
        vnfLcmOpOccId = lifecycleOperation.getOperationOccurrenceId();
    }

    public VnfLcmOperationOccurrenceNotification withProblemDetails(String title, String detail, Integer httpStatus, String vnfmHost) {
        error = new ProblemDetails()
                .title(title)
                .type(URI.create(TYPE_BLANK))
                .detail(detail)
                .instance(URI.create("https://" + vnfmHost + LCM_VNF_INSTANCES + getVnfInstanceId()))
                .status(httpStatus);
        return this;
    }
}
