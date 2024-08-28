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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyServiceImpl;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class PostHealVnfPositiveBase {

    @Mock
    private LifeCycleManagementService lifeCycleManagementService;

    @Mock
    private InstanceService instanceService;

    @Mock
    private OperationsInProgressRepository operationsInProgressRepository;

    @Mock
    private UsernameCalculationService usernameCalculationService;

    @Mock
    private IdempotencyServiceImpl idempotencyService;

    @InjectMocks
    private VnfInstancesControllerImpl vnfInstancesController;

    @BeforeEach
    public void setUp() {
        when(idempotencyService.executeTransactionalIdempotentCall(any(), any())).thenCallRealMethod();

        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("test-id");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        given(lifeCycleManagementService.executeRequest(any(), anyString(), any(), anyString(), anyMap()))
                .willReturn("d807978b-13e2-478e-8694-5bedbf2145e2");

        doReturn("E2E_USERNAME").when(usernameCalculationService).calculateUsername();

        RestAssuredMockMvc.standaloneSetup(vnfInstancesController);
    }
}
