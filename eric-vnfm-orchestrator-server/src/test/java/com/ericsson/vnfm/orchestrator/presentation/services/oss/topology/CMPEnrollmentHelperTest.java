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
package com.ericsson.vnfm.orchestrator.presentation.services.oss.topology;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.CERTM_ADDITIONAL_LLS_CERTIFICATE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class CMPEnrollmentHelperTest extends AbstractDbSetupTest {

    @Autowired
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    @MockBean
    private OssNodeService ossNodeService;

    @Test
    public void testAddNodeToENMWithEnrollmentSuccess() throws IOException, URISyntaxException {
        VnfInstance vnfInstance = createVnfInstance(false);
        Map<String, Object> additionalParams = createAdditionalParams();

        String expectedLdapConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validLdapConfig.json");
        String expectedEnrollmentConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentConfig.json");
        when(ossNodeService.generateLDAPServerConfiguration(any(), any())).thenReturn(expectedLdapConfiguration);
        when(ossNodeService.generateCertificateEnrollmentConfiguration(any())).thenReturn(expectedEnrollmentConfiguration);

        cmpEnrollmentHelper.addNodeToENMWithEnrollment(vnfInstance, additionalParams);

        verify(ossNodeService).addNode(vnfInstance);
        verify(ossNodeService).generateLDAPServerConfiguration(any(), any());
        verify(ossNodeService).generateCertificateEnrollmentConfiguration(any());
        assertTrue(additionalParams.containsKey(DAY0_CONFIGURATION_SECRETS));
        Map<String, Object> secrets = (Map<String, Object>)additionalParams.get(DAY0_CONFIGURATION_SECRETS);
        assertEquals(2, secrets.size());
        assertTrue(secrets.containsKey(EXTERNAL_LDAP_SECRET_NAME));
        assertTrue(secrets.containsKey(ENROLLMENT_CERTM_SECRET_NAME));
        assertEquals(Map.of(EXTERNAL_LDAP_SECRET_KEY, expectedLdapConfiguration), secrets.get(EXTERNAL_LDAP_SECRET_NAME));
        assertEquals(Map.of(ENROLLMENT_CERTM_SECRET_KEY, expectedEnrollmentConfiguration), secrets.get(ENROLLMENT_CERTM_SECRET_NAME));
    }

    @Test
    public void testAddNodeToENMWithEnrollmentFailure() {
        VnfInstance vnfInstance = createVnfInstance(false);
        Map<String, Object> additionalParams = createAdditionalParams();

        when(ossNodeService.generateOssNodeProtocolFile(any(), any())).thenThrow(RuntimeException.class);
        vnfInstance.setAddedToOss(true);

        assertThrows(InternalRuntimeException.class,
                () -> cmpEnrollmentHelper.addNodeToENMWithEnrollment(vnfInstance, additionalParams));

        verify(ossNodeService).addNode(vnfInstance);
        verify(ossNodeService).deleteNodeFromENM(vnfInstance, false);
        assertFalse(vnfInstance.isAddedToOss());
    }

    @Test
    public void testAddNodeToENMWithEnrollmentForSpecificNodeSuccess() throws IOException, URISyntaxException {
        VnfInstance vnfInstance = createVnfInstance(false);
        vnfInstance.setVnfProductName("EXILIS-VDU");
        Map<String, Object> additionalParams = createAdditionalParams();

        String expectedLdapConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validLdapConfig.json");
        String expectedEnrollmentConfiguration = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentConfig.json");
        when(ossNodeService.generateLDAPServerConfiguration(any(), any())).thenReturn(expectedLdapConfiguration);
        when(ossNodeService.generateCertificateEnrollmentConfiguration(any())).thenReturn(expectedEnrollmentConfiguration);

        cmpEnrollmentHelper.addNodeToENMWithEnrollment(vnfInstance, additionalParams);

        assertTrue(additionalParams.containsKey(DAY0_CONFIGURATION_SECRETS));
        Map<String, Object> secrets = (Map<String, Object>)additionalParams.get(DAY0_CONFIGURATION_SECRETS);
        assertEquals(2, secrets.size());
        assertTrue(secrets.containsKey(EXTERNAL_LDAP_SECRET_NAME));
        assertTrue(secrets.containsKey(ENROLLMENT_CERTM_SECRET_NAME));
        Map<String, String> certmSecret = (Map<String, String>)secrets.get(ENROLLMENT_CERTM_SECRET_NAME);
        assertTrue(certmSecret.get(ENROLLMENT_CERTM_SECRET_KEY).contains(CERTM_ADDITIONAL_LLS_CERTIFICATE_NAME));
    }

    private static Map<String, Object> createAdditionalParams() {
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology", Collections.singletonMap("name", "testTopology"));
        additionalParams.put("ipVersion", "ipV4");
        additionalParams.put("addNodeToOSS", "true");
        additionalParams.put("CMPv2Enrollment", "true");
        return additionalParams;
    }

}
