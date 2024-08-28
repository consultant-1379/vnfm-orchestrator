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

import com.ericsson.am.shared.retries.TransactionalAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfig {

    @Value("${txRetry.attempts}")
    private Integer attempts;

    @Value("${txRetry.delayInSeconds}")
    private Integer delayInSeconds;

    @Bean
    public TransactionalAspect transactionalAspect() {
        return new TransactionalAspect(attempts, delayInSeconds);
    }
}
