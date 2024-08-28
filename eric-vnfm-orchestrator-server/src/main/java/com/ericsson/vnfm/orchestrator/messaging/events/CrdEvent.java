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
package com.ericsson.vnfm.orchestrator.messaging.events;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.AbstractMessageProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageConverter;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.ChangeVnfPackageOperation;
import com.ericsson.vnfm.orchestrator.messaging.operations.InstantiateOperation;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CrdEvent extends AbstractMessageProcessor {
    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private InstantiateOperation instantiateOperation;

    @Autowired
    private ChangeVnfPackageOperation changeVnfPackageOperation;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private MessageUtility messageUtility;

    @Override
    public Conditions getConditions() {
        return new Conditions(WorkflowServiceEventType.CRD.toString(), WorkflowServiceEventMessage.class);
    }

    @Override
    public void completed(final WorkflowServiceEventMessage message) {
        LOGGER.info("In CRD event completed flow with message : {}", message.getMessage());
        String operationId = message.getLifecycleOperationId();
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(operationId);
        if (lifecycleOperation.getLifecycleOperationType().equals(LifecycleOperationType.INSTANTIATE)) {
            HelmReleaseLifecycleMessage helmMessage = MessageConverter.convertEventToHelmMessage(message, HelmReleaseOperationType.INSTANTIATE);
            instantiateOperation.completed(helmMessage);
        } else {
            HelmReleaseLifecycleMessage helmMessage = MessageConverter.convertEventToHelmMessage(message, HelmReleaseOperationType.CHANGE_VNFPKG);
            changeVnfPackageOperation.completed(helmMessage);
        }
    }

    @Override
    public void failed(final WorkflowServiceEventMessage message) {
        LOGGER.info("In CRD event failed flow with message : {} ", message.getMessage());
        String operationId = message.getLifecycleOperationId();
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(operationId);

        LifecycleOperationState state = LifecycleOperationType.INSTANTIATE.equals(lifecycleOperation.getLifecycleOperationType())
                ? LifecycleOperationState.FAILED : LifecycleOperationState.ROLLED_BACK;

        lifecycleOperation.setOperationState(state);
        setError(message.getMessage(), lifecycleOperation);

        final VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        updateChartState(message.getReleaseName(), vnfInstance, state);
        messageUtility.updateNamespaceDetails(lifecycleOperation);
        if (lifecycleOperation.getLifecycleOperationType() == LifecycleOperationType.CHANGE_VNFPKG) {
            instanceService.updateAssociationBetweenPackageAndVnfInstanceForFailedUpgrade(vnfInstance);
        }

        databaseInteractionService.persistVnfInstanceAndOperation(vnfInstance, lifecycleOperation);
    }

    private static void updateChartState(final String releaseName, final VnfInstance vnfInstance, final LifecycleOperationState operationState) {
        vnfInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), releaseName))
                .findFirst()
                .ifPresent(chart -> chart.setState(operationState.toString()));
    }
}
