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
package com.ericsson.vnfm.orchestrator.presentation.controllers.filter;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import com.ericsson.vnfm.orchestrator.infrastructure.model.IdempotencyProps;
import com.ericsson.vnfm.orchestrator.model.ProcessingState;
import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.cachewrapper.CachedBodyRequestWrapper;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.cachewrapper.CachedMultipartRequestWrapper;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
public class IdempotencyFilter extends OncePerRequestFilter {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyProps idempotencyProps;

    @Autowired
    private IdempotencyContext idempotencyContext;

    private static final List<String> IDEMPOTENT_METHODS = Arrays.asList("POST", "DELETE");
    private static final Integer DEFAULT_RETRY_AFTER = 30;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest currentRequest = mapRequest(httpServletRequest);

        String idempotencyKey = httpServletRequest.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null) {
            LOGGER.error("Idempotency key isn't presented as header value for request {}", currentRequest.getRequestURI());
            httpServletResponse.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value());
            httpServletResponse.flushBuffer();
            return;
        }

        String hash;
        try {
            hash = calculateRequestHash(currentRequest);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalRuntimeException(
                    String.format("Failed to generate digest for %s due to %s", httpServletRequest.getRequestURI(), e.getMessage()), e);
        }

        RequestProcessingDetails requestProcessingDetails = idempotencyService.getRequestProcessingDetails(idempotencyKey);

        if (requestProcessingDetails == null) {
            Integer retryAfter = calculateRetryAfterValue(currentRequest.getRequestURI(), currentRequest.getMethod());
            idempotencyService.createProcessingRequest(idempotencyKey, hash, retryAfter);
            idempotencyContext.setIdempotencyId(idempotencyKey);
            try {
                filterChain.doFilter(currentRequest, httpServletResponse);
            } finally {
                idempotencyContext.clear();
            }
        } else {
            proceedWithExistedRequestDetails(currentRequest, httpServletResponse, filterChain, requestProcessingDetails, hash);
        }
    }

    private static HttpServletRequest mapRequest(final HttpServletRequest httpServletRequest) throws IOException {
        if (WebUtils.getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class) == null) {
            if (httpServletRequest.getContentType() != null &&
                    httpServletRequest.getContentType().contains(ContentType.MULTIPART_FORM_DATA.getMimeType())) {
                return new CachedMultipartRequestWrapper(httpServletRequest);
            } else {
                return new CachedBodyRequestWrapper(httpServletRequest);
            }
        }
        return httpServletRequest;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !IDEMPOTENT_METHODS.contains(request.getMethod());
    }

    private void proceedWithExistedRequestDetails(final HttpServletRequest httpServletRequest,
                                                  final HttpServletResponse httpServletResponse,
                                                  final FilterChain filterChain,
                                                  final RequestProcessingDetails requestProcessingDetails,
                                                  final String hash) throws IOException, ServletException {
        if (requestProcessingDetails.getRequestHash().equals(hash)) {
            if (requestProcessingDetails.getProcessingState() == ProcessingState.STARTED) {
                proceedWithStartedRequest(httpServletRequest, httpServletResponse, filterChain, requestProcessingDetails);
            } else {
                idempotencyService.updateResponseWithProcessedData(httpServletResponse, requestProcessingDetails);
            }
        } else {
            LOGGER.error("Request with the same idempotency key, but with different hash already exist for {}", httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }

    private void proceedWithStartedRequest(final HttpServletRequest httpServletRequest,
                                           final HttpServletResponse httpServletResponse,
                                           final FilterChain filterChain,
                                           final RequestProcessingDetails requestProcessingDetails) throws IOException, ServletException {
        LocalDateTime creationTime = requestProcessingDetails.getCreationTime();
        if (LocalDateTime.now().isAfter(creationTime.plusSeconds(2L * requestProcessingDetails.getRetryAfter()))) {
            idempotencyService.updateProcessingRequestCreationTime(requestProcessingDetails);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            LOGGER.info("Request {} processing still in progress", httpServletRequest.getRequestURI());
            httpServletResponse.setStatus(HttpStatus.TOO_EARLY.value());
            httpServletResponse.addHeader(HttpHeaders.RETRY_AFTER, requestProcessingDetails.getRetryAfter().toString());
        }
    }

    private static String calculateRequestHash(final HttpServletRequest httpServletRequest) throws IOException, NoSuchAlgorithmException {

        MultipartHttpServletRequest multipartRequest = WebUtils
                .getNativeRequest(httpServletRequest, MultipartHttpServletRequest.class);

        if (multipartRequest != null) {
            return calculateMultipartRequestHash(multipartRequest);
        } else {
            return calculateInputStreamRequestHash(httpServletRequest);
        }
    }

    private static String calculateInputStreamRequestHash(final HttpServletRequest httpServletRequest) throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);

        digest.update(httpServletRequest.getRequestURI().getBytes());
        digest.update(httpServletRequest.getMethod().getBytes());
        digest.update(IOUtils.toByteArray(httpServletRequest.getInputStream()));

        byte[] hash = digest.digest();
        String stringHash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        LOGGER.debug("Hash {} has been calculated for request {}", hash, httpServletRequest.getRequestURI());
        return stringHash;
    }

    private static String calculateMultipartRequestHash(final MultipartHttpServletRequest multipartRequest) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);

        digest.update(multipartRequest.getRequestURI().getBytes());
        digest.update(multipartRequest.getMethod().getBytes());

        Map<String, String[]> parameterMap = new TreeMap<>(multipartRequest.getParameterMap());
        parameterMap.forEach((key, value1) -> {
            digest.update(key.getBytes());
            Arrays.stream(value1).map(String::getBytes).forEach(digest::update);
        });

        multipartRequest.getMultiFileMap().forEach((key, value) -> {
            digest.update(key.getBytes());
            value.forEach(file -> {
                try {
                    digest.update(file.getBytes());
                } catch (IOException e) {
                    LOGGER.warn("Multipart file {} content cannot be parsed", key, e);
                }
            });
        });

        byte[] hash = digest.digest();
        String stringHash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        LOGGER.debug("Hash {} has been calculated for request {}", hash, multipartRequest.getRequestURI());
        return stringHash;
    }

    private Integer calculateRetryAfterValue(String requestUri, String method) {
        Integer requestLatency = idempotencyProps.findEndpointLatency(requestUri, method);

        return requestLatency != null ? requestLatency : DEFAULT_RETRY_AFTER;
    }
}
