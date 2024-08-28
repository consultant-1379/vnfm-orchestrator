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
package com.ericsson.vnfm.orchestrator.scheduler;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.VERIFICATION_NAMESPACE;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.HttpFileResource;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.WorkflowService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ClusterVerificationUidUpdater {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WorkflowService workflowService;

    @Value("${clusterVerificationUidUpdater.enable}")
    private Boolean enableFillVerificationNamespaceUid;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void fillVerificationNamespaceUid() {
        if (Boolean.TRUE.equals(enableFillVerificationNamespaceUid)) {

            final List<ClusterConfigFile> configs =
                    databaseInteractionService.getClusterConfigFilesWhereUuidIsNull();

            try {
                configs.forEach(this::updateConfig);
            } catch (InternalRuntimeException e) {
                LOGGER.error("An error occurred during getting cluster config files", e);
            }
        }
    }

    public void updateConfig(ClusterConfigFile config) {
        final HttpFileResource resource = new HttpFileResource(config.getContent().getBytes(StandardCharsets.UTF_8), config.getName());

        try {
            final ClusterServerDetailsResponse clusterServerDetails = workflowService.validateClusterConfigFile(resource);

            if (clusterServerDetails != null) {
                clusterServerDetails.getNamespaces().forEach(namespace -> {
                    if (VERIFICATION_NAMESPACE.equals(namespace.getName())) {
                        config.setVerificationNamespaceUid(namespace.getUid());
                        databaseInteractionService.saveClusterConfig(config);
                    }
                });
            }
        } catch (ValidationException e) {
            LOGGER.warn("An error occurred during validate cluster config files", e);
        }
    }
}
