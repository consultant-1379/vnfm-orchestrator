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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.LOCAL_WORKING_QUEUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.WORKING_QUEUE_TIMEOUTS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecoveryLifeCycleOperationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations hashOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RecoveryLifeCycleOperationService recoveryLifeCycleOperationService;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(recoveryLifeCycleOperationService, "operationEventValidTimeMinutes",  3);
    }

    @Test
    public void searchLostMessageTest() {
        // given
        Map<String, String> messagesWithTimeout = Map.of("key1", "123", "key2", "123");

        List<String> queue = Arrays.asList("key1", "key2", "key3");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(hashOperations.entries(WORKING_QUEUE_TIMEOUTS)).thenReturn(messagesWithTimeout);
        when(listOperations.range(LOCAL_WORKING_QUEUE, 0, -1)).thenReturn(queue);

        // when
        recoveryLifeCycleOperationService.searchLostMessage();

        // then
        verify(hashOperations, times(1)).put(eq(WORKING_QUEUE_TIMEOUTS), anyString(), anyString());
        verify(hashOperations).put(eq(WORKING_QUEUE_TIMEOUTS), eq("key3"), anyString());
    }
}
