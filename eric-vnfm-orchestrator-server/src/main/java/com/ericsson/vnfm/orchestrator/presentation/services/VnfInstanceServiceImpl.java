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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleOperationUtils;
import com.google.common.base.Strings;

@Service
public class VnfInstanceServiceImpl implements VnfInstanceService {

    private static final String POLICIES_NOT_PRESENT_ERROR_MESSAGE = "Policies not present for " + "instance %s";

    private static final String SCALE_INFO_MISSING_IN_VNF_INSTANCE =
            "Scale not supported as scale info is not " + "present for vnf instance %s";

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Override
    public Policies getPolicies(final VnfInstance vnfInstance) {
        String policies = vnfInstance.getPolicies();
        if (StringUtils.isBlank(policies)) {
            throw new IllegalArgumentException(String.format(POLICIES_NOT_PRESENT_ERROR_MESSAGE,
                                               vnfInstance.getVnfInstanceId()));
        }
        return replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance);
    }

    @Override
    public Map<String, String> getVnfControlledScalingExtension(final VnfInstance instance) {
        String allExtensionsString = instance.getVnfInfoModifiableAttributesExtensions();
        if (Strings.isNullOrEmpty(allExtensionsString)) {
            return new HashMap<>();
        }

        return extensionsMapper.getVnfControlledScalingValues(allExtensionsString);
    }

    @Override
    public boolean isVnfControlledScalingExtensionPresent(VnfInstance instance) {
        return !MapUtils.isEmpty(getVnfControlledScalingExtension(instance));
    }

    @Override
    public int getInitialDelta(Map<String, InitialDelta> allInitialDelta, String target) {
        Optional<InitialDelta> initialDelta = getInitialDeltaForTarget(target, allInitialDelta);
        return initialDelta.orElseThrow(() -> new IllegalArgumentException(String.format("No initial delta exists for %s.", target)))
                .getProperties().getInitialDelta().getNumberOfInstances();
    }

    @Override
    public int getInitialDelta(VnfInstance vnfInstance, String target) {
        Policies policies = getPolicies(vnfInstance);

        return getInitialDelta(policies.getAllInitialDelta(), target);
    }

    private static Optional<InitialDelta> getInitialDeltaForTarget(String target, Map<String, InitialDelta> allInitialDelta) {
        for (Map.Entry<String, InitialDelta> initialDelta : allInitialDelta.entrySet()) {
            InitialDelta selectedInitialDelta = initialDelta.getValue();
            if (Arrays.asList(selectedInitialDelta.getTargets()).contains(target)) {
                return Optional.of(selectedInitialDelta);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ScaleInfoEntity> getCurrentScaleInfo(final VnfInstance vnfInstance) {
        List<ScaleInfoEntity> scaleInfoEntity = vnfInstance.getScaleInfoEntity();
        if (!(scaleInfoEntity == null || scaleInfoEntity.isEmpty())) {
            return scaleInfoEntity;
        } else {
            throw new IllegalArgumentException(
                    String.format(SCALE_INFO_MISSING_IN_VNF_INSTANCE, vnfInstance.getVnfInstanceId()));
        }
    }

    @Override
    public Optional<ScaleInfoEntity> getScaleInfoForAspect(VnfInstance vnfInstance, String aspectId) {
        List<ScaleInfoEntity> currentScaleLevel = getCurrentScaleInfo(vnfInstance);
        for (ScaleInfoEntity scaleInfo : currentScaleLevel) {
            if (scaleInfo.getAspectId().equals(aspectId)) {
                return Optional.of(scaleInfo);
            }
        }
        return Optional.empty();
    }

    @Override
    public int getMaxScaleLevel(VnfInstance vnfInstance, String aspectId) {
        ScalingAspectDataType scalingAspectDataType = ScaleOperationUtils
                .getScalingDataType(aspectId, getPolicies(vnfInstance))
                .orElseThrow(() -> new IllegalStateException(
                        String.format("ScalingAspect for aspectId: %s was not found", aspectId)));

        return scalingAspectDataType.getMaxScaleLevel();
    }
}
