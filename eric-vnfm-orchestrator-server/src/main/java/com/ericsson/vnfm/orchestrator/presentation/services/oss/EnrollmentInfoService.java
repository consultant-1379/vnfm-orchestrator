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
package com.ericsson.vnfm.orchestrator.presentation.services.oss;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.FTL_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_CONFIGURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.FILE_TO_UPLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.GENERATED_XML_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.LDAP_DETAILS;
import static com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService.COMMAND_OUTPUT;
import static com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService.getCommonScriptAttributes;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.dontLogPasswords;
import static com.ericsson.vnfm.orchestrator.utils.SshResponseUtils.extractSpecificFailure;
import static com.ericsson.vnfm.orchestrator.utils.Utility.createTempPath;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.readFileContentThenDelete;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnrollmentInfoService {

    @Autowired
    private EnmTopologyService enmTopologyService;

    @Autowired
    private ObjectProvider<SshHelper> sshHelperProvider;

    @Autowired
    private InstanceService instanceService;

    public Map<String, String> getEnrollmentInfoFromENM(VnfInstance vnfInstance) {
        Path enrollmentConfigurationFile = createTempPath(ENROLLMENT_CONFIGURATION, "xml");
        Path sitebasicFile = getSitebasicFile(vnfInstance);
        try {
            Map<String, Object> topologyAttributes = getCommonScriptAttributes(vnfInstance, EnmOperationEnum.ENROLLMENT_INFO);
            topologyAttributes.put(FILE_TO_UPLOAD, sitebasicFile.getFileName());
            topologyAttributes.put(GENERATED_XML_FILE, enrollmentConfigurationFile.getFileName());
            Path generateGetEnrollmentInfoScript = enmTopologyService.generateGetEnrollmentInfoScript(topologyAttributes,
                                                                                                      EnmOperationEnum.ENROLLMENT_INFO);
            return generateEnrollmentInfo(vnfInstance,
                                          topologyAttributes,
                                          generateGetEnrollmentInfoScript,
                                          sitebasicFile,
                                          enrollmentConfigurationFile);
        } finally {
            deleteFile(sitebasicFile);
        }
    }

    private Map<String, String> generateEnrollmentInfo(final VnfInstance vnfInstance,
                                                       final Map<String, Object> topologyAttributes,
                                                       final Path script,
                                                       final Path sitebasicFile,
                                                       final Path enrollmentConfigurationFile) {
        SshHelper sshHelper = sshHelperProvider.getObject();
        SshResponse sshResponse = sshHelper.executeScriptWithFileParam(script, sitebasicFile);
        checkStatus(vnfInstance, topologyAttributes, sshResponse, EnmOperationEnum.ENROLLMENT_INFO);
        Path downloadedEnrollmentFile = sshHelper.downloadFile(enrollmentConfigurationFile);
        String enrollmentFileOutput = readFileContentThenDelete(downloadedEnrollmentFile);
        LOGGER.info("Enrollment file generated as {} and downloaded from ENM successfully.", downloadedEnrollmentFile.getFileName());
        String ldapDetailsAsString = getLdapDetailsAsString(sshResponse);
        Map<String, String> enrollmentInfo = new HashMap<>();
        enrollmentInfo.put(ENROLLMENT_CONFIGURATION, enrollmentFileOutput);
        enrollmentInfo.put(LDAP_DETAILS, ldapDetailsAsString);
        return enrollmentInfo;
    }

    private static String getLdapDetailsAsString(SshResponse sshResponse) {
        try {
            JSONObject jsonOutput = new JSONObject(sshResponse.getOutput());
            return jsonOutput
                    .getJSONObject(EnmOperationEnum.ENROLLMENT_INFO.getLdapDetails())
                    .get(COMMAND_OUTPUT)
                    .toString();
        } catch (JSONException e) {
            throw new InternalRuntimeException(String.format("Cannot get LdapDetails from ssh response due to :: %s", e.getMessage()), e);
        }
    }

    private void checkStatus(final VnfInstance vnfInstance, final Map<String, Object> topologyAttributes,
                             final SshResponse sshResponse, final EnmOperationEnum operation) {
        LOGGER.info("Command completed with status {}", sshResponse.getExitStatus());
        if (sshResponse.getExitStatus() == 0) {
            instanceService.addCommandResultToInstance(vnfInstance, operation);
        } else {
            String specificErrorMessage = extractSpecificFailure(sshResponse, operation);
            throw new FileExecutionException(String.format(FTL_EXCEPTION, operation.getOperation(), dontLogPasswords(topologyAttributes).toString(),
                                                           specificErrorMessage));
        }
    }

    public static Path getSitebasicFile(VnfInstance instance) {
        Path sitebasicFilePath = createTempPath("sitebasic", "xml");
        String sitebasicFileAsString = instance.getSitebasicFile();
        if (StringUtils.isNotEmpty(sitebasicFileAsString)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(sitebasicFilePath.toFile(), StandardCharsets.UTF_8))) {
                writer.write(sitebasicFileAsString);
                writer.flush();
                return sitebasicFilePath;
            } catch (IOException e) {
                throw new InvalidInputException(String.format("Failed to create temp file to store sitebasic.xml file :: %s", e.getMessage()), e);
            }
        } else {
            throw new InvalidInputException("Sitebasic information is not available in the vnf instance.");
        }
    }
}