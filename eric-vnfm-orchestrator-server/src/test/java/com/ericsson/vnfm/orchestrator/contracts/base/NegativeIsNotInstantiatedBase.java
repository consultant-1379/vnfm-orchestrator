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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class NegativeIsNotInstantiatedBase extends GetInstanceNegativeBase {

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        instance.setVnfInstanceId("not-instantiated-vnfinstanceid");
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(instance);
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
