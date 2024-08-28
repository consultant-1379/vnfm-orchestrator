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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.ericsson.vnfm.orchestrator.TestUtils.createCnfHelmChart;
import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.retrieveFailedHelmChart;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils.retrieveFailingUpgradeHelmChart;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Suite
@SelectClasses({
        OperationsUtilsTest.RetrieveFailedHelmChartSuccessTest.class,
        OperationsUtilsTest.RetrieveFailedHelmChartFailedTest.class
})
public class OperationsUtilsTest {

    public static class RetrieveFailedHelmChartSuccessTest {

        public static Collection<Object[]> getChartState() {
            return List.of(new Object[][]{
                    {createCnfHelmChart(1), createCnfHelmChart(2, FAILED), createCnfHelmChart(2, FAILED)},
                    {createCnfHelmChart(1, FAILED), createCnfHelmChart(2), createCnfHelmChart(1, FAILED)},
                    {createCnfHelmChart(1, COMPLETED), createCnfHelmChart(2, FAILED), createCnfHelmChart(2, FAILED)},
                    {createCnfHelmChart(1, FAILED), createCnfHelmChart(2, COMPLETED), createCnfHelmChart(1, FAILED)},
            });
        }

        @ParameterizedTest(name = "testRetrieveFailedHelmChart: chart1 {0} chart2 {1} expected {2}")
        @MethodSource("getChartState")
        public void testRetrieveFailedHelmChartSuccess(HelmChart cnfHelmChart1, HelmChart cnfHelmChart2, HelmChart expectedCnfHelmChart) {
            VnfInstance tempInstance = createVnfInstance(true);
            tempInstance.setHelmCharts(List.of(cnfHelmChart1, cnfHelmChart2));
            LifecycleOperation lifecycleOperation = TestUtils.createLifecycleOperation(tempInstance, CHANGE_VNFPKG, FAILED_TEMP);
            HelmChart helmChart = retrieveFailedHelmChart(tempInstance, lifecycleOperation);
            assertThat(helmChart).isNotNull().usingRecursiveComparison()
                    .comparingOnlyFields("id", "helmChartArtifactKey", "helmChartType", "state")
                    .isEqualTo(expectedCnfHelmChart);
        }
    }

    public static class RetrieveFailedHelmChartFailedTest {

        @Test
        public void testRetrieveFailedHelmChartFail() {
            VnfInstance tempInstance = createVnfInstance(true);
            HelmChart cnfHelmChart1 = createCnfHelmChart(1);
            HelmChart cnfHelmChart2 = createCnfHelmChart(2);
            tempInstance.setHelmCharts(Arrays.asList(cnfHelmChart1, cnfHelmChart2));
            LifecycleOperation lifecycleOperation = TestUtils.createLifecycleOperation(tempInstance, CHANGE_VNFPKG, FAILED_TEMP);
            assertThatThrownBy(() -> retrieveFailedHelmChart(tempInstance, lifecycleOperation))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Unable to identify the chart to rollback.");
        }

        @Test
        public void testRetrieveFailingUpgradeHelmChartPositive() {
            VnfInstance sourceInstance = createVnfInstance(false);
            sourceInstance.setHelmCharts(List.of(createCnfHelmChart(0, COMPLETED)));
            VnfInstance tempInstance = createVnfInstance(false);
            final HelmChart failedCnfChart = createCnfHelmChart(1, FAILED);
            tempInstance.setHelmCharts(List.of(failedCnfChart));
            sourceInstance.setTempInstance(convertObjToJsonString(tempInstance));

            assertThat(retrieveFailingUpgradeHelmChart(sourceInstance, tempInstance)).hasValue(failedCnfChart);
        }
    }
}