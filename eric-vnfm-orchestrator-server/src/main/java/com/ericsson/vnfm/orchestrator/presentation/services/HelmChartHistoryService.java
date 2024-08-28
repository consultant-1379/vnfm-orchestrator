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

import java.util.List;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;

/**
 * Service for operating with helm chart history records used to store information
 * about helm charts states at the moment of completion of lifecycle operation.
 */
public interface HelmChartHistoryService {

    /**
     * Creates and persists new helm chart history records for given operation and charts.
     *
     * @param helmCharts list of helm charts to be recorded
     * @param operationId operation occurrence id to identify operation
     */
    void createAndPersistHistoryRecords(List<HelmChart> helmCharts, String operationId);

    /**
     * Retrieves helm chart history records for given operation by its id.
     *
     * @param operationId operation occurrence id to search for records
     * @return HelmChartHistoryRecords associated with given operation id
     */
    List<HelmChartHistoryRecord> getHelmChartHistoryRecordsByOperationId(String operationId);
}
