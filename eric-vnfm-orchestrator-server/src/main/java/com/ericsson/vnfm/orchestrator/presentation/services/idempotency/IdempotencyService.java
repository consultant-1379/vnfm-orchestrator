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

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;

public interface IdempotencyService {

    <T> ResponseEntity<T> executeTransactionalIdempotentCall(Supplier<ResponseEntity<T>> supplier, String idempotencyKey);

    void updateResponseWithProcessedData(HttpServletResponse response, RequestProcessingDetails requestProcessingDetails);

    RequestProcessingDetails getRequestProcessingDetails(String idempotencyKey);

    <T> void saveIdempotentResponse(String idempotencyKey, ResponseEntity<T> response);

    void createProcessingRequest(String idempotencyKey, String requestHash, Integer retryAfter);

    void updateProcessingRequestCreationTime(RequestProcessingDetails requestProcessingDetails);
}
