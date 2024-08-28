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
package com.ericsson.vnfm.orchestrator.presentation.exceptions;

public class RunningLcmOperationsAmountExceededException extends RuntimeException {
    private static final long serialVersionUID = -1141222316648672131L;

    private static final String RUNNING_LCM_OPERATIONS_AMOUNT_EXCEEDED_EXCEPTION_MESSAGE =
            "Operation cannot be created due to reached global limit of concurrent LCM operations: %d";

    public RunningLcmOperationsAmountExceededException(int limit) {
        super(String.format(RUNNING_LCM_OPERATIONS_AMOUNT_EXCEEDED_EXCEPTION_MESSAGE, limit));
    }
}
