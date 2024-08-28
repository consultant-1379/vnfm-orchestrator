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
package com.ericsson.vnfm.orchestrator.messaging.handlers.scale;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpdateScaleInfo extends MessageHandler<HelmReleaseLifecycleMessage> {

    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling update Scale Info");
        LifecycleOperation operation = context.getOperation();
        VnfInstance instance = context.getVnfInstance();
        VnfInstance scaledInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        instance.setCombinedValuesFile(scaledInstance.getCombinedValuesFile());
        instance.setCombinedAdditionalParams(scaledInstance.getCombinedAdditionalParams());
        instance.setResourceDetails(scaledInstance.getResourceDetails());
        MessageUtility.updateScaleInfoInInstance(instance, scaledInstance.getScaleInfoEntity());
        MessageUtility.updateHelmChartInInstanceAfterScale(instance, scaledInstance.getHelmCharts());
        updateOperationState(operation, LifecycleOperationState.COMPLETED);
        OperationsUtils.mergeFieldsFromVnfInstanceToLifeCycleOperation(instance, operation);
        helmChartHistoryService.createAndPersistHistoryRecords(instance.getHelmCharts(), operation.getOperationOccurrenceId());
        passToSuccessor(getSuccessor(), context);
    }
}
