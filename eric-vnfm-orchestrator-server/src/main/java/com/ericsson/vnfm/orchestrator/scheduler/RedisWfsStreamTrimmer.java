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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WFS_STREAM_MAX_SIZE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "redis.listener.enabled")
public class RedisWfsStreamTrimmer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Scheduled(cron = "@hourly")
    public void trimStream() {
        LOGGER.info("Trimming redis wfs stream");
        Long trimmed = redisTemplate.opsForStream().trim(WFS_STREAM_KEY, WFS_STREAM_MAX_SIZE, true);
        LOGGER.info("Trimmed {} wfs entries", trimmed != null ? trimmed : 0);
    }
}
