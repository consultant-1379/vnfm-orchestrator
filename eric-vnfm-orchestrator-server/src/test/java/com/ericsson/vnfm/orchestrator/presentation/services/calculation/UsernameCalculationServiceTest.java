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
package com.ericsson.vnfm.orchestrator.presentation.services.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Keycloack.NO_TOKEN_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EOCM_USERNAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_USERNAME_KEY;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

@ExtendWith(MockitoExtension.class)
public class UsernameCalculationServiceTest {

    private static final String USERNAME = "EVNFM";

    @Mock
    private JwtDecoder JwtDecoder;

    @Mock
    private HttpServletRequest httpServletRequest;

    private UsernameCalculationService unit;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }

    @Test
    public void testCalculateUsernameShouldReturnPredefinedUsernameForEocmUser() {
        // given
        unit = new UsernameCalculationService(JwtDecoder, "false");

        // when
        var username = unit.calculateUsername();

        // then
        assertEquals(EOCM_USERNAME, username);
    }

    @Test
    public void testCalculateUsernameShouldReturnUsernameFromCookieForEvnfmUser() {
        // given
        unit = new UsernameCalculationService(JwtDecoder, "true");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[] { new Cookie(EVNFM_USERNAME_KEY, USERNAME) });

        // when
        var actualUsername = unit.calculateUsername();

        // then
        assertEquals(USERNAME, actualUsername);
    }

    @Test
    public void testCalculateUsernameShouldReturnUsernameFromKeyclockForOtherUser() {
        // given
        unit = new UsernameCalculationService(JwtDecoder, "true");
        when(JwtDecoder.extractUsername(httpServletRequest)).thenReturn(USERNAME);

        // when
        var actualUsername = unit.calculateUsername();

        // then
        assertEquals(USERNAME, actualUsername);
    }

    @Test
    public void testCalculateUsernameShouldThrowErrorOnKeyclockError() {
        // given
        unit = new UsernameCalculationService(JwtDecoder, "true");
        when(JwtDecoder.extractUsername(httpServletRequest)).thenThrow(new InternalRuntimeException(NO_TOKEN_ERROR));

        // when and then
        var exception = assertThrows(InternalRuntimeException.class, () -> unit.calculateUsername());
        assertEquals(NO_TOKEN_ERROR, exception.getMessage());
    }
}
