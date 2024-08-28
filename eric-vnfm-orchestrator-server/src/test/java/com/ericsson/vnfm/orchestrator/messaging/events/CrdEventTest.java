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
package com.ericsson.vnfm.orchestrator.messaging.events;

import static java.util.Collections.singletonList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.ChangeVnfPackageOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.InstantiateOperation;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;


@SpringBootTest(classes = CrdEvent.class)
public class CrdEventTest {

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private InstantiateOperation instantiateOperation;

    @MockBean
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private MessageUtility messageUtility;

    @Autowired
    private CrdEvent crdEvent;

    @Captor
    private ArgumentCaptor<VnfInstance> instanceCaptor;

    @Captor
    private ArgumentCaptor<LifecycleOperation> operationCaptor;

    @Test
    public void failedShouldProcessInstantiate() {
        // given
        final var message = createMessage();
        final var instance = createInstance();
        final var operation = createOperation(LifecycleOperationType.INSTANTIATE, instance);

        given(databaseInteractionService.getLifecycleOperation(anyString())).willReturn(operation);

        // when
        crdEvent.failed(message);

        // then
        verify(databaseInteractionService).persistVnfInstanceAndOperation(instanceCaptor.capture(), operationCaptor.capture());

        final var savedInstance = instanceCaptor.getValue();
        assertThat(savedInstance.getHelmCharts())
                .extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                .containsOnly(tuple("release-name-1", LifecycleOperationState.FAILED.toString()));

        final var savedOperation = operationCaptor.getValue();
        assertThat(savedOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(savedOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Error "
                                                                + "message 1\",\"instance\":\"about:blank\"}");

        verifyNoInteractions(instanceService);
    }

    @Test
    public void failedShouldProcessUpgradeUsageStateUpdated() {
        // given
        final var message = createMessage();

        final var upgradedInstance = new VnfInstance();
        upgradedInstance.setVnfPackageId("target-vnfd-id");

        final var instance = createInstance();
        instance.setTempInstance(convertObjToJsonString(upgradedInstance));

        final var operation = createOperation(LifecycleOperationType.CHANGE_VNFPKG, instance);

        given(databaseInteractionService.getLifecycleOperation(anyString())).willReturn(operation);

        // when
        crdEvent.failed(message);

        // then
        verify(instanceService).updateAssociationBetweenPackageAndVnfInstanceForFailedUpgrade(same(instance));

        verify(databaseInteractionService).persistVnfInstanceAndOperation(instanceCaptor.capture(), operationCaptor.capture());

        final var savedInstance = instanceCaptor.getValue();
        assertThat(savedInstance.getHelmCharts())
                .extracting(HelmChartBaseEntity::getReleaseName, HelmChartBaseEntity::getState)
                .containsOnly(tuple("release-name-1", LifecycleOperationState.ROLLED_BACK.toString()));

        final var savedOperation = operationCaptor.getValue();
        assertThat(savedOperation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(savedOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Error "
                                                                + "message 1\",\"instance\":\"about:blank\"}");
    }

    private static WorkflowServiceEventMessage createMessage() {
        final var message = new WorkflowServiceEventMessage();
        message.setLifecycleOperationId("lifecycle-operation-id-1");
        message.setReleaseName("release-name-1");
        message.setMessage("Error message 1");

        return message;
    }

    private static VnfInstance createInstance() {
        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id-1");
        instance.setHelmCharts(createCharts());
        instance.setVnfPackageId("source-vnfd-id");

        return instance;
    }

    private static List<HelmChart> createCharts() {
        final var chart = new HelmChart();
        chart.setReleaseName("release-name-1");

        return singletonList(chart);
    }

    private static LifecycleOperation createOperation(final LifecycleOperationType operationType, final VnfInstance instance) {
        final var operation = new LifecycleOperation();
        operation.setLifecycleOperationType(operationType);
        operation.setVnfInstance(instance);

        return operation;
    }

}
