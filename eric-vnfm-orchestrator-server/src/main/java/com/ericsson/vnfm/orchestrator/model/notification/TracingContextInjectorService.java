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
package com.ericsson.vnfm.orchestrator.model.notification;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TracingContextInjectorService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Tracing tracing;

    public String createNewTraceContext() {
        Span span = tracing.tracer().newTrace();

        Map<String, String> tracingContextSerialized = new HashMap<>();
        TraceContext.Injector<Map<String, String>> injector = tracing.propagation().injector(Map<String, String>::put);
        injector.inject(span.context(), tracingContextSerialized);

        String tracingCtxJsonStr = null;
        try {
            tracingCtxJsonStr = mapper.writeValueAsString(tracingContextSerialized);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Serialization problem for trace context", e);
        }

        return tracingCtxJsonStr;
    }

    public void injectTracing(String tracingData) {
        Map<String, String> tracingContextSerialized;
        try {
            tracingContextSerialized = mapper.readValue(tracingData, Map.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to parse tracing context due to: {}", e.getMessage(), e);
            return;
        }
        Span span = tracing.tracer().toSpan(tracing.propagation()
                .extractor(Map<String, String>::get)
                .extract(tracingContextSerialized)
                .context());
        tracing.tracer().withSpanInScope(span);
    }
}
