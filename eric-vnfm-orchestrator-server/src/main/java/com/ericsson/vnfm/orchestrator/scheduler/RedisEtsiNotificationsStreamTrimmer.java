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
package com.ericsson.vnfm.orchestrator.scheduler;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ETSI_NOTIFICATIONS_STREAM_MAX_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ETSI_STREAM_KEY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "notifications.enabled")
public class RedisEtsiNotificationsStreamTrimmer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Scheduled(cron = "0 0,30 * * * *")
    public void trimStream() {
        LOGGER.debug("Trimming redis ETSI notifications stream");
        Long trimmed = redisTemplate.opsForStream().trim(ETSI_STREAM_KEY, ETSI_NOTIFICATIONS_STREAM_MAX_SIZE, true);
        LOGGER.debug("Trimmed {} ETSI notifications entries", trimmed != null ? trimmed : 0);
    }
}
