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
package com.ericsson.vnfm.orchestrator.presentation.services.validator;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETNAME_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.KEY_SUFFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.SECRET_PARAM_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.VALUE_SUFFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Day0ConfigurationService {

    public Map<String, String> retrieveDay0ConfigurationParams(Map<String, Object> additionalParams) {
        Map<String, String> day0ConfigurationParams = new HashMap<>();
        Map<String, String> oldSecretMap = new HashMap<>();

        for (Map.Entry<String, Object> parameter : additionalParams.entrySet()) {
            String parameterKey = parameter.getKey();
            if (parameterKey.equals(DAY0_CONFIGURATION_SECRETNAME_KEY) || (parameterKey.startsWith(SECRET_PARAM_PREFIX))) {
                oldSecretMap.put(parameterKey, String.valueOf(parameter.getValue()));
            } else if (parameterKey.startsWith(DAY0_CONFIGURATION_SECRETS)) {
                validateDay0ConfigurationNewSecret(additionalParams.get(DAY0_CONFIGURATION_SECRETS));
                fillMapWithNewSecrets(day0ConfigurationParams, parameter);
            }
        }

        validateDay0ConfigurationOldSecret(oldSecretMap);
        transformOldSecretToNewSecretFormat(day0ConfigurationParams, oldSecretMap);

        removeSensitiveData(oldSecretMap, additionalParams, day0ConfigurationParams);

        return day0ConfigurationParams;
    }

    private static Map<String, String> validateDay0ConfigurationOldSecret(Map<String, String> oldSecretMap) {
        if (oldSecretMap.containsKey(DAY0_CONFIGURATION_SECRETNAME_KEY)) {
            checkAtLeastOneKeyValuePairExists(oldSecretMap);
        } else {
            LOGGER.info("{} is not present. Removing all Day0 parameters.", DAY0_CONFIGURATION_SECRETNAME_KEY);
            Map<String, String> mapCopy = new HashMap<>(oldSecretMap);
            mapCopy.keySet().removeIf(parameter -> parameter.contains(DAY0_CONFIGURATION_PREFIX));
            return mapCopy;
        }
        return oldSecretMap;
    }

    private static void transformOldSecretToNewSecretFormat(final Map<String, String> day0ConfigurationParams,
                                                            final Map<String, String> oldSecretMap) {
        Map<String, String> secretValues = buildSecret(oldSecretMap);
        String secretName = oldSecretMap.get(DAY0_CONFIGURATION_SECRETNAME_KEY);
        if (secretName != null) {
            if (!day0ConfigurationParams.containsKey(secretName)) {
                day0ConfigurationParams.put(secretName, convertMapToJson(secretValues));
            } else {
                throw new InvalidInputException(String.format("Duplicate secret name '%s' in Day0Configuration", secretName));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void validateDay0ConfigurationNewSecret(Object day0ConfigurationSecretsParam) {
        Map<String, Map<String, Object>> day0ConfigurationSecrets = (Map<String, Map<String, Object>>) day0ConfigurationSecretsParam;
        day0ConfigurationSecrets.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .findAny()
                .ifPresent(s -> {
                    throw new InvalidInputException("Invalid Day0 configuration. At least one key-value pair is needed.");
                });

        day0ConfigurationSecrets.entrySet().stream()
                .filter(Day0ConfigurationService::hasEmptyValues)
                .findAny()
                .ifPresent(s -> {
                    throw new InvalidInputException("Invalid Day0 configuration. Every value in the secret must not be empty.");
                });
    }

    private static boolean hasEmptyValues(final Map.Entry<String, Map<String, Object>> item) {
        return item.getValue().entrySet().stream()
                .anyMatch(entry -> "".equals(entry.getValue()));
    }

    @SuppressWarnings("unchecked")
    private static void fillMapWithNewSecrets(final Map<String, String> day0ConfigurationParams, final Map.Entry<String, Object> parameter) {
        Map<String, Object> secret = (Map<String, Object>) parameter.getValue();
        secret.forEach((k, v) -> day0ConfigurationParams.put(k, convertMapToJson(v)));
    }

    private static Map<String, String> buildSecret(final Map<String, String> oldSecretMap) {
        final Map<String, String> keyParamSecret = oldSecretMap.entrySet().stream()
                .filter(entry -> {
                    final String key = "day0.configuration.param(\\d+).key";
                    return entry.getKey().matches(key);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<String, String> valueParamSecret = oldSecretMap.entrySet().stream()
                .filter(entry -> {
                    final String value = "day0.configuration.param(\\d+).value";
                    return entry.getKey().matches(value);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        checkForDuplicatedKeys(oldSecretMap, keyParamSecret);

        return keyParamSecret.entrySet().stream()
                .map(entry -> Pair.of(String.valueOf(entry.getValue()),
                                      String.valueOf(valueParamSecret.get(entry.getKey().replace("key", "value")))))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private static void checkForDuplicatedKeys(final Map<String, String> oldSecretMap, final Map<String, String> keyParamSecret) {
        getMoreThanOneKey(keyParamSecret)
                .map(Map.Entry::getKey)
                .findAny()
                .ifPresent(item -> {
                    throw new InvalidInputException(String.format("Duplicate keys within secret name '%s' in Day0Configuration",
                                                                  oldSecretMap.get(DAY0_CONFIGURATION_SECRETNAME_KEY)));
                });
    }

    private static Stream<Map.Entry<String, Long>> getMoreThanOneKey(final Map<String, String> keyParamSecret) {
        return keyParamSecret.values().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1);
    }

    private static String convertMapToJson(final Object stringStringMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonDay0ConfigurationParams = null;
        try {
            jsonDay0ConfigurationParams = objectMapper.writeValueAsString(stringStringMap);
        } catch (JsonProcessingException exp) {
            throw new IllegalArgumentException("Unable to parse resource details, Invalid value provided during instantiate", exp);
        }
        return jsonDay0ConfigurationParams;
    }

    private static void removeSensitiveData(final Map<String, String> oldSecretMap, final Map<String, Object> additionalParams,
                                            final Map<String, String> day0ConfigurationParams) {
        oldSecretMap.keySet().forEach(additionalParams::remove);
        day0ConfigurationParams.keySet().forEach(additionalParams::remove);
        additionalParams.remove(DAY0_CONFIGURATION_SECRETS);
    }

    private static void checkAtLeastOneKeyValuePairExists(Map<String, String> additionalParams) {
        Map<String, String> secretValues = new HashMap<>();
        additionalParams.keySet().stream()
                .filter(param -> param.startsWith(SECRET_PARAM_PREFIX) && param.endsWith(KEY_SUFFIX))
                .forEach(paramKey -> {
                    if (validateKeyHasValue(paramKey, additionalParams)) {
                        final String paramValue = paramKey.substring(0, paramKey.length() - KEY_SUFFIX.length()) + VALUE_SUFFIX;
                        secretValues.put(additionalParams.get(paramKey), additionalParams.get(paramValue));
                    }
                });
        if (secretValues.isEmpty()) {
            throw new InvalidInputException("Invalid Day0 configuration. At least one key-value pair is needed.");
        }
    }

    private static boolean validateKeyHasValue(String secretParamKey, Map<String, String> day0Configuration) {
        final String pairingKey = secretParamKey.substring(0, secretParamKey.length() - KEY_SUFFIX.length()) + VALUE_SUFFIX;
        return StringUtils.isNotEmpty(pairingKey) && day0Configuration.containsKey(pairingKey);
    }
}
