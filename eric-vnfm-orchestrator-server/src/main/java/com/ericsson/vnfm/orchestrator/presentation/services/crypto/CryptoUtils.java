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
package com.ericsson.vnfm.orchestrator.presentation.services.crypto;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

@Slf4j
@Service
public class CryptoUtils {
    static final String SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE = "sensitive data key and value can't be null";
    static final String SENSITIVE_DATA_KEY_NULL_ERROR_MESSAGE = "sensitive data key can't be null";
    static final String VNF_INSTANCE_IS_NULL_ERROR_MESSAGE = "Provided vnf instance is null";
    static final String KEY_ALREADY_PRESENT_ERROR_MESSAGE = "%s already present in the sensitive info";

    /**
     * Returns the value for the key provided, returns null if the key is not present
     *
     * @param key         key
     * @param vnfInstance vnfInstance
     * @return String value of the key, null if key is not present
     */
    public String getDecryptDetailsForKey(String key, VnfInstance vnfInstance) {
        checkNullValues(key, vnfInstance);
        Map<String, String> sensitiveInfoMap = getSensitiveInfoAsMap(vnfInstance);
        return sensitiveInfoMap.get(key);
    }

    /**
     * Set the new key in the sensitiveInfo of the VnfInstance
     *
     * @param key         key
     * @param value       value
     * @param vnfInstance vnfInstance
     * @throws IllegalArgumentException if key is already present
     */
    public void setEncryptDetailsForKey(String key, String value, VnfInstance vnfInstance) {
        Consumer<String> setMessage = s -> {
            throw new IllegalArgumentException(String.format(KEY_ALREADY_PRESENT_ERROR_MESSAGE, s));
        };
        changeEncryptDetailsForKey(key, value, vnfInstance, setMessage);
    }

    /**
     * Updates the value of the key provided in the SensitiveInfo.
     *
     * @param key         key
     * @param value       value
     * @param vnfInstance vnfInstance
     * @throws IllegalArgumentException if any parameter is null
     *                                  Updates the key if present
     *                                  if key is missing inserts key.
     */
    public void updateEncryptDetailsForKey(String key, String value, VnfInstance vnfInstance) {
        Consumer<String> setMessage = s -> LOGGER.info("Provided key present in the sensitive info data: {}", s);
        changeEncryptDetailsForKey(key, value, vnfInstance, setMessage);
    }

    private void changeEncryptDetailsForKey(String key, String value, VnfInstance vnfInstance,
                                                   Consumer<String> setCondition) {
        checkNullValues(key, value, vnfInstance);
        Map<String, String> sensitiveInfoAsMap = getSensitiveInfoAsMap(vnfInstance);
        if (sensitiveInfoAsMap.put(key, value) != null) {
            setCondition.accept(key);
        }
        String newSensitiveInfoAsJson = convertObjToJsonString(sensitiveInfoAsMap);
        vnfInstance.setSensitiveInfo(newSensitiveInfoAsJson);
    }

    private void checkNullValues(String key, String value, VnfInstance vnfInstance) {
        if (Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(value)) {
            throw new IllegalArgumentException(SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE);
        }
        checkVnfInstance(vnfInstance);
    }

    private void checkNullValues(String key, VnfInstance vnfInstance) {
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException(SENSITIVE_DATA_KEY_NULL_ERROR_MESSAGE);
        }
        checkVnfInstance(vnfInstance);
    }

    private void checkVnfInstance(VnfInstance vnfInstance) {
        if (vnfInstance == null) {
            throw new IllegalArgumentException(VNF_INSTANCE_IS_NULL_ERROR_MESSAGE);
        }
    }

    private Map<String, String> getSensitiveInfoAsMap(VnfInstance vnfInstance) {
        String sensitiveInfo = vnfInstance.getSensitiveInfo();
        if (Strings.isNullOrEmpty(sensitiveInfo)) {
            return new HashMap<>();
        } else {
            return parseJsonToGenericType(sensitiveInfo, new TypeReference<HashMap<String, String>>() {
            });
        }
    }
}
