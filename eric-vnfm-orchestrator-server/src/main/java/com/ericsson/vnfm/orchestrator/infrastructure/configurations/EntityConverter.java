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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.am.shared.vnfd.service.CryptoService;

@Converter
public class EntityConverter implements AttributeConverter<String, String> {

    @Autowired
    private CryptoService cryptoService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cryptoService.encryptString(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cryptoService.decryptString(dbData);
    }
}