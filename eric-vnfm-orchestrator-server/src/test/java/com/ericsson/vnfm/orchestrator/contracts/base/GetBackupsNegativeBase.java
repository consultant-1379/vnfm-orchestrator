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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetBackupsNegativeBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @BeforeEach
    public void setUp() {
        VnfInstance noUrlVnfInstanceMock = new VnfInstance();
        noUrlVnfInstanceMock.setVnfInstanceId("wf1ce-rd45-477c-vnf0-backup001");
        noUrlVnfInstanceMock.setInstantiationState(InstantiationState.INSTANTIATED);

        VnfInstance notInstantiatedVnfInstance = new VnfInstance();
        notInstantiatedVnfInstance.setVnfInstanceId("g7def1ce-4cf4-477c-ahb3-61c454e6a344");
        notInstantiatedVnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);

        when(vnfInstanceRepository.findById(anyString())).thenReturn(Optional.of(noUrlVnfInstanceMock));
        when(vnfInstanceRepository.findById(contains("not-found"))).thenReturn(Optional.empty());
        when(vnfInstanceRepository.findById(contains("61c454e6a344"))).thenReturn(Optional.of(notInstantiatedVnfInstance));

        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
