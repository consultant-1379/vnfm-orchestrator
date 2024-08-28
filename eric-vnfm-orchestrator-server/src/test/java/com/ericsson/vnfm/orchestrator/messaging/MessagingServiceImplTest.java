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
package com.ericsson.vnfm.orchestrator.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.ObjectMapperConfig;
import com.ericsson.vnfm.orchestrator.model.notification.OperationState;
import com.ericsson.vnfm.orchestrator.model.notification.TracingContextInjectorService;
import com.ericsson.vnfm.orchestrator.model.notification.VnfIdentifierCreationNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = { MessagingServiceImpl.class, ObjectMapperConfig.class })
@TestPropertySource(properties = { "notifications.enabled=true" })
public class MessagingServiceImplTest {

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private TracingContextInjectorService tracingContextInjectorService;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @MockBean
    @Qualifier("redisRetryTemplate")
    private RetryTemplate retryTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MessagingServiceImpl messagingService;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullySendMessage() throws JsonProcessingException {
        // given
        final var message = new VnfIdentifierCreationNotification("instance-id-1", OperationState.COMPLETED);

        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        given(retryTemplate.execute(any())).willAnswer(invocation -> ((RetryCallback<?, ?>) invocation.getArgument(0)).doWithRetry(null));

        // when
        assertThatNoException().isThrownBy(() -> messagingService.sendMessage(message));

        // then
        final ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());
        final var mapRecord = captor.getValue();
        assertThat(mapRecord).isInstanceOf(StringRecord.class);
        assertThat(mapRecord.getStream()).isEqualTo("Global:Streams:etsi-notification-events");
        assertThat(mapRecord.getValue().size()).isEqualTo(2);
        assertThat(mapRecord.getValue().get("payload")).isEqualTo( mapper.writeValueAsString(message));
    }

    @Test
    public void shouldTolerateRedisConnectionFailureException() {
        // given
        final var message = new VnfIdentifierCreationNotification("instance-id-1", OperationState.COMPLETED);

        given(redisTemplate.opsForStream()).willThrow(new RedisConnectionFailureException("Error occurred"));
        given(retryTemplate.execute(any())).willAnswer(invocation -> ((RetryCallback<?, ?>) invocation.getArgument(0)).doWithRetry(null));

        // when and then
        assertThatNoException().isThrownBy(() -> messagingService.sendMessage(message));
    }

    @Test
    public void shouldTolerateGenericException() {
        // given
        final var message = new VnfIdentifierCreationNotification("instance-id-1", OperationState.COMPLETED);

        given(retryTemplate.execute(any())).willThrow(new RuntimeException("Error occurred"));

        // when and then
        assertThatNoException().isThrownBy(() -> messagingService.sendMessage(message));
    }
}
