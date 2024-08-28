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

import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getCnfChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getFullNameOfChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getFullNameOfChartWithPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getHelmChartByPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getHelmChartWithHighestPriorityByDeployableModulesSupported;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertMapToYamlFormat;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoMap;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.writeMapToValuesFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.AdditionalArtifactModel;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ValuesFileService {

    private static final Pattern ESCAPED_DOT_DELIMITER_PATTERN = Pattern.compile("\\.");
    private static final String PATH_TO_ADDITIONAL_VALUES = "Definitions/OtherTemplates/";

    @Autowired
    private ValuesFileComposer valuesFileComposer;
    @Autowired
    private ReplicaDetailsService replicaDetailsService;
    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;
    @Autowired
    private AdditionalAttributesHelper additionalAttributesHelper;
    @Autowired
    private PackageService packageService;

    public Path getCombinedAdditionalValuesFile(final VnfInstance instance) {
        Map<String, Object> mergedParams = additionalAttributesHelper.getCombinedAdditionalValuesMap(instance);
        if (CollectionUtils.isEmpty(mergedParams)) {
            return null;
        }
        return writeMapToValuesFile(mergedParams);
    }

    public void updateValuesYamlMapWithReplicaDetailsAndHighestPriority(Map<String, Object> valuesYamlMap, VnfInstance vnfInstance) {
        LOGGER.info("Updating values map with replica details");
        VnfInstance tempInstance = getTempInstance(vnfInstance);
        if (tempInstance != null && !CollectionUtils.isEmpty(tempInstance.getHelmCharts())) {
            HelmChart helmChart = getHelmChartWithHighestPriorityByDeployableModulesSupported(vnfInstance);
            updateValuesMapWithReplicaDetailsFromTempInstance(valuesYamlMap, tempInstance, helmChart.getPriority());
        }
    }

    private static VnfInstance getTempInstance(VnfInstance vnfInstance) {
        if (!Strings.isNullOrEmpty(vnfInstance.getTempInstance())) {
            return Utility.parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        }
        return null;
    }

    public void updateValuesMapWithReplicaDetailsFromTempInstance(Map<String, Object> valuesYamlMap,
                                                                  VnfInstance tempInstance,
                                                                  Integer priority) {
        HelmChart helmChart = getHelmChartForVnfByPriority(tempInstance, priority);

        LOGGER.info("Helm chart to fetch replica details {}", helmChart);
        updateValuesYamlMapWithReplicaDetails(valuesYamlMap, helmChart);
    }

    public void updateValuesMapWithReplicaDetailsFromTempInstance(Map<String, Object> valuesYamlMap, HelmChart helmChart) {
        LOGGER.info("Helm chart to fetch replica details {}", helmChart);
        updateValuesYamlMapWithReplicaDetails(valuesYamlMap, helmChart);
    }

    public void valuesFileWithVduInterfaceParam(Map<String, Object> valuesYamlMap, Map<String, Object> vduParams) {
        if (!CollectionUtils.isEmpty(vduParams)) {
            LOGGER.info("Vdu params to inserted to values file {}", vduParams);
            mergeValuesYamlMap(valuesYamlMap, vduParams);
        }
    }

    public Map<String, Object> getReplicaParameter(HelmChart helmChart) {
        Map<String, Object> replicaDetails = new HashMap<>();
        Map<String, ReplicaDetails> allReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(helmChart);
        if (!CollectionUtils.isEmpty(allReplicaDetails)) {
            for (ReplicaDetails replica : allReplicaDetails.values()) {
                setMaxAndMinReplicaIfPresent(replicaDetails, replica);
            }
        }
        return removeDotDelimitersFromReplicaMap(replicaDetails);
    }

    private static void setMaxAndMinReplicaIfPresent(Map<String, Object> replicaDetails, ReplicaDetails replica) {
        replicaDetails.put(replica.getScalingParameterName(), replica.getCurrentReplicaCount());
        boolean autoScalingEnabled = com.ericsson.vnfm.orchestrator.utils.BooleanUtils.getBooleanValue(replica.getAutoScalingEnabledValue());
        if (!Strings.isNullOrEmpty(replica.getMaxReplicasParameterName()) && replica.getMaxReplicasCount() != null) {
            replicaDetails.put(replica.getMaxReplicasParameterName(), replica.getMaxReplicasCount());
        }
        if (!Strings.isNullOrEmpty(replica.getMinReplicasParameterName()) && replica.getMinReplicasCount() != null) {
            replicaDetails.put(replica.getMinReplicasParameterName(), replica.getMinReplicasCount());
        }

        if (!Strings.isNullOrEmpty(replica.getAutoScalingEnabledParameterName())) {
            replicaDetails.put(replica.getAutoScalingEnabledParameterName(), autoScalingEnabled);
        }
    }

    private static Map<String, Object> removeDotDelimitersFromReplicaMap(Map<String, Object> replicaDetails) {
        if (replicaDetails.isEmpty()) {
            return replicaDetails;
        } else {
            return YamlUtility.removeDotDelimitersFromYamlMap(replicaDetails);
        }
    }

    public void updateValuesYamlMapWithReplicaDetails(Map<String, Object> valuesYamlMap, HelmChart helmChart) {
        Map<String, Object> replicaDetails = getReplicaParameter(helmChart);
        if (!CollectionUtils.isEmpty(replicaDetails)) {
            LOGGER.info("Replica details to inserted to values file {}", replicaDetails);
            mergeValuesYamlMap(valuesYamlMap, replicaDetails);
        }
    }

    public void mergeValuesYamlMap(final Map<String, Object> valuesYamlMap, final Map<String, Object> additionalParams) {
        convertKeysToString(valuesYamlMap);
        convertKeysToString(additionalParams);
        String composedValuesContent = valuesFileComposer.compose(convertMapToYamlFormat(valuesYamlMap), additionalParams);
        convertYamlStringAndSetToValuesYamlMap(valuesYamlMap, composedValuesContent);
    }

    private static void convertKeysToString(final Map<String, Object> valuesYamlMap) {
        Map<String, Object> tempMap = new HashMap<>();
        for (Map.Entry<?, Object> entry : valuesYamlMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                convertKeysToString((Map<String, Object>) value);
            }
            if (key instanceof Number) {
                key = String.valueOf(key);
            }
            tempMap.put((String) key, value);
        }

        valuesYamlMap.clear();
        valuesYamlMap.putAll(tempMap);
    }

    public static void convertYamlStringAndSetToValuesYamlMap(final Map<String, Object> valuesYamlMap, final String yamlContent) {
        Map<String, Object> tempMap = convertYamlStringIntoMap(yamlContent);

        valuesYamlMap.clear();
        valuesYamlMap.putAll(tempMap);
    }

    public static <T> T getPropertyFromValuesYamlMap(Map<String, Object> valuesMap, String propertyNameWithDelimiters, Class<T> mapTo) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] keyChain = ESCAPED_DOT_DELIMITER_PATTERN.split(propertyNameWithDelimiters);
            Map<String, Object> nestedMap = YamlUtility.extractNestedMap(valuesMap, keyChain, keyChain.length - 1);
            if (nestedMap != null) {
                return mapper.convertValue(nestedMap.get(keyChain[keyChain.length - 1]), mapTo);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract property from values.yaml", e);
        }
        return null;
    }

    public static Map<String, String> getPropertiesFromValuesYamlMap(Map<String, Object> valuesMap, List<String> propertiesNameWithDelimiters) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> mapPropertiesValues = new HashMap<>();

        try {
            for (String property : propertiesNameWithDelimiters) {
                String[] keyChain = ESCAPED_DOT_DELIMITER_PATTERN.split(property);
                Map<String, Object> nestedMap = YamlUtility.extractNestedMap(valuesMap, keyChain, keyChain.length - 1);

                if (nestedMap == null) {
                    return Collections.emptyMap();
                }
                String value = mapper.convertValue(nestedMap.get(keyChain[keyChain.length - 1]), String.class);
                if (value != null) {
                    mapPropertiesValues.put(property, value);
                }
            }
            return mapPropertiesValues;
        } catch (Exception e) {
            LOGGER.warn("Failed to extract property from values.yaml", e);
        }
        return Collections.emptyMap();
    }

    public Path getChartSpecificValues(VnfInstance tempInstance,
                                       Integer priority) {
        String helmChartFullName = getChartFullName(tempInstance, priority);

        PackageResponse vnfPackageInfo = packageService.getPackageInfo(tempInstance.getVnfDescriptorId());
        Optional<List<AdditionalArtifactModel>> additionalArtifacts = Optional.ofNullable(vnfPackageInfo.getAdditionalArtifacts());

        Optional<String> artifactPath = additionalArtifacts.orElse(List.of()).stream()
                .map(AdditionalArtifactModel::getArtifactPath)
                .filter(path -> path.contains(PATH_TO_ADDITIONAL_VALUES + helmChartFullName + ".yaml"))
                .findFirst();

        return artifactPath.flatMap(path -> packageService.getPackageArtifacts(tempInstance.getVnfPackageId(), path)
                .map(YamlUtility::writeStringToValuesFile)).orElse(null);
    }

    public Path getChartSpecificValues(VnfInstance tempInstance,
                                       HelmChart chart) {
        String helmChartFullName = String.format("%s-%s", chart.getHelmChartName(), chart.getHelmChartVersion());

        PackageResponse vnfPackageInfo = packageService.getPackageInfo(tempInstance.getVnfDescriptorId());
        Optional<List<AdditionalArtifactModel>> additionalArtifacts = Optional.ofNullable(vnfPackageInfo.getAdditionalArtifacts());

        Optional<String> artifactPath = additionalArtifacts.orElse(List.of()).stream()
                .map(AdditionalArtifactModel::getArtifactPath)
                .filter(path -> path.contains(PATH_TO_ADDITIONAL_VALUES + helmChartFullName + ".yaml"))
                .findFirst();

        return artifactPath.flatMap(path -> packageService.getPackageArtifacts(tempInstance.getVnfPackageId(), path)
                .map(YamlUtility::writeStringToValuesFile)).orElse(null);
    }

    private static HelmChart getHelmChartForVnfByPriority(VnfInstance tempInstance, Integer priority) {
        return priority == null
                ? getCnfChartWithHighestPriority(tempInstance)
                : getHelmChartByPriority(tempInstance, priority);
    }

    private static String getChartFullName(VnfInstance tempInstance, Integer priority) {
        return priority == null
                ? getFullNameOfChartWithHighestPriority(tempInstance)
                : getFullNameOfChartWithPriority(tempInstance, priority);
    }
}
