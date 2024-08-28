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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.ericsson.am.shared.locks.LockManager;
import com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class LockManagerConfiguration {

    @Value("${instance}")
    private String instanceName;
    @Bean
    public LockManager lockManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        return new LockManager(connectionFactory, objectMapper, CommonConstants.Request.REDIS_KEY_PREFIX, instanceName);
    }
}
