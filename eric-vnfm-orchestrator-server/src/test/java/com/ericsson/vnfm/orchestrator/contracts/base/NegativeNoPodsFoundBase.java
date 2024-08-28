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

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PodStatusException;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class NegativeNoPodsFoundBase extends AbstractDbSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;
    @MockBean
    private ResourcesService resourcesService;
    @MockBean
    private WorkflowRoutingService workflowRoutingService;
    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("testNOPODSFOUND");
        given(resourcesService.getInstanceWithoutOperations(anyString())).willReturn(instance);
        given(workflowRoutingService
                      .getComponentStatusRequest(any(VnfInstance.class))).willThrow(new PodStatusException(String.format(
                "No pods found for instance %s",
                instance.getVnfInstanceId()), HttpStatus.NOT_FOUND));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
