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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_HELM_CLIENT_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.DELETE_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        WorkflowRoutingServicePassThrough.class,
        CryptoUtils.class
})
@MockBean(classes = {
        DatabaseInteractionService.class,
        ValuesFileService.class,
        ScaleService.class,
        ObjectMapper.class,
        YamlMapFactoryBean.class,
        CryptoService.class,
        WorkflowRequestBodyBuilder.class,
        RestTemplate.class,
        ReplicaDetailsMapper.class })
@MockBean(classes = RetryTemplate.class, name = "wfsRoutingRetryTemplate")
public class EvnfmWorkFlowTerminateRequestTest {

    @Autowired
    private WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    private final String operationOccuranceId = UUID.randomUUID().toString();

    @SuppressWarnings("unchecked")
    @Test
    public void testEvnfmWorkflowTerminateRequest() {
        var helmCharts = createHelmCharts();
        var vnfInstance = createVnfInstance(helmCharts);
        String terminateRequest = workflowRoutingServicePassThrough
                .getTerminateRequest(vnfInstance, createLifeCycleOperation(vnfInstance), (Map) createTerminateVnfRequestBody().getAdditionalParams(), "test");
        assertThat(terminateRequest).isEqualTo("http://localhost:10103/api/lcm/v3/resources/test/terminate?applicationTimeOut"
                + "=500&clusterName=changepackage-1266&skipJobVerification"
                + "=true&namespace=testchangepackage&lifecycleOperationId="+ operationOccuranceId + "&state"
                + "=PROCESSING&skipVerification=true");
    }

    @Test
    public void testEvnfmWorkflowTerminateRequestWithHelmClientVersion() {
        var helmCharts = createHelmCharts();
        var vnfInstance = createVnfInstance(helmCharts);
        var operation = createLifeCycleOperation(vnfInstance);
        operation.setHelmClientVersion(DUMMY_HELM_CLIENT_VERSION);
        String terminateRequest = workflowRoutingServicePassThrough
                .getTerminateRequest(vnfInstance, operation,
                                     (Map) createTerminateVnfRequestBody().getAdditionalParams(),
                                     "test");
        assertThat(terminateRequest).isEqualTo("http://localhost:10103/api/lcm/v3/resources/test/terminate?applicationTimeOut"
                                                       + "=500&helmClientVersion=3.8&clusterName=changepackage-1266&skipJobVerification"
                                                       + "=true&namespace=testchangepackage&lifecycleOperationId=" + operationOccuranceId + "&state"
                                                       + "=PROCESSING&skipVerification=true");
    }

    private List<HelmChart> createHelmCharts() {
        HelmChart chart1 = new HelmChart();
        chart1.setHelmChartType(HelmChartType.CNF);
        HelmChart chart2 = new HelmChart();
        chart2.setHelmChartType(HelmChartType.CNF);
        List<HelmChart> helmChartList = List.of(chart1, chart2);
        return helmChartList;
    }

    private VnfInstance createVnfInstance(final List<HelmChart> helmChartList) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setOperationOccurrenceId(operationOccuranceId);
        vnfInstance.setHelmCharts(helmChartList);
        vnfInstance.setClusterName("changepackage-1266");
        vnfInstance.setNamespace("testchangepackage");
        return vnfInstance;
    }

    private LifecycleOperation createLifeCycleOperation(final VnfInstance vnfInstance) {
        LifecycleOperation operation = new LifecycleOperation();
        operation.setVnfInstance(vnfInstance);
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setOperationOccurrenceId(operationOccuranceId);
        operation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);
        operation.setAutomaticInvocation(false);
        operation.setCancelMode(CancelModeType.FORCEFUL);
        return operation;
    }

    private TerminateVnfRequest createTerminateVnfRequestBody() {
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        additionalParams.put(SKIP_JOB_VERIFICATION, true);
        additionalParams.put(SKIP_VERIFICATION, true);
        additionalParams.put(DELETE_IDENTIFIER, true);
        additionalParams.put(APPLICATION_TIME_OUT, "500");
        request.setAdditionalParams(additionalParams);
        return request;
    }
}
