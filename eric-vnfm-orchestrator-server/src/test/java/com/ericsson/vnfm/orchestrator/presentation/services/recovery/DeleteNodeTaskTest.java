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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        InstanceService.class,
        OssNodeService.class,
        IdempotencyContext.class
})
@MockBean(classes = {
        EnmTopologyService.class,
        RestoreBackupFromEnm.class,
        EnrollmentInfoService.class,
        NotificationService.class,
        NfvoConfig.class,
        VnfInstanceQuery.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ReplicaCountCalculationService.class,
        ObjectMapper.class,
        ExtensionsService.class,
        PackageService.class,
        AdditionalAttributesHelper.class,
        VnfInstanceMapper.class,
        LifeCycleManagementHelper.class,
        LcmOpSearchService.class,
        ChangePackageOperationDetailsService.class,
        ChangeVnfPackageService.class,
        EnmMetricsExposers.class,
        RedisTemplate.class
})
public class DeleteNodeTaskTest {

    @Autowired
    private OssNodeService ossNodeService;

    @MockBean
    private SshHelper sshHelper;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void testDeleteNodeSuccess() {
        final VnfInstance vnfInstance = getVnfInstance();

        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));
        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(vnfInstance);

        Task task = prepareTask();
        TaskProcessor taskProcessor = new DeleteNodeTask(Optional.empty(), task, ossNodeService, databaseInteractionService);

        taskProcessor.execute();

        assertThat(vnfInstance.isAddedToOss()).isFalse();
    }

    @Test
    public void testDeleteNodeFailedVnfInstanceInNotInstantiatedState() {
        final VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);

        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteNodeTask(Optional.empty(), task, ossNodeService, databaseInteractionService);
        taskProcessor.execute();

        verify(databaseInteractionService).deleteTask(task);
    }

    @Test
    public void testDeleteNodeFailedVnfInstanceIsNotAddedToOss() {
        final VnfInstance vnfInstance = getVnfInstance();
        vnfInstance.setAddedToOss(false);

        when(databaseInteractionService.getVnfInstance("instance-id")).thenReturn(vnfInstance);
        when(sshHelper.executeScript(any())).thenReturn(createSshResponse(0));

        Task task = prepareTask();

        TaskProcessor taskProcessor = new DeleteNodeTask(Optional.empty(), task, ossNodeService, databaseInteractionService);
        taskProcessor.execute();

        verify(databaseInteractionService).deleteTask(task);
    }

    private Task prepareTask() {
        Task task = new Task();
        task.setVnfInstanceId("instance-id");
        task.setTaskName(DELETE_NODE);
        task.setAdditionalParams("{\"managedElementId\":\"test-node\"}");

        return task;
    }

    private VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("instance-id");
        vnfInstance.setInstantiationState(INSTANTIATED);
        vnfInstance.setOssTopology("{\"managedElementId\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"elementId-2\"},\n"
                                           + " \"networkElementType\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"nodetype\"}}");
        vnfInstance.setAddNodeOssTopology("{\"managedElementId\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"elementId\"},\n"
                                                  + " \"networkElementType\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"nodetype\"},\n"
                                                  + " \"networkElementVersion\":{\"type\":\"string\",\"required\":\"false\","
                                                  + "\"default\":\"nodeVersion\"},\n"
                                                  + " \"nodeIpAddress\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"my-ip\"},\n"
                                                  + " \"networkElementUsername\":{\"type\":\"string\",\"required\":\"false\","
                                                  + "\"default\":\"admin\"},\n"
                                                  + " \"networkElementPassword\":{\"type\":\"string\",\"required\":\"false\","
                                                  + "\"default\":\"password\"}}");
        vnfInstance.setAddedToOss(true);

        return vnfInstance;
    }

    private SshResponse createSshResponse(int exitStatus) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(exitStatus);
        sshResponse.setOutput("{\"check_node_data\": {\"exitStatus\":0, \"commandOutput\":\"Found 1 instance(s)\"}}");
        return  sshResponse;
    }
}
