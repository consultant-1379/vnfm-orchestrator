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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.onboarding.VnflcmModel;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.NodeUtility;
import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.am.shared.vnfd.model.VnfDescriptorDetails;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Node;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.TopologyTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VnfmLcmInterface;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.AdditionalParametersModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.DataTypesPropertiesModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.DescriptorModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.InputsModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.InterfaceModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeNotSupportedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VnfdUtils {

    private static final String TOSCA_DEFINITIONS_VERSION_KEY = "tosca_definitions_version";
    private static final String TOSCA_DEFINITIONS_VERSION_1_3_VALUE = "tosca_simple_yaml_1_3";
    private static final String NODE_TYPES_KEY = "node_types";
    private static final String DATA_TYPES_KEY = "data_types";

    private VnfdUtils() {
    }

    public static String getInstantiateName(JSONObject descriptorModel) throws IOException {
        DescriptorModel model = getDescriptorModel(descriptorModel);
        VnflcmModel vnflcmModel = model.getInterfacesModel().getVnflcmModel();

        InterfaceModel instantiateModel = TOSCA_DEFINITIONS_VERSION_1_3_VALUE.equals(descriptorModel.getString(TOSCA_DEFINITIONS_VERSION_KEY))
                ? vnflcmModel.getOperationsModelV13().getInstantiateModel()
                : vnflcmModel.getInstantiateModel();

        return getAdditionalParamsFromInterfaceModel(instantiateModel);
    }

    public static String getChangePackageName(JSONObject descriptorModel) throws IOException {
        DescriptorModel model = getDescriptorModel(descriptorModel);
        VnflcmModel vnflcmModel = model.getInterfacesModel().getVnflcmModel();

        InterfaceModel changePackageModel = TOSCA_DEFINITIONS_VERSION_1_3_VALUE.equals(descriptorModel.getString(TOSCA_DEFINITIONS_VERSION_KEY))
                ? vnflcmModel.getOperationsModelV13().getChangePackageModel()
                : vnflcmModel.getChangePackageModel();

        return getAdditionalParamsFromInterfaceModel(changePackageModel);
    }

    static DescriptorModel getDescriptorModel(final JSONObject descriptorModel) throws JsonProcessingException {
        JSONObject nodeTypes = descriptorModel.getJSONObject(NODE_TYPES_KEY);
        Iterator<String> keys = nodeTypes.keys();
        JSONObject interfaces = nodeTypes.getJSONObject(keys.next());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(interfaces.toString(), DescriptorModel.class);
    }

    private static String getAdditionalParamsFromInterfaceModel(InterfaceModel interfaceModel) {
        if (interfaceModel == null) {
            return null;
        }
        InputsModel changePackageInputsModel = interfaceModel.getInputsModel();
        if (changePackageInputsModel == null) {
            return null;
        }
        AdditionalParametersModel additionalParametersModel = changePackageInputsModel.getAdditionalParametersModel();
        if (additionalParametersModel == null) {
            return null;
        }
        return additionalParametersModel.getType();
    }

    public static Map<String, PropertiesModel> getPropertiesModelMap(final JSONObject descriptorModel,
                                                                     final String type) throws JsonProcessingException {
        JSONObject dataTypes = descriptorModel.getJSONObject(DATA_TYPES_KEY).getJSONObject(type);
        ObjectMapper mapper = new ObjectMapper();
        DataTypesPropertiesModel model = mapper.readValue(dataTypes.toString(), DataTypesPropertiesModel.class);
        return model.getProperties();
    }

    public static Optional<VnfmLcmInterface> searchForInterface(JSONObject vnfd, String interfaceToCheck) {
        VnfDescriptorDetails descriptorDetails = new VnfDescriptorDetails();
        Node node = NodeUtility.getNode(vnfd, descriptorDetails);
        List<VnfmLcmInterface> interfaces = node.getNodeType().getInterfaces();
        return interfaces.stream().filter(lcmInterface -> lcmInterface.getType().getLabel().equals(interfaceToCheck)).findAny();
    }


    public static boolean isDowngradeSupported(final JSONObject vnfd, final VnfInstance sourceVnfInstance,
                                               final String targetVnfdId, final String targetVnfSoftwareVersion) {
        try {
            VnfDescriptorDetails descriptorDetails = VnfdUtility.buildVnfDescriptorDetails(vnfd);
            TopologyTemplate topologyTemplate = descriptorDetails.getDefaultFlavour().getTopologyTemplate();
            return PolicyUtility.getDowngradeAdditionalParameters(sourceVnfInstance.getVnfDescriptorId(), targetVnfdId,
                    sourceVnfInstance.getVnfSoftwareVersion(), targetVnfSoftwareVersion, topologyTemplate) != null;
        } catch (Exception ex) {
            throw new DowngradeNotSupportedException(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, sourceVnfInstance.getVnfInstanceId()), ex);
        }
    }

    public static Map<String, Property> getVnfdDowngradeParams(final JSONObject vnfd, String sourceVnfdId,
                                                               String targetVnfdId, String sourceSoftwareVersion,
                                                               String targetSoftwareVersion) {
        VnfDescriptorDetails descriptorDetails = VnfdUtility.buildVnfDescriptorDetails(vnfd);
        TopologyTemplate topologyTemplate = descriptorDetails.getDefaultFlavour().getTopologyTemplate();
        return PolicyUtility.getDowngradeAdditionalParameters(sourceVnfdId, targetVnfdId, sourceSoftwareVersion,
                targetSoftwareVersion, topologyTemplate);
    }

    public static Map<String, Property> getVnfdDowngradeParams(final JSONObject vnfd, String sourceVnfdId,
                                                               String targetVnfdId) {
        VnfDescriptorDetails descriptorDetails = VnfdUtility.buildVnfDescriptorDetails(vnfd);
        TopologyTemplate topologyTemplate = descriptorDetails.getDefaultFlavour().getTopologyTemplate();
        return PolicyUtility.getDowngradeAdditionalParameters(sourceVnfdId, targetVnfdId, topologyTemplate);
    }


    public static boolean isResourcesAllowedByVnfd(final JSONObject vnfdDescriptorModel, final String parameter) {
        String changePackageNameFromVnfd = getChangePackageNameFromVnfd(vnfdDescriptorModel);
        Map<String, PropertiesModel> properties = new HashMap<>();
        if (StringUtils.isNotEmpty(changePackageNameFromVnfd)) {
            try {
                properties = getPropertiesModelMap(vnfdDescriptorModel, changePackageNameFromVnfd);
            } catch (IOException e) {
                LOGGER.warn("Error retrieving properties for inputs type {}", changePackageNameFromVnfd, e);
                return false;
            }
        }
        return !CollectionUtils.isEmpty(properties) && getVnfdParameter(properties, parameter);
    }

    public static String getChangePackageNameFromVnfd(final JSONObject vnfdDescriptorModel) {
        String name = null;
        try {
            name = getChangePackageName(vnfdDescriptorModel);
        } catch (IOException e) {
            LOGGER.warn("Error retrieving change_package inputs type", e);
        }
        return name;
    }

    private static boolean getVnfdParameter(final Map<String, PropertiesModel> properties, final String parameter) {
        PropertiesModel propertiesModel = properties.get(parameter);
        if (propertiesModel == null && parameter.equals(IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY)) {
            return true;
        } else if (propertiesModel == null) {
            return false;
        } else {
            return Boolean.parseBoolean(propertiesModel.getDefaultValue());
        }
    }

    public static Object parseVnfdParameter(String defaultValue) {
        if (BooleanUtils.isBoolean(defaultValue)) {
            return Boolean.valueOf(defaultValue);
        }
        if (StringUtils.isNumeric(defaultValue)) {
            return Integer.parseInt(defaultValue);
        }
        return defaultValue;
    }

    public static String getOperationNameFromVnfd(LifecycleOperationType type,
                                                  JSONObject vnfdDescriptorModel) throws IOException {
        return switch (type) {
            case INSTANTIATE -> getInstantiateName(vnfdDescriptorModel);
            case CHANGE_VNFPKG -> getChangePackageName(vnfdDescriptorModel);
            default -> null;
        };
    }
}
