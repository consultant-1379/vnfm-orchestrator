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

import java.util.Map;

import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public interface ScaleParametersService {

    Map<String, Map<String, Integer>> getScaleParameters(VnfInstance vnfInstance,
                                                         ScaleVnfRequest scaleVnfRequest);

    int calculateNumberOfInstancesForScale(ScalingAspectDataType scalingAspectDataType,
                                       int currentScaleLevel,
                                       int scaleStep,
                                       ScalingAspectDeltas scaleDelta,
                                       ScaleVnfRequest.TypeEnum scaleOperation);
}
