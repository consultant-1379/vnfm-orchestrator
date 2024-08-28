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

import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ApplicationExceptionHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyServiceImpl;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"spring.flyway.enabled = false"})
public class NegativeInstantiationLevelIdBase extends AbstractDbSetupTest {
    @InjectMocks
    private VnfInstancesControllerImpl vnfInstancesController;
    @Mock
    private IdempotencyServiceImpl idempotencyService;
    @Mock
    private UsernameCalculationService usernameCalculationService;
    @Mock
    private LifeCycleManagementService lifeCycleManagementService;

    @BeforeEach
    public void setUp() throws IOException {
        when(idempotencyService.executeTransactionalIdempotentCall(any(), any())).thenCallRealMethod();
        doReturn("E2E_USERNAME").when(usernameCalculationService).calculateUsername();
        given(lifeCycleManagementService.executeRequest(any(), anyString(), any(), anyString(), anyMap()))
                .willThrow(new InvalidInputException("InstantiationLevelId: " +
                        "invalid-instantiation-level not present in VNFD."));
        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(vnfInstancesController)
                .setControllerAdvice(new ApplicationExceptionHandler()));
    }
}
