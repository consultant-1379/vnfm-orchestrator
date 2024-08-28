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

import com.ericsson.vnfm.orchestrator.model.notification.NotificationBase;

public interface MessagingService {

    /**
     * Send a message to the Redis Streams.
     *
     * @param message an object which is the message to send.
     * @param <T>     the type of the object
     */
    <T extends NotificationBase> void sendMessage(T message);
}
