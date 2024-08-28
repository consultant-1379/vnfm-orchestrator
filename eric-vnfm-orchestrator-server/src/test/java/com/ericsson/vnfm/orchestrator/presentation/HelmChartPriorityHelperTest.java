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
package com.ericsson.vnfm.orchestrator.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.createCnfHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.createCrdHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.prepareHelmCHarts;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartPriorityHelper;

public class HelmChartPriorityHelperTest {
    private static final String VNF_INSTANCE_ID = UUID.randomUUID().toString();

    private static final String RANDOM_STRING = UUID.randomUUID().toString();

    private static void checkHelmChart(String helmPackage, HelmChart cnfChart, int expected, int expected1) {
        if (helmPackage.equals(cnfChart.getHelmChartArtifactKey())) {
            assertThat(cnfChart.getOperationChartsPriority().get(LifecycleOperationType.TERMINATE)).isNotNull().isEqualTo(expected);
            assertThat(cnfChart.getOperationChartsPriority().get(LifecycleOperationType.INSTANTIATE)).isNotNull().isEqualTo(expected1);
        }
    }

    public static void setDefaultPriority(List<HelmChart> helmCharts) {
        int defaultPriority = 1;
        for (HelmChart helmChart : helmCharts) {
            helmChart.setPriority(defaultPriority);
            Map<LifecycleOperationType, Integer> operationChartsPriority = new HashMap<>();
            operationChartsPriority.putAll(Map.of(LifecycleOperationType.CHANGE_VNFPKG, defaultPriority,
                    LifecycleOperationType.INSTANTIATE, defaultPriority));
            helmChart.setOperationChartsPriority(operationChartsPriority);
            defaultPriority++;
        }
    }

    @Test
    public void testPrioritizedTerminateHelmChartsSuccess() {
        List<HelmChart> helmCharts = prepareHelmCHarts();
        setDefaultPriority(helmCharts);
        List<HelmChart> prioritizedTerminateHelmCharts = HelmChartPriorityHelper.getHelmChartsWithReversedTerminate(createVnfInstance(), helmCharts);
        List<HelmChart> cnfCharts = prioritizedTerminateHelmCharts.stream().filter(i -> i.getHelmChartType().equals(HelmChartType.CNF))
                .collect(Collectors.toList());
        assertThat(cnfCharts).isNotNull().hasSize(4);
        for (HelmChart cnfChart : cnfCharts) {
            checkHelmChart("helm_package4", cnfChart, 1, 8);
            checkHelmChart("helm_package3", cnfChart, 2, 7);
            checkHelmChart("helm_package2", cnfChart, 3, 6);
            checkHelmChart("helm_package1", cnfChart, 4, 5);
        }
        List<HelmChart> crdCharts = prioritizedTerminateHelmCharts.stream().filter(i -> i.getHelmChartType().equals(HelmChartType.CRD))
                .collect(Collectors.toList());
        assertThat(crdCharts).isNotNull();
    }

    @Test
    public void testPrioritizedTerminateHelmChartsFail() {
        List<HelmChart> helmCharts = prepareHelmCHarts();
        assertThatThrownBy(() -> HelmChartPriorityHelper.getHelmChartsWithReversedTerminate(createVnfInstance(), helmCharts))
                .isInstanceOf(InternalRuntimeException.class)
                .hasMessage("The operation charts priority map should not be empty.");
    }

    @Test
    public void testCalculatePriorityInstantiateAndCcvpDefined() {
        Map<LCMOperationsEnum, List<HelmPackage>> lcmOperationsEnumListMap = new EnumMap<>(LCMOperationsEnum.class);
        HelmPackage helmPackage = new HelmPackage("helm_package1", 3);
        HelmPackage helmPackage2 = new HelmPackage("helm_package1", 4);
        lcmOperationsEnumListMap.put(LCMOperationsEnum.INSTANTIATE, List.of(helmPackage));
        lcmOperationsEnumListMap.put(LCMOperationsEnum.CHANGE_CURRENT_PACKAGE, List.of(helmPackage2));
        HelmChart helmChart = createCnfHelmChart(1);
        helmChart.setPriority(5);
        Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap = HelmChartPriorityHelper.calculatePriority(helmChart, lcmOperationsEnumListMap);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.INSTANTIATE)).isEqualTo(3);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.CHANGE_VNFPKG)).isEqualTo(4);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SCALE)).isEqualTo(3);
    }

    @Test
    public void testCalculatePriorityCrdChart() {
        Map<LCMOperationsEnum, List<HelmPackage>> lcmOperationsEnumListMap = new EnumMap<>(LCMOperationsEnum.class);
        HelmPackage helmPackage = new HelmPackage("crd_package1", 3);
        HelmPackage helmPackage2 = new HelmPackage("helm_package1", 4);
        lcmOperationsEnumListMap.put(LCMOperationsEnum.INSTANTIATE, List.of(helmPackage));
        lcmOperationsEnumListMap.put(LCMOperationsEnum.SCALE, List.of(helmPackage2));
        HelmChart helmChart = createCrdHelmChart(1);
        helmChart.setPriority(5);
        Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap = HelmChartPriorityHelper.calculatePriority(helmChart, lcmOperationsEnumListMap);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.INSTANTIATE)).isEqualTo(3);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.CHANGE_VNFPKG)).isEqualTo(5);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SCALE)).isEqualTo(5);
    }

    @Test
    public void testCalculatePriorityInstantiateAndScaleDefined() {
        Map<LCMOperationsEnum, List<HelmPackage>> lcmOperationsEnumListMap = new EnumMap<>(LCMOperationsEnum.class);
        HelmPackage helmPackage = new HelmPackage("helm_package1", 3);
        HelmPackage helmPackage2 = new HelmPackage("helm_package1", 4);
        lcmOperationsEnumListMap.put(LCMOperationsEnum.INSTANTIATE, List.of(helmPackage));
        lcmOperationsEnumListMap.put(LCMOperationsEnum.SCALE, List.of(helmPackage2));
        HelmChart helmChart = createCnfHelmChart(1);
        helmChart.setPriority(5);
        Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap = HelmChartPriorityHelper.calculatePriority(helmChart, lcmOperationsEnumListMap);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.INSTANTIATE)).isEqualTo(3);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.CHANGE_VNFPKG)).isEqualTo(5);
        assertThat(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SCALE)).isEqualTo(4);
    }

    private VnfInstance createVnfInstance() {

        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId(VNF_INSTANCE_ID);
        instance.setVnfDescriptorId(RANDOM_STRING);
        return instance;
    }
}
