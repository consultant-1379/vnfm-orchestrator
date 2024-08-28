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
import java.util.List;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter(autoApply = true)
public class OperationDetailListConverter implements AttributeConverter<List<OperationDetail>, String> {

    private static final String EMPTY_JSON_ARRAY = "[]";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<OperationDetail> operationDetails) {
        try {
            if (operationDetails != null && !operationDetails.isEmpty()) {
                return objectMapper.writeValueAsString(operationDetails);
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Can't parse operationDetails into String", e);
            return EMPTY_JSON_ARRAY;
        }
        return EMPTY_JSON_ARRAY;
    }

    @Override
    public List<OperationDetail> convertToEntityAttribute(String operationDetails) {
        try {
            if (operationDetails != null) {
                return objectMapper.readValue(operationDetails,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, OperationDetail.class));
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Can't parse operationDetails String into List", e);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
