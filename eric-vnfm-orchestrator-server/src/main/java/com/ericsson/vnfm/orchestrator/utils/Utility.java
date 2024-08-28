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

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CONFIG_EXTENSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DEFAULT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.UNABLE_TO_OBJECT_TO_JSON_CAUSE_FORMAT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.UNABLE_TO_PARSE_JSON_CAUSE_FORMAT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_V4;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_V6;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADDITIONAL_PARAMETERS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.NETWORK_ELEMENT_ID;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.model.ClusterConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.OperationalState;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.io.Resources;

import io.lettuce.core.RedisBusyException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Utility {

    public static final String UNEXPECTED_EXCEPTION_OCCURRED = "Unexpected Exception occurred";
    private static final String MANAGED_ELEMENT_ID_NAME = "%s-%s-%s";

    private Utility() {

    }

    public static Date convertToDate(LocalDateTime localDateTime) {
        return localDateTime != null ?
                Date.from(localDateTime.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant()) :
                null;
    }

    public static ProblemDetails getProblemDetails(String error) {
        ProblemDetails problemDetails = null;

        if (StringUtils.isNotEmpty(error)) {
            problemDetails = new ProblemDetails();
            try {
                var errorObj = new ErrorParser(error);

                problemDetails.setStatus(errorObj.getStatus());
                problemDetails.setDetail(errorObj.getDetails());
                problemDetails.setType(URI.create(TYPE_BLANK));
                problemDetails.setTitle(errorObj.getTitle());
                problemDetails.setInstance(getVnfInstanceURI(errorObj.getInstance()));
            } catch (JSONException e) {
                LOGGER.warn(BAD_REQUEST.getReasonPhrase(), e);
                problemDetails
                        .title(BAD_REQUEST.getReasonPhrase())
                        .type(URI.create(TYPE_BLANK))
                        .detail(error)
                        .status(BAD_REQUEST.value())
                        .instance(URI.create(TYPE_BLANK));
            }
        }
        return problemDetails;
    }

    public static URI getVnfInstanceURI(String instance) {
        if (StringUtils.isNotEmpty(instance) && !("null").equals(instance)) {
            if (isUriFormat(instance)) {
                return URI.create(instance);
            } else {
                return URI.create(HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + instance);
            }
        } else {
            return URI.create(TYPE_BLANK);
        }
    }

    public static boolean isUriFormat(String uriString) {
        try {
            new URI(uriString);
            return true;
        } catch (URISyntaxException e) { // NOSONAR
            return false;
        }
    }

    public static Path storeFileTemp(final MultipartFile values, final String filename, final String suffix) {
        try {
            Path valuesFile = Files.createTempFile(filename, suffix);
            values.transferTo(valuesFile.toFile());
            return valuesFile;
        } catch (IOException e) {
            throw new InternalRuntimeException(String.format("Unable to store values file %s", values.getName()), e);
        }
    }

    public static void deleteFile(final Path file) {
        try {
            if (file != null && Files.exists(file)) {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't deleteFile file {}", file, e);
        }
    }

    public static String readJsonFromResource(String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), StandardCharsets.UTF_8);
    }

    public static Map<String, Object> convertStringToJSONObj(String jsonString) {
        if (jsonString == null) {
            return new HashMap<>();
        }
        try {
            return new JSONObject(jsonString).toMap();
        } catch (JSONException e) {
            throw new InternalRuntimeException("Unable to convert to JSON Object. Invalid JSON string", e);
        }
    }

    public static <T> T parseJson(final String jsonString,
                                  final Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, valueType);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_JSON_CAUSE_FORMAT, jsonString, e.getMessage()), e);
        }
    }

    public static <T> T parseJsonIgnoreUnknown(final String jsonString,
                                  final Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(jsonString, valueType);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to parse json: [%s], because of %s", jsonString, e.getMessage()), e);
        }
    }

    public static <T> T parseJsonToGenericType(final String jsonString,
                                               final TypeReference<T> typeReference) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_PARSE_JSON_CAUSE_FORMAT, jsonString, e.getMessage()), e);
        }
    }

    public static String convertObjToJsonString(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_OBJECT_TO_JSON_CAUSE_FORMAT, obj, e.getMessage()), e);
        }
    }

    public static boolean isValidJsonString(String jsonString) {
        try {
            new JSONObject(jsonString);
            return true;
        } catch (Exception e) {
            LOGGER.debug("An error occurred during JSON validation", e);
            return false;
        }
    }

    public static void setManageElementIdIfNotPresent(Map<String, Object> ossParam, VnfInstance vnfInstance) {
        if (ossParam.get(NETWORK_ELEMENT_ID) != null && !ossParam.get(NETWORK_ELEMENT_ID).toString().isEmpty()) {
            ossParam.put(MANAGED_ELEMENT_ID, ossParam.get(NETWORK_ELEMENT_ID));
        } else if (ossParam.get(MANAGED_ELEMENT_ID) == null || ossParam.get(MANAGED_ELEMENT_ID).toString().isEmpty()) {
            ossParam.put(MANAGED_ELEMENT_ID, String.format(MANAGED_ELEMENT_ID_NAME, vnfInstance.getClusterName(),
                    vnfInstance.getNamespace(), vnfInstance.getVnfInstanceName()));
        }
    }

    public static Map<String, Object> copyParametersMap(final Object originalParametersMap) {
        Map<String, Object> parametersMap = new HashMap<>();
        if (originalParametersMap != null) {
            parametersMap.putAll(checkAndCastObjectToMap(originalParametersMap));
        }
        return parametersMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getOperationParamsWithoutDay0Configuration(String lifecycleOperationParams) {
        if (Strings.isNullOrEmpty(lifecycleOperationParams)) {
            return null; // NOSONAR
        }

        Map<String, Object> operationParameters = convertStringToJSONObj(lifecycleOperationParams);
        if (operationParameters != null && operationParameters.get(ADDITIONAL_PARAMETERS) != null) {
            Map<String, Object> additionalParams = (Map<String, Object>) operationParameters.get(ADDITIONAL_PARAMETERS);
            Set<String> keysToRemove = additionalParams.keySet().stream()
                    .filter(key -> key.startsWith(DAY0_CONFIGURATION_PREFIX))
                    .collect(Collectors.toSet());
            keysToRemove.forEach(additionalParams::remove);
            operationParameters.put(ADDITIONAL_PARAMETERS, additionalParams);
        }

        return operationParameters;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> checkAndCastObjectToMap(final Object object) {
        if (object == null) {
            return new HashMap<>();
        } else if (object instanceof Map) {
            return (Map<String, Object>) object;
        } else {
            throw new IllegalArgumentException(
                    "Provided object is not a Map of key value pairs: " + object.toString());
        }
    }

    /**
     * Put file content to string with exception logging
     */
    public static String readFileContent(Path value) {
        try {
            return Files.readString(value);
        } catch (IOException ioe) {
            LOGGER.warn("Unable to read file {}", value, ioe);
            return "";
        }
    }

    /**
     * Read file from classpath resources and return content as String
     */
    public static String readFileContent(String pathToFile) throws IOException {
        if (StringUtils.isNotEmpty(pathToFile)) {
            ClassPathResource resource = new ClassPathResource(pathToFile);
            try (InputStream stream = resource.getInputStream()) {
                return new String(stream.readAllBytes());
            }
        } else {
            throw new IllegalArgumentException("Provided path to file is empty or null");
        }

    }

    /**
     * Checks if file under path provided is empty.
     * Always returns <b>false</b> for directories and non-existing files.
     *
     * @return <b>true</b> if file exist, not a directory and empty.
     */
    public static boolean isEmptyFile(Path path) {
        return Files.exists(path)
                && !Files.isDirectory(path)
                && path.toFile().length() == 0;
    }

    /**
     * Put file content to string with exception logging and throws runtime exception then deletes the file from file system
     */
    public static String readFileContentThenDelete(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException ioe) {
            throw new InternalRuntimeException(String.format("Error occurred while reading %s from local file system due to %s", file.getFileName(),
                                                             ioe.getMessage()), ioe);
        } finally {
            deleteFile(file);
        }
    }

    public static StandardProtocolFamily convertStringToProtocol(String ipVersion) {
        if (IP_V4.equalsIgnoreCase(ipVersion)) {
            return StandardProtocolFamily.INET;
        } else if (IP_V6.equalsIgnoreCase(ipVersion)) {
            return StandardProtocolFamily.INET6;
        } else {
            LOGGER.warn("Invalid ipVersion: {}, String values for ipVersion can be: {} or {}.", ipVersion, IP_V4, IP_V6);
            return null;
        }
    }

    public static JSONObject convertXMLToJson(final String xmlAsString, final String fileName) {
        try {
            return XML.toJSONObject(xmlAsString);
        } catch (JSONException e) {
            throw new InvalidInputException(String.format("Invalid XML format:: Error when converting %s file: %s", fileName, e.getMessage()), e);
        }
    }

    public static Path createTempPath(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, "." + suffix);
        } catch (IOException e) {
            throw new InternalRuntimeException("Couldn't create temporary file", e);
        }
    }

    public static String multipartFileToString(final MultipartFile configMultipartFile) {
        try (InputStream is = configMultipartFile.getInputStream()) {
            byte[] content = is.readAllBytes();
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error while trying to read config file with name %s",
                                                             configMultipartFile.getOriginalFilename()), e);
        }
    }

    public static ByteArrayOutputStream readFromByteArray(byte[] bytes) {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return readFromInputStream(inputStream);
    }

    public static ByteArrayOutputStream readFromInputStream(InputStream inputStream) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            for (int length = 0; length >= 0; length = inputStream.read(buffer)) {
                stream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new InternalRuntimeException("Failed to read from byte array", e);
        }
        return stream;
    }

    public static String addConfigExtension(final String clusterName) {
        if (null == clusterName) {
            return null;
        }
        return clusterName.contains(CONFIG_EXTENSION) ? clusterName : clusterName + CONFIG_EXTENSION;
    }

    public static int compareClusterConfigNames(String configName1, String configName2) {
        if (DEFAULT.equals(configName1)) {
            return -1;
        }
        if (DEFAULT.equals(configName2)) {
            return 1;
        }
        return configName1.compareToIgnoreCase(configName2);
    }

    public static boolean isRunning(List<VimLevelAdditionalResourceInfo> vimLevelResourceInfoList) {
        return vimLevelResourceInfoList.stream()
                .allMatch(item -> StringUtils.equalsAnyIgnoreCase(item.getStatus(), "Running", "Succeeded"));
    }

    public static boolean hasRedisBusyException(RedisSystemException exception) {
        return Optional.ofNullable(exception.getCause())
                .map(Object::getClass)
                .map(RedisBusyException.class::isInstance)
                .orElse(false);
    }

    public static <K, V> Map<K, V> putAll(Map<K, V> currentMap, Map<K, V> newMap) {
        Map<K, V> resultMap = new HashMap<>();
        resultMap.putAll(currentMap);
        resultMap.putAll(newMap);
        return resultMap;
    }

    public static void checkPackageOperationalState(PackageResponse packageResponse) {
        if (OperationalState.DISABLED.equals(packageResponse.getOperationalState())) {
            throw new UnprocessablePackageException(
                    String.format("Package %s rejected due to its %s state", packageResponse.getVnfdId(), packageResponse.getOperationalState()));
        }
    }

    public static Map<String, Object> convertClusterConfigToMap(ClusterConfig clusterConfig) {
        if (clusterConfig == null) {
            return new HashMap<>();
        } else {
            return new ObjectMapper().convertValue(clusterConfig, new TypeReference<>() { });
        }
    }

}
