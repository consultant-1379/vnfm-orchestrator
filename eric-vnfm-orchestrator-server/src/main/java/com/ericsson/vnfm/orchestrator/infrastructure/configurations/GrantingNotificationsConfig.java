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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@RefreshScope
public class GrantingNotificationsConfig {
    private boolean isGrantSupported;
    private GrantingNotificationsEndpointsConfig grantingNotificationsEndpointsConfig;

    public GrantingNotificationsConfig(@Value("${isGrantSupported:false}") final boolean isGrantSupported,
                                       final GrantingNotificationsEndpointsConfig grantingNotificationsEndpointsConfig) {
        this.isGrantSupported = isGrantSupported;
        this.grantingNotificationsEndpointsConfig = grantingNotificationsEndpointsConfig;
    }
}
