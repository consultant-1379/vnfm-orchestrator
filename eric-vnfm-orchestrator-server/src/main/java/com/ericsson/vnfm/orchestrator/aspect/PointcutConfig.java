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

import org.aspectj.lang.annotation.Pointcut;

public class PointcutConfig {

    @Pointcut("@annotation(com.ericsson.vnfm.orchestrator.infrastructure.annotations.SendStateChangedNotification)")
    public void persistOperationExecution() {
      // pointcut for sending notifications
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
    public void exceptionHandler() {
        // pointcut for update idempotency processing details
    }
}
