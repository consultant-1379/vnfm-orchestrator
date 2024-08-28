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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Keycloack.NO_TOKEN_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Keycloack.TOKEN_PARSE_ERROR;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.keycloack.JwtPayloadResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtDecoder {

    private final ObjectMapper mapper;

    @Autowired
    public JwtDecoder(
            final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String extractUsername(final HttpServletRequest request) {
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        return decodeJwtToken(jwtToken).getPreferredUsername();
    }

    public List<String> extractUserRoles(final HttpServletRequest request) {
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        var realmAccess = decodeJwtToken(jwtToken).getRealmAccess();
        if (realmAccess != null) {
            return realmAccess.getRoles() == null ? Collections.emptyList() : realmAccess.getRoles();
        }
        return Collections.emptyList();
    }

    private JwtPayloadResponseDto decodeJwtToken(String jwtToken) {
        if (jwtToken == null) {
            throw new InternalRuntimeException(NO_TOKEN_ERROR);
        }
        try {
            var payload = jwtToken.split("\\.")[1];
            var decodedBytes = Base64.getUrlDecoder().decode(payload);
            return mapper.readValue(decodedBytes, JwtPayloadResponseDto.class);
        } catch (RuntimeException | IOException e) {
            throw new InternalRuntimeException(format(TOKEN_PARSE_ERROR, e.getMessage()), e);
        }
    }
}