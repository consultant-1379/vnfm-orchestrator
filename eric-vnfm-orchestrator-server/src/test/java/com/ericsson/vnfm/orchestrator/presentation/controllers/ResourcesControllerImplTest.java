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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NO_LIFE_CYCLE_OPERATION_FOUND;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.WebConfiguration;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.IdempotencyFilter;
import com.ericsson.vnfm.orchestrator.presentation.controllers.resources.ResourcesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
        controllers = ResourcesControllerImpl.class,
        excludeAutoConfiguration = { SecurityAutoConfiguration.class },
        excludeFilters = @Filter(
                value = { WebConfiguration.class, HandlerInterceptor.class, RequestBodyAdviceAdapter.class,
                IdempotencyFilter.class },
                type = FilterType.ASSIGNABLE_TYPE))
@MockBean({
        InstanceService.class,
        WorkflowRoutingService.class })
public class ResourcesControllerImplTest {

    private static final String GET_RESOURCES_URI = "/vnflcm/v1/resources";
    private static final String GET_ALL_RESOURCES = "?getAllResources=true";
    private static final String PATH_SEPERATOR = "/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ResourcesService resourcesService;

    @MockBean
    private ScaleService scaleService;

    @Test
    public void verifyGetAllResources() throws Exception {
        // given
        final var resource = new VnfResource();
        resource.setInstanceId("instance-id");
        final var allResources = List.of(resource);

        when(resourcesService.getVnfResources(eq(true))).thenReturn(allResources);

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + GET_ALL_RESOURCES))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        List<VnfResource> actualAllResources = mapper.readValue(response, mapper.getTypeFactory()
                .constructCollectionType(List.class, VnfResource.class));
        assertThat(actualAllResources).extracting(VnfResource::getInstanceId).contains("instance-id");
    }

    @Test
    public void verifyGetAllResourcesWithLifeCycleOperation() throws Exception {
        // given
        when(resourcesService.getVnfResources(eq(false))).thenReturn(List.of());

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        List<VnfResource> allResources = mapper.readValue(response, mapper.getTypeFactory()
                .constructCollectionType(List.class, VnfResource.class));
        assertThat(allResources).isEmpty();
    }

    @Test
    public void verifyGetResourceById() throws Exception {
        // given
        final var instanceId = "instance-id";
        final var username = "user-name";
        final var instantiateOssTopology = Map.of(
                "managedElementId", Map.of("type", "string", "required", "false", "default", "elementId"));

        final var operation = new VnfResourceLifecycleOperation();
        operation.setUsername(username);

        final var resource = new VnfResource();
        resource.setInstanceId(instanceId);
        resource.setLcmOperationDetails(List.of(operation));
        resource.setInstantiateOssTopology(instantiateOssTopology);

        when(resourcesService.getVnfResource(eq(instanceId), eq(false))).thenReturn(resource);

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                        GET_RESOURCES_URI + PATH_SEPERATOR + instanceId))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        VnfResource actualResource = mapper.readValue(response, VnfResource.class);
        assertThat(actualResource.getInstanceId()).isEqualTo(instanceId);
        assertThat(actualResource.getLcmOperationDetails()).extracting(VnfResourceLifecycleOperation::getUsername).contains(username);
        assertThat(actualResource.getInstantiateOssTopology()).isEqualTo(instantiateOssTopology);
    }

    @Test
    public void verifyGetResourceByIdWithNoLifeCycleOperation() throws Exception {
        // given
        final var instanceId = "instance-id";

        when(resourcesService.getVnfResource(eq(instanceId), eq(false))).thenReturn(null);

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                        GET_RESOURCES_URI + PATH_SEPERATOR + instanceId))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ProblemDetails error = mapper.readValue(response, ProblemDetails.class);
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getTitle()).isEqualTo("Not Found Exception");
        assertThat(error.getType().toString()).isEqualTo(TYPE_BLANK);
        assertThat(error.getDetail()).isEqualTo(instanceId + NO_LIFE_CYCLE_OPERATION_FOUND);
    }

    @Test
    public void verifyGetResourceByIdWithNoLifeCycleOperationGetAllResources() throws Exception {
        final var instanceId = "instance-id";

        final var resource = new VnfResource();
        resource.setInstanceId(instanceId);

        when(resourcesService.getVnfResource(eq(instanceId), eq(true))).thenReturn(resource);

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                        GET_RESOURCES_URI + PATH_SEPERATOR + instanceId + GET_ALL_RESOURCES))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        VnfResource actualResource = mapper.readValue(response, VnfResource.class);
        assertThat(actualResource.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    public void verifyGetResourceByIdWithInstanceIdNotPresent() throws Exception {
        final var instanceId = "instance-id";

        when(resourcesService.getVnfResource(eq(instanceId), eq(false))).thenThrow(new NotFoundException("Instance not found"));

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                        GET_RESOURCES_URI + PATH_SEPERATOR + instanceId))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        ProblemDetails error = mapper.readValue(response, ProblemDetails.class);
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getTitle()).isEqualTo("Not Found Exception");
        assertThat(error.getType().toString()).isEqualTo(TYPE_BLANK);
        assertThat(error.getDetail()).isEqualTo("Instance not found");
    }

    @Test
    public void verifyGetResourcesWithFilter() throws Exception {
        final var instanceId = "instance-id";
        final var filter = "(eq,lcmOperationDetails/operationState,FAILED)";

        final var resource = new VnfResource();
        resource.setInstanceId(instanceId);
        final var allResources = List.of(resource);

        when(resourcesService.getAllResourcesWithFilter(eq(filter))).thenReturn(allResources);

        // when and then
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                        GET_RESOURCES_URI + String.format("?filter=%s", filter)))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        List<VnfResource> actualResources = mapper.readValue(response, new TypeReference<>() {
        });
        assertThat(actualResources).extracting(VnfResource::getInstanceId).contains(instanceId);
    }

    @Test
    public void verifyGetVnfcScaleInfo() throws Exception {
        // given
        final var instanceId = "instance-id";

        final var instance = new VnfInstance();
        when(resourcesService.getInstanceWithoutOperations(instanceId)).thenReturn(instance);

        when(scaleService.getVnfcScaleInfoList(same(instance), eq(ScaleVnfRequest.TypeEnum.IN), eq(2), eq("Payload")))
                .thenReturn(List.of(new VnfcScaleInfo()));

        // when and then
        String vnfcScaleInfoParams = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(GET_RESOURCES_URI + PATH_SEPERATOR +
                                                                                 instanceId + PATH_SEPERATOR
                                                                                 + vnfcScaleInfoParams)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).contains("vnfcName");

        List<VnfcScaleInfo> vnfcScaleInfoList = mapper.readValue(response, new TypeReference<>() {
        });
        assertThat(vnfcScaleInfoList).isNotEmpty();
    }

    @Test
    public void verifyGetVnfcScaleInfoWithInstanceIdNotFound() throws Exception {
        // given
        final var instanceId = "instance-id";

        when(resourcesService.getInstanceWithoutOperations(eq(instanceId))).thenThrow(new NotFoundException("Instance not found"));

        // when and then
        String vnfcScaleInfoParams = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + instanceId + PATH_SEPERATOR + vnfcScaleInfoParams)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).contains("Instance not found");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithInstanceNotInstantiated() throws Exception {
        // given
        final var instanceId = "instance-id";

        final var instance = new VnfInstance();
        when(resourcesService.getInstanceWithoutOperations(instanceId)).thenReturn(instance);

        when(scaleService.getVnfcScaleInfoList(same(instance), eq(ScaleVnfRequest.TypeEnum.IN), eq(2), eq("Payload")))
                .thenThrow(new NotInstantiatedException(instance));

        // when and then
        String vnfcScaleInfoParams = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + instanceId + PATH_SEPERATOR + vnfcScaleInfoParams)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.CONFLICT.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).contains("is not in the INSTANTIATED state");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithAspectIdNotFound() throws Exception {
        // given
        final var instanceId = "instance-id";

        final var instance = new VnfInstance();
        when(resourcesService.getInstanceWithoutOperations(instanceId)).thenReturn(instance);

        when(scaleService.getVnfcScaleInfoList(same(instance), eq(ScaleVnfRequest.TypeEnum.IN), eq(2), eq("Payload")))
                .thenThrow(new IllegalArgumentException("Aspect id not found"));

        // when and then
        String vnfcScaleInfoParams = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + instanceId + PATH_SEPERATOR + vnfcScaleInfoParams)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();

        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).contains("Aspect id not found");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithNegativeStepsToScale() throws Exception {
        String vnfcScaleInfoParamsNegativeInteger = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=-1&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + "instance-id" + PATH_SEPERATOR + vnfcScaleInfoParamsNegativeInteger)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String responseNegativeInteger = mvcResult.getResponse().getContentAsString();
        assertThat(responseNegativeInteger).contains("Invalid scale step provided, Scale step should be a positive integer");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithInvalidStepsToScale() throws Exception {
        String vnfcScaleInfoParamsNotAnInteger = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=test&type=SCALE_IN";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + "instance-id" + PATH_SEPERATOR + vnfcScaleInfoParamsNotAnInteger)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String responseNotAnInteger = mvcResult.getResponse().getContentAsString();
        assertThat(responseNotAnInteger).contains("Invalid scale step provided, Scale step should be a positive integer");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithInvalidScaleType() throws Exception {
        String vnfcScaleInfoParams = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2&type=invalid_type";
        final var mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + "instance-id" + PATH_SEPERATOR + vnfcScaleInfoParams)
                                                      .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).contains("Unexpected value 'invalid_type'");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithMissingTypeParameter() throws Exception {
        String vnfcScaleInfoParamsWithoutType = "vnfcScaleInfo?aspectId=Payload&numberOfSteps=2";
        MvcResult mvcResultWithoutType = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + "instance-id" + PATH_SEPERATOR + vnfcScaleInfoParamsWithoutType)
                                                                 .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String responseWithoutType = mvcResultWithoutType.getResponse().getContentAsString();
        assertThat(responseWithoutType).contains("Required request parameter 'type' for method parameter type String is not present");
    }

    @Test
    public void verifyGetVnfcScaleInfoWithMissingAspectIdParameter() throws Exception {
        String vnfcScaleInfoParamsWithoutAspectId = "vnfcScaleInfo?numberOfSteps=2&type=SCALE_IN";
        MvcResult mvcResultWithoutAspectId = mockMvc.perform(MockMvcRequestBuilders.get(
                                GET_RESOURCES_URI + PATH_SEPERATOR + "instance-id" + PATH_SEPERATOR + vnfcScaleInfoParamsWithoutAspectId)
                                                                     .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String responseWithoutAspectId = mvcResultWithoutAspectId.getResponse().getContentAsString();
        assertThat(responseWithoutAspectId).contains("Required request parameter 'aspectId' for method parameter type String is not present");
    }
}
