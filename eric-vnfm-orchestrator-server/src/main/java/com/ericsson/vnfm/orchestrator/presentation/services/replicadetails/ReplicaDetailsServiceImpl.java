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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import static com.ericsson.vnfm.orchestrator.utils.Utility.putAll;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReplicaDetailsServiceImpl implements ReplicaDetailsService {

    @Autowired
    private DefaultReplicaDetailsCalculationService defaultReplicaDetailsCalculationService;

    @Autowired
    private ScaleMappingReplicaDetailsCalculationService scaleMappingReplicaDetailsCalculationService;

    @Autowired
    private MappingFileService mappingFileService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Override
    public void setReplicaDetailsToVnfInstance(final String descriptorModel, final VnfInstance vnfInstance) {
        LOGGER.info("Setting replica details to helm chart without scale");
        setReplicaDetails(vnfInstance, descriptorModel,  null);
    }

    @Override
    public void updateAndSetReplicaDetailsToVnfInstance(final String descriptorModel, final VnfInstance vnfInstance,
                                                        final Map<String, Object> valuesYamlMap) {
        LOGGER.info("Setting replica details to helm charts");
        setReplicaDetails(vnfInstance, descriptorModel, valuesYamlMap);
        scaleService.setReplicaParameterForScaleInfo(vnfInstance);
    }

    private void setReplicaDetails(final VnfInstance vnfInstance, final String descriptorModel, final Map<String, Object> valuesYamlMap) {
        mappingFileService
                .getScaleMapFilePathFromDescriptorModel(descriptorModel)
                .map(path -> mappingFileService.getMappingFile(path, vnfInstance))
                .filter(MapUtils::isNotEmpty)
                .ifPresentOrElse(scalingMapping -> setReplicaDetailsFromScalingMappingMap(
                        vnfInstance, scalingMapping, descriptorModel, valuesYamlMap), () -> setDefaultReplicaDetails(vnfInstance));
    }

    private void setReplicaDetailsFromScalingMappingMap(VnfInstance vnfInstance, Map<String, ScaleMapping> scaleMappingMap,
                                                         String descriptorModel, Map<String, Object> valuesYamlMap) {
        final Map<String, ReplicaDetails> replicaDetails = scaleMappingReplicaDetailsCalculationService.calculate(descriptorModel,
                                                                                                                  scaleMappingMap,
                                                                                                                  vnfInstance,
                                                                                                                  valuesYamlMap);
        scaleMappingMap.entrySet().stream()
                .map(entry -> Pair.of(ReplicaDetailsUtility.getTargetChartName(entry.getValue(), descriptorModel),
                                      Map.of(entry.getKey(), replicaDetails.get(entry.getKey()))))
                .forEach(pair -> setReplicaDetailsToHelmCharts(vnfInstance, pair.getKey(), pair.getValue()));
    }

    private void setDefaultReplicaDetails(VnfInstance vnfInstance) {
        final Map<String, ReplicaDetails> replicaDetails = defaultReplicaDetailsCalculationService.calculate(vnfInstance);
        String replicaDetailsAsString = replicaDetailsMapper.getReplicaDetailsAsString(replicaDetails);
        vnfInstance.getHelmCharts().forEach(helmChart -> helmChart.setReplicaDetails(replicaDetailsAsString));
    }

    private void setReplicaDetailsToHelmCharts(VnfInstance vnfInstance, String targetChartName, Map<String, ReplicaDetails> replicaDetails) {
        vnfInstance.getHelmCharts()
                .stream()
                .filter(helmChart -> helmChart.getHelmChartUrl().endsWith(targetChartName))
                .forEach(helmChart -> setReplicaDetailsToHelmChart(helmChart, replicaDetails));
    }

    private void setReplicaDetailsToHelmChart(final HelmChart helmChart, final Map<String, ReplicaDetails> replicaDetailsMap) {
        Map<String, ReplicaDetails> allReplicaDetailsMap = Optional.ofNullable(helmChart.getReplicaDetails())
                .filter(StringUtils::isNotEmpty)
                .map(currentReplicaDetails -> replicaDetailsMapper.getReplicaDetailsFromHelmChart(helmChart))
                .map(currentReplicaDetails -> putAll(replicaDetailsMap, currentReplicaDetails))
                .orElse(replicaDetailsMap);
        String replicaDetailsAsString = replicaDetailsMapper.getReplicaDetailsAsString(allReplicaDetailsMap);
        helmChart.setReplicaDetails(replicaDetailsAsString);
    }
}
