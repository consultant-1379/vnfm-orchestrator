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
package com.ericsson.vnfm.orchestrator.presentation.constants;

import java.util.HashMap;
import java.util.Map;

public final class OssTopologyConstants {
    public static final String MANAGED_ELEMENT_ID = "managedElementId";
    public static final String NETWORK_ELEMENT_ID = "networkElementId";
    public static final String ALARM_SET_VALUE = "set_value";
    public static final String NOT_AVAILABLE = "not-available";
    public static final int EXIT_STATUS_SUCCESS = 0;
    public static final String OSS_TOPOLOGY = "oss_topology";

    private OssTopologyConstants() {
    }

    public static final class GenerateOssNodeProtocol {
        public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
        public static final String END_CERT = "-----END CERTIFICATE-----";
        public static final String ENROLLMENT_INFO_STRING = "enrollmentInfo";
        public static final String VERBOSE_ENROLLMENT_INFO_STRING = "verboseEnrollmentInfo";
        public static final String TRUSTED_CERTIFICATES_STRING = "trustedCertificates";
        public static final String TRUSTED_CERTIFICATE_STRING = "trustedCertificate";
        public static final String CA_PEM_STRING = "caPem";
        public static final String LDAP_IP_STRING = "ldapIP";
        public static final String LDAP_IPV4_ADDRESS_STRING = "ldapIpv4Address";
        public static final String FALLBACK_LDAP_IPV4_ADDRESS_STRING = "fallbackLdapIpv4Address";
        public static final String LDAP_IPV6_ADDRESS_STRING = "ldapIpv6Address";
        public static final String FALLBACK_LDAP_IPV6_ADDRESS_STRING = "fallbackLdapIpv6Address";
        public static final String ADD_NODE_OSS_TOPOLOGY = "add_node_oss_topology";
        public static final String INSTANTIATE_OSS_TOPOLOGY = "instantiate_oss_topology";
        public static final String OSS_NODE_PROTOCOL_FILE = "oss_node_protocol_file";
        public static final String ADDED_TO_OSS = "addedToOss";
        public static final String LDAP_PORT_PARAMETER = "port";
        public static final String LDAP_SERVER_PORT_STRING = "ldapsPort";
        public static final String PRIMARY_LDAP_SERVER_NAME = "primary-server";
        public static final String FALLBACK_LDAP_SERVER_NAME = "fallback-server";
        public static final String LDAP_SECURITY_BASE_DN_STRING = "baseDn";
        public static final String LDAP_SECURITY_BIND_DN_STRING = "bindDn";
        public static final String LDAP_SECURITY_BIND_PASSWD_STRING = "bindPassword";
        public static final String LDAP_CONFIGURATION_TIMEOUT_OPTION_STRING = "timeout";
        public static final int LDAP_CONFIGURATION_DEFAULT_TIMEOUT_OPTION = 5;
        public static final String LDAP_CONFIGURATION_REFERRALS_OPTION_STRING = "enable-referrals";
        private GenerateOssNodeProtocol() {
        }
    }

    public static final class AddNode {
        public static final String PM_FUNCTION = "pmFunction";
        public static final String NETWORK_ELEMENT_TYPE = "networkElementType";
        public static final String CNF_TYPE = "cnfType";
        public static final String NETWORK_ELEMENT_VERSION = "networkElementVersion";
        public static final String NODE_IP_ADDRESS = "nodeIpAddress";
        public static final String NET_CONF_PORT = "netConfPort";
        public static final String COMMUNITY_STRING = "communityString";
        public static final String NETWORK_ELEMENT_USERNAME = "networkElementUsername";
        public static final String NETWORK_ELEMENT_PASSWORD = "networkElementPassword"; // NOSONAR
        public static final String SNMP_PORT = "snmpPort";
        public static final String SNMP_AUTH_PROTOCOL = "snmpAuthProtocol";
        public static final String SNMP_PRIV_PROTOCOL = "snmpPrivProtocol";
        public static final String CM_NODE_HEARTBEAT_SUPERVISION = "cmNodeHeartbeatSupervision";
        public static final String FM_ALARM_SUPERVISION = "fmAlarmSupervision";
        public static final String TRANSPORT_PROTOCOL = "transportProtocol";
        public static final String SNMP_VERSION = "snmpVersion";
        public static final String VNF_INSTANCE_ID = "vnfInstanceId";
        public static final String TENANT = "tenant";
        public static final String SMALL_STACK_APPLICATION = "smallStackApplication";
        public static final String TENANT_NAME = "tenantName";
        public static final String VNFM_NAME = "vnfmName";

        private AddNode() {
        }
    }

    public static final class GenerateEnrollment {
        public static final String FILE_TO_UPLOAD = "file_to_upload";
        public static final String GENERATED_XML_FILE = "generated_xml_file";
        public static final String ENROLLMENT_CONFIGURATION = "enrollmentConfiguration";
        public static final String LDAP_DETAILS = "ldapDetails";
        public static final String ENROLLMENT = "enrollment";
        public static final String LDAP = "ldap";
        public static final String ROOT_NODES_TAG = "Nodes";
        public static final String NODE_TAG = "Node";
        public static final String NODE_FDN_TAG = "nodeFdn";
        public static final String CERT_TYPE_TAG = "certType";
        public static final String ENROLLMENT_MODE_TAG = "enrollmentMode";
        public static final String OTP_VALIDITY_PERIOD_IN_MINUTES = "otpValidityPeriodInMinutes";
        public static final Integer OTP_VALIDITY_PERIOD_MAX_VALUE = 43200;
        public static final Integer OTP_VALIDITY_PERIOD_MIN_VALUE = 1;
        public static final Integer OTP_VALIDITY_PERIOD_NON_EXPIRING = -1;
        public static final String ENTITY_PROFILE_NAME = "entityProfileName";
        public static final String ENROLLMENT_CERT_TYPE = "OAM";
        public static final String CMPV2_ENROLLMENT_MODE = "CMPv2_INITIAL";
        public static final String ISSUER_CA_PARAM_NAME = "issuerCA";
        public static final String ENROLLMENT_CMP_CONFIG_PARAM_NAME = "enrollmentCmpConfig";
        public static final String ENROLLMENT_SERVER_ID_PARAM_NAME = "EnrollmentServerId";
        public static final String ENROLLMENT_SERVER_URL_PARAM_NAME = "url";
        public static final String ENROLLMENT_AUTHORITY_PARAM_NAME = "EnrollmentAuthority";
        public static final String ENROLLMENT_CA_CERTS_PARAM_NAME = "cacerts";
        public static final String ENROLLMENT_SERVER_GROUP_ID_PARAM_NAME = "EnrollmentServerGroupId";
        public static final String ENROLLMENT_NODE_CREDS_ID_PARAM_NAME = "NodeCredentialId";
        public static final String ENROLLMENT_KEY_INFO_PARAM_NAME = "keyInfo";
        public static final String ENROLLMENT_SUBJECT_NAME_PARAM_NAME = "subjectName";
        public static final String ENROLLMENT_CHALLENGE_PASSWORD_PARAM_NAME = "challengePassword";
        public static final String ENROLLMENT_TRUSTED_CERTS_PARAM_NAME = "TrustedCerts";
        public static final String ENM_ROOT_CA_PKI_CERTIFICATE_NAME = "ENM_PKI_Root_CA";
        public static final String CERTM_OAM_ROOT_CERTIFICATE_NAME = "oamCmpCaTrustCategory";
        public static final String CERTM_ADDITIONAL_LLS_CERTIFICATE_NAME = "llsTrustCategory";
        public static final Map<String, String> ENROLLMENT_PARAMETER = new HashMap<>();
        public static final Map<String, String> MANDATORY_CERTIFICATE_KEY = new HashMap<>();

        static {
            ENROLLMENT_PARAMETER.put(ENROLLMENT_CHALLENGE_PASSWORD_PARAM_NAME, "$.enrollmentInfo.challengePassword");
            ENROLLMENT_PARAMETER.put(ENROLLMENT_KEY_INFO_PARAM_NAME, "$.enrollmentInfo.keyInfo");
            ENROLLMENT_PARAMETER.put(ENROLLMENT_SUBJECT_NAME_PARAM_NAME, "$.enrollmentInfo.subjectName");
            ENROLLMENT_PARAMETER.put(ISSUER_CA_PARAM_NAME, "$.enrollmentInfo.issuerCA");
            ENROLLMENT_PARAMETER.put(ENROLLMENT_SERVER_URL_PARAM_NAME, "$.enrollmentInfo.url");

            MANDATORY_CERTIFICATE_KEY.put("neOAMCA", "NE_OAM_CA");
            MANDATORY_CERTIFICATE_KEY.put("enmOAMCA", "ENM_OAM_CA");
            MANDATORY_CERTIFICATE_KEY.put("enmInfrastructureCA", "ENM_Infrastructure_CA");
            MANDATORY_CERTIFICATE_KEY.put("enmPKIRootCA", "ENM_PKI_Root_CA");
        }

        private GenerateEnrollment() {
        }
    }

    public static final class Error {
        public static final String UNABLE_TO_FIND_CERTIFICATE_ERROR_MESSAGE = "Unable to find the certificate because %s";
        public static final String PARAMETER_MISSING_ERROR_MESSAGE = "%s details are either null or empty";
        public static final String UNKNOWN_SERVER_LDAP_SERVER_TYPE_MESSAGE = "Unknown LDAP server type: %s. Valid values are [%s, %s]";

        private Error() {
        }
    }

    public static final class RestoreBackup {
        public static final String RESTORE_BACKUP_FILE = "restoreBackup.ftl";
        public static final String LATEST = "Latest";
        public static final String ACTION_ID = "actionId";
        public static final String BACKUP_FILE = "backupFile";
        public static final String BACKUP_FILE_REF = "backupFileRef";
        public static final String BACKUP_FILE_REF_PASSWORD = "password";
        public static final String RESULT_RESPONSE = "result";
        public static final String RESULT_INFO_RESPONSE = "result-info";
        public static final String RETURN_VALUE_RESPONSE = "return-value";
        public static final String RESTORE_OPERATION_RESPONSE = "restore_backup_data";
        public static final String IMPORT_OPERATION_RESPONSE = "import_backup_data";
        public static final String IMPORT_PROGRESS_OPERATION_RESPONSE = "import_progress";
        public static final String RESTORE_LATEST_OPERATION_RESPONSE = "restore_latest_backup_data";

        private RestoreBackup() {
        }
    }
}
