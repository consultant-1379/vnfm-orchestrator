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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DOWNSIZE_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_FILE_PREFIX;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.createTempPath;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.vnfm.orchestrator.model.HttpFileResource;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidFileException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * This class contains methods for working with Yaml
 */
@Slf4j
public final class YamlUtility {

    private static final Pattern ESCAPED_DOT_REGEX_PATTERN = Pattern.compile("(?<!\\\\)\\.");
    private static final String BACKSLASH_DOT_DELIMITER = "\\.";
    private static final String DOT_DELIMITER = ".";
    public static final String YAML_PARSER_ERROR_MSG = "Unable to parse the yaml file";
    private static final String CONTAINS_INVALID_YAML = "Values file contains invalid YAML. ";
    private static final String FILE_CANNOT_BE_EMPTY = "Values file cannot be empty";
    private static final String INVALID_YAML_FORMAT = "Invalid YAML format. ";

    private YamlUtility() {
    }

    /**
     * Take a string of escaped Json and return a string of Yaml
     *
     * @param escapedJson
     * @return a string of Yaml
     */
    public static String convertEscapedJsonToYaml(final String escapedJson) {
        try {
            Map<String, Object> parsedJson = parseJsonToYamlMapIfNotNull(escapedJson);
            return convertMapToYamlFormat(parsedJson);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Contents of values.yaml additional parameter are malformed. " + e.getMessage(), e);
        }
    }

    /**
     * Read a Yaml file into a String
     *
     * @param toValuesFile
     * @return
     */
    public static Optional<String> getValuesString(Path toValuesFile) {
        if (toValuesFile != null) {
            try {
                String yaml = Files.readString(toValuesFile);
                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                Object obj = yamlReader.readValue(yaml, Object.class);
                ObjectMapper jsonWriter = new ObjectMapper();
                return Optional.of(jsonWriter.writeValueAsString(obj));
            } catch (IOException ioe) {
                LOGGER.warn("Unable to read file " + toValuesFile, ioe);
            }
        }
        return Optional.empty();
    }

    /**
     * Take a Yaml document in map format and convert it to a String
     *
     * @param values
     * @return
     */
    public static String convertMapToYamlFormat(final Map<String, Object> values) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(Integer.MAX_VALUE);
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
        return yaml.dump(values);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> removeDotDelimitersFromYamlMap(Map<String, Object> mapWithDotDelimiters) {
        if (!CollectionUtils.isEmpty(mapWithDotDelimiters)) {
            Map<String, Object> resultMap = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : mapWithDotDelimiters.entrySet()) {
                String key = entry.getKey();
                if (key.contains(".")) {
                    splitAndPutParameterWithDelimiter(mapWithDotDelimiters, resultMap, key);
                } else if (mapWithDotDelimiters.get(key) instanceof Map) {
                    resultMap.put(key, removeDotDelimitersFromYamlMap((Map<String, Object>) mapWithDotDelimiters.get(key)));
                } else {
                    resultMap.put(key, mapWithDotDelimiters.get(key));
                }
            }
            return resultMap;
        }
        return mapWithDotDelimiters;
    }

    private static void splitAndPutParameterWithDelimiter(Map<String, Object> mapWithDotDelimiters,
                                                          Map<String, Object> resultMap, String keyWithDelimiters) {
        String[] keyChain = Arrays.stream(keyWithDelimiters.split(ESCAPED_DOT_REGEX_PATTERN.pattern()))
                .map(key -> key.replace(BACKSLASH_DOT_DELIMITER, DOT_DELIMITER))
                .toArray(String[]::new);
        Object originalParamValue = mapWithDotDelimiters.get(keyWithDelimiters);
        int nestingDepthNumber = getContainedKeysNestingDepthNumber(resultMap, keyChain);
        Map<String, Object> nestedExistingMap = extractNestedMap(resultMap, keyChain, nestingDepthNumber);

        for (int i = keyChain.length - 1; i > nestingDepthNumber; i--) {
            Map<String, Object> mapWrapper = new LinkedHashMap<>();
            mapWrapper.put(keyChain[i], originalParamValue);
            originalParamValue = mapWrapper;
        }
        nestedExistingMap.put(keyChain[nestingDepthNumber], originalParamValue);
    }

    /**
     * Calculates the nesting depth of already inserted keys, in case the key parameters are repeated.
     *
     * @param keyChainMap - a map that potentially already contains keys from the chain.
     * @param keyChain    - a chain potentially already contained in the map, possibly partially.
     * @return - the number of keys already contained in the map.
     */
    @SuppressWarnings("unchecked")
    private static int getContainedKeysNestingDepthNumber(Map<String, Object> keyChainMap, String[] keyChain) {
        int nestingDepthNumber = 0;
        Map<String, Object> iterativeKeyChainMap = keyChainMap;
        for (int i = 0; i < keyChain.length - 1
                && iterativeKeyChainMap.containsKey(keyChain[i])
                && iterativeKeyChainMap.get(keyChain[i]) instanceof Map; i++) {
            iterativeKeyChainMap = (Map<String, Object>) iterativeKeyChainMap.get(keyChain[i]);
            nestingDepthNumber++;
        }
        return nestingDepthNumber;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractNestedMap(Map<String, Object> keyChainMap,
                                                       String[] keyChain, int nestingDepthNumber) {
        Map<String, Object> resultNestedMap = keyChainMap;
        for (int j = 0; j < nestingDepthNumber; j++) {
            resultNestedMap = (Map<String, Object>) resultNestedMap.get(keyChain[j]);
        }
        return resultNestedMap;
    }

    public static Map<String, Object> validateYamlFileAndConvertToMap(final MultipartFile values) {
        try (InputStream inputStream = values.getInputStream()) {
            Object config = convertAndValidateInputStreamIntoYamlString(inputStream);
            return checkAndCastObjectToMap(config);
        } catch (final IOException e) {
            throw new IllegalArgumentException(YAML_PARSER_ERROR_MSG, e);
        }
    }

    public static Map<String, Object> convertYamlFileIntoMap(final Path yamlFile) {
        if (yamlFile == null) {
            return new HashMap<>();
        }

        try (InputStream inputStream = Files.newInputStream(yamlFile)) {
            Object config = convertInputStreamIntoYamlString(inputStream);
            return checkAndCastObjectToMap(config);
        } catch (IOException e) {
            throw new IllegalArgumentException(YAML_PARSER_ERROR_MSG, e);
        }
    }

    public static JSONObject convertYamlFileIntoJson(final Path yamlFile) {
        final Map<String, Object> yamlFileMap = convertYamlFileIntoMap(yamlFile);
        return new JSONObject(yamlFileMap);
    }

    private static Object convertInputStreamIntoYamlString(InputStream inputStream) {
        final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        try {
            return yaml.load(inputStream);
        } catch (final Exception e) {
            throw new InvalidFileException(CONTAINS_INVALID_YAML, e);
        }
    }

    private static Object convertAndValidateInputStreamIntoYamlString(InputStream inputStream) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        final Yaml yaml = new Yaml(new SafeConstructor(loaderOptions),
                                   new Representer(new DumperOptions()),
                                   new DumperOptions(),
                                   loaderOptions);

        Object config;

        try {
            config = yaml.load(inputStream);
        } catch (final Exception e) {
            throw new InvalidFileException(CONTAINS_INVALID_YAML, e);
        }

        if (config == null) {
            throw new InvalidFileException(FILE_CANNOT_BE_EMPTY);
        }
        if (!(config instanceof Map)) {
            throw new InvalidFileException(CONTAINS_INVALID_YAML);
        }
        return config;
    }

    public static Resource convertYamlStringIntoResource(String yaml, String name) {
        return new HttpFileResource(yaml.getBytes(StandardCharsets.UTF_8), name);
    }

    public static Map<String, Object> convertYamlStringIntoMap(final String yamlContent) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try {
            return yaml.load(yamlContent);
        } catch (final Exception e) {
            throw new InvalidInputException(INVALID_YAML_FORMAT, e);
        }
    }

    public static JSONObject convertYamlStringIntoJson(final String yamlContent) {
        Map<String, Object> map = convertYamlStringIntoMap(yamlContent);
        return new JSONObject(map);
    }

    public static Map<String, Object> parseJsonToYamlMapIfNotNull(String escapedJson) {
        return Objects.isNull(escapedJson)
                ? Collections.emptyMap()
                : parseJsonToGenericType(escapedJson, new TypeReference<>() { });
    }

    public static Path writeStringToValuesFile(final String output) {
        Path valuesFile = createValuesFile();
        return writeStringToValuesFile(output, valuesFile);
    }

    public static Path writeStringToValuesFile(final String output, final Path valuesFilePath) {
        Path valuesFile = createValuesFileIfNeeded(valuesFilePath);
        try {
            Files.write(valuesFile, output.getBytes(StandardCharsets.UTF_8));
            return valuesFile;
        } catch (IOException e) {
            throw new InternalRuntimeException("Unable to store values file", e);
        }
    }

    public static Path writeMapToValuesFile(final Map<String, Object> valuesMap) {
        Path valuesFile = createValuesFile();
        return writeMapToValuesFile(valuesMap, valuesFile);
    }

    public static Path writeMapToValuesFile(final Map<String, Object> valuesMap, final Path valuesFilePath) {
        Path valuesFile = createValuesFileIfNeeded(valuesFilePath);
        Map<String, Object> sanitizedMap = sanitizeMap(valuesMap);
        String output = convertMapToYamlFormat(sanitizedMap);
        return writeStringToValuesFile(output, valuesFile);
    }

    public static Path mergeJsonObjectAndStringAndWriteToValuesFile(final Map<String, Object> values, String scaleInfo) {
        Path valuesFile = createValuesFile();
        String output = convertMapToYamlFormat(values).concat(scaleInfo);
        return writeStringToValuesFile(output, valuesFile);
    }

    public static Path createValuesFileIfNeeded(Path valuesFile) {
        if (valuesFile == null) {
            return createValuesFile();
        }
        return valuesFile;
    }

    public static Path createValuesFile() {
        return createTempPath(VALUES_FILE_PREFIX, "yaml");
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> valuesMap) {
        if (CollectionUtils.isEmpty(valuesMap)) {
            return valuesMap;
        }
        return valuesMap.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(ValuesFileComposer::mergeParamPredicate)
                .map(YamlUtility::sanitizeComplexValues)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<String, Object> sanitizeComplexValues(Map.Entry<String, Object> entry) {
        String[] keySequence = DOWNSIZE_VNFD_KEY.split("\\.");
        return sanitizeComplexValueEntry(entry, keySequence);
    }

    public static Map.Entry<String, Object> sanitizeComplexValueEntry(Map.Entry<String, Object> entry, String[] keySequence) {
        String currentKey = keySequence[0];
        if (entry.getKey().equals(currentKey) && entry.getValue() instanceof Map) {
            Map<String, Object> nextElement = sanitizeComplexValueMap((Map<String, Object>) entry.getValue(),
                                                                      Arrays.copyOfRange(keySequence, 1, keySequence.length));
            if (nextElement != null) {
                entry.setValue(nextElement);
            } else {
                return null;
            }
        }
        return entry;
    }

    private static Map<String, Object> sanitizeComplexValueMap(Map<String, Object> map, String[] keySequence) {
        String currentKey = keySequence[0];
        if (map.containsKey(currentKey)) {
            if (keySequence.length > 1) {
                processMapValue(map, keySequence, currentKey);
            } else {
                map.remove(currentKey);
            }
        }
        return map.isEmpty() ? null : map;
    }

    private static void processMapValue(final Map<String, Object> map, final String[] keySequence, final String currentKey) {
        if (map.get(currentKey) instanceof Map) {
            Map<String, Object> value = sanitizeComplexValueMap((Map<String, Object>) map.get(currentKey),
                                                                Arrays.copyOfRange(keySequence, 1, keySequence.length));
            if (value == null) {
                map.remove(currentKey);
            }
        }
    }
}
