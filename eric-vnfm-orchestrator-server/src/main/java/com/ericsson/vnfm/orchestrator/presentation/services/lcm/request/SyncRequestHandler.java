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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.SyncVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.sync.SyncService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SyncRequestHandler extends LifecycleRequestHandler {

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private SyncService syncService;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.SYNC;
    }

    @Override
    public void specificValidation(VnfInstance vnfInstance, Object request) {
        final SyncVnfRequest syncVnfRequest = (SyncVnfRequest) request;
        instantiateVnfRequestValidatingService.validateTimeouts((Map<?, ?>) syncVnfRequest.getAdditionalParams());

        super.commonValidation(vnfInstance, InstantiationState.NOT_INSTANTIATED, LCMOperationsEnum.SYNC);
    }

    @Override
    public void updateInstance(VnfInstance vnfInstance, Object request, final LifecycleOperationType type,
                               LifecycleOperation operation, final Map<String, Object> additionalParams) {

        // no need to update instance
    }

    @Override
    public void sendRequest(VnfInstance vnfInstance, LifecycleOperation operation,
                            Object request, Path toValuesFile) {
        syncService.execute(vnfInstance, operation);
    }

    @Override
    public void persistOperationAndInstanceAfterExecution(VnfInstance vnfInstance, LifecycleOperation operation) {
        // Since SYNC is asynchronous operation, vnf instance and operation are already being saved to database
    }
}
