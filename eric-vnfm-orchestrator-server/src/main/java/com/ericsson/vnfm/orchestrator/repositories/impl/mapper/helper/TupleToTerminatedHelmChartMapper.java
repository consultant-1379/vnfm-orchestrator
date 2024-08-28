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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;

import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;

public final class TupleToTerminatedHelmChartMapper {

    private TupleToTerminatedHelmChartMapper() {
    }

    public static <T> TerminatedHelmChart map(Tuple tuple, Join<T, TerminatedHelmChart> join) {
        return TerminatedHelmChart.builder()
                .id(tuple.get(join.get("id")))
                .helmChartName(tuple.get(join.get("helmChartName")))
                .helmChartVersion(tuple.get(join.get("helmChartVersion")))
                .helmChartType(tuple.get(join.get("helmChartType")))
                .helmChartArtifactKey(tuple.get(join.get("helmChartArtifactKey")))
                .helmChartUrl(tuple.get(join.get("helmChartUrl")))
                .priority(tuple.get(join.get("priority")))
                .releaseName(tuple.get(join.get("releaseName")))
                .revisionNumber(tuple.get(join.get("revisionNumber")))
                .state(tuple.get(join.get("state")))
                .retryCount(tuple.get(join.get("retryCount")))
                .deletePvcState(tuple.get(join.get("deletePvcState")))
                .downsizeState(tuple.get(join.get("downsizeState")))
                .replicaDetails(tuple.get(join.get("replicaDetails")))
                .operationOccurrenceId(tuple.get(join.get("operationOccurrenceId")))
                .build();
    }
}
