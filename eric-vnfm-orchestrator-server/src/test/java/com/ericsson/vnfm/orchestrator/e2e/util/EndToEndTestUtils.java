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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static org.assertj.core.api.Assertions.fail;

import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder.JSON_REQUEST_PARAMETER_NAME;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;

import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EndToEndTestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EndToEndTestUtils() {
    }

    public static String getLifeCycleOperationId(final MvcResult result) {
        return result.getResponse().getHeader(HttpHeaders.LOCATION).split("/")[6];
    }

    public static WorkflowServiceEventMessage getWfsEventMessage(final String releaseName,
                                                                 final WorkflowServiceEventStatus status,
                                                                 final String lifeCycleOperationId,
                                                                 final WorkflowServiceEventType type) {

        WorkflowServiceEventMessage message = new WorkflowServiceEventMessage();
        message.setLifecycleOperationId(lifeCycleOperationId);
        message.setReleaseName(releaseName);
        message.setType(type);
        message.setStatus(status);
        return message;
    }

    public static WorkflowServiceEventMessage getWfsEventMessage(final String lifecycleOperationId,
                                                                 final WorkflowServiceEventType type,
                                                                 final WorkflowServiceEventStatus status,
                                                                 final String message) {

        WorkflowServiceEventMessage eventMessage = new WorkflowServiceEventMessage();
        eventMessage.setLifecycleOperationId(lifecycleOperationId);
        eventMessage.setType(type);
        eventMessage.setStatus(status);
        eventMessage.setMessage(message);
        return eventMessage;
    }

    public static HelmReleaseLifecycleMessage getHelmReleaseLifecycleMessage(final String releaseName,
                                                                             HelmReleaseState releaseState,
                                                                             final String lifeCycleOperationId,
                                                                             final HelmReleaseOperationType type,
                                                                             final String revisionNumber) {
        HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setLifecycleOperationId(lifeCycleOperationId);
        completed.setReleaseName(releaseName);
        completed.setOperationType(type);
        completed.setState(releaseState);
        completed.setRevisionNumber(revisionNumber);
        return completed;
    }

    public static <T> T extractEvnfmWorkflowRequest(HttpEntity request, Class<T> targetClass) {
        MultiValueMap<String, Object> requestBody = (MultiValueMap<String, Object>) request.getBody();
        List<Object> jsonParameterList = requestBody.get(JSON_REQUEST_PARAMETER_NAME);
        try {
            return MAPPER.readValue((String) jsonParameterList.get(0), targetClass);
        } catch (JsonProcessingException e) {
            fail("Request can`t be converted request={}", request);
            return null;
        }
    }
}
