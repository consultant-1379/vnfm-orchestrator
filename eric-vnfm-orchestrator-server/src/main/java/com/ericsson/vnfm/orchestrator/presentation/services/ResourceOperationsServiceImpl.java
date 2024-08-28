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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ResourceOperationsMapper;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

@Service
public class ResourceOperationsServiceImpl implements ResourceOperationsService {
    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ResourceOperationsMapper resourceOperationsMapper;

    @Override
    public List<OperationDetails> getAllOperationDetails() {
        List<LifecycleOperation> lifecycleOperationList = databaseInteractionService.getAllOperations();
        List<OperationDetails> allOperations = new ArrayList<>();
        for (LifecycleOperation lifecycleOperation : lifecycleOperationList) {
            OperationDetails operationDetails = resourceOperationsMapper.toInternalModel(lifecycleOperation);
            allOperations.add(operationDetails);
        }
        allOperations.sort(Comparator.comparing(OperationDetails::getTimestamp).reversed());
        return allOperations;
    }
}
