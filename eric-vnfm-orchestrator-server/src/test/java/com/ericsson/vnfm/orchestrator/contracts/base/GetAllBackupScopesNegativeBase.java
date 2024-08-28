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

import static org.mockito.BDDMockito.given;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllBackupScopesNegativeBase extends ContractTestRunner{
    private static final String NOT_INSTANTIATED_ID = "g7def1ce-4cf4-477c-ahb3-61c454e6a344";
    private static final String BRO_SERVICE_URL_NOT_PROVIDED_ID = "wf1ce-rd45-477c-vnf0-backup001";
    private static final String NOT_FOUND_ID = "not-found";

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @BeforeEach
    public void setup() {
        NotFoundException exception = new NotFoundException(String.format("Vnf instance with id %s does not exist", NOT_FOUND_ID));

        given(databaseInteractionService.getVnfInstance(NOT_FOUND_ID)).willThrow(exception);

        given(databaseInteractionService.getVnfInstance(NOT_INSTANTIATED_ID)).willReturn(notInstantiatedVnfInstance());
        given(databaseInteractionService.getVnfInstance(BRO_SERVICE_URL_NOT_PROVIDED_ID)).willReturn(broServiceUrlNotProvidedVnfInstance());
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private VnfInstance notInstantiatedVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("g7def1ce-4cf4-477c-ahb3-61c454e6a344");
        vnfInstance.setInstantiationState(NOT_INSTANTIATED);
        return vnfInstance;
    }

    private VnfInstance broServiceUrlNotProvidedVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("wf1ce-rd45-477c-vnf0-backup001");
        vnfInstance.setInstantiationState(INSTANTIATED);
        return vnfInstance;
    }
}
