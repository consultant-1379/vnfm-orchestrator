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

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        InstanceService.class,
        ObjectMapper.class })
@MockBean({
        ValuesFileService.class,
        RestTemplate.class,
        VnfInstanceRepository.class,
        VnfInstanceMapper.class,
        HelmChartHistoryRepository.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        NfvoConfig.class,
        ReplicaDetailsService.class,
        ReplicaCountCalculationService.class,
        VnfInstanceQuery.class,
        PackageService.class,
        VnfdHelper.class,
        AdditionalAttributesHelper.class,
        LifecycleOperationHelper.class,
        LifeCycleManagementHelper.class,
        LcmOpSearchService.class,
        ChangePackageOperationDetailsService.class,
        ExtensionsService.class,
        ClusterConfigInstanceRepository.class,
        DatabaseInteractionService.class,
        ChangeVnfPackageService.class,
        NotificationService.class })
@TestPropertySource(properties = "orchestrator.suffixFirstCnfReleaseSchema=false")
public class InstanceServiceReleaseWithoutSuffixTest {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void testCreateInstanceEntityShouldGenerateReleaseNameWithoutSuffix() {
        // given
        PackageResponse packageInfo = readPackageInfo("instance-service/single-chart-descriptor-model.json");

        // when
        VnfInstance vnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest("vnfd-id"), Optional.empty());

        // then
        assertThat(vnfInstance.getHelmCharts())
                .hasSize(1)
                .extracting(HelmChart::getReleaseName)
                .containsOnly("test_application");
    }

    @Test
    public void testCreateInstanceEntityShouldGenerateFirstReleaseNameWithoutSuffixWhenMultipleHelmCharts() {
        // given
        PackageResponse packageInfo = readPackageInfo("instance-service/multi-chart-descriptor-model.json");

        // when
        VnfInstance vnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest("vnfd-id"), Optional.empty());

        // then
        assertThat(vnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChart::getReleaseName)
                .containsOnly("test_application", "test_application-2");
    }

    @Test
    public void testUpdateInstanceEntityShouldPreserveReleaseNameWithSuffixWhenUpgradingFromSingleToMultiHelmCharts() {
        // given
        VnfInstance sourceVnfInstance = new VnfInstance();
        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        HelmChart sourceChart = new HelmChart();
        sourceChart.setHelmChartType(HelmChartType.CNF);
        sourceChart.setReleaseName("test_application-1");
        sourceVnfInstance.setHelmCharts(List.of(sourceChart));

        PackageResponse targetPackageInfo = readPackageInfo("instance-service/multi-chart-descriptor-model.json");

        // when
        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());
        // then
        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);

        // when
        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().forEach(helmChart -> {
            String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
            helmChart.setReleaseName(newReleaseName);
        });

        // then
        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-1", "test_application-2");
    }

    @Test
    public void testUpdateInstanceEntityShouldPreserveReleaseNameWithSuffixWhenUpgradingFromMultiToMultiHelmCharts() {
        // given
        VnfInstance sourceVnfInstance = new VnfInstance();
        sourceVnfInstance.setVnfInstanceName("test_application");
        sourceVnfInstance.setTerminatedHelmCharts(Collections.emptyList());

        HelmChart sourceChart1 = new HelmChart();
        sourceChart1.setHelmChartType(HelmChartType.CNF);
        sourceChart1.setReleaseName("test_application-1");
        HelmChart sourceChart2 = new HelmChart();
        sourceChart2.setHelmChartType(HelmChartType.CNF);
        sourceChart2.setReleaseName("test_application-2");
        sourceVnfInstance.setHelmCharts(List.of(sourceChart1, sourceChart2));

        PackageResponse targetPackageInfo = readPackageInfo("instance-service/multi-chart-descriptor-model.json");

        // when
        VnfInstance updatedVnfInstance = instanceService.createTempInstanceForUpgradeOperation(sourceVnfInstance,
                                                                                               targetPackageInfo,
                                                                                               Optional.empty());

        // then
        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly(null, null);

        // when
        final var helmReleaseNameGenerator = HelmReleaseNameGenerator.forUpgrade(updatedVnfInstance, updatedVnfInstance.getHelmCharts());

        updatedVnfInstance.getHelmCharts().forEach(helmChart -> {
            String newReleaseName = helmReleaseNameGenerator.generateNextReleaseName();
            helmChart.setReleaseName(newReleaseName);
        });

        // then
        assertThat(updatedVnfInstance.getHelmCharts())
                .hasSize(2)
                .extracting(HelmChartBaseEntity::getReleaseName)
                .containsOnly("test_application-1", "test_application-2");
    }

    private CreateVnfRequest getCreateVnfRequest(final String vnfdId) {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest();
        createVnfRequest.setVnfdId(vnfdId);
        createVnfRequest.setVnfInstanceName("test_application");
        createVnfRequest.setVnfInstanceDescription("dummy application to be created");
        return createVnfRequest;
    }

    private PackageResponse readPackageInfo(final String fileName) {
        try {
            return mapper.readValue(readDataFromFile(getClass(), fileName), PackageResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
