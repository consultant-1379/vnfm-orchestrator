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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.RetrieveDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class ExtensionsMapper {

    @Autowired
    private ObjectMapper mapper;

    public Map<String, String> getVnfControlledScalingValues(final String vnfInfoModifiableAttributesExtensions) {
        Map<String, Map<String, String>> extensions = getVnfInfoModifiableAttributesExtensions(vnfInfoModifiableAttributesExtensions);

        return extensions.get(VNF_CONTROLLED_SCALING);
    }

    public Map<String, String> getDeployableModulesValues(final String vnfInfoModifiableAttributesExtensions) {
        Map<String, Map<String, String>> extensions = getVnfInfoModifiableAttributesExtensions(vnfInfoModifiableAttributesExtensions);

        return extensions.getOrDefault(DEPLOYABLE_MODULES, new HashMap<>());
    }

    public Map<String, String> getExtensionsDefaultValue(final String defaultValue) {
        Map<String, String> extensionsDefaultValue;
        try {
            extensionsDefaultValue = mapper.readValue(defaultValue, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RetrieveDataException("Could not parse extensions default value", e);
        }
        return extensionsDefaultValue;
    }

    public Map<String, Map<String, String>> getVnfInfoModifiableAttributesExtensions(final String vnfInfoModifiableAttributesExtensions) {
        if (StringUtils.isEmpty(vnfInfoModifiableAttributesExtensions)) {
            return new HashMap<>();
        }
        TypeReference<Map<String, Map<String, String>>> typeRef = new TypeReference<>() {
        };

        try {
            return mapper.readValue(vnfInfoModifiableAttributesExtensions, typeRef);
        } catch (JsonProcessingException e) {
            throw new RetrieveDataException("Could not parse vnfInfoModifiableAttributesExtensionsMap", e);
        }
    }
}
