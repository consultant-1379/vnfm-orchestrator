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
package com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExtCpValidationServiceFactory {

    private Map<Class<?>, ExtCpValidationService<?>> validationServiceCache = new HashMap<>();

    @Autowired
    private List<ExtCpValidationService> extCpValidationServices;

    @PostConstruct
    public void initMyServiceCache() {
        for (ExtCpValidationService<?> validationService : extCpValidationServices) {
            validationServiceCache.put(validationService.getType(), validationService);
        }
    }

    public ExtCpValidationService<?> getValidationService(Class<?> clazz) {
        return validationServiceCache.get(clazz);
    }
}
