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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import static com.ericsson.am.shared.vnfd.utils.Constants.EXTENSIONS_KEY;
import static com.ericsson.am.shared.vnfd.utils.Constants.MODIFIABLE_ATTRIBUTES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEPLOYABLE_MODULES_NOT_PRESENT_IN_VNFD_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEPLOYABLE_MODULE_VALUES_INVALID_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALID_DEPLOYABLE_MODULE_VALUES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VNF_CONTROLLED_SCALING_SHOULD_BE_KEY_VALUE_PAIR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.DataType;
import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.DeployableModule;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExtensionsServiceImpl implements ExtensionsService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Override
    public void validateVnfControlledScalingExtension(final Map<String, Object> extensions, String policies) {
        if (extensions.containsKey(VNF_CONTROLLED_SCALING)) {
            validateAspects(castToMapControlledScaling(extensions), policies);
        }
    }

    @Override
    public void validateDeployableModulesExtension(final Map<String, Object> extensions, VnfInstance vnfInstance, String vnfd) {
        if (extensions.containsKey(DEPLOYABLE_MODULES) && vnfInstance.isDeployableModulesSupported()) {
            Map<String, Object> dmExtensions = checkAndCastObjectToMap(extensions.get(DEPLOYABLE_MODULES));
            NodeTemplate nodeTemplate = ReplicaDetailsUtility.getNodeTemplate(vnfd);
            Map<String, DeployableModule> vnfdDeployableModules = nodeTemplate.getDeploymentModules();
            validateDeployableModulesRequestExtensionsIfExistInVnfd(vnfInstance, dmExtensions, vnfdDeployableModules);
            validateDeployableModulesRequestExtensionsForValidValues(dmExtensions);
        }
    }


    private void validateDeployableModulesRequestExtensionsIfExistInVnfd(VnfInstance vnfInstance,
                                                                         Map<String, Object> extensionsFromRequest,
                                                                         Map<String, DeployableModule> vnfdDeployableModules) {
        final Map<String, Object> nonExistentDeployableModules =
                Maps.difference(extensionsFromRequest, vnfdDeployableModules).entriesOnlyOnLeft();
        if (!nonExistentDeployableModules.isEmpty()) {
            throw new IllegalArgumentException(String.format(DEPLOYABLE_MODULES_NOT_PRESENT_IN_VNFD_ERROR, nonExistentDeployableModules.keySet(),
                                                             vnfInstance.getVnfInstanceId()));
        }
    }

    private void validateDeployableModulesRequestExtensionsForValidValues(Map<String, Object> extensionsFromRequest) {
        final List<String> invalidDeployableModuleValues = extensionsFromRequest.values().stream()
                .map(String.class::cast)
                .filter(deployableModuleValue -> !VALID_DEPLOYABLE_MODULE_VALUES.contains(deployableModuleValue))
                .collect(Collectors.toList());
        if (!invalidDeployableModuleValues.isEmpty()) {
            throw new IllegalArgumentException(String.format(DEPLOYABLE_MODULE_VALUES_INVALID_ERROR, invalidDeployableModuleValues,
                                                             VALID_DEPLOYABLE_MODULE_VALUES));
        }
    }

    @Override
    public void setDefaultExtensions(final VnfInstance vnfInstance, final String descriptorModel) {
        var topologyTemplateInputs = ReplicaDetailsUtility.getTopologyTemplateInputs(descriptorModel);
        resetExtensions(vnfInstance);
        if (MapUtils.isNotEmpty(topologyTemplateInputs)) {
            final Map<String, Map<String, String>> defaultExtensions = createDefaultExtensionsMap(topologyTemplateInputs);
            populateVnfInstanceWithExtensions(vnfInstance, defaultExtensions);
        }
    }

    @Override
    public void updateInstanceWithExtensionsInRequest(Map<String, Object> requestExtensions, VnfInstance instance) {
        Map<String, Object> currentExtensions = Collections.emptyMap();
        if (StringUtils.isNotEmpty(instance.getVnfInfoModifiableAttributesExtensions())) {
            currentExtensions = convertStringToJSONObj(instance.getVnfInfoModifiableAttributesExtensions());
        }
        final Map<String, Object> combinedExtensions = combineExtensions(currentExtensions, requestExtensions);
        instance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(combinedExtensions));
    }

    private static Map<String, Object> combineExtensions(final Map<String, Object> currentExtensions,
                                                         final Map<String, Object> additionalExtensions) {
        Map<String, Object> extensions = new HashMap<>();

        Map<String, Object> vnfScaling = combineExtension(currentExtensions, additionalExtensions, VNF_CONTROLLED_SCALING);
        if (MapUtils.isNotEmpty(vnfScaling)) {
            extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);
        }

        Map<String, Object> deployableModules = combineExtension(currentExtensions, additionalExtensions, DEPLOYABLE_MODULES);
        if (MapUtils.isNotEmpty(deployableModules)) {
            extensions.put(DEPLOYABLE_MODULES, deployableModules);
        }

        return extensions;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> combineExtension(final Map<String, Object> currentExtensions,
                                                        final Map<String, Object> additionalExtensions,
                                                        final String extensionName) {
        Map<String, Object> extensions = new HashMap<>();
        if (MapUtils.isNotEmpty(currentExtensions) && currentExtensions.containsKey(extensionName)) {
            extensions.putAll((Map<String, Object>) currentExtensions.get(extensionName));
        }
        if (additionalExtensions.containsKey(extensionName)) {
            extensions.putAll((Map<String, Object>) additionalExtensions.get(extensionName));
        }
        return extensions;
    }

    private void validateAspects(final Map<String, Object> vnfControlledScaling, String policies) {
        Map<String, ScalingAspectDataType> aspectsFromPolicy = ScalingUtils.getScalingDetails(policies, mapper);
        if (aspectsFromPolicy.isEmpty()) {
            LOGGER.info("VNFD consists only non-scalable VDUs. Scaling Aspects validation will be skipped.");
            return;
        }
        for (Map.Entry<String, Object> aspect : vnfControlledScaling.entrySet()) {
            if (!aspectsFromPolicy.containsKey(aspect.getKey())) {
                throw new IllegalArgumentException(ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY);
            }
            if (!aspect.getValue().equals(CISM_CONTROLLED) && !aspect.getValue().equals(MANUAL_CONTROLLED)) {
                throw new IllegalArgumentException(VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMapControlledScaling(final Map<String, Object> extensions) {
        try {
            return (Map<String, Object>) extensions.get(VNF_CONTROLLED_SCALING);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(VNF_CONTROLLED_SCALING_SHOULD_BE_KEY_VALUE_PAIR, ex);
        }
    }

    private void populateVnfInstanceWithExtensions(final VnfInstance vnfInstance, final Map<String, Map<String, String>> defaultExtensions) {
        if (MapUtils.isNotEmpty(defaultExtensions)) {
            try {
                String defaultExtensionsAsString = mapper.writeValueAsString(defaultExtensions);
                LOGGER.info("Setting default Extensions {}", defaultExtensionsAsString);
                vnfInstance.setVnfInfoModifiableAttributesExtensions(defaultExtensionsAsString);
            } catch (JsonProcessingException e) {
                throw new InternalRuntimeException("Couldn't transform replicaDetails into JSON", e);
            }
        }
    }

    private Map<String, Map<String, String>> createDefaultExtensionsMap(final Map<String, DataType> topologyTemplateInputs) {
        Map<String, Map<String, String>> defaultExtensions = new HashMap<>();
        Map<String, Property> extensions = getExtensionsFromTemplateInputs(topologyTemplateInputs);
        for (Map.Entry<String, Property> extension : extensions.entrySet()) {
            var extensionKey = extension.getKey();
            var extensionDefaultValue = extension.getValue().getDefaultValue();
            validateExtensionDefaultValue(extensionKey, extensionDefaultValue);
            defaultExtensions.put(extensionKey, extensionsMapper.getExtensionsDefaultValue(extensionDefaultValue));
        }

        return defaultExtensions;
    }

    private static Map<String, Property> getExtensionsFromTemplateInputs(final Map<String, DataType> topologyTemplateInputs) {
        var modifiableAttributes = topologyTemplateInputs.get(MODIFIABLE_ATTRIBUTES);
        return modifiableAttributes != null ?
                modifiableAttributes.getProperties().get(EXTENSIONS_KEY).getTypeValue().getProperties() : Collections.emptyMap();
    }

    private static void validateExtensionDefaultValue(final String extension, final String defaultValue) {
        if (extension.equals(VNF_CONTROLLED_SCALING) && StringUtils.isEmpty(defaultValue)) {
            throw new InternalRuntimeException("vnfControlledScaling missing default value.");
        }
        if (extension.equals(DEPLOYABLE_MODULES) && StringUtils.isEmpty(defaultValue)) {
            throw new InternalRuntimeException("deployableModules missing default value.");
        }
    }

    private void resetExtensions(final VnfInstance vnfInstance) {
        vnfInstance.setVnfInfoModifiableAttributesExtensions(null);
    }
}
