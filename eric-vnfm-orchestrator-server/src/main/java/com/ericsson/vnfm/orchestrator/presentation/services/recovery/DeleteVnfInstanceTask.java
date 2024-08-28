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
package com.ericsson.vnfm.orchestrator.presentation.services.recovery;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteVnfInstanceTask extends TaskProcessor {

    private DatabaseInteractionService databaseInteractionService;

    private Task task;

    protected DeleteVnfInstanceTask(final Optional<TaskProcessor> nextProcessor, Task task,
                                    DatabaseInteractionService databaseInteractionService) {
        super(nextProcessor);
        this.task = task;
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    public void execute() {
        String vnfInstanceId = task.getVnfInstanceId();

        LOGGER.info(VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME, "Delete VNF Instance", vnfInstanceId);

        try {
            VnfInstance vnfInstance = getVnfInstance(vnfInstanceId);

            if (vnfInstance != null) {
                databaseInteractionService.checkLifecycleInProgress(vnfInstanceId);

                if (!vnfInstance.getInstantiationState().equals(InstantiationState.NOT_INSTANTIATED)) {
                    throw new IllegalStateException(String.format(
                            "Conflicting resource state - VNF instance resource for VnfInstanceId %s is currently NOT in the NOT_INSTANTIATED state.",
                            vnfInstanceId));
                }

                databaseInteractionService.deleteVnfInstance(vnfInstance);
            }
            databaseInteractionService.deleteAllClusterConfigInstancesByInstanceId(vnfInstanceId);

            databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstanceId);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Delete VNF Instance", vnfInstanceId);

        } catch (Exception e) {
            LOGGER.error("Error occurred during Delete VnfInstance Task", e);
        } finally {
            databaseInteractionService.deleteTask(task);
            nextProcessor.ifPresent(TaskProcessor::execute);
        }
    }

    private VnfInstance getVnfInstance(String vnfInstanceId) {
        try {
            return databaseInteractionService.getVnfInstance(vnfInstanceId);
        } catch (NotFoundException e) {
            LOGGER.info("VnfInstance with id {} is not found", vnfInstanceId, e);

            return null;
        }
    }
}
