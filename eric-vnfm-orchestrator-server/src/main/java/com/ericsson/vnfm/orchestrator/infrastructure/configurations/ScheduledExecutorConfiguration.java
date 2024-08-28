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

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScheduledExecutorConfiguration {
    @Bean
    public ScheduledThreadPoolExecutor  taskExecutor() {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(10);
        pool.setRemoveOnCancelPolicy(true);
        return pool;
    }
}