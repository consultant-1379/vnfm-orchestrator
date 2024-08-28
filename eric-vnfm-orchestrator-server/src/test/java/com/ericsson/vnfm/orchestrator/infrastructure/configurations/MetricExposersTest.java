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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.RUNNING_LIFECYCLE_OPERATIONS_COUNT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.RUNNING_LIFECYCLE_OPERATION_TAG;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;


import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CLOSE_SSH_TO_ENM_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.COMPLETED_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CONNECTIONS_TO_ENM_METRIC_TAGS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.ENM_METRIC_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.INSTANTIATE_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.INSTANTIATE_ENROLLMENT_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPEN_SSH_TO_ENM_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.STARTED_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class MetricExposersTest {

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    private final MetricExposersConfig config = new MetricExposersConfig();

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    public void testExposingLifecycleOperationsCount() {
        //given
        Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap = Map.of(
                LifecycleOperationType.INSTANTIATE, 2,
                LifecycleOperationType.SCALE, 0,
                LifecycleOperationType.TERMINATE, 4,
                LifecycleOperationType.SYNC, 3,
                LifecycleOperationType.CHANGE_VNFPKG, 1,
                LifecycleOperationType.HEAL, 7,
                LifecycleOperationType.MODIFY_INFO, 2
                );

        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.INSTANTIATE))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.INSTANTIATE));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.SCALE))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SCALE));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.TERMINATE))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.TERMINATE));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.SYNC))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SYNC));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.CHANGE_VNFPKG))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.CHANGE_VNFPKG));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.HEAL))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.HEAL));
        when(databaseInteractionService.getOperationsCountNotInTerminalStatesByType(LifecycleOperationType.MODIFY_INFO))
                .thenReturn(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.MODIFY_INFO));

        //when
        config.exposeLifecycleOperationsCount(meterRegistry, databaseInteractionService);

        //then
        assertThat(meterRegistry.getMeters()).hasSize(7);

        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.INSTANTIATE.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.INSTANTIATE).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.SCALE.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SCALE).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.TERMINATE.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.TERMINATE).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.SYNC.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.SYNC).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.CHANGE_VNFPKG.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.CHANGE_VNFPKG).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.HEAL.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.HEAL).intValue());
        assertThat(meterRegistry.get(RUNNING_LIFECYCLE_OPERATIONS_COUNT).tag(RUNNING_LIFECYCLE_OPERATION_TAG, LifecycleOperationType.MODIFY_INFO.name()).gauge().value())
                .isEqualTo(lifecycleOperationTypeIntegerMap.get(LifecycleOperationType.MODIFY_INFO).intValue());
    }

    @Test
    public void exposeEnmOperations() {
        Map<String, Integer> metricsValue = Map.of(
                OPEN_SSH_TO_ENM_METRIC_TAG, 2,
                CLOSE_SSH_TO_ENM_METRIC_TAG, 1,
                STARTED_ADD_NODE_METRIC_TAG, 1,
                COMPLETED_ADD_NODE_METRIC_TAG, 1,
                INSTANTIATE_ADD_NODE_METRIC_TAG, 3,
                INSTANTIATE_ENROLLMENT_METRIC_TAG, 0
        );
        Map<String, Object> additionalParams = Map.of(ADD_NODE_TO_OSS, true);

        EnmMetricsExposers enmMetricsExposers = new EnmMetricsExposers(meterRegistry);
        enmMetricsExposers.getConnToEnmMetric().increment();
        enmMetricsExposers.getConnToEnmMetric().increment();
        enmMetricsExposers.getConnToEnmMetric().decrement();
        enmMetricsExposers.getAddNodeMetric().increment();
        enmMetricsExposers.getAddNodeMetric().decrement();
        enmMetricsExposers.incrementInstantiateMetrics(INSTANTIATE, additionalParams);
        enmMetricsExposers.incrementInstantiateMetrics(INSTANTIATE, additionalParams);
        enmMetricsExposers.incrementInstantiateMetrics(INSTANTIATE, additionalParams);


        assertThat(meterRegistry.getMeters()).hasSize(6);

        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, OPEN_SSH_TO_ENM_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(OPEN_SSH_TO_ENM_METRIC_TAG));
        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, CLOSE_SSH_TO_ENM_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(CLOSE_SSH_TO_ENM_METRIC_TAG));
        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, STARTED_ADD_NODE_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(STARTED_ADD_NODE_METRIC_TAG));
        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, COMPLETED_ADD_NODE_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(COMPLETED_ADD_NODE_METRIC_TAG));
        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, INSTANTIATE_ADD_NODE_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(INSTANTIATE_ADD_NODE_METRIC_TAG));
        assertThat((int) meterRegistry.get(CONNECTIONS_TO_ENM_METRIC_TAGS).tag(ENM_METRIC_NAME, INSTANTIATE_ENROLLMENT_METRIC_TAG)
                .counter().count()).isEqualTo(metricsValue.get(INSTANTIATE_ENROLLMENT_METRIC_TAG));

    }
}