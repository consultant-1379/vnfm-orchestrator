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

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;

import java.util.Optional;
import jakarta.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostBackupsNegativeBase extends ContractTestRunner {

    @Inject
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @BeforeEach
    public void setUp() {
        VnfInstance instanceWithoutUrl = getVnfInstance("wm8fcbc8-rd45-4673-oper-snapshot001");
        instanceWithoutUrl.setBroEndpointUrl(null);

        LifecycleOperation lifecycleOperation = getLifecycleOperation();

        given(vnfInstanceRepository.findById("wf1ce-rd45-477c-vnf0-backup001")).willReturn(Optional.of(instanceWithoutUrl));
        given(lifecycleOperationRepository.findByOperationOccurrenceId("wm8fcbc8-rd45-4673-oper-snapshot001")).willReturn(lifecycleOperation);

        VnfInstance instance = getVnfInstance("wm8fcbc8-rd45-4673-oper-snapshot004");

        LifecycleOperation failedOperation = getLifecycleOperation();
        failedOperation.setOperationState(FAILED);

        given(vnfInstanceRepository.findById("wf1ce-rd45-477c-vnf0-snapshot004")).willReturn(Optional.of(instance));
        given(lifecycleOperationRepository.findByOperationOccurrenceId("wm8fcbc8-rd45-4673-oper-snapshot004")).willReturn(failedOperation);

        VnfInstance notInstantiatedInstance = getVnfInstance("g7def1ce-4cf4-477c-ahb3-61c454e6a344");
        notInstantiatedInstance.setInstantiationState(NOT_INSTANTIATED);

        given(vnfInstanceRepository.findById("g7def1ce-4cf4-477c-ahb3-61c454e6a344")).willReturn(Optional.of(notInstantiatedInstance));

        VnfInstance backupInstance = getVnfInstance("wf1ce-rd45-477c-vnf0-backup003");

        LifecycleOperation backupOperation = getLifecycleOperation();

        given(vnfInstanceRepository.findById("wf1ce-rd45-477c-vnf0-backup003")).willReturn(Optional.of(backupInstance));
        given(lifecycleOperationRepository.findByOperationOccurrenceId("wf1ce-rd45-477c-vnf0-backup003")).willReturn(backupOperation);

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @NotNull
    private static LifecycleOperation getLifecycleOperation() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId("wm8fcbc8-rd45-4673-oper-snapshot003");
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        return lifecycleOperation;
    }

    @NotNull
    private static VnfInstance getVnfInstance(String operationId) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("g7def1ce-4cf4-477c-ahb3-61c454e6a344");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setCombinedValuesFile("{\"bro_endpoint_url\":\"http://snapshot-bro.test\"}");
        vnfInstance.setBroEndpointUrl("http://snapshot-bro.test");
        vnfInstance.setOperationOccurrenceId(operationId);
        return vnfInstance;
    }
}
