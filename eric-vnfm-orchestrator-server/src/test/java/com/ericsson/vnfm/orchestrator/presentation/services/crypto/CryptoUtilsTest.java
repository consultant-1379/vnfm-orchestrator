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
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.HashMap;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        CryptoUtils.class
})
public class CryptoUtilsTest {

    @Autowired
    private CryptoUtils cryptoUtils;

    @Test
    public void testSetEncryptDetails() {
        VnfInstance instance = new VnfInstance();
        String key = "test-key";
        String value = "test-value";
        cryptoUtils.setEncryptDetailsForKey(key, value, instance);
        assertThat(instance.getSensitiveInfo()).isEqualTo("{\"test-key\":\"test-value\"}");
        String sensitiveInfoAfterString = instance.getSensitiveInfo();
        Map sensitiveInfoAfter = parseJsonToGenericType(sensitiveInfoAfterString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(sensitiveInfoAfter.get(key)).isEqualTo(value);
        assertThat(sensitiveInfoAfter.keySet().size()).isEqualTo(1);
    }

    @Test
    public void testSetEncryptDetailsWithExistingData() {
        String sensitiveInfo = "{\"test-key-1\":\"test-value-1\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(sensitiveInfo);
        cryptoUtils.setEncryptDetailsForKey("test-key-2", "test-value-2", instance);
        assertThat(instance.getSensitiveInfo()).isNotBlank();
        String sensitiveInfoAfterString = instance.getSensitiveInfo();
        Map sensitiveInfoAfter = parseJsonToGenericType(sensitiveInfoAfterString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(sensitiveInfoAfter.get("test-key-1")).isEqualTo("test-value-1");
        assertThat(sensitiveInfoAfter.get("test-key-2")).isEqualTo("test-value-2");
        assertThat(sensitiveInfoAfter.size()).isEqualTo(2);
        assertThat(sensitiveInfoAfterString).isEqualTo("{\"test-key-1\":\"test-value-1\",\"test-key-2\":\"test-value-2\"}");
    }

    @Test
    public void testSetEncryptDetailsWithKeyIsNull() {
        VnfInstance instance = new VnfInstance();
        assertThatThrownBy(() -> cryptoUtils.setEncryptDetailsForKey(null, "test-value", instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testSetEncryptDetailsWithValueIsNull() {
        VnfInstance instance = new VnfInstance();
        assertThatThrownBy(() -> cryptoUtils.setEncryptDetailsForKey("test-key", null, instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testSetEncryptDetailsWithInstanceAsNull() {
        assertThatThrownBy(() -> cryptoUtils.setEncryptDetailsForKey("test-key", "test-value", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .VNF_INSTANCE_IS_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testSetEncryptDetailsWithUpdatingExistingData() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(plainString);
        assertThatThrownBy(() -> cryptoUtils.setEncryptDetailsForKey("test-key", "test-value", instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(String.format(CryptoUtils
                                .KEY_ALREADY_PRESENT_ERROR_MESSAGE,
                        "test-key"));
    }

    @Test
    public void testGetDecryptDetailsForKey() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(plainString);
        String value = cryptoUtils.getDecryptDetailsForKey("test-key", instance);
        assertThat(value).isEqualTo("test-value");
    }

    @Test
    public void testGetDecryptDetailsForKeyWithKeyNotPresent() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(plainString);
        String value = cryptoUtils.getDecryptDetailsForKey("test-key-1", instance);
        assertThat(value).isNull();
    }

    @Test
    public void testGetDecryptDetailsForKeyWithKeyIsNull() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(plainString);
        assertThatThrownBy(() -> cryptoUtils.getDecryptDetailsForKey(null, instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .SENSITIVE_DATA_KEY_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testGetDecryptDetailsForKeyWithVnfInstanceIsNull() {
        assertThatThrownBy(() -> cryptoUtils.getDecryptDetailsForKey("test-key", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .VNF_INSTANCE_IS_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testUpdateCryptDetailsForKey() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        instance.setSensitiveInfo(plainString);
        cryptoUtils.updateEncryptDetailsForKey("test-key", "test-value-1", instance);
        assertThat(instance.getSensitiveInfo()).isNotBlank().isNotNull();
        String sensitiveInfoAfterString = instance.getSensitiveInfo();
        Map sensitiveInfoAfter = parseJsonToGenericType(sensitiveInfoAfterString, new TypeReference<HashMap<String, String>>() {
        });
        assertThat(sensitiveInfoAfter.get("test-key")).isEqualTo("test-value-1");
        assertThat(sensitiveInfoAfter.size()).isEqualTo(1);
        assertThat(sensitiveInfoAfterString).isEqualTo("{\"test-key\":\"test-value-1\"}");
    }

    @Test
    public void testUpdateCryptDetailsForKeyIsNull() {
        VnfInstance instance = new VnfInstance();
        assertThatThrownBy(() -> cryptoUtils.updateEncryptDetailsForKey(null, "test-value", instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testUpdateCryptDetailsForKeyWithValueIsNull() {
        VnfInstance instance = new VnfInstance();
        assertThatThrownBy(() -> cryptoUtils.updateEncryptDetailsForKey("test-key", null, instance))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .SENSITIVE_KEY_AND_VALUE_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testUpdateCryptDetailsForKeyWithInstanceAsNull() {
        assertThatThrownBy(() -> cryptoUtils.updateEncryptDetailsForKey("test-key", "test-value", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(CryptoUtils
                        .VNF_INSTANCE_IS_NULL_ERROR_MESSAGE);
    }

    @Test
    public void testUpdateCryptDetailsForKeyWithSensitiveInfoNull() {
        String plainString = "{\"test-key\":\"test-value\"}";
        VnfInstance instance = new VnfInstance();
        String key = "test-key";
        String value = "test-value";
        cryptoUtils.updateEncryptDetailsForKey(key, value, instance);
        String sensitiveInfoAfterString = instance.getSensitiveInfo();
        JSONObject data = new JSONObject(sensitiveInfoAfterString);
        assertThat(data.keySet().size()).isEqualTo(1);
        assertThat(data.getString(key)).isEqualTo(value);
        assertThat(instance.getSensitiveInfo()).isNotNull().isEqualTo(plainString);
    }
}
