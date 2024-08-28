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
package com.ericsson.vnfm.orchestrator.utils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractRedisSetupTest extends AbstractDbSetupTest {

    private final static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/redis:6.2.13-alpine")
                    .asCompatibleSubstituteFor("redis"))
                    .withExposedPorts(6379);

    static {
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getContainerIpAddress());
        System.setProperty("spring.data.redis.port", redisContainer.getFirstMappedPort().toString());
    }
}
