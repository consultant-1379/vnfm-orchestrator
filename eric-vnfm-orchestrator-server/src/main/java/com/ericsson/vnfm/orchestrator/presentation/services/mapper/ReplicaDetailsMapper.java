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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.REPLICA_DETAILS_MAP_TYPE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RetrieveDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ReplicaDetailsMapper {

    private static final String INVALID_SCALE_PARAMETERS = "Invalid format of policies stored in db for instance id %s";

    @Autowired
    private ObjectMapper mapper;

    public Map<String, Map<String, ReplicaDetails>> getReplicaDetailsForAllCharts(List<HelmChart> allHelmChart) {
        Map<String, Map<String, ReplicaDetails>> allReplicaDetails = new HashMap<>();
        for (HelmChart helmChart : allHelmChart) {
            Map<String, ReplicaDetails> replicaDetails = getReplicaDetailsFromHelmChart(helmChart);
            allReplicaDetails.put(helmChart.getReleaseName(), replicaDetails);
        }
        return allReplicaDetails;
    }

    public Policies getPoliciesFromVnfInstance(final VnfInstance vnfInstance) {
        try {
            return mapper.readValue(vnfInstance.getPolicies(), Policies.class);
        } catch (JsonProcessingException e) {
            throw new RetrieveDataException(String.format(INVALID_SCALE_PARAMETERS, vnfInstance.getVnfInstanceId()), e);
        }
    }

    public Map<String, ReplicaDetails> getReplicaDetailsFromHelmChart(final HelmChart helmChart) {
        Map<String, ReplicaDetails> replicaDetails = new HashMap<>();
        if (isCnfChartWithReplicaDetails(helmChart)) {
            try {
                replicaDetails = mapper.readValue(helmChart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE);
            } catch (JsonProcessingException e) {
                throw new RetrieveDataException("Could not get replicaDetails from JSON", e);
            }
        }
        return replicaDetails;
    }

    public Map<String, Integer> getReplicaCountFromHelmCharts(List<HelmChart> helmCharts) {
        return helmCharts.stream()
                .filter(h -> HelmChartType.CNF == h.getHelmChartType() && h.isChartEnabled())
                .flatMap(helmChart -> getReplicaDetailsFromHelmChart(helmChart).entrySet().stream())
                .filter(entry -> Objects.nonNull(entry.getValue().getCurrentReplicaCount()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCurrentReplicaCount()));
    }

    public Set<String> getScalableVdusNames(VnfInstance vnfInstance) {
        Policies policies = getPoliciesFromVnfInstance(vnfInstance);
        return policies.getAllScalingAspectDelta().values().stream()
                .map(ScalingAspectDeltas::getTargets)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());
    }

    private static boolean isCnfChartWithReplicaDetails(HelmChart helmChart) {
        return (helmChart.getHelmChartType() == null || HelmChartType.CNF == helmChart.getHelmChartType())
                && StringUtils.isNotEmpty(helmChart.getReplicaDetails()) && !StringUtils.equals("{}", helmChart.getReplicaDetails());
    }

    public String getReplicaDetailsAsString(final Map<String, ReplicaDetails> replicaDetailsMap) {
        String replicaDetailsAsString;
        try {
            replicaDetailsAsString = mapper.writeValueAsString(replicaDetailsMap);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse replica details", e);
        }
        return replicaDetailsAsString;
    }

    public Map<String, Object> getReplicaDetailsFromHelmHistory(HelmChartHistoryRecord helmChartHistoryRecord) {
        String replicaDetailsString = helmChartHistoryRecord.getReplicaDetails();
        Map<String, ReplicaDetails> allReplicaDetails = new HashMap<>();
        if (!StringUtils.isEmpty(replicaDetailsString)) {
            try {
                allReplicaDetails = mapper.readValue(replicaDetailsString, REPLICA_DETAILS_MAP_TYPE);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Could not get replicaDetails from JSON", e);
                return new HashMap<>();
            }
        }
        Map<String, Object> replicaDetails = new HashMap<>();
        allReplicaDetails.values().forEach(details -> {
            replicaDetails.put(details.getScalingParameterName(), details.getCurrentReplicaCount());
            if (details.getAutoScalingEnabledValue() != null && details.getAutoScalingEnabledValue()
                    && !Strings.isNullOrEmpty(details.getMaxReplicasParameterName())
                    && details.getMaxReplicasCount() != null) {
                replicaDetails.put(details.getMaxReplicasParameterName(), details.getMaxReplicasCount());
            }
            if (details.getAutoScalingEnabledValue() != null && details.getAutoScalingEnabledValue()
                    && !Strings.isNullOrEmpty(details.getMinReplicasParameterName())
                    && details.getMinReplicasCount() != null) {
                replicaDetails.put(details.getMinReplicasParameterName(), details.getMinReplicasCount());
            }
        });
        return replicaDetails;
    }

    public Map<String, List<ReplicaDetails>> targetToReplicaDetails(Map<String, Map<String, ReplicaDetails>> replicaParametersForAllCharts) {
        return replicaParametersForAllCharts.values().stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }

    public Map<String, Integer> getReplicaDetailsForTarget(VnfInstance vnfInstance) {
        Map<String, Integer> replicaDetailsForTarget = new HashMap<>();
        for (HelmChart chart : vnfInstance.getHelmCharts()) {
            Map<String, ReplicaDetails> allReplicaDetails = getReplicaDetailsFromHelmChart(chart);
            for (Map.Entry<String, ReplicaDetails> replica : allReplicaDetails.entrySet()) {
                replicaDetailsForTarget.put(replica.getKey(), replica.getValue().getCurrentReplicaCount());
            }
        }
        return replicaDetailsForTarget;
    }
}
