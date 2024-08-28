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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.messaging.operations.RollbackService;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmAutoRollback;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Rollback;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.RollbackOperationFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.CcvpPatternCommandFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.LocalDateMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfLcmOpOccMapperImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    ReplicaDetailsMapper.class,
    LocalDateMapper.class,
    VnfLcmOpOccMapperImpl.class,
    LifeCycleManagementHelper.class,
    VnfLcmOperationServiceImpl.class,
    ObjectMapper.class,
    RollbackOperationFactory.class,
    EvnfmAutoRollback.class,
    MessageUtility.class,
    CcvpPatternCommandFactory.class,
    Rollback.class,
    WorkflowRoutingServicePassThrough.class,
    WorkflowRequestBodyBuilder.class,
    CryptoUtils.class
})
@MockBean(classes = {
    CryptoService.class,
    ScaleService.class,
    ValuesFileService.class,
    ChangePackageOperationDetailsRepository.class,
    ChangedInfoRepository.class,
    HelmChartHistoryServiceImpl.class,
    ScaleInfoRepository.class,
    InstanceService.class,
    ClusterConfigService.class,
    HelmClientVersionValidator.class,
    DatabaseInteractionService.class,
    LifecycleOperationQuery.class,
    RestTemplate.class,
    CcvpPatternTransformer.class,
    RollbackService.class
})
public class VnfLcmOperationServiceTest {

    public static final String VNF_LCM_OP_OCC_ID = "54321";
    public static final String VNF_INSTANCE_ID = "12345";
    public static final long OPERATION_TIME = 1196676930000L;
    public static final long OPERATION_STATE_TIME = 1196676990000L;
    @Autowired
    private VnfLcmOperationServiceImpl vnfLcmOperationService;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean(name = "wfsRoutingRetryTemplate")
    private RetryTemplate wfsRoutingRetryTemplate;
    @Test
    public void testCompletedStateWithOperationParams() {
        LifecycleOperation  lifecycleOperation = getLifeCycleOperations();
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);

        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID);

        assertThat(vnfLcmOpOcc.getId()).isEqualTo(VNF_LCM_OP_OCC_ID);
        assertThat(vnfLcmOpOcc.getVnfInstanceId()).isEqualTo(VNF_INSTANCE_ID);
        assertThat(vnfLcmOpOcc.getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.fromValue(LifecycleOperationState.PROCESSING.name()));
        assertThat(vnfLcmOpOcc.getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.fromValue(LifecycleOperationType.INSTANTIATE.name()));
        assertThat(vnfLcmOpOcc.getStartTime().getTime()).isEqualTo(OPERATION_TIME);
        assertThat(vnfLcmOpOcc.getStateEnteredTime().getTime()).isEqualTo(OPERATION_STATE_TIME);
    }

    @Test
    public void testCompletedStateWithNullOperationParams() {
        LifecycleOperation lifecycleOperation = getLifeCycleOperations();
        lifecycleOperation.setOperationParams(null);
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);
        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID);

        assertThat(vnfLcmOpOcc.getId()).isEqualTo(VNF_LCM_OP_OCC_ID);
        assertThat(vnfLcmOpOcc.getVnfInstanceId()).isEqualTo(VNF_INSTANCE_ID);
        assertThat(vnfLcmOpOcc.getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.fromValue(LifecycleOperationState.PROCESSING.name()));
        assertThat(vnfLcmOpOcc.getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.fromValue(LifecycleOperationType.INSTANTIATE.name()));
        assertThat(vnfLcmOpOcc.getStartTime().getTime()).isEqualTo(OPERATION_TIME);
        assertThat(vnfLcmOpOcc.getStateEnteredTime().getTime()).isEqualTo(OPERATION_STATE_TIME);
    }

    @Test
    public void testCompletedStateWithInvalidOperationParamsJson() {
        LifecycleOperation lifecycleOperation = getLifeCycleOperations();
        lifecycleOperation.setOperationParams("{\"flavourId\": \"dummyFlavourId\",\"instantiationLevelId\": \"dummyInstantiationLevelId\",\"vnfInstanceId\": \"12345\"");
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);

        assertThatThrownBy(() -> vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID))
            .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void testGetVnfLcmOpOccForNullResponse() {
        String vnfLcmOpOccId = "98765";
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(null);

        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(vnfLcmOpOccId);
        assertThat(vnfLcmOpOcc).isNull();
    }

    @Test
    public void testFailedStateForGetVnfLcmOpOcc() {
        String errorObj = "{ \"type\": \"about:blank\", \"title\": \"Onboarding Overloaded\", \"status\": 503, \"detail\": \"Onboarding service not "
            + "available\", \"instance\": \"\" }";
        LifecycleOperation lifecycleOperation = getLifeCycleOperations();
        lifecycleOperation.setOperationState(LifecycleOperationState.FAILED);
        setError(errorObj, lifecycleOperation);
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);
        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID);

        assertThat(vnfLcmOpOcc.getId()).isEqualTo(VNF_LCM_OP_OCC_ID);
        assertThat(vnfLcmOpOcc.getVnfInstanceId()).isEqualTo(VNF_INSTANCE_ID);
        assertThat(vnfLcmOpOcc.getError()).isInstanceOf(ProblemDetails.class);
        assertThat(vnfLcmOpOcc.getError().getTitle()).isEqualTo("Onboarding Overloaded");
        assertThat(vnfLcmOpOcc.getError().getStatus()).isEqualTo(503);
        assertThat(vnfLcmOpOcc.getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.fromValue(LifecycleOperationType.INSTANTIATE.name()));
        assertThat(vnfLcmOpOcc.getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.fromValue(LifecycleOperationState.FAILED.name()));
        assertThat(vnfLcmOpOcc.getStartTime().getTime()).isEqualTo(OPERATION_TIME);
        assertThat(vnfLcmOpOcc.getStateEnteredTime().getTime()).isEqualTo(OPERATION_STATE_TIME);
    }

    @Test
    public void testGetVnfLcmOpOccForInvalidJson() {
        String invalidOperationParam = "{\"flavourId\": \"dummyFlavourId\",\"instantiationLevelId\": \"dummyInstantiationLevelId\",\"vnfInstanceId\": \"12345\"";
        LifecycleOperation lifecycleOperation = getLifeCycleOperations();
        lifecycleOperation.setOperationParams(invalidOperationParam);
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);

        assertThatThrownBy(() -> vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID))
            .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    public void testFailedStateInWFSDuringHistoryService() {
        String errorObj = "Application Timeout";

        LifecycleOperation lifecycleOperation = getLifeCycleOperations();
        lifecycleOperation.setOperationState(LifecycleOperationState.FAILED);
        setError(errorObj, lifecycleOperation);
        when(databaseInteractionService.getLifecycleOperation(anyString())).thenReturn(lifecycleOperation);

        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(VNF_LCM_OP_OCC_ID);

        assertThat(vnfLcmOpOcc.getId()).isEqualTo(VNF_LCM_OP_OCC_ID);
        assertThat(vnfLcmOpOcc.getVnfInstanceId()).isEqualTo(VNF_INSTANCE_ID);
        assertThat(vnfLcmOpOcc.getError()).isInstanceOf(ProblemDetails.class);
        assertThat(vnfLcmOpOcc.getError().getStatus()).isEqualTo(400);
        assertThat(vnfLcmOpOcc.getError().getDetail()).isEqualTo("Application Timeout");
        assertThat(vnfLcmOpOcc.getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.fromValue(LifecycleOperationType.INSTANTIATE.name()));
        assertThat(vnfLcmOpOcc.getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.fromValue(LifecycleOperationState.FAILED.name()));
        assertThat(vnfLcmOpOcc.getStartTime().getTime()).isEqualTo(OPERATION_TIME);
        assertThat(vnfLcmOpOcc.getStateEnteredTime().getTime()).isEqualTo(OPERATION_STATE_TIME);
    }

    @Test
    public void testGetAllVnfLcmOpOcc() {
        List<LifecycleOperation> lifecycleOperationList = new ArrayList<>();
        lifecycleOperationList.add(getLifeCycleOperations());
        Page<LifecycleOperation> lifecycleOperationPage = new PageImpl<>(lifecycleOperationList);
        when(databaseInteractionService.getAllOperations(any(Pageable.class))).thenReturn(lifecycleOperationPage);
        Pageable pageable = new PaginationUtils.PageableBuilder().build();
        Page<LifecycleOperation> lifecycleOperations = vnfLcmOperationService.getAllLcmOperationsPage(null, pageable);
        List<VnfLcmOpOcc> vnfLcmOpOccs = vnfLcmOperationService.mapToVnfLcmOpOcc(lifecycleOperations);

        assertThat(vnfLcmOpOccs.size()).isGreaterThan(0);
        assertThat(vnfLcmOpOccs.get(0).getId()).isEqualTo(VNF_LCM_OP_OCC_ID);
        assertThat(vnfLcmOpOccs.get(0).getVnfInstanceId()).isEqualTo(VNF_INSTANCE_ID);
        assertThat(vnfLcmOpOccs.get(0).getOperationState()).isEqualTo(VnfLcmOpOcc.OperationStateEnum.fromValue(LifecycleOperationState.PROCESSING.name()));
        assertThat(vnfLcmOpOccs.get(0).getOperation()).isEqualTo(VnfLcmOpOcc.OperationEnum.fromValue(LifecycleOperationType.INSTANTIATE.name()));
        assertThat(vnfLcmOpOccs.get(0).getStartTime().getTime()).isEqualTo(OPERATION_TIME);
        assertThat(vnfLcmOpOccs.get(0).getStateEnteredTime().getTime()).isEqualTo(OPERATION_STATE_TIME);
    }

    @Test
    public void testGetAllVnfLcmOpOccForEmptyResponse() {
        when(databaseInteractionService.getAllOperations(any(Pageable.class))).thenReturn(new PageImpl<>(new ArrayList<>()));
        Pageable pageable = new PaginationUtils.PageableBuilder().build();
        Page<LifecycleOperation> lifecycleOperations = vnfLcmOperationService.getAllLcmOperationsPage(null, pageable);
        List<VnfLcmOpOcc> vnfLcmOpOccs = vnfLcmOperationService.mapToVnfLcmOpOcc(lifecycleOperations);

        assertThat(vnfLcmOpOccs.size()).isEqualTo(0);
    }

    private LifecycleOperation getLifeCycleOperations() {
        String operationParams = "{\"flavourId\": \"dummyFlavourId\",\"instantiationLevelId\": \"dummyInstantiationLevelId\",\"vnfInstanceId\": \"12345\"}";
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(VNF_INSTANCE_ID);

        LifecycleOperation lifecycleOperations = new LifecycleOperation();
        lifecycleOperations.setVnfInstance(vnfInstance);
        lifecycleOperations.setAutomaticInvocation(false);
        lifecycleOperations.setOperationState(LifecycleOperationState.PROCESSING);
        lifecycleOperations.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperations.setOperationOccurrenceId(VNF_LCM_OP_OCC_ID);
        lifecycleOperations.setStartTime(LocalDateTime.parse("2007-12-03T10:15:30"));
        lifecycleOperations.setStateEnteredTime(LocalDateTime.parse("2007-12-03T10:16:30"));
        lifecycleOperations.setOperationParams(operationParams);

        return lifecycleOperations;
    }
}
