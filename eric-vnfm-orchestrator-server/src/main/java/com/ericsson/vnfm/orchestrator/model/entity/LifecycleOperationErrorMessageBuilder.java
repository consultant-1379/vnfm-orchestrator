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
package com.ericsson.vnfm.orchestrator.model.entity;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.getProblemDetails;

import com.ericsson.vnfm.orchestrator.model.ProblemDetails;

public final class LifecycleOperationErrorMessageBuilder {

    private LifecycleOperationErrorMessageBuilder() {
    }

    public static void appendError(String errorMessage, LifecycleOperation operation) {
        if (errorMessage != null) {
            ProblemDetails existingProblemDetails = getProblemDetails(operation.getError());
            ProblemDetails newProblemDetails = getProblemDetails(errorMessage);
            if (existingProblemDetails != null) {
                String existingErrorDetail = existingProblemDetails.getDetail();
                existingProblemDetails.setDetail(existingErrorDetail.concat("\n").concat(newProblemDetails.getDetail()));
                operation.setError(convertObjToJsonString(existingProblemDetails));
            } else {
                operation.setError(convertObjToJsonString(newProblemDetails));
            }
        }
    }

    public static void setError(String errorMessage, LifecycleOperation operation) {
        if (errorMessage == null) {
            operation.setError(null);
        } else {
            ProblemDetails problemDetails = getProblemDetails(errorMessage);
            operation.setError(convertObjToJsonString(problemDetails));
        }
    }
}
