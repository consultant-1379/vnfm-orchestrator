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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.*;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.CA_PEM_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.VERBOSE_ENROLLMENT_INFO_STRING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.readFileContent;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CACertificate;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServer;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServerGroup;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPServerGroups;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CertificateAuthority;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.Enrollment;
import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPV2EnrollmentConfig;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public final class CMPV2EnrollmentUtility {

    private static final String CMP_SERVER_GROUP_NAME_PREFIX = "enm-cmp-group-";
    private static final String CMP_SERVER_NAME_PREFIX = "enm-cmp-server-";
    private static final String CERTIFICATE_NAME_PARAM = "name";

    private CMPV2EnrollmentUtility() { }

    public static String decodeCertificatePem(final String encoded) {
        return new String(Base64.getDecoder().decode(encoded.getBytes()));
    }

    public static List<CACertificate> createCertificatesFromEnrollmentJson(JSONArray certificates) {
        List<CACertificate> formattedCertificates = new ArrayList<>();
        for (int i = 0; i < certificates.length(); i++) {
            JSONObject currentCertificate = certificates.getJSONObject(i);
            var certificateName = currentCertificate.getString(CERTIFICATE_NAME_PARAM);

            if (certificateName.contains(ENM_ROOT_CA_PKI_CERTIFICATE_NAME)) {
                CACertificate certificate = new CACertificate();
                certificate.setName(CERTM_OAM_ROOT_CERTIFICATE_NAME);
                certificate.setPem(decodeCertificatePem(currentCertificate.getString(CA_PEM_STRING)));
                formattedCertificates.add(certificate);
            }
        }
        return formattedCertificates;
    }

    public static CertificateAuthority getAuthorityFromEnrollment(JSONObject enrollment) {
        CertificateAuthority authority = new CertificateAuthority();
        if (enrollment.has(ISSUER_CA_PARAM_NAME)) {
            authority.setCertificateAuthority(List.of(Map.of(CERTIFICATE_NAME_PARAM, enrollment.getString(ISSUER_CA_PARAM_NAME))));
        }
        return authority;
    }

    public static CMPServerGroups getCMPServerGroupsFromEnrollment(JSONObject enrollment) {
        CMPServerGroups cmpServerGroups = new CMPServerGroups();
        JSONObject verboseEnrollmentInfo = enrollment.getJSONObject(VERBOSE_ENROLLMENT_INFO_STRING);
        if (verboseEnrollmentInfo.has(ENROLLMENT_CMP_CONFIG_PARAM_NAME)) {
            JSONObject enrollmentCmpConfig = verboseEnrollmentInfo.getJSONObject(ENROLLMENT_CMP_CONFIG_PARAM_NAME);

            CMPServer cmpServer = new CMPServer();
            cmpServer.setName(CMP_SERVER_NAME_PREFIX.concat(enrollmentCmpConfig.get(ENROLLMENT_SERVER_ID_PARAM_NAME).toString()));
            cmpServer.setUri(enrollment.getString(ENROLLMENT_SERVER_URL_PARAM_NAME));
            cmpServer.setCertificateAuthority(enrollmentCmpConfig.getString(ENROLLMENT_AUTHORITY_PARAM_NAME));
            cmpServer.setCaCerts(enrollmentCmpConfig.getString(ENROLLMENT_CA_CERTS_PARAM_NAME));
            cmpServer.setPriority(1);

            CMPServerGroup cmpServerGroup = new CMPServerGroup();
            cmpServerGroup.setName(CMP_SERVER_GROUP_NAME_PREFIX.concat(enrollmentCmpConfig.get(ENROLLMENT_SERVER_GROUP_ID_PARAM_NAME).toString()));
            cmpServerGroup.setCmpServers(List.of(cmpServer));
            cmpServerGroups.setCmpServerGroups(List.of(cmpServerGroup));
        }
        return cmpServerGroups;
    }

    public static List<Enrollment> getEnrollmentsFromEnrollmentJson(JSONObject enrollmentJson) {
        Enrollment enrollment = new Enrollment();
        JSONObject verboseEnrollmentInfo = enrollmentJson.getJSONObject(VERBOSE_ENROLLMENT_INFO_STRING);
        if (verboseEnrollmentInfo.has(ENROLLMENT_CMP_CONFIG_PARAM_NAME)) {
            JSONObject enrollmentCmpConfig = verboseEnrollmentInfo.getJSONObject(ENROLLMENT_CMP_CONFIG_PARAM_NAME);

            enrollment.setName(enrollmentCmpConfig.getString(ENROLLMENT_NODE_CREDS_ID_PARAM_NAME));
            enrollment.setCertificateName(enrollmentCmpConfig.getString(ENROLLMENT_NODE_CREDS_ID_PARAM_NAME));
            enrollment.setAlgorithm(enrollmentJson.getString(ENROLLMENT_KEY_INFO_PARAM_NAME));
            enrollment.setSubject(enrollmentJson.getString(ENROLLMENT_SUBJECT_NAME_PARAM_NAME));
            enrollment.setPassword(enrollmentJson.getString(ENROLLMENT_CHALLENGE_PASSWORD_PARAM_NAME));
            enrollment.setCmpServerGroup(CMP_SERVER_GROUP_NAME_PREFIX + enrollmentCmpConfig.getInt(ENROLLMENT_SERVER_GROUP_ID_PARAM_NAME));
            enrollment.setTrustedCerts(enrollmentCmpConfig.getString(ENROLLMENT_TRUSTED_CERTS_PARAM_NAME));
        }
        return List.of(enrollment);
    }

    /**
     * This method adds llsTrustCategory certificates into CertM deployment configuration secret
     * specifically for vDU nodes
     */
    public static String includeAdditionalCertForSpecificNode(String enrollmentConfiguration, String certificatePath) {
        try {
            CMPV2EnrollmentConfig enrollmentConfig = parseJson(enrollmentConfiguration, CMPV2EnrollmentConfig.class);
            String additionalCertificate = readFileContent(certificatePath);

            CACertificate certificate = new CACertificate();
            certificate.setName(CERTM_ADDITIONAL_LLS_CERTIFICATE_NAME);
            certificate.setPem(additionalCertificate);
            enrollmentConfig.getCertificates().add(certificate);

            return convertObjToJsonString(enrollmentConfig);
        } catch (IOException ex) {
            LOGGER.error("Unable to include additional certificate to the enrollment configuration due to: {}", ex);
            throw new IllegalStateException(ex);
        }
    }

    public static boolean checkInstanceRequiresAdditionalCertificate(String configuredProductTypes, String instanceProductType) {
        return Arrays.asList(configuredProductTypes.split(",")).contains(instanceProductType);
    }
}
