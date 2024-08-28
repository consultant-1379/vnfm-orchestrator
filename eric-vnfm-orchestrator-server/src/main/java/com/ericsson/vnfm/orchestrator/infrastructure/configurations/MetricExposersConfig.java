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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CLUSTERS_COUNT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CLUSTERS_COUNT_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CLUSTERS_COUNT_USED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CLUSTERS_COUNT_USED_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.RUNNING_LIFECYCLE_OPERATIONS_COUNT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.RUNNING_LIFECYCLE_OPERATIONS_COUNT_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.RUNNING_LIFECYCLE_OPERATION_TAG;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

@Configuration
public class MetricExposersConfig {

    private static final List<LifecycleOperationType> LIFECYCLE_OPERATION_TYPES_TO_EXPOSE = List.of(LifecycleOperationType.INSTANTIATE,
                                                                                                    LifecycleOperationType.SCALE,
                                                                                                    LifecycleOperationType.TERMINATE,
                                                                                                    LifecycleOperationType.SYNC,
                                                                                                    LifecycleOperationType.CHANGE_VNFPKG,
                                                                                                    LifecycleOperationType.HEAL,
                                                                                                    LifecycleOperationType.MODIFY_INFO);

    @Bean
    Gauge exposeClustersInUseCount(MeterRegistry meterRegistry, ClusterConfigFileRepository clusterConfigFileRepository) {
        return Gauge.builder(CLUSTERS_COUNT_USED, clusterConfigFileRepository, ClusterConfigFileRepository::getClustersInUseCount)
                .description(CLUSTERS_COUNT_USED_DESCRIPTION)
                .register(meterRegistry);
    }

    @Bean
    Gauge exposeClustersCount(MeterRegistry meterRegistry, ClusterConfigFileRepository clusterConfigFileRepository) {
        return Gauge.builder(CLUSTERS_COUNT, clusterConfigFileRepository, ClusterConfigFileRepository::getClustersCount)
                .description(CLUSTERS_COUNT_DESCRIPTION)
                .register(meterRegistry);
    }

    @Bean
    MultiGauge exposeLifecycleOperationsCount(MeterRegistry meterRegistry, DatabaseInteractionService databaseInteractionService) {
        MultiGauge multiGauge = MultiGauge.builder(RUNNING_LIFECYCLE_OPERATIONS_COUNT)
                .description(RUNNING_LIFECYCLE_OPERATIONS_COUNT_DESCRIPTION)
                .register(meterRegistry);
        List<MultiGauge.Row<?>> rows = LIFECYCLE_OPERATION_TYPES_TO_EXPOSE.stream()
                .map(type -> MultiGauge.Row.of(Tags.of(RUNNING_LIFECYCLE_OPERATION_TAG, type.name()),
                    () -> databaseInteractionService.getOperationsCountNotInTerminalStatesByType(type)))
                .collect(Collectors.toList());
        multiGauge.register(rows, true);
        return multiGauge;
    }
}
