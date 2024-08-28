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
package com.ericsson.vnfm.orchestrator.presentation.controllers.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.LAST_STATE_CHANGE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.WebConfiguration;
import com.ericsson.vnfm.orchestrator.model.PagedResourcesResponse;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.IdempotencyFilter;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;


@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
        controllers = EvnfmResourcesController.class,
        excludeAutoConfiguration = { SecurityAutoConfiguration.class },
        excludeFilters = @Filter(
                value = { WebConfiguration.class, HandlerInterceptor.class, RequestBodyAdviceAdapter.class,
                        IdempotencyFilter.class },
                type = FilterType.ASSIGNABLE_TYPE))
@MockBean(classes = {
        ScaleService.class,
        InstanceService.class,
        WorkflowRoutingService.class })
public class EvnfmResourcesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ResourcesService resourcesService;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private static final String GET_RESOURCES_URI = "/api/v1/resources";

    @Test
    public void verifyGetAllResources() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        performMvcRequestWithBasicAsserts(GET_RESOURCES_URI);

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       0,
                       15,
                       Sort.by(Sort.Direction.DESC, LAST_STATE_CHANGE));
    }

    @Test
    public void verifyGetAllResourcesWithPage() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        PagedResourcesResponse response = performMvcRequestWithBasicAsserts(GET_RESOURCES_URI + "?page=2");

        assertEquals(2, response.getPage().getNumber().intValue());

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       1,
                       15,
                       Sort.by(Sort.Direction.DESC, LAST_STATE_CHANGE));
    }

    @Test
    public void verifyGetAllResourcesWithPageAndSize() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(40));

        // when and then
        PagedResourcesResponse response = performMvcRequestWithBasicAsserts(GET_RESOURCES_URI + "?page=2&size=20");

        assertEquals(2, response.getPage().getNumber().intValue());
        assertEquals(20, response.getPage().getSize().intValue());

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       1,
                       20,
                       Sort.by(Sort.Direction.DESC, LAST_STATE_CHANGE));
    }

    @Test
    public void verifyGetAllResourcesWithPageAndSort() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        PagedResourcesResponse response = performMvcRequestWithBasicAsserts(GET_RESOURCES_URI + "?page=2&sort=vnfInstanceName");

        assertEquals(2, response.getPage().getNumber().intValue());

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       1,
                       15,
                       Sort.by(Sort.Direction.ASC, "vnfInstanceName"));
    }

    @Test
    public void verifyGetAllResourcesWithPageAndMappedSort() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        PagedResourcesResponse response = performMvcRequestWithBasicAsserts(
                GET_RESOURCES_URI + "?page=1&sort=lastLifecycleOperation/lifecycleOperationType");

        assertEquals(1, response.getPage().getNumber().intValue());

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       0,
                       15,
                       Sort.by(Sort.Direction.ASC, "lifecycleOperationType"));
    }

    @Test
    public void verifyGetAllResourcesWithPageAndComplexSort() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        PagedResourcesResponse response = performMvcRequestWithBasicAsserts(
                GET_RESOURCES_URI + "?page=2&sort=lastStateChanged,desc&sort=vnfInstanceName");

        assertEquals(2, response.getPage().getNumber().intValue());

        verify(resourcesService).getVnfResourcesPage(isNull(), eq(false), pageableCaptor.capture());

        assertPageable(pageableCaptor.getValue(),
                       1,
                       15,
                       Sort.by(Sort.Order.desc("lastStateChanged"), Sort.Order.asc("vnfInstanceName")));
    }

    @Test
    public void verifyGetAllResourcesWithFilter() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(withResourcesResponse(30));

        // when and then
        performMvcRequestWithBasicAsserts(
                GET_RESOURCES_URI + "?filter=(cont,vnfInstanceName,test)&sort=lastStateChanged,desc&sort=vnfInstanceName");

        verify(resourcesService).getVnfResourcesPage(eq("(cont,vnfInstanceName,test)"), eq(false), any());
    }

    @Test
    public void shouldFailOnInvalidSort() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + "?sort=invalidField,desc"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void shouldFailOnInvalidFilter() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Filter not supported (cont,vnfResourceName,test)"));

        // when and then
        mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + "?filter=(cont,vnfResourceName,test)"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void shouldFailOnInvalidPageSize() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + "?size=0"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void shouldFailOnInvalidPageNumber() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + "?page=0"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void shouldFailOnOutOfRangePageNumber() throws Exception {
        // given
        when(resourcesService.getVnfResourcesPage(any(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 30));

        // when and then
        mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + "?page=50"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    private static Answer<Object> withResourcesResponse(final int total) {
        // third argument passed to resourcesService mock is a Pageable object
        return invocation -> getResourcesResponse(invocation.getArgument(2), total);
    }

    private static Page<ResourceResponse> getResourcesResponse(final Pageable pageable, final int total) {
        final List<ResourceResponse> resourceResponseList = new ArrayList<>();
        for (int i = 0; i < pageable.getPageSize(); i++) {
            ResourceResponse response = new ResourceResponse();
            response.setInstanceId("instance-id-" + i);
            response.vnfInstanceName("instance-name-" + i);
            resourceResponseList.add(response);
        }

        return new PageImpl<>(resourceResponseList, pageable, total);
    }

    private PagedResourcesResponse performMvcRequestWithBasicAsserts(String uri) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(uri))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        String responseContent = mvcResult.getResponse().getContentAsString();
        assertThat(responseContent).isNotBlank();
        PagedResourcesResponse response = mapper.readValue(responseContent, PagedResourcesResponse.class);
        assertNotNull(response.getItems());
        return response;
    }

    private static void assertPageable(final Pageable actualPageable, final int pageNumber, final int pageSize, final Sort sort) {
        assertThat(actualPageable.getPageNumber()).isEqualTo(pageNumber);
        assertThat(actualPageable.getPageSize()).isEqualTo(pageSize);
        assertThat(actualPageable.getSort()).isEqualTo(sort);
    }
}
