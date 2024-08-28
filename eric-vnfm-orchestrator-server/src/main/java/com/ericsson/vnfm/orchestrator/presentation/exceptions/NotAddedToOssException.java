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

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import lombok.Getter;

@Getter
public final class NotAddedToOssException extends RuntimeException {

    private static final long serialVersionUID = 134778434464228490L;
    private final transient VnfInstance vnfInstance;

    public NotAddedToOssException(final VnfInstance vnfInstance, final String message) {
        super(message);
        this.vnfInstance = vnfInstance;
    }
}
