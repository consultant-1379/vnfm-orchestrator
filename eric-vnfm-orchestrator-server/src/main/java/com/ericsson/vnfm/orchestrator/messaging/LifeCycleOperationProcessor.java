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

/**
 * Specific interface for the LCM operations related messages from MB.
 */
public interface LifeCycleOperationProcessor<M> extends MessageProcessor<M> {

    /**
     * Processes message for operation when rollback required.
     *
     * @param message the message from MB payload
     */
    void rollBack(M message);
}
