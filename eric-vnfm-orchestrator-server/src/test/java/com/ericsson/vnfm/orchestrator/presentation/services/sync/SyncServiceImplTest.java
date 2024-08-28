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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SYNC;
import static com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType.CNF;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.AUTOSCALING_PARAM_MISMATCH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.REPLICA_DETAILS_MAP_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleLevelCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleParametersServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        SyncServiceImpl.class,
        ActualScaleValuesRetriever.class,
        SyncOperationValidator.class,
        SyncActualValuesProcessor.class,
        ScaleServiceImpl.class,
        ScaleParametersServiceImpl.class,
        ScaleLevelCalculationServiceImpl.class,
        VnfInstanceServiceImpl.class,
        ExtensionsMapper.class,
        ObjectMapper.class })
@MockBean({
        HelmChartHistoryService.class,
        ReplicaCountCalculationService.class,
        MappingFileService.class })
@SpyBean({
        ReplicaDetailsMapper.class })
public class SyncServiceImplTest {

    private static final TypeReference<Map<String, Integer>> RESOURCE_DETAILS_TYPE_REF = new TypeReference<>() {
    };

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private WorkflowRoutingService workflowRoutingService;

    @SpyBean
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private SyncService syncService;

    @Test
    public void testExecuteSyncOperationForNonScalablePodFailed() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithNonScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc2.replicaCount")
                                .withCurrentReplicaCount(2)
                                .build())));

        givenActualValues(Map.of(
                "vnfc2.replicaCount", 3));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("vnfc2 is not scalable, can not perform scale operation");
    }

    @Test
    public void testExecuteSyncOperationForNonScalablePodCompleted() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithNonScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc2.replicaCount")
                                .withCurrentReplicaCount(2)
                                .build())));

        givenActualValues(Map.of(
                "vnfc2.replicaCount", 2));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
    }

    @Test
    public void testExecuteSyncOperationWithoutScalePolicyFailed() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithoutPolicies();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc2.replicaCount")
                                .withCurrentReplicaCount(2)
                                .build())));

        givenActualValues(Map.of(
                "vnfc2.replicaCount", 2));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("Scale not supported as policies not present for instance instance-id");
    }

    @Test
    public void testChangeAutoscalingParamForNonScalablePod() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithNonScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc2.replicaCount")
                                .withCurrentReplicaCount(2)
                                .withAutoScalingEnabledParameterName("vnfc2.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.of(
                "vnfc2.replicaCount", 2,
                "vnfc2.autoscaling.enabled", true));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("vnfc2 is not scalable, can not enable autoscaling.");
    }

    @Test
    public void testSyncOperationReturnInternalServerErrorFromWFS() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithNonScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withCurrentReplicaCount(2)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        HttpServerErrorException exception = new HttpServerErrorException(
                INTERNAL_SERVER_ERROR,
                "Internal server error",
                "{\"errorDetails\": [{\"message\": \"Error: release: not found\"}]".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        givenActualValuesException(exception);

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("{\"type\":\"about:blank\",\"title\":\"Exception occurred\",\"status\":503,"
                                                                 + "\"detail\":\"{\\\"errorDetails\\\": [{\\\"message\\\": \\\"Error: release: not "
                                                                 + "found\\\"}]\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void testSyncOperationReturnCommunicationErrorFromWFS() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithNonScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "vnfc2", ReplicaDetails.builder()
                                .withCurrentReplicaCount(2)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValuesException(new ResourceAccessException("Communication error"));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("{\"type\":\"about:blank\",\"title\":\"Exception occurred\",\"status\":503,"
                                                                 + "\"detail\":\"Communication error\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void testScalablePodsNonLinearScalingMultipleAspectsCismToManualControlledSuccess() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.test-cnf.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withMaxReplicasParameterName("vnfc5.maxReplicas")
                                .withMaxReplicasCount(1)
                                .withMinReplicasParameterName("vnfc5.minReplicas")
                                .withMinReplicasCount(1)
                                .withAutoScalingEnabledParameterName("vnfc5.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 1),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 5),
                entry("vnfc4.replicaCount", 5),
                entry("vnfc5.replicaCount", 1),
                entry("vnfc5.autoscaling.enabled", true),
                entry("vnfc5.maxReplicas", 1),
                entry("vnfc5.minReplicas", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // extension
        assertThat(scalingExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("Aspect3", CISM_CONTROLLED),
                Assertions.entry("Aspect1", MANUAL_CONTROLLED),
                Assertions.entry("Aspect2", MANUAL_CONTROLLED),
                Assertions.entry("Aspect5", CISM_CONTROLLED));

        // scale info
        List<ScaleInfoEntity> scaleInfoEntities = updatedVnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities.size()).isEqualTo(4);
        assertScaleLevel(scaleInfoEntities, 0, 1, 0, 0);

        // resource details
        assertThat(resourceDetailsFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("test-cnf-vnfc3", 5),
                Assertions.entry("test-cnf-vnfc4", 5),
                Assertions.entry("test-cnf", 1),
                Assertions.entry("test-cnf-vnfc5", 1),
                Assertions.entry("eric-pm-bulk-reporter", 1),
                Assertions.entry("test-cnf-vnfc1", 1));
    }

    @Test
    public void testExecuteSyncOperationWithDeployableModulesSuccess() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePodsAndDeployableModules();

        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.test-cnf.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withMaxReplicasParameterName("vnfc5.maxReplicas")
                                .withMaxReplicasCount(1)
                                .withMinReplicasParameterName("vnfc5.minReplicas")
                                .withMinReplicasCount(1)
                                .withAutoScalingEnabledParameterName("vnfc5.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 1),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 5),
                entry("vnfc4.replicaCount", 5),
                entry("vnfc5.replicaCount", 1),
                entry("vnfc5.autoscaling.enabled", true),
                entry("vnfc5.maxReplicas", 1),
                entry("vnfc5.minReplicas", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // extension
        assertThat(deployableModulesExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("deployable_module_1", "disabled"),
                Assertions.entry("deployable_module_2", "enabled"),
                Assertions.entry("deployable_module_3", "enabled"));

        assertThat(scalingExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("Aspect3", CISM_CONTROLLED),
                Assertions.entry("Aspect1", MANUAL_CONTROLLED),
                Assertions.entry("Aspect2", MANUAL_CONTROLLED),
                Assertions.entry("Aspect5", CISM_CONTROLLED));
    }

    @Test
    public void testScalablePodsLinearScalingMultipleAspectsCismToManualControlledSuccess() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.test-cnf.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 7),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 7),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.replicaCount", 7),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 7),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 1),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // extension
        assertThat(scalingExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("Aspect3", CISM_CONTROLLED),
                Assertions.entry("Aspect1", MANUAL_CONTROLLED),
                Assertions.entry("Aspect2", MANUAL_CONTROLLED),
                Assertions.entry("Aspect5", MANUAL_CONTROLLED));

        // scale info
        List<ScaleInfoEntity> scaleInfoEntities = updatedVnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities.size()).isEqualTo(4);
        assertScaleLevel(scaleInfoEntities, 0, 0, 3, 0);

        // resource details
        assertThat(resourceDetailsFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("test-cnf-vnfc3", 1),
                Assertions.entry("test-cnf-vnfc4", 1),
                Assertions.entry("test-cnf", 7),
                Assertions.entry("test-cnf-vnfc5", 1),
                Assertions.entry("eric-pm-bulk-reporter", 1),
                Assertions.entry("test-cnf-vnfc1", 7));

        // replica details
        Map<String, ReplicaDetails> replicaDetails = replicaDetailsOfFirstChartFrom(updatedVnfInstance);
        assertReplicaCount(replicaDetails.get("test-cnf"), 7, 7, 1, true);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc1"), 7, 7, 1, true);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc3"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc4"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc5"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("eric-pm-bulk-reporter"), 1, null, null, false);
    }

    @Test
    public void testScalablePodsNonLinearScalingMultipleAspectsManualToManualControlledSuccess() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 6),
                entry("vnfc4.replicaCount", 6),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // extension
        assertThat(scalingExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("Aspect3", MANUAL_CONTROLLED),
                Assertions.entry("Aspect1", MANUAL_CONTROLLED),
                Assertions.entry("Aspect2", MANUAL_CONTROLLED),
                Assertions.entry("Aspect5", MANUAL_CONTROLLED));

        // scale info
        List<ScaleInfoEntity> scaleInfoEntities = updatedVnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities.size()).isEqualTo(4);
        assertScaleLevel(scaleInfoEntities, 0, 2, 0, 0);

        // resource details
        assertThat(resourceDetailsFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("test-cnf-vnfc3", 6),
                Assertions.entry("test-cnf-vnfc4", 6),
                Assertions.entry("test-cnf", 1),
                Assertions.entry("test-cnf-vnfc5", 1),
                Assertions.entry("eric-pm-bulk-reporter", 1),
                Assertions.entry("test-cnf-vnfc1", 1));

        // replica details
        Map<String, ReplicaDetails> replicaDetails = replicaDetailsOfFirstChartFrom(updatedVnfInstance);
        assertReplicaCount(replicaDetails.get("test-cnf"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc1"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc3"), 6, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc4"), 6, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc5"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("eric-pm-bulk-reporter"), 1, null, null, false);
    }

    @Test
    public void testScalablePodsNonLinearScalingInMultipleAspectsManualToManualControlledSuccess() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScaledAspects();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(5)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(5)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(5)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(5)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(3)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(3)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 1),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // extension
        assertThat(scalingExtensionFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("Aspect3", MANUAL_CONTROLLED),
                Assertions.entry("Aspect1", MANUAL_CONTROLLED),
                Assertions.entry("Aspect2", MANUAL_CONTROLLED),
                Assertions.entry("Aspect5", MANUAL_CONTROLLED));

        // scale info
        List<ScaleInfoEntity> scaleInfoEntities = updatedVnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities.size()).isEqualTo(4);
        assertScaleLevel(scaleInfoEntities, 0, 0, 0, 0);

        // resource details
        assertThat(resourceDetailsFrom(updatedVnfInstance)).containsOnly(
                Assertions.entry("test-cnf-vnfc3", 1),
                Assertions.entry("test-cnf-vnfc4", 1),
                Assertions.entry("test-cnf", 1),
                Assertions.entry("test-cnf-vnfc5", 1),
                Assertions.entry("eric-pm-bulk-reporter", 1),
                Assertions.entry("test-cnf-vnfc1", 1));

        // replica details
        Map<String, ReplicaDetails> replicaDetails = replicaDetailsOfFirstChartFrom(updatedVnfInstance);
        assertReplicaCount(replicaDetails.get("test-cnf"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc1"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc3"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc4"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc5"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("eric-pm-bulk-reporter"), 1, null, null, false);
    }

    @Test
    public void testFailScalablePodsPartiallyUpdatedAutoscaling() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc3.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasParameterName("vnfc3.minReplicas")
                                .withMinReplicasCount(1)
                                .withMaxReplicasParameterName("vnfc3.maxReplicas")
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 6),
                entry("vnfc3.autoscaling.enabled", true),
                entry("vnfc3.minReplicas", 1),
                entry("vnfc3.maxReplicas", 1),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc4.autoscaling.enabled", false),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains(String.format(AUTOSCALING_PARAM_MISMATCH, "test-cnf-vnfc4, test-cnf-vnfc3"));
    }

    @Test
    public void testFailScalablePodsPartiallyScaledAndExceedMaxScaleLevel() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.test-cnf.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 7),
                entry("vnfc1.test-cnf.maxReplicas", 7),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 6),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc5.replicaCount", 50),
                entry("eric-pm-bulk-reporter.replicaCount", 50)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains(
                "Scale level for test-cnf-vnfc3 should be equal to 0, but it is equal to 2.",
                "Scale level for test-cnf-vnfc1 should be equal to 3, but it is equal to 0.",
                "Scale level for test-cnf-vnfc5 is 49 which exceeds max scale level 10.",
                "Scale level for eric-pm-bulk-reporter is 49 which exceeds max scale level 10.");
    }

    @Test
    public void testFailReplicaCountDoesNotMatchScaleLevel() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasCount(1)
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 3),
                entry("vnfc4.replicaCount", 3),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError())
                .contains("For target test-cnf-vnfc4 replica count 3 does not belong to any scaling level. " +
                                  "Closest scaling level: 1, closets replica count: 5.");
    }

    @Test
    public void testFailManoControlledScalingNoExtensions() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithManoControlledNoExtension();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc3.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc3.minReplicas")
                                .withMaxReplicasParameterName("vnfc3.maxReplicas")
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc4.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc4.minReplicas")
                                .withMaxReplicasParameterName("vnfc4.maxReplicas")
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 5),
                entry("vnfc3.autoscaling.enabled", true),
                entry("vnfc3.minReplicas", 1),
                entry("vnfc3.maxReplicas", 5),
                entry("vnfc4.replicaCount", 5),
                entry("vnfc4.autoscaling.enabled", true),
                entry("vnfc4.minReplicas", 1),
                entry("vnfc4.maxReplicas", 5),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError())
                .contains("manoControlledScaling is true, but autoscaling for test-cnf-vnfc4 is true.");
    }

    @Test
    public void testFailMinReplicaCountDoesNotMatchInitialDelta() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc3.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasParameterName("vnfc3.minReplicas")
                                .withMinReplicasCount(1)
                                .withMaxReplicasParameterName("vnfc3.maxReplicas")
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc4.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .withMinReplicasParameterName("vnfc4.minReplicas")
                                .withMinReplicasCount(1)
                                .withMaxReplicasParameterName("vnfc4.maxReplicas")
                                .withMaxReplicasCount(1)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc3.replicaCount", 6),
                entry("vnfc3.autoscaling.enabled", true),
                entry("vnfc3.minReplicas", 3),
                entry("vnfc3.maxReplicas", 1),
                entry("vnfc4.replicaCount", 6),
                entry("vnfc4.autoscaling.enabled", true),
                entry("vnfc4.minReplicas", 3),
                entry("vnfc4.maxReplicas", 1),
                entry("vnfc5.replicaCount", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError())
                .contains("Min replica count for test-cnf-vnfc4 should be equal to initial delta:  1, but it is equal to 3.");
    }

    @Test
    public void testScalablePodsNoParameterNamesInReplicaDetails() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withMaxReplicasParameterName("vnfc5.maxReplicas")
                                .withMaxReplicasCount(1)
                                .withMinReplicasParameterName("vnfc5.minReplicas")
                                .withMinReplicasCount(1)
                                .withAutoScalingEnabledParameterName("vnfc5.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 1),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 1),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc5.replicaCount", 1),
                entry("vnfc5.autoscaling.enabled", true),
                entry("vnfc5.maxReplicas", 1),
                entry("vnfc5.minReplicas", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        // operation
        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(updatedOperation.getError()).isNull();

        // instance
        final VnfInstance updatedVnfInstance = captureVnfInstance();

        // replica details
        Map<String, ReplicaDetails> replicaDetails = replicaDetailsOfFirstChartFrom(updatedVnfInstance);
        assertReplicaCount(replicaDetails.get("test-cnf"), 1, 1, 1, true);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc1"), 1, 1, 1, true);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc3"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc4"), 1, null, null, false);
        assertReplicaCount(replicaDetails.get("test-cnf-vnfc5"), 1, 1, 1, true);
        assertReplicaCount(replicaDetails.get("eric-pm-bulk-reporter"), 1, null, null, false);
    }

    @Test
    public void testFailScalablePodsNoMinMaxReplicasAndAutoscalingEnabled() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();
        LifecycleOperation operation = createLifecycleOperation(vnfInstance);

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withMaxReplicasParameterName("vnfc5.maxReplicas")
                                .withMaxReplicasCount(1)
                                .withMinReplicasParameterName("vnfc5.minReplicas")
                                .withMinReplicasCount(1)
                                .withAutoScalingEnabledParameterName("vnfc5.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc3.replicaCount", 1),
                entry("vnfc4.replicaCount", 1),
                entry("vnfc5.replicaCount", 1),
                entry("vnfc5.autoscaling.enabled", true),
                entry("vnfc5.minReplicas", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains(
                "Min replica count for test-cnf is missing",
                "Max replica count for test-cnf-vnfc5 is missing");
    }

    @Test
    public void testFailSyncOperationApplicationTimeOutExpired() {
        // given
        VnfInstance vnfInstance = createVnfInstanceWithScalablePods();

        LifecycleOperation operation = createLifecycleOperation(vnfInstance);
        operation.setExpiredApplicationTime(LocalDateTime.now());

        givenReplicaDetails(Map.of(
                "test-cnf", Map.of(
                        "test-cnf", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.test-cnf.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.test-cnf.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.test-cnf.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.test-cnf.maxReplicas")
                                .build(),
                        "test-cnf-vnfc1", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc1.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledParameterName("vnfc1.autoscaling.enabled")
                                .withAutoScalingEnabledValue(false)
                                .withMinReplicasParameterName("vnfc1.minReplicas")
                                .withMaxReplicasParameterName("vnfc1.maxReplicas")
                                .build(),
                        "test-cnf-vnfc3", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc3.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc4", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc4.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build(),
                        "test-cnf-vnfc5", ReplicaDetails.builder()
                                .withScalingParameterName("vnfc5.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withMaxReplicasParameterName("vnfc5.maxReplicas")
                                .withMaxReplicasCount(1)
                                .withMinReplicasParameterName("vnfc5.minReplicas")
                                .withMinReplicasCount(1)
                                .withAutoScalingEnabledParameterName("vnfc5.autoscaling.enabled")
                                .withAutoScalingEnabledValue(true)
                                .build(),
                        "eric-pm-bulk-reporter", ReplicaDetails.builder()
                                .withScalingParameterName("eric-pm-bulk-reporter.replicaCount")
                                .withCurrentReplicaCount(1)
                                .withAutoScalingEnabledValue(false)
                                .build())));

        givenActualValues(Map.ofEntries(
                entry("vnfc1.test-cnf.replicaCount", 1),
                entry("vnfc1.test-cnf.autoscaling.enabled", true),
                entry("vnfc1.test-cnf.maxReplicas", 1),
                entry("vnfc1.test-cnf.minReplicas", 1),
                entry("vnfc1.replicaCount", 1),
                entry("vnfc1.autoscaling.enabled", true),
                entry("vnfc1.maxReplicas", 1),
                entry("vnfc1.minReplicas", 1),
                entry("vnfc3.replicaCount", 5),
                entry("vnfc4.replicaCount", 5),
                entry("vnfc5.replicaCount", 1),
                entry("vnfc5.autoscaling.enabled", true),
                entry("vnfc5.maxReplicas", 1),
                entry("vnfc5.minReplicas", 1),
                entry("eric-pm-bulk-reporter.replicaCount", 1)));

        // when
        syncService.execute(vnfInstance, operation);

        // then
        awaitInstanceAndOperationPersisted();

        LifecycleOperation updatedOperation = captureLifecycleOperation();
        assertThat(updatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(updatedOperation.getError()).contains("SYNC operation failed due to timeout.");
    }

    private VnfInstance createVnfInstanceWithoutPolicies() {
        final var chart = new HelmChart();
        chart.setHelmChartUrl("test");
        chart.setReleaseName("test-cnf");
        chart.setHelmChartName("test");
        chart.setHelmChartType(CNF);
        chart.setPriority(1);

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setVnfInstanceName("instance-name");
        instance.setHelmCharts(List.of(chart));
        instance.setScaleInfoEntity(emptyList());

        return instance;
    }

    private VnfInstance createVnfInstanceWithNonScalablePods() {
        final var instance = createVnfInstanceWithoutPolicies();
        instance.setPolicies("{\"allInitialDelta\": {\"vnfc2\": {\"type\": \"tosca.policies.nfv.VduInitialDelta\", \"properties\": "
                                     + "{\"initial_delta\": {\"number_of_instances\": 1}}, \"targets\": [\"test-cnf-vnfc2\"]}}}");

        return instance;
    }

    private VnfInstance createVnfInstanceWithScalablePods() {
        final VnfInstance instance = createVnfInstanceWithPoliciesAndResourceDetails();
        instance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{"
                                                                  + "\"Aspect5\":\"ManualControlled\","
                                                                  + "\"Aspect1\":\"ManualControlled\","
                                                                  + "\"Aspect2\":\"ManualControlled\","
                                                                  + "\"Aspect3\":\"ManualControlled\"}}");
        instance.setScaleInfoEntity(List.of(
                scaleInfo("Aspect1", 0),
                scaleInfo("Aspect2", 0),
                scaleInfo("Aspect3", 0),
                scaleInfo("Aspect5", 0)));

        return instance;
    }

    private VnfInstance createVnfInstanceWithScalablePodsAndDeployableModules() {
        final VnfInstance instance = createVnfInstanceWithPoliciesAndResourceDetails();
        instance.setVnfInfoModifiableAttributesExtensions("{\n" +
                "   \"vnfControlledScaling\":{\n" +
                "      \"Aspect5\":\"ManualControlled\",\n" +
                "      \"Aspect1\":\"ManualControlled\",\n" +
                "      \"Aspect2\":\"ManualControlled\",\n" +
                "      \"Aspect3\":\"ManualControlled\"\n" +
                "   },\n" +
                "   \"deployableModules\":{\n" +
                "      \"deployable_module_1\":\"disabled\",\n" +
                "      \"deployable_module_2\":\"enabled\",\n" +
                "      \"deployable_module_3\":\"enabled\"\n" +
                "   }\n" +
                "}\n" +
                "}");

        instance.setScaleInfoEntity(List.of(
                scaleInfo("Aspect1", 0),
                scaleInfo("Aspect2", 0),
                scaleInfo("Aspect3", 0),
                scaleInfo("Aspect5", 0)));

        return instance;
    }

    private VnfInstance createVnfInstanceWithScaledAspects() {
        final VnfInstance instance = createVnfInstanceWithPoliciesAndResourceDetails();
        instance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{"
                                                                  + "\"Aspect5\":\"ManualControlled\","
                                                                  + "\"Aspect1\":\"ManualControlled\","
                                                                  + "\"Aspect2\":\"ManualControlled\","
                                                                  + "\"Aspect3\":\"ManualControlled\"}}");
        instance.setScaleInfoEntity(List.of(
                scaleInfo("Aspect1", 0),
                scaleInfo("Aspect2", 1),
                scaleInfo("Aspect3", 0),
                scaleInfo("Aspect5", 2)));

        return instance;
    }

    private VnfInstance createVnfInstanceWithManoControlledNoExtension() {
        final VnfInstance instance = createVnfInstanceWithPoliciesAndResourceDetails();
        instance.setScaleInfoEntity(List.of(
                scaleInfo("Aspect1", 0),
                scaleInfo("Aspect2", 0),
                scaleInfo("Aspect3", 0),
                scaleInfo("Aspect5", 0)));
        instance.setManoControlledScaling(true);

        return instance;
    }

    private VnfInstance createVnfInstanceWithPoliciesAndResourceDetails() {
        final var instance = createVnfInstanceWithoutPolicies();
        instance.setPolicies(readDataFromFile(getClass(), "policies-multiple-aspects.json"));
        instance.setResourceDetails("{\"test-cnf-vnfc3\":1,"
                                            + "\"test-cnf-vnfc4\":1,"
                                            + "\"test-cnf\":1,"
                                            + "\"test-cnf-vnfc5\":1,"
                                            + "\"eric-pm-bulk-reporter\":1,"
                                            + "\"test-cnf-vnfc1\":1}");

        return instance;
    }

    private static ScaleInfoEntity scaleInfo(final String aspectId, final int scaleLevel) {
        final var scaleInfo = new ScaleInfoEntity();
        scaleInfo.setAspectId(aspectId);
        scaleInfo.setScaleLevel(scaleLevel);

        return scaleInfo;
    }

    private static LifecycleOperation createLifecycleOperation(final VnfInstance vnfInstance) {
        LifecycleOperation operation = TestUtils.createLifecycleOperation(vnfInstance, SYNC, PROCESSING);
        operation.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        operation.setStartTime(LocalDateTime.now());
        operation.setStateEnteredTime(LocalDateTime.now());

        return operation;
    }

    private void givenReplicaDetails(final Map<String, Map<String, ReplicaDetails>> allReplicaDetails) {
        doReturn(allReplicaDetails).when(replicaDetailsMapper).getReplicaDetailsForAllCharts(any());
    }

    private void givenActualValues(final Map<String, Object> values) {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        when(workflowRoutingService.getChartValuesRequest(any(), any())).thenReturn(new ResponseEntity<>(values, headers, OK));
    }

    private void givenActualValuesException(final RestClientException exception) {
        when(workflowRoutingService.getChartValuesRequest(any(), any())).thenThrow(exception);
    }

    private void awaitInstanceAndOperationPersisted() {
        await().untilAsserted(() -> verify(databaseInteractionService).persistVnfInstanceAndOperation(any(), any()));
    }

    private LifecycleOperation captureLifecycleOperation() {
        final var captor = ArgumentCaptor.forClass(LifecycleOperation.class);
        verify(databaseInteractionService).persistVnfInstanceAndOperation(any(), captor.capture());

        return captor.getValue();
    }

    private VnfInstance captureVnfInstance() {
        final var captor = ArgumentCaptor.forClass(VnfInstance.class);
        verify(databaseInteractionService).persistVnfInstanceAndOperation(captor.capture(), any());

        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scalingExtensionFrom(final VnfInstance vnfInstance) {
        return (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions())
                .get(VNF_CONTROLLED_SCALING);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deployableModulesExtensionFrom(final VnfInstance vnfInstance) {
        return (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions())
                .get(DEPLOYABLE_MODULES);
    }

    private static Map<String, Integer> resourceDetailsFrom(final VnfInstance vnfInstance) {
        return parseJsonToGenericType(vnfInstance.getResourceDetails(), RESOURCE_DETAILS_TYPE_REF);
    }

    private static HashMap<String, ReplicaDetails> replicaDetailsOfFirstChartFrom(final VnfInstance vnfInstance) {
        return parseJsonToGenericType(vnfInstance.getHelmCharts().get(0).getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE);
    }

    private void assertReplicaCount(ReplicaDetails details,
                                    Integer currentReplicaCount,
                                    Integer maxReplicaCount,
                                    Integer minReplicaCount,
                                    boolean autoscalingEnabled) {

        assertThat(details.getCurrentReplicaCount()).isEqualTo(currentReplicaCount);
        assertThat(details.getMinReplicasCount()).isEqualTo(minReplicaCount);
        assertThat(details.getMaxReplicasCount()).isEqualTo(maxReplicaCount);
        assertThat(details.getAutoScalingEnabledValue()).isEqualTo(autoscalingEnabled);
    }

    private static void assertScaleLevel(List<ScaleInfoEntity> scaleInfoEntities,
                                         int aspect1Level,
                                         int aspect2Level,
                                         int aspect3Level,
                                         int aspect5Level) {

        assertThat(scaleInfoEntities)
                .extracting(ScaleInfoEntity::getAspectId, ScaleInfoEntity::getScaleLevel)
                .containsOnly(
                        tuple("Aspect1", aspect1Level),
                        tuple("Aspect2", aspect2Level),
                        tuple("Aspect3", aspect3Level),
                        tuple("Aspect5", aspect5Level));
    }
}
