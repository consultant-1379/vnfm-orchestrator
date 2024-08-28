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

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_INSTANCE_NOT_ADDED_TO_OSS;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertEscapedJsonToYaml;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AlreadyInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotAddedToOssException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class InstanceUtils {

    public static String getAdditionalParamsYamlConversionResult(final VnfInstance instance) {
        String combinedAdditionalParams = instance.getCombinedAdditionalParams();
        if (combinedAdditionalParams == null) {
            return "";
        }
        try {
            final Map<String, Object> additionalParamsWithoutDotDelimeters = YamlUtility
                    .removeDotDelimitersFromYamlMap(new JSONObject(combinedAdditionalParams).toMap());
            return new YAMLMapper().writeValueAsString(additionalParamsWithoutDotDelimeters);
        } catch (JsonProcessingException e) {
            LOGGER.warn("An error during parsing JSON", e);
            return "";
        }
    }

    public static String getCombinedValuesFileAsYaml(final VnfInstance instance) {
        String combinedValuesFile = instance.getCombinedValuesFile();
        if (!StringUtils.isEmpty(combinedValuesFile)) {
            return convertEscapedJsonToYaml(combinedValuesFile);
        }
        return "";
    }

    public static VnfInstance createTempInstance(final VnfInstance vnfInstance) {
        VnfInstance tempInstance = new VnfInstance();
        BeanUtils.copyProperties(vnfInstance, tempInstance, "tempInstance");
        tempInstance.getHelmCharts().forEach(obj -> obj.setState(null));
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        return tempInstance;
    }

    public static void updateCombinedValuesEntity(final VnfInstance tempVnfInstance,
                                                  final Map<String, Object> valuesYamlMap,
                                                  final Map<String, Object> additionalParams) {
        if (MapUtils.isNotEmpty(valuesYamlMap)) {
            tempVnfInstance.setCombinedValuesFile(convertObjToJsonString(valuesYamlMap));
        }
        if (MapUtils.isNotEmpty(additionalParams)) {
            tempVnfInstance.setCombinedAdditionalParams(convertObjToJsonString(additionalParams));
        }
    }

    public static void checkVnfNotInState(final VnfInstance vnfInstance, final InstantiationState undesiredState) {
        if (undesiredState.equals(vnfInstance.getInstantiationState())) {
            if (undesiredState.equals(INSTANTIATED)) {
                throw new AlreadyInstantiatedException(vnfInstance);
            } else {
                throw new NotInstantiatedException(vnfInstance);
            }
        }
    }

    public static void checkAddedToOss(final VnfInstance vnfInstance) {
        if (!vnfInstance.isAddedToOss()) {
            throw new NotAddedToOssException(vnfInstance, String.format(VNF_INSTANCE_NOT_ADDED_TO_OSS, vnfInstance.getVnfInstanceId()));
        }
    }

    public static void checkAddedToOss(final VnfInstance vnfInstance, final OssNodeService ossNodeService) {
        if (!ossNodeService.checkNodePresent(vnfInstance)) {
            throw new NotAddedToOssException(vnfInstance, String.format(VNF_INSTANCE_NOT_ADDED_TO_OSS, vnfInstance.getVnfInstanceId()));
        }
    }

    public static void resetCombinedValues(final VnfInstance vnfInstance) {
        vnfInstance.setCombinedAdditionalParams(null);
        vnfInstance.setCombinedValuesFile(null);
    }
}
