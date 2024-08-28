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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_YAML_ADDITIONAL_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.OSS_TOPOLOGY_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_CLIENT_VERSION_YAML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENTITY_PROFILE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_IN_MINUTES;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertMapToYamlFormat;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.removeDotDelimitersFromYamlMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.utils.FileMerger;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ValuesFileComposer {

    @Autowired
    @Qualifier("yamlFileMerger")
    private FileMerger fileMerger;

    public static Map<String, Object> extractAdditionalParamsToAppend(Map<String, Object> requestAdditionalParams) {
        validateAdditionalParamsForNullValue(requestAdditionalParams);
        return requestAdditionalParams.entrySet().stream().filter(ValuesFileComposer::mergeParamPredicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static void validateAdditionalParamsForNullValue(final Map<String, Object> requestAdditionalParams) {
        List<String> additionalParamsWithNullValue = requestAdditionalParams.entrySet()
                .stream()
                .filter(elem -> elem.getValue() == null)
                .map(Map.Entry::getKey).collect(Collectors.toList());

        if (!additionalParamsWithNullValue.isEmpty()) {
            throw new IllegalArgumentException(String.format("You cannot merge yaml where value is null for %s",
                                                             StringUtils.join(additionalParamsWithNullValue, ", ")));
        }
    }

    public static boolean mergeParamPredicate(Map.Entry<String, Object> entry) {
        String paramKey = entry.getKey();

        boolean isParamKeyContainsPrefix = StringUtils.containsAny(paramKey,
                                                                   DAY0_CONFIGURATION_PREFIX,
                                                                   OSS_TOPOLOGY_PREFIX,
                                                                   UPGRADE_FAILED_VNFD_KEY);

        boolean isParamKeyEqualsOneOfListWord = EVNFM_PARAMS.contains(paramKey) || EVNFM_PARAMS_FOR_WFS.contains(paramKey)
                || StringUtils.equalsAny(paramKey,
                                         VALUES_YAML_ADDITIONAL_PARAMETER,
                                         HELM_CLIENT_VERSION_YAML,
                                         ADD_NODE_TO_OSS,
                                         CMP_V2_ENROLLMENT,
                                         OTP_VALIDITY_PERIOD_IN_MINUTES,
                                         ENTITY_PROFILE_NAME);

        return !isParamKeyEqualsOneOfListWord && !isParamKeyContainsPrefix;
    }

    public String compose(String fileYamlValues, Map<String, Object> additionalParams) {
        if (CollectionUtils.isEmpty(additionalParams)) {
            return StringUtils.defaultString(fileYamlValues);
        }
        String valuesFileWithValuesYamlParam = addValuesYamlParamToValuesFile(fileYamlValues, additionalParams);
        Map<String, Object> additionalParamsToAppend = extractAdditionalParamsToAppend(additionalParams);
        return mergeAdditionalParams(valuesFileWithValuesYamlParam, additionalParamsToAppend);
    }

    private String addValuesYamlParamToValuesFile(final String valuesYamlContent, final Map<String, Object> additionalParams) {
        Optional<String> valuesYamlParamOpt = Optional.ofNullable((String) additionalParams.get(VALUES_YAML_ADDITIONAL_PARAMETER));
        return valuesYamlParamOpt
                .map(YamlUtility::convertEscapedJsonToYaml)
                .map(fileMerger::merge)
                .orElse(valuesYamlContent);
    }

    private String mergeAdditionalParams(String fileYamlValues, Map<String, Object> additionalParamsToAppend) {
        Map<String, Object> additionalParamsWithoutDelimiters = removeDotDelimitersFromYamlMap(additionalParamsToAppend);
        String additionalParamsYaml = convertMapToYamlFormat(additionalParamsWithoutDelimiters);
        return fileMerger.merge(fileYamlValues, additionalParamsYaml);
    }
}
