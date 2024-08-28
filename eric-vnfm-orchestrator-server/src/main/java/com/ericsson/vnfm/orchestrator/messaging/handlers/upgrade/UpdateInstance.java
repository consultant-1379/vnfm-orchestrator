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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpdateInstance extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final InstanceService instanceService;
    private final MessageUtility utility;
    private final HelmChartHistoryService helmChartHistoryService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling update instance");
        HelmReleaseLifecycleMessage message = context.getMessage();
        LifecycleOperation lifecycleOperation = context.getOperation();
        final VnfInstance instance = context.getVnfInstance();
        VnfInstance upgradedInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        upgradedInstance = utility.updateChartStateAndRevisionNumber(upgradedInstance,
                                                                     message.getReleaseName(),
                                                                     LifecycleOperationState.COMPLETED.toString(),
                                                                     message.getRevisionNumber());

        LOGGER.info("All charts have been processed");

        if (LifecycleOperationType.MODIFY_INFO.equals(lifecycleOperation.getLifecycleOperationType())) {
            utility.updateInstanceWithHelmCharts(upgradedInstance, instance);
        }

        utility.updateUnusedInstance(instance, upgradedInstance.getVnfPackageId());
        instanceService.removeScaleEntriesFromInstance(instance);
        utility.updateScaleInfo(instance, upgradedInstance);
        utility.updateReplicaDetails(instance, upgradedInstance);


        utility.updateInstanceOnChangeVnfPackageOperation(instance,
                                                          upgradedInstance,
                                                          lifecycleOperation,
                                                          LifecycleOperationState.COMPLETED);

        if (lifecycleOperation.getOperationState().equals(LifecycleOperationState.ROLLING_BACK)) {
            OperationsUtils.updateCompletedChangeVnfPackageOperation(upgradedInstance,
                                                                     lifecycleOperation,
                                                                     LifecycleOperationState.ROLLED_BACK);
        } else {
            OperationsUtils.updateCompletedChangeVnfPackageOperation(upgradedInstance,
                                                                     lifecycleOperation,
                                                                     LifecycleOperationState.COMPLETED);
        }

        helmChartHistoryService.createAndPersistHistoryRecords(upgradedInstance.getHelmCharts(), lifecycleOperation.getOperationOccurrenceId());

        context.setVnfInstance(upgradedInstance);

        passToSuccessor(getSuccessor(), context);
    }
}
