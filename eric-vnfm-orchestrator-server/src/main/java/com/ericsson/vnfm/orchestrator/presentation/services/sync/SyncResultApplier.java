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

import static java.util.stream.Collectors.toMap;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.BeanUtils;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.sync.AspectScaleDetails;
import com.ericsson.vnfm.orchestrator.model.sync.SyncResult;
import com.ericsson.vnfm.orchestrator.model.sync.TargetScaleDetails;
import com.ericsson.vnfm.orchestrator.model.sync.VnfInstanceContext;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SyncResultApplier {

    private static final TypeReference<HashMap<String, Integer>> RESOURCE_DETAILS_TYPE_REF = new TypeReference<>() {
    };

    public static void apply(final SyncResult syncResult, final VnfInstanceContext vnfInstanceContext) {
        final var vnfInstance = vnfInstanceContext.getInstance();

        updateReplicaDetailsInCharts(vnfInstance.getHelmCharts(), syncResult, vnfInstanceContext.getReleaseNameToPerTargetReplicaDetails());
        updateScaleInfoEntities(vnfInstance.getScaleInfoEntity(), syncResult);
        vnfInstance.setResourceDetails(convertObjToJsonString(updateResourceDetails(syncResult, vnfInstance.getResourceDetails())));
        updateExtensions(syncResult, vnfInstanceContext, vnfInstance);
    }

    private static void updateExtensions(SyncResult syncResult, VnfInstanceContext vnfInstanceContext, VnfInstance vnfInstance) {
        Object deployableModules = convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(DEPLOYABLE_MODULES);

        if (deployableModules != null) {
            vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(Map.of(
                    VNF_CONTROLLED_SCALING,
                    updateVnfControlledScalingExtension(syncResult, vnfInstanceContext.getVnfControlledScalingExtension()),
                    DEPLOYABLE_MODULES, deployableModules
            )));
        } else {
            vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(Map.of(
                    VNF_CONTROLLED_SCALING,
                    updateVnfControlledScalingExtension(syncResult, vnfInstanceContext.getVnfControlledScalingExtension()))));
        }
    }

    private static void updateReplicaDetailsInCharts(final List<HelmChart> helmCharts,
                                                     final SyncResult syncResult,
                                                     final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails) {

        final Map<String, Map<String, ReplicaDetails>> newReleaseNameToPerTargetReplicaDetails =
                updateReleaseNameToPerTargetReplicaDetails(syncResult, releaseNameToPerTargetReplicaDetails);

        for (final var helmChart : helmCharts) {
            if (newReleaseNameToPerTargetReplicaDetails.containsKey(helmChart.getReleaseName())) {
                helmChart.setReplicaDetails(Utility.convertObjToJsonString(newReleaseNameToPerTargetReplicaDetails.get(helmChart.getReleaseName())));
            }
        }
    }

    private static Map<String, Map<String, ReplicaDetails>> updateReleaseNameToPerTargetReplicaDetails(
            final SyncResult syncResult,
            final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails) {

        final Map<String, Map<String, ReplicaDetails>> newReleaseNameToPerTargetReplicaDetails = new HashMap<>(releaseNameToPerTargetReplicaDetails);
        newReleaseNameToPerTargetReplicaDetails.replaceAll((key, currentValue) -> new HashMap<>(emptyIfNull(currentValue)));

        for (final var currentAspectScaleDetails : syncResult.getAspectScaleDetails()) {
            for (final var targetScaleDetails : currentAspectScaleDetails.getTargetsScaleDetails()) {
                final var replicaDetailsForReleaseName = newReleaseNameToPerTargetReplicaDetails.get(targetScaleDetails.getReleaseName());
                final var target = targetScaleDetails.getTarget();
                final var currentReplicaDetails = replicaDetailsForReleaseName.get(target);
                final var updatedReplicaDetails = updateReplicaDetails(currentReplicaDetails, targetScaleDetails);
                replicaDetailsForReleaseName.put(target, updatedReplicaDetails);
            }
        }

        return newReleaseNameToPerTargetReplicaDetails;
    }

    private static ReplicaDetails updateReplicaDetails(final ReplicaDetails replicaDetails, final TargetScaleDetails targetScaleDetails) {
        final var updatedReplicaDetails = ReplicaDetails.builder().build();
        BeanUtils.copyProperties(replicaDetails, updatedReplicaDetails);

        final var autoscalingEnabled = BooleanUtils.toBoolean(targetScaleDetails.getAutoscalingEnabled());

        updatedReplicaDetails.setCurrentReplicaCount(targetScaleDetails.getActualScaleValues().getReplicaCount());
        updatedReplicaDetails.setMaxReplicasCount(autoscalingEnabled ? targetScaleDetails.getActualScaleValues().getMaxReplicaCount() : null);
        updatedReplicaDetails.setMinReplicasCount(autoscalingEnabled ? targetScaleDetails.getActualScaleValues().getMinReplicaCount() : null);
        updatedReplicaDetails.setAutoScalingEnabledValue(autoscalingEnabled);

        return updatedReplicaDetails;
    }

    private static void updateScaleInfoEntities(final List<ScaleInfoEntity> scaleInfoEntities, final SyncResult syncResult) {
        for (final var scaleInfoEntity : scaleInfoEntities) {
            final var aspectId = scaleInfoEntity.getAspectId();

            syncResult.getAspectScaleDetails().stream()
                    .filter(details -> details.getAspectId().equals(aspectId))
                    .findFirst()
                    .ifPresent(aspectScaleDetails -> scaleInfoEntity.setScaleLevel(aspectScaleDetails.getScaleLevel()));
        }
    }

    private static Map<String, Integer> updateResourceDetails(final SyncResult syncResult, final String resourceDetails) {
        final Map<String, Integer> newResourceDetails = new HashMap<>(getResourceDetails(resourceDetails));
        newResourceDetails.putAll(toResourceDetails(syncResult));

        return newResourceDetails;
    }

    private static Map<String, Integer> getResourceDetails(final String resourceDetails) {
        if (resourceDetails != null) {
            return parseJsonToGenericType(resourceDetails, RESOURCE_DETAILS_TYPE_REF);
        }
        return new HashMap<>();
    }

    private static Map<String, Integer> toResourceDetails(final SyncResult syncResult) {
        return syncResult.getAspectScaleDetails().stream()
                .map(AspectScaleDetails::getTargetsScaleDetails)
                .flatMap(Collection::stream)
                .collect(toMap(
                    TargetScaleDetails::getTarget,
                    targetScaleDetails -> targetScaleDetails.getActualScaleValues().getReplicaCount()));
    }

    private static Map<String, String> updateVnfControlledScalingExtension(final SyncResult syncResult,
                                                                           final Map<String, String> vnfControlledScalingExtension) {
        final Map<String, String> newVnfControlledScalingExtension =
                new HashMap<>(emptyIfNull(vnfControlledScalingExtension));

        if (MapUtils.isEmpty(newVnfControlledScalingExtension)) {
            return newVnfControlledScalingExtension;
        }

        newVnfControlledScalingExtension.putAll(toVnfControlledScalingExtension(syncResult));

        return newVnfControlledScalingExtension;
    }

    private static Map<String, String> toVnfControlledScalingExtension(final SyncResult syncResult) {
        return syncResult.getAspectScaleDetails().stream()
                .collect(toMap(
                    AspectScaleDetails::getAspectId,
                    aspectScaleDetails -> toScalingExtensionValue(BooleanUtils.toBoolean(aspectScaleDetails.getAutoscalingEnabled()))));
    }

    private static String toScalingExtensionValue(final boolean autoscalingEnabled) {
        return autoscalingEnabled ? CISM_CONTROLLED : MANUAL_CONTROLLED;
    }
}
