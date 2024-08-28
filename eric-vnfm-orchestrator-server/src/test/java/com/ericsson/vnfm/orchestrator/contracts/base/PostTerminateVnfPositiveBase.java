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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostTerminateVnfPositiveBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @SpyBean
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @BeforeEach
    public void setUp() {
        mockRepositories();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @BeforeAll
    public static void beforeAll() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterAll
    public static void afterAll() {
        TransactionSynchronizationManager.clear();
    }

    private void mockRepositories() {
        when(vnfInstanceRepository.findById(anyString()))
                .thenReturn(Optional.of(TestUtils.createVnfInstance(true)));

        when(clusterConfigFileRepository.findByName(anyString()))
                .thenReturn(Optional.of(new ClusterConfigFile()));

        doNothing().when(lifeCycleManagementHelper).persistNamespaceDetails(any(VnfInstance.class));

        when(lifecycleOperationRepository.save(any(LifecycleOperation.class)))
                .thenAnswer(invocation -> {
                    LifecycleOperation operation = invocation.getArgument(0, LifecycleOperation.class);
                    operation.setOperationOccurrenceId("d807978b-13e2-478e-8694-5bedbf2145e2");
                    return operation;
                });
    }
}
