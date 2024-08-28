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

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.DefaultClientResources;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${redis.acl.enabled}")
    private boolean isRedisACLEnabled;

    @Bean
    @ConditionalOnProperty(name = "redis.cluster.enabled")
    public LettuceClientConfiguration clientConfiguration() {

        DefaultClientResources clientResources = DefaultClientResources.create();

        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofMillis(10000L))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(refreshOptions)
                .build();

        return LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clientOptions)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "redis.cluster.enabled")
    public RedisConnectionFactory connectionFactory(final LettuceClientConfiguration clientConfiguration) {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.addClusterNode(new RedisServer(redisHost, redisPort));
        if (isRedisACLEnabled) {
            clusterConfiguration.setUsername(redisUsername);
            clusterConfiguration.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
    }

    @Bean
    public RedisHealthIndicator redisHealthIndicator(final RedisConnectionFactory connectionFactory) {
        return new RedisHealthIndicator(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            final RedisConnectionFactory connectionFactory, final ObjectMapper objectMapper) {
        return buildRedisTemplate(connectionFactory, String.class, objectMapper);
    }

    private <V> RedisTemplate<String, V> buildRedisTemplate(final RedisConnectionFactory connectionFactory,
                                                            Class<V> valueType, ObjectMapper objectMapper) {
        RedisTemplate<String, V> redisTemplate = new RedisTemplate<>();
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        if (String.class.equals(valueType)) {
            redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        } else {
            Jackson2JsonRedisSerializer<V> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, valueType);
            redisTemplate.setValueSerializer(valueSerializer);
        }
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
