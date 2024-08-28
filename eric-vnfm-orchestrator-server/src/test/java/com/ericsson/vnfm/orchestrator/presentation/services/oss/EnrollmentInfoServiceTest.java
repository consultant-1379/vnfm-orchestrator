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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import static com.ericsson.vnfm.orchestrator.TestUtils.createDuplicateResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_CONFIGURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.LDAP_DETAILS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.FreemarkerConfiguration;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        FreemarkerConfiguration.class,
        EnrollmentInfoService.class,
        InstanceService.class,
        EnmTopologyService.class
})
@MockBean(classes = {
        OnboardingConfig.class,
        NfvoConfig.class,
        OnboardingClient.class,
        OnboardingUriProvider.class,
        RestTemplate.class,
        VnfInstanceQuery.class,
        DatabaseInteractionService.class,
        OperationsInProgressRepository.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ChangePackageOperationDetailsRepository.class,
        VnfInstanceService.class,
        ObjectMapper.class,
        YamlMapFactoryBean.class,
        ExtensionsService.class,
        InstantiationLevelService.class,
        PackageService.class,
        ValuesFileService.class,
        NotificationService.class,
        InstanceUtils.class,
        VnfInstanceMapper.class,
        AdditionalAttributesHelper.class,
        LifecycleOperationHelper.class,
        LcmOpSearchService.class,
        ReplicaCountCalculationService.class,
        LifeCycleManagementHelper.class,
        ChangePackageOperationDetailsService.class,
        ChangeVnfPackageService.class
})
public class EnrollmentInfoServiceTest {

    @Autowired
    private EnrollmentInfoService enrollmentInfoService;

    @MockBean
    private SshHelper sshHelper;

    @MockBean(name = "nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @TempDir
    public File temporaryFolder;

    @Test
    public void successGetEnrollmentInfoFromENM() throws IOException {
        VnfInstance instance = createVnfInstance();
        String sshResponseOutput = readDataFromFile(getClass(),
                "enrollment-info/generateEnrollmentResponseSuccess.json");
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(0);
        sshResponse.setOutput(sshResponseOutput);
        Path enrollmentConfigurationFile = createDuplicateResource(getClass(),"enrollment-info/enrollmentFile.xml", temporaryFolder);
        doReturn(enrollmentConfigurationFile).when(sshHelper).downloadFile(any());
        doReturn(sshResponse).when(sshHelper).executeScriptWithFileParam(any(), any());
        Map enrollmentOutput = enrollmentInfoService.getEnrollmentInfoFromENM(instance);
        String enrollmentFileString = (String) enrollmentOutput.get(ENROLLMENT_CONFIGURATION);
        String ldapDetailsString = (String) enrollmentOutput.get(LDAP_DETAILS);

        assertThat(ldapDetailsString).contains("{\"bindDn\":\"cn=ProxyAccount_3,ou=proxyagent,ou=com,dc=ieatenmc4a10-39,dc=com\","
                                                       + "\"ldapIpv6Address\":\"2001:1b70:6207:0029:0000:0878:1010:0029\",");
        assertThat(enrollmentFileString)
                .contains("<certificateFingerPrint>C1:8A:F4:FC:3E:2D:17:FF:C4:C6:64:38:46:60:76:84:2A:61:CA:43:48:B7:C9:50:3E:63:2B:A3:ED:E2:92:98"
                                  + "</certificateFingerPrint>")
                .contains("<url>http://131.160.205.49:8091/pkira-cmp/NE_OAM_CA/synch</url>")
                .contains("</enrollmentInfo>")
                .contains("<challengePassword>64FezTM2LL6ryNWkAXNu1</challengePassword>")
                .contains("<issuerCA>OU=Athlone, O=Ericsson, C=IE, CN=NE_OAM_CA</issuerCA>")
                .contains("<keyInfo>RSA_2048</keyInfo>");
    }

    @Test
    public void invalidLdapDetailsFromSshResponse() throws IOException {
        VnfInstance instance = createVnfInstance();
        String sshResponseOutput = readDataFromFile(getClass(),
                "enrollment-info/generateEnrollmentResponseWithInvalidLdap.txt");
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(0);
        sshResponse.setOutput(sshResponseOutput);
        Path enrollmentConfigurationFile = createDuplicateResource(getClass(),"enrollment-info/enrollmentFile.xml", temporaryFolder);
        doReturn(enrollmentConfigurationFile).when(sshHelper).downloadFile(any());
        doReturn(sshResponse).when(sshHelper).executeScriptWithFileParam(any(), any());
        assertThatThrownBy(() -> enrollmentInfoService.getEnrollmentInfoFromENM(instance))
                .isInstanceOf(InternalRuntimeException.class).hasMessageContaining("Cannot get LdapDetails from ssh response");
    }

    @Test
    public void failedGetEnrollmentInfoFromENMWithFailResponse() {
        VnfInstance instance = createVnfInstance();
        String sshResponseOutput = readDataFromFile(getClass(),
                "enrollment-info/generateEnrollmentResponseFailed.json");
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(1);
        sshResponse.setOutput(sshResponseOutput);
        doReturn(sshResponse).when(sshHelper).executeScriptWithFileParam(any(), any());
        FileExecutionException exception = assertThrows(FileExecutionException.class,
                                                        () -> enrollmentInfoService.getEnrollmentInfoFromENM(instance));
        assertThat(exception).hasMessageContaining("Failed to generateEnrollmentInfo with");
    }

    private VnfInstance createVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setSitebasicFile("<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\r\\n<Nodes>\\r\\n   <Node>\\r\\n      "
                                          + "<nodeFdn>VPP00001</nodeFdn>\\r\\n         <certType>OAM</certType>\\r\\n      "
                                          + "<enrollmentMode>CMPv2_INITIAL</enrollmentMode>\\r\\n   </Node>\\r\\n</Nodes>\\r\\n\\r\\n");
        instance.setVnfInstanceId("vnfInstanceId");
        instance.setAddNodeOssTopology("{\"managedElementId\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"elementId\"},\n"
                                               + " \"networkElementType\":{\"type\":\"string\",\"required\":\"true\",\"default\":\"nodetype\"},\n"
                                               + " \"networkElementVersion\":{\"type\":\"string\",\"required\":\"false\","
                                               + "\"default\":\"nodeVersion\"},\n"
                                               + " \"nodeIpAddress\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"my-ip\"},\n"
                                               + " \"networkElementUsername\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"admin\"},\n"
                                               + " \"networkElementPassword\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"password"
                                               + "\"}}");
        return instance;
    }
}
