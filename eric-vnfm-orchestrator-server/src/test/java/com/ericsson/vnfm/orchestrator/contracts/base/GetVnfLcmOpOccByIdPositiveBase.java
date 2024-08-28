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

import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfLcmOperationsController;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_ID;
import static org.mockito.BDDMockito.given;

public class GetVnfLcmOpOccByIdPositiveBase extends ContractTestRunner{
    @Autowired
    private VnfLcmOperationsController vnfLcmOperationsController;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean
    private ChangedInfoRepository changedInfoRepository;

    @BeforeEach
    public void setUp() {
        given(databaseInteractionService.getLifecycleOperation("54321no_changed_info")).willReturn(getLifecycleOperationNoChangedInfo());
        final LifecycleOperation lifecycleOperationWithChangedInfo = getLifecycleOperationWithChangedInfo();
        final ChangedInfo changedInfo = getChangedInfo();
        given(databaseInteractionService.getLifecycleOperation("12345with_changed_info")).willReturn(lifecycleOperationWithChangedInfo);
        given(changedInfoRepository.findById(lifecycleOperationWithChangedInfo.getOperationOccurrenceId())).willReturn(Optional.of(changedInfo));

        RestAssuredMockMvc.standaloneSetup(vnfLcmOperationsController);
    }

    private LifecycleOperation getLifecycleOperationNoChangedInfo() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(DUMMY_INSTANCE_ID);

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId("54321no_changed_info");
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setAutomaticInvocation(true);
        lifecycleOperation.setCancelPending(false);
        lifecycleOperation.setStartTime(LocalDateTime.now());
        lifecycleOperation.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);

        return lifecycleOperation;
    }
    private LifecycleOperation getLifecycleOperationWithChangedInfo() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(DUMMY_INSTANCE_ID);

        ChangedInfo changedInfo = getChangedInfo();

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId("12345with_changed_info");
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setAutomaticInvocation(true);
        lifecycleOperation.setCancelPending(false);
        lifecycleOperation.setStartTime(LocalDateTime.now());
        lifecycleOperation.setStateEnteredTime(LocalDateTime.now());
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        changedInfo.setLifecycleOperation(lifecycleOperation);

        return lifecycleOperation;
    }

    private ChangedInfo getChangedInfo() {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceName("test-vnf");
        changedInfo.setVnfInstanceDescription("description");
        changedInfo.setMetadata("{ \"metadataKey\":\"metadataValue\"}");
        changedInfo.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Aspect1\":\"CISMControlled\",\"Aspect2\":\"ManualControlled\"}}");
        return changedInfo;
    }
}
