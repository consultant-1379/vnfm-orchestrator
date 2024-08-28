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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class NegativeOperationNotFoundBase extends AbstractDbSetupTest {

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationState(FAILED);
        given(lifecycleOperationRepository.findByOperationOccurrenceId(anyString())).willReturn(null);
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}

