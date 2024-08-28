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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.CM_NODE_HEARTBEAT_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.COMMUNITY_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.FM_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.NET_CONF_PORT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.PM_FUNCTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.SNMP_AUTH_PROTOCOL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.SNMP_PORT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.SNMP_PRIV_PROTOCOL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.SNMP_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.TRANSPORT_PROTOCOL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.Error.PARAMETER_MISSING_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.Error.UNABLE_TO_FIND_CERTIFICATE_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.Error.UNKNOWN_SERVER_LDAP_SERVER_TYPE_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.LDAP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.MANDATORY_CERTIFICATE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.BEGIN_CERT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.CA_PEM_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.END_CERT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ENROLLMENT_INFO_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.FALLBACK_LDAP_IPV4_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.FALLBACK_LDAP_IPV6_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.FALLBACK_LDAP_SERVER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_CONFIGURATION_DEFAULT_TIMEOUT_OPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_CONFIGURATION_REFERRALS_OPTION_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_CONFIGURATION_TIMEOUT_OPTION_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_IPV4_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_IPV6_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_IP_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_PORT_PARAMETER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_SECURITY_BASE_DN_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_SECURITY_BIND_DN_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_SECURITY_BIND_PASSWD_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_SERVER_PORT_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.PRIMARY_LDAP_SERVER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.TRUSTED_CERTIFICATES_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.TRUSTED_CERTIFICATE_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.VERBOSE_ENROLLMENT_INFO_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.createCertificatesFromEnrollmentJson;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.getAuthorityFromEnrollment;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.getCMPServerGroupsFromEnrollment;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.getEnrollmentsFromEnrollmentJson;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENABLE_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.CHECK_NODE;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.dontLogPasswords;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertXMLToJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.readFileContentThenDelete;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.StandardProtocolFamily;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.oss.enrollment.CMPV2EnrollmentConfig;
import com.ericsson.vnfm.orchestrator.model.oss.ldap.ExternalLdapConfig;
import com.ericsson.vnfm.orchestrator.model.oss.ldap.LdapAuthConfig;
import com.ericsson.vnfm.orchestrator.model.oss.ldap.LdapSecurityConfig;
import com.ericsson.vnfm.orchestrator.model.oss.ldap.LdapServer;
import com.ericsson.vnfm.orchestrator.model.oss.ldap.LdapTCPConfig;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EnmTopologyService implements OssTopologyService {

    private static final String LOGGER_MASSAGE = "Input with defaults added: {}";
    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();
    private static final List<String> BOOLEAN_ATTRIBUTES = Arrays.asList(PM_FUNCTION, CM_NODE_HEARTBEAT_SUPERVISION,
                                                                         FM_ALARM_SUPERVISION);
    private static final List<String> NUMBER_ATTRIBUTES = Arrays.asList(NET_CONF_PORT, SNMP_PORT);

    @Autowired
    private Configuration freemarkerConfiguration;

    @Value("${oss.topology.directory}")
    private String scriptsDirectory;

    @PostConstruct
    public void setupDefaultValues() {
        DEFAULT_VALUES.putIfAbsent(COMMUNITY_STRING, "enm-public");
        DEFAULT_VALUES.putIfAbsent(SNMP_PORT, 161);
        DEFAULT_VALUES.putIfAbsent(PM_FUNCTION, false);
        DEFAULT_VALUES.putIfAbsent(CM_NODE_HEARTBEAT_SUPERVISION, true);
        DEFAULT_VALUES.putIfAbsent(FM_ALARM_SUPERVISION, true);
        DEFAULT_VALUES.putIfAbsent(NET_CONF_PORT, 830);
        DEFAULT_VALUES.putIfAbsent(TRANSPORT_PROTOCOL, "SSH");
    }

    @Override
    public Path generateAddNodeScript(final Map<String, Object> topologyAttributes) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoEmptyValues(topologyAttributes);
        validateTypes(topologyAttributes);
        setSNMPV3DefaultValues(topologyAttributes);
        Map<String, Object> fullSetOfAttributes = getFullSetOfAttributes(topologyAttributes);
        fullSetOfAttributes.put(OPERATION_RESPONSE, ADD_NODE.getOperationResponse());
        fullSetOfAttributes.put(OPERATION, ADD_NODE.getOperationResponse());
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(fullSetOfAttributes));
        final Path addNodeScript = Paths.get(scriptsDirectory).resolve("addNode-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(fullSetOfAttributes, addNodeScript, "addnode.ftl");
    }

    private static void ensureThereAreNoEmptyValues(final Map<String, Object> topologyAttributes) {
        for (Entry<String, Object> entry : topologyAttributes.entrySet()) {
            if (StringUtils.isEmpty(String.valueOf(entry.getValue()))) {
                throw new IllegalArgumentException("The parameter " + entry.getKey() + " was found to be null or empty");
            }
        }
    }
    private static void ensureThereAreNoNullValues(final Map<String, Object> topologyAttributes) {
        for (Entry<String, Object> entry : topologyAttributes.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("The parameter " + entry.getKey() + " was found to be null");
            }
        }
    }

    private static void ensureMapIsNotNull(final Map<String, Object> topologyAttributes) {
        if (topologyAttributes == null) {
            throw new IllegalArgumentException("Attributes must be specified for the generation of the script");
        }
    }

    private static void ensureManagedElementIdPresent(final Map<String, Object> topologyAttributes) {
        if (!topologyAttributes.containsKey(MANAGED_ELEMENT_ID)) {
            throw new IllegalArgumentException("Attribute 'managedElementId' is missing from ossTopology parameters");
        }
    }

    private static void setSNMPV3DefaultValues(final Map<String, Object> topologyAttributes) {
        if ("SNMP_V3".equals(topologyAttributes.get(SNMP_VERSION))) {
            LOGGER.info("Setting SNMP_V3 default values if not provided");
            topologyAttributes.putIfAbsent(SNMP_AUTH_PROTOCOL, "MD5");
            topologyAttributes.putIfAbsent(SNMP_PRIV_PROTOCOL, "AES128");
        }
    }

    @Override
    public Path generateDeleteNodeScript(final Map<String, Object> topologyAttributes) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoEmptyValues(topologyAttributes);
        LOGGER.info("Input values for deleteFile: {}", dontLogPasswords(topologyAttributes));
        final Path deleteNodeScript = Paths.get(scriptsDirectory).resolve("deleteNode-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(topologyAttributes, deleteNodeScript, "deletenode.ftl");
    }

    @Override
    public Path generateEnableSupervisionsScript(Map<String, Object> topologyAttributes) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoEmptyValues(topologyAttributes);
        Map<String, Object> fullSetOfAttributes = getFullSetOfAttributes(topologyAttributes);
        fullSetOfAttributes.put(OPERATION, ENABLE_SUPERVISION.getOperation());
        fullSetOfAttributes.put(OPERATION_RESPONSE, ENABLE_SUPERVISION.getOperationResponse());
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(fullSetOfAttributes));
        final Path enableSupervisionScript =
                Paths.get(scriptsDirectory).resolve("enableSupervision" + "-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(fullSetOfAttributes, enableSupervisionScript, "enableSupervisions.ftl");
    }

    @Override
    public Path generateSetAlarmScript(final Map<String, Object> topologyAttributes, EnmOperationEnum operationEnum) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoEmptyValues(topologyAttributes);
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(topologyAttributes));
        final Path setAlarmScript =
                Paths.get(scriptsDirectory).resolve(operationEnum.getOperation() + "-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(topologyAttributes, setAlarmScript, "setAlarmSupervision.ftl");
    }

    @Override
    public Path generateGetEnrollmentInfoScript(final Map<String, Object> topologyAttributes, EnmOperationEnum operationEnum) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoEmptyValues(topologyAttributes);
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(topologyAttributes));
        final Path getEnrollmentInfoScript =
                Paths.get(scriptsDirectory).resolve(operationEnum.getOperation() + "-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(topologyAttributes, getEnrollmentInfoScript, "generate-enrollment.ftl");
    }

    @Override
    public Path generateRestoreScript(final Map<String, Object> topologyAttributes, String templateName, EnmOperationEnum enmOperationEnum) {
        ensureMapIsNotNull(topologyAttributes);
        ensureThereAreNoNullValues(topologyAttributes);
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(topologyAttributes));
        final Path restoreScript = Paths.get(scriptsDirectory)
                .resolve(enmOperationEnum.getOperation() + "-" + UUID.randomUUID().toString() + ".py");
        return createScriptFromTemplate(topologyAttributes, restoreScript, templateName);
    }

    private Path createScriptFromTemplate(final Map<String, Object> topologyAttributes, final Path script, final String template) {
        try (PrintWriter printWriter = new PrintWriter(script.toFile(), StandardCharsets.UTF_8)) {
            final Template scriptTemplate = freemarkerConfiguration.getTemplate(template);
            LOGGER.info(String.format("Generating the %s file to {}", script.getFileName()), script.toFile().getAbsolutePath());
            scriptTemplate.process(topologyAttributes, printWriter);
            return script;
        } catch (final IOException e) {
            throw new InternalRuntimeException(String.format("Could not create %s file with following params %s",
                                                             script.getFileName(),
                                                             topologyAttributes.toString()), e);
        } catch (final InvalidReferenceException e) {
            throw new IllegalArgumentException(String.format("The parameter %s was not provided, Full List of parameters provided %s",
                                                             e.getBlamedExpressionString(),
                                                             topologyAttributes.toString()), e);
        } catch (final TemplateException e) {
            throw new InternalRuntimeException(String.format("Could not generate %s file from template", script.getFileName()), e);
        }
    }

    private static void validateTypes(final Map<String, Object> topologyAttributes) {
        LOGGER.info("Validating types in {}", dontLogPasswords(topologyAttributes));
        for (final String booleanTypes : BOOLEAN_ATTRIBUTES) {
            if (booleanTypeIsWrongValue(topologyAttributes, booleanTypes)) {
                throw new IllegalArgumentException(String.format("The parameter %s was the wrong type", booleanTypes));
            }
        }
        for (final String numberType : NUMBER_ATTRIBUTES) {
            if (numberTypeIsWrongValue(topologyAttributes, numberType)) {
                throw new IllegalArgumentException(String.format("The parameter %s was the wrong type", numberType));
            }
        }
    }

    private static boolean numberTypeIsWrongValue(final Map<String, Object> topologyAttributes, final String numberType) {
        return topologyAttributes.containsKey(numberType) && !(StringUtils.isNumeric(String.valueOf(topologyAttributes.get(numberType))));
    }

    private static boolean booleanTypeIsWrongValue(final Map<String, Object> topologyAttributes,
                                                   final String numberType) {
        if (topologyAttributes.containsKey(numberType)) {
            Object booleanValue = topologyAttributes.get(numberType);
            return !(booleanValue instanceof Boolean || "false".equalsIgnoreCase(booleanValue.toString())
                    || "true".equalsIgnoreCase(booleanValue.toString()));
        }
        return false;
    }

    public Map<String, Object> getFullSetOfAttributes(final Map<String, Object> topologyAttributes) {
        Map<String, Object> fullSetOfAttributes = new HashMap<>(DEFAULT_VALUES);
        fullSetOfAttributes.putAll(topologyAttributes);
        return fullSetOfAttributes;
    }

    @Override
    public String getOssNodeProtocolFileContent(String enrollmentFileOutput, String ldapDetails,
                                                StandardProtocolFamily ipVersion) {
        Path ossNodeProtocolPath = generateOssNodeProtocolFilePath(enrollmentFileOutput, ldapDetails, ipVersion);
        return readFileContentThenDelete(ossNodeProtocolPath);
    }

    @Override
    public Path generateOssNodeProtocolFilePath(String enrollmentFileOutput, String ldapDetails,
                                    StandardProtocolFamily ipVersion) {
        if (Strings.isBlank(enrollmentFileOutput)) {
            throw new IllegalArgumentException(String.format(PARAMETER_MISSING_ERROR_MESSAGE, ENROLLMENT));
        }
        if (Strings.isBlank(ldapDetails)) {
            throw new IllegalArgumentException(String.format(PARAMETER_MISSING_ERROR_MESSAGE, LDAP));
        }
        JSONObject enrollmentFile = convertXMLToJson(enrollmentFileOutput, ENROLLMENT);
        List<String> errorParameter = new ArrayList<>();
        Map<String, Object> ossNodeProtocolValue = new HashMap<>();

        validateAndSetOssNodeProtocolParameter(enrollmentFile, ossNodeProtocolValue, errorParameter);

        JSONArray allCertificate = getAllCertificateFromJson(enrollmentFile);
        validateAndSetCertificateInNodeProtocolParameter(allCertificate, ossNodeProtocolValue);

        for (Map.Entry<String, String> entry : MANDATORY_CERTIFICATE_KEY.entrySet()) {
            if (ossNodeProtocolValue.get(entry.getKey()) == null) {
                errorParameter.add(entry.getValue());
            }
        }

        setLdapDetailsInNodeProtocolParameter(ldapDetails, ossNodeProtocolValue, ipVersion, errorParameter);

        if (!errorParameter.isEmpty()) {
            throw new IllegalArgumentException("Following details are missing in the enrollment file : "
                    + errorParameter);
        }
        return generateOssNodeProtocolFile(ossNodeProtocolValue);
    }

    private Path generateOssNodeProtocolFile(final Map<String, Object> ossNodeProtocolValue) {
        final Path ossNodeProtocolFile = Paths.get(scriptsDirectory).resolve("ossNodeProtocol-"
                + UUID.randomUUID().toString() + ".xml");
        return createScriptFromTemplate(ossNodeProtocolValue, ossNodeProtocolFile, "ossNodeProtocol.ftl");
    }

    @Override
    public String generateLdapConfigurationJSONString(String ldapDetails, StandardProtocolFamily ipVersion) {
        JSONObject jsonLdapDetails = new JSONObject(ldapDetails);
        validateLDAPDetailsResponse(jsonLdapDetails);
        ExternalLdapConfig ldapConfig = new ExternalLdapConfig();
        ldapConfig.setServer(List.of(
                createLdapServer(PRIMARY_LDAP_SERVER_NAME, jsonLdapDetails, ipVersion),
                createLdapServer(FALLBACK_LDAP_SERVER_NAME, jsonLdapDetails, ipVersion)
        ));
        ldapConfig.setSecurity(createLdapSecurityConfiguration(jsonLdapDetails));
        ldapConfig.setOptions(Map.of(LDAP_CONFIGURATION_TIMEOUT_OPTION_STRING, LDAP_CONFIGURATION_DEFAULT_TIMEOUT_OPTION,
                                     LDAP_CONFIGURATION_REFERRALS_OPTION_STRING, false));
        return convertObjToJsonString(ldapConfig);
    }

    @Override
    public String generateEnrollmentConfigurationJSONString(String enrollmentConfigString) {
        JSONObject enrollmentJson = convertXMLToJson(enrollmentConfigString, ENROLLMENT);
        CMPV2EnrollmentConfig enrollmentConfig = createCMPEnrollmentConfiguration(enrollmentJson);
        return convertObjToJsonString(enrollmentConfig);
    }

    @Override
    public Path generateCheckNodePresentScript(final Map<String, Object> topologyAttributes) {
        ensureMapIsNotNull(topologyAttributes);
        ensureManagedElementIdPresent(topologyAttributes);
        Map<String, Object> fullSetOfAttributes = new HashMap<>(topologyAttributes);
        fullSetOfAttributes.put(OPERATION_RESPONSE, CHECK_NODE.getOperationResponse());
        fullSetOfAttributes.put(OPERATION, CHECK_NODE.getOperationResponse());
        LOGGER.info(LOGGER_MASSAGE, dontLogPasswords(fullSetOfAttributes));
        final Path checkNodeScript = Paths.get(scriptsDirectory).resolve("checkNodePresent-" + UUID.randomUUID() + ".py");
        return createScriptFromTemplate(fullSetOfAttributes, checkNodeScript, "checkNodePresent.ftl");
    }

    private static LdapServer createLdapServer(final String serverName,
                                               final JSONObject ldapDetails,
                                               StandardProtocolFamily ipVersion) {
        LdapServer server = new LdapServer();
        server.setName(serverName);

        LdapTCPConfig tcpConfig = new LdapTCPConfig();
        tcpConfig.setAddress(getLDAPAddressBasedOnIPVersion(serverName, ldapDetails, ipVersion));
        tcpConfig.setLdaps(Map.of(LDAP_PORT_PARAMETER, ldapDetails.getInt(LDAP_SERVER_PORT_STRING)));
        server.setTcp(tcpConfig);

        return server;
    }

    private static LdapSecurityConfig createLdapSecurityConfiguration(final JSONObject ldapDetails) {
        LdapSecurityConfig securityConfig = new LdapSecurityConfig();
        securityConfig.setUserBaseDn(ldapDetails.getString(LDAP_SECURITY_BASE_DN_STRING));

        LdapAuthConfig authConfig = new LdapAuthConfig();
        authConfig.setBindDn(ldapDetails.getString(LDAP_SECURITY_BIND_DN_STRING));
        authConfig.setBindPassword(ldapDetails.getString(LDAP_SECURITY_BIND_PASSWD_STRING));
        securityConfig.setSimpleAuth(authConfig);
        securityConfig.setTls(Collections.emptyMap());
        return securityConfig;
    }

    private static String getLDAPAddressBasedOnIPVersion(final String serverName,
                                                         final JSONObject ldapDetails,
                                                         StandardProtocolFamily ipVersion) {
        switch (serverName) {
            case PRIMARY_LDAP_SERVER_NAME:
                return StandardProtocolFamily.INET6.equals(ipVersion) ?
                        ldapDetails.get(LDAP_IPV6_ADDRESS_STRING).toString() :
                        ldapDetails.get(LDAP_IPV4_ADDRESS_STRING).toString();
            case FALLBACK_LDAP_SERVER_NAME:
                return StandardProtocolFamily.INET6.equals(ipVersion) ?
                        ldapDetails.get(FALLBACK_LDAP_IPV6_ADDRESS_STRING).toString() :
                        ldapDetails.get(FALLBACK_LDAP_IPV4_ADDRESS_STRING).toString();
            default:
                String msg = String.format(UNKNOWN_SERVER_LDAP_SERVER_TYPE_MESSAGE, serverName,
                                             PRIMARY_LDAP_SERVER_NAME, FALLBACK_LDAP_SERVER_NAME);
                LOGGER.error(msg);
                throw new IllegalArgumentException(msg);
        }
    }

    private static void setLdapDetailsInNodeProtocolParameter(String ldapDetails,
                                                       Map<String, Object> ossNodeProtocolValue,
                                                       StandardProtocolFamily ipVersion, List<String> errorParameter) {
        JSONObject jsonLdapDetails = new JSONObject(ldapDetails);
        if (StandardProtocolFamily.INET.equals(ipVersion)) {
            setIpAddressInOssNodeProtocolValue(jsonLdapDetails, ossNodeProtocolValue, errorParameter,
                    LDAP_IPV4_ADDRESS_STRING, FALLBACK_LDAP_IPV4_ADDRESS_STRING);
        } else if (StandardProtocolFamily.INET6.equals(ipVersion)) {
            setIpAddressInOssNodeProtocolValue(jsonLdapDetails, ossNodeProtocolValue, errorParameter,
                    LDAP_IPV6_ADDRESS_STRING, FALLBACK_LDAP_IPV6_ADDRESS_STRING);
        } else {
            setIpAddressInOssNodeProtocolValueWhenIPTypeNotDefined(jsonLdapDetails, ossNodeProtocolValue,
                    errorParameter);
        }

        validateAndSetLdapDetails(jsonLdapDetails, ossNodeProtocolValue, "ldapsPort", errorParameter);
        validateAndSetLdapDetails(jsonLdapDetails, ossNodeProtocolValue, "bindPassword", errorParameter);
        validateAndSetLdapDetails(jsonLdapDetails, ossNodeProtocolValue, "bindDn", errorParameter);
        validateAndSetLdapDetails(jsonLdapDetails, ossNodeProtocolValue, "baseDn", errorParameter);
    }

    private static void setIpAddressInOssNodeProtocolValue(JSONObject jsonLdapDetails, Map<String, Object> ossNodeProtocolValue,
                                                      List<String> errorParameter, String ipAddressKey,
                                                      String ipAddressFallbackKey) {
        if (jsonLdapDetails.has(ipAddressKey)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(ipAddressKey));
        } else if (jsonLdapDetails.has(ipAddressFallbackKey)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(ipAddressFallbackKey));
        } else {
            errorParameter.add(LDAP_IP_STRING);
        }
    }

    private static void setIpAddressInOssNodeProtocolValueWhenIPTypeNotDefined(JSONObject jsonLdapDetails,
                                                                        Map<String, Object> ossNodeProtocolValue,
                                                                        List<String> errorParameter) {
        if (jsonLdapDetails.has(LDAP_IPV4_ADDRESS_STRING)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(LDAP_IPV4_ADDRESS_STRING));
        } else if (jsonLdapDetails.has(FALLBACK_LDAP_IPV4_ADDRESS_STRING)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(FALLBACK_LDAP_IPV4_ADDRESS_STRING));
        } else if (jsonLdapDetails.has(LDAP_IPV6_ADDRESS_STRING)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(LDAP_IPV6_ADDRESS_STRING));
        } else if (jsonLdapDetails.has(FALLBACK_LDAP_IPV6_ADDRESS_STRING)) {
            ossNodeProtocolValue.put(LDAP_IP_STRING, jsonLdapDetails.getString(FALLBACK_LDAP_IPV6_ADDRESS_STRING));
        } else {
            errorParameter.add(LDAP_IP_STRING);
        }
    }

    private static void validateLDAPDetailsResponse(final JSONObject ldapConfig) {
        Set<String> keys = ldapConfig.keySet();
        if (keys.containsAll(List.of(LDAP_SECURITY_BIND_DN_STRING, LDAP_IPV4_ADDRESS_STRING, FALLBACK_LDAP_IPV4_ADDRESS_STRING,
                                                    LDAP_IPV6_ADDRESS_STRING, FALLBACK_LDAP_IPV6_ADDRESS_STRING, LDAP_SECURITY_BASE_DN_STRING,
                                                    LDAP_SERVER_PORT_STRING, LDAP_SECURITY_BIND_PASSWD_STRING))) {
            keys.forEach(key -> {
                if (StringUtils.isEmpty(ldapConfig.getString(key))) {
                    throw new IllegalArgumentException(String.format("The following parameter is missing in the LDAP configuration: %s", key));
                }
            });
        } else {
            throw new IllegalArgumentException("Some parameters are missing from LDAP configuration");
        }
    }

    private static void validateAndSetLdapDetails(JSONObject jsonLdapDetails, Map<String, Object> ossNodeProtocolValue,
                                           String parameterName, List<String> errorParameter) {
        if (jsonLdapDetails.has(parameterName)) {
            ossNodeProtocolValue.put(parameterName, jsonLdapDetails.getString(parameterName));
        } else {
            errorParameter.add(parameterName);
        }
    }

    private CMPV2EnrollmentConfig createCMPEnrollmentConfiguration(final JSONObject enrollmentFile) {
        JSONObject enrollmentInfo = enrollmentFile.getJSONObject(ENROLLMENT_INFO_STRING);

        CMPV2EnrollmentConfig cmpv2EnrollmentConfig = new CMPV2EnrollmentConfig();
        cmpv2EnrollmentConfig.setCertificates(createCertificatesFromEnrollmentJson(getAllCertificateFromJson(enrollmentFile)));
        cmpv2EnrollmentConfig.setCertificateAuthorities(getAuthorityFromEnrollment(enrollmentInfo));
        cmpv2EnrollmentConfig.setCmpServerGroups(getCMPServerGroupsFromEnrollment(enrollmentInfo));
        cmpv2EnrollmentConfig.setEnrollments(getEnrollmentsFromEnrollmentJson(enrollmentInfo));
        cmpv2EnrollmentConfig.setRetryTimeout(5);

        return cmpv2EnrollmentConfig;
    }

    @VisibleForTesting
    @SuppressWarnings("java:S4248")
    void validateAndSetCertificateInNodeProtocolParameter(JSONArray allCertificate,
                                                                  Map<String, Object> ossNodeProtocolValue) {
        for (int index = 0; index < allCertificate.length(); index++) {
            JSONObject certificate = allCertificate.getJSONObject(index);
            String caPem = certificate.getString(CA_PEM_STRING);
            X509Certificate x509Certificate = getX509Certificate(caPem);
            Map<String, String> subjectNameComponent = getSubjectNameComponent(x509Certificate);
            String encodedPemCertificate = Base64.getEncoder().encodeToString(caPem
                    .replaceAll("[\\r]+", "").replaceAll("[\\t]+", "")
                    .getBytes(StandardCharsets.UTF_8));
            switch (subjectNameComponent.get("CN")) {
                case "NE_OAM_CA":
                    ossNodeProtocolValue.put("neOAMCA", encodedPemCertificate);
                    break;
                case "ENM_OAM_CA":
                    ossNodeProtocolValue.put("enmOAMCA", encodedPemCertificate);
                    break;
                case "ENM_Infrastructure_CA":
                    ossNodeProtocolValue.put("enmInfrastructureCA", encodedPemCertificate);
                    break;
                case "ENM_PKI_Root_CA":
                    ossNodeProtocolValue.put("enmPKIRootCA", encodedPemCertificate);
                    break;
                default:
                    LOGGER.warn("Unknown common name in enrollment file " + subjectNameComponent.get("CN"));
            }
        }
    }

    @SuppressWarnings("java:S4248")
    private static X509Certificate getX509Certificate(String certificate) {
        String crtToDecode = certificate.replaceAll(BEGIN_CERT, "")
                .replaceAll(END_CERT, "").replaceAll("[\\n]+", "")
                .replaceAll("[\\r]+", "").replaceAll("[\\t]+", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(crtToDecode);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decoded)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(byteArrayInputStream);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid certificate format provided", ex);
        }
    }

    @SuppressWarnings("java:S4248")
    private static Map<String, String> getSubjectNameComponent(X509Certificate x509Certificate) {
        String[] subjectDnComponent = x509Certificate.getSubjectDN().getName()
                .replaceAll("\\s+", "").split(",");
        Map<String, String> subjectNameComponent = new HashMap<>();
        for (String component : subjectDnComponent) {
            String[] keyValue = component.split("=");
            subjectNameComponent.put(keyValue[0], keyValue[1]);
        }
        return subjectNameComponent;
    }

    private static void validateAndSetOssNodeProtocolParameter(JSONObject enrollmentFile,
                                                        Map<String, Object> ossNodeProtocolValue,
                                                        List<String> errorParameter) {
        for (Map.Entry<String, String> entry : ENROLLMENT_PARAMETER.entrySet()) {
            String value = null;
            try {
                value = JsonPath.read(enrollmentFile.toString(), entry.getValue());
            } catch (PathNotFoundException pnfe) {
                LOGGER.warn("Missing value {} in enrollment file", entry.getKey(), pnfe);
            }
            if (Strings.isBlank(value)) {
                errorParameter.add(entry.getKey());
            } else {
                ossNodeProtocolValue.put(entry.getKey(), value);
            }
        }
    }

    @VisibleForTesting
    JSONArray getAllCertificateFromJson(JSONObject enrollmentFile) {
        if (enrollmentFile.has(ENROLLMENT_INFO_STRING) && enrollmentFile.get(ENROLLMENT_INFO_STRING) instanceof JSONObject) {
            JSONObject enrollmentInfo = enrollmentFile.getJSONObject(ENROLLMENT_INFO_STRING);
            if (enrollmentInfo.has(VERBOSE_ENROLLMENT_INFO_STRING)
                    && enrollmentInfo.get(VERBOSE_ENROLLMENT_INFO_STRING) instanceof JSONObject) {
                return getTrustedCertificatesFromEnrollmentInfo(enrollmentInfo);
            }
        }
        throw new IllegalArgumentException(String.format(UNABLE_TO_FIND_CERTIFICATE_ERROR_MESSAGE,
                "some enrollment attributes are missing in the enrollment xml"));
    }

    private static JSONArray getTrustedCertificatesFromEnrollmentInfo(JSONObject enrollmentInfo) {
        JSONObject verboseEnrollmentInfo = enrollmentInfo.getJSONObject(VERBOSE_ENROLLMENT_INFO_STRING);
        if (verboseEnrollmentInfo.has(TRUSTED_CERTIFICATES_STRING)
                && verboseEnrollmentInfo.get(TRUSTED_CERTIFICATES_STRING) instanceof JSONObject) {
            JSONObject trustedCertificates = verboseEnrollmentInfo.getJSONObject(TRUSTED_CERTIFICATES_STRING);
            if (trustedCertificates.has(TRUSTED_CERTIFICATE_STRING) && trustedCertificates.get(TRUSTED_CERTIFICATE_STRING) instanceof JSONArray) {
                return trustedCertificates.getJSONArray(TRUSTED_CERTIFICATE_STRING);
            }
        }
        throw new IllegalArgumentException(String.format(UNABLE_TO_FIND_CERTIFICATE_ERROR_MESSAGE, "trustedCertificates " +
                "attributes are missing in the enrollmentInfo.verboseEnrollmentInfo section of enrollment xml"));
    }
}
