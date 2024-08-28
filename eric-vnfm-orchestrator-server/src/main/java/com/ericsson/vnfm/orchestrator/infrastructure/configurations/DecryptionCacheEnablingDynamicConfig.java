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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.config.DecryptionCacheEnablingConfig;

@Component
@RefreshScope
@Primary
public class DecryptionCacheEnablingDynamicConfig implements DecryptionCacheEnablingConfig {
    @Value("${crypto.cache.enabled:false}")
    private boolean isDecryptionCacheEnabled;

    @Override
    public boolean isDecryptionCacheEnabled() {
        return isDecryptionCacheEnabled;
    }

    public void setDecryptionCacheEnabled(boolean decryptionCacheEnabled) {
        this.isDecryptionCacheEnabled = decryptionCacheEnabled;
    }
}
