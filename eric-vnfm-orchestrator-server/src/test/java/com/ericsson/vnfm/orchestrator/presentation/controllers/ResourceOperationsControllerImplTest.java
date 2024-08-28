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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.internal.operations.ResourceOperationsControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourceOperationsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.LocalDateMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ResourceOperationsMapperImpl;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
      ObjectMapper.class,
      ResourceOperationsMapperImpl.class,
      LocalDateMapper.class,
      ResourceOperationsControllerImpl.class,
      ResourceOperationsServiceImpl.class
})
@AutoConfigureMockMvc(addFilters = false)
@EnableWebMvc
@MockBean ({
      LifecycleOperationRepository.class,
      VnfInstanceRepository.class,
      ClusterConfigFileRepository.class,
      ClusterConfigInstanceRepository.class,
      VnfInstanceNamespaceDetailsRepository.class,
      ScaleInfoRepository.class,
      OperationsInProgressRepository.class
})
public class ResourceOperationsControllerImplTest {

    private static final String GET_OPERATIONS_URI = "/vnflcm/v1/operations";

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    private MvcResult mvcResult;

    @Test
    public void testGetAllOperations() throws Exception {
        when(databaseInteractionService.getAllOperations()).thenReturn(createDummyLifecycleOperationsList());

        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(GET_OPERATIONS_URI).accept(MediaType.APPLICATION_JSON)
                                            .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();

        List<OperationDetails> operationDetailsList = mapper.readValue(response, mapper.getTypeFactory()
                .constructCollectionType(List.class, OperationDetails.class));

        assertThat(operationDetailsList).isNotEmpty();
    }

    @Test
    public void testGetAllOperationsSorted() throws Exception {
        when(databaseInteractionService.getAllOperations()).thenReturn(createDummyLifecycleOperationsList());

        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(GET_OPERATIONS_URI).accept(MediaType.APPLICATION_JSON)
                                            .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();

        List<OperationDetails> operationDetailsList = new ObjectMapper().readValue(response, new
                TypeReference<List<OperationDetails>>(){});

        for (int i = 0; i < operationDetailsList.size()-1; i++) {
            assertThat(operationDetailsList.get(i).getTimestamp()).isAfterOrEqualTo(operationDetailsList.get(i+1).getTimestamp());
        }
    }

    private List<LifecycleOperation> createDummyLifecycleOperationsList() {
        return List.of(
              createDummyLifecycleOperation(1),
              createDummyLifecycleOperation(3),
              createDummyLifecycleOperation(5),
              createDummyLifecycleOperation(1),
              createDummyLifecycleOperation(7),
              createDummyLifecycleOperation(4),
              createDummyLifecycleOperation(3),
              createDummyLifecycleOperation(3));
    }

    private LifecycleOperation createDummyLifecycleOperation(int dayOfMonth){
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceName("dummyInstanceName");
        vnfInstance.setVnfInstanceId("dummyInstanceId");

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, Month.DECEMBER, dayOfMonth, 10, 20));
        return lifecycleOperation;
    }
}
