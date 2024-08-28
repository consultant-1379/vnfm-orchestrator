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

import static java.net.StandardProtocolFamily.INET6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_V6;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_PASSWORD;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.UrlValidatorConfig;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;


@SpringBootTest(classes = {CNFHealRequestHandler.class, UrlValidatorConfig.class, CMPEnrollmentHelper.class})
@ActiveProfiles("test")
public class CNFHealRequestHandlerTest {

    private static final String VALID_BACKUP_FILE_REFERENCE = "sftp://users@14BCP04/my-backup";

    private static final String PASSWORD = "password";

    private static final String RESULT_PROTOCOL = "some protocol";

    private static final String PASSWORD_ERROR_MASSAGE = "Password cannot be null or empty";

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private TerminateRequestHandler terminateRequestHandler;

    @Autowired
    private UrlValidator urlValidator;

    @Autowired
    private CNFHealRequestHandler cnfHealRequestHandler;

    @Autowired
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    private VnfInstance vnfInstance;

    private HealVnfRequest healVnfRequest;


    @BeforeEach
    public void setup() {
        healVnfRequest = new HealVnfRequest();
        vnfInstance = new VnfInstance();
    }

    @Test
    public void invalidBackupFileRefThrowsException() {
        assertThatThrownBy(() -> {
            cnfHealRequestHandler.validateBackupParams("vnbrkep", PASSWORD);
        })
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("is not an acceptable value for backupFileReference. It must either be \"Latest\" or a valid url");
    }

    @Test
    public void validBackupFilRefDoesNotThrowException() {
        assertThatNoException().isThrownBy(() -> cnfHealRequestHandler.validateBackupParams(
              VALID_BACKUP_FILE_REFERENCE, PASSWORD));
    }

    @Test
    public void invalidBackupPasswordThrowsException() {
        assertThatThrownBy(() -> {
            cnfHealRequestHandler.validateBackupParams(VALID_BACKUP_FILE_REFERENCE, "");
        })
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining(PASSWORD_ERROR_MASSAGE);
    }

    @Test
    public void validBackupFileRefLatestDoesNotThrowException() {
        assertThatNoException().isThrownBy(() -> cnfHealRequestHandler.validateBackupParams("latest", ""));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepareTerminateRequestTest() {
        //given
        HashMap<String, String> mapOfAdditionalParams = new HashMap<>();
        mapOfAdditionalParams.put(RESTORE_BACKUP_FILE_REFERENCE, "some value");
        mapOfAdditionalParams.put(IP_VERSION, "some value");
        mapOfAdditionalParams.put("Test key", "some value");

        TerminateVnfRequest terminateVnfRequest = new TerminateVnfRequest();
        terminateVnfRequest.setAdditionalParams(mapOfAdditionalParams);

        //then
        assertThat((Map<String, String>) cnfHealRequestHandler.prepareTerminateRequest(terminateVnfRequest, terminateRequestHandler, vnfInstance)
                    .getAdditionalParams())
              .isNotNull()
              .containsKey("Test key")
              .doesNotContainKeys(RESTORE_BACKUP_FILE_REFERENCE, IP_VERSION);
    }

    @Test
    public void specificValidationTest() {
        //given
        healVnfRequest.setAdditionalParams(
              Map.of(RESTORE_BACKUP_FILE_REFERENCE, VALID_BACKUP_FILE_REFERENCE,
                     RESTORE_PASSWORD, PASSWORD,
                     IP_VERSION, IP_V6));

        //when
        when(ossNodeService.generateOssNodeProtocolFile(vnfInstance, INET6)).thenReturn(RESULT_PROTOCOL);
        cnfHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);

        //then
        assertThat(vnfInstance.getOssNodeProtocolFile()).isEqualTo(RESULT_PROTOCOL);
    }

    @Test
    public void specificValidationWithAbsentBackupFileReference() {
        //given
        healVnfRequest.setAdditionalParams(
              Map.of(RESTORE_PASSWORD, PASSWORD,
                     IP_VERSION, IP_V6));

        //when
        when(ossNodeService.generateOssNodeProtocolFile(vnfInstance, INET6)).thenReturn(RESULT_PROTOCOL);
        cnfHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);

        //then
        assertThat(vnfInstance.getOssNodeProtocolFile()).isEqualTo(RESULT_PROTOCOL);
    }

    @Test
    public void specificValidationWithAbsentRestorePassword() {
        //given
        healVnfRequest.setAdditionalParams(
              Map.of(RESTORE_BACKUP_FILE_REFERENCE, VALID_BACKUP_FILE_REFERENCE,
                     IP_VERSION, IP_V6));

        //then
        assertThatThrownBy(() -> {
            cnfHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);
        })
              .isInstanceOf(InvalidInputException.class)
              .hasMessageContaining(PASSWORD_ERROR_MASSAGE);
    }

    @Test
    public void specificValidationTestAbsentWithIpVersion() {
        //given
        healVnfRequest.setAdditionalParams(
              Map.of(RESTORE_BACKUP_FILE_REFERENCE, VALID_BACKUP_FILE_REFERENCE,
                     RESTORE_PASSWORD, PASSWORD));

        //when
        when(ossNodeService.generateOssNodeProtocolFile(vnfInstance, null)).thenReturn(RESULT_PROTOCOL);
        cnfHealRequestHandler.specificValidation(vnfInstance, healVnfRequest);

        //then
        assertThat(vnfInstance.getOssNodeProtocolFile()).isEqualTo(RESULT_PROTOCOL);
    }
}