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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;

@Service
public class HelmChartHistoryServiceImpl implements HelmChartHistoryService {

    @Autowired
    private HelmChartHistoryRepository helmChartHistoryRepository;

    @Override
    @Transactional
    public void createAndPersistHistoryRecords(List<HelmChart> helmCharts, String operationId) {
        helmCharts.stream().map(h -> new HelmChartHistoryRecord(h, operationId))
                .forEach(helmChartHistoryRepository::save);
    }

    @Override
    @Transactional
    public List<HelmChartHistoryRecord> getHelmChartHistoryRecordsByOperationId(String operationId) {
        return helmChartHistoryRepository.findAllByLifecycleOperationIdOrderByPriorityAsc(operationId);
    }
}
