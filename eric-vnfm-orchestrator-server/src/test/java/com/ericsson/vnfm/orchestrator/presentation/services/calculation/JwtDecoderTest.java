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

import static java.lang.String.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Keycloack.NO_TOKEN_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Keycloack.TOKEN_PARSE_ERROR;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class JwtDecoderTest {

    private final static String VALID_BEARER_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDQy1XNEZ1cWdCSzJvQ0pEQ3JZaFBDNWRXdmhteENTemcySjZsdkxsMF9VIn0"
                    +
                    ".eyJleHAiOjE2NjYzMzY3NjQsImlhdCI6MTY2NjMzNjQ2NCwianRpIjoiOWJhMWEwNTktMGE3NC00ZTkxLWIzM2QtNzVmZTk5NTBjMTNiIiwiaXNzIjoiaHR0cHM6Ly9pYW0tc2VydmljZS5la2hhdml0LmhhYmVyMDAyLWljY3IuZXdzLmdpYy5lcmljc3Nvbi5zZS9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibWFzdGVyLXJlYWxtIiwiYWNjb3VudCJdLCJzdWIiOiJlY2ViNzNhMy0wZmQ3LTQ1MTktYWE0ZC0zNzM3MWE3OThkZmQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlbyIsInNlc3Npb25fc3RhdGUiOiJhYzM5MmY1Yy01ODMxLTQzODktODVlYi1mMjc0NzJmMGEwNGMiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJVc2VyQWRtaW4iLCJvZmZsaW5lX2FjY2VzcyIsIlZNIFZORk0gV0ZTIiwiRS1WTkZNIFVJIFVzZXIgUm9sZSIsIkUtVk5GTSBTdXBlciBVc2VyIFJvbGUiLCJ1bWFfYXV0aG9yaXphdGlvbiIsIkdBU19Vc2VyIiwiVk0gVk5GTSBWSUVXIFdGUyIsIlN5c3RlbV9TZWN1cml0eV9DZXJ0TUFkbWluIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbIm1hbmFnZS1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJtYW5hZ2UtdXNlcnMiLCJtYW5hZ2UtY2xpZW50cyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwic2lkIjoiYWMzOTJmNWMtNTgzMS00Mzg5LTg1ZWItZjI3NDcyZjBhMDRjIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ2bmZtIn0.XA3v0Lit2SdPc0UzmxI1uvtsRGatM_5ZHv7FQRQ_WkTiqSqFO2g5XrAdXMElhn5d6xqEj6FurggnRwoH1nqkWT1RWC8vG-JHFsnHFpyPMTS_5fiCp7hlJpvw2ARRF4bDkhjkO60I3KddbH5FyiGG_tkd8bONx9XW3bMpyVmIgloUhdWLNzMktrKQeTspVvtVIpnAthr-Db3V8pAosAQdZURP19Nh0Chqytj92_xyBNoqnRfb6woy9KgfriTVzxER6xns2vWZFNYtgDumIUJAWsO3twRT8C3bYhuORE94ln2FGn0yla-ADq2sHVThTt8zJfKDTVQXJBRAnx7pvJ7wmA";

    private final static String INVALID_BEARER_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDQy1XNEZ1cWdCSzJvQ0pEQ3JZaF";
    @Mock
    private HttpServletRequest httpServletRequest;

    private JwtDecoder unit;

    @BeforeEach
    public void setUp() {
        unit = new JwtDecoder(new ObjectMapper());
    }

    @Test
    public void testExtractUsernameShouldReturnExtractedUsernameFromRequestHeader() {
        // given
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_BEARER_TOKEN);

        // when
        var extractUsername = unit.extractUsername(httpServletRequest);

        // then
        assertEquals("vnfm", extractUsername);
    }

    @Test
    public void testExtractUserRolesShouldReturnExtractedUserRolesFromRequestHeader() {
        // given
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(VALID_BEARER_TOKEN);

        // when
        var extractUserRoles = unit.extractUserRoles(httpServletRequest);
        List<String> expectedUserRoles = List.of("default-roles-master", "UserAdmin", "offline_access",
                                                 "VM VNFM WFS", "E-VNFM UI User Role", "E-VNFM Super User Role",
                                                 "uma_authorization", "GAS_User", "VM VNFM VIEW WFS",
                                                 "System_Security_CertMAdmin");
        // then
        assertEquals(expectedUserRoles, extractUserRoles);
    }

    @Test
    public void testExtractUsernameShouldThrowErrorWhenWrongAccessTokenInResponse() {
        // given
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(INVALID_BEARER_TOKEN);

        // when and then
        var exception = assertThrows(InternalRuntimeException.class, () -> unit.extractUsername(httpServletRequest));
        var expectedErrorMessage = format(TOKEN_PARSE_ERROR, "Index 1 out of bounds for length 1");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void testExtractUserRolesShouldThrowErrorWhenWrongAccessTokenInResponse() {
        // given
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(INVALID_BEARER_TOKEN);

        // when and then
        var exception = assertThrows(InternalRuntimeException.class, () -> unit.extractUserRoles(httpServletRequest));
        var expectedErrorMessage = format(TOKEN_PARSE_ERROR, "Index 1 out of bounds for length 1");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void testExtractUsernameShouldThrowErrorWhenNoAccessTokenInHeader() {
        var exception = assertThrows(InternalRuntimeException.class, () -> unit.extractUsername(httpServletRequest));
        assertEquals(NO_TOKEN_ERROR, exception.getMessage());
    }

    @Test
    public void testExtractUserRolesShouldThrowErrorWhenNoAccessTokenInHeader() {
        var exception = assertThrows(InternalRuntimeException.class, () -> unit.extractUserRoles(httpServletRequest));
        assertEquals(NO_TOKEN_ERROR, exception.getMessage());
    }
}
