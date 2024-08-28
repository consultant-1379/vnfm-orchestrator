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
package com.ericsson.vnfm.orchestrator.messaging;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import lombok.Getter;
import lombok.Setter;

/**
 * This POJO's purpose is to keep message handling variables in order to transfer them across different message handlers.
 * Feel free to add any additional fields required for message handling by any flow.
 */
@Getter
public class MessageHandlingContext<M> {

    @Setter
    private M message;
    @Setter
    private LifecycleOperation operation;
    @Setter
    private VnfInstance vnfInstance;

    public MessageHandlingContext(M message) {
        this.message = message;
    }
}
