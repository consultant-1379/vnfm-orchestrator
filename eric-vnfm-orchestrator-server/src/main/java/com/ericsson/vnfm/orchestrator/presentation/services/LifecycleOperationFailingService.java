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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.messaging.LifeCycleOperationProcessor;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.routing.Conditions;
import com.ericsson.vnfm.orchestrator.messaging.routing.MessageOperationFactory;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Service
public class LifecycleOperationFailingService {
    @Autowired
    private MessageOperationFactory messageOperationFactory;

    @Autowired
    private MessageUtility utility;

    @Transactional
    public void initFailing(LifecycleOperation operation, String errorMessage) {
        String operationType = operation.getLifecycleOperationType().toString();
        LifeCycleOperationProcessor<HelmReleaseLifecycleMessage> service =
                (LifeCycleOperationProcessor<HelmReleaseLifecycleMessage>)
                        messageOperationFactory.getProcessor(new Conditions(operationType, HelmReleaseLifecycleMessage.class));
        if (service != null) {
            service.rollBack(getTimeOutMessageForRollback(operation, errorMessage));
        } else if (Objects.equals(LifecycleOperationType.SYNC.toString(), operationType) ||
                Objects.equals(LifecycleOperationType.MODIFY_INFO.toString(), operationType)) {
            utility.updateOperation(String.format(errorMessage, operation.getOperationOccurrenceId()), operation, LifecycleOperationState.FAILED);
        }
    }

    private static HelmReleaseLifecycleMessage getTimeOutMessageForRollback(LifecycleOperation operation, String errorMessage) {
        HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setLifecycleOperationId(operation.getOperationOccurrenceId());
        message.setMessage(String.format(errorMessage, operation.getOperationOccurrenceId()));
        return message;
    }
}
