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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class NegativeInstantiateInSameNamespaceBase extends AbstractDbSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        value.setVnfInstanceId("same-namespace");
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(value);
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(value);
        given(vnfInstanceRepository.findById("same-namespace")).willReturn(Optional.of(value));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
