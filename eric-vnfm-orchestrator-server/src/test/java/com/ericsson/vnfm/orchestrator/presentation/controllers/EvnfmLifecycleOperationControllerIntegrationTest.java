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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.am.shared.filter.model.OperandOneValue.EQUAL;
import static com.ericsson.am.shared.filter.model.OperandOneValue.NOT_EQUAL;
import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_COLUMN;
import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_ORDER;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAGE;
import static com.ericsson.vnfm.orchestrator.TestUtils.QUERY_PARAMETER_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkLinkBody;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkPaginationBody;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ericsson.am.shared.filter.FilterErrorMessage;
import com.ericsson.vnfm.orchestrator.model.PagedOperationsResponse;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.controllers.internal.EvnfmLifecycleOperationController;
import com.ericsson.vnfm.orchestrator.presentation.services.LifecycleOperationsService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wiremock.com.jayway.jsonpath.JsonPath;

@AutoConfigureMockMvc
public class EvnfmLifecycleOperationControllerIntegrationTest extends AbstractDbSetupTest {

    private static final String GET_ALL_OPERATIONS = "/api/v1/operations";
    private static final List<String> VALID_SORT_COLUMNS = Arrays.asList("operationState", "lifecycleOperationType", "vnfProductName",
                                                                         "vnfSoftwareVersion", "vnfInstanceName", "clusterName", "namespace",
                                                                         "startTime", "stateEnteredTime");
    public static final String USERNAME = "username";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @InjectMocks
    private EvnfmLifecycleOperationController evnfmLifecycleOperationController;

    @Mock
    private LifecycleOperationsService lifecycleOperationsService;

    @Test
    public void testGetAllOperations() throws Exception {
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull()
                .hasSize(15)
                .extracting(VnfResourceLifecycleOperation::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.reverseOrder());
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 1, 15);
        checkLinkBody(operationsResponse.getLinks(), 1, totalPages);
    }

    @Test
    public void testGetAllOperationsWithPage() throws Exception {
        //Second Page
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(PAGE, "2");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty()
                .hasSize(15);
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 2, 15);
        checkLinkBody(operationsResponse.getLinks(), 2, totalPages);
    }

    @Test
    public void testGetAllOperationsWithSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("size", "5");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty()
                .hasSize(5);
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 1, 5);
        checkLinkBody(operationsResponse.getLinks(), 1, totalPages);
    }

    @Test
    public void testGetAllOperationsWithSort() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "stateEnteredTime,asc");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty()
                .extracting(VnfResourceLifecycleOperation::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.naturalOrder());
        assertThat(operationsResponse.getItems().size()).isEqualTo(15);
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 1, 15);
        checkLinkBody(operationsResponse.getLinks(), 1, totalPages);
    }

    @Test
    public void testGetAllOperationsWithSortTimeDefault() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "stateEnteredTime");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty()
                .extracting(VnfResourceLifecycleOperation::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(operationsResponse.getItems().size()).isEqualTo(15);
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 1, 15);
        checkLinkBody(operationsResponse.getLinks(), 1, totalPages);
    }

    @Test
    public void testGetAllOperationsWithSortOtherPossibleCombinations() throws Exception {
        // Skip fetching all data
        given(lifecycleOperationsService.getLifecycleOperationsPage(any(), any())).willReturn(new PageImpl<>(new ArrayList<>()));
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        for (String eachValidColumn : VALID_SORT_COLUMNS) {
            queries.add("sort", eachValidColumn);
            getMockMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
            queries.clear();
        }
        queries.add("sort", "lifecycleOperationType,asc");
        queries.add("sort", "stateEnteredTime");
        getMockMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "lifecycleOperationType");
        queries.add("sort", "stateEnteredTime");
        getMockMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "lifecycleOperationType,asc");
        queries.add("sort", "stateEnteredTime,desc");
        getMockMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
    }

    @Test
    public void testGetAllOperationsWithSortAndSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "startTime,desc");
        queries.add("size", "20");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty()
                .hasSize(20)
                .extracting(VnfResourceLifecycleOperation::getStartTime)
                .isSortedAccordingTo(Comparator.reverseOrder());
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 1, 20);
        checkLinkBody(operationsResponse.getLinks(), 1, totalPages);
    }

    @Test
    public void testGetAllOperationsWithPageAndSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(PAGE, "3");
        queries.add("size", "60");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems()).isNotNull().isNotEmpty().hasSize(60);
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 3, 60);
        checkLinkBody(operationsResponse.getLinks(), 3, totalPages);
    }

    @Test
    public void testGetAllOperationsWithFilterAndSize() throws Exception {
        String filter = "(%s,operationState,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(PAGE, "3");
        queries.add("filter", String.format(filter, EQUAL.getFilterOperation(), VnfLcmOpOcc.OperationStateEnum.COMPLETED));
        queries.add("size", "10");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .isNotNull().isNotEmpty().hasSizeLessThanOrEqualTo(10)
                .extracting(VnfResourceLifecycleOperation::getOperationState)
                .allMatch(state -> state.equals(VnfLcmOpOcc.OperationStateEnum.COMPLETED.toString()));
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 3, 10);
        checkLinkBody(operationsResponse.getLinks(), 3, totalPages);
    }

    @Test
    public void testGetAllOperationsWithFilterPageAndSize() throws Exception {
        String clusterName = "multiple-charts";
        String filter = "(neq,clusterName,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(PAGE, "2");
        queries.add("filter", String.format(filter, clusterName));
        queries.add("size", "50");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems()).isNotNull()
                .extracting(VnfResourceLifecycleOperation::getClusterName)
                .noneMatch(cluster -> cluster.equals(clusterName));
        int totalPages = checkPaginationBody(operationsResponse.getPage(), 2, 50);
        checkLinkBody(operationsResponse.getLinks(), 2, totalPages);
    }

    @Test
    public void testGetAllOperationsWithFilterNotForEqualOperationForEnumerationValue() throws Exception {
        String filter = "(neq,operationState,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationStateEnum.FAILED));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getOperationState)
                .doesNotContain(VnfLcmOpOcc.OperationStateEnum.FAILED.toString());
    }

    @Test
    public void testGetAllOperationsWithFilterForNotEqualOperationForDateValue() throws Exception {
        String operation = NOT_EQUAL.getFilterOperation();
        String parameterName = "startTime";
        String filter = "(" + operation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        ProblemDetails problemDetails = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForEqualOperationForEnumerationValue() throws Exception {
        String filter = "(%s,lifecycleOperationType,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, EQUAL.getFilterOperation(), "CHANGE_PACKAGE_INFO"));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getLifecycleOperationType)
                .allMatch(op -> op.equals("CHANGE_PACKAGE_INFO"));
    }

    @Test
    public void testGetAllOperationsWithFilterForEqualOperationForDateValue() throws Exception {
        String operation = "eq";
        String parameterName = "stateEnteredTime";
        String filter = "(" + operation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        ProblemDetails problemDetails = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation,
                                                                       parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForInOperatorOnEnumerationDataType() throws Exception {
        String filter = "(in,vnfInstanceName,%s)";
        String vnfName = "rollback-with-crd";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfName));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getVnfInstanceName)
                .allMatch(instanceName -> instanceName.equals(vnfName));
    }

    @Test
    public void testGetAllOperationsWithFilterForInOperatorOnDateDataType() throws Exception {
        String filterOperation = "in";
        String parameterName = "startTime";
        String filter = "(" + filterOperation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       filterOperation, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForInOperatorOnStringDataType() throws Exception {
        String namespace1 = "backup-test";
        String namespace2 = "testscale";
        String filter = "(in,namespace,%s,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, namespace1, namespace2));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getNamespace).contains(namespace1, namespace2);
    }

    @Test
    public void testGetAllOperationsWithFilterForNotInOperatorOnEnumerationDataType() throws Exception {
        String filter = "(nin,lifecycleOperationType,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, "TERMINATE"));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getLifecycleOperationType)
                .doesNotContain("TERMINATE");
    }

    @Test
    public void testGetAllOperationsWithFilterForNotInOperatorOnDateDataType() throws Exception {
        String filterOperation = "nin";
        String parameterName = "startTime";
        String filter = "(" + filterOperation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       filterOperation, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForLessThanOperatorOnStringDataType() throws Exception {
        String stateEnteredTime = "2021-05-28T16:20:24.49";
        String filter = "(lt,stateEnteredTime,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, stateEnteredTime));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getStateEnteredTime)
                .allSatisfy(eachTime -> assertThat(eachTime).isLessThan(stateEnteredTime));
    }

    @Test
    public void testGetAllOperationsWithFilterForLessThanOperatorOnDateDataType() throws Exception {
        String now = LocalDateTime.now().toString();
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(lt,stateEnteredTime," + now + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllOperationsWithFilterForLessThanOperatorOnEnumerationDataType() throws Exception {
        String operation = "lt";
        String parameterName = "operationState";
        String filter = "(" + operation + "," + parameterName + "," + VnfLcmOpOcc.OperationStateEnum.PROCESSING + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForLessThanEqualOperatorOnStringDataType() throws Exception {
        String vnfInstanceName = "test-cnf";
        String filter = "(lte,vnfInstanceName,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceName));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation)
                .extracting(VnfResourceLifecycleOperation::getVnfInstanceName)
                .allSatisfy(eachId -> assertThat(eachId).isLessThanOrEqualTo(vnfInstanceName));
    }

    @Test
    public void testGetAllOperationsWithFilterForLessThanEqualOperatorOnDateDataType() throws Exception {
        String now = LocalDateTime.now().toString();
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(lte,startTime," + now + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllOperationsWithFilterForGreaterThanEqualOperatorOnStringDataType() throws Exception {
        String vnfInstanceId = "wf1ce-rd14-477c-vnf0-downsize0100";
        String filter = "(gt,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation)
                .extracting(VnfResourceLifecycleOperation::getVnfInstanceId)
                .allSatisfy(eachId -> assertThat(eachId).isGreaterThanOrEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllOperationsWithFilterForGreaterThanEqualOperatorOnEnumerationDataType() throws Exception {
        String operation = "gte";
        String parameterName = "lifecycleOperationType";
        String filter = "(" + operation + "," + parameterName + "," + "INSTANTIATE" + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForContainsOperatorOnStringDataType() throws Exception {
        String id = "4673";
        String filter = "(cont,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation).extracting(VnfResourceLifecycleOperation::getVnfInstanceId).allSatisfy(eachId -> assertThat(eachId).contains(id));
    }

    @Test
    public void testGetAllOperationsWithFilterForNotContainsOperatorOnDateDataType() throws Exception {
        String operator = "ncont";
        String parameterName = "startTime";
        String filter = "(" + operator + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllOperationsWithFilterForEqualsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(%s,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, EQUAL.getFilterOperation(), vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation)
                .extracting(VnfResourceLifecycleOperation::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllOperationsWithFilterForNotEqualsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(neq,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        List<VnfResourceLifecycleOperation> allOperation = getLifecycleOperations(mvcResult).getItems();
        assertThat(allOperation)
                .extracting(VnfResourceLifecycleOperation::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isNotEqualTo(vnfInstanceId));
    }

    @Test
    @Ignore("Ignoring due to failing in jenkings and a bug fix needs to be merged - bug fix SM-103486 is not affecting behaviour tested here. " +
            "Manual test were executed for this operation successfully.")
    public void testGetAllOperationsFailsPageLimitExceeded() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add(PAGE, "22");
        MvcResult result = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).contains(
                "Requested page number exceeds the total number of pages. Requested page:: 22. Total page size::");
    }

    @Test
    public void testGetAllOperationsFailsInvalidPageParams() throws Exception {
        // Invalid page value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add(PAGE, "one");
        getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);

        // Negative page value
        requestParams.set(PAGE, "-1");
        getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);
    }

    @Test
    public void testGetAllOperationsFailsInvalidSizeParams() throws Exception {
        // Invalid size value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("size", "ten");
        getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);

        // Negative size value
        requestParams.set("size", "-10");
        MvcResult result = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page size:: -10, page size must be greater than 0");
    }

    @Test
    public void testGetAllOperationsFailsInvalidSortFields() throws Exception {
        // Invalid sort field single without order
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "vnfSoftwareVersion123");
        checkSortFieldFailure(requestParams, "vnfSoftwareVersion123");

        // Invalid sort field single with order
        requestParams.clear();
        requestParams.add("sort", "vnfSoftwareVersion123,asc");
        checkSortFieldFailure(requestParams, "vnfSoftwareVersion123");

        // Invalid sort field multi only one order
        requestParams.clear();
        requestParams.add("sort", "vnfSoftwareVersion123,asc");
        requestParams.add("sort", "vnfProductName");
        checkSortFieldFailure(requestParams, "vnfSoftwareVersion123");

        // Invalid sort field multi both with order
        requestParams.clear();
        requestParams.add("sort", "vnfSoftwareVersion123,asc");
        requestParams.add("sort", "vnfProductName,asc");
        checkSortFieldFailure(requestParams, "vnfSoftwareVersion123");

        // Invalid sort field multi both field invalid
        requestParams.clear();
        requestParams.add("sort", "vnfProductNameInvalid,asc");
        requestParams.add("sort", "vnfSoftwareVersion123,asc");
        checkSortFieldFailure(requestParams, "vnfProductNameInvalid");
    }

    @Test
    public void testGetAllOperationsFailsInvalidSortValues() throws Exception {
        // Invalid sort value single
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "vnfSoftwareVersion,ascending");
        checkSortValueFailure(requestParams, "ascending");

        // Invalid sort value multi one invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "vnfSoftwareVersion");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi both invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "vnfSoftwareVersion,ascending");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi same sort one invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "clusterName,asc");
        requestParams.add("sort", "vnfSoftwareVersion");
        checkSortValueFailure(requestParams, "descending");
    }

    @Test
    public void testGetAllOperationsFailsWithExceededPageSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("size", "250");
        MvcResult result = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, queries);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Total size of the results will be shown cannot be more than 100. Requested page size 250");
    }

    @Test
    public void testGetDataByUsernameFilter() throws Exception {
        //given
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        String testUsername = "some_username";
        String filter = "(" + EQUAL.getFilterOperation() + "," + USERNAME + "," + testUsername + ")";
        queries.add("filter", filter);
        //when
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        //then
        assertThat(operationsResponse.getItems())
                .hasSizeGreaterThanOrEqualTo(2)
                .extracting(VnfResourceLifecycleOperation::getUsername)
                .allMatch(op -> op.equals(testUsername));
    }

    @Test
    public void testGetEmptyResponseForNotExistedUser() throws Exception {
        //given
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        String notExistingUsername = "not_existed_username";
        String filter = "(" + EQUAL.getFilterOperation() + "," + USERNAME + "," + notExistingUsername + ")";
        queries.add("filter", filter);
        //when
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        //then
        assertThat(operationsResponse.getItems())
                .isEmpty();
    }

    @Test
    public void testGetDataByUsernameAndVnfInstanceIdFilter() throws Exception {
        //given
        String vnfInstanceId = "30def1ce-4cf4-477c-aab3-21c454e6a389";
        String testUsername = "some_username";
        String filter =
                "(" + EQUAL.getFilterOperation() + "," + USERNAME + "," + testUsername + ");(" + EQUAL.getFilterOperation() + ",vnfInstanceId,"
                        + vnfInstanceId + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        //when
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        //then
        assertThat(operationsResponse.getItems())
                .isNotEmpty().hasSizeLessThanOrEqualTo(1)
                .extracting(VnfResourceLifecycleOperation::getUsername)
                .allMatch(op -> op.equals(testUsername));
        assertThat(operationsResponse.getItems())
                .extracting(VnfResourceLifecycleOperation::getVnfInstanceId)
                .allMatch(op -> op.equals(vnfInstanceId));
    }

    @Test
    public void testGetEmptyResponseForNotExistingUsernameAndVnfInstanceIdFilter() throws Exception {
        //given
        String vnfInstanceId = "708fcbc8-474f-4673-91ee-761fd83641e6";
        String notExistingUsername = "not_existed_username";
        String filter =
                "(" + EQUAL.getFilterOperation() + "," + USERNAME + "," + notExistingUsername + ");(" + EQUAL.getFilterOperation() + ",vnfInstanceId,"
                        + vnfInstanceId + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        //when
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK, queries);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        //then
        assertThat(operationsResponse.getItems())
                .isEmpty();
    }

    @Test
    public void testGetAllOperationsReturnUsername() throws Exception {
        MvcResult mvcResult = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.OK);
        PagedOperationsResponse operationsResponse = getLifecycleOperations(mvcResult);
        assertThat(operationsResponse.getItems())
                .flatExtracting(VnfResourceLifecycleOperation::getUsername)
                .isNotNull();
    }

    private PagedOperationsResponse getLifecycleOperations(final MvcResult mvcResult) throws JsonProcessingException, UnsupportedEncodingException {
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();
        return new ObjectMapper().readValue(response, new TypeReference<>() {
        });
    }

    private MvcResult getMvcResult(final String url, final HttpStatus expectedStatus) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private MvcResult getMvcResult(final String url, final HttpStatus expectedStatus, final MultiValueMap<String, String> queries) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).queryParams(queries).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private MvcResult getMockMvcResult(String url, final HttpStatus expectedStatus, MultiValueMap<String, String> queryParams) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).queryParams(queryParams).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(evnfmLifecycleOperationController).build();
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private void checkSortFieldFailure(final LinkedMultiValueMap<String, String> requestParams, String invalidField) throws Exception {
        assertThat((String) JsonPath.read(getSortErrorResponse(requestParams), "$.detail"))
                .contains(String.format(ERROR_BODY_SORT_COLUMN, invalidField))
                .contains(VALID_SORT_COLUMNS);
    }

    private void checkSortValueFailure(final LinkedMultiValueMap<String, String> requestParams, String invalidValue) throws Exception {
        assertThat((String) JsonPath.read(getSortErrorResponse(requestParams), "$.detail"))
                .contains(String.format(ERROR_BODY_SORT_ORDER, invalidValue));
    }

    private String getSortErrorResponse(final LinkedMultiValueMap<String, String> requestParams) throws Exception {
        MvcResult result = getMvcResult(GET_ALL_OPERATIONS, HttpStatus.BAD_REQUEST, requestParams);
        String response = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(response, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        return response;
    }
}
