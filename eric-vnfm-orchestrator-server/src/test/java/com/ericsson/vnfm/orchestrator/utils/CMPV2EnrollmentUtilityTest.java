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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.CERTM_OAM_ROOT_CERTIFICATE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.BEGIN_CERT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.END_CERT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ENROLLMENT_INFO_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.TRUSTED_CERTIFICATES_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.TRUSTED_CERTIFICATE_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.VERBOSE_ENROLLMENT_INFO_STRING;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CACertificate;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServer;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServerGroup;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServerGroups;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CertificateAuthority;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.Enrollment;

public class CMPV2EnrollmentUtilityTest {

    private final String ENROLLMENT_JSON_FILE = "cmpv2_enrollment.json";

    @Test
    public void testCreateCertificateListFromValidEnrollmentJSON() throws URISyntaxException, IOException {
        JSONObject enrollmentJson = new JSONObject(Files.readString(TestUtils.getResource(ENROLLMENT_JSON_FILE)));
        List<CACertificate> certs = CMPV2EnrollmentUtility.createCertificatesFromEnrollmentJson(enrollmentJson.getJSONObject(ENROLLMENT_INFO_STRING)
                                                                            .getJSONObject(VERBOSE_ENROLLMENT_INFO_STRING)
                                                                            .getJSONObject(TRUSTED_CERTIFICATES_STRING)
                                                                            .getJSONArray(TRUSTED_CERTIFICATE_STRING));
        assertThat(certs).hasSize(1);
        assertThat(certs.get(0).getName()).isEqualTo(CERTM_OAM_ROOT_CERTIFICATE_NAME);
        assertThat(certs.get(0).getPem().contains(BEGIN_CERT) && certs.get(0).getPem().contains(END_CERT)).isTrue();
    }

    @Test
    public void testCreateCertificateListFailsFromInvalidEnrollmentJSON() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("invalid");
        assertThatThrownBy(() -> {
            CMPV2EnrollmentUtility.createCertificatesFromEnrollmentJson(jsonArray);
        }).isInstanceOf(JSONException.class);
    }

    @Test
    public void testCreateCertificateAuthorityFromEnrollment() throws URISyntaxException, IOException{
        JSONObject enrollmentJson = new JSONObject(Files.readString(TestUtils.getResource(ENROLLMENT_JSON_FILE)));
        CertificateAuthority authorities = CMPV2EnrollmentUtility.getAuthorityFromEnrollment(enrollmentJson.getJSONObject(ENROLLMENT_INFO_STRING));
        assertThat(authorities.getCertificateAuthority().size()).isEqualTo(1);
        assertThat(authorities.getCertificateAuthority().get(0).get("name")).contains("OU=Athlone,O=Ericsson,C=IE,CN=NE_OAM_CA");
    }

    @Test
    public void testCreateCMPServerGroupsFromEnrollment() throws URISyntaxException, IOException{
        JSONObject enrollmentJson = new JSONObject(Files.readString(TestUtils.getResource(ENROLLMENT_JSON_FILE)));
        CMPServerGroups serverGroups =
                CMPV2EnrollmentUtility.getCMPServerGroupsFromEnrollment(enrollmentJson.getJSONObject(ENROLLMENT_INFO_STRING));
        assertThat(serverGroups.getCmpServerGroups()).hasSize(1);
        CMPServerGroup group = serverGroups.getCmpServerGroups().get(0);
        assertThat(group.getName()).isEqualTo("enm-cmp-group-1");
        assertThat(group.getCmpServers()).hasSize(1);
        CMPServer server = group.getCmpServers().get(0);
        assertThat(server.getName()).isEqualTo("enm-cmp-server-1");
        assertThat(server.getUri()).isEqualTo("http://127.0.0.1:8091/pkira-cmp/NE_OAM_CA/synch");
    }

    @Test
    public void testCreateEnrollmentsFromEnrollmentJson() throws URISyntaxException, IOException{
        JSONObject enrollmentJson = new JSONObject(Files.readString(TestUtils.getResource(ENROLLMENT_JSON_FILE)));
        List<Enrollment> enrollments = CMPV2EnrollmentUtility.getEnrollmentsFromEnrollmentJson(enrollmentJson.getJSONObject(ENROLLMENT_INFO_STRING));
        assertThat(enrollments).hasSize(1);
        Enrollment enrollment = enrollments.get(0);
        assertThat(enrollment).isNotNull();
        assertThat(enrollment.getName()).isEqualTo("oamNodeCredential");
        assertThat(enrollment.getCertificateName()).isEqualTo("oamNodeCredential");
        assertThat(enrollment.getCmpServerGroup()).isEqualTo("enm-cmp-group-1");
    }
}
