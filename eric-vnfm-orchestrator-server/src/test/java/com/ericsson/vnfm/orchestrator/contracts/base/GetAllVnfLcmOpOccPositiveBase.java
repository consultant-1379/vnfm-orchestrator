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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;

public class GetAllVnfLcmOpOccPositiveBase extends ContractTestRunner {

    @Autowired
    private VnfLcmOperationService vnfLcmOperationService;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    Page<LifecycleOperation> lifecycleOperationPageMock;

    @MockBean
    private LifecycleOperationQuery lifecycleOperationQuery;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @BeforeEach
    public void setUp() {
        when(lifecycleOperationPageMock.getSize()).thenReturn(100);
        when(lifecycleOperationPageMock.getContent()).thenReturn(getAllOperations());
        when(lifecycleOperationPageMock.getTotalElements()).thenReturn(5L);
        when(lifecycleOperationPageMock.getTotalPages()).thenReturn(1);
        when(lifecycleOperationPageMock.getNumber()).thenReturn(0); // 0 Indexed
        when(databaseInteractionService.getAllOperations(any())).thenReturn(lifecycleOperationPageMock);
        when(lifecycleOperationQuery.getPageWithFilter(anyString(), any())).thenReturn(lifecycleOperationPageMock);
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<LifecycleOperation> getAllOperations() {

        List<LifecycleOperation> operationList = new ArrayList<>();

        for (int i = 1; i < 7; i++) {
            operationList.add(getLifeCycleOperation(String.valueOf(i)));
        }
        return operationList;
    }

    private LifecycleOperation getLifeCycleOperation(String suffix) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a37" + suffix);
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setOperationOccurrenceId("b08fcbc8-474f-4673-91ee-761fd83991e" + suffix);
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setAutomaticInvocation(false);
        lifecycleOperation.setCancelPending(false);
        lifecycleOperation.setCancelMode(null);
        lifecycleOperation.setGrantId(null);
        setError(null, lifecycleOperation);
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperation.setStateEnteredTime(LocalDateTime.of(2022, 11, 16, 22, 17, 55));
        lifecycleOperation.setStartTime(LocalDateTime.of(2022, 11, 16, 22, 17, 55));

        return lifecycleOperation;
    }

}
