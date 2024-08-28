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
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkAddedToOss;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

@Slf4j
public class DeleteNodeTask extends TaskProcessor {

    private OssNodeService ossNodeService;

    private DatabaseInteractionService databaseInteractionService;

    private Task task;

    protected DeleteNodeTask(final Optional<TaskProcessor> nextProcessor, Task task,
                             OssNodeService ossNodeService, DatabaseInteractionService databaseInteractionService) {
        super(nextProcessor);
        this.task = task;
        this.ossNodeService = ossNodeService;
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    public void execute() {
        String vnfInstanceId = task.getVnfInstanceId();

        LOGGER.info(VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME, "Delete Node", vnfInstanceId);
        try {
            final VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            calculateManagedElementId(vnfInstance);

            checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);
            checkAddedToOss(vnfInstance, ossNodeService);

            ossNodeService.deleteNodeFromENM(vnfInstance, false);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Delete Node", vnfInstanceId);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Delete Node Task", e);
        } finally {
            databaseInteractionService.deleteTask(task);
            nextProcessor.ifPresent(TaskProcessor::execute);
        }
    }

    private void calculateManagedElementId(VnfInstance vnfInstance) {
        JSONObject taskAdditionalParams = new JSONObject(task.getAdditionalParams());
        Map<String, Object> addNodeOssTopology = !StringUtils.isEmpty(vnfInstance.getAddNodeOssTopology()) ?
                new JSONObject(vnfInstance.getAddNodeOssTopology()).toMap() :
                Collections.emptyMap();

        if (!addNodeOssTopology.containsKey(MANAGED_ELEMENT_ID)) {
            vnfInstance.setAddNodeOssTopology(new JSONObject(Map.of(MANAGED_ELEMENT_ID,
                    taskAdditionalParams.get(MANAGED_ELEMENT_ID))).toString());
        }
    }
}
