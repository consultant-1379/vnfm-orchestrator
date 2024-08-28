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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DOWNSIZE_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.isResourcesAllowedByVnfd;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.EvnfmOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.NfvoOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClientImpl;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.ericsson.vnfm.orchestrator.utils.VnfdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        PackageServiceImpl.class,
        InstanceService.class,
        ObjectMapper.class,
        LifeCycleManagementHelper.class
})
@MockBean({
        VnfInstanceRepository.class,
        LifecycleOperationRepository.class,
        OnboardingClientImpl.class,
        OnboardingUriProvider.class,
        EvnfmOnboardingRoutingClient.class,
        NfvoOnboardingRoutingClient.class,
        NfvoConfig.class,
        RestTemplate.class,
        VnfInstanceQuery.class,
        DatabaseInteractionService.class,
        OperationsInProgressRepository.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ChangePackageOperationDetailsService.class,
        ChangePackageOperationDetailsRepository.class,
        VnfInstanceService.class,
        YamlMapFactoryBean.class,
        ExtensionsService.class,
        InstantiationLevelService.class,
        ValuesFileService.class,
        NotificationService.class,
        InstanceUtils.class,
        VnfInstanceMapper.class,
        AdditionalAttributesHelper.class,
        LifecycleOperationHelper.class,
        LcmOpSearchService.class,
        ReplicaCountCalculationService.class,
        ClusterConfigService.class,
        HelmClientVersionValidator.class,
        ChangeVnfPackageService.class,
        WorkflowRoutingService.class
})
public class InstanceServiceTopologyTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;
    @Autowired
    private InstanceService instanceService;

    @Autowired
    private PackageService packageService;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @MockBean
    private OnboardingConfig onboardingConfig;

    @MockBean
    private OnboardingClient onboardingClient;

    @Test
    public void testGetVnfdDescriptorModel() throws IOException {
        //given
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId("testId");
        PackageResponse[] packageResponses = new PackageResponse[] {packageResponse};

        ResponseEntity<PackageResponse[]> mockedResponseWithPackageResponse =
                new ResponseEntity(packageResponses, HttpStatus.OK);

        ResponseEntity mockedResponseWithYaml =
                new ResponseEntity(readDataFromFile(getClass(), "vnfd-with-scaling.yaml"), HttpStatus.OK);

        //when
        createOnboardingConfig();
        when(onboardingClient.get(any(), any(), eq(PackageResponse[].class)))
                .thenReturn(Optional.ofNullable(mockedResponseWithPackageResponse.getBody()));
        when(onboardingClient.get(any(), any(), eq(String.class)))
                .thenReturn(Optional.ofNullable((String)mockedResponseWithYaml.getBody()));

        //then
        PackageResponse packageInfo =
                packageService.getPackageInfoWithDescriptorModel("test-vnfPkId-with-scaling");
        JSONObject vnfdDescriptorModel = new JSONObject(packageInfo.getDescriptorModel());
        assertThat(vnfdDescriptorModel).isNotNull();
    }

    @Test
    public void testGetInstantiateNameFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModel.json");
        String type = "ericsson.datatypes.nfv.InstantiateVnfOperationAdditionalParameters";
        JSONObject jsonObject = new JSONObject(descriptorModel);
        String instantiateNameType = InstanceService.getInstantiateNameFromVnfd(jsonObject);
        assertThat(instantiateNameType).isEqualTo(type);
    }

    @Test
    public void testSetOssTopology() throws IOException {
        //given
        VnfInstance instance = new VnfInstance();
        String mockedResponse = readDataFromFile(getClass(), "vnfd-without-scaling.yaml");

        //when
        createOnboardingConfig();
        when(onboardingClient.get(any(), any(), any())).thenReturn(Optional.ofNullable(mockedResponse));

        //then
        PackageResponse packageInfo = new PackageResponse();
        packageInfo.setDescriptorModel(packageService.getVnfd("test-vnfPkId-without-scaling").toString());
        instanceService.setOssTopology(instance, packageInfo);
        assertThat(instance.getOssTopology()).isEqualTo(getOssToplogy());
    }

    @Test
    public void testGetChangepackageNameFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModel.json");
        String type = "ericsson.datatypes.nfv.ChangePackageVnfOperationAdditionalParameters";
        JSONObject jsonObject = new JSONObject(descriptorModel);
        String changePackageNameFromVnfd = VnfdUtils.getChangePackageNameFromVnfd(jsonObject);
        assertThat(changePackageNameFromVnfd).isEqualTo(type);
    }

    @Test
    public void testGetDownsizeFalseFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModel.json");
        JSONObject vnfdDescriptorModel = new JSONObject(descriptorModel);
        assertThat(isResourcesAllowedByVnfd(vnfdDescriptorModel, DOWNSIZE_VNFD_KEY)).isFalse();
    }

    @Test
    public void testGetDownsizeTrueFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModelDownsizeTrue.json");
        JSONObject vnfdDescriptorModel = new JSONObject(descriptorModel);
        assertThat(isResourcesAllowedByVnfd(vnfdDescriptorModel, DOWNSIZE_VNFD_KEY)).isTrue();
    }

    @Test
    public void testDownsizeWrongTypeFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModelDownsizeNotBoolean.json");
        JSONObject vnfdDescriptorModel = new JSONObject(descriptorModel);
        assertThat(isResourcesAllowedByVnfd(vnfdDescriptorModel, DOWNSIZE_VNFD_KEY)).isFalse();
    }

    @Test
    public void testDownsizeNotfoundFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModelWithScaleInterface.json");
        JSONObject vnfdDescriptorModel = new JSONObject(descriptorModel);
        assertThat(isResourcesAllowedByVnfd(vnfdDescriptorModel, DOWNSIZE_VNFD_KEY)).isFalse();
    }

    @Test
    public void testDownsizeNoDefaultFromVnfdFromValidModel() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModelDownsizeNoDefault.json");
        JSONObject vnfdDescriptorModel = new JSONObject(descriptorModel);
        assertThat(isResourcesAllowedByVnfd(vnfdDescriptorModel, DOWNSIZE_VNFD_KEY)).isFalse();
    }

    public static String getOssToplogy() {
        return "{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}";
    }

    public void createOnboardingConfig() {
        when(onboardingConfig.getHost()).thenReturn("http://localhost2");
        when(onboardingConfig.getPath()).thenReturn("somepath");
        when(onboardingConfig.getQueryValue()).thenReturn("somevalue");
    }
}

