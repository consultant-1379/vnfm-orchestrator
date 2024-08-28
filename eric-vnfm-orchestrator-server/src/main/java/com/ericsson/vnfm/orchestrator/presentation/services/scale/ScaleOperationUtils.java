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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspects;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

public final class ScaleOperationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleOperationUtils.class);

    private ScaleOperationUtils() {}

    public static void copyScaleInfo(VnfInstance currentInstance, VnfInstance tempInstance) {
        List<ScaleInfoEntity> scaleInfoEntity = new ArrayList<>();
        for (ScaleInfoEntity entity : currentInstance.getScaleInfoEntity()) {
            ScaleInfoEntity newEntity = new ScaleInfoEntity();
            BeanUtils.copyProperties(entity, newEntity, "scaleInfoId");
            scaleInfoEntity.add(newEntity);
        }
        tempInstance.setScaleInfoEntity(scaleInfoEntity);
    }

    public static void copyHelmChart(VnfInstance currentInstance, VnfInstance tempInstance) {
        List<HelmChart> charts = new ArrayList<>();
        for (HelmChart chart : currentInstance.getHelmCharts()) {
            HelmChart newChart = new HelmChart();
            BeanUtils.copyProperties(chart, newChart, "id", "state", "retryCount");
            charts.add(newChart);
        }
        tempInstance.setHelmCharts(charts);
    }

    public static int getScaleLevelInTempInstance(ScaleInfoEntity scaleInfo, ScaleVnfRequest request) {
        if (ScaleVnfRequest.TypeEnum.IN.equals(request.getType())) {
            return scaleInfo.getScaleLevel() - request.getNumberOfSteps();
        } else {
            return scaleInfo.getScaleLevel() + request.getNumberOfSteps();
        }
    }

    public static void updateScaleLevel(VnfInstance tempInstance, ScaleVnfRequest request) {
        for (ScaleInfoEntity scaleInfo : tempInstance.getScaleInfoEntity()) {
            if (request.getAspectId().equals(scaleInfo.getAspectId())) {
                int requiredLevel = ScaleOperationUtils.getScaleLevelInTempInstance(scaleInfo, request);
                LOGGER.info("Required scale level to set is {} for aspect id {}", requiredLevel, scaleInfo.getAspectId());
                scaleInfo.setScaleLevel(requiredLevel);
            }
        }
    }

    public static List<VnfcScaleInfo> createScaleInfo(Map<String, Integer> currentReplicaCount,
                                                      Map<String, Integer> requiredReplicaCount) {
        List<VnfcScaleInfo> allScaleInfo = new ArrayList<>();
        if (currentReplicaCount.size() != requiredReplicaCount.size()) {
            throw new InternalRuntimeException("Invalid replica calculation logic for orchestrator");
        }
        for (Map.Entry<String, Integer> currentCount : currentReplicaCount.entrySet()) {
            if (requiredReplicaCount.containsKey(currentCount.getKey())) {
                VnfcScaleInfo scaleInfo = new VnfcScaleInfo();
                scaleInfo.setVnfcName(currentCount.getKey());
                scaleInfo.setCurrentReplicaCount(currentCount.getValue());
                scaleInfo.setExpectedReplicaCount(requiredReplicaCount.get(currentCount.getKey()));
                allScaleInfo.add(scaleInfo);
            } else {
                throw new InternalRuntimeException(currentCount.getKey() + " is not defined in the required " +
                                                           "replica details");
            }
        }
        return allScaleInfo;
    }

    public static boolean isLinearScaling(ScalingAspectDataType scalingAspectDataType) {
        return scalingAspectDataType.getStepDeltas().size() == 1;
    }

    public static Optional<ScalingAspectDataType> getScalingDataType(String aspectId, Policies policies) {
        Map<String, ScalingAspects> allScalingAspects = policies.getAllScalingAspects();
        for (Map.Entry<String, ScalingAspects> scalingAspectName : allScalingAspects.entrySet()) {
            Map<String, ScalingAspectDataType> scalingAspects = scalingAspectName.getValue().getProperties()
                    .getAllAspects();
            if (scalingAspects.containsKey(aspectId)) {
                return Optional.of(scalingAspects.get(aspectId));
            }
        }
        return Optional.empty();
    }
}
