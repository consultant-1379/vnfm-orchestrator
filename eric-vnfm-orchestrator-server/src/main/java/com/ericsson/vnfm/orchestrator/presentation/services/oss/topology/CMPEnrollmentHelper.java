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

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.StandardProtocolFamily;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETNAME_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETNAME_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_SECRETS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.DAY0_CONFIGURATION_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.OSS_NODE_PROTOCOL_FILE_KEY_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.ENROLLMENT_CERTM_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.EXTERNAL_LDAP_SECRET_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.parameterPresent;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.includeAdditionalCertForSpecificNode;
import static com.ericsson.vnfm.orchestrator.utils.CMPV2EnrollmentUtility.checkInstanceRequiresAdditionalCertificate;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToProtocol;

@Component
@Slf4j
public class CMPEnrollmentHelper {

    @Autowired
    private OssNodeService ossNodeService;
    @Value("${oss.topology.enrollment.additionalCertificate.path}")
    private String additionalCertificatePath;
    @Value("${oss.topology.enrollment.additionalCertificate.productNames}")
    private String productNamesForAdditionalCert;

    public void addNodeToENMWithEnrollment(VnfInstance vnfInstance,
                                           Map<String, Object> additionalParams) {
        try {
            ossNodeService.addNode(vnfInstance);
            Map<String, Object> day0secrets = createDay0ConfigSecrets(vnfInstance, additionalParams);
            additionalParams.putAll(day0secrets);
        } catch (Exception e) {
            String message = String.format("Adding node to ENM with Enrollment generation failed for vnf [%s] with error: %s",
                    vnfInstance.getVnfInstanceName(), e.getMessage());
            LOGGER.error(message);
            if (vnfInstance.isAddedToOss()) {
                ossNodeService.deleteNodeFromENM(vnfInstance, false);
                vnfInstance.setAddedToOss(false);
            }
            throw new InternalRuntimeException(message);
        }
    }

    public void generateAndSaveOssNodeProtocolFile(final VnfInstance vnfInstance,
                                                   final Map<String, Object> additionalParams) {
        LOGGER.info("Generating ossNodeProtocol file");
        StandardProtocolFamily ipVersion = getIpVersion(additionalParams);
        String ossNodeProtocolAsString = ossNodeService.generateOssNodeProtocolFile(vnfInstance, ipVersion);
        vnfInstance.setOssNodeProtocolFile(ossNodeProtocolAsString);
        LOGGER.info("Successfully generated ossNodeProtocol file");
    }

    private Map<String, Object> createDay0ConfigSecrets(final VnfInstance vnfInstance, final Map<String, Object> additionalParams) {
        Map<String, Object> day0Secrets = new HashMap<>();
        Map<String, Object> otpDay0Params = new HashMap<>();
        day0Secrets.put(DAY0_CONFIGURATION_SECRETS, otpDay0Params);
        otpDay0Params.put(EXTERNAL_LDAP_SECRET_NAME,
                          Map.of(EXTERNAL_LDAP_SECRET_KEY, createLDAPConfigContent(vnfInstance, additionalParams)));
        otpDay0Params.put(ENROLLMENT_CERTM_SECRET_NAME,
                          Map.of(ENROLLMENT_CERTM_SECRET_KEY, createEnrollmentConfigContent(vnfInstance)));
        return day0Secrets;
    }

    private String createLDAPConfigContent(final VnfInstance vnfInstance,
                                          final Map<String, Object> additionalParams) {
        LOGGER.info("Generating external LDAP configuration");
        StandardProtocolFamily ipVersion = getIpVersion(additionalParams);
        String ldapServerConfiguration = ossNodeService.generateLDAPServerConfiguration(vnfInstance, ipVersion);
        LOGGER.info("Successfully generated external LDAP configuration");
        return  ldapServerConfiguration;
    }

    private String createEnrollmentConfigContent(final VnfInstance vnfInstance) {
        LOGGER.info("Generating certificate management configuration from enrollment");
        String enrollmentConfig = ossNodeService.generateCertificateEnrollmentConfiguration(vnfInstance);
        if (checkInstanceRequiresAdditionalCertificate(productNamesForAdditionalCert, vnfInstance.getVnfProductName())) {
            LOGGER.info("{} deployment detected, including additional certificate to day-0 configuration", vnfInstance.getVnfProductName());
            enrollmentConfig = includeAdditionalCertForSpecificNode(enrollmentConfig, additionalCertificatePath);
        }
        LOGGER.info("Successfully generated certificate management configuration");
        return  enrollmentConfig;
    }

    public static Map<String, Object> createDay0ConfigurationParams(VnfInstance vnfInstance) {
        return Optional.ofNullable(vnfInstance.getOssNodeProtocolFile())
                .map(CMPEnrollmentHelper::addOssNodeProtocolFileAsConfig0)
                .orElse(new HashMap<>());
    }

    private static Map<String, Object> addOssNodeProtocolFileAsConfig0(String ossNodeProtocolAsString) {
        Map<String, Object> day0Params = new HashMap<>();
        day0Params.put(DAY0_CONFIGURATION_SECRETNAME_KEY, DAY0_CONFIGURATION_SECRETNAME_VALUE);
        day0Params.put(String.format(DAY0_CONFIGURATION_KEY, 1), OSS_NODE_PROTOCOL_FILE_KEY_NAME);
        day0Params.put(String.format(DAY0_CONFIGURATION_VALUE, 1), ossNodeProtocolAsString);
        LOGGER.info("Day0 configuration parameters were created: {}, {}, {}.",
                DAY0_CONFIGURATION_SECRETNAME_KEY, DAY0_CONFIGURATION_KEY, DAY0_CONFIGURATION_VALUE);
        return day0Params;
    }

    private static StandardProtocolFamily getIpVersion(Map<String, Object> additionalParams) {
        String ipVersion = parameterPresent(additionalParams, IP_VERSION)
                ? (String) additionalParams.get(IP_VERSION)
                : "";
        return convertStringToProtocol(ipVersion);
    }

}
