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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.SCALE_DISABLED_FOR_NON_SCALABLE_CHART;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.SCALE_DISABLED_FOR_NON_SCALABLE_CHART_CANNOT_ENABLE_AUTOSCALING;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.sync.ActualScaleValues;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class NonScalableTargetsValidator {

    public static List<String> validate(final Map<String, List<String>> targetsByAspectId,
                                        final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails,
                                        final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues) {

        final List<String> validationErrors = new ArrayList<>();

        final List<String> targetsNotAssociatedWithAspectId =
                retrieveTargetsNotAssociatedWithAspectId(targetsByAspectId, releaseNameToPerTargetActualValues);

        for (final Map.Entry<String, Map<String, ActualScaleValues>> releaseTargets : releaseNameToPerTargetActualValues.entrySet()) {
            for (final Map.Entry<String, ActualScaleValues> values : releaseTargets.getValue().entrySet()) {
                if (targetsNotAssociatedWithAspectId.contains(values.getKey())) {
                    ReplicaDetails replicaDetails = releaseNameToPerTargetReplicaDetails.get(releaseTargets.getKey()).get(values.getKey());
                    ActualScaleValues actualScaleValues = values.getValue();
                    updateValidationErrors(validationErrors, values, replicaDetails, actualScaleValues);
                }
            }
        }

        return validationErrors;
    }

    private static void updateValidationErrors(final List<String> validationErrors,
                                  final Map.Entry<String, ActualScaleValues> values,
                                  final ReplicaDetails replicaDetails,
                                  final ActualScaleValues actualScaleValues) {
        if (!replicaDetails.getCurrentReplicaCount().equals(actualScaleValues.getReplicaCount())) {
            validationErrors.add(String.format(SCALE_DISABLED_FOR_NON_SCALABLE_CHART, values.getKey()));
        }
        if (actualScaleValues.getMaxReplicaCount() != null) {
            validationErrors.add(String.format(SCALE_DISABLED_FOR_NON_SCALABLE_CHART, values.getKey()));
        }
        if (actualScaleValues.getMinReplicaCount() != null) {
            validationErrors.add(String.format(SCALE_DISABLED_FOR_NON_SCALABLE_CHART, values.getKey()));
        }
        if (actualScaleValues.getAutoScalingEnabled() != null && actualScaleValues.getAutoScalingEnabled()) {
            validationErrors.add(String.format(SCALE_DISABLED_FOR_NON_SCALABLE_CHART_CANNOT_ENABLE_AUTOSCALING, values.getKey()));
        }
    }

    private static List<String> retrieveTargetsNotAssociatedWithAspectId(
            final Map<String, List<String>> targetsByAspectId,
            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues) {

        final List<String> targetsNotAssociatedWithAspectId = new ArrayList<>();
        for (final Map.Entry<String, Map<String, ActualScaleValues>> releaseTargets : releaseNameToPerTargetActualValues.entrySet()) {
            for (final Map.Entry<String, ActualScaleValues> values : releaseTargets.getValue().entrySet()) {
                final AtomicBoolean isTargetAssociatedWithAspect = new AtomicBoolean(false);
                targetsByAspectId.forEach((aspect, targets) -> {
                    if (targets.contains(values.getKey())) {
                        isTargetAssociatedWithAspect.set(true);
                    }
                });

                if (!isTargetAssociatedWithAspect.get()) {
                    targetsNotAssociatedWithAspectId.add(values.getKey());
                }
            }
        }

        return targetsNotAssociatedWithAspectId;
    }
}
