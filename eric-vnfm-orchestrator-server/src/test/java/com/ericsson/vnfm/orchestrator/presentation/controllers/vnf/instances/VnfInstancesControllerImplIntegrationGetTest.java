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
package com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_COLUMN;
import static com.ericsson.vnfm.orchestrator.TestUtils.ERROR_BODY_SORT_ORDER;
import static com.ericsson.vnfm.orchestrator.TestUtils.NEXTPAGE_OPAQUE_MARKER;
import static com.ericsson.vnfm.orchestrator.TestUtils.QUERY_PARAMETER_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkLinkHeader;
import static com.ericsson.vnfm.orchestrator.TestUtils.checkPaginationHeader;
import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse.InstantiationStateEnum;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.DEFAULT_PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS_ID;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.InstantiatedVnfInfo;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ComponentStatusResponseList;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.yaml.snakeyaml.constructor.SafeConstructor;
import wiremock.com.jayway.jsonpath.JsonPath;

@AutoConfigureMockMvc
public class VnfInstancesControllerImplIntegrationGetTest extends AbstractEndToEndTest {

    private static final String DB_VNF_ID_1 = "ef1ce-4cf4-477c-aab3-21c454e6a379";
    private static final String DB_VNF_ID_2 = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
    private static final String NON_EXISTENT_VNF_ID = "123456789";

    private static final String VNF_ID_RESPONSE = "\"id\":\"%s\"";
    private static final List<String> VALID_SORT_COLUMNS = Arrays.asList("vnfInstanceName", "instantiationState", "vnfProductName", "vnfdVersion",
                                                                         "vnfSoftwareVersion", "vnfProviderName", "clusterName");

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private InstanceService instanceService;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private PackageService packageService;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    public void init() {
        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_1);
        vnfInstance.setClusterName("my-cluster");
        vnfInstanceRepository.save(vnfInstance);
    }

    @Test
    public void testGetAllVnfsDefaultPage() throws Exception {
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsWithPage() throws Exception {
        // Second Page
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.set(NEXTPAGE_OPAQUE_MARKER, "2");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(DEFAULT_PAGE_SIZE);
        assertThat(vnfInstanceResponses.size()).isEqualTo(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 2, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 2, totalPages);
    }

    @Test
    public void testGetAllVnfsWithSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("size", "60");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(60);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, 60);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsWithFilter() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();

        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", "(eq,vnfInstanceName,my-release-name)");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(12);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsWithSort() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "vnfInstanceName,asc");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        // Check first 15 items
        assertThat(vnfInstanceResponses.subList(0, 15))
                .isNotNull().isNotEmpty()
                .extracting(VnfInstanceResponse::getVnfInstanceName)
                .isSortedAccordingTo(Comparator.naturalOrder());
        assertThat(vnfInstanceResponses.size()).isEqualTo(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsWithSortOtherPossibleCombinations() throws Exception {
        // Skip fetching all data
        doReturn(new PageImpl<>(new ArrayList<>())).when(instanceService).getVnfInstancePage(any(), any());
        doReturn(new ArrayList<>()).when(workflowRoutingService).getComponentStatusRequest(anyList());
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        for (String eachValidColumn : VALID_SORT_COLUMNS) {
            queries.add("sort", eachValidColumn);
            getMockMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
            queries.clear();
        }
        queries.add("sort", "vnfInstanceName,asc");
        queries.add("sort", "clusterName");
        getMockMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "vnfInstanceName");
        queries.add("sort", "clusterName");
        getMockMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        queries.clear();
        queries.add("sort", "vnfInstanceName,asc");
        queries.add("sort", "clusterName,desc");
        getMockMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
    }

    @Test
    public void testGetAllVnfsWithSizeAndSort() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("sort", "vnfInstanceName");
        queries.add("size", "20");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(20);
//                .extracting(VnfInstanceResponse::getVnfInstanceName)
//                .isSortedAccordingTo(Comparator.naturalOrder());

        queries.set("sort", "vnfInstanceName,asc");
        getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(20);
//                .extracting(VnfInstanceResponse::getVnfInstanceName)
//                .isSortedAccordingTo(Comparator.naturalOrder());

        queries.set("sort", "vnfInstanceName,desc");
        getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSize(20)
                .extracting(VnfInstanceResponse::getVnfInstanceName)
                .isSortedAccordingTo(Comparator.reverseOrder());
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, 20);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsWithPageAndSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add(NEXTPAGE_OPAQUE_MARKER, "2");
        queries.add("size", "70");
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses.size()).isEqualTo(70);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 2, 70);
        checkLinkHeader(getAllVnfs.getResponse(), 2, totalPages);

        queries.set(NEXTPAGE_OPAQUE_MARKER, "5");
        queries.set("size", "50");
        getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses.size()).isGreaterThanOrEqualTo(40);
        totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 5, 50);
        checkLinkHeader(getAllVnfs.getResponse(), 5, totalPages);
    }

    @Test
    public void testGetAllVnfsWithPageSizeAndFilter() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();

        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.set(NEXTPAGE_OPAQUE_MARKER, "2");
        queries.set("size", "10");
        queries.add("filter", String.format("(eq,instantiationState,%s)", InstantiationStateEnum.NOT_INSTANTIATED));
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses.size()).isEqualTo(10);
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 2, 10);
        checkLinkHeader(getAllVnfs.getResponse(), 2, totalPages);
    }

    @Test
    public void testGetAllVnfsWithPageSizeSortAndFilter() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();

        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.set(NEXTPAGE_OPAQUE_MARKER, "2");
        queries.set("size", "10");
        queries.set("sort", "vnfInstanceName,asc");
        queries.add("filter", String.format("(eq,instantiationState,%s)", InstantiationStateEnum.NOT_INSTANTIATED));
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        assertThat(vnfInstanceResponses)
                .isNotNull().isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(10)
                .extracting(VnfInstanceResponse::getVnfInstanceName)
                .isSortedAccordingTo(Comparator.naturalOrder());
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 2, 10);
        checkLinkHeader(getAllVnfs.getResponse(), 2, totalPages);
    }

    @Test
    public void testGetAllVnfsFailsPageLimitExceeded() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        final List<VnfInstance> allVnfInstances = vnfInstanceRepository.findAll();
        int size = (int) Math.ceil(allVnfInstances.size() / (double) DEFAULT_PAGE_SIZE);
        int nextPage = size + 1;
        requestParams.add(NEXTPAGE_OPAQUE_MARKER, String.valueOf(nextPage));
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Requested page number exceeds the total number of pages. Requested page:: %s. Total page size:: %s", nextPage, size);
    }

    @Test
    public void testGetAllVnfsFailsInvalidPageParams() throws Exception {
        // Invalid page value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add(NEXTPAGE_OPAQUE_MARKER, "one");
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page value for nextpage_opaque_marker:: one");

        // Negative page value
        requestParams.set(NEXTPAGE_OPAQUE_MARKER, "-1");
        result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page number:: -1, page number must be greater than 0");
    }

    @Test
    public void testGetAllVnfsFailsInvalidSizeParams() throws Exception {
        // Invalid size value
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("size", "ten");
        getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);

        // Negative size value
        requestParams.set("size", "-10");
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo("Invalid page size:: -10, page size must be greater than 0");
    }

    @Test
    public void testGetAllVnfsFailsInvalidSortFields() throws Exception {
        // Invalid sort field single without order
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "vnfInstanceNameee");
        checkSortFieldFailure(requestParams, "vnfInstanceNameee");

        // Invalid sort field single with order
        requestParams.clear();
        requestParams.add("sort", "vnfInstanceNameee,asc");
        checkSortFieldFailure(requestParams, "vnfInstanceNameee");

        // Invalid sort field multi only one order
        requestParams.clear();
        requestParams.add("sort", "vnfInstanceNameee,asc");
        requestParams.add("sort", "clusterName");
        checkSortFieldFailure(requestParams, "vnfInstanceNameee");

        // Invalid sort field multi both with order
        requestParams.clear();
        requestParams.add("sort", "vnfInstanceNameee,asc");
        requestParams.add("sort", "clusterName,asc");
        checkSortFieldFailure(requestParams, "vnfInstanceNameee");

        // Invalid sort field multi both field invalid
        requestParams.clear();
        requestParams.add("sort", "clusterNameInvalid,asc");
        requestParams.add("sort", "vnfInstanceNameee,asc");
        checkSortFieldFailure(requestParams, "clusterNameInvalid");
    }

    @Test
    public void testGetAllVnfsFailsInvalidSortValues() throws Exception {
        // Invalid sort value single
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("sort", "vnfInstanceName,ascending");
        checkSortValueFailure(requestParams, "ascending");

        // Invalid sort value multi one invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "vnfInstanceName");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi both invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "vnfInstanceName,ascending");
        checkSortValueFailure(requestParams, "descending");

        // Invalid sort value multi same sort one invalid
        requestParams.clear();
        requestParams.add("sort", "clusterName,descending");
        requestParams.add("sort", "clusterName,asc");
        requestParams.add("sort", "vnfProductName");
        checkSortValueFailure(requestParams, "descending");
    }

    @Test
    public void testGetAllVnfsFailsInvalidFilterParams() throws Exception {
        // Invalid filter attribute
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(eeq,vnfInstanceName,test-instance)");
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.detail")).contains("Invalid operation provided eeq,vnfInstanceName,test-instance");

        // Invalid filter field
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("filter", "(cont,vnfInstanceNameee,test-instance)");
        result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.detail")).contains("Filter cont,vnfInstanceNameee,test-instance not supported");
    }

    @Test
    public void testGetAllVnfsWithInvalidParams() throws Exception {
        // Non-existent query - Should return default page (first page)
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("invalidParam", "4");
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, requestParams);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(result);
        assertThat(vnfInstanceResponses.size()).isEqualTo(DEFAULT_PAGE_SIZE);
        int totalPages = checkPaginationHeader(result.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(result.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetAllVnfsFailsWithExceededPageNumber() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        final List<VnfInstance> allVnfInstances = vnfInstanceRepository.findAll();
        int size = (int) Math.ceil(allVnfInstances.size() / (double) DEFAULT_PAGE_SIZE);
        int nextPage = size + 1;
        requestParams.add(NEXTPAGE_OPAQUE_MARKER, String.valueOf(nextPage));
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Requested page number exceeds the total number of pages. Requested page:: %s. Total page size:: %s", nextPage, size);
    }

    @Test
    public void testGetAllVnfsFailsWithExceededPageSize() throws Exception {
        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.set("size", "150");
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, queries);
        String responseBody = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(responseBody, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        assertThat((String) JsonPath.read(responseBody, "$.detail")).isEqualTo(
                "Total size of the results will be shown cannot be more than 100. Requested page size 150");
    }

    @Test
    public void testFindAllVnfsWithFilterWithInstantiationStateAsNotInstantiated() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();

        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format("(eq,instantiationState,%s)", InstantiationStateEnum.NOT_INSTANTIATED));
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        for (VnfInstanceResponse instanceResponse : vnfInstanceResponses) {
            assertThat(instanceResponse.getInstantiationState())
                    .isEqualTo(InstantiationStateEnum.valueOf(InstantiationStateEnum.NOT_INSTANTIATED.name()));
        }
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testFindAllVnfsWithFilterWithInstantiationStateAsInstantiated() throws Exception {
        whenOnboardingRespondsWithSupportedOperations();

        MultiValueMap<String, String> queries = new LinkedMultiValueMap<>();
        queries.add("filter", String.format("(eq,instantiationState,%s)", InstantiationStateEnum.INSTANTIATED));
        MvcResult getAllVnfs = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.OK, queries);
        List<VnfInstanceResponse> vnfInstanceResponses = getMockHttpServletResponseAsList(getAllVnfs);
        for (VnfInstanceResponse instanceResponse : vnfInstanceResponses) {
            assertThat(instanceResponse.getInstantiationState())
                    .isEqualTo(InstantiationStateEnum.valueOf(InstantiationStateEnum.INSTANTIATED.name()));
        }
        int totalPages = checkPaginationHeader(getAllVnfs.getResponse(), 1, DEFAULT_PAGE_SIZE);
        checkLinkHeader(getAllVnfs.getResponse(), 1, totalPages);
    }

    @Test
    public void testGetInstantiatedVnfWithScaling() throws Exception {
        MvcResult getById = getMvcResult(REST_URL + REST_URL_VNFS_ID + "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2");
        String response = getById.getResponse().getContentAsString();
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(response, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse.getInstantiatedVnfInfo()).isNotNull();
        InstantiatedVnfInfo instantiatedVnfInfo = vnfInstanceResponse.getInstantiatedVnfInfo();
        assertThat(instantiatedVnfInfo.getScaleStatus()).hasSize(2).extracting("aspectId").contains("Payload", "Payload_2");
        assertThat(instantiatedVnfInfo.getScaleStatus()).extracting("scaleLevel").containsExactly(0, 0);
    }

    @Test
    public void testFindVnfById() throws Exception {
        whenWfsPodsRespondsWith("component-status-response.json");

        MvcResult getById = getMvcResult(REST_URL + REST_URL_VNFS_ID + "ef1ce-4cf4-477c-aab3-65da6d5w");
        String response = getById.getResponse().getContentAsString();
        final InputStream vnfInstanceResponseStream = this.getClass().getResourceAsStream("/vnfResponseData/vnfInstanceResponse.json");
        VnfInstanceResponse vnfInstanceResponse = mapper.readValue(response, VnfInstanceResponse.class);
        VnfInstanceResponse vnfInstanceResponseExpected = mapper.readValue(vnfInstanceResponseStream, VnfInstanceResponse.class);
        assertThat(vnfInstanceResponse).isEqualTo(vnfInstanceResponseExpected);
    }

    @Test
    public void testFindVnfByNonExistentId() throws Exception {
        MvcResult getById = getMvcResult(REST_URL + REST_URL_VNFS_ID + NON_EXISTENT_VNF_ID);
        String response = getById.getResponse().getContentAsString();
        assertThat(response).contains("Vnf instance with id " + NON_EXISTENT_VNF_ID + " does not exist");
    }

    @Test
    public void shouldGetValuesFile() throws Exception {
        String vnfId = "values-4cf4-477c-aab3-21c454e6a380";
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(REST_URL + REST_URL_VNFS_ID + vnfId + "/values")
                .accept(MediaType.TEXT_PLAIN);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentType()).isNotNull().isEqualTo("text/plain");
        assertThat(mvcResult.getResponse().getContentAsString()).isNotBlank();
        checkIfValuesContentCorrect(mvcResult.getResponse());
    }

    private void checkIfValuesContentCorrect(MockHttpServletResponse response) throws UnsupportedEncodingException {
        byte[] responseInByteArray = response.getContentAsByteArray();
        final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        final Map<String, Object> responseMap = yaml.load(new ByteArrayInputStream(responseInByteArray));
        assertThat(EVNFM_PARAMS.stream().anyMatch(responseMap::containsKey)).isFalse();
        assertThat(responseMap.toString()).isEqualTo("{Payload_InitialDelta={replicaCount=3}, Payload_InitialDelta_1={replicaCount=1}}");
        assertThat(EVNFM_PARAMS.stream().anyMatch(responseMap::containsKey)).isFalse();
        Assertions.assertThat(response.getContentAsString()).contains("# Aspects and Current Scale level\n"
                                                                              + "# Aspect1: 3\n"
                                                                              + "# Aspect2: 3");
    }

    private MvcResult getMvcResult(String urlToQuery) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(urlToQuery).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andReturn();
    }

    private MvcResult getMvcResult(final String url, final HttpStatus expectedStatus, final MultiValueMap<String, String> queryParams)
    throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).queryParams(queryParams).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private MvcResult getMockMvcResult(String url, final HttpStatus expectedStatus, MultiValueMap<String, String> queryParams) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).queryParams(queryParams).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value())).andReturn();
    }

    private List<VnfInstanceResponse> getMockHttpServletResponseAsList(final MvcResult result) throws
            JsonProcessingException, UnsupportedEncodingException {
        String responseAsString = result.getResponse().getContentAsString();
        return new ObjectMapper()
                .readValue(responseAsString, new TypeReference<>() {
                });
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
        MvcResult result = getMvcResult(REST_URL + REST_URL_VNFS, HttpStatus.BAD_REQUEST, requestParams);
        String response = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(response, "$.title")).isEqualTo(QUERY_PARAMETER_EXCEPTION);
        return response;
    }

    private void whenOnboardingRespondsWithSupportedOperations() {
        doReturn(createSupportedOperations(LCMOperationsEnum.INSTANTIATE,
                                           LCMOperationsEnum.TERMINATE,
                                           LCMOperationsEnum.CHANGE_VNFPKG,
                                           LCMOperationsEnum.ROLLBACK,
                                           LCMOperationsEnum.SCALE,
                                           LCMOperationsEnum.SYNC,
                                           LCMOperationsEnum.MODIFY_INFO,
                                           LCMOperationsEnum.HEAL))
                .when(packageService).getSupportedOperations(anyString());
    }

    private void whenWfsPodsRespondsWith(final String fileName) {
        when(restTemplate.exchange(contains("additionalResourceInfo"), eq(HttpMethod.POST), any(), eq(ComponentStatusResponseList.class)))
                .thenReturn(new ResponseEntity<>(readObject(fileName, ComponentStatusResponseList.class), HttpStatus.OK));
    }

    private <T> T readObject(final String fileName, final Class<T> targetClass) {
        try {
            return mapper.readValue(readDataFromFile(getClass(), fileName), targetClass);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
