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

import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfLcmOperationsController;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ApplicationExceptionHandler;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class GetVnfLcmOpOccByIdNegativeBase extends ContractTestRunner{

    @Autowired
    private VnfLcmOperationsController vnfLcmOperationsController;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @BeforeEach
    public void setUp() {
        given(databaseInteractionService.getLifecycleOperation(any())).willReturn(null);
        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(vnfLcmOperationsController)
                                                   .setControllerAdvice(new ApplicationExceptionHandler()));
    }
}
