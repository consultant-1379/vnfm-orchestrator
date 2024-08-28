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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class PostChangeVnfPkgInfoPositiveBase extends AbstractDbSetupTest {

    @MockBean
    private LifeCycleManagementService lifeCycleManagementService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        given(operationsInProgressRepository.save(any(OperationInProgress.class))).willReturn(new OperationInProgress());
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("test-id");
        given(lifeCycleManagementService.executeRequest(any(), anyString(),
                                                        any(ChangeCurrentVnfPkgRequest.class), anyString(), anyMap())).willReturn("d807978b-13e2-478e-8694"
                                                                                                                                         + "-5bedbf2145e2");
        given(lifeCycleManagementService.executeRequest(any(), anyString(),
                                                        any(ChangeCurrentVnfPkgRequest.class), anyString(), anyMap()))
                .willReturn("d807978b-13e2-478e-8694-5bedbf2145e2");
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(vnfInstance);
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
