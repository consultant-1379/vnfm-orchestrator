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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_COLUMN;
import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_ORDER;
import static com.ericsson.vnfm.orchestrator.TestUtils.NEXTPAGE_OPAQUE_MARKER;
import static com.ericsson.vnfm.orchestrator.TestUtils.QUERY_PARAMETER_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkLinkHeader;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkPaginationHeader;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.DEFAULT_PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.TaskName;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wiremock.com.jayway.jsonpath.JsonPath;

@AutoConfigureMockMvc
public class VnfLcmOperationsControllerIntegrationTest extends AbstractDbSetupTest {

    private static final String GET_ALL_VNF_OP_OCC = "/vnflcm/v1/vnf_lcm_op_occs";
    private static final String PATH_SEPARATOR = "/";
    private static final String VNF_OP_OCC_ID = "b08fcbc8-474f-4673-91ee-761fd83991e6";
    private static final String VNF_OP_OCC_ID_NON_EXISTENT = "123456-abcd-789-efgh";
    private static final String VNF_FAILED_TEMP = "rd3f70380-74ce-11ea-bc55-0242ac130003";
    private static final String VNF_FAILED_TEMP_PROCESSING = "rdf70380-74ce-11ea-bc55-0242ac130003";
    private static final List<String> VALID_SORT_COLUMNS = Arrays.asList("operationState", "stateEnteredTime", "startTime", "vnfInstance",
                                                                         "lifecycleOperationType");
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @InjectMocks
    private VnfLcmOperationsController vnfLcmOperationsController;

    @MockBean
    private InstanceService instanceService;

    @Mock
    private VnfLcmOperationService vnfLcmOperationService;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @Test
    public void testGetAllVnfOperationOccurrences() throws Exception {
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .hasSize(DEFAULT_PAGE_SIZE)
                .extracting(VnfLcmOpOcc::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.reverseOrder());
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithPage() throws Exception {
        //Second Page
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(NEXTPAGE_OPAQUE_MARKER, "2");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .hasSize(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 2, DEFAULT_PAGE_SIZE);
        checkLinkHeader(mvcResult.getResponse(), 2, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("size", "40");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .hasSize(40);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, 40);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithSortTimeDefault() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "startTime");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .extracting(VnfLcmOpOcc::getStartTime)
                .isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(vnfLcmOpOccList.size()).isEqualTo(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithSort() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "stateEnteredTime,asc");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .extracting(VnfLcmOpOcc::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.naturalOrder());
        assertThat(vnfLcmOpOccList.size()).isEqualTo(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithSortOtherPossibleCombinations() throws Exception {
        // Skip fetching all data
        given(vnfLcmOperationService.getAllLcmOperationsPage(any(), any())).willReturn(new PageImpl<>(new ArrayList<>()));
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        for (String eachValidColumn : VALID_SORT_COLUMNS) {
            queries.add("sort", eachValidColumn);
            getMockMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
            queries.clear();
        }
        queries.add("sort", "operationState,asc");
        queries.add("sort", "startTime");
        getMockMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "operationState");
        queries.add("sort", "startTime");
        getMockMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "operationState,asc");
        queries.add("sort", "startTime,desc");
        getMockMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithSortAndSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "stateEnteredTime,desc");
        queries.add("size", "40");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList)
                .isNotNull().isNotEmpty()
                .hasSize(40)
                .extracting(VnfLcmOpOcc::getStateEnteredTime)
                .isSortedAccordingTo(Comparator.comparing(Date::getTime).reversed());
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, 40);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithPageAndSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(NEXTPAGE_OPAQUE_MARKER, "3");
        queries.add("size", "60");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList).isNotNull().isNotEmpty().hasSize(60);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 3, 60);
        checkLinkHeader(mvcResult.getResponse(), 3, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithFilterAndSize() throws Exception {
        String filter = "(eq,operationState,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(NEXTPAGE_OPAQUE_MARKER, "3");
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationStateEnum.COMPLETED));
        queries.add("size", "10");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList).isNotNull().isNotEmpty().hasSizeLessThanOrEqualTo(10);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 3, 10);
        checkLinkHeader(mvcResult.getResponse(), 3, totalPages);
    }

    @Test
    public void testGetAllVnfOperationOccurrencesWithFilterPageAndSize() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(neq,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(NEXTPAGE_OPAQUE_MARKER, "2");
        queries.add("filter", String.format(filter, id));
        queries.add("size", "5");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> vnfLcmOpOccList = getLifecycleOperations(mvcResult);
        assertThat(vnfLcmOpOccList).isNotNull().isNotEmpty().hasSizeLessThanOrEqualTo(5);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 2, 5);
        checkLinkHeader(mvcResult.getResponse(), 2, totalPages);
    }

    @Test
    public void testGetVnfOperationOccurrenceByOccId() throws Exception {
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC + PATH_SEPARATOR +
                                                   VNF_OP_OCC_ID, HttpStatus.OK);
        VnfLcmOpOcc vnfLcmOpOcc = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), VnfLcmOpOcc.class);
        assertThat(vnfLcmOpOcc.getId()).isEqualTo("b08fcbc8-474f-4673-91ee-761fd83991e6");
        assertThat(vnfLcmOpOcc.getVnfInstanceId()).isEqualTo("d3def1ce-4cf4-477c-aab3-21c454e6a379");
        assertThat(vnfLcmOpOcc.getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.INSTANTIATE);
        assertThat(vnfLcmOpOcc.getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.STARTING);
    }

    @Test
    public void testGetVnfOpOccByNonExistentOccId() throws Exception {
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC + PATH_SEPARATOR +
                                                   VNF_OP_OCC_ID_NON_EXISTENT, HttpStatus.NOT_FOUND);
        ProblemDetails problemDetails = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getType().toString()).isEqualTo(TYPE_BLANK);
        assertThat(problemDetails.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetails.getTitle()).isEqualTo("Not Found Exception");
        assertThat(problemDetails.getDetail()).isEqualTo("The vnfLcmOpOccId-123456-abcd-789-efgh does not exist");
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotEqualOperationForStringValue() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(neq,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).doesNotContain(id);
        int totalPages = checkPaginationHeader(mvcResult.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(mvcResult.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterNotForEqualOperationForEnumerationValue() throws Exception {
        String filter = "(neq,operationState,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationStateEnum.FAILED));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getOperationState)
                .doesNotContain(VnfLcmOpOcc.OperationStateEnum.FAILED);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotEqualOperationForBooleanValueTrue() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(neq,isCancelPending,true)");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getIsCancelPending)
                .allSatisfy(eachisIsCancelPending -> assertThat(eachisIsCancelPending).isFalse());
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotEqualOperationForBooleanValueFalse() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(neq,isCancelPending,false)");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getIsCancelPending)
                .allSatisfy(eachisIsCancelPending -> assertThat(eachisIsCancelPending).isTrue());
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotEqualOperationForDateValue() throws Exception {
        String operation = "neq";
        String parameterName = "startTime";
        String filter = "(" + operation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        ProblemDetails problemDetails = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualOperationForStringValue() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(eq,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).containsOnly(id);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualOperationForEnumerationValue() throws Exception {
        String filter = "(eq,operationState,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationStateEnum.FAILED));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getOperationState).containsOnly(VnfLcmOpOcc.OperationStateEnum.FAILED);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualOperationForBooleanValueTrue() throws Exception {
        String filter = "(eq,isCancelPending,true)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).isEmpty();
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualOperationForBooleanValueFalse() throws Exception {
        String filter = "(eq,isCancelPending,false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getIsCancelPending)
                .allSatisfy(eachisIsCancelPending -> assertThat(eachisIsCancelPending).isEqualTo(false));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualOperationForDateValue() throws Exception {
        String operation = "eq";
        String parameterName = "startTime";
        String filter = "(" + operation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        ProblemDetails problemDetails = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation,
                                                                       parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForInOperatorOnEnumerationDataType() throws Exception {
        String filter = "(in,operation,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationEnum.INSTANTIATE));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getOperation).containsOnly(VnfLcmOpOcc.OperationEnum.INSTANTIATE);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForInOperatorOnDateDataType() throws Exception {
        String filterOperation = "in";
        String parameterName = "startTime";
        String filter = "(" + filterOperation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       filterOperation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForInOperatorOnStringDataType() throws Exception {
        String id1 = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String id2 = "h08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(in,id,%s,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id1, id2));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).containsOnly(id1, id2);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForInOperatorOnBooleanDataType() throws Exception {
        String operation = "in";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotInOperatorOnEnumerationDataType() throws Exception {
        String filter = "(nin,operation,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, VnfLcmOpOcc.OperationEnum.INSTANTIATE));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getOperation).doesNotContain(VnfLcmOpOcc.OperationEnum.INSTANTIATE);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotInOperatorOnDateDataType() throws Exception {
        String filterOperation = "nin";
        String parameterName = "startTime";
        String filter = "(" + filterOperation + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       filterOperation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotInOperatorOnStringDataType() throws Exception {
        String id1 = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String id2 = "h08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(nin,id,%s,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id1, id2));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).doesNotContain(id1, id2);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotInOperatorOnBooleanDataType() throws Exception {
        String operation = "nin";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation,
                                                                       parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOperatorOnStringDataType() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(lt,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).allSatisfy(eachId -> assertThat(eachId).isLessThan(id));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOperatorOnDateDataType() throws Exception {
        String now = LocalDateTime.now().toString();
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(lt,startTime," + now + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOperatorOnEnumerationDataType() throws Exception {
        String operation = "lt";
        String parameterName = "operation";
        String filter = "(" + operation + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOperatorOnBooleanDataType() throws Exception {
        String operation = "lt";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanEqualOperatorOnStringDataType() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(lte,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getId)
                .allSatisfy(eachId -> assertThat(eachId).isLessThanOrEqualTo(id));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanEqualOperatorOnDateDataType() throws Exception {
        String now = LocalDateTime.now().toString();
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(lte,startTime," + now + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanEqualOperatorOnEnumerationDataType() throws Exception {
        String operation = "lte";
        String parameterName = "operation";
        String filter = "(" + operation + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanEqualOperatorOnBooleanDataType() throws Exception {
        String operation = "lte";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterOperatorOnStringDataType() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(gt,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getId)
                .allSatisfy(eachId -> assertThat(eachId).isGreaterThan(id));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterOperatorOnDateDataType() throws Exception {
        String startTime = "2012-09-17T18:47:52";
        String filter = "(gt,startTime,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, startTime));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterOperatorOnEnumerationDataType() throws Exception {
        String operation = "gt";
        String parameterName = "operation";
        String filter = "(" + operation + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterOperatorOnBooleanDataType() throws Exception {
        String operation = "gt";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanEqualOperatorOnStringDataType() throws Exception {
        String id = "c08fcbc8-474f-4673-91ee-761fd83991e6";
        String filter = "(gt,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getId)
                .allSatisfy(eachId -> assertThat(eachId).isGreaterThanOrEqualTo(id));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanEqualOperatorOnDateDataType() throws Exception {
        String startTime = "2012-09-17T18:47:52";
        String filter = "(gt,startTime,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, startTime));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanEqualOperatorOnEnumerationDataType() throws Exception {
        String operation = "gte";
        String parameterName = "operation";
        String filter = "(" + operation + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanEqualOperatorOnBooleanDataType() throws Exception {
        String operation = "gte";
        String parameterName = "isCancelPending";
        String filter = "(" + operation + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operation, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForContainsOperatorOnStringDataType() throws Exception {
        String id = "c0";
        String filter = "(cont,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).allSatisfy(eachId -> assertThat(eachId).contains(id));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForContainsOperatorOnDateDataType() throws Exception {
        String operator = "cont";
        String parameterName = "startTime";
        String filter = "(" + operator + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForContainsOperatorOnEnumerationDataType() throws Exception {
        String operator = "cont";
        String parameterName = "operation";
        String filter = "(" + operator + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForContainsOperatorOnBooleanDataType() throws Exception {
        String operator = "cont";
        String parameterName = "isCancelPending";
        String filter = "(" + operator + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotContainsOperatorOnStringDataType() throws Exception {
        String id = "c0";
        String filter = "(ncont,id,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, id));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation).extracting(VnfLcmOpOcc::getId).doesNotContain(id);
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotContainsOperatorOnDateDataType() throws Exception {
        String operator = "ncont";
        String parameterName = "startTime";
        String filter = "(" + operator + "," + parameterName + ",2012-09-17T18:47:52)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotContainsOperatorOnEnumerationDataType() throws Exception {
        String operator = "ncont";
        String parameterName = "operation";
        String filter = "(" + operator + "," + parameterName + "," + VnfLcmOpOcc.OperationEnum.INSTANTIATE + ")";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotContainsOperatorOnBooleanDataType() throws Exception {
        String operator = "ncont";
        String parameterName = "isCancelPending";
        String filter = "(" + operator + "," + parameterName + ",false)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", filter);
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.OPERATION_NOT_SUPPORTED_FOR_KEY_ERROR_MESSAGE,
                                                                       operator, parameterName));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForEqualsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(eq,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotEqualsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(neq,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isNotEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(gt,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isGreaterThan(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForGreaterThanOrEqualOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(gte,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isGreaterThanOrEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(lt,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isLessThan(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForLessThanOrEqualOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(lte,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isLessThanOrEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForInOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(in,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotInOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        String filter = "(nin,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().isNotEqualTo(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForContainsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3de";
        String filter = "(cont,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().contains(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationWithFilterForNotContainsOperationOnVnfInstanceId() throws Exception {
        String vnfInstanceId = "e3de";
        String filter = "(ncont,vnfInstanceId,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, vnfInstanceId));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.OK, queries);
        List<VnfLcmOpOcc> allOperation = getLifecycleOperations(mvcResult);
        assertThat(allOperation)
                .extracting(VnfLcmOpOcc::getVnfInstanceId)
                .allSatisfy(eachvnfInstanceId -> assertThat(eachvnfInstanceId).isNotNull().isNotEmpty().doesNotContain(vnfInstanceId));
    }

    @Test
    public void testGetAllLifeCycleOperationInvalidOperation() throws Exception {
        String filter = "(ont,vnfInstanceId,e3de)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(" + filter + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.INVALID_OPERATION_ERROR_MESSAGE, filter));
    }

    @Test
    public void testGetAllLifeCycleOperationInvalidParameter() throws Exception {
        String filter = "cont,test,e3de";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(" + filter + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.FILTER_NOT_SUPPORTED_ERROR_MESSAGE, filter));
    }

    @Test
    public void testGetAllLifeCycleOperationInvalidBooleanValueProvided() throws Exception {
        String value = "test";
        String filter = "(eq,isCancelPending,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, value));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.INVALID_BOOLEAN_VALUE_ERROR_MESSAGE, value));
    }

    @Test
    public void testGetAllLifeCycleOperationInvalidDateValueProvided() throws Exception {
        String value = "test";
        String filter = "(gt,startTime,%s)";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format(filter, value));
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo(String.format(FilterErrorMessage.INVALID_DATE_VALUE_ERROR_MESSAGE, "test"));
    }

    @Test
    public void testGetAllLifeCycleOperationInvalidFilter() throws Exception {
        String filter = "gt,startTime";
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(" + filter + ")");
        MvcResult mvcResult = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail()).isEqualTo("Invalid filter value provided " + filter);
    }

    @Test
    public void testGetAllLifeCycleOperationFailsPageLimitExceeded() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        final List<LifecycleOperation> allLifecycleOperation = lifecycleOperationRepository.findAll();
        int size = (int) Math.ceil(allLifecycleOperation.size() / (double)DEFAULT_PAGE_SIZE);
        int requestedPage = size+1;
        requestParams.add(NEXTPAGE_OPAQUE_MARKER, String.valueOf(requestedPage));
        MvcResult result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Requested page number exceeds the total number of pages. Requested page:: %s. Total page size:: %s", requestedPage, size);
    }

    @Test
    public void testGetAllLifeCycleOperationFailsInvalidPageParams() throws Exception {
        // Invalid page value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add(NEXTPAGE_OPAQUE_MARKER, "one");
        MvcResult result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page value for nextpage_opaque_marker:: one");

        // Negative page value
        requestParams.set(NEXTPAGE_OPAQUE_MARKER, "-1");
        result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);
        responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page number:: -1, page number must be greater than 0");
    }

    @Test
    public void testGetAllLifeCycleOperationFailsInvalidSizeParams() throws Exception {
        // Invalid size value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("size", "ten");
        getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);

        // Negative size value
        requestParams.set("size", "-10");
        MvcResult result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo("Invalid Pagination Query Parameter Exception");
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page size:: -10, page size must be greater than 0");
    }

    @Test
    public void testGetAllLifeCycleOperationFailsInvalidSortFields() throws Exception {
        // Invalid sort field single without order
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "stateEnteredTime123");
        checkSortFieldFailure(requestParams, "stateEnteredTime123");

        // Invalid sort field single with order
        requestParams.clear();
        requestParams.add("sort", "stateEnteredTime123,asc");
        checkSortFieldFailure(requestParams, "stateEnteredTime123");

        // Invalid sort field multi only one order
        requestParams.clear();
        requestParams.add("sort", "stateEnteredTime123,asc");
        requestParams.add("sort", "operation");
        checkSortFieldFailure(requestParams, "stateEnteredTime123");

        // Invalid sort field multi both with order
        requestParams.clear();
        requestParams.add("sort", "stateEnteredTime123,asc");
        requestParams.add("sort", "operation,asc");
        checkSortFieldFailure(requestParams, "stateEnteredTime123");

        // Invalid sort field multi both field invalid
        requestParams.clear();
        requestParams.add("sort", "operationInvalid,asc");
        requestParams.add("sort", "stateEnteredTime123,asc");
        checkSortFieldFailure(requestParams, "operationInvalid");
    }

    @Test
    public void testGetAllLifeCycleOperationFailsInvalidSortValues() throws Exception {
        // Invalid sort value single
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "stateEnteredTime,ascending");
        checkSortValueFailure(requestParams, "ascending");

        // Invalid sort value multi one invalid
        requestParams.clear();
        requestParams.add("sort", "operationState,descending");
        requestParams.add("sort", "stateEnteredTime");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi both invalid
        requestParams.clear();
        requestParams.add("sort", "operationState,descending");
        requestParams.add("sort", "stateEnteredTime,ascending");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi same sort one invalid
        requestParams.clear();
        requestParams.add("sort", "operationState,descending");
        requestParams.add("sort", "operationState,asc");
        requestParams.add("sort", "stateEnteredTime");
        checkSortValueFailure(requestParams, "descending");
    }

    @Test
    public void testGetAllVnfOperationOccurrencesFailsWithExceededPageSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.set("size", "250");
        MvcResult result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, queries);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Total size of the results will be shown cannot be more than 100. Requested page size 250");
    }

    @Test
    public void testfailLifecycleManagementOperationById() throws Exception {
        verifyOperationState(VNF_FAILED_TEMP, LifecycleOperationState.FAILED_TEMP);
        MvcResult mvcResult = postMvcResult(GET_ALL_VNF_OP_OCC + "/" + VNF_FAILED_TEMP + "/fail", HttpStatus.OK);
        final String response = mvcResult.getResponse().getContentAsString();
        VnfLcmOpOcc opertion = new ObjectMapper().readValue(response, VnfLcmOpOcc.class);
        assertThat(opertion.getOperationState()).hasToString(LifecycleOperationState.FAILED.toString());
        verifyOperationState(VNF_FAILED_TEMP, LifecycleOperationState.FAILED);
        verify(databaseInteractionService, times(1)).saveTasksInNewTransaction(any());
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.UPDATE_PACKAGE_STATE));
        verify(databaseInteractionService, times(1)).deleteTasksByVnfInstanceAndTaskName(any(),
                                                                                         eq(TaskName.SEND_NOTIFICATION));
    }

    @Test
    public void testFailLifecycleManagementOperationByIdThrowsException() throws Exception {
        verifyOperationState(VNF_FAILED_TEMP_PROCESSING, LifecycleOperationState.PROCESSING);
        MvcResult mvcResult = postMvcResult(GET_ALL_VNF_OP_OCC + "/" + VNF_FAILED_TEMP_PROCESSING + "/fail", HttpStatus.CONFLICT);
        final String response = mvcResult.getResponse().getContentAsString();
        ProblemDetails problemDetails = new ObjectMapper().readValue(response, ProblemDetails.class);
        assertThat(problemDetails.getDetail())
                .isEqualTo("Operation state has to be in FAILED_TEMP in order to rollback/fail the operation");
        verifyOperationState(VNF_FAILED_TEMP_PROCESSING, LifecycleOperationState.PROCESSING);
        verify(databaseInteractionService, times(0)).saveTasksInNewTransaction(any());
    }

    private void verifyOperationState(final String operationId, final LifecycleOperationState state) {
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(operationId);
        assertThat(byOperationOccurrenceId.getOperationState()).isEqualTo(state);
    }

    private List<VnfLcmOpOcc> getLifecycleOperations(final MvcResult mvcResult) throws JsonProcessingException, UnsupportedEncodingException {
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
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(vnfLcmOperationsController).build();
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private MvcResult postMvcResult(final String url, final HttpStatus expectedStatus) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(url).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID());
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
        MvcResult result = getMvcResult(GET_ALL_VNF_OP_OCC, HttpStatus.BAD_REQUEST, requestParams);
        String response = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(response, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        return response;
    }
}
