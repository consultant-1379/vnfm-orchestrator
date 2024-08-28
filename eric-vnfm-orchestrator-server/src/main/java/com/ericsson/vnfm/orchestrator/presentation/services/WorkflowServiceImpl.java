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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.INVALID_CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.WORKFLOW_URI;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.WORKFLOW_VALIDATE_CONFIG_FILE_URI;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.getWorkflowErrorDetailsMessages;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.sanitizeWorkflowError;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.WorkflowInfo;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final String CLUSTER_CONFIG = "clusterConfig";
    public static final String WFS_SERVICE_IS_UNAVAILABLE = "Can`t validate config file. Service is unavailable.";

    @Value("${workflow.host}")
    private String workflowHost;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("wfsRetryTemplate")
    private RetryTemplate wfsRetryTemplate;

    @VisibleForTesting
    static WorkflowInfo getWorkflowInfo(final EvnfmWorkFlowHistoryResponse responseContent) {
        EvnfmWorkFlowQuery evnfmWorkFlowQuery = responseContent.getWorkflowQueries().get(0);
        WorkflowInfo workflowInfo = new WorkflowInfo();
        workflowInfo.setWorkflowState(evnfmWorkFlowQuery.getWorkflowState());
        workflowInfo.setNamespace(evnfmWorkFlowQuery.getNamespace());
        workflowInfo.setErrorMessage(evnfmWorkFlowQuery.getMessage() != null ?
                                             evnfmWorkFlowQuery.getMessage().toString() : null);
        return workflowInfo;
    }

    @Override
    public WorkflowInfo getWorkflowInfo(final String releaseName, final String instanceId) {
        final String workflowQueryApi = createQueryToWorkflowService(releaseName, instanceId);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<EvnfmWorkFlowHistoryResponse> workflowHistoryResponse = null;
        try {
            workflowHistoryResponse = restTemplate
                    .getForEntity(workflowQueryApi, EvnfmWorkFlowHistoryResponse.class, entity);
        } catch (final HttpStatusCodeException hsce) {
            LOGGER.warn("An error during getting workflow info", hsce);
            if (hsce.getStatusCode() == HttpStatus.NOT_FOUND) {
                String message = String
                        .format("There is no resource with releaseName: %s and instance Id: %s", releaseName,
                                instanceId);
                throw new NotFoundException(message, hsce);
            }
        }
        if (workflowHistoryResponse != null && workflowHistoryResponse.getBody() != null) {
            return getWorkflowInfo(workflowHistoryResponse.getBody()); // NOSONAR
        } else {
            throw new InternalRuntimeException("Unable to retrieve resource history");
        }
    }

    @Override
    public String createQueryToWorkflowService(final String releaseName, final String workflowInstanceId) {
        return String.format(WORKFLOW_URI, workflowHost, releaseName, workflowInstanceId);
    }

    @Override
    public ClusterServerDetailsResponse validateClusterConfigFile(final Resource resource) {
        final String workflowQueryApi = String.format(WORKFLOW_VALIDATE_CONFIG_FILE_URI, workflowHost);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add(CLUSTER_CONFIG, resource);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<ClusterServerDetailsResponse> response =
                    wfsRetryTemplate.execute(context -> restTemplate.postForEntity(
                            workflowQueryApi,
                            request,
                            ClusterServerDetailsResponse.class));

            if (!response.hasBody() || response.getBody() == null || response.getStatusCode() != HttpStatus.OK) {
                throw new InternalRuntimeException("Unable to get response from Workflow service");
            }
            return response.getBody();
        } catch (ResourceAccessException resourceAccessException) {
            throw new InternalRuntimeException(WFS_SERVICE_IS_UNAVAILABLE, resourceAccessException);
        } catch (HttpClientErrorException any) {
            String errorDetails = sanitizeWorkflowError(any.getResponseBodyAsString());
            throw new ValidationException("An error occurred during validating cluster config: " + getWorkflowErrorDetailsMessages(errorDetails),
                                          INVALID_CLUSTER_CONFIG, HttpStatus.BAD_REQUEST, any);
        }
    }
}
