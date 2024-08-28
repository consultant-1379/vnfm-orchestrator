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
package com.ericsson.vnfm.orchestrator.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspects;
import com.ericsson.vnfm.orchestrator.model.InstantiatedVnfInfo;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ScalingUtils {

    public static final int DEFAULT_SCALE_LEVEL = 0;

    public Map<String, ScalingAspectDataType> getScalingDetails(final String policiesAsString, ObjectMapper mapper) {
        Policies policies;
        try {
            policies = mapper.readValue(policiesAsString, Policies.class);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse policies: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
        if (CollectionUtils.isEmpty(policies.getAllInitialDelta())) {
            if (!CollectionUtils.isEmpty(policies.getAllScalingAspects()) || !CollectionUtils.isEmpty(policies.getAllScalingAspectDelta())) {
                LOGGER.warn("Scaling aspect data present in VNFD with only non-scalable VDUs");
            }
            return Collections.emptyMap();
        }
        if (!CollectionUtils.isEmpty(policies.getAllScalingAspects())) {
            return policies.getAllScalingAspects().entrySet().stream().map(ScalingUtils::getAllAspects)
                    .flatMap(map -> map.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            LOGGER.debug("No scaling aspect details present");
            return Collections.emptyMap();
        }
    }

    public InstantiatedVnfInfo mapInstantiatedVnfInfo(final List<ScaleInfoEntity> scaleInfoEntities) {
        if (scaleInfoEntities == null || scaleInfoEntities.isEmpty()) {
            return null;
        }
        List<ScaleInfo> scaleInfos = scaleInfoEntities.stream().map(ScalingUtils::toScaleInfo).collect(Collectors.toList());
        InstantiatedVnfInfo instantiatedVnfInfo = new InstantiatedVnfInfo();
        instantiatedVnfInfo.setScaleStatus(scaleInfos);
        return instantiatedVnfInfo;
    }

    private Map<String, ScalingAspectDataType> getAllAspects(Map.Entry<String, ScalingAspects> stringScalingAspectsEntry) {
        return stringScalingAspectsEntry.getValue().getProperties().getAllAspects();
    }

    public static ScaleInfo toScaleInfo(final ScaleInfoEntity scaleInfoEntity) {
        ScaleInfo scaleInfo = new ScaleInfo();
        scaleInfo.setScaleLevel(scaleInfoEntity.getScaleLevel());
        scaleInfo.setAspectId(scaleInfoEntity.getAspectId());
        return scaleInfo;
    }

    public static ScaleInfoEntity buildScaleInfoWithDefaultScaleLevel(final String aspectId) {
        final ScaleInfoEntity scaleInfo = new ScaleInfoEntity();
        scaleInfo.setAspectId(aspectId);
        scaleInfo.setScaleLevel(DEFAULT_SCALE_LEVEL);
        return scaleInfo;
    }

    public static Stream<? extends String> toAspectIds(Map.Entry<String, ScalingAspects> stringScalingAspectsEntry) {
        return stringScalingAspectsEntry.getValue()
                .getProperties()
                .getAllAspects()
                .keySet()
                .stream();
    }

    public static String getCommentedScaleInfo(final List<ScaleInfoEntity> scaleInfoEntities) {
        StringBuilder scaleLevels = new StringBuilder("\n\n# Aspects and Current Scale level");
        scaleInfoEntities.forEach(scaleInfoEntity -> scaleLevels.append("\n# ")
                .append(scaleInfoEntity.getAspectId())
                .append(": ")
                .append(scaleInfoEntity.getScaleLevel()));
        return scaleLevels.toString();
    }
}
