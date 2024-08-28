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
package com.ericsson.vnfm.orchestrator.model.converter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class OperationChartsPriorityConverter implements AttributeConverter<Map<LifecycleOperationType, Integer>, String> {

    private static final String EMPTY_STRING = "";

    @Autowired
    private ObjectMapper mapper;

    @Override
    public String convertToDatabaseColumn(final Map<LifecycleOperationType, Integer> chartsPriorityMap) {
        if (chartsPriorityMap == null || chartsPriorityMap.isEmpty()) {
            return EMPTY_STRING;
        }

        try {
            return mapper.writeValueAsString(chartsPriorityMap);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Can't parse chartsPriorityMap into String", e);
            return EMPTY_STRING;
        }
    }

    @Override
    public Map<LifecycleOperationType, Integer> convertToEntityAttribute(final String chartsPriority) {
        if (chartsPriority == null || chartsPriority.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return mapper.readValue(chartsPriority,
                                    mapper.getTypeFactory().constructMapType(EnumMap.class, LifecycleOperationType.class, Integer.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Can't parse chartsPriority string into Map", e);
            return Collections.emptyMap();
        }
    }
}
