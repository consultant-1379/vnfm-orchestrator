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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EOCM_USERNAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_USERNAME_KEY;

import java.util.Optional;
import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

@Service
public class UsernameCalculationService {

    private final JwtDecoder keyclockService;

    private final String smallstackApplication;

    public UsernameCalculationService(JwtDecoder keyclockService, @Value("${smallstack.application}") String smallstackApplication) {
        this.keyclockService = keyclockService;
        this.smallstackApplication = smallstackApplication;
    }

    /**
     * Calculate username for different use cases:<br>
     * <p>
     * 1. For EO-CM users (smallstak.application == false) should be used predefined username<br>
     * 2. For EVNFM users (smallstack.application == false) should be used username from cookies<br>
     * 3. For other users (smallstack.application == false && no "userName" in cookies) should be used username from Keyclock
     */
    public String calculateUsername() {
        if (!Boolean.parseBoolean(smallstackApplication)) {
            return EOCM_USERNAME;
        }
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return Optional.ofNullable(WebUtils.getCookie(request, EVNFM_USERNAME_KEY))
                .map(Cookie::getValue)
                .orElseGet(() -> keyclockService.extractUsername(request));
    }
}