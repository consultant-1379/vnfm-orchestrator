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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import static com.ericsson.vnfm.orchestrator.model.ConfigFileStatus.NOT_IN_USE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InstantiateLcmOpErrorProcessor extends DefaultLcmOpErrorProcessor {

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Override
    public void process(final LifecycleOperation operation, final HttpStatus status, final String errorTitle, final String errorDetail) {
        super.process(operation, status, errorTitle, errorDetail);
        final VnfInstance vnfInstance = operation.getVnfInstance();
        LOGGER.info("Started deletion VNF instance namespace details");
        databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstance.getVnfInstanceId());

        if (operation.getOperationState().equals(LifecycleOperationState.ROLLED_BACK) && vnfInstance.getClusterName() != null) {
            deleteAssociationBetweenClusterAndVnfInstance(vnfInstance);
        }
    }

    public void deleteAssociationBetweenClusterAndVnfInstance(final VnfInstance vnfInstance) {
        String clusterName = vnfInstance.getClusterName();
        vnfInstance.setClusterName(null);
        vnfInstance.setNamespace(null);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        LOGGER.info("LCM operation is in ROLLED_BACK state, updating cluster config file status to NOT_IN_USE");
        clusterConfigService
                .changeClusterConfigFileStatus(clusterName, vnfInstance, NOT_IN_USE);
    }

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.INSTANTIATE;
    }
}
