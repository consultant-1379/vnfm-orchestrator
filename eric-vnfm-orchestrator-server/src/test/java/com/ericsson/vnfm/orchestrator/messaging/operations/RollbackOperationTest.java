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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.ROLLBACK;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.github.jknack.handlebars.internal.lang3.StringUtils;


public class RollbackOperationTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private RollbackOperation rollbackOperation;

    @Autowired
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @SpyBean
    private WorkflowRoutingServicePassThrough workflowRoutingService;

    @SpyBean
    private PackageService packageService;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private InstanceService instanceService;

    @Test
    public void testRollbackSuccessForSingleChart() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.COMPLETED);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        final String lifecycleOperationId = "23f70380-74ce-11ea-bc55-0242ac130003";
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-single-success-1");
        firstChart.setRevisionNumber("1");
        rollbackOperation.completed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        assertThat(vnfInstance.getHelmCharts()).extracting("state", "revisionNumber")
                .containsExactlyInAnyOrder(
                        tuple(HelmReleaseState.ROLLED_BACK.toString(),"1"));
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    @Test
    public void testRollbackOperationAfterFirstChartSuccess() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.PROCESSING);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        final String lifecycleOperationId = "0dd3755c-25ca-4672-90af-c0d89550dc97";
        setOperationTimeouts(lifecycleOperationId);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-multiple-success-2");

        whenWfsRollbackRespondsWithAccepted();

        rollbackOperation.completed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String tempInstanceString = vnfInstance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-success-1")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLING_BACK.toString());
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-success-2")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLED_BACK.toString());
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    @Test
    public void testRollbackFailureForSingleChart() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        final String lifecycleOperationId = "64bdf09d-b9e4-4c68-8685-76c90d3d39b9";
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-single-failed-1");
        firstChart.setMessage("Helm/kubectl command timedOut.");
        rollbackOperation.failed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        assertThat(vnfInstance.getHelmCharts().stream()
                .allMatch(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.FAILED.toString()))).isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);

    }

    @Test
    public void testRollbackFailureSuccessFirstFailureSecond() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.PROCESSING);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        final String lifecycleOperationId = "695d82df-6e60-452c-8127-2bf486e3f607";
        setOperationTimeouts(lifecycleOperationId);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-multiple-failure-1");

        whenWfsRollbackRespondsWithAccepted();

        rollbackOperation.completed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String tempInstanceString = vnfInstance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-failure-1")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLED_BACK.toString());
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-failure-2")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLING_BACK.toString());
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.FAILED);
        secondChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("rollback-multiple-failure-2");
        secondChart.setMessage("Helm/kubectl command timedOut.");
        rollbackOperation.failed(secondChart);


        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance instance = operationOccurrenceId.getVnfInstance();
        assertThat(instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-failure-1")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLED_BACK.toString());
        assertThat(instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-multiple-failure-2")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.FAILED.toString());
        assertThat(operationOccurrenceId.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                       + "\"detail\":\"Rollback failed for rollback-multiple-failure-2 due to Helm/kubectl command timedOut.\",\"instance\":\"about:blank\"}");
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    @Test
    public void testRollbackFailureFailureFirstSuccessSecond() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        final String lifecycleOperationId = "bb26cacc-bcae-411b-98bf-75175e66a555";
        setOperationTimeouts(lifecycleOperationId);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-mixed-failure-1");
        firstChart.setMessage("Helm/kubectl command timedOut.");

        whenWfsRollbackRespondsWithAccepted();

        rollbackOperation.failed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String tempInstanceString = vnfInstance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-mixed-failure-1")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.FAILED.toString());
        assertThat(upgradedInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-mixed-failure-2")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLING_BACK.toString());
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);

        final HelmReleaseLifecycleMessage secondChart = new HelmReleaseLifecycleMessage();
        secondChart.setState(HelmReleaseState.FAILED);
        secondChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        secondChart.setLifecycleOperationId(lifecycleOperationId);
        secondChart.setReleaseName("rollback-mixed-failure-2");
        rollbackOperation.completed(secondChart);

        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance instance = operationOccurrenceId.getVnfInstance();
        assertThat(instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-mixed-failure-1")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.FAILED.toString());
        assertThat(instance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), "rollback-mixed-failure-2")).findFirst().get()
                .getState()).isEqualTo(LifecycleOperationState.ROLLED_BACK.toString());
        assertThat(operationOccurrenceId.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                       + "\"detail\":\"Rollback failed for rollback-mixed-failure-1 due to Helm/kubectl command timedOut.\",\"instance\":\"about:blank\"}");
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    @Test
    public void testRollbackOnFailedUpgradeSingleChartRollbackSuccess() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        firstChart.setMessage("Helm/kubectl command timedOut");
        final String lifecycleOperationId = "51e01da5-7ab6-4784-923a-61081a71818f";
        setOperationTimeouts(lifecycleOperationId);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("upgrade-failed-single-1");

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String tempInstanceString = vnfInstance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        assertThat(upgradedInstance.getHelmCharts().stream()
                .allMatch(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.ROLLING_BACK.toString()))).isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(lifecycleOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                    + "\"detail\":\"CHANGE_VNFPKG for upgrade-failed-single-1 failed with Helm/kubectl command timedOut.\",\"instance\":\"about:blank\"}");

        final HelmReleaseLifecycleMessage chart = new HelmReleaseLifecycleMessage();
        chart.setState(HelmReleaseState.COMPLETED);
        chart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        chart.setLifecycleOperationId(lifecycleOperationId);
        chart.setReleaseName("upgrade-failed-single-1");
        rollbackOperation.completed(chart);
        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance instance = operationOccurrenceId.getVnfInstance();
        assertThat(instance.getHelmCharts().stream()
                .allMatch(chart1 -> StringUtils.equals(chart1.getState(), LifecycleOperationState.ROLLED_BACK.toString()))).isTrue();
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);

    }

    @Test
    public void testRollbackOnFailedUpgradeSingleChartRollbackFailed() {
        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        firstChart.setMessage("Helm/kubectl command timedOut");
        final String lifecycleOperationId = "c2921594-4b51-4969-b84a-ed5743633d99";
        setOperationTimeouts(lifecycleOperationId);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("upgrade-failed-rollback-1");

        whenWfsRollbackRespondsWithAccepted();

        changeVnfPackageOperation.failed(firstChart);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String tempInstanceString = vnfInstance.getTempInstance();
        VnfInstance upgradedInstance = parseJson(tempInstanceString, VnfInstance.class);
        assertThat(upgradedInstance.getHelmCharts().stream()
                .allMatch(chart -> StringUtils.equals(chart.getState(), LifecycleOperationState.ROLLING_BACK.toString()))).isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(lifecycleOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                                    + "\"detail\":\"CHANGE_VNFPKG for upgrade-failed-rollback-1 failed with Helm/kubectl command timedOut.\",\"instance\":\"about:blank\"}");

        final HelmReleaseLifecycleMessage chart = new HelmReleaseLifecycleMessage();
        chart.setState(HelmReleaseState.FAILED);
        chart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        chart.setMessage("Helm/kubectl command timedOut.");
        chart.setLifecycleOperationId(lifecycleOperationId);
        chart.setReleaseName("upgrade-failed-rollback-1");
        rollbackOperation.failed(chart);
        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance instance = operationOccurrenceId.getVnfInstance();
        assertThat(instance.getHelmCharts().stream()
                           .allMatch(chart1 -> StringUtils.equals(chart1.getState(), LifecycleOperationState.FAILED.toString()))).isTrue();
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);

        assertThat(operationOccurrenceId.getError()).isEqualTo(
                "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"CHANGE_VNFPKG for upgrade-failed-rollback-1 failed"
                        + " with Helm/kubectl command timedOut.\\nRollback failed for upgrade-failed-rollback-1 due to Helm/kubectl command "
                        + "timedOut.\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void testRollbackOnCompleteDowngradeSingleChart() {
        final String lifecycleOperationId = "64024380-5db5-4d95-9ed1-598056558c71";
        HelmReleaseLifecycleMessage helmMessage = new HelmReleaseLifecycleMessage();
        helmMessage.setState(HelmReleaseState.COMPLETED);
        helmMessage.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        helmMessage.setLifecycleOperationId(lifecycleOperationId);
        helmMessage.setReleaseName("downgrade-single-success-1");

        rollbackOperation.completed(helmMessage);

        assertDowngradeOperationSuccessful(lifecycleOperationId, 1);
    }

    @Test
    public void testRollbackOnCompleteDowngradeMultipleChart() {
        final String lifecycleOperationId = "d4889fa1-561a-4303-bc2f-bdb2339abc30";
        HelmReleaseLifecycleMessage helmMessage = new HelmReleaseLifecycleMessage();
        helmMessage.setState(HelmReleaseState.COMPLETED);
        helmMessage.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        helmMessage.setLifecycleOperationId(lifecycleOperationId);
        helmMessage.setReleaseName("downgrade-single-success-1");

        rollbackOperation.completed(helmMessage);

        assertDowngradeOperationSuccessful(lifecycleOperationId, 2);
    }

    @Test
    public void testRollbackOnFailedDowngradeSingleChart() {
        final HelmReleaseLifecycleMessage helmMessage = new HelmReleaseLifecycleMessage();
        final String lifecycleOperationId = "84324380-5db5-4d95-9ed1-598056558c71";
        helmMessage.setState(HelmReleaseState.FAILED);
        helmMessage.setOperationType(HelmReleaseOperationType.CHANGE_VNFPKG);
        helmMessage.setLifecycleOperationId(lifecycleOperationId);
        helmMessage.setReleaseName("downgrade-single-failed-1");

        rollbackOperation.failed(helmMessage);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(lifecycleOperation).isNotNull();
        assertThat(StringUtils.isNotEmpty(lifecycleOperation.getError())).isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(lifecycleOperation.getVnfInstance().getHelmCharts().stream()
                           .allMatch(chart ->
                                             StringUtils.equals(chart.getState(), LifecycleOperationState.FAILED.toString())))
                .isTrue();
    }

    @Test
    public void testRollbackSuccessShouldNotRollbackCrd() {
        final String cnfReleaseName = "crd-inst-cnf";
        final String lifecycleOperationId = "rollback1Crd-1234-1234-1234-123456789010";

        final HelmReleaseLifecycleMessage crdFailedRollbackMessage = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(crdFailedRollbackMessage, HelmReleaseState.COMPLETED, cnfReleaseName, ROLLBACK);
        crdFailedRollbackMessage.setLifecycleOperationId(lifecycleOperationId);

        rollbackOperation.completed(crdFailedRollbackMessage);
        verify(workflowRoutingService, times(0))
                .routeRollbackRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyString(), anyString());
    }

    @Test
    public void testRollbackFailedShouldNotRollbackCrd() {
        final String cnfReleaseName = "crd-inst-cnf";
        final String lifecycleOperationId = "rollback2Crd-1234-1234-1234-123456789010";

        final HelmReleaseLifecycleMessage crdFailedRollbackMessage = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(crdFailedRollbackMessage, HelmReleaseState.FAILED, cnfReleaseName, ROLLBACK);
        crdFailedRollbackMessage.setLifecycleOperationId(lifecycleOperationId);

        rollbackOperation.failed(crdFailedRollbackMessage);
        verify(workflowRoutingService, times(0))
                .routeRollbackRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackFailureForSingleChartWhenUpdateUsageStateThrowError() {
        // given
        ReflectionTestUtils.setField(instanceService, "smallStackApplication","true");

        final String lifecycleOperationId = "342fg9d-b9e4-4c68-8685-76c90d890pty";

        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.FAILED);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-single-failed-2");
        firstChart.setMessage("Helm/kubectl command timedOut.");

        doThrow(new InternalRuntimeException("Simulate internal server error"))
                .when(packageService).updateUsageState(anyString(), anyString(), anyBoolean());

        // when
        rollbackOperation.failed(firstChart);

        // then
        verify(packageService, times(1))
                .updateUsageState(anyString(), anyString(), anyBoolean());

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        assertThat(vnfInstance.getHelmCharts())
                .extracting(HelmChartBaseEntity::getState)
                .containsOnly(LifecycleOperationState.FAILED.toString());
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackSuccessForSingleChartWhenUpdateUsageStateThrowError() {
        // given
        ReflectionTestUtils.setField(instanceService, "smallStackApplication", "true");

        final String lifecycleOperationId = "fa88b87a-bf7c-43ac-8b18-1f6494c124a7";

        final HelmReleaseLifecycleMessage firstChart = new HelmReleaseLifecycleMessage();
        firstChart.setState(HelmReleaseState.COMPLETED);
        firstChart.setOperationType(HelmReleaseOperationType.ROLLBACK);
        firstChart.setLifecycleOperationId(lifecycleOperationId);
        firstChart.setReleaseName("rollback-single-success-2");
        firstChart.setRevisionNumber("1");

        doThrow(new InternalRuntimeException("Simulate internal server error"))
                .when(packageService).updateUsageState(anyString(), anyString(), anyBoolean());

        // when
        rollbackOperation.completed(firstChart);

        // then
        verify(packageService, times(1))
                .updateUsageState(anyString(), anyString(), anyBoolean());

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        assertThat(vnfInstance.getHelmCharts())
                .extracting(HelmChartBaseEntity::getState, HelmChartBaseEntity::getRevisionNumber)
                .containsExactly(tuple(HelmReleaseState.ROLLED_BACK.toString(), "1"));
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    private void assertDowngradeOperationSuccessful(String lifecycleOperationId, int sizeofHelmChartsExpected) {
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        assertThat(vnfInstance).isNotNull();
        assertThat(vnfInstance.getHelmCharts()).isNotNull();
        assertThat(vnfInstance.getHelmCharts().size()).isEqualTo(sizeofHelmChartsExpected);
        assertThat(vnfInstance.getCombinedAdditionalParams()).isEqualTo("downgrade-params");
        assertThat(vnfInstance.getCombinedValuesFile()).isEqualTo("downgrade-file");
        assertThat(vnfInstance.getHelmCharts().stream()
                           .allMatch(chart ->
                                             StringUtils.equals(chart.getState(), LifecycleOperationState.COMPLETED.toString())))
                .isTrue();
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    private static void getHelmReleaseMessage(final HelmReleaseLifecycleMessage message, final HelmReleaseState state,
                                              final String releaseName, final HelmReleaseOperationType type) {
        message.setState(state);
        message.setReleaseName(releaseName);
        message.setOperationType(type);
    }

    private void whenWfsRollbackRespondsWithAccepted() {
        when(restTemplate.exchange(endsWith("rollback"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private void setOperationTimeouts(final String lifecycleOperationId) {
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        byOperationOccurrenceId.setApplicationTimeout("80");
        byOperationOccurrenceId.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        lifecycleOperationRepository.save(byOperationOccurrenceId);
    }
}
