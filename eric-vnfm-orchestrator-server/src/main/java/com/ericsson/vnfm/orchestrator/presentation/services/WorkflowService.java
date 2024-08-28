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

import org.springframework.core.io.Resource;

import com.ericsson.vnfm.orchestrator.model.WorkflowInfo;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;


/**
 * Service for working with workflowService and retrieving and saving to appropriate repositories
 */
public interface WorkflowService {

    /**
     * @param releaseName
     *         the releaseName of the resource
     * @param workflowInstanceId
     *         the instance Id of the workflow
     *
     * @return workflow information related to the request
     */
    WorkflowInfo getWorkflowInfo(String releaseName, String workflowInstanceId);

    /**
     * @param releaseName
     *         the releaseName of the resource
     * @param workflowInstanceId
     *         the instance Id of the workflow
     *
     * @return A string representation of the URI
     */
    String createQueryToWorkflowService(String releaseName, String workflowInstanceId);

    /**
     * During registration we need to validate config file for naming, test connection to cluster, etc.
     * All validation performing on Workflow service.
     * This method about configuring and sending request for validation to WFS.
     * @param resource cluster configuration file to validate.
     *
     * @return A cluster server details response
     */
    ClusterServerDetailsResponse validateClusterConfigFile(Resource resource);
}
