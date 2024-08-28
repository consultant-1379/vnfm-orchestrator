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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.ProcessingState;
import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public <T> ResponseEntity<T> executeTransactionalIdempotentCall(final Supplier<ResponseEntity<T>> supplier, String idempotencyKey) {
        ResponseEntity<T> responseEntity = supplier.get();

        saveIdempotentResponse(idempotencyKey, responseEntity);

        return responseEntity;
    }

    @Override
    public void updateResponseWithProcessedData(HttpServletResponse response, RequestProcessingDetails requestProcessingDetails) {
        try {
            Map<String, List<String>> jsonHeadersMap = objectMapper
                    .readValue(requestProcessingDetails.getResponseHeaders(), new TypeReference<>() { });

            String body = requestProcessingDetails.getResponseBody();

            jsonHeadersMap.forEach((headerName, headerValues) -> headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue)));
            response.setStatus(requestProcessingDetails.getResponseCode());

            if (!body.isBlank()) {
                response.setContentType(ContentType.APPLICATION_JSON.toString());
                response.getWriter().write(body);
            }
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Response can not be read %s", e.getMessage()), e);
        }
    }

    @Override
    public RequestProcessingDetails getRequestProcessingDetails(final String idempotencyKey) {
        Optional<RequestProcessingDetails> requestProcessingDetails = requestProcessingDetailsRepository
                .findById(idempotencyKey);
        return requestProcessingDetails.orElse(null);
    }

    @Override
    public <T> void saveIdempotentResponse(final String idempotencyKey, final ResponseEntity<T> response) {
        Optional<RequestProcessingDetails> requestProcessingDetailsOptional = requestProcessingDetailsRepository
                .findById(idempotencyKey);

        requestProcessingDetailsOptional.ifPresentOrElse(requestProcessingDetails -> {
            try {
                requestProcessingDetails.setResponseHeaders(objectMapper.writeValueAsString(response.getHeaders()));
                requestProcessingDetails.setResponseCode(response.getStatusCode().value());
                requestProcessingDetails.setResponseBody(objectMapper.writeValueAsString(response.getBody()));
                requestProcessingDetails.setProcessingState(ProcessingState.FINISHED);

                requestProcessingDetailsRepository.save(requestProcessingDetails);
            } catch (JsonProcessingException e) {
                throw new InternalRuntimeException(String.format("Response can not be saved %s", e.getMessage()), e);
            }
        }, () -> LOGGER.warn("RequestProcessingDetails not found, id = {}", idempotencyKey));
    }

    @Override
    public void createProcessingRequest(final String idempotencyKey, final String requestHash, final Integer retryAfter) {
        RequestProcessingDetails requestProcessingDetails = new RequestProcessingDetails();
        requestProcessingDetails.setId(idempotencyKey);
        requestProcessingDetails.setRequestHash(requestHash);
        requestProcessingDetails.setProcessingState(ProcessingState.STARTED);
        requestProcessingDetails.setRetryAfter(retryAfter);
        requestProcessingDetails.setCreationTime(LocalDateTime.now());

        requestProcessingDetailsRepository.save(requestProcessingDetails);
    }

    @Override
    public void updateProcessingRequestCreationTime(final RequestProcessingDetails requestProcessingDetails) {
        requestProcessingDetails.setCreationTime(LocalDateTime.now());
        requestProcessingDetailsRepository.save(requestProcessingDetails);
    }
}
