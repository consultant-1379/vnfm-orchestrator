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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import static java.util.stream.Collectors.toMap;

import static com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService.getPropertyFromValuesYamlMap;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.getOperationNameFromVnfd;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.getPropertiesModelMap;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.parseVnfdParameter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.utils.VnfdUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VnfdParametersHelper {

    @Autowired
    private PackageService packageService;

    @Autowired
    private ValuesFileService valuesFileService;

    public void mergeDefaultParameters(final String vnfdId,
                                       final LifecycleOperationType type,
                                       final Map<String, Object> additionalParams,
                                       final Map<String, Object> valuesYamlMap) {
        LOGGER.info("Retrieve default parameters from vnfd with id [{}]", vnfdId);
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(vnfdId);

        mergeDefaultParameters(packageInfo, type, additionalParams, valuesYamlMap);
    }

    public void mergeDefaultParameters(final PackageResponse packageInfo,
                                       final LifecycleOperationType type,
                                       final Map<String, Object> additionalParams,
                                       final Map<String, Object> valuesYamlMap) {
        JSONObject vnfdDescriptorModel = new JSONObject(packageInfo.getDescriptorModel());
        Map<String, PropertiesModel> propertiesModelMap = null;

        try {
            String operationName = getOperationNameFromVnfd(type, vnfdDescriptorModel);
            if (operationName != null) {
                propertiesModelMap = getPropertiesModelMap(vnfdDescriptorModel, operationName);
                LOGGER.info("Default parameters retrieved successfully");
            }
        } catch (IOException e) { // NOSONAR
            LOGGER.warn("Failed to retrieve default properties from vnfd with id [{}]. Default values may be missed", packageInfo.getVnfdId());
        }
        Map<String, Object> defaultValues = Optional.ofNullable(propertiesModelMap).orElse(new HashMap<>())
                .entrySet().stream()
                .filter(entry -> entry.getValue().getDefaultValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().getDefaultValue()));

        mergeDefaultParamsIntoAdditionalAndValuesYaml(additionalParams, valuesYamlMap, defaultValues);
    }

    public void mergeDefaultDowngradeParameters(final VnfInstance vnfInstance,
                                                final Object request,
                                                final LifecycleOperationType type,
                                                final Map<String, Object> additionalParams, final Map<String, Object> valuesYamlMap) {
        String sourceVnfdId = vnfInstance.getVnfDescriptorId();
        String targetVnfdId = ((ChangeCurrentVnfPkgRequest) request).getVnfdId();

        LOGGER.info("Retrieve default parameters from vnfd with id [{}]", sourceVnfdId);
        PackageResponse sourcePackageInfo = packageService.getPackageInfoWithDescriptorModel(sourceVnfdId);
        LOGGER.info("Retrieve default parameters from vnfd with id [{}]", targetVnfdId);
        PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);

        JSONObject sourceVnfd = new JSONObject(sourcePackageInfo.getDescriptorModel());
        Map<String, Property> downgradeParams = VnfdUtils.getVnfdDowngradeParams(sourceVnfd, sourceVnfdId, targetVnfdId,
                vnfInstance.getVnfSoftwareVersion(), targetPackageInfo.getVnfSoftwareVersion());

        if (downgradeParams != null) {
            Map<String, Object> defaultValues = downgradeParams.entrySet().stream()
                    .filter(entry -> entry.getValue().getDefaultValue() != null)
                    .collect(toMap(Map.Entry::getKey, value -> value.getValue().getDefaultValue()));
            mergeDefaultParamsIntoAdditionalAndValuesYaml(additionalParams, valuesYamlMap, defaultValues);
        } else {
            mergeDefaultParameters(targetPackageInfo, type, additionalParams, valuesYamlMap);
        }
    }

    private void mergeDefaultParamsIntoAdditionalAndValuesYaml(final Map<String, Object> additionalParams,
                                                               final Map<String, Object> valuesYamlMap,
                                                               final Map<String, Object> defaultValues) {
        filterDefaultParams(additionalParams, valuesYamlMap, defaultValues);

        defaultValues.forEach((key, defaultValue) -> {
            Object parsedValue = parseVnfdParameter((String) defaultValue);
            defaultValues.replace(key, parsedValue);
            additionalParams.computeIfAbsent(key, additionalValue -> parsedValue);
        });

        valuesFileService.mergeValuesYamlMap(valuesYamlMap, defaultValues);
    }

    private static void filterDefaultParams(final Map<String, Object> additionalParams,
                                            final Map<String, Object> valuesYamlMap,
                                            final Map<String, Object> defaultValues) {
        Set<String> keysToRemove = defaultValues.keySet().stream()
                .filter(key -> additionalParams.containsKey(key)
                        || getPropertyFromValuesYamlMap(valuesYamlMap, key, Object.class) != null)
                .collect(Collectors.toSet());

        defaultValues.keySet().removeAll(keysToRemove);
    }

}
