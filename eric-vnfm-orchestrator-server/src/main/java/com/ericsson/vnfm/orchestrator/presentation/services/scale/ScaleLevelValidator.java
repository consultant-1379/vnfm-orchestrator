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
package com.ericsson.vnfm.orchestrator.presentation.services.scale;

import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;

public final class ScaleLevelValidator {

    private static final String REQUIRED_SCALE_LEVEL_EXCEEDS_MAX_LIMIT_SCALE_LEVEL = "Scale Out operation cannot be "
            + "performed for aspect Id %s because required scale level %s exceeds max scale level %s and current "
            + "scale level is %s";
    private static final String REQUIRED_SCALE_LEVEL_EXCEEDS_MIN_LIMIT_SCALE_LEVEL = "Scale In operation cannot be "
            + "performed for aspect Id %s because required scale level %s exceeds min scale level 0 and current "
            + "scale level is %s";

    private ScaleLevelValidator() { }

    public static void validate(ScalingAspectDataType scalingAspectDataType,
                         ScaleInfoEntity scaleInfo,
                         ScaleVnfRequest scaleVnfRequest) {

        if (scaleVnfRequest.getType().equals(ScaleVnfRequest.TypeEnum.IN)) {
            validateRequiredScaleInLevel(scaleInfo, scaleVnfRequest.getNumberOfSteps());
        } else {
            validateRequiredScaleOutLevel(scaleInfo, scalingAspectDataType.getMaxScaleLevel(), scaleVnfRequest.getNumberOfSteps());
        }
    }

    private static void validateRequiredScaleInLevel(ScaleInfoEntity currentScaleInfoEntity, int inputScaleLevel) {
        int requiredScaleLevel = currentScaleInfoEntity.getScaleLevel() - inputScaleLevel;
        if (requiredScaleLevel < 0) {
            throw new IllegalArgumentException(String.format(REQUIRED_SCALE_LEVEL_EXCEEDS_MIN_LIMIT_SCALE_LEVEL,
                                                             currentScaleInfoEntity.getAspectId(),
                                                             inputScaleLevel,
                                                             currentScaleInfoEntity.getScaleLevel()));
        }
    }

    private static void validateRequiredScaleOutLevel(ScaleInfoEntity currentScaleInfoEntity,
                                               int maxScaleLevel,
                                               int inputScaleLevel) {

        int requiredScaleLevel = currentScaleInfoEntity.getScaleLevel() + inputScaleLevel;
        if (requiredScaleLevel > maxScaleLevel) {
            throw new IllegalArgumentException(String.format(REQUIRED_SCALE_LEVEL_EXCEEDS_MAX_LIMIT_SCALE_LEVEL,
                                                             currentScaleInfoEntity.getAspectId(),
                                                             inputScaleLevel,
                                                             maxScaleLevel,
                                                             currentScaleInfoEntity.getScaleLevel()));
        }
    }
}
