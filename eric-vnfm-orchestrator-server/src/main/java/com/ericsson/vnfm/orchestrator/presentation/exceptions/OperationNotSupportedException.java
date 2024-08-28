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


import org.apache.commons.lang3.StringUtils;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class OperationNotSupportedException extends RuntimeException {
    private static final long serialVersionUID = 3214667348570830025L;

    public OperationNotSupportedException(String operationName, String packageId, String causeMessage) {
        super(getSupportedOperationErrorMessage(operationName, packageId, causeMessage));
    }

    public static String getSupportedOperationErrorMessage(String operationName, String packageId, String causeMessage) {
        if (StringUtils.isNotBlank(causeMessage)) {
            return String.format("Operation %s is not supported for package %s due to cause: %s", operationName, packageId, causeMessage);
        } else {
            return String.format("Operation %s is not supported for package %s", operationName, packageId);
        }
    }
}
