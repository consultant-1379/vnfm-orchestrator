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

import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.PACKAGE_ID;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.util.Map;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateUsageStateTask extends TaskProcessor {

    private Task task;

    private PackageService packageService;
    private DatabaseInteractionService databaseInteractionService;

    protected UpdateUsageStateTask(final Optional<TaskProcessor> nextProcessor, Task task, PackageService packageService,
                                   DatabaseInteractionService databaseInteractionService) {
        super(nextProcessor);
        this.task = task;
        this.packageService = packageService;
        this.databaseInteractionService = databaseInteractionService;
    }

    @Override
    public void execute() {
        String vnfInstanceId = task.getVnfInstanceId();
        Map<String, Object> additionalParams = parseJsonToGenericType(task.getAdditionalParams(), new TypeReference<>() { });
        String packageId = String.valueOf(additionalParams.get(PACKAGE_ID));
        try {
            packageService.updateUsageState(packageId, vnfInstanceId, false);
        } catch (Exception e) {
            LOGGER.error("Error occurred during Delete VnfInstance Task", e);
        } finally {
            databaseInteractionService.deleteTask(task);
            nextProcessor.ifPresent(TaskProcessor::execute);
        }
    }
}
