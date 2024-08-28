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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import static org.apache.commons.collections4.MapUtils.isEmpty;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.sanitizeWorkflowError;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.sync.ActualScaleValues;
import com.ericsson.vnfm.orchestrator.model.sync.VnfInstanceContext;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncFailedException;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ChartValuesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActualScaleValuesRetriever {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private ObjectMapper mapper;

    public Map<String, Map<String, ActualScaleValues>> retrievePerTargetActualValuesByReleaseName(final VnfInstanceContext vnfInstanceContext) {
        final var vnfInstance = vnfInstanceContext.getInstance();
        final Map<String, Map<String, ReplicaDetails>> releaseNameToPerTargetReplicaDetails =
                vnfInstanceContext.getReleaseNameToPerTargetReplicaDetails();

        final Map<String, Map<String, ActualScaleValues>> releaseNameToPerTargetActualValues = new HashMap<>();

        vnfInstance.getHelmCharts().stream().filter(helmChart -> !helmChart.getHelmChartType().equals(HelmChartType.CRD)).forEach(helmChart -> {
            final var releaseName = helmChart.getReleaseName();
            final Map<String, ReplicaDetails> chartPerTargetReplicaDetails = releaseNameToPerTargetReplicaDetails.get(releaseName);
            if (MapUtils.isEmpty(chartPerTargetReplicaDetails)) {
                LOGGER.warn("No replica details for chart {}", helmChart.getHelmChartName());
                return;
            }
            final Map<String, ActualScaleValues> chartPerTargetActualValues =
                    getActualChartValuesFromWfs(vnfInstance, releaseName, chartPerTargetReplicaDetails);
            releaseNameToPerTargetActualValues.put(releaseName, chartPerTargetActualValues);
        });

        return releaseNameToPerTargetActualValues;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ActualScaleValues> getActualChartValuesFromWfs(final VnfInstance vnfInstance,
                                                                       final String releaseName,
                                                                       final Map<String, ReplicaDetails> chartReplicaDetails) {

        final ResponseEntity<Map> response = executeRequestToWfs(vnfInstance, releaseName);

        final var chartValuesResponse = new ChartValuesResponse(response.getBody());

        return parseChartValuesResponse(chartValuesResponse, chartReplicaDetails);
    }

    private ResponseEntity<Map> executeRequestToWfs(final VnfInstance vnfInstance, final String releaseName) {
        try {
            return workflowRoutingService.getChartValuesRequest(vnfInstance, releaseName);
        } catch (final RestClientResponseException e) {
            throw new SyncFailedException(sanitizeWorkflowError(e.getResponseBodyAsString()),
                                          getWorkflowErrorStatus(e.getRawStatusCode()),  e);
        } catch (final RestClientException e) {
            throw new SyncFailedException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, e);
        }
    }

    private static HttpStatus getWorkflowErrorStatus(final int statusCode) {
        return HttpStatus.valueOf(statusCode).is4xxClientError()
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.SERVICE_UNAVAILABLE;
    }

    private static Map<String, ActualScaleValues> parseChartValuesResponse(final ChartValuesResponse chartValuesResponse,
                                                                           final Map<String, ReplicaDetails> chartPerTargetReplicaDetails) {

        return chartPerTargetReplicaDetails.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toTargetValues(chartValuesResponse.getValues(), entry.getValue())));
    }

    private static ActualScaleValues toTargetValues(final Map<String, Object> chartValues, final ReplicaDetails replicaDetails) {
        if (isEmpty(chartValues)) {
            return ActualScaleValues.empty();
        }

        final var parameterNameResolver = new ParameterNameResolver(replicaDetails);

        return ActualScaleValues.create((Integer) chartValues.get(parameterNameResolver.getScalingParameterName()),
                                        (Boolean) chartValues.get(parameterNameResolver.getAutoScalingEnabledParameterName()),
                                        (Integer) chartValues.get(parameterNameResolver.getMinReplicasParameterName()),
                                        (Integer) chartValues.get(parameterNameResolver.getMaxReplicasParameterName()));
    }
}
