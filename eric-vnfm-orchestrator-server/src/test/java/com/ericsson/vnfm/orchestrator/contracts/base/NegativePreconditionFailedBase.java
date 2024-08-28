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

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class NegativePreconditionFailedBase extends AbstractDbSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("PRECONDITION_FAILED");
        instance.setNamespace("test");
        instance.setOperationOccurrenceId("CURRENT");
        instance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));

        LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationState(LifecycleOperationState.PROCESSING);
        operation.setVnfInstance(instance);

        OperationInProgress operationInProgress = new OperationInProgress();
        operationInProgress.setVnfId(instance.getVnfInstanceId());
        operationInProgress.setLifecycleOperationType(LifecycleOperationType.SYNC);

        given(lifecycleOperationRepository.findByOperationOccurrenceId(anyString())).willReturn(operation);
        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(instance));
        given(operationsInProgressRepository.findByVnfId(instance.getVnfInstanceId())).willReturn(Optional.of(operationInProgress));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
