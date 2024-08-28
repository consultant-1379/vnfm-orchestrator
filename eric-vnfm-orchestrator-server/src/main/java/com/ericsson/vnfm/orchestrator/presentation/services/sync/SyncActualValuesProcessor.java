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

import static java.lang.String.format;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspects;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.sync.ActualScaleValues;
import com.ericsson.vnfm.orchestrator.model.sync.AspectContext;
import com.ericsson.vnfm.orchestrator.model.sync.AspectScaleDetails;
import com.ericsson.vnfm.orchestrator.model.sync.SyncResult;
import com.ericsson.vnfm.orchestrator.model.sync.TargetScaleDetails;
import com.ericsson.vnfm.orchestrator.model.sync.VnfInstanceContext;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncFailedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SyncActualValuesProcessor {

    private static final String SCALE_NOT_SUPPORTED_ERROR_MESSAGE =
            "Scale not supported as policies not present for " + "instance %s";

    @Autowired
    private SyncOperationValidator syncOperationValidator;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private VnfInstanceService vnfInstanceService;

    public SyncResult process(final VnfInstanceContext vnfInstanceContext,
                              final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues) {

        final Map<String, List<String>> targetsByAspectId = targetsByAspectId(vnfInstanceContext.getInstance());

        validateNonScalableTargets(targetsByAspectId,
                                   vnfInstanceContext.getReleaseNameToPerTargetReplicaDetails(),
                                   releaseNameToPerTargetActualValues);

        return validateScalableTargetsAndCreateSyncResult(vnfInstanceContext, targetsByAspectId, releaseNameToPerTargetActualValues);
    }

    private static void validateNonScalableTargets(final Map<String, List<String>> targetsByAspectId,
                                                   final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails,
                                                   final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues) {

        final List<String> validationErrors = NonScalableTargetsValidator.validate(targetsByAspectId,
                                                                                   releaseNameToPerTargetReplicaDetails,
                                                                                   releaseNameToPerTargetActualValues);

        if (!validationErrors.isEmpty()) {
            throw new SyncFailedException(String.join(";", validationErrors), HttpStatus.CONFLICT);
        }
    }

    private SyncResult validateScalableTargetsAndCreateSyncResult(
            final VnfInstanceContext vnfInstanceContext,
            final Map<String, List<String>> targetsByAspectId,
            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues) {

        final List<String> validationErrors = new ArrayList<>();
        final List<AspectScaleDetails> aspectScaleDetails = new ArrayList<>();

        for (final var aspectIdToTargets : targetsByAspectId.entrySet()) {
            final var aspectId = aspectIdToTargets.getKey();
            final var targetsInAspect = aspectIdToTargets.getValue();

            final Optional<Integer> currentAspectScaleLevelOptional = getAspectScaleLevel(vnfInstanceContext.getInstance(), aspectId);
            if (currentAspectScaleLevelOptional.isEmpty()) {
                break;
            }

            final int currentAspectScaleLevel = currentAspectScaleLevelOptional.get();

            final var aspectWithDeps = new AspectContext(aspectId, targetsInAspect, currentAspectScaleLevel);

            try {
                aspectScaleDetails.add(validateAndCreateAspectScaleDetails(vnfInstanceContext, releaseNameToPerTargetActualValues, aspectWithDeps));
            } catch (SyncValidationException e) {
                LOGGER.error("An error occurred during validating scalable targets", e);
                validationErrors.addAll(e.getErrors());
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new SyncFailedException(String.join(";", validationErrors), HttpStatus.CONFLICT);
        }

        return new SyncResult(aspectScaleDetails);
    }

    private AspectScaleDetails validateAndCreateAspectScaleDetails(
            final VnfInstanceContext instanceWithDeps,
            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues,
            final AspectContext aspectContext) {

        final List<TargetScaleDetails> targetScaleDetailsForAspect =
                createTargetScaleDetailsForAspect(instanceWithDeps, releaseNameToPerTargetActualValues, aspectContext);

        syncOperationValidator.validateAutoscalingEnabledIsSame(targetScaleDetailsForAspect);
        syncOperationValidator.validateScaleLevelIsSame(targetScaleDetailsForAspect);

        return new AspectScaleDetails(aspectContext.getAspectId(), targetScaleDetailsForAspect);
    }

    private List<TargetScaleDetails> createTargetScaleDetailsForAspect(
            final VnfInstanceContext instanceWithDeps,
            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues,
            final AspectContext aspectContext) {

        final var targetsInAspect = aspectContext.getTargets();

        final List<TargetScaleDetails> targetScaleDetailsForAspect = new ArrayList<>();
        for (final var target : targetsInAspect) {
            final Optional<String> releaseNameOptional = getReleaseNameByTarget(instanceWithDeps.getReleaseNameToPerTargetReplicaDetails(), target);
            if (releaseNameOptional.isEmpty()) {
                break;
            }

            final var releaseName = releaseNameOptional.get();

            targetScaleDetailsForAspect.add(validateAndCreateTargetScaleDetails(instanceWithDeps,
                                                                                releaseNameToPerTargetActualValues,
                                                                                aspectContext,
                                                                                releaseName,
                                                                                target));
        }

        return targetScaleDetailsForAspect;
    }

    private TargetScaleDetails validateAndCreateTargetScaleDetails(
            final VnfInstanceContext instanceWithDeps,
            final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues,
            final AspectContext aspectContext,
            final String releaseName,
            final String target) {

        final var instance = instanceWithDeps.getInstance();
        final var releaseNameToPerTargetReplicaDetails = instanceWithDeps.getReleaseNameToPerTargetReplicaDetails();
        final var vnfControlledScalingExtension = instanceWithDeps.getVnfControlledScalingExtension();

        final var targetValues = releaseNameToPerTargetActualValues.get(releaseName).get(target);
        final var currentReplicaDetails = releaseNameToPerTargetReplicaDetails.get(releaseName).get(target);

        final var newAutoscalingEnabledForTarget = toBoolean(targetValues.getAutoScalingEnabled());

        syncOperationValidator.validateManoControlledScaling(vnfControlledScalingExtension,
                                                             instance.getManoControlledScaling(),
                                                             newAutoscalingEnabledForTarget,
                                                             target);

        if (newAutoscalingEnabledForTarget) {
            syncOperationValidator.validateMinReplicaCount(instance, currentReplicaDetails, targetValues, target);
            syncOperationValidator.validateMaxReplicaCountPresent(targetValues, target);
        }

        final var newScaleLevelForTarget = calculateNewScaleLevelForTarget(instance,
                                                                           targetValues,
                                                                           aspectContext,
                                                                           target,
                                                                           currentReplicaDetails,
                                                                           newAutoscalingEnabledForTarget);

        syncOperationValidator.validateScaleLevelDoesNotExceedMaxLevel(instance, aspectContext.getAspectId(), target, newScaleLevelForTarget);

        return new TargetScaleDetails(releaseName, target, targetValues, targetValues.getAutoScalingEnabled(), newScaleLevelForTarget);
    }

    private int calculateNewScaleLevelForTarget(final VnfInstance instance,
                                                final ActualScaleValues actualScaleValues,
                                                final AspectContext aspectContext,
                                                final String target,
                                                final ReplicaDetails currentReplicaDetails,
                                                final boolean newAutoscalingEnabledForTarget) {

        final var effectiveNewReplicaCount = newAutoscalingEnabledForTarget
                ? actualScaleValues.getMaxReplicaCount()
                : actualScaleValues.getReplicaCount();

        final var currentMaxReplicaCount = currentReplicaDetails.getMaxReplicasCount();
        final var currentReplicaCount = currentReplicaDetails.getCurrentReplicaCount();

        final var effectiveCurrentReplicaCount = currentMaxReplicaCount != null ? currentMaxReplicaCount : currentReplicaCount;

        final var currentAspectScaleLevel = aspectContext.getCurrentAspectScaleLevel();

        if (effectiveNewReplicaCount != null && !effectiveNewReplicaCount.equals(effectiveCurrentReplicaCount)) {
            try {
                return scaleService.getScaleLevel(instance,
                                                  aspectContext.getAspectId(),
                                                  currentAspectScaleLevel,
                                                  effectiveCurrentReplicaCount,
                                                  effectiveNewReplicaCount,
                                                  target);
            } catch (IllegalArgumentException e) {
                throw new SyncValidationException(String.format("An error occurred during calculate new scale level for target due to: %s",
                                                                e.getMessage()), e);
            }
        }

        return currentAspectScaleLevel;
    }

    private Map<String, List<String>> targetsByAspectId(final VnfInstance vnfInstance) {
        final Map<String, List<String>> targetsByAspectId = new HashMap<>();

        final var policies = getPolicies(vnfInstance);
        if (policies.getAllScalingAspects() == null) {
            return targetsByAspectId;
        }

        for (Map.Entry<String, ScalingAspects> scalingAspects : policies.getAllScalingAspects().entrySet()) {
            for (Map.Entry<String, ScalingAspectDataType> scalingAspectDataType :
                    scalingAspects.getValue().getProperties().getAllAspects().entrySet()) {
                for (Map.Entry<String, ScalingAspectDeltas> scalingAspectDeltas :
                        scalingAspectDataType.getValue().getAllScalingAspectDelta().entrySet()) {
                    final var aspectId = scalingAspectDataType.getKey();
                    final var targets = Arrays.asList(scalingAspectDeltas.getValue().getTargets());
                    targetsByAspectId.put(aspectId, targets);
                }
            }
        }

        return targetsByAspectId;
    }

    private Policies getPolicies(final VnfInstance vnfInstance) {
        try {
            return vnfInstanceService.getPolicies(vnfInstance);
        } catch (IllegalArgumentException exception) {
            String message = format(SCALE_NOT_SUPPORTED_ERROR_MESSAGE, vnfInstance.getVnfInstanceId());
            throw new SyncFailedException(message, HttpStatus.CONFLICT, exception);
        }
    }

    private static Optional<Integer> getAspectScaleLevel(final VnfInstance instance, final String aspectId) {
        return instance.getScaleInfoEntity().stream()
                .filter(entity -> entity.getAspectId().equals(aspectId))
                .findFirst()
                .map(ScaleInfoEntity::getScaleLevel);
    }

    private static Optional<String> getReleaseNameByTarget(final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails,
                                                           final String target) {

        return releaseNameToPerTargetReplicaDetails.entrySet().stream()
                .filter(releaseNameToPerTargetReplicaDetailsEntry -> releaseNameToPerTargetReplicaDetailsEntry.getValue().containsKey(target))
                .findFirst()
                .map(Map.Entry::getKey);
    }
}
