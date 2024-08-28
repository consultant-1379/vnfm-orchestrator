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

import java.net.StandardProtocolFamily;
import java.nio.file.Path;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;

/**
 * API for interacting with an OSS system, such as ENM
 */
public interface OssTopologyService {

    /**
     * Generate the script to add a VNF instance to an OSS system
     * @param topologyAttributes The attributes to be replaced in the script template
     * @return The path to the generated script
     */
    Path generateAddNodeScript(Map<String, Object> topologyAttributes);

    /**
     * Generate the script to deleteFile a VNF instance from an OSS system
     * @param topologyAttributes The attributes to be replaced in the script template
     * @return The path to the generated script
     */
    Path generateDeleteNodeScript(Map<String, Object> topologyAttributes);

    /**
     * Generate the script to enable Supervision in an OSS system
     * @param topologyAttributes The attributes to be replaced in the script template
     * @return The path to the generated script
     */
    Path generateEnableSupervisionsScript(Map<String, Object> topologyAttributes);

    /**
     * Generate the script to enable/disable FM Alarm Supervision in an OSS system
     * @param topologyAttributes The attributes to be replaced in the script template
     * @param operationEnum enum for the operation type
     * @return The path to the generated script
     */
    Path generateSetAlarmScript(Map<String, Object> topologyAttributes, EnmOperationEnum operationEnum);

    /**
     * Generate OssNodeProtocol file string from enrollment file
     *
     * @param enrollmentFileOutput
     * @param ldapDetails
     * @param ipVersion
     * @return String of OssNodeProtocol
     */
    String getOssNodeProtocolFileContent(String enrollmentFileOutput, String ldapDetails,
                                         StandardProtocolFamily ipVersion);
    /**
     * Generate LDAP configuration from ENM LDAP details
     *
     * @param ldapDetails
     * @param ipVersion
     * @return JSON String with external LDAP configuration
     */
    String generateLdapConfigurationJSONString(String ldapDetails, StandardProtocolFamily ipVersion);

    /**
     * Generate enrollment configuration from enrollment file
     *
     * @param enrollmentConfigString
     * @return JSON String with CMP enrollment configuration
     */
    String generateEnrollmentConfigurationJSONString(String enrollmentConfigString);

    /**
     * Generate OssNodeProtocol file from enrollment file
     *
     * @param enrollmentFileOutput
     * @param ldapDetails
     * @param ipVersion
     * @return Path to OssNodeProtocol file
     */
    Path generateOssNodeProtocolFilePath(String enrollmentFileOutput, String ldapDetails,
                                    StandardProtocolFamily ipVersion);

    /**
     * Generate the script to generate enrollment information in an OSS system
     * @param topologyAttributes The attributes to be replaced in the script template
     * @param operationEnum enum for the operation type
     * @return The path to the generated script
     */
    Path generateGetEnrollmentInfoScript(Map<String, Object> topologyAttributes, EnmOperationEnum operationEnum);

    /***
     *
     * Generate the script to restore using specified Backup File reference
     * @param topologyAttributes The attributes to be replaced in the script template
     * @param templateName Restore backup ftl template name
     * @param enmOperationEnum restore operation
     * @return The path to the generated script
     */
    Path generateRestoreScript(Map<String, Object> topologyAttributes, String templateName, EnmOperationEnum enmOperationEnum);

    /***
     *
     * Generate the script to check whether node is present in ENM
     * @param topologyAttributes The attributes to be replaced in the script template
     * @return The path to the generated script
     */
    Path generateCheckNodePresentScript(Map<String, Object> topologyAttributes);
}
