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
package com.ericsson.vnfm.orchestrator.model.sync;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class AspectScaleDetails {

    private final String aspectId;
    private final List<TargetScaleDetails> targetsScaleDetails;

    public Boolean getAutoscalingEnabled() {
        return targetsScaleDetails.get(0).getAutoscalingEnabled();
    }

    public Integer getScaleLevel() {
        return targetsScaleDetails.get(0).getScaleLevel();
    }
}
