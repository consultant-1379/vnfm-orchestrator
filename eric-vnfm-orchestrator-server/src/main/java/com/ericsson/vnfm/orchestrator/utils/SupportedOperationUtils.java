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
package com.ericsson.vnfm.orchestrator.utils;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Optional;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;

public final class SupportedOperationUtils {
    private SupportedOperationUtils() {
    }

    public static void validateOperationIsSupported(VnfInstance vnfInstance, String operationName) {
        OperationDetail operationDetail = getOperationDetailByOperationName(vnfInstance, operationName)
                        .orElseThrow(() ->
                                new InternalRuntimeException(
                                        String.format("Cannot find supported operation %s for VNF instance %s",
                                                operationName,
                                                vnfInstance.getVnfInstanceId()
                                        )));
        if (!operationDetail.isSupported()) {
            throw new OperationNotSupportedException(
                    operationDetail.getOperationName(),
                    vnfInstance.getVnfPackageId(),
                    operationDetail.getErrorMessage());
        }
    }

    public static Optional<OperationDetail> getOperationDetailByOperationName(VnfInstance vnfInstance, String operationName) {
        return emptyIfNull(vnfInstance.getSupportedOperations())
                .stream()
                .filter(operation -> operationName.equals(operation.getOperationName()))
                .findAny();
    }

    public static boolean isOperationSupported(VnfInstance vnfInstance, String operationName) {
        Optional<OperationDetail> operationDetail = getOperationDetailByOperationName(vnfInstance, operationName);
        return operationDetail.isPresent() && operationDetail.get().isSupported();
    }

}
