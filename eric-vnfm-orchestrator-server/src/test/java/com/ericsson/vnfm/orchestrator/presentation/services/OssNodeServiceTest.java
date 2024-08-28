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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_CONFIGURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.LDAP_DETAILS;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENABLE_ALARM_SUPERVISION;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.FreemarkerConfiguration;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
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
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ActiveProfiles("test")
@SpringBootTest(classes = {
        OssNodeService.class,
        EnmTopologyService.class,
        InstanceService.class,
        ObjectMapper.class,
        Configuration.class,
        PackageService.class,
        FreemarkerConfiguration.class,
        IdempotencyContext.class
})
@MockBean({
        RestoreBackupFromEnm.class,
        EnrollmentInfoService.class,
        NotificationService.class,
        OnboardingConfig.class,
        NfvoConfig.class,
        OnboardingClient.class,
        OnboardingUriProvider.class,
        RestTemplate.class,
        VnfInstanceQuery.class,
        OperationsInProgressRepository.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ChangePackageOperationDetailsRepository.class,
        ChangePackageOperationDetailsService.class,
        VnfInstanceService.class,
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
        LifeCycleManagementHelper.class,
        ReplicaCountCalculationService.class,
        ChangeVnfPackageService.class
})
public class OssNodeServiceTest {

    private final String OSS_TOPOLOGY = """
        {
        "managedElementId": "test-node",
        "networkElementType": "Shared-CNF",
        "networkElementVersion": "1.0.0",
        "nodeIpAddress": "10.203.101.100",
        "networkElementUsername": "user",
        "networkElementPassword": "passwd"
       }
       """;

    @Autowired
    @InjectMocks
    private OssNodeService ossNodeService;

    @Autowired
    IdempotencyContext idempotencyContext;

    @MockBean
    private EnmMetricsExposers enmMetricsExposers;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private SshHelper sshHelper;

    @MockBean
    private EnrollmentInfoService enrollmentInfoService;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;
    @MockBean
    private ValueOperations<String, String> valueOps;

    @AfterEach
    void cleanUp() {
        idempotencyContext.clear();
    }

    @Test
    public void successSetAlarmSupervisionInEnm() {
        //given
        VnfInstance instance = createVnfInstance(null);

        //when
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));

        //then
        assertThat(instance.getAlarmSupervisionStatus()).isNullOrEmpty();
        ossNodeService.setAlarmSuperVisionInENM(instance, ENABLE_ALARM_SUPERVISION);
        assertThat(instance.getAlarmSupervisionStatus()).isEqualTo("on");
        ossNodeService.setAlarmSuperVisionInENM(instance, DISABLE_ALARM_SUPERVISION);
        assertThat(instance.getAlarmSupervisionStatus()).isEqualTo("off");
    }

    @Test
    public void failedEnableAlarmSupervisionInEnm() {
        //given
        VnfInstance instance = createVnfInstance("off");

        LifecycleOperation lifecycleOperation = new LifecycleOperation();

        //when
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(1));
        when(databaseInteractionService.getLifecycleOperation(any())).thenReturn(lifecycleOperation);
        when(databaseInteractionService.persistLifecycleOperation(any())).thenReturn(any());

        //then
        FileExecutionException exception = assertThrows(FileExecutionException.class,
                () -> ossNodeService.setAlarmSuperVisionInENM(instance, ENABLE_ALARM_SUPERVISION));
        assertThat(instance.getAlarmSupervisionStatus()).isEqualTo("off");
        assertThat(lifecycleOperation.getSetAlarmSupervisionErrorMessage()).isNotEmpty();
        assertThat(exception).hasMessageContaining("Failed to enableAlarmSupervision");
    }

    @Test
    public void skippingDisableAlarmSupervisionInEnm() {
        //given
        VnfInstance instance = createVnfInstance("off");

        //when
        ossNodeService.setAlarmSuperVisionInENM(instance, DISABLE_ALARM_SUPERVISION);

        //then
        assertThat(instance.getAlarmSupervisionStatus()).isEqualTo("off");
    }

    @Test
    public void addOssNodeProtocolFileToConfig0Success() throws IOException, URISyntaxException {
        //given
        String enrollmentFileOutput = readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetailsAsString = readDataFromFile("enrollmentAndLdapFile/validLdapDetails.json");
        Map<String, String> enrollmentInfo = new HashMap<>();
        enrollmentInfo.put(ENROLLMENT_CONFIGURATION, enrollmentFileOutput);
        enrollmentInfo.put(LDAP_DETAILS, ldapDetailsAsString);

        //when
        doReturn(enrollmentInfo).when(enrollmentInfoService).getEnrollmentInfoFromENM(any());
        String ossNodeProtocolFile = ossNodeService.generateOssNodeProtocolFile(new VnfInstance(), StandardProtocolFamily.INET);

        //then
        assertThat(ossNodeProtocolFile)
                .isNotNull()
                .contains("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
                .contains("<install-certificate-pem xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext\">")
                .contains("<name>OU=Athlone,O=Ericsson,C=IE,CN=NE_OAM_CA</name>")
                .contains("<certificate-name>oamNodeCredential</certificate-name>")
                .contains("<port>1636</port>")
                .contains("<rpc message-id=\"Close Session\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
    }

    @Test
    public void addOssNodeProtocolFileToConfig0Fails() {
        doReturn(new HashMap<>()).when(enrollmentInfoService).getEnrollmentInfoFromENM(any());
        assertThatThrownBy(() -> ossNodeService.generateOssNodeProtocolFile(new VnfInstance(), StandardProtocolFamily.INET))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("enrollment details are either null or empty");
    }

    @Test
    public void checkNodeIsPresentInEnmSuccess() {
        //given
        VnfInstance instance = createVnfInstance(null);
        //when
        when(sshHelper.executeScript(any())).thenReturn(
                createSshResponse(0, "{\"check_node_data\": {\"exitStatus\":0, \"commandOutput\":\"Found 1 instance(s)\"}}"));
        //then
        assertThat(ossNodeService.checkNodePresent(instance)).isEqualTo(true);
    }

    @Test
    public void checkNodeNotPresentInEnmSuccess() {
        //given
        VnfInstance instance = createVnfInstance(null);
        //when
        when(sshHelper.executeScript(any())).thenReturn(
                createSshResponse(0, "{\"check_node_data\": {\"exitStatus\":0, \"commandOutput\":\"Found 0 instance(s)\"}}"));
        //then
        assertThat(ossNodeService.checkNodePresent(instance)).isEqualTo(false);
    }

    @Test
    public void checkNodePresentInEnmFail() {
        //given
        VnfInstance instance = createVnfInstance(null);
        //when
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(1));
        //then
        assertThrows(InternalRuntimeException.class, () -> ossNodeService.checkNodePresent(instance));
    }

    @Test
    void addNodeTwiceWithSameIdempotencyId() {
        //given
        idempotencyContext.setIdempotencyId(UUID.randomUUID().toString());
        VnfInstance vnfInstance = createVnfInstance(null);

        //when
        Map<String, String> redisMock = new HashMap<>();
        when(valueOps.get(any())).thenAnswer(mock -> redisMock.get(mock.<String>getArgument(0)));
        when(valueOps.setIfAbsent(any(), any(), any())).thenAnswer(mock -> redisMock.put(mock.getArgument(0), mock.getArgument(1)));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));

        when(enmMetricsExposers.getAddNodeMetric()).thenAnswer(mock -> new EnmMetricsExposers.AddNodeOperations(new SimpleMeterRegistry()));

        ossNodeService.addNode(vnfInstance, Map.of(), null);
        ossNodeService.addNode(vnfInstance, Map.of(), null);

        //then
        verify(sshHelper, times(1)).executeScript(any());
        verify(valueOps, times(1)).setIfAbsent(any(), any(), any());
    }

    private SshResponse createSshResponse(int exitStatus, String output) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setOutput(output);
        sshResponse.setExitStatus(exitStatus);
        return sshResponse;
    }

    private SshResponse createSshResponse(int exitStatus) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(exitStatus);
        return  sshResponse;
    }

    private VnfInstance createVnfInstance(String alarmSupervisionStatus) {
        VnfInstance instance = new VnfInstance();
        instance.setAddNodeOssTopology(OSS_TOPOLOGY);
        instance.setVnfInstanceId("someId");
        instance.setAddedToOss(true);
        instance.setAlarmSupervisionStatus(alarmSupervisionStatus);
        return instance;
    }
}
