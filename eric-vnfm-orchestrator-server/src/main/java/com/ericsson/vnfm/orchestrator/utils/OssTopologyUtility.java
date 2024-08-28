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

import static java.lang.String.format;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.CERT_TYPE_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.CMPV2_ENROLLMENT_MODE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_CERT_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_MODE_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENTITY_PROFILE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.NODE_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.NODE_FDN_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_IN_MINUTES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_MAX_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_MIN_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_NON_EXPIRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ROOT_NODES_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.NETWORK_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logoOssTopologyMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OssTopologyUtility {

    public static final String OSS_TOPOLOGY = "ossTopology";

    private OssTopologyUtility() {
    }

    public static Map<String, PropertiesModel> getOssTopology(JSONObject descriptorModel,
            final String instantiateName) {
        Map<String, PropertiesModel> topology = new HashMap<>();
        try {
            topology = getTopology(descriptorModel, instantiateName);
        } catch (IOException e) {
            LOGGER.warn("Error retrieving oss topology for inputs type {}", instantiateName, e);
        }
        return topology;
    }

    public static Map<String, PropertiesModel> getTopology(JSONObject descriptorModel, String type) throws IOException {
        Map<String, PropertiesModel> properties = VnfdUtils.getPropertiesModelMap(descriptorModel, type);
        return properties == null ? new HashMap<>() : getOssTopologySpecificParameters(properties);
    }

    public static <T> Map<String, T> getOssTopologyAsMap(final String ossTopology, Class<T> type) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeFactory typeFactory = mapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, type);
            return mapper.readValue(ossTopology, mapType);
        } catch (IOException e) {
            LOGGER.warn("failed to get oss topology from db", e);
        }
        return new HashMap<>();
    }

    public static <T> Map<String, T> getOssTopologySpecificParameters(final Map<String, T> properties) {
        Map<String, T> ossTopologyMap = properties.entrySet()
                .stream().filter(e -> e.getKey().startsWith(OSS_TOPOLOGY) && !Objects.isNull(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return removeOssTopologyFromKey(ossTopologyMap);
    }

    public static <T> Map<String, T> extendOssTopologySpecificParameters(Map<String, T> ossParams, Map<String, T> additionalParams,
                                                                         String... optionalParams) {
        for (String optionalParam: optionalParams) {
            if (additionalParams.containsKey(optionalParam)) {
                ossParams.put(optionalParam, additionalParams.get(optionalParam));
            }
        }
        return ossParams;
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> removeOssTopologyFromKey(Map<String, T> properties) {
        Map<String, Object> propertiesWithoutPassword = dontLogPasswords(properties);
        LOGGER.debug("Removing oss topology key from following attributes: {}", logoOssTopologyMap(propertiesWithoutPassword));

        if (properties.containsKey(OSS_TOPOLOGY)) {
            return (Map<String, T>) properties.get(OSS_TOPOLOGY);
        }
        return properties.entrySet().stream()
                .filter(e -> !Objects.isNull(e.getValue()))
                .collect(Collectors.toMap(entry -> entry.getKey().contains(OSS_TOPOLOGY) ?
                                         entry.getKey().substring(OSS_TOPOLOGY.length() + 1) : entry.getKey(),
                                 Map.Entry::getValue));
    }

    public static <T> Map<String, T> removeOssTopologyFromKeyWithAdditionalAttributes(Map<String, T> propertiesInAdditionalParams) {
        if (propertiesInAdditionalParams.get("additionalAttributes") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, T> properties = (Map<String, T>) propertiesInAdditionalParams.get("additionalAttributes");
            Map<String, Object> propertiesWithoutPassword = dontLogPasswords(properties);
            LOGGER.debug("additionalAttributes from addNode: {}", logoOssTopologyMap(propertiesWithoutPassword));
            return removeOssTopologyFromKey(properties);
        } else {
            return removeOssTopologyFromKey(propertiesInAdditionalParams);
        }
    }

    public static void mergeMaps(final Map<String, PropertiesModel> ossTopology,
            final Map<String, ? extends Object> ossTopologySpecificParameters) {
        ossTopologySpecificParameters.forEach((k, v) -> {
            if (ossTopology.containsKey(k)) {
                ossTopology.get(k).setDefaultValue(v.toString());
            } else {
                ossTopology.put(k, new PropertiesModel(v.toString()));
            }
        });
        logoOssTopologyMap(ossTopology);
    }

    public static Map<String, Object> dontLogPasswords(final Map<String, ? extends Object> fullSetOfAttributes) {
        Map<String, Object> hashedPasswords = new HashMap<>();
        if (!CollectionUtils.isEmpty(fullSetOfAttributes)) {
            for (Entry<String, ? extends Object> entry : fullSetOfAttributes.entrySet()) {
                if (entry.getKey().toLowerCase().contains("password")) {
                    hashedPasswords.put(entry.getKey(), "*************");
                    continue;
                }
                hashedPasswords.put(entry.getKey(), entry.getValue());
            }
        }
        return hashedPasswords;
    }

    public static Optional<Object> getOssTopologyManagedElementId(final String ossTopology) {
        // Oss topology does not always map to PropertyModel, so have to map to Object here
        Map<String, Object> ossTopologyAsMap = getOssTopologyAsMap(ossTopology, Object.class);
        Object networkElementId = ossTopologyAsMap.get(NETWORK_ELEMENT_ID);
        Object managedElementId = ossTopologyAsMap.get(MANAGED_ELEMENT_ID);

        if (networkElementId != null && !StringUtils.isEmpty(networkElementId.toString())) {
            return Optional.of(networkElementId);
        } else if (managedElementId != null && !StringUtils.isEmpty(managedElementId.toString())) {
            return Optional.of(managedElementId);
        }
        return Optional.empty();
    }

    public static Map<String, Object> transformModelToFtlDataTypes(Map<String, PropertiesModel> mergedMap) {
        return mergedMap.entrySet().stream().filter(entry -> entry.getValue().getDefaultValue() != null).collect(
                Collectors.toMap(Map.Entry::getKey, entry -> transformValue().apply(entry.getValue())));
    }

    public static void validateOtpValidityPeriod(final Map<?, ?> additionalParams) {
        Object otpValidityPeriodParam = additionalParams.get(OTP_VALIDITY_PERIOD_IN_MINUTES);

        if (MapUtils.isEmpty(additionalParams)
                || otpValidityPeriodParam == null
                || StringUtils.isEmpty(otpValidityPeriodParam.toString())) {
            return;
        }

        int otpValidityPeriodInMinutes;
        try {
            otpValidityPeriodInMinutes = Integer.parseInt(otpValidityPeriodParam.toString());
        } catch (NumberFormatException e) { // NOSONAR
            throw new InvalidInputException(
                    format("Unable to convert value of 'otpValidityPeriodInMinutes' to integer: '%s'", otpValidityPeriodParam));
        }

        if ((otpValidityPeriodInMinutes < OTP_VALIDITY_PERIOD_MIN_VALUE || otpValidityPeriodInMinutes > OTP_VALIDITY_PERIOD_MAX_VALUE)
                && otpValidityPeriodInMinutes != OTP_VALIDITY_PERIOD_NON_EXPIRING) {
            throw new InvalidInputException(
                    format("Invalid time period provided for '%s'. It should be >= 1 and <= 43200, or -1 if never expiring.",
                           OTP_VALIDITY_PERIOD_IN_MINUTES));
        }
    }

    private static Function<PropertiesModel, Object> transformValue() {
        return propertiesModel ->
                StringUtils.equalsIgnoreCase(propertiesModel.getType(), "boolean")
                        ? BooleanUtils.toBoolean(propertiesModel.getDefaultValue())
                        : propertiesModel.getDefaultValue();
    }

    public static String createSitebasicFileFromOSSParams(final VnfInstance vnfInstance, final Map<String, Object> ossParams) {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put(NODE_FDN_TAG, getNodeFdn(vnfInstance, ossParams));
        nodeMap.put(CERT_TYPE_TAG, ENROLLMENT_CERT_TYPE);
        nodeMap.computeIfAbsent(ENTITY_PROFILE_NAME, key -> getParam(key, ossParams));
        nodeMap.put(ENROLLMENT_MODE_TAG, CMPV2_ENROLLMENT_MODE);
        nodeMap.computeIfAbsent(OTP_VALIDITY_PERIOD_IN_MINUTES, key -> getParam(key, ossParams));

        return convertMapToSitebasicXML(nodeMap);
    }

    private static String getNodeFdn(final VnfInstance vnfInstance, final Map<String, Object> ossParams) {
        if (CollectionUtils.isEmpty(ossParams) || StringUtils.isEmpty((String) ossParams.get(MANAGED_ELEMENT_ID))) {
            LOGGER.debug("{} is missing from ossTopology attributes, VNF instance name will be used as nodeFdn in sitebasic.xml", MANAGED_ELEMENT_ID);
            return vnfInstance.getVnfInstanceName();
        }
        return (String) ossParams.get(MANAGED_ELEMENT_ID);
    }

    private static Object getParam(String key, Map<String, Object> ossParams) {
        return Objects.isNull(ossParams)
                ? null
                : ossParams.get(key);
    }

    private static String convertMapToSitebasicXML(final Map<String, Object> nodeMap) {
        try {
            Document document = XMLUtility.createEmptyXMLDocument();

            Element rootNodesTag = document.createElement(ROOT_NODES_TAG);
            Element nodeTag = document.createElement(NODE_TAG);
            for (var entry : nodeMap.entrySet()) {
                Element nodeElement = document.createElement(entry.getKey());
                nodeElement.appendChild(document.createTextNode(entry.getValue().toString()));
                nodeTag.appendChild(nodeElement);
            }
            rootNodesTag.appendChild(nodeTag);
            document.appendChild(rootNodesTag);

            return XMLUtility.transformXMLDocumentToString(document);
        } catch (ParserConfigurationException | TransformerException e) {
            LOGGER.warn("Unable to generate sitebasic.xml due to parsing errors: {}", e.getMessage());
            return StringUtils.EMPTY;
        }
    }
}
