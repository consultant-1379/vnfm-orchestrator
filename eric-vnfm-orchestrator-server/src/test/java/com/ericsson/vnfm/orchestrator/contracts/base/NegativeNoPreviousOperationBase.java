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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LastOperationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ApplicationExceptionHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyServiceImpl;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"spring.flyway.enabled = false"})
public class NegativeNoPreviousOperationBase {

    @Mock
    private LifeCycleManagementService lifeCycleManagementService;

    @Mock
    private UsernameCalculationService usernameCalculationService;

    @Mock
    private DatabaseInteractionService databaseInteractionService;

    @Mock
    private IdempotencyServiceImpl idempotencyService;

    @InjectMocks
    private VnfInstancesControllerImpl vnfInstancesController;

    @BeforeEach
    public void setUp() {
        when(idempotencyService.executeTransactionalIdempotentCall(any(), any())).thenCallRealMethod();
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(new VnfInstance());

        given(lifeCycleManagementService.cleanup(any(), any(), anyString())).willThrow(new LastOperationException(
                "No previous operation found for instance last-op-not-existent"));

        doReturn("E2E_USERNAME").when(usernameCalculationService).calculateUsername();

        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(vnfInstancesController)
                                                   .setControllerAdvice(new ApplicationExceptionHandler()));
    }

    private VnfInstance buildVnfInstance() {
        VnfInstance instance = new VnfInstance();

        instance.setVnfInstanceId("last-op-not-existent");

        return instance;
    }
}
