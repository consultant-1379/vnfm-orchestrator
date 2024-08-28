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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

@TestComponent
public class AwaitHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwaitHelper.class);

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    public Callable<Boolean> operationReachesState(final String lifeCycleOperationId, final LifecycleOperationState expectedState) {
        return () -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

            return expectedState.equals(operation.getOperationState());
        };
    }

    public Callable<Boolean> operationReachesState(final String lifeCycleOperationId, final LifecycleOperationState... expectedStates) {
        return () -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

            return List.of(expectedStates).contains(operation.getOperationState());
        };
    }

    public Callable<Boolean> vnfInstanceIsDeleted(final String vnfInstanceId) {
        return () -> {
            VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);

            return vnfInstance == null;
        };
    }


    public Callable<Boolean> operationHasCombinedAdditionalParams(final String lifeCycleOperationId) {
        return () -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
            return operation.getCombinedAdditionalParams() != null;
        };
    }

    public Callable<Boolean> operationHasCombinedValuesFile(final String lifeCycleOperationId) {
        return () -> {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
            return operation.getCombinedValuesFile() != null;
        };
    }

    public Callable<Boolean> helmChartReachesState(final String releaseName,
                                                   final String vnfInstanceId,
                                                   final HelmReleaseState expectedState,
                                                   final boolean checkTempInstance) {

        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            if (checkTempInstance) {
                instance = parseJson(instance.getTempInstance(), VnfInstance.class);
            }
            final Stream<HelmChartBaseEntity> allCharts = Stream.concat(instance.getHelmCharts().stream(),
                                                                        instance.getTerminatedHelmCharts().stream());
            String helmChartState = allCharts
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst()
                    .map(HelmChartBaseEntity::getState)
                    .orElse(null);
            return expectedState.toString().equalsIgnoreCase(helmChartState);
        };
    }

    public Callable<Boolean> helmChartDeleted(final String releaseName, final String vnfInstanceId, final boolean checkTempInstance) {
        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            if (checkTempInstance) {
                instance = parseJson(instance.getTempInstance(), VnfInstance.class);
            }
            Optional<HelmChart> helmChart = instance.getHelmCharts().stream()
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst();
            return helmChart.isEmpty();
        };
    }

    public Callable<Boolean> helmChartReachesRetryCount(final String releaseName,
                                                        final String vnfInstanceId,
                                                        final int expectedCount,
                                                        final boolean checkTempInstance) {

        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            if (checkTempInstance) {
                instance = parseJson(instance.getTempInstance(), VnfInstance.class);
            }
            int retryCount = instance.getHelmCharts().stream()
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst()
                    .map(HelmChartBaseEntity::getRetryCount)
                    .orElse(null);
            return expectedCount == retryCount;
        };
    }

    public Callable<Boolean> helmChartDownSizeReachesState(final String releaseName,
                                                           final String vnfInstanceId,
                                                           final HelmReleaseState expectedState,
                                                           final boolean checkTempInstance) {

        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            if (checkTempInstance) {
                instance = parseJson(instance.getTempInstance(), VnfInstance.class);
            }
            String helmChartDownsizeState = instance.getHelmCharts().stream()
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst()
                    .map(HelmChartBaseEntity::getDownsizeState)
                    .orElse(null);
            return expectedState.toString().equalsIgnoreCase(helmChartDownsizeState);
        };
    }

    public Callable<Boolean> helmChartDeletePvcReachesState(final String releaseName,
                                                            final String vnfInstanceId,
                                                            final HelmReleaseState expectedState,
                                                            final boolean checkTempInstance) {

        return () -> {
            VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            if (checkTempInstance) {
                instance = parseJson(instance.getTempInstance(), VnfInstance.class);
            }
            final Stream<HelmChartBaseEntity> allCharts = Stream.concat(instance.getHelmCharts().stream(),
                                                                        instance.getTerminatedHelmCharts().stream());
            String helmChartState = allCharts
                    .filter(hc -> hc.getReleaseName().equalsIgnoreCase(releaseName))
                    .findFirst()
                    .map(HelmChartBaseEntity::getDeletePvcState)
                    .orElse(null);
            return expectedState.toString().equalsIgnoreCase(helmChartState);
        };
    }

    public Callable<Boolean> helmChartInTempInstanceReachesState(final String releaseName,
                                                                 final String vnfInstanceId,
                                                                 final HelmReleaseState expectedState) {

        return helmChartReachesState(releaseName, vnfInstanceId, expectedState, true);
    }

    public void awaitOperationReachingState(final String lifeCycleOperationId, LifecycleOperationState expectedOperationsState) {
        try {
            await().atMost(90, TimeUnit.SECONDS).until(operationReachesState(lifeCycleOperationId, expectedOperationsState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationsState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }
}
