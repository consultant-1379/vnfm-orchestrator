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

import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_INSTANCE_IS_NOT_INSTANTIATED;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public final class NotInstantiatedException extends RuntimeException {

    public NotInstantiatedException(final VnfInstance vnfInstance) {
        super(String.format(VNF_INSTANCE_IS_NOT_INSTANTIATED, vnfInstance.getVnfInstanceId()));
    }
}
