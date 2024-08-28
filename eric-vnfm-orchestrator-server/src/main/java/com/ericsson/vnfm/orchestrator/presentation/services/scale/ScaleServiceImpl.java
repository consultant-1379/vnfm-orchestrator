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

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static org.slf4j.LoggerFactory.getLogger;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

@Service
public class ScaleServiceImpl implements ScaleService {

    private static final String ASPECT_SCALING_DATA_MISSING_ERROR = "No scaling data type exists for aspect %s.";
    private static final String TARGET_SCALING_DATA_MISSING_ERROR = "No scaling delta exists for target %s.";
    private static final String SCALE_ASPECT_NOT_DEFINED_ERROR_MESSAGE = "Scaling Aspect %s not defined for instance %s";
    private static final String INVALID_REPLICA_DETAILS = "Unable to convert replica details";
    private static final String RESOURCES_DETAILS_NOT_UPDATED_DURING_INSTANTIATION_UPGRADE_ERROR = "Scale cannot be performed, as Resources details "
            + "not updated during instantiation/upgrade";
    private static final String INVALID_RESOURCES_DETAILS_PROVIDED_DURING_INSTANTIATION = "Invalid resources details provided during instantiation";
    private static final String SCALING_DELTA_FOR_ASPECT_NOT_EXIST = "Unable to find scaling delta for aspect ";
    private static final String SCALE_INFO_NOT_PRESENT_FOR_INSTANCE = "Scale info not present for instance ";

    private static final Logger LOGGER = getLogger(ScaleServiceImpl.class);
    private static final TypeReference<Map<String, Integer>> RESOURCE_DETAILS_TYPE_REF = new TypeReference<>() {
    };

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Value("${retry.scaleRetryAttempts}")
    private String scaleRetryAttempts;

    @Autowired
    private MappingFileService mappingFileService;

    @Autowired
    private VnfInstanceService vnfInstanceService;

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private ScaleParametersService scaleParametersService;

    @Autowired
    private ScaleLevelCalculationService scaleLevelCalculationService;



    @Override
    public void validateScaleRequestAndPolicies(VnfInstance vnfInstance, ScaleVnfRequest scaleVnfRequest) {
        if (StringUtils.isBlank(vnfInstance.getPolicies())) {
            throw new IllegalArgumentException("Scale not supported as policies not present for instance "
                                                       + vnfInstance.getVnfInstanceId());
        }
        Optional<ScaleInfoEntity> scaleInfoEntity = vnfInstanceService.getScaleInfoForAspect(vnfInstance, scaleVnfRequest.getAspectId());
        if (scaleInfoEntity.isEmpty()) {
            throw new IllegalArgumentException(String.format("Scale for aspect id %s is not supported", scaleVnfRequest.getAspectId()));
        }
        Optional<ScalingAspectDataType> scalingAspect = getScalingDataType(scaleVnfRequest.getAspectId(), vnfInstance);
        if (scalingAspect.isEmpty()) {
            throw new IllegalArgumentException(
                    "Scaling Aspect not defined for instance " + vnfInstance.getVnfInstanceId());
        }
        if (CollectionUtils.isEmpty(scalingAspect.get().getStepDeltas())) {
            throw new IllegalArgumentException(
                    "Scaling Aspect " + scaleVnfRequest.getAspectId() + " should at least define one step delta, " +
                            "Invalid VNFD");
        }

        ScaleLevelValidator.validate(scalingAspect.get(), scaleInfoEntity.get(), scaleVnfRequest);
    }

    @Override
    public int getScaleLevel(VnfInstance vnfInstance, String aspectId, int currentScaleLevel,
                             int currentNumOfInstances, int numOfInstances, String targetName) {
        ScalingAspectDataType scalingAspectDataType = getScalingDataType(aspectId, vnfInstance)
                .orElseThrow(() -> new IllegalArgumentException(String.format(ASPECT_SCALING_DATA_MISSING_ERROR, aspectId)));
        Map.Entry<String, ScalingAspectDeltas> deltasEntry = scalingAspectDataType.getAllScalingAspectDelta().entrySet().stream()
                .filter(delta -> Arrays.asList(delta.getValue().getTargets()).contains(targetName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format(TARGET_SCALING_DATA_MISSING_ERROR, targetName)));
        ScaleVnfRequest.TypeEnum scaleType;
        if (currentNumOfInstances < numOfInstances) {
            scaleType = ScaleVnfRequest.TypeEnum.OUT;
        } else {
            scaleType = ScaleVnfRequest.TypeEnum.IN;
        }
        return getScaleLevelForSync(scalingAspectDataType, currentScaleLevel, currentNumOfInstances, numOfInstances,
                                    deltasEntry.getValue(), scaleType, targetName);
    }

    @Override
    public void setReplicaParameterForScaleInfo(VnfInstance vnfInstance) {
        resetReplicaCountToInitialDelta(vnfInstance);
        List<ScaleInfoEntity> allScaleInfoEntity = vnfInstance.getScaleInfoEntity();
        Map<String, Integer> allScaleInfo = new HashMap<>();
        for (ScaleInfoEntity scaleInfo : allScaleInfoEntity) {
            if (scaleInfo.getScaleLevel() != 0) {
                allScaleInfo.put(scaleInfo.getAspectId(), scaleInfo.getScaleLevel());
                scaleInfo.setScaleLevel(0);
            }
        }
        for (Map.Entry<String, Integer> aspectLevel : allScaleInfo.entrySet()) {
            ScaleVnfRequest request = new ScaleVnfRequest();
            request.setType(ScaleVnfRequest.TypeEnum.OUT);
            request.setAspectId(aspectLevel.getKey());
            request.setNumberOfSteps(aspectLevel.getValue());
            setReplicaParameterForScaleRequest(vnfInstance, request);
            ScaleOperationUtils.updateScaleLevel(vnfInstance, request);
        }
    }

    @Override
    public void setReplicaParameterForScaleRequest(VnfInstance vnfInstance, ScaleVnfRequest scaleVnfRequest) {
        final var adjustmentsPerTarget = calculateInstancesAdjustment(vnfInstance, scaleVnfRequest);

        final var replicaParametersForAllCharts = replicaDetailsMapper.getReplicaDetailsForAllCharts(vnfInstance.getHelmCharts());
        adjustNumberOfInstancesInReplicaDetails(replicaParametersForAllCharts, adjustmentsPerTarget);
        setReplicaDetailsInChart(vnfInstance.getHelmCharts(), replicaParametersForAllCharts);

        final var isMappingFilePresent = mappingFileService.isMappingFilePresent(vnfInstance.getVnfPackageId());
        final var isVnfControlledScalingExtensionPresent = vnfInstanceService.isVnfControlledScalingExtensionPresent(vnfInstance);

        if (!isMappingFilePresent && !isVnfControlledScalingExtensionPresent) {
            disableAutomaticScalingForAllHelmChart(vnfInstance);
        } else if (!isMappingFilePresent) {
            setMaxReplicaIfMappingFileNotPresentAndExtensionPresent(vnfInstance);
        }
    }

    @Override
    public Integer getMinReplicasCountFromVduInitialDelta(final VnfInstance vnfInstance, final String targetName) {
        var vnfInstancePolicies = vnfInstanceService.getPolicies(vnfInstance);
        Integer initialDelta = null;
        for (Map.Entry<String, InitialDelta> allInitialDelta : vnfInstancePolicies.getAllInitialDelta().entrySet()) {
            if (Arrays.asList(allInitialDelta.getValue().getTargets()).contains(targetName)) {
                initialDelta = allInitialDelta.getValue().getProperties().getInitialDelta().getNumberOfInstances();
            }
        }
        return initialDelta;
    }

    @Override
    public Map<String, Object> getAutoScalingEnabledParameter(HelmChart chart) {
        Map<String, Object> additionalParameter = new HashMap<>();
        Optional<String> replicas = Optional.ofNullable(chart.getReplicaDetails());
        replicas.ifPresent(replicaList -> {
            try {
                Map<String, ReplicaDetails> allReplicaDetails = mapper.readValue(
                        chart.getReplicaDetails(), new TypeReference<>() {
                        });
                for (Map.Entry<String, ReplicaDetails> replicaDetails : allReplicaDetails.entrySet()) {
                    ReplicaDetails replica = replicaDetails.getValue();
                    if (replica.getAutoScalingEnabledValue() != null && !Strings.isNullOrEmpty(
                            replica.getAutoScalingEnabledParameterName())) {
                        additionalParameter.put(replica.getAutoScalingEnabledParameterName(),
                                                replica.getAutoScalingEnabledValue());
                    }
                }
            } catch (JsonProcessingException jpe) {
                throw new IllegalArgumentException(INVALID_REPLICA_DETAILS, jpe);
            }
        });
        return additionalParameter;
    }

    @Override
    public Map<String, Map<String, Integer>> getScaleResourcesFromChart(HelmChart chart) {
        Map<String, Map<String, Integer>> allReplicaParameter = new HashMap<>();
        Map<String, ReplicaDetails> allReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(chart);
        for (Map.Entry<String, ReplicaDetails> replica : allReplicaDetails.entrySet()) {
            ReplicaDetails details = replica.getValue();
            Map<String, Integer> specificTargetReplicaDetails = new HashMap<>();
            specificTargetReplicaDetails.put(details.getScalingParameterName(), details.getCurrentReplicaCount());
            if (!Strings.isNullOrEmpty(details.getMaxReplicasParameterName()) && details.getMaxReplicasCount() != null) {
                specificTargetReplicaDetails.put(details.getMaxReplicasParameterName(), details.getMaxReplicasCount());
            }
            if (!Strings.isNullOrEmpty(details.getMinReplicasParameterName()) && details.getMinReplicasCount() != null) {
                specificTargetReplicaDetails.put(details.getMinReplicasParameterName(), details.getMinReplicasCount());
            }
            allReplicaParameter.put(replica.getKey(), specificTargetReplicaDetails);
        }
        return allReplicaParameter;
    }

    @Override
    public Map<String, Map<String, Integer>> getScaleParameters(VnfInstance vnfInstance,
                                                                ScaleVnfRequest scaleVnfRequest) {
        return scaleParametersService.getScaleParameters(vnfInstance, scaleVnfRequest);
    }

    @Override
    public String updateResourcesDetails(final VnfInstance instance, final ScaleVnfRequest scaleVnfRequest) {
        final var resourcesDetails = instance.getResourceDetails();
        if (Strings.isNullOrEmpty(resourcesDetails)) {
            throw new IllegalArgumentException(RESOURCES_DETAILS_NOT_UPDATED_DURING_INSTANTIATION_UPGRADE_ERROR);
        }

        final var adjustmentsPerTarget = calculateInstancesAdjustment(instance, scaleVnfRequest);

        try {
            final var currentResourceDetails = mapper.readValue(resourcesDetails, RESOURCE_DETAILS_TYPE_REF);
            final var adjustedResourceDetails = adjustInstancesInResourceDetails(currentResourceDetails, adjustmentsPerTarget);

            return mapper.writeValueAsString(adjustedResourceDetails);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(INVALID_RESOURCES_DETAILS_PROVIDED_DURING_INSTANTIATION, ex);
        }
    }

    @Override
    public List<VnfcScaleInfo> getVnfcScaleInfoList(VnfInstance vnfInstance, ScaleVnfRequest.TypeEnum typeEnum, Integer numberOfStepsAsInt,
                                                    String aspectId) {

        if (vnfInstance.getInstantiationState() == NOT_INSTANTIATED) {
            throw new NotInstantiatedException(vnfInstance);
        }

        ScaleVnfRequest scaleVnfRequest = new ScaleVnfRequest();
        scaleVnfRequest.setType(typeEnum);
        scaleVnfRequest.setNumberOfSteps(numberOfStepsAsInt);
        scaleVnfRequest.setAspectId(aspectId);

        VnfInstance tempInstance = createTempInstance(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentReplicaCount = replicaDetailsMapper.getReplicaDetailsForTarget(vnfInstance);
        Map<String, Integer> requiredReplicaCount = replicaDetailsMapper.getReplicaDetailsForTarget(tempInstance);
        return ScaleOperationUtils.createScaleInfo(currentReplicaCount, requiredReplicaCount);
    }

    @Override
    public VnfInstance createTempInstance(VnfInstance instance, ScaleVnfRequest request) {
        VnfInstance tempInstance = new VnfInstance();
        BeanUtils.copyProperties(instance, tempInstance, "helmCharts", "scaleInfoEntity");
        ScaleOperationUtils.copyScaleInfo(instance, tempInstance);
        ScaleOperationUtils.copyHelmChart(instance, tempInstance);
        setReplicaParameterForScaleRequest(tempInstance, request);
        String resourcesDetails = updateResourcesDetails(instance, request);
        LOGGER.info("Calculated resource details is {}", resourcesDetails);
        tempInstance.setResourceDetails(resourcesDetails);
        ScaleOperationUtils.updateScaleLevel(tempInstance, request);
        tempInstance.setTempInstance(null);
        instance.setTempInstance(convertObjToJsonString(tempInstance));
        return tempInstance;
    }

    @Override
    public int getScaleRetryAttempts() {
        try {
            return Integer.parseInt(scaleRetryAttempts);
        } catch (NumberFormatException nfe) {
            LOGGER.debug("Could not parse scaleRetryAttempts: '{}' into a number. Returning default of 2 retry attempts instead", scaleRetryAttempts);
            return 2;
        }
    }

    private int getScaleLevelForSync(ScalingAspectDataType scalingAspectDataType, int currentScaleLevel,
                                     int currentNumOfInstances, int numOfInstances, ScalingAspectDeltas scaleDelta,
                                     ScaleVnfRequest.TypeEnum scaleOperation, final String targetName) {

        int initialDelta = vnfInstanceService.getInitialDelta(scaleDelta.getAllInitialDelta(), targetName);
        if (ScaleOperationUtils.isLinearScaling(scalingAspectDataType)) {
            return scaleLevelCalculationService.calculateScaleLevelAfterLinearScaling(numOfInstances, scaleDelta, targetName, initialDelta);
        } else {
            return scaleLevelCalculationService.calculateScaleLevelAfterNonLinearScaling(currentScaleLevel, currentNumOfInstances, numOfInstances,
                                                                                         scaleDelta,
                                                            scaleOperation, targetName);
        }
    }

    private void resetReplicaCountToInitialDelta(VnfInstance vnfInstance) {
        Map<String, Integer> initialReplicaCount = replicaCountCalculationService.calculateFromVduInitialDelta(vnfInstance);
        if (CollectionUtils.isEmpty(initialReplicaCount)) {
            return;
        }
        vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .map(chart -> replicaDetailsMapper.getReplicaDetailsFromHelmChart(chart))
                .forEach(allReplicaDetails -> resetReplicaDetailsToInitialDelta(initialReplicaCount, allReplicaDetails));
    }

    private void resetReplicaDetailsToInitialDelta(Map<String, Integer> initialReplicaCount,
                                                         Map<String, ReplicaDetails> allReplicaDetails) {
        allReplicaDetails.forEach((key, details) -> {
            if (initialReplicaCount.containsKey(key)) {
                int numberOfInstance = initialReplicaCount.get(key);
                setReplicaDetails(details, numberOfInstance);
            }
        });
    }

    private static void setReplicaDetails(ReplicaDetails details, int numberOfInstance) {
        details.setCurrentReplicaCount(numberOfInstance);
        if (BooleanUtils.isTrue(details.getAutoScalingEnabledValue())) {
            details.setMaxReplicasCount(numberOfInstance);
        }
    }

    private void setMaxReplicaIfMappingFileNotPresentAndExtensionPresent(VnfInstance instance) {
        instance.getHelmCharts().stream().filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> !Strings.isNullOrEmpty(chart.getReplicaDetails())).forEach(chart -> {
                    Map<String, ReplicaDetails> allReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(chart);
                    allReplicaDetails.replaceAll((key, value) -> {
                        if (value.getAutoScalingEnabledValue() != null && value.getAutoScalingEnabledValue()) {
                            value.setMaxReplicasCount(value.getCurrentReplicaCount());
                            value.setMinReplicasCount(getMinReplicasCountFromVduInitialDelta(instance, key));
                        } else if (value.getAutoScalingEnabledValue() != null) {
                            value.setMaxReplicasCount(value.getCurrentReplicaCount());
                            value.setMinReplicasCount(value.getCurrentReplicaCount());
                        }
                        return value;
                    });
                    chart.setReplicaDetails(replicaDetailsMapper.getReplicaDetailsAsString(allReplicaDetails));
                });
    }

    private void disableAutomaticScalingForAllHelmChart(VnfInstance instance) {
        instance.getHelmCharts().stream().filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> !Strings.isNullOrEmpty(chart.getReplicaDetails())).forEach(chart -> {
                    Map<String, ReplicaDetails> allReplicaDetails = replicaDetailsMapper
                            .getReplicaDetailsFromHelmChart(chart);
                    allReplicaDetails.replaceAll((key, value) -> {
                        value.setAutoScalingEnabledValue(false);
                        if (!Strings.isNullOrEmpty(value.getMaxReplicasParameterName())) {
                            value.setMaxReplicasCount(value.getCurrentReplicaCount());
                        }
                        if (!Strings.isNullOrEmpty(value.getMinReplicasParameterName())) {
                            value.setMinReplicasCount(value.getCurrentReplicaCount());
                        }
                        return value;
                    });
                    chart.setReplicaDetails(replicaDetailsMapper.getReplicaDetailsAsString(allReplicaDetails));
                });
    }

    private void adjustNumberOfInstancesInReplicaDetails(Map<String, Map<String, ReplicaDetails>> replicaParametersForAllCharts,
                                                         Map<String, Integer> adjustmentsPerTarget) {

        final Map<String, List<ReplicaDetails>> targetToReplicaDetails = replicaDetailsMapper.targetToReplicaDetails(replicaParametersForAllCharts);

        for (final var adjustment : adjustmentsPerTarget.entrySet()) {
            final var target = adjustment.getKey();
            final var numberAdjustment = adjustment.getValue();

            if (targetToReplicaDetails.containsKey(target)) {
                calculateAndSetReplicaCount(targetToReplicaDetails.get(target), numberAdjustment);
            }
        }
    }

    private static void calculateAndSetReplicaCount(final List<ReplicaDetails> replicaDetailsList, final Integer numberAdjustment) {
        for (final var replicaDetails : replicaDetailsList) {
            final var newReplicaCount = replicaDetails.getCurrentReplicaCount() + numberAdjustment;

            setReplicaDetails(replicaDetails, newReplicaCount);
        }
    }

    private void setReplicaDetailsInChart(List<HelmChart> allHelmChart, Map<String, Map<String, ReplicaDetails>>
            replicaParametersForAllCharts) {
        for (HelmChart chart : allHelmChart) {
            Map<String, ReplicaDetails> replicaDetails = replicaParametersForAllCharts.get(chart.getReleaseName());
            chart.setReplicaDetails(replicaDetailsMapper.getReplicaDetailsAsString(replicaDetails));
        }
    }

    private Map<String, Integer> calculateInstancesAdjustment(VnfInstance instance,
                                                              ScaleVnfRequest scaleVnfRequest) {

        final var aspectId = scaleVnfRequest.getAspectId();

        final var scalingAspectDataType = getScalingDataType(aspectId, instance)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(SCALE_ASPECT_NOT_DEFINED_ERROR_MESSAGE, aspectId, instance.getVnfInstanceId())));
        if (scalingAspectDataType.getAllScalingAspectDelta() == null) {
            throw new IllegalArgumentException(SCALING_DELTA_FOR_ASPECT_NOT_EXIST + aspectId);
        }

        final var scaleInfo = vnfInstanceService.getScaleInfoForAspect(instance, aspectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        SCALE_INFO_NOT_PRESENT_FOR_INSTANCE + instance.getVnfInstanceId()));

        return calculateAdjustmentsForAspect(scalingAspectDataType, scaleInfo, scaleVnfRequest);
    }

    private Map<String, Integer> calculateAdjustmentsForAspect(ScalingAspectDataType scalingAspectDataType,
                                                               ScaleInfoEntity scaleInfo,
                                                               ScaleVnfRequest scaleVnfRequest) {

        ScaleLevelValidator.validate(scalingAspectDataType, scaleInfo, scaleVnfRequest);

        return scalingAspectDataType.getAllScalingAspectDelta().values().stream()
                .map(scaleDelta -> calculateAdjustmentsForAspectDelta(scaleDelta, scalingAspectDataType, scaleInfo, scaleVnfRequest))
                .flatMap(adjustments -> adjustments.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Integer> calculateAdjustmentsForAspectDelta(ScalingAspectDeltas aspectDelta,
                                                                           ScalingAspectDataType scalingAspectDataType,
                                                                           ScaleInfoEntity scaleInfo,
                                                                           ScaleVnfRequest scaleVnfRequest) {

        final var numberOfInstances = scaleParametersService.calculateNumberOfInstancesForScale(
                scalingAspectDataType, scaleInfo.getScaleLevel(), scaleVnfRequest.getNumberOfSteps(), aspectDelta, scaleVnfRequest.getType());
        final var adjustment = ScaleVnfRequest.TypeEnum.OUT.equals(scaleVnfRequest.getType()) ? numberOfInstances : -numberOfInstances;

        return targetsWithAdjustment(aspectDelta, adjustment);
    }

    private static Map<String, Integer> targetsWithAdjustment(ScalingAspectDeltas aspectDelta, int adjustment) {
        return stream(aspectDelta.getTargets())
                .collect(toMap(identity(), target -> adjustment));
    }

    private static Map<String, Integer> adjustInstancesInResourceDetails(Map<String, Integer> currentResourceDetails,
                                                                         Map<String, Integer> adjustmentsPerTarget) {

        final var updatedResourceDetails = new HashMap<>(currentResourceDetails);

        for (final var adjustmentEntry : adjustmentsPerTarget.entrySet()) {
            final var target = adjustmentEntry.getKey();
            final var numberAdjustment = adjustmentEntry.getValue();

            LOGGER.info("Number of instance to be added {} for target {}", numberAdjustment, target);
            updatedResourceDetails.put(target, updatedResourceDetails.get(target) + numberAdjustment);
        }

        return updatedResourceDetails;
    }

    private Optional<ScalingAspectDataType> getScalingDataType(String aspectId, VnfInstance vnfInstance) {
        Policies policies = vnfInstanceService.getPolicies(vnfInstance);
        return ScaleOperationUtils.getScalingDataType(aspectId, policies);
    }

    public void removeHelmChartFromTempInstance(VnfInstance currentInstance, VnfInstance tempInstance) throws JsonProcessingException {
        List<HelmChart> currentHelmChart = currentInstance.getHelmCharts();
        List<HelmChart> tempInstanceHelmChart = tempInstance.getHelmCharts();
        List<HelmChart> chartsToRemove = new ArrayList<>();
        for (HelmChart tempChart : tempInstanceHelmChart) {
            if (isCrdChartOrChartWithoutReplicaDetails(tempChart)) {
                chartsToRemove.add(tempChart);
                continue;
            }
            for (HelmChart currentChart : currentHelmChart) {
                chartsToRemove.addAll(getChartIfMatchFound(tempChart, currentChart));
            }
        }
        if (!CollectionUtils.isEmpty(chartsToRemove)) {
            tempInstanceHelmChart.removeAll(chartsToRemove);
        }
    }

    private List<HelmChart> getChartIfMatchFound(HelmChart tempChart,
                                                 HelmChart currentChart) throws JsonProcessingException {
        List<HelmChart> allChartsToRemove = new ArrayList<>();
        if (isCrdChartOrChartWithoutReplicaDetails(currentChart)) {
            return allChartsToRemove;
        }
        if (currentChart.getHelmChartUrl().equals(tempChart.getHelmChartUrl())) {
            TypeReference<Map<String, ReplicaDetails>> typeReference = new TypeReference<>() {
            };
            Map<String, ReplicaDetails> currentReplicaDetails = mapper
                    .readValue(currentChart.getReplicaDetails(), typeReference);
            Map<String, ReplicaDetails> tempReplicaDetails = mapper
                    .readValue(tempChart.getReplicaDetails(), typeReference);
            if (currentReplicaDetails.equals(tempReplicaDetails)) {
                allChartsToRemove.add(tempChart);
            }
        }
        return allChartsToRemove;
    }

    private static boolean isCrdChartOrChartWithoutReplicaDetails(HelmChart chart) {
        return HelmChartType.CRD == chart.getHelmChartType() || StringUtils.isEmpty(chart.getReplicaDetails()) ||
                StringUtils.equals("{}", chart.getReplicaDetails());
    }
}
