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

import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;

public interface ScaleLevelCalculationService {

    int calculateScaleLevelAfterLinearScaling(int numOfInstances,
                                              ScalingAspectDeltas scaleDelta,
                                              String targetName,
                                              int initialDelta);


    int calculateScaleLevelAfterNonLinearScaling(int currentScaleLevel,
                                                 int currentNumOfInstances,
                                                 int numOfInstances,
                                                 ScalingAspectDeltas scaleDelta,
                                                 ScaleVnfRequest.TypeEnum scaleOperation,
                                                 String targetName);

}
