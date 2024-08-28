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
package com.ericsson.vnfm.orchestrator.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.logging.InMemoryAppender;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        IdempotencyAspect.class,
        IdempotencyService.class,
        RequestProcessingDetailsRepository.class,
        ObjectMapper.class, })
public class IdempotencyAspectTest {

    @Autowired
    private IdempotencyAspect idempotencyAspect;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @MockBean
    ObjectMapper objectMapper;

    static private InMemoryAppender inMemoryAppender;
    static private final Logger LOGGER = (Logger) LoggerFactory.getLogger("com.ericsson.vnfm.orchestrator");

    final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

    final ResponseEntity<Object> response = new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT);

    @BeforeAll
    static public void init() {
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        LOGGER.addAppender(inMemoryAppender);
        LOGGER.setLevel(Level.INFO);
        inMemoryAppender.start();
    }

    @AfterEach
    public void reset() {
        inMemoryAppender.reset();
    }

    @Test
    public void testIdempotencyAspectIdempotencyKeyFail() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(response).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.setRequestURI("dummy");
            utilities.when(HttpUtility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains(String.format("Idempotency key is not present for call to %s", "dummy"), Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectServletRequestIsNullFail() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(response).when(pjp).proceed();
            
            final MockHttpServletRequest servletRequest = null;
            utilities.when(HttpUtility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains("Unable to get incoming HttpServletRequest", Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectTrowIllegalStateExceptionFail() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(response).when(pjp).proceed();
            
            utilities.when(HttpUtility::getCurrentHttpRequest).thenThrow(new IllegalStateException("dummy"));
            idempotencyAspect.around(pjp);

            assertThat(inMemoryAppender.contains("dummy", Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectSignatureIsNullFail() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(new Object()).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(IDEMPOTENCY_KEY_HEADER, "dummyKey");
            servletRequest.setRequestURI("dummyURI");

            utilities.when(HttpUtility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            assertThat(inMemoryAppender.contains("Cannot invoke \"org.aspectj.lang.reflect.MethodSignature.getMethod()\" because \"signature\" is null",
                                                 Level.WARN)).isTrue();
            verify(idempotencyService, times(0)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectSuccessful() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(response).when(pjp).proceed();

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(IDEMPOTENCY_KEY_HEADER, "dummyKey");
            utilities.when(HttpUtility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            verify(idempotencyService, times(1)).saveIdempotentResponse(any(), any());
        }
    }

    @Test
    public void testIdempotencyAspectCheckInternalRuntimeExceptionFail() throws Throwable {
        try (MockedStatic<HttpUtility> utilities = mockStatic(HttpUtility.class)) {
            doReturn(response).when(pjp).proceed();

            String idempotencyKey = "dummyKey";
            doThrow(new InternalRuntimeException("")).when(idempotencyService).saveIdempotentResponse(eq(idempotencyKey), any());

            final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.addHeader(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
            utilities.when(HttpUtility::getCurrentHttpRequest).thenReturn(servletRequest);
            idempotencyAspect.around(pjp);
            verify(idempotencyService, times(1)).saveIdempotentResponse(any(), any());

            assertThat(inMemoryAppender.contains(String.format("The request processing details was not updated for idempotencyKey = %s",
                                                               idempotencyKey),
                                                 Level.ERROR)).isTrue();
        }
    }
}
