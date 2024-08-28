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

import java.net.URISyntaxException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetValuesNegativeBase extends ContractTestRunner {

    @MockBean
    private InstanceService instanceService;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}


