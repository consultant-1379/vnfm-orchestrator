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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public interface VnfInstanceService {

    Policies getPolicies(VnfInstance vnfInstance);

    Map<String, String> getVnfControlledScalingExtension(VnfInstance instance);

    boolean isVnfControlledScalingExtensionPresent(VnfInstance instance);

    int getInitialDelta(VnfInstance vnfInstance, String target);

    int getInitialDelta(Map<String, InitialDelta> allInitialDelta, String target);

    List<ScaleInfoEntity> getCurrentScaleInfo(VnfInstance vnfInstance);

    Optional<ScaleInfoEntity> getScaleInfoForAspect(VnfInstance vnfInstance, String aspectId);

    int getMaxScaleLevel(VnfInstance vnfInstance, String aspectId);

}
