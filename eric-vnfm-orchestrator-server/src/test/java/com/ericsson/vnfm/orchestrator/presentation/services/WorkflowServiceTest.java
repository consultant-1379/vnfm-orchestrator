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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.URL.WORKFLOW_URI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.WorkflowInfo;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        WorkflowServiceImpl.class,
        RestTemplate.class,
        ObjectMapper.class
})
public class WorkflowServiceTest {

    private static final String WORKFLOW_HOST_NAME = "localhost";
    private static final String DUMMY_RELEASE_NAME = "my-release";
    private static final String DUMMY_INSTANCE_ID = "58832646-3379-11e9-a2ef-7c2a31ce675f";
    private static final String COMPLETED = "COMPLETED";

    @Autowired
    private WorkflowServiceImpl workflowService;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean(name = "wfsRetryTemplate")
    private RetryTemplate wfsRetryTemplate;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    public void init() throws URISyntaxException {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testGetWorkflowInfo() {
        try {
            mockServer.expect(ExpectedCount.once(), requestTo(new URI(String.
                                                                              format(WORKFLOW_URI,
                                                                                     WORKFLOW_HOST_NAME,
                                                                                     DUMMY_RELEASE_NAME,
                                                                                     DUMMY_INSTANCE_ID)))).
                    andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.OK).
                                                                         contentType(MediaType.APPLICATION_JSON)
                                                                         .body(getWorkflowInfo(COMPLETED, DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID)));
        } catch (URISyntaxException use) {
            fail("");
        }
        ReflectionTestUtils.setField(workflowService, "workflowHost", WORKFLOW_HOST_NAME);
        WorkflowInfo workflowInfo = workflowService.getWorkflowInfo(DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID);
        mockServer.verify();
        assertThat(workflowInfo).isNotNull();
    }

    @Test
    public void testGetWorkflowInfoWith500Error() {
        Assertions.assertThrows(InternalRuntimeException.class, () -> {
            try {
                setupMockResponse500ErrorForGetWorkflowInfo();
            } catch (URISyntaxException use) {
                fail("");
            }
            ReflectionTestUtils.setField(workflowService, "workflowHost", WORKFLOW_HOST_NAME);
            workflowService.getWorkflowInfo(DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID);
            mockServer.verify();
        });
    }

    private void setupMockResponse500ErrorForGetWorkflowInfo() throws URISyntaxException {
        String workflowURI = String.format(WORKFLOW_URI, WORKFLOW_HOST_NAME, DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(workflowURI))).
                andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).
                                                                     contentType(MediaType.APPLICATION_JSON)
                                                                     .body(getWorkflowInfo(COMPLETED, DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID)));
    }

    @Test
    public void testGetWorkflowInfoNullResponse() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            try {
                setupMockWorkflowInfoNullResponse();
            } catch (URISyntaxException use) {
                fail("");
            }
            ReflectionTestUtils.setField(workflowService, "workflowHost", WORKFLOW_HOST_NAME);
            workflowService.getWorkflowInfo(DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID);
            mockServer.verify();
        });
    }

    private void setupMockWorkflowInfoNullResponse() throws URISyntaxException {
        String workflowURI = String.format(WORKFLOW_URI, WORKFLOW_HOST_NAME, DUMMY_RELEASE_NAME, DUMMY_INSTANCE_ID);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(workflowURI))).
                andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND).
                                                                     contentType(MediaType.APPLICATION_JSON).body(getResourceNotFoundErrorMessage()));
    }

    @Test
    public void getWorkflowStatusTest() throws IOException {
        EvnfmWorkFlowHistoryResponse response = mapper.readValue(getWorkflowInfo(COMPLETED, "status-release",
                                                                                 DUMMY_INSTANCE_ID), EvnfmWorkFlowHistoryResponse.class);
        WorkflowInfo workflowInfo = WorkflowServiceImpl.getWorkflowInfo(response);
        assertThat(workflowInfo.getWorkflowState()).isEqualTo(COMPLETED);
        assertThat(workflowInfo.getErrorMessage()).isNotEmpty();
    }

    @Test
    public void getWorkFlowStatusWithNullMessageTest() throws IOException {
        EvnfmWorkFlowHistoryResponse response =
                mapper.readValue(getWorkflowInfoNullMessage(COMPLETED, "status-release",
                                                            DUMMY_INSTANCE_ID), EvnfmWorkFlowHistoryResponse.class);

        WorkflowInfo workflowInfo = WorkflowServiceImpl.getWorkflowInfo(response);
        assertThat(workflowInfo.getWorkflowState()).isEqualTo(COMPLETED);
        assertThat(workflowInfo.getErrorMessage()).isNull();
    }

    private static String getWorkflowInfo(final String state, final String releaseName, final String instanceId) {
        return "{\n" + "  \"workflowQueries\": [\n" + "    {\n" + "      \"instanceId\": \"" + instanceId + "\",\n"
                + "      \"definitionKey\": \"UpgradeApplication__top\",\n" + "      \"chartName\": \"stable/mysql\",\n"
                + "      \"chartUrl\": null,\n" + "      \"chartVersion\": \"mysql-0.13.1\",\n"
                + "      \"releaseName\": \"" + releaseName + "\",\n" + "      \"namespace\": null,\n"
                + "      \"userId\": \"UNKNOWN\",\n" + "      \"workflowState\": \"" + state + "\",\n"
                + "      \"message\": \"Application Error\",\n"
                + "      \"startTime\": \"2019-02-18T12:33:08.056+0000\",\n" + "      \"additionalParams\": null,\n"
                + "      \"revision\": \"2\",\n" + "      \"revisionDescription\": \"Upgrade complete\"\n" + "    }\n"
                + "  ],\n" + "  \"metadata\": {\n" + "    \"count\": 1\n" + "  }\n" + "}";
    }

    private static String getWorkflowInfoNullMessage(final String state, final String releaseName, final String instanceId) {
        return "{\n" + "  \"workflowQueries\": [\n" + "    {\n" + "      \"instanceId\": \"" + instanceId + "\",\n"
                + "      \"definitionKey\": \"UpgradeApplication__top\",\n" + "      \"chartName\": \"stable/mysql\",\n"
                + "      \"chartUrl\": null,\n" + "      \"chartVersion\": \"mysql-0.13.1\",\n"
                + "      \"releaseName\": \"" + releaseName + "\",\n" + "      \"namespace\": null,\n"
                + "      \"userId\": \"UNKNOWN\",\n" + "      \"workflowState\": \"" + state + "\",\n"
                + "      \"message\": null,\n"
                + "      \"startTime\": \"2019-02-18T12:33:08.056+0000\",\n" + "      \"additionalParams\": null,\n"
                + "      \"revision\": \"2\",\n" + "      \"revisionDescription\": \"Upgrade complete\"\n" + "    }\n"
                + "  ],\n" + "  \"metadata\": {\n" + "    \"count\": 1\n" + "  }\n" + "}";
    }

    private static String getResourceNotFoundErrorMessage() {
        return "{\n" + "    \"errorDetails\": [\n" + "        {\n" + "            \"message\": \"Resource not found\"\n"
                + "        }\n" + "    ]\n" + "}";
    }
}