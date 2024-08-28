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

import java.time.LocalDateTime;
import java.util.List;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChangeOperationContextBuilderTestCommon {

    protected static final String TARGET_VNFD_ID = "i3def1ce-4cf4-477c-aab3-21c454e6targetvnfd";
    protected static final String SOURCE_VNFD_ID = "i3def1ce-4cf4-477c-aab3-21c454e6sourcevnfd";

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected VnfInstance buildVnfInstance(LifecycleOperationState helmChartDownsizeState) throws JsonProcessingException {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("i3def1ce-4cf4-477c-aab3-21c454e6a400");
        vnfInstance.setVnfDescriptorId(SOURCE_VNFD_ID);

        VnfInstance tempVnfInstance = new VnfInstance();
        tempVnfInstance.setVnfInstanceId("i3def1ce-4cf4-477c-aab3-21c454e6a400");
        tempVnfInstance.setVnfDescriptorId(TARGET_VNFD_ID);
        tempVnfInstance.setOperationOccurrenceId("i3def1ce-4cf4-477c-aab3-21c454e6a500");
        String tempVnfInstanceAsString = objectMapper.writeValueAsString(tempVnfInstance);

        vnfInstance.setTempInstance(tempVnfInstanceAsString);

        HelmChart helmChart = new HelmChart();
        helmChart.setDownsizeState(helmChartDownsizeState.name());
        vnfInstance.setHelmCharts(List.of(helmChart));

        return vnfInstance;
    }

    protected LifecycleOperation buildLifecycleOperation(boolean isAutoRollbackEnabled) {
        LifecycleOperation operation = new LifecycleOperation();

        operation.setOperationOccurrenceId("i3def1ce-4cf4-477c-aab3-21c454e6a500");
        operation.setExpiredApplicationTime(LocalDateTime.now().plusMinutes(5));
        operation.setAutoRollbackAllowed(isAutoRollbackEnabled);

        return operation;
    }
}
