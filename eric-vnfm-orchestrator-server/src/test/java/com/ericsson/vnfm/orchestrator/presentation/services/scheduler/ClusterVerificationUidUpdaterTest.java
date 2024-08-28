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
package com.ericsson.vnfm.orchestrator.presentation.services.scheduler;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.WorkflowService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.scheduler.ClusterVerificationUidUpdater;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;


@TestPropertySource(properties = { "clusterVerificationUidUpdater.enable=true" })
public class ClusterVerificationUidUpdaterTest extends AbstractDbSetupTest {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    ClusterVerificationUidUpdater clusterVerificationUidUpdater;

    @MockBean
    private WorkflowService workflowService;

    @Test
    public void testFillVerificationNamespaceUidWFSUnavailable() {
        Mockito.when(workflowService.validateClusterConfigFile(any()))
                .thenThrow(new InternalRuntimeException("Can`t validate config file. Service is unavailable."));

        final List<ClusterConfigFile> configs =
                databaseInteractionService.getClusterConfigFilesWhereUuidIsNull();

        clusterVerificationUidUpdater.fillVerificationNamespaceUid();

        await().atMost(30, SECONDS)
                .untilAsserted(() -> assertThat(databaseInteractionService.getClusterConfigFilesWhereUuidIsNull().size()).isEqualTo(configs.size()));
    }

    @Test
    public void testFillVerificationNamespaceUidConfigNotValid() {
        Mockito.when(workflowService.validateClusterConfigFile(any()))
                .thenThrow(new ValidationException("Cluster config file not valid.", "Cluster config file not valid.", HttpStatus.BAD_REQUEST));

        final List<ClusterConfigFile> configs =
                databaseInteractionService.getClusterConfigFilesWhereUuidIsNull();

        clusterVerificationUidUpdater.fillVerificationNamespaceUid();

        await().atMost(30, SECONDS)
                .untilAsserted(() -> assertThat(databaseInteractionService.getClusterConfigFilesWhereUuidIsNull().size()).isEqualTo(configs.size()));
    }

    @Test
    public void testFillVerificationNamespaceUid() {
        final List<ClusterConfigFile> configs =
                databaseInteractionService.getClusterConfigFilesWhereUuidIsNull();

        assertThat(configs).isNotEmpty();

        final Namespace nameSpace = new Namespace();
        nameSpace.setName("kube-system");
        nameSpace.setUid("kube-system");

        final List<Namespace> nameSpaces = List.of(nameSpace);
        final ClusterServerDetailsResponse clusterServerDetailsResponse = new ClusterServerDetailsResponse();
        clusterServerDetailsResponse.setNamespaces(nameSpaces);

        Mockito.when(workflowService.validateClusterConfigFile(any())).thenReturn(clusterServerDetailsResponse);

        clusterVerificationUidUpdater.fillVerificationNamespaceUid();

        await().atMost(30, SECONDS)
                .untilAsserted(() -> assertThat(databaseInteractionService.getClusterConfigFilesWhereUuidIsNull()).isEmpty());
    }
}
