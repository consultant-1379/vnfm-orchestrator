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
package com.ericsson.vnfm.orchestrator.e2e.limitations;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.DEFAULT_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.TERMINAL_OPERATION_STATES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@Sql(value = "delete_test_data_for_e2e_lcm_operation_limitations_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "insert_test_data_for_e2e_lcm_operation_limitations_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class EndToEndLcmOperationLimitsTest extends AbstractEndToEndTest {

    private static final String RUNNING_VNF_INSTANCE_1 = "running-vnf-instance-1";
    private static final String RUNNING_VNF_INSTANCE_2 = "running-vnf-instance-2";
    private static final String RUNNING_VNF_INSTANCE_3 = "running-vnf-instance-3";
    private static final String RUNNING_VNF_INSTANCE_4 = "running-vnf-instance-4";

    private static final int LCM_OPERATIONS_LIMIT = 3;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LcmOperationsConfig lcmOperationsConfig;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(lcmOperationsConfig, "lcmOperationsLimit", LCM_OPERATIONS_LIMIT);
    }

    @AfterEach
    public void after() {
        flyway.clean();
    }

    @Test
    public void successfulConcurrentRunningInstantiatesWhenLimitReached() throws Exception {
        runInstantiateAndVerifyStatus(RUNNING_VNF_INSTANCE_1, "running-namespace-1");
        LifecycleOperation secondLifecycleOperation = runInstantiateAndVerifyStatus(RUNNING_VNF_INSTANCE_2, "running-namespace-2");
        runInstantiateAndVerifyStatus(RUNNING_VNF_INSTANCE_3, "running-namespace-3");

        runInstantiateAndVerifyLimitError(RUNNING_VNF_INSTANCE_4, "running-namespace-4");

        assertThat(operationsInProgressRepository.findByVnfId(RUNNING_VNF_INSTANCE_4)).isEmpty();
        assertThat(operationsInProgressRepository.count()).isEqualTo(3);
        assertThat(getRunningOperations()).containsExactlyInAnyOrderEntriesOf(Map.of(LifecycleOperationType.INSTANTIATE, 3L));

        completeWfsMessagesForLifecycleOperation(secondLifecycleOperation);
        final String secondLifecycleOperationId = secondLifecycleOperation.getOperationOccurrenceId();

        LifecycleOperation completedSecondOperation = lifecycleOperationRepository.findByOperationOccurrenceId(secondLifecycleOperationId);
        assertThat(completedSecondOperation.getOperationState()).isEqualTo(COMPLETED);

        assertThat(operationsInProgressRepository.count()).isEqualTo(2);
        assertThat(getRunningOperations()).containsExactlyInAnyOrderEntriesOf(Map.of(LifecycleOperationType.INSTANTIATE, 2L));

        runInstantiateAndVerifyStatus(RUNNING_VNF_INSTANCE_4, "running-namespace-4");
        assertThat(operationsInProgressRepository.count()).isEqualTo(3);
        assertThat(getRunningOperations()).containsExactlyInAnyOrderEntriesOf(Map.of(LifecycleOperationType.INSTANTIATE, 3L));
    }

    private Map<LifecycleOperationType, Long> getRunningOperations() {
        final List<LifecycleOperation> lifecycleOperations = lifecycleOperationRepository.findAll();
        return lifecycleOperations.stream()
                .filter(lifecycleOperation -> !TERMINAL_OPERATION_STATES.contains(lifecycleOperation.getOperationState()))
                .collect(Collectors.groupingBy(LifecycleOperation::getLifecycleOperationType, Collectors.counting()));
    }

    private LifecycleOperation runInstantiateAndVerifyStatus(String vnfInstanceId, String namespace) throws Exception {
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(vnfInstanceId, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        MvcResult instantiateRequestResult = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, namespace);
        String lifecycleOperationId = getLifeCycleOperationId(instantiateRequestResult);
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findById(lifecycleOperationId).orElseThrow();
        assertThat(lifecycleOperation.getOperationState()).isNotIn(TERMINAL_OPERATION_STATES);

        return lifecycleOperation;
    }

    private void runInstantiateAndVerifyLimitError(String vnfInstanceId, String namespace) throws Exception {
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(vnfInstanceId, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        ProblemDetails expectedProblemDetails = createLimitReachedProblemDetails(vnfInstanceResponse.getId());
        MvcResult instantiateRequestResult = requestHelper.getMvcResultNegativeInstantiateRequest(vnfInstanceResponse, namespace,
                                                                                                  DEFAULT_CLUSTER_NAME, false);
        assertThat(instantiateRequestResult.getResponse().getStatus()).isEqualTo(429);
        ProblemDetails actualProblemDetails = objectMapper.readValue(instantiateRequestResult.getResponse().getContentAsString(),
                                                                     ProblemDetails.class);
        assertThat(expectedProblemDetails).isEqualTo(actualProblemDetails);
    }

    private void completeWfsMessagesForLifecycleOperation(LifecycleOperation lifecycleOperation) throws JsonProcessingException {
        String operationOccurrenceId = lifecycleOperation.getOperationOccurrenceId();
        String vnfInstanceId = lifecycleOperation.getVnfInstance().getVnfInstanceId();
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage("",
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             operationOccurrenceId,
                                                                             WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedWfsMessage, vnfInstanceId,
                                                            HelmReleaseOperationType.INSTANTIATE, PROCESSING,
                                                            true, false);

        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage("",
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               operationOccurrenceId,
                                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                                               "1");
        messageHelper.sendCompleteInstantiateMessageForCnfCharts(completedLifecycleMessage, vnfInstanceId, COMPLETED,
                                                                 true);
    }

    private ProblemDetails createLimitReachedProblemDetails(String vnfInstanceId) {
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setType(URI.create(TYPE_BLANK));
        problemDetails.setTitle("Limit of concurrent LCM operations is reached");
        problemDetails.setStatus(429);
        problemDetails.setDetail(String.format("Operation cannot be created due to reached global limit of concurrent LCM operations: %d",
                                               LCM_OPERATIONS_LIMIT));
        problemDetails.setInstance(URI.create(HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + vnfInstanceId));
        return problemDetails;
    }
}


