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
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType.CNF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        WorkflowRoutingServicePassThrough.class,
        ObjectMapper.class,
        CryptoUtils.class
})
@MockBean(classes = {
        DatabaseInteractionService.class,
        ValuesFileService.class,
        ScaleService.class,
        YamlMapFactoryBean.class,
        CryptoService.class,
        WorkflowRequestBodyBuilder.class,
        RestTemplate.class,
        ReplicaDetailsMapper.class
})
@MockBean(classes = RetryTemplate.class, name = "wfsRoutingRetryTemplate")
public class EvnfmWorkFlowDeletePvcRequestTest {
    @Autowired
    private WorkflowRoutingServicePassThrough workflowRoutingServicePassThrough;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void testEvnfmWorkflowDeletePvcRequest() {
        String operationOccuranceId = UUID.randomUUID().toString();

        HelmChart chart1 = new HelmChart();
        chart1.setHelmChartType(CNF);
        chart1.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        chart1.setPriority(0);
        HelmChart chart2 = new HelmChart();
        chart2.setHelmChartType(CNF);
        chart2.setHelmChartUrl("https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz");
        chart2.setPriority(1);
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setClusterName("changepackage-1266");
        vnfInstance.setNamespace("testchangepackage");
        vnfInstance.setHelmCharts(List.of(chart1, chart2));
        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationOccurrenceId(operationOccuranceId);
        operation.setOperationState(PROCESSING);
        operation.setVnfInstance(vnfInstance);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(620));
        operation.setLifecycleOperationType(LifecycleOperationType.TERMINATE);

        when(databaseInteractionService
                .getLifecycleOperation(vnfInstance.getOperationOccurrenceId())).thenReturn(operation);

        String deletePvcRequest = workflowRoutingServicePassThrough
                .getDeletePvcRequest(vnfInstance, "test");
        assertThat(deletePvcRequest).isEqualTo(
                "http://localhost:10103/api/internal/kubernetes/pvcs/test/delete?applicationTimeOut=499&clusterName"
                        + "=changepackage-1266&namespace=testchangepackage&lifecycleOperationId="
                        + operationOccuranceId + "&state=PROCESSING");
        deletePvcRequest = workflowRoutingServicePassThrough
                .getDeletePvcRequest(vnfInstance, "test", "label1", "label2");
        assertThat(deletePvcRequest).isEqualTo(
                "http://localhost:10103/api/internal/kubernetes/pvcs/test/delete?applicationTimeOut=499&clusterName"
                        + "=changepackage-1266&namespace=testchangepackage&lifecycleOperationId="
                        + operationOccuranceId + "&state=PROCESSING&labels=label1,label2");
    }
}
