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
package com.ericsson.vnfm.orchestrator.utils;

import static java.util.Collections.emptyMap;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.UNABLE_TO_PARSE_JSON_CAUSE_FORMAT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.RequestWithAdditionalParams;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Component
public final class AdditionalParamsUtils {

    private static final ObjectMapper YAML_READER = new ObjectMapper(new YAMLFactory());

    @Autowired
    @Qualifier("yamlFileMerger")
    private FileMerger fileMerger;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OperationsUtils operationsUtils;

    public String convertYamlStringToJsonString(String yamlString) {
        Object obj;
        String resultString;
        try {
            obj = YAML_READER.readValue(yamlString, Object.class);
            resultString = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException(String.format("Unable process JSON - %s", yamlString), e);
        }
        return resultString;
    }

    @SafeVarargs
    public final String mergeAdditionalParams(Map<String, Object>... additionalParamsToMerge) {
        String[] additionalParamsToMergeYaml = Arrays.stream(additionalParamsToMerge)
                .filter(Objects::nonNull)
                .map(ValuesFileComposer::extractAdditionalParamsToAppend)
                .map(YamlUtility::removeDotDelimitersFromYamlMap)
                .map(YamlUtility::convertMapToYamlFormat)
                .toArray(String[]::new);
        return fileMerger.merge(additionalParamsToMergeYaml);
    }

    public static Map<String, Object> getUpgradeFailedAdditionalParams(LifecycleOperation operation) {
        final Object additionalParams = parseJsonOperationAdditionalParams(operation);
        final Map<String, Object> requestAdditionalParams = Utility.copyParametersMap(additionalParams);
        return Utility.copyParametersMap(requestAdditionalParams.get(UPGRADE_FAILED_VNFD_KEY));
    }

    public Map<String, Object> convertAdditionalParamsToMap(String additionalParams) {
        return StringUtils.isBlank(additionalParams) ? emptyMap() :
                convertStringToJSONObj(convertYamlStringToJsonString(additionalParams));
    }

    public static Object parseJsonOperationAdditionalParams(LifecycleOperation operation) {
        String initialRequest = operation.getOperationParams();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(initialRequest, RequestWithAdditionalParams.class).getAdditionalParams();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_JSON_CAUSE_FORMAT, initialRequest, e.getMessage()), e);
        }
    }
}
