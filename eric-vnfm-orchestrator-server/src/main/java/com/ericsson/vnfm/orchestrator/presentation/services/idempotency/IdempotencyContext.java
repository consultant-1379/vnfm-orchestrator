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
package com.ericsson.vnfm.orchestrator.presentation.services.idempotency;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyContext {

    ThreadLocal<String> idempotencyIdHolder = new ThreadLocal<>();

    public void setIdempotencyId(String idempotencyId) {
        this.idempotencyIdHolder.set(idempotencyId);
    }

    public Optional<String> getIdempotencyId() {
        return Optional.ofNullable(idempotencyIdHolder.get());
    }

    public void clear() {
        idempotencyIdHolder.remove();
    }
}
