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

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class DeleteVnfIdentifierNegativeBase extends ContractTestRunner {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        given(vnfInstanceRepository.findById("already-instantiated"))
                .willReturn(Optional.of(getVnfInstance(InstantiationState.INSTANTIATED)));
        given(vnfInstanceRepository.findById("operation-in-progress"))
                .willReturn(Optional.of(getVnfInstance(InstantiationState.INSTANTIATED)));
        given(vnfInstanceRepository.findById("wrong-package-id"))
                .willReturn(Optional.of(getVnfInstance(InstantiationState.NOT_INSTANTIATED)));
        given(operationsInProgressRepository.findByVnfId("operation-in-progress"))
                .willReturn(Optional.of(getOperationInProgress()));
    }

    private VnfInstance getVnfInstance(InstantiationState instantiationState) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(instantiationState);
        return vnfInstance;
    }

    private OperationInProgress getOperationInProgress() {
        OperationInProgress operation = new OperationInProgress();
        operation.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        return operation;
    }
}
