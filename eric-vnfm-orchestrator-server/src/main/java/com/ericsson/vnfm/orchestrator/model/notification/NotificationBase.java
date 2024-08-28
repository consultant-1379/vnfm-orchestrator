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

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationBase {

    @JsonProperty("id")
    private final UUID id = UUID.randomUUID();

    @JsonProperty("timeStamp")
    private final ZonedDateTime timeStamp = ZonedDateTime.now();

    @JsonProperty("notificationType")
    private String notificationType;

    @JsonProperty("vnfInstanceId")
    private String vnfInstanceId;

    @JsonProperty("_links")
    private LccnLinks links;

    public NotificationBase(NotificationType notificationType, String vnfInstanceId, LccnLinks links) {
        this.notificationType = notificationType.toString();
        this.vnfInstanceId = vnfInstanceId;
        this.links = links;
    }

    public NotificationBase(String vnfInstanceId, NotificationType notificationType) {
        this.links = new LccnLinks();
        this.vnfInstanceId = vnfInstanceId;
        this.notificationType = notificationType.toString();
    }
}