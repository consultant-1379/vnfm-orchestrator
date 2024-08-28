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
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PatchVnfInfoPositiveBase extends ContractTestRunner {

    @MockBean
    private LifeCycleManagementService lifeCycleManagementService;

    @Autowired
    protected WebApplicationContext context;

    @BeforeEach
    public void setup() {
        given(lifeCycleManagementService.executeRequest(any(), anyString(), any(VnfInfoModificationRequest.class), anyString(), anyMap())).willReturn(
                "5f43fb8e-1316-468a-9f9c-b375e5d82094");
        RestAssuredMockMvc.webAppContextSetup(this.context);
    }
}
