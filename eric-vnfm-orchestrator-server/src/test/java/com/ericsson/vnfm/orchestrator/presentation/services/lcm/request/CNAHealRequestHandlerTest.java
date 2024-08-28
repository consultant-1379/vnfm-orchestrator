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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;


@SpringBootTest(classes = { CNAHealRequestHandler.class, Day0ConfigurationService.class })
@ActiveProfiles("test")
public class CNAHealRequestHandlerTest {

    @Autowired
    private CNAHealRequestHandler cnaHealRequestHandler;

    private HealVnfRequest healVnfRequest;
    private VnfInstance vnfInstance;

    @BeforeEach
    public void setup() {
        healVnfRequest = new HealVnfRequest();
        vnfInstance = new VnfInstance();
    }

    @Test
    public void testMissingRestoreBackupNameThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        "DUMMY", "DUMMY"
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidHealRequestException.class)
                .hasMessageContaining("Invalid CNA Restore request. restore.backupName is not present or an invalid value.");
    }

    @Test
    public void testMissingRestoreScopeThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        RESTORE_BACKUP_NAME, "DUMMY"
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidHealRequestException.class)
                .hasMessageContaining("Invalid CNA Restore request. restore.scope is not present or an invalid value.");
    }

    @Test
    public void testInvalidRequiredParamsThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        RESTORE_BACKUP_NAME, ""
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidHealRequestException.class)
                .hasMessageContaining("Invalid CNA Restore request. restore.backupName is not present or an invalid value.");
    }

    @Test
    public void testDay0ConfigurationContainsNoValueThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        "restore.backupName", "test",
                        "restore.scope", "test",
                        "day0.configuration.secretname", "secret",
                        "day0.configuration.param1.key", "key"
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Invalid Day0 configuration. At least one key-value pair is needed.");
    }

    @Test
    public void testDay0ConfigurationContainsNoKeyThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        "restore.backupName", "test",
                        "restore.scope", "test",
                        "day0.configuration.secretname", "secret",
                        "day0.configuration.param1.value", "value"
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Invalid Day0 configuration. At least one key-value pair is needed.");
    }

    @Test
    public void testAllDay0ParamsAreRemovedIfSecretNameIsAbsent() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.param1.key", "key");
        additionalParameter.put("day0.configuration.param1.value", "value");

        healVnfRequest.setAdditionalParams(additionalParameter);
        cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);

        assertEquals(2, additionalParameter.keySet().size());
    }

    @Test
    public void testRequiredNumberOfDay0ConfigurationParamsThrowsNoException() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "secret");
        additionalParameter.put("day0.configuration.param100.key", "key");
        additionalParameter.put("day0.configuration.param100.value", "value");

        healVnfRequest.setAdditionalParams(additionalParameter);

        assertThatNoException().isThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest));
    }

    @Test
    public void testDay0ConfigurationParamsWrongNamesThrowsException() {
        healVnfRequest.setAdditionalParams(
                Map.of(
                        "restore.backupName", "test",
                        "restore.scope", "test",
                        "day0.configuration.param100.key", "key",
                        "day0.configuration.param5.value", "value",
                        "day0.configuration.secretname", "secret"
                )
        );
        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Invalid Day0 configuration. At least one key-value pair is needed.");
    }

    @Test
    public void testMinimalRequiredParamsThrowsNoException() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");
        healVnfRequest.setAdditionalParams(additionalParameter);

        assertThatNoException().isThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest));
    }

    @Test
    public void testMultipleDay0ParamsThrowsNoException() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "secret");
        additionalParameter.put("day0.configuration.param1.key", "key1");
        additionalParameter.put("day0.configuration.param1.value", "value1");
        additionalParameter.put("day0.configuration.param2.key", "key2");
        additionalParameter.put("day0.configuration.param2.value", "value2");

        healVnfRequest.setAdditionalParams(additionalParameter);
        assertThatNoException().isThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest));
    }

    @Test
    public void testMultipleDay0ParamsWithDuplicatedKeysThrowsException() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");

        //adding day0 configuration
        additionalParameter.put("day0.configuration.secretname", "secret");
        additionalParameter.put("day0.configuration.param1.key", "key");
        additionalParameter.put("day0.configuration.param1.value", "value");
        additionalParameter.put("day0.configuration.param2.key", "key");
        additionalParameter.put("day0.configuration.param2.value", "value");

        healVnfRequest.setAdditionalParams(additionalParameter);

        assertThatThrownBy(() -> cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Duplicate keys within secret name 'secret' in Day0Configuration");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleDay0ParamsNewFormat() {
        Map<String, Object> additionalParameter = new HashMap<>();
        additionalParameter.put("restore.backupName", "test");
        additionalParameter.put("restore.scope", "test");

        //adding day0 configuration
        Map<String, Object> secretContent1 = new HashMap<>();
        secretContent1.put("username", "John");
        secretContent1.put("password", "hashpassword");
        secretContent1.put("others", 3);
        Map<String, Map<String, Object>> secrets = new HashMap<>();
        secrets.put("secret1", secretContent1);

        Map<String, Object> secretContent2 = new HashMap<>();
        secretContent2.put("username", "TestKey");
        secretContent2.put("password", "TestPass");
        secrets.put("secret2", secretContent2);

        Map<String, Object> secretContent3 = new HashMap<>();
        secretContent3.put("username", "TestKey2");
        secretContent3.put("password", "TestPass2");
        secrets.put("secret3", secretContent3);

        additionalParameter.put(DAY0_CONFIGURATION_SECRETS, secrets);

        healVnfRequest.setAdditionalParams(additionalParameter);
        cnaHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);

        assertEquals(3, additionalParameter.keySet().size());
        assertThat(additionalParameter.keySet()).containsExactlyInAnyOrder("restore.backupName", "restore.scope", DAY0_CONFIGURATION_PREFIX);

        Map<String, Object> day0Params = (Map<String, Object>) additionalParameter.get(DAY0_CONFIGURATION_PREFIX);
        assertThat(day0Params.keySet()).containsExactlyInAnyOrder("secret1", "secret2", "secret3");
    }
}