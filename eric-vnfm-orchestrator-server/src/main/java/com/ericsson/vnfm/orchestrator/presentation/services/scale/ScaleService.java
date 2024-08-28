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

import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ScaleService {

    void validateScaleRequestAndPolicies(VnfInstance vnfInstance, ScaleVnfRequest scaleVnfRequest);

    int getScaleLevel(VnfInstance vnfInstance, String aspectId, int currentScaleLevel,
                  int currentNumOfInstances, int numOfInstances, String targetName);

    void setReplicaParameterForScaleInfo(VnfInstance vnfInstance);

    void setReplicaParameterForScaleRequest(VnfInstance vnfInstance, ScaleVnfRequest scaleVnfRequest);

    Integer getMinReplicasCountFromVduInitialDelta(VnfInstance vnfInstance, String targetName);

    Map<String, Object> getAutoScalingEnabledParameter(HelmChart chart);

    Map<String, Map<String, Integer>> getScaleResourcesFromChart(HelmChart chart);

    Map<String, Map<String, Integer>> getScaleParameters(VnfInstance vnfInstance,
                                                         ScaleVnfRequest scaleVnfRequest);

    String updateResourcesDetails(VnfInstance instance, ScaleVnfRequest scaleVnfRequest);

    List<VnfcScaleInfo> getVnfcScaleInfoList(VnfInstance vnfInstance, ScaleVnfRequest.TypeEnum typeEnum, Integer numberOfStepsAsInt,
                                             String aspectId);

    VnfInstance createTempInstance(VnfInstance instance, ScaleVnfRequest request);

    int getScaleRetryAttempts();

    void removeHelmChartFromTempInstance(VnfInstance currentInstance, VnfInstance tempInstance) throws JsonProcessingException;
}
