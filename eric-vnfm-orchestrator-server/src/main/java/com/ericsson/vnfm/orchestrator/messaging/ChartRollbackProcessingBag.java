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
package com.ericsson.vnfm.orchestrator.messaging;

import com.ericsson.vnfm.orchestrator.messaging.operations.ChartRollbackType;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChartRollbackProcessingBag {
    private ChartRollbackType type;
    private HelmChart helmChart;

    public ChartRollbackType getType() {
        return type;
    }

    public HelmChart getHelmChart() {
        return helmChart;
    }
}
