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

import static java.util.Collections.singletonList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.TestUtils.ONBOARDING_PATH;
import static com.ericsson.vnfm.orchestrator.TestUtils.REPO_DOWNGRADE_NOT_SUPPORTED_NO_POLICY_VNF_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.REPO_DOWNGRADE_NOT_SUPPORTED_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.REPO_DOWNGRADE_NO_PACKAGE_VNF_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.REPO_DOWNGRADE_SUPPORTED_VNF_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.REPO_DOWNGRADE_SUPPORTED_VNF_PACKAGE_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_DOWNGRADE_SUPPORTED_SOURCE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_DOWNGRADE_SUPPORTED_TARGET_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_PACKAGE_ID_BY_FILTER;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_PACKAGE_VERSION;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_UPDATE_USAGE_STATE_VNF_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_VNFD_CHARTS_QUANTITY;
import static com.ericsson.vnfm.orchestrator.TestUtils.STUB_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.createCnfHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.parseJsonFile;
import static com.ericsson.vnfm.orchestrator.TestUtils.prepareHelmCHarts;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.TestUtils.setDefaultPriority;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.SKIP_IMAGE_UPLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.URI_PATH_SEPARATOR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.USAGE_STATE_API;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.SITEBASIC_XML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService.getSitebasicFile;
import static com.ericsson.vnfm.orchestrator.utils.ScalingUtils.getCommentedScaleInfo;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.AddNodeToVnfInstanceByIdRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.DowngradeInfo;
import com.ericsson.vnfm.orchestrator.model.DowngradePackageInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeInfoInstanceIdNotPresentException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradePackageDeletedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@TestPropertySource(properties = { "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
@Slf4j
public class InstanceServiceTest extends AbstractDbSetupTest {

    private static final String DUMMY_PACKAGE_ID = "packageId";
    private static final String DUMMY_INSTANCE_ID = "instanceId";
    private static final String NFVO_CONFIG_NAME = "nfvoConfig";
    private static final String SMALL_STACK_APPLICATION_PARAM_NAME = "smallStackApplication";
    private static final String DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE = "Downgrade not supported for instance id %s";
    private static final String DOWNGRADE_NOT_SUPPORTED_AS_DOWNGRADE_PACKAGE_IS_ERROR_MESSAGE =
            "Downgrade not supported for instance id %s as the target downgrade package is no longer available";
    private static final String PACKAGE_SERVICE_FIELD_NAME = "packageService";

    private static final String CRD_PACKAGE_KEY = "crd_package";
    private static final String HELM_PACKAGE_KEY = "helm_package";

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ValuesFileService valuesService;

    @Spy
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ReplicaDetailsService replicaDetailsService;

    @Value("${onboarding.host}")
    private String onboardingHost;

    @Spy
    @Autowired
    private PackageService packageService;

    @Autowired
    private VnfdHelper vnfdHelper;

    @Autowired
    private AdditionalAttributesHelper additionalAttributesHelper;

    @Autowired
    private LifecycleOperationHelper lifecycleOperationHelper;

    @Autowired
    private ClusterConfigInstanceRepository clusterConfigInstanceRepository;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void init() {
        // Setup and inject spy for rest template to make call verifications possible
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(instanceService, PACKAGE_SERVICE_FIELD_NAME, packageService);

        // Reset nfvoConfig to disabled to avoid cross-test side effects
        setUpNfvoConfig(false);
    }

    @Test
    public void testCreateInstanceEntity() {
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(STUB_VNFD_ID);

        assertThat(packageInfo).isNotNull();

        VnfInstance vnfInstance =
                instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest(STUB_VNFD_ID), Optional.empty());
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();

        assertThat(vnfInstance).isNotNull();
        assertThat(vnfInstance.getVnfDescriptorId()).isEqualTo(STUB_VNFD_ID);
        assertThat(vnfInstance.getHelmCharts()).hasSize(STUB_VNFD_CHARTS_QUANTITY);
        assertThat(vnfInstance.getManoControlledScaling()).isNull();
        helmCharts.stream().forEach(helmChart -> {
            assertThat(helmChart.getOperationChartsPriority()).isNotEmpty();
        });
    }

    @Test
    public void testReplicaDetailsCreatedWhenNoMappingFile() {
        String vnfd = packageService.getVnfd("no-mappingDOWNGRADE").toString();
        Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(vnfd));
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel("def1ce-4cf4-477c-aab3-2b04e6a382");
        VnfInstance vnfInstance = instanceService.
                createVnfInstanceEntity(packageInfo, getCreateVnfRequest("def1ce-4cf4-477c-aab3-2b04e6a382"), policies);
        replicaDetailsService.setReplicaDetailsToVnfInstance(packageInfo.getDescriptorModel(), vnfInstance);
        assertThat(vnfInstance).isNotNull();
        assertThat(vnfInstance.getHelmCharts().get(0).getReplicaDetails()).isNotNull();
        assertThat(vnfInstance.getHelmCharts().get(1).getReplicaDetails()).isNotNull();
    }

    @Test
    public void testCreateInstanceEntityShouldGenerateReleaseNameWithSuffixWhenOnlyOneHelmChart() {
        String vnfdId = "single-helm-chart";
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(vnfdId);

        VnfInstance vnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest(vnfdId), Optional.empty());

        assertThat(vnfInstance.getHelmCharts())
                .hasSize(1)
                .extracting(HelmChart::getReleaseName)
                .containsOnly("test_application-1");
    }

    @Test
    public void testCreateInstanceEntityShouldGenerateReleaseNamesWithSuffixWhenMultipleHelmCharts() {
        String vnfdId = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(vnfdId);

        VnfInstance updatedVnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest(vnfdId), Optional.empty());

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-1", "test_application-2");
    }

    @Test
    public void testUpdateInstanceEntityShouldCreateReleaseNameWhenUpgradingFromSingleToMultiHelmCharts() {
        VnfInstance sourceVnfInstance = new VnfInstance();

        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        // release name for single chart generated in the old style
        HelmChart sourceChart = new HelmChart();
        sourceChart.setHelmChartType(HelmChartType.CNF);
        sourceChart.setReleaseName("test_application");
        sourceVnfInstance.setHelmCharts(singletonList(sourceChart));

        String targetVnfdId = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
        PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);

        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);

        updatedVnfInstance.setHelmCharts(sourceVnfInstance.getHelmCharts());

        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().forEach(helmChart -> {
            String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
            helmChart.setReleaseName(newReleaseName);
        });

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(1)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-2");
    }

    @Test
    public void testUpdateInstanceEntityShouldWhenUpgradingFromSingleToMultiHelmChartsWithFirstNotGeneratedSuffix() {
        VnfInstance sourceVnfInstance = new VnfInstance();

        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        // release name for single chart generated in the old style
        HelmChart sourceChart = new HelmChart();
        sourceChart.setHelmChartType(HelmChartType.CNF);
        sourceChart.setReleaseName("test_application");
        sourceVnfInstance.setHelmCharts(singletonList(sourceChart));

        String targetVnfdId = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
        PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);

        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);

        updatedVnfInstance.getHelmCharts().add(sourceChart);

        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName() == null)
                .forEach(helmChart -> {
                    String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
                    helmChart.setReleaseName(newReleaseName);
                });

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(3)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application", "test_application-2", "test_application-3");
    }

    @Test
    public void testUpdateInstanceEntityShouldWhenUpgradingFromSingleToMultiHelmChartsWithFirstGeneratedSuffix() {
        VnfInstance sourceVnfInstance = new VnfInstance();

        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        // release name for single chart generated in the old style
        HelmChart sourceChart = new HelmChart();
        sourceChart.setHelmChartType(HelmChartType.CNF);
        sourceChart.setReleaseName("test_application-1");
        sourceVnfInstance.setHelmCharts(singletonList(sourceChart));

        String targetVnfdId = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
        PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);

        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);

        updatedVnfInstance.getHelmCharts().add(sourceChart);

        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getReleaseName() == null)
                .forEach(helmChart -> {
                    String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
                    helmChart.setReleaseName(newReleaseName);
                });

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(3)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-1", "test_application-2", "test_application-3");
    }

    @Test
    public void testUpdateInstanceEntityShouldCreateNewReleaseNameWhenUpgradingFromMultiToMultiHelmCharts() {
        VnfInstance sourceVnfInstance = new VnfInstance();

        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        // simulate instance that once was instantiated from single-chart package (old-style release name)
        // and then was upgraded to multi-chart package
        HelmChart sourceChart1 = new HelmChart();
        sourceChart1.setHelmChartType(HelmChartType.CNF);
        sourceChart1.setReleaseName("test_application");
        HelmChart sourceChart2 = new HelmChart();
        sourceChart2.setHelmChartType(HelmChartType.CNF);
        sourceChart2.setReleaseName("test_application-2");
        sourceVnfInstance.setHelmCharts(List.of(sourceChart1, sourceChart2));

        String targetVnfdId = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
        PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);

        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);
        updatedVnfInstance.setHelmCharts(sourceVnfInstance.getHelmCharts());

        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().forEach(helmChart -> {
            String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
            helmChart.setReleaseName(newReleaseName);
        });

        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-3", "test_application-4");
    }

    @Test
    public void testInstanceHelmChartUrlsHaveBeenMigrated() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a379");
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).hasSize(1);
        assertThat(charts).extracting("priority").containsOnly(1);
        assertThat(charts).extracting("helmChartUrl")
                .containsOnly("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        assertThat(charts).extracting("releaseName").containsOnly(vnfInstance.getVnfInstanceName());
    }

    @Test
    public void testInstanceMultipleHelmChartsHaveBeenMigrated() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h3def1ce-4cf4-477c-aab3-21c454e6a389");
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).hasSize(2);
        assertThat(charts).extracting("priority").containsOnly(1, 2);
        assertThat(charts).extracting("releaseName").containsOnly(vnfInstance.getVnfInstanceName() + "-1", vnfInstance.getVnfInstanceName() + "-2");
    }

    @Test
    public void testExtractOssTopologyWithAdditionalParams() {
        String vnfInstanceId = "10def1ce-4cf4-477c-aab3-21c454e6a389";
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        AddNodeToVnfInstanceByIdRequest overrideOssParams = new AddNodeToVnfInstanceByIdRequest("networkElementTypeTest",
                                                                                                "networkElementUsernameTest",
                                                                                                "networkElementPasswordTest",
                                                                                                "nodeIpAddressTest",
                                                                                                "22");

        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromParams(
                vnfInstance, overrideOssParams);

        JSONObject ossParam = new JSONObject(vnfInstance.getAddNodeOssTopology());
        assertThat(ossParam.getString("managedElementId")).isEqualTo((String) mergedMap.get("managedElementId"));
        assertThat(mergedMap.get("networkElementType")).isEqualTo("networkElementTypeTest");
        assertThat(mergedMap.get("networkElementUsername")).isEqualTo("networkElementUsernameTest");
        assertThat(mergedMap.get("networkElementPassword")).isEqualTo("networkElementPasswordTest");
        assertThat(mergedMap.get("nodeIpAddress")).isEqualTo("nodeIpAddressTest");
        assertThat(mergedMap.get("netConfPort")).isEqualTo("22");
        assertThat(mergedMap.get("vnfInstanceId")).isEqualTo(vnfInstanceId);
    }

    @Test
    public void testExtractOssTopologyWithoutAdditionalParams() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("10def1ce-4cf4-477c-aab3-21c454e6a389");
        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromParams(
                vnfInstance, null);
        assertThat(mergedMap.get("snmpSecurityLevel")).isEqualTo("snmpSecurityLvl1");
        assertThat(mergedMap.get("vnfInstanceId")).isEqualTo("10def1ce-4cf4-477c-aab3-21c454e6a389");
    }

    @Test
    public void testExtractOddTopologyWithTenantName() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("c3def1ce-4cf4-477c-ahb3-61c454e6a344");
        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromParams(
                vnfInstance, null);
        assertThat(mergedMap.get("tenant")).isEqualTo("ecm");
    }

    @Test
    public void testExtractOddTopologyWithoutTenantName() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("10def1ce-4cf4-477c-aab3-21c454e6a389");
        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromParams(
                vnfInstance, null);
        assertThat(mergedMap.get("tenant")).isEqualTo(null);
    }

    @Test
    public void testExtractOddTopologyWithEmptyMetadata() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("c4def1ce-4cf4-477c-ahb3-61c454e6a344");
        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromParams(
                vnfInstance, null);
        assertThat(mergedMap.get("tenant")).isEqualTo(null);
    }

    @Test
    public void testExtractOssTopologyWithFile() throws URISyntaxException {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("11def1ce-4cf4-477c-aab3-21c454e6a389");
        Path valuesFile = TestUtils.getResource("oss_topology.yaml");
        Map<String, Object> mergedMap = instanceService.extractOssTopologyFromValuesYamlMap(
                vnfInstance, convertYamlFileIntoMap(valuesFile));
        assertThat(mergedMap.get("snmpSecurityLevel")).isEqualTo("snmpSecurityLvl2");
        assertThat(mergedMap.get("vnfInstanceId")).isEqualTo("11def1ce-4cf4-477c-aab3-21c454e6a389");
    }

    @Test
    public void testGetOverrideGlobalRegistryTrue() {
        PackageResponse packageResponse = getPackageResponseWithUserDefinedData(SKIP_IMAGE_UPLOAD, "true");
        boolean skipImageUpload = InstanceService.getSkipImageUploadValue(packageResponse);
        assertThat(skipImageUpload).isTrue();
    }

    @Test
    public void testGetOverrideGlobalRegistryTrueIgnoreCase() {
        PackageResponse packageResponse = getPackageResponseWithUserDefinedData(SKIP_IMAGE_UPLOAD, true);
        boolean skipImageUpload = InstanceService.getSkipImageUploadValue(packageResponse);
        assertThat(skipImageUpload).isTrue();
    }

    @Test
    public void testGetOverrideGlobalRegistryFalse() {
        PackageResponse packageResponse = getPackageResponseWithUserDefinedData(SKIP_IMAGE_UPLOAD, "false");
        boolean skipImageUpload = InstanceService.getSkipImageUploadValue(packageResponse);
        assertThat(skipImageUpload).isFalse();
    }

    @Test
    public void testGetOverrideGlobalRegistryNotFound() {
        PackageResponse packageResponse = getPackageResponseWithUserDefinedData("notFound", "false");
        boolean skipImageUpload = InstanceService.getSkipImageUploadValue(packageResponse);
        assertThat(skipImageUpload).isFalse();
    }

    private PackageResponse getPackageResponseWithUserDefinedData(final String key, final Object value) {
        PackageResponse packageResponse = new PackageResponse();
        Map<String, Object> userDefinedData = new HashMap<>();
        userDefinedData.put(key, value);
        packageResponse.setUserDefinedData(userDefinedData);
        return packageResponse;
    }

    @Test
    public void testGetOverrideGlobalRegistryUserDefinedDataNull() {
        PackageResponse packageResponse = new PackageResponse();
        boolean skipImageUpload = InstanceService.getSkipImageUploadValue(packageResponse);
        assertThat(skipImageUpload).isFalse();
    }

    @Test
    public void testVnfHelmChartsUpdate() {
        String chartUrl =
                "https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz";
        VnfInstance instance = verifyInstance("ff1ce-4cf4-477c-aab3-21c454e6a379", chartUrl);
        String helmChartUrl = saveNewHelmChart(instance);
        vnfInstanceRepository.save(instance);
        VnfInstance updatedInstance = databaseInteractionService.getVnfInstance("ff1ce-4cf4-477c-aab3-21c454e6a379");
        assertThat(updatedInstance.getHelmCharts().size()).isEqualTo(1);
        int count = 0;
        for (HelmChart chart : updatedInstance.getHelmCharts()) {
            if (chart.getHelmChartUrl().equals(helmChartUrl)) {
                count++;
            }
        }
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void shouldNotUpdateUsageStateWithNfvoEnabled() {
        setUpNfvoConfig(true);
        ReflectionTestUtils.setField(instanceService, SMALL_STACK_APPLICATION_PARAM_NAME, "true");

        instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(DUMMY_PACKAGE_ID, DUMMY_INSTANCE_ID, true);

        verify(restTemplate, never()).put(eq(getUsageStateApiUri(DUMMY_PACKAGE_ID)), any());
    }

    @Test
    public void shouldUpdateUsageStateWithNfvoDisabled() {
        ReflectionTestUtils.setField(instanceService, SMALL_STACK_APPLICATION_PARAM_NAME, "true");

        instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(DUMMY_PACKAGE_ID, STUB_UPDATE_USAGE_STATE_VNF_ID, true);

        verify(packageService, times(1)).updateUsageState(eq(DUMMY_PACKAGE_ID), eq(STUB_UPDATE_USAGE_STATE_VNF_ID), eq(true));
    }

    @Test
    public void shouldUpdateUsageStateForFailedUpgrade() {
        final var upgradedInstance = new VnfInstance();
        upgradedInstance.setVnfPackageId("target-vnfd-id");

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setVnfPackageId("source-vnfd-id");
        instance.setTempInstance(convertObjToJsonString(upgradedInstance));

        instanceService.updateAssociationBetweenPackageAndVnfInstanceForFailedUpgrade(instance);
        verify(packageService, times(1)).updateUsageState(eq("target-vnfd-id"), eq("instance-id"), eq(false));
    }

    @Test
    public void shouldUpdateUsageStateForFailedUpgradeToleratingException() {
        final var upgradedInstance = new VnfInstance();
        upgradedInstance.setVnfPackageId("target-vnfd-id");

        final var instance = new VnfInstance();
        instance.setVnfPackageId("source-vnfd-id");
        instance.setTempInstance(convertObjToJsonString(upgradedInstance));

        assertThatNoException().isThrownBy(() -> instanceService.updateAssociationBetweenPackageAndVnfInstanceForFailedUpgrade(instance));
    }

    @Test
    public void testMergeMapsWithValuesFileEmpty() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile("{}");
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(3);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingFile());
    }

    @Test
    public void testMergeMapsWithValuesFile() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile(" {\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}},"
                                               + "\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2.rontgen010.seli.gic.ericsson"
                                               + ".se\"}},\"eric-pm-server\":{\"server\":{\"persistentVolume\":{\"storageClass\":\"erikube-rbd\"},"
                                               + "\"ingress\":{\"enabled\":false}}},\"pm-testapp\":{\"ingress\":{\"domain\":\"rontgen010.seli.gic"
                                               + ".ericsson.se\"}},\"tags\":{\"all\":false,\"pm\":\"<xml version=1.0 encoding=UTF-8?><Nodes>  "
                                               + "<Node>    <nodeFdn>VPP00001<\\/nodeFdn>    <certType>OAM<\\/certType>    "
                                               + "<enrollmentMode>CMPv2_INITIAL<\\/enrollmentMode>  <\\/Node><\\/Nodes>\"}}");
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(6);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedMergedOutput());
    }

    @Test
    public void testMergeMapsWithValuesFileNull() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        addParams(additionalParams);
        instance.setCombinedAdditionalParams(mapper.writeValueAsString(additionalParams));
        instance.setCombinedValuesFile(null);
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(3);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingFile());
    }

    @Test
    public void testMergeMapsWithAdditionalValuesAsNull() {
        VnfInstance instance = new VnfInstance();
        instance.setCombinedAdditionalParams(null);
        instance.setCombinedValuesFile(" {\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}},"
                                               + "\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2.rontgen010.seli.gic.ericsson"
                                               + ".se\"}},\"eric-pm-server\":{\"server\":{\"persistentVolume\":{\"storageClass\":\"erikube-rbd\"},"
                                               + "\"ingress\":{\"enabled\":false}}},\"pm-testapp\":{\"ingress\":{\"domain\":\"rontgen010.seli.gic"
                                               + ".ericsson.se\"}},\"tags\":{\"all\":false,\"pm\":true}}");
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(5);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingAdditionalParams());
    }

    @Test
    public void testMergeMapsWithAdditionalValuesAsEmpty() {
        VnfInstance instance = new VnfInstance();
        instance.setCombinedAdditionalParams("{}");
        instance.setCombinedValuesFile(" {\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}},"
                                               + "\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2.rontgen010.seli.gic.ericsson"
                                               + ".se\"}},\"eric-pm-server\":{\"server\":{\"persistentVolume\":{\"storageClass\":\"erikube-rbd\"},"
                                               + "\"ingress\":{\"enabled\":false}}},\"pm-testapp\":{\"ingress\":{\"domain\":\"rontgen010.seli.gic"
                                               + ".ericsson.se\"}},\"tags\":{\"all\":false,\"pm\":true}}");
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(5);
        Assertions.assertThat(stringObjectMap.toString()).contains(getExpectedOutputMissingAdditionalParams());
    }

    @Test
    public void testMergeMapsWithAdditionalValuesWithFileAtrributes() {
        VnfInstance instance = new VnfInstance();
        String inputAttributes = parseJsonFile("additional-attributes-to-merge-with-file-types.json");
        String expectedAttributes = parseJsonFile("combined-values-file-with-file-attributes.txt");
        instance.setCombinedValuesFile(inputAttributes);
        Path path = valuesService.getCombinedAdditionalValuesFile(instance);
        Map<String, Object> stringObjectMap = convertYamlFileIntoJson(path).toMap();
        Assertions.assertThat(stringObjectMap.size()).isEqualTo(4);
        Assertions.assertThat(stringObjectMap.toString()).isEqualToIgnoringNewLines(expectedAttributes);
    }

    @Test
    public void testGetCommentedScaleInfo() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("values-4cf4-477c-aab3-21c454e6a380");
        String string = getCommentedScaleInfo(instance.getScaleInfoEntity());
        assertThat(string.contains("# Aspects and Current Scale level\n"
                                           + "# Aspect1: 3\n"
                                           + "# Aspect2: 3")).isTrue();
    }

    private static void addParams(final Map<String, Object> additionalParams) {
        additionalParams.put("pm-testapp.ingress.domain", "server");
        additionalParams.put("influxdb.ext.apiAccessHostname", true);
        additionalParams.put("test-with-quotes", "<config xmlns=\"http://tail-f.com/ns/config/1.0\">");
    }

    private String getExpectedMergedOutput() {
        return "{test-with-quotes=<config xmlns=\"http://tail-f.com/ns/config/1.0\">, eric-pm-server={server={ingress={enabled=false}, "
                + "persistentVolume={storageClass=erikube-rbd}}}, "
                + "eric-adp-gs-testapp={ingress={enabled=false}}, influxdb={ext={apiAccessHostname=true}}, "
                + "pm-testapp={ingress={domain=server}}, tags={all=false, pm=<xml version=1.0 encoding=UTF-8?><Nodes>  <Node>    "
                + "<nodeFdn>VPP00001</nodeFdn>    <certType>OAM</certType>    <enrollmentMode>CMPv2_INITIAL</enrollmentMode>  </Node></Nodes>}}";
    }

    private String getExpectedOutputMissingFile() {
        return "{test-with-quotes=<config xmlns=\"http://tail-f.com/ns/config/1.0\">, influxdb={ext={apiAccessHostname=true}}, "
                + "pm-testapp={ingress={domain=server}}}";
    }

    private String getExpectedOutputMissingAdditionalParams() {
        return "{eric-pm-server={server={ingress={enabled=false}, persistentVolume={storageClass=erikube-rbd}}}, "
                + "eric-adp-gs-testapp={ingress={enabled=false}}, influxdb={ext={apiAccessHostname=influxdb-service2"
                + ".rontgen010.seli.gic.ericsson.se}}, pm-testapp={ingress={domain=rontgen010.seli.gic.ericsson.se}},"
                + " tags={all=false, pm=true}}";
    }

    private String saveNewHelmChart(final VnfInstance instance) {
        HelmChart helmChart = new HelmChart();
        String helmChartUrl = "https://testing/testing1.tgz";
        helmChart.setHelmChartUrl(helmChartUrl);
        helmChart.setPriority(1);
        helmChart.setVnfInstance(instance);
        helmChart.setReleaseName(instance.getVnfInstanceName());
        List<HelmChart> charts = new ArrayList<>();
        charts.add(helmChart);
        instance.setHelmCharts(charts);
        return helmChartUrl;
    }

    private VnfInstance verifyInstance(final String vnfInstanceId, String chart) {
        VnfInstance instance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThat(instance.getHelmCharts().size()).isEqualTo(1);
        assertThat(instance.getHelmCharts().get(0).getHelmChartUrl()).isEqualTo(chart);
        return instance;
    }

    private CreateVnfRequest getCreateVnfRequest(final String vnfdId) {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest();
        createVnfRequest.setVnfdId(vnfdId);
        createVnfRequest.setVnfInstanceName("test_application");
        createVnfRequest.setVnfInstanceDescription("dummy application to be created");
        return createVnfRequest;
    }

    @Test
    public void shouldReturnDowngradePackagesInfo() {
        returnAndCheckDowngradeInfo();
    }

    @Test
    public void shouldReturnDowngradePackagesInfoWithNoRollbackPatternInVnfd() {
        stubVnfdResponse("instance-service/valid-vnfd-with-rollback-policies-no-pattern.yaml");
        returnAndCheckDowngradeInfo();
    }

    @Test
    public void shouldReturnDowngradePackagesInfoWithDefaultRollbackPatternInVnfd() {
        stubVnfdResponse("instance-service/valid-vnfd-with-rollback-policies-default-pattern.yaml");
        returnAndCheckDowngradeInfo();
    }

    @Test
    public void shouldThrowDowngradeNotSupportedExceptionIfOnlySelfUpgradeOperationOccurred() {
        String vnfId = REPO_DOWNGRADE_NOT_SUPPORTED_VNFD_ID;

        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(vnfId))
                .isInstanceOf(DowngradeNotSupportedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, vnfId));
    }

    @Test
    public void shouldThrowDowngradeNotSupportedExceptionIfNoDowngradePolicyDefinedInVnfd() throws JsonProcessingException {
        String vnfId = REPO_DOWNGRADE_NOT_SUPPORTED_NO_POLICY_VNF_ID;
        stubPackageResponse("instance-service/package-response-for-downgrade.json");

        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(vnfId))
                .isInstanceOf(DowngradeNotSupportedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, vnfId));
    }

    @Test
    public void shouldThrowDowngradeNotSupportedExceptionIfVnfdContainsInvalidPattern() {
        String vnfId = REPO_DOWNGRADE_NOT_SUPPORTED_NO_POLICY_VNF_ID;
        stubVnfdResponse("instance-service/invalid-vnfd-with-rollback-policies-invalid-pattern.yaml");

        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(vnfId))
                .isInstanceOf(DowngradeNotSupportedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, vnfId));
    }

    @Test
    public void shouldThrowDowngradeInfoInstanceIdNotPresentExceptionWhenNoSourceInstance() {
        String notExistentInstanceId = "no_such_instance_id";

        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(notExistentInstanceId))
                .isInstanceOf(DowngradeInfoInstanceIdNotPresentException.class)
                .hasMessage(notExistentInstanceId + VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE);
    }

    @Test
    public void shouldThrowDowngradeNotSupportedExceptionWhenNoVnfdChangingOperations() {
        String noVnfdChangingOperationsInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a379";

        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(noVnfdChangingOperationsInstanceId))
                .isInstanceOf(DowngradeNotSupportedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, noVnfdChangingOperationsInstanceId));
    }

    @Test
    public void shouldThrowDowngradePackageDeletedExceptionWhenNoTargetPackage() {
        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(REPO_DOWNGRADE_NO_PACKAGE_VNF_ID))
                .isInstanceOf(DowngradePackageDeletedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_AS_DOWNGRADE_PACKAGE_IS_ERROR_MESSAGE,
                                          REPO_DOWNGRADE_NO_PACKAGE_VNF_ID));
    }

    @Test
    public void shouldThrowDowngradePackageDeletedExceptionWhenNoTargetPackageIfNfvoConfigEnabled() {
        setUpNfvoConfig(true);
        assertThatThrownBy(() -> instanceService.getDowngradePackagesInfo(REPO_DOWNGRADE_NO_PACKAGE_VNF_ID))
                .isInstanceOf(DowngradePackageDeletedException.class)
                .hasMessage(String.format(DOWNGRADE_NOT_SUPPORTED_AS_DOWNGRADE_PACKAGE_IS_ERROR_MESSAGE,
                                          REPO_DOWNGRADE_NO_PACKAGE_VNF_ID));
    }

    private String getUsageStateApiUri(String packageId) {
        return new StringBuilder(onboardingHost)
                .append(ONBOARDING_PATH)
                .append(URI_PATH_SEPARATOR)
                .append(packageId)
                .append(URI_PATH_SEPARATOR)
                .append(USAGE_STATE_API)
                .toString();
    }

    private void setUpNfvoConfig(final boolean nfvoConfigEnabled) {
        nfvoConfig.setEnabled(nfvoConfigEnabled);
        ReflectionTestUtils.setField(instanceService, NFVO_CONFIG_NAME, nfvoConfig);
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, "nfvoToken", "token");
    }

    private int getConfigInstancesCount(final VnfInstance instance) {
        return (int) clusterConfigInstanceRepository.findAll()
                .stream()
                .filter(e -> e.getInstanceId().equals(instance.getVnfInstanceId()))
                .count();
    }

    @Test
    public void createSitebasicFileFromInstance() throws IOException {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("kxnam34q-7065-49b1-831c-d687130c6123");
        Path sitebasicFile = getSitebasicFile(vnfInstance);
        assertThat(sitebasicFile).exists();
        assertThat(sitebasicFile.getFileName().toString()).startsWith("sitebasic");
        assertThat(sitebasicFile.getFileName().toString()).endsWith(".xml");
        assertThat(Files.readAllLines(sitebasicFile)).anyMatch(s -> s.contains("<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>"));
    }

    @Test
    public void createSitebasicFileFromInstanceFailedWithNoSitebasicFile() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h4def1ce-4cf4-477c-aab3-21c454e6666");
        assertThatThrownBy(() -> getSitebasicFile(vnfInstance))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("Sitebasic information is not available in the vnf instance.");
    }

    @Test
    public void setInstanceWithValidSitebasicFile() throws IOException, URISyntaxException {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        String sitebasic = readDataFromFile("node.xml");
        additionalParams.put(SITEBASIC_XML, sitebasic);
        assertThat(instance.getSitebasicFile()).isNull();
        InstanceService.setInstanceWithSitebasicFile(instance, additionalParams);
        assertThat(instance.getSitebasicFile())
                .isNotNull()
                .contains("<nodeFdn>VPP00001</nodeFdn>")
                .contains("<enrollmentMode>CMPv2_INITIAL</enrollmentMode>")
                .contains("</Nodes>");
    }

    @Test
    public void setInstanceWithInvalidSitebasicFile() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(SITEBASIC_XML, "<Nodes>\n<Node>\n<nodeFdn>VPP00001/nodeFdn>");
        assertThat(instance.getSitebasicFile()).isNull();
        assertThatThrownBy(() -> InstanceService.setInstanceWithSitebasicFile(instance, additionalParams))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("Invalid XML format:: Error when converting sitebasic.xml file:");
    }

    @Test
    public void setInstanceWithSitebasicFileAbsentFromAdditionalParams() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.managedElementId", "VPP00001");
        assertThat(instance.getSitebasicFile()).isNull();
        InstanceService.setInstanceWithSitebasicFile(instance, additionalParams);
        assertThat(instance.getSitebasicFile())
                .isNotNull()
                .contains("<nodeFdn>VPP00001</nodeFdn>")
                .contains("<enrollmentMode>CMPv2_INITIAL</enrollmentMode>")
                .contains("<certType>OAM</certType>");
    }

    @Test
    public void setInstanceWithSitebasicFileAbsentFromAdditionalParamsWithOtpValidityPeriodAndEntityProfileName() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.managedElementId", "VPP00001");
        additionalParams.put("otpValidityPeriodInMinutes", 100);
        additionalParams.put("entityProfileName", "testName");
        assertThat(instance.getSitebasicFile()).isNull();
        InstanceService.setInstanceWithSitebasicFile(instance, additionalParams);
        assertThat(instance.getSitebasicFile())
                .isNotNull()
                .contains("<nodeFdn>VPP00001</nodeFdn>")
                .contains("<certType>OAM</certType>")
                .contains("<entityProfileName>testName</entityProfileName>")
                .contains("<enrollmentMode>CMPv2_INITIAL</enrollmentMode>")
                .contains("<otpValidityPeriodInMinutes>100</otpValidityPeriodInMinutes>");
    }

    @Test
    public void setInstanceWithSitebasicFileAbsentFromAdditionalParamsWithEmptyManagedElementId() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.managedElementId", "");
        assertThat(instance.getSitebasicFile()).isNull();
        InstanceService.setInstanceWithSitebasicFile(instance, additionalParams);
        assertThat(instance.getSitebasicFile())
                .isNotNull()
                .contains(String.format("<nodeFdn>%s</nodeFdn>", instance.getVnfInstanceName()))
                .contains("<enrollmentMode>CMPv2_INITIAL</enrollmentMode>")
                .contains("<certType>OAM</certType>");
    }

    @Test
    public void setInstanceWithSitebasicFileWithErrorDuringCreationFromOssParams() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("fe7eehf7-7065-49b1-831c-d687130c6123");
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("ossTopology.managedElementId", "VPP00001");
        try (MockedStatic<OssTopologyUtility> utility = Mockito.mockStatic(OssTopologyUtility.class)) {
            utility.when(() -> OssTopologyUtility.createSitebasicFileFromOSSParams(Mockito.any(), Mockito.anyMap()))
                    .thenReturn("");
            InstanceService.setInstanceWithSitebasicFile(instance, additionalParams);
            assertThat(instance.getSitebasicFile()).isNull();
        }
    }

    @Test
    public void testDeleteVnfIdentifierDoNotGoThroughWithFailedPackageUpdateUsageState() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("ggdef1ce-4cf4-477c-aab3-34erf32df");

        int clusterConfigInstancesBeforeDeletionCount = getConfigInstancesCount(instance);

        assertThatThrownBy(() -> instanceService.deleteInstanceEntity(instance.getVnfInstanceId(), false))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining(String.format("Unable to update usage state for package %s; Failed due to unavailable Onboarding Service",
                                                    instance.getVnfPackageId()));

        assertThat(vnfInstanceRepository.existsById("ggdef1ce-4cf4-477c-aab3-34erf32df")).isTrue();

        int clusterConfigInstancesAfterDeletionCount = getConfigInstancesCount(instance);

        assertThat(clusterConfigInstancesBeforeDeletionCount).isEqualTo(clusterConfigInstancesAfterDeletionCount);
    }

    @Test
    public void testUpdatingPackageStateWithPackageNotFoundWithoutException() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("a1def1ce-4cf4-477c-aab3-21cb04e6a379");

        assertThatNoException()
                .isThrownBy(() -> packageService.updateUsageState(instance.getVnfPackageId(), instance.getVnfInstanceId(), false));
    }

    @Test
    public void testUpdatingPackageStateWithServerError() {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId("ggdef1ce-4cf4-477c-aab3-5sa9ff3gf");

        assertThatThrownBy(() -> packageService.updateUsageState(instance.getVnfPackageId(), instance.getVnfInstanceId(), false))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessageContaining(String.format("Update usage state for package %s and VNF Instance %s to isUsageState false failed",
                                                    instance.getVnfPackageId(), instance.getVnfInstanceId()));
    }

    @Test
    public void testDeleteInstanceDeletesInstanceNamespaceDetails() {
        String vnfInstanceId = "97ba1047-01b8-4536-bb95-9f8d7e3797ab";
        VnfInstance vnfInstanceBeforeDeletion = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        Optional<VnfInstanceNamespaceDetails> vnfInstanceNamespaceDetailsBeforeDeletion = databaseInteractionService
                .getNamespaceDetails(vnfInstanceId);

        assertThat(vnfInstanceBeforeDeletion).isNotNull();
        assertThat(vnfInstanceNamespaceDetailsBeforeDeletion.isPresent()).isTrue();

        instanceService.deleteInstanceEntity(vnfInstanceId, true);

        VnfInstance vnfInstanceAfterDeletion = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        Optional<VnfInstanceNamespaceDetails> vnfInstanceNamespaceDetailsAfterDeletion = databaseInteractionService
                .getNamespaceDetails(vnfInstanceId);

        assertThat(vnfInstanceAfterDeletion).isNull();
        assertThat(vnfInstanceNamespaceDetailsAfterDeletion.isPresent()).isFalse();
    }

    @Test
    public void helmChartsWithoutIsChartEnabledHaveBeenMigrated() {
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId("h3def1ce-4cf4-477c-aab3-21c454e6a389");
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).hasSize(2);
        assertThat(charts).extracting("isChartEnabled").containsOnly(true);
    }

    @Test
    public void helmChartHistoryRecordWithoutIsChartEnabledHaveBeenMigrated() {
        HelmChartHistoryRecord helmChartHistoryRecord =
                helmChartHistoryRepository.findById(UUID.fromString("62153344-ea3e-43cd-8ccb-13b6b09cf97b")).get();
        assertThat(helmChartHistoryRecord.isChartEnabled()).isTrue();
    }

    @Test
    public void testCalculateChartsPriorityFromLcmInterfacesInstantiateNotDefined() throws URISyntaxException, JsonProcessingException {
        String parseJsonFile = parseJsonFile("priority/expected_priority_instantiate_not_defined.json");
        List<ExpectedPriority> expectedPriorities = new ObjectMapper().readValue(parseJsonFile, new TypeReference<>() {
        });
        List<HelmChart> helmCharts = prepareHelmCHarts();
        setDefaultPriority(helmCharts);
        PackageResponse packageResponse = preparePackageResponse("priority/calculate-charts-priority-vnfd-instantiate-not-defined.yaml");

        List<HelmChart> prioritizedHelmCharts = InstanceService.getPrioritizedHelmCharts(helmCharts, packageResponse, new VnfInstance());

        for (ExpectedPriority expectedPriority : expectedPriorities) {
            HelmChart helmChart = prioritizedHelmCharts.stream().filter(i -> i.getHelmChartArtifactKey().equals(expectedPriority.getHelmChartName()))
                    .findFirst().orElseThrow(() -> new InternalRuntimeException("Helm Chart not found"));
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            expectedPriority
                    .getPriorities()
                    .forEach(priority -> assertThat(priority.getPriority()).isEqualTo(operationChartsPriority.get(priority.lcmType)));
        }
    }

    @Test
    public void testCalculateChartsPriorityWithoutNodeTemplate() throws URISyntaxException, JsonProcessingException {
        String parseJsonFile = parseJsonFile("priority/expected_priority_without_node_template.json");
        List<ExpectedPriority> expectedPriorities = new ObjectMapper().readValue(parseJsonFile, new TypeReference<>() {
        });
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(createCnfHelmChart(1));
        setDefaultPriority(helmCharts);
        PackageResponse packageResponse = preparePackageResponse("priority/calculate-charts-priority-vnfd-without-node-template.yaml");

        List<HelmChart> prioritizedHelmCharts = InstanceService.getPrioritizedHelmCharts(helmCharts, packageResponse, new VnfInstance());

        for (ExpectedPriority expectedPriority : expectedPriorities) {
            HelmChart helmChart = prioritizedHelmCharts.stream().filter(i -> i.getHelmChartArtifactKey().equals(expectedPriority.getHelmChartName()))
                    .findFirst().orElseThrow(() -> new InternalRuntimeException("Helm Chart not found"));
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            expectedPriority
                    .getPriorities()
                    .forEach(priority -> assertThat(priority.getPriority()).isEqualTo(operationChartsPriority.get(priority.lcmType)));
        }
    }

    @Test
    public void testCalculateChartsPriorityFromLcmInterfacesAllNotDefined() throws URISyntaxException, JsonProcessingException {
        String parseJsonFile = parseJsonFile("priority/expected_priority_all_not_defined.json");
        List<ExpectedPriority> expectedPriorities = new ObjectMapper().readValue(parseJsonFile, new TypeReference<>() {
        });
        List<HelmChart> helmCharts = prepareHelmCHarts();
        setDefaultPriority(helmCharts);
        PackageResponse packageResponse = preparePackageResponse("priority/calculate-charts-priority-vnfd-all-not-defined.yaml");

        List<HelmChart> prioritizedHelmCharts = InstanceService.getPrioritizedHelmCharts(helmCharts, packageResponse, new VnfInstance());

        for (ExpectedPriority expectedPriority : expectedPriorities) {
            HelmChart helmChart = prioritizedHelmCharts.stream().filter(i -> i.getHelmChartArtifactKey().equals(expectedPriority.getHelmChartName()))
                    .findFirst().orElseThrow(() -> new InternalRuntimeException("Helm Chart not found"));
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            expectedPriority
                    .getPriorities()
                    .forEach(priority -> assertThat(priority.getPriority()).isEqualTo(operationChartsPriority.get(priority.lcmType)));
        }
    }

    @Test
    public void testCalculateChartsPriorityFromLcmInterfacesCCVPNotDefined() throws URISyntaxException, JsonProcessingException {
        String parseJsonFile = parseJsonFile("priority/expected_priority_ccvp_not_defined.json");
        List<ExpectedPriority> expectedPriorities = new ObjectMapper().readValue(parseJsonFile, new TypeReference<>() {
        });
        List<HelmChart> helmCharts = prepareHelmCHarts();
        setDefaultPriority(helmCharts);
        PackageResponse packageResponse = preparePackageResponse("priority/calculate-charts-priority-vnfd-ccvp-not-defined.yaml");

        List<HelmChart> prioritizedHelmCharts = InstanceService.getPrioritizedHelmCharts(helmCharts, packageResponse, new VnfInstance());

        for (ExpectedPriority expectedPriority : expectedPriorities) {
            HelmChart helmChart = prioritizedHelmCharts.stream().filter(i -> i.getHelmChartArtifactKey().equals(expectedPriority.getHelmChartName()))
                    .findFirst().orElseThrow(() -> new InternalRuntimeException("Helm Chart not found"));
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            expectedPriority
                    .getPriorities()
                    .forEach(priority -> assertThat(priority.getPriority()).isEqualTo(operationChartsPriority.get(priority.lcmType)));
        }
    }

    @Test
    public void testCalculateChartsPriorityFromLcmInterfacesAllDefined() throws URISyntaxException, JsonProcessingException {
        String parseJsonFile = parseJsonFile("priority/expected_priority_all_defined.json");
        List<ExpectedPriority> expectedPriorities = new ObjectMapper().readValue(parseJsonFile, new TypeReference<>() {
        });
        List<HelmChart> helmCharts = prepareHelmCHarts();
        setDefaultPriority(helmCharts);
        PackageResponse packageResponse = preparePackageResponse("priority/calculate-charts-priority-vnfd-all-defined.yaml");

        List<HelmChart> prioritizedHelmCharts = InstanceService.getPrioritizedHelmCharts(helmCharts, packageResponse, new VnfInstance());

        for (ExpectedPriority expectedPriority : expectedPriorities) {
            HelmChart helmChart = prioritizedHelmCharts.stream().filter(i -> i.getHelmChartArtifactKey().equals(expectedPriority.getHelmChartName()))
                    .findFirst().orElseThrow(() -> new InternalRuntimeException("Helm Chart not found"));
            Map<LifecycleOperationType, Integer> operationChartsPriority = helmChart.getOperationChartsPriority();
            expectedPriority
                    .getPriorities()
                    .forEach(priority -> assertThat(priority.getPriority()).isEqualTo(operationChartsPriority.get(priority.lcmType)));
        }
    }

    private PackageResponse preparePackageResponse(String fileName) throws URISyntaxException {
        PackageResponse packageResponse = new PackageResponse();
        String descriptor = convertYamlFileIntoJson(getResource(fileName)).toString();
        packageResponse.setDescriptorModel(descriptor);

        return packageResponse;
    }

    private void stubPackageResponse(String fileName) throws JsonProcessingException {
        Mockito.doReturn(mapper.readValue(readDataFromFile(getClass(), fileName), PackageResponse.class))
                .when(packageService).getPackageInfo(any());
    }

    private void stubVnfdResponse(String fileName) {
        Mockito.doReturn(YamlUtility.convertYamlStringIntoJson(readDataFromFile(getClass(), fileName)))
                .when(packageService).getVnfd(any());
    }

    private void returnAndCheckDowngradeInfo() {
        DowngradePackageInfo expectedSourceDowngradePackageInfo = new DowngradePackageInfo();
        expectedSourceDowngradePackageInfo.setPackageId(REPO_DOWNGRADE_SUPPORTED_VNF_PACKAGE_ID);
        expectedSourceDowngradePackageInfo.setPackageVersion(STUB_PACKAGE_VERSION);
        expectedSourceDowngradePackageInfo.setVnfdId(STUB_DOWNGRADE_SUPPORTED_SOURCE_VNFD_ID);

        DowngradePackageInfo expectedTargetDowngradePackageInfo = new DowngradePackageInfo();
        expectedTargetDowngradePackageInfo.setPackageId(STUB_PACKAGE_ID_BY_FILTER);
        expectedTargetDowngradePackageInfo.setPackageVersion(STUB_PACKAGE_VERSION);
        expectedTargetDowngradePackageInfo.setVnfdId(STUB_DOWNGRADE_SUPPORTED_TARGET_VNFD_ID);

        DowngradeInfo expectedDowngradeInfo = new DowngradeInfo();
        expectedDowngradeInfo.setSourceDowngradePackageInfo(expectedSourceDowngradePackageInfo);
        expectedDowngradeInfo.setTargetDowngradePackageInfo(expectedTargetDowngradePackageInfo);

        DowngradeInfo actualDowngradeInfo = instanceService.getDowngradePackagesInfo(REPO_DOWNGRADE_SUPPORTED_VNF_ID);

        assertEquals(expectedDowngradeInfo.getSourceDowngradePackageInfo(),
                     actualDowngradeInfo.getSourceDowngradePackageInfo());
        assertEquals(expectedDowngradeInfo.getTargetDowngradePackageInfo(),
                     actualDowngradeInfo.getTargetDowngradePackageInfo());
    }

    @Data
    private static class ExpectedPriority {
        private String helmChartName;
        private List<ExpectedOperationPriority> priorities;
    }

    @Data
    private static class ExpectedOperationPriority {
        private Integer priority;
        private LifecycleOperationType lcmType;
    }
}
