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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.PACKAGE_ID;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageServiceImpl;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;


@SpringBootTest(classes = {
        PackageService.class
})
@MockBean(classes = {
        OnboardingClient.class,
        OnboardingUriProvider.class,
        NfvoConfig.class,
        PackageServiceImpl.class
})
public class UpdateUsageStateTaskTest {

    @Autowired
    private PackageService packageService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void testUpdatePackageState() {
        Task task = prepareTask();

        TaskProcessor taskProcessor = new UpdateUsageStateTask(Optional.empty(), task, packageService, databaseInteractionService);

        taskProcessor.execute();

        verify(packageService, times(1))
                .updateUsageState(eq("source-vnfd-id"), eq("instance-id"), eq(false));
    }


    private Task prepareTask() {
        Task task = new Task();
        task.setVnfInstanceId("instance-id");
        task.setTaskName(UPDATE_PACKAGE_STATE);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PACKAGE_ID, "source-vnfd-id");

        task.setAdditionalParams(convertObjToJsonString(additionalParams));

        return task;
    }
}