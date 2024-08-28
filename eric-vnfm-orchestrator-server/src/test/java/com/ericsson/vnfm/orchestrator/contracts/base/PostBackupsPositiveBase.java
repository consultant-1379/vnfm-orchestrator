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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostBackupsPositiveBase extends ContractTestRunner {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("test-id");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setCombinedValuesFile("{\"bro_endpoint_url\":\"http://snapshot-bro.test\"}");
        vnfInstance.setBroEndpointUrl("http://snapshot-bro.test");
        vnfInstance.setOperationOccurrenceId("wm8fcbc8-rd45-4673-oper-snapshot003");

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId("wm8fcbc8-rd45-4673-oper-snapshot003");
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);

        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(vnfInstance));
        given(lifecycleOperationRepository.findByOperationOccurrenceId(anyString())).willReturn(lifecycleOperation);
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"id\": \"54321\"}", HttpStatus.CREATED);
        given(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class),
                                    ArgumentMatchers.<Class<String>>any())).willReturn(responseEntity);
    }
}
