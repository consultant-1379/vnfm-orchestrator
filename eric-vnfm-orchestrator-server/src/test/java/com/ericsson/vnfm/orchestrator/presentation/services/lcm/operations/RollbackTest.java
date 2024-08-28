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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.Utility;


@SpringBootTest(classes = Rollback.class)
public class RollbackTest {

    @Autowired
    private Rollback rollback;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @MockBean
    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    private final static String RELEASE_NAME = "releaseName";
    private final static String REVISION_NUMBER = "revisionNumber";

    private HelmChart helmChart;
    private LifecycleOperation lifecycleOperation;

    @BeforeEach
    public void setUp() {
        VnfInstance vnfInstance = prepareVnfInstance();
        lifecycleOperation = prepareLifecycleOperation(vnfInstance);

        helmChart = new HelmChart();
        helmChart.setReleaseName(RELEASE_NAME);

        WorkflowRoutingResponse workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.ACCEPTED);

        Mockito.doReturn(workflowRoutingResponse)
                .when(workflowRoutingService)
                .routeRollbackRequest(Mockito.any(), Mockito.any(), Mockito.eq(RELEASE_NAME), Mockito.eq(REVISION_NUMBER));
    }

    @Test
    public void testExecuteDowngradeForRollback() {
        final String targetOperationOccurrenceId = "targetOperationOccurrenceId";
        final String descriptorModel = "descriptorModel";

        final List<HelmChartHistoryRecord> helmChartHistoryRecords = prepareHelmChartHistoryRecords();
        final ChangePackageOperationDetails changePackageOperationDetails = new ChangePackageOperationDetails();
        changePackageOperationDetails.setTargetOperationOccurrenceId(targetOperationOccurrenceId);
        changePackageOperationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.DOWNGRADE);
        final PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(descriptorModel);

        Mockito.doReturn(Optional.of(changePackageOperationDetails)).when(changePackageOperationDetailsRepository).findById(Mockito.anyString());
        Mockito.when(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(targetOperationOccurrenceId))
                .thenReturn(helmChartHistoryRecords);

        rollback.execute(lifecycleOperation, helmChart, true);

        Mockito.verify(workflowRoutingService)
                .routeRollbackRequest(Mockito.any(), Mockito.eq(lifecycleOperation), Mockito.eq(RELEASE_NAME), Mockito.eq(REVISION_NUMBER));
    }

    @Test
    public void testExecuteRollback() {
        rollback.execute(lifecycleOperation, helmChart, false);

        Mockito.verify(workflowRoutingService)
                .routeRollbackRequest(Mockito.any(), Mockito.eq(lifecycleOperation), Mockito.eq(RELEASE_NAME), Mockito.eq(REVISION_NUMBER));
    }

    private List<HelmChartHistoryRecord> prepareHelmChartHistoryRecords() {
        final HelmChartHistoryRecord helmChartHistoryRecord = new HelmChartHistoryRecord();
        helmChartHistoryRecord.setReleaseName(RELEASE_NAME);
        helmChartHistoryRecord.setRevisionNumber(REVISION_NUMBER);
        return List.of(helmChartHistoryRecord);
    }

    private VnfInstance prepareVnfInstance() {
        final String vnfDescriptorId = "vnfDescriptorId";
        final VnfInstance vnfInstance = new VnfInstance();
        final VnfInstance tempInstance = TestUtils.getVnfInstance();
        tempInstance.setVnfDescriptorId(vnfDescriptorId);
        tempInstance.setHelmCharts(Collections.emptyList());
        vnfInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        TestUtils.createHelmChart(vnfInstance);
        vnfInstance.getHelmCharts().forEach(helmChart -> {
            helmChart.setReleaseName(RELEASE_NAME);
            helmChart.setRevisionNumber(REVISION_NUMBER);
        });
        return vnfInstance;
    }

    private LifecycleOperation prepareLifecycleOperation(final VnfInstance vnfInstance) {
        final String targetOperationOccurrenceId = "targetOperationOccurrenceId";
        final String targetVnfdId = "targetVnfdId";
        final String applicationTimeout = "80";
        final LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setOperationOccurrenceId(targetOperationOccurrenceId);
        lifecycleOperation.setTargetVnfdId(targetVnfdId);
        lifecycleOperation.setApplicationTimeout(applicationTimeout);
        return lifecycleOperation;
    }
}
