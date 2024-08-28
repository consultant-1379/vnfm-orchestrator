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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.AUTOSCALING_PARAM_CHANGED_FOR_MANO_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.AUTOSCALING_PARAM_MISMATCH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.MAX_REPLICA_COUNT_MISSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.MIN_REPLICA_COUNT_MISMATCH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.MIN_REPLICA_COUNT_MISSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.SCALE_LEVEL_EXCEEDS_MAX_SCALE_LEVEL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.SCALE_LEVEL_MISMATCH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANO_CONTROLLED_SCALING;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.sync.ActualScaleValues;
import com.ericsson.vnfm.orchestrator.model.sync.TargetScaleDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SyncOperationValidator {

    @Autowired
    private VnfInstanceService vnfInstanceService;

    public void validateManoControlledScaling(final Map<String, String> vnfControlledScalingExtension,
                                              final Boolean isManoControlledScaling,
                                              final Boolean newAutoscalingEnabledForTarget,
                                              final String target) {

        if (MapUtils.isEmpty(vnfControlledScalingExtension) && isManoControlledScaling == newAutoscalingEnabledForTarget) {
            throw new SyncValidationException(String.format(AUTOSCALING_PARAM_CHANGED_FOR_MANO_CONTROLLED_SCALING,
                                                            MANO_CONTROLLED_SCALING,
                                                            isManoControlledScaling,
                                                            target,
                                                            newAutoscalingEnabledForTarget));
        }
    }

    public void validateMinReplicaCount(final VnfInstance instance,
                                        final ReplicaDetails replicaDetails,
                                        final ActualScaleValues actualScaleValues,
                                        final String target) {

        final var currentMinReplicaCount = replicaDetails.getMinReplicasCount();
        final var newMinReplicaCount = actualScaleValues.getMinReplicaCount();

        if (newMinReplicaCount == null) {
            throw new SyncValidationException(String.format(MIN_REPLICA_COUNT_MISSING, target));
        }

        if (currentMinReplicaCount != null && !currentMinReplicaCount.equals(newMinReplicaCount)) {
            throw new SyncValidationException(String.format(MIN_REPLICA_COUNT_MISMATCH, target, currentMinReplicaCount, newMinReplicaCount));
        }

        if (currentMinReplicaCount == null) {
            int initialDelta = vnfInstanceService.getInitialDelta(instance, target);

            if (initialDelta != newMinReplicaCount) {
                throw new SyncValidationException(String.format(MIN_REPLICA_COUNT_MISMATCH, target, initialDelta, newMinReplicaCount));
            }
        }
    }

    public void validateMaxReplicaCountPresent(final ActualScaleValues actualScaleValues, final String target) {
        if (actualScaleValues.getMaxReplicaCount() == null) {
            throw new SyncValidationException(String.format(MAX_REPLICA_COUNT_MISSING, target));
        }
    }

    public void validateScaleLevelDoesNotExceedMaxLevel(final VnfInstance instance,
                                                        final String aspectId,
                                                        final String targetName,
                                                        final int newScaleLevel) {

        int maxScaleLevel = vnfInstanceService.getMaxScaleLevel(instance, aspectId);
        if (newScaleLevel > maxScaleLevel) {
            throw new SyncValidationException(String.format(SCALE_LEVEL_EXCEEDS_MAX_SCALE_LEVEL, targetName, newScaleLevel, maxScaleLevel));
        }
    }

    public void validateAutoscalingEnabledIsSame(final List<TargetScaleDetails> targetScaleDetailsForAspect) {
        if (targetScaleDetailsForAspect.isEmpty()) {
            return;
        }

        List<TargetScaleDetails> detailsWithAutoscalingValue = targetScaleDetailsForAspect.stream()
                .filter(details -> details.getAutoscalingEnabled() != null)
                .collect(Collectors.toList());

        final boolean isAutoscalingValueDifferent = detailsWithAutoscalingValue.stream()
                .map(TargetScaleDetails::getAutoscalingEnabled)
                .distinct()
                .count() > 1;

        if (isAutoscalingValueDifferent) {
            logAutoscalingDifference(detailsWithAutoscalingValue);

            String targetNames = detailsWithAutoscalingValue.stream()
                    .map(TargetScaleDetails::getTarget)
                    .collect(Collectors.joining(", "));
            throw new SyncValidationException(String.format(AUTOSCALING_PARAM_MISMATCH, targetNames));
        }
    }

    public void validateScaleLevelIsSame(final List<TargetScaleDetails> targetScaleDetailsForAspect) {
        if (targetScaleDetailsForAspect.isEmpty()) {
            return;
        }

        final int firstTargetValue = targetScaleDetailsForAspect.get(0).getScaleLevel();

        final var validationErrors = targetScaleDetailsForAspect.stream()
                .skip(1)
                .filter(details -> details.getScaleLevel() != firstTargetValue)
                .map(details -> String.format(String.format(SCALE_LEVEL_MISMATCH, details.getTarget(), firstTargetValue, details.getScaleLevel())))
                .collect(Collectors.toList());

        if (!validationErrors.isEmpty()) {
            throw new SyncValidationException(validationErrors);
        }
    }

    private void logAutoscalingDifference(final List<TargetScaleDetails> detailsWithAutoscalingValue) {
        Map<Boolean, String> validationErrors = detailsWithAutoscalingValue
                .stream()
                .collect(groupingBy(TargetScaleDetails::getAutoscalingEnabled,
                                    mapping(TargetScaleDetails::getTarget, joining(", "))));
        String combinedError = validationErrors.entrySet()
                .stream().map(entry -> String.format("autoscaling for %s is %s", entry.getValue(), entry.getKey()))
                .collect(joining("; "));

        LOGGER.warn("Autoscaling divergence: {}", combinedError);
    }
}
