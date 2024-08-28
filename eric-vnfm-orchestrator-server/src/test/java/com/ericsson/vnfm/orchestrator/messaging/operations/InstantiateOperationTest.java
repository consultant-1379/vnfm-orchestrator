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
package com.ericsson.vnfm.orchestrator.messaging.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLING_BACK;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ADD_NODE;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.INSTANTIATE;
import static com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType.TERMINATE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.model.WorkflowSecretAttribute;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.scheduler.CheckApplicationTimeout;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.databind.ObjectMapper;


public class InstantiateOperationTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private InstantiateOperation instantiateOperation;

    @Autowired
    private TerminateOperation terminateOperation;

    @Autowired
    private DeletePvcOperation deletePvcOperation;

    @MockBean
    private OssNodeService ossNodeService;

    @Autowired
    private InstanceService instanceService;

    @MockBean
    private EnmTopologyService enmTopologyService;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private DeleteNamespaceOperation deleteNamespaceOperation;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private CryptoService cryptoService;

    @MockBean
    private CryptoUtils cryptoUtils;

    @SpyBean
    private WorkflowRoutingServicePassThrough workflowRoutingService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void initialSetup() {
        doNothing().when(cryptoUtils).setEncryptDetailsForKey(anyString(), anyString(), any(VnfInstance.class));
        when(packageService.getPackageInfo(Mockito.any())).thenReturn(new PackageResponse());
        whenWfsSecretsRespondsWithUnsealKey();
    }

    @Test
    public void operationInProcessingWhichCompletesWithHealSupportedExceptionDuringUnsealEncryption() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "713ec68f";
        final String errorMessage = "Can't encrypt";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(cryptoUtils)
                .setEncryptDetailsForKey(anyString(), anyString(), any(VnfInstance.class));
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(FAILED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNull();
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();
        assertThat(instance.isCleanUpResources()).isFalse();
        assertThat(instance.getSensitiveInfo()).isNull();
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"unable to "
                                                           + "encrypt the data for instance name msg-inst-failed due to Can't encrypt\","
                                                           + "\"instance\":\"about:blank\"}");

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void operationInProcessingWhichCompletes() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setOperationType(INSTANTIATE);
        final String lifecycleOperationId = "713ec68e-708c-4dab-9cd4-826a5d3d31e1";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNotNull();
        assertThat(operation.getTargetVnfdId()).isEqualTo(operation.getVnfInstance().getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams())
                .isEqualTo(operation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(operation.getVnfInstance().getCombinedValuesFile());
        assertThat(instance.isCleanUpResources()).isFalse();

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void operationInProcessingWhichCompletesAddNode() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        final String lifecycleOperationId = "87ebcbc8-474f-4673-91ee-761fd83641e6";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();

        doAnswer(invocation -> {
            VnfInstance argument = invocation.getArgument(0);
            argument.setAddedToOss(true);
            return null;
        }).when(ossNodeService).addNode(any());

        instanceService.addCommandResultToInstance(priorToMessage.getVnfInstance(), ADD_NODE);
        instantiateOperation.completed(completed);

        verify(ossNodeService, times(1)).addNode(any());
        verify(ossNodeService, times(1)).enableSupervisionsInENM(any());

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNotNull();
        assertThat(operation.getTargetVnfdId()).isEqualTo(operation.getVnfInstance().getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams())
                .isEqualTo(operation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(operation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmCharts() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();

        getHelmReleaseMessage(completed, HelmReleaseState.COMPLETED, "messaging-charts-1", INSTANTIATE);
        final String lifecycleOperationId = "rm8fcbc8-474f-4673-91ee-761fd83991e6";
        setOperationTimeouts(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        completed.setRevisionNumber("1");

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(instance, PROCESSING);
        completed.setReleaseName("messaging-charts-2");
        instantiateOperation.completed(completed);
        LifecycleOperation updateOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = updateOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        checkHelmChartStates(vnfInstance, COMPLETED);
        assertThat(vnfInstance.getResourceDetails()).isNotNull();
        assertThat(updateOperation.getTargetVnfdId()).isEqualTo(updateOperation.getVnfInstance().getVnfDescriptorId());
        assertThat(updateOperation.getCombinedAdditionalParams())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(updateOperation.getCombinedValuesFile())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        List<HelmChartHistoryRecord> historyRecords = helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId);
        assertThat(historyRecords.size())
                .isEqualTo(instance.getHelmCharts().size());
        assertThat(historyRecords).extracting("revisionNumber").containsOnly("1", "1");
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmChartsWithAdditionalParams() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(completed, HelmReleaseState.COMPLETED, "messaging-charts-22-1", INSTANTIATE);
        final String lifecycleOperationId = "rm8fcbc8-474f-4673-91ee-761fd83991e6200";
        setOperationTimeouts(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);
        completed.setRevisionNumber("1");

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(instance, PROCESSING);
        completed.setReleaseName("messaging-charts-22-2");
        instantiateOperation.completed(completed);
        LifecycleOperation updateOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = updateOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        checkHelmChartStates(vnfInstance, COMPLETED);
        assertThat(vnfInstance.getResourceDetails()).isNotNull();
        assertThat(updateOperation.getTargetVnfdId()).isEqualTo(updateOperation.getVnfInstance().getVnfDescriptorId());
        assertThat(updateOperation.getCombinedAdditionalParams())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(updateOperation.getCombinedValuesFile())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        List<HelmChartHistoryRecord> historyRecords = helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId);
        assertThat(historyRecords.size())
                .isEqualTo(instance.getHelmCharts().size());
        assertThat(historyRecords).extracting("revisionNumber").containsOnly("1", "1");
    }

    private void setOperationTimeouts(final String lifecycleOperationId) {
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        byOperationOccurrenceId.setApplicationTimeout("80");
        byOperationOccurrenceId.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        lifecycleOperationRepository.save(byOperationOccurrenceId);
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmChartsWithAdditionalParamsAndValuesFile() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(completed, HelmReleaseState.COMPLETED, "messaging-charts-23-1", INSTANTIATE);
        final String lifecycleOperationId = "rm8fcbc8-474f-4673-91ee-761fd83991e6300";
        completed.setLifecycleOperationId(lifecycleOperationId);

        setOperationTimeouts(lifecycleOperationId);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(instance, PROCESSING);
        completed.setReleaseName("messaging-charts-23-2");
        instantiateOperation.completed(completed);
        LifecycleOperation updateOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = updateOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        checkHelmChartStates(vnfInstance, COMPLETED);
        assertThat(vnfInstance.getResourceDetails()).isNotNull();
        assertThat(updateOperation.getTargetVnfdId()).isEqualTo(updateOperation.getVnfInstance().getVnfDescriptorId());
        assertThat(updateOperation.getCombinedAdditionalParams())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(updateOperation.getCombinedValuesFile())
                .isEqualTo(updateOperation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    @Test
    public void operationInProcessingWhichFailsMultipleHelmCharts() {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "messaging-fail-1", INSTANTIATE);
        final String lifecycleOperationId = "rm28fcbc8-474f-4673-91ee-761fd83991e6";
        setOperationTimeouts(lifecycleOperationId);
        message.setLifecycleOperationId(lifecycleOperationId);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(message);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(instance, PROCESSING);
        getHelmReleaseMessage(message, HelmReleaseState.FAILED, "messaging-fail-2", INSTANTIATE);
        message.setMessage("chart timed out");
        instantiateOperation.failed(message);
        LifecycleOperation updateOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = updateOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(vnfInstance, FAILED);
        assertThat(updateOperation.getError())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"INSTANTIATE for messaging-fail-2 failed"
                                   + " with chart timed out.\",\"instance\":\"about:blank\"}");
        // Operation has not been updated wih values as is failed
        assertThat(updateOperation.getTargetVnfdId()).isNull();
        assertThat(updateOperation.getCombinedAdditionalParams()).isNull();
        assertThat(updateOperation.getCombinedValuesFile()).isNull();
    }

    private void checkHelmChartStates(final VnfInstance instance, final LifecycleOperationState state) {
        HelmChart firstChart = getHelmChart(instance, 1);
        HelmChart secondChart = getHelmChart(instance, 2);
        assertThat(firstChart.getState()).isEqualTo(LifecycleOperationState.COMPLETED.toString());
        assertThat(secondChart.getState()).isEqualTo(state.toString());
    }

    @NotNull
    private HelmChart getHelmChart(final VnfInstance instance, final int priority) {
        return instance.getHelmCharts().stream().filter(chart -> chart.getPriority() == priority).findFirst().get();
    }

    @Test
    public void operationInProcessingWhichFailsWfsErrorMultipleHelmCharts() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(completed, HelmReleaseState.COMPLETED, "messaging-charts-fail-1", INSTANTIATE);
        final String lifecycleOperationId = "rm18fcbc8-474f-4673-91ee-761fd83991e6";
        setOperationTimeouts(lifecycleOperationId);
        completed.setLifecycleOperationId(lifecycleOperationId);

        whenWfsResourcesRespondsWithError(HttpMethod.POST);

        instantiateOperation.completed(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(FAILED);
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        checkHelmChartStates(instance, FAILED);
        assertThat(operation.getError()).contains("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
                                                          + "\"detail\":\"[{\\\"parameterName\\\":\\\"releaseName\\\","
                                                          + "\\\"message\\\":\\\"releaseName must consist of lower case alphanumeric characters or "
                                                          + "-. It must start with an alphabetic character, and end with an alphanumeric "
                                                          + "character\\\"}]\",\"instance\":\"about:blank\"}");
        // Operation has not been updated wih values as is failed
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();
    }

    @Test
    public void operationInProcessingWhichFailsWithAddedENMNode() {
        final HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(failed, HelmReleaseState.FAILED, null, INSTANTIATE);
        failed.setMessage("Failed to install");
        final String lifecycleOperationId = "804a34ac-d35d-4ba7-8a6a-24561e2ea4d7";
        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);

        VnfInstance vnfInstance = priorToMessage.getVnfInstance();
        vnfInstance.setAddedToOss(true);
        vnfInstanceRepository.save(vnfInstance);

        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        instantiateOperation.failed(failed);

        verify(ossNodeService).deleteNodeFromENM(any(VnfInstance.class), anyBoolean());

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(FAILED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).isNotNull();
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNull();
        assertThat(instance.isCleanUpResources()).isFalse();
        // Operation has not been updated wih values as is failed
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();
        assertThat(operation.getVnfInstance().isAddedToOss()).isFalse();
    }

    @Test
    public void operationInProcessingWhichFailsWithNoErrorMessage() {
        final HelmReleaseLifecycleMessage failed = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(failed, HelmReleaseState.FAILED, "msg-failed-1", INSTANTIATE);
        final String lifecycleOperationId = "rm6fcbc8-474f-4673-91ee-761fd83991e6";
        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        instantiateOperation.failed(failed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(FAILED);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failure event");
        // Operation has not been updated wih values as is failed
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();
    }

    @Test
    public void operationFailedWithCleanUpResourcesOneChart() {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(message, HelmReleaseState.FAILED, "clean-failed-1", INSTANTIATE);
        final String lifecycleOperationId = "rm5fcbc8-474f-4673-91ee-761fd83991e6";
        message.setLifecycleOperationId(lifecycleOperationId);
        instantiateOperation.failed(message);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        HelmChart helmChart = operation.getVnfInstance().getHelmCharts().get(0);
        assertThat(helmChart.getState()).isEqualTo(FAILED.toString());
        // Operation has not been updated wih values as is failed
        assertThat(operation.getTargetVnfdId()).isNull();
        assertThat(operation.getCombinedAdditionalParams()).isNull();
        assertThat(operation.getCombinedValuesFile()).isNull();
    }

    @Test
    public void operationFailedWithCleanUpResourcesTriggerTerminateSecondChart() {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "msg-failed-cleanup-1", INSTANTIATE);
        final String lifecycleOperationId = "rm7fcbc8-474f-4673-91ee-761fd83991e6";
        setOperationTimeouts(lifecycleOperationId);
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = byOperationOccurrenceId.getVnfInstance();
        vnfInstance.setCleanUpResources(true);
        vnfInstance.setNamespace("msg-failed-cleanup");
        vnfInstanceRepository.save(vnfInstance);
        message.setLifecycleOperationId(lifecycleOperationId);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(message);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        checkHelmChartStates(operation.getVnfInstance(), PROCESSING);

        getHelmReleaseMessage(message, HelmReleaseState.FAILED, "msg-failed-cleanup-2", INSTANTIATE);
        message.setMessage("chart 2 timed out");

        whenWfsResourcesRespondsWithAccepted(HttpMethod.PUT);

        instantiateOperation.failed(message);
        LifecycleOperation updatedOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(updatedOperation.getOperationState()).isEqualTo(ROLLING_BACK);
        assertThat(getHelmChart(updatedOperation.getVnfInstance(), 1).getState()).isEqualTo(PROCESSING.toString());
        assertThat(getHelmChart(updatedOperation.getVnfInstance(), 2).getState()).isEqualTo(PROCESSING.toString());
        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "msg-failed-cleanup-1", TERMINATE);

        whenWfsPvcsRespondsWithAccepted();

        terminateOperation.completed(message);
        deletePvcOperation.completed(message);
        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "msg-failed-cleanup-2", TERMINATE);
        terminateOperation.completed(message);
        deletePvcOperation.completed(message);
        deleteNamespaceOperation.completed(message);
        LifecycleOperation terminatedOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(terminatedOperation.getOperationState()).isEqualTo(ROLLED_BACK);
        assertThat(terminatedOperation.getError()).isEqualTo(
                "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Instantiate failed and cleanUpResources is true, "
                        + "now proceeding to terminate and delete all pvcs. \\nINSTANTIATE for msg-failed-cleanup-2 failed with chart 2 timed out"
                        + ".\",\"instance\":\"about:blank\"}");
        checkHelmChartStates(terminatedOperation.getVnfInstance(), COMPLETED);
        // Operation has not been updated wih values as is failed
        assertThat(terminatedOperation.getTargetVnfdId()).isNull();
        assertThat(terminatedOperation.getCombinedAdditionalParams()).isNull();
        assertThat(terminatedOperation.getCombinedValuesFile()).isNull();
        VnfInstance vnfInstanceCompleted = operation.getVnfInstance();
        assertThat(vnfInstanceCompleted.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        assertThat(vnfInstanceCompleted.getClusterName()).isNotNull();
        assertThat(vnfInstanceCompleted.getInstantiationState()).isNotNull();
    }

    @Test
    public void operationFailedWithCleanUpResourcesTriggerTerminateFailedFirstChart() {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "msg-terminate-cleanup-1", INSTANTIATE);
        final String lifecycleOperationId = "rm9fcbc8-474f-4673-91ee-761fd83991e6";
        setOperationTimeouts(lifecycleOperationId);
        message.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = byOperationOccurrenceId.getVnfInstance();
        vnfInstance.setCleanUpResources(true);
        vnfInstance.setNamespace("msg-terminate-cleanup");
        vnfInstanceRepository.save(vnfInstance);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.POST);

        instantiateOperation.completed(message);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(PROCESSING);
        checkHelmChartStates(operation.getVnfInstance(), PROCESSING);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.PUT);

        getHelmReleaseMessage(message, HelmReleaseState.FAILED, "msg-terminate-cleanup-2", INSTANTIATE);
        instantiateOperation.failed(message);
        LifecycleOperation updatedOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(updatedOperation.getOperationState()).isEqualTo(ROLLING_BACK);
        assertThat(getHelmChart(updatedOperation.getVnfInstance(), 1).getState()).isEqualTo(PROCESSING.toString());
        assertThat(getHelmChart(updatedOperation.getVnfInstance(), 2).getState()).isEqualTo(PROCESSING.toString());
        getHelmReleaseMessage(message, HelmReleaseState.FAILED, "msg-terminate-cleanup-1", TERMINATE);
        message.setMessage("Helm/Kubectl command timedOut.");
        terminateOperation.failed(message);

        whenWfsPvcsRespondsWithAccepted();

        getHelmReleaseMessage(message, HelmReleaseState.COMPLETED, "msg-terminate-cleanup-2", TERMINATE);
        terminateOperation.completed(message);
        deletePvcOperation.completed(message);
        LifecycleOperation terminatedOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(terminatedOperation.getOperationState()).isEqualTo(FAILED);
        assertThat(terminatedOperation.getError()).contains("Helm/Kubectl command timedOut.");
        assertThat(getHelmChart(terminatedOperation.getVnfInstance(), 1).getState()).isEqualTo(FAILED.toString());
        assertThat(getHelmChart(terminatedOperation.getVnfInstance(), 2).getState()).isEqualTo(COMPLETED.toString());
        // Operation has not been updated wih values as is failed
        assertThat(terminatedOperation.getTargetVnfdId()).isNull();
        assertThat(terminatedOperation.getCombinedAdditionalParams()).isNull();
        assertThat(terminatedOperation.getCombinedValuesFile()).isNull();
    }

    @Test
    public void operationFailedWithTimeOut() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setMessage(CheckApplicationTimeout.TIME_OUT_ERROR_MESSAGE);
        final String lifecycleOperationId = "713ec68e-708c-4dab-9cd4-826a5d3d31e1647464";
        completed.setLifecycleOperationId(lifecycleOperationId);
        instantiateOperation.rollBack(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        assertThat(operation.getVnfInstance().getHelmCharts().get(0).getState()).isEqualTo("FAILED");
    }

    @Test
    public void operationFailedWithTimeOutForMultipleCharts() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        completed.setMessage(CheckApplicationTimeout.TIME_OUT_ERROR_MESSAGE);
        final String lifecycleOperationId = "713ec68e-708c-4dab-9cd4-826a5d3d31e16474649";
        completed.setLifecycleOperationId(lifecycleOperationId);
        instantiateOperation.rollBack(completed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
        List<HelmChart> allChart = operation.getVnfInstance().getHelmCharts();
        assertThat(allChart).isNotNull();
        assertThat(allChart.size()).isEqualTo(2);
        for (HelmChart chart : allChart) {
            assertThat(chart.getState()).isEqualTo("FAILED");
        }
    }

    @Test
    public void operationInProcessingWithCleanUpResourcesShouldNotTerminateCrdCharts() {
        final String crd1ReleaseName = "crd-inst-crd1";
        final String crd2ReleaseName = "crd-inst-crd2";
        final String cnfReleaseName = "crd-inst-CNF";

        final String lifecycleOperationId = "operCrd-4cf4-477c-aab3-123456789101";
        setOperationTimeouts(lifecycleOperationId);

        final HelmReleaseLifecycleMessage crdSuccessfulInstantiationMessage = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(crdSuccessfulInstantiationMessage, HelmReleaseState.COMPLETED, crd2ReleaseName, INSTANTIATE);
        crdSuccessfulInstantiationMessage.setLifecycleOperationId(lifecycleOperationId);

        whenWfsResourcesRespondsWithError(HttpMethod.POST);
        whenWfsResourcesRespondsWithError(HttpMethod.PUT);

        instantiateOperation.completed(crdSuccessfulInstantiationMessage);
        verify(workflowRoutingService, times(0))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(crd1ReleaseName));
        verify(workflowRoutingService, times(0))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(crd2ReleaseName));
        verify(workflowRoutingService, times(1))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(cnfReleaseName));
    }

    @Test
    public void operationFailedWithCleanUpResourcesShouldNotTerminateCrdCharts() {
        final String crd1ReleaseName = "crd-inst-crd1";
        final String crd2ReleaseName = "crd-inst-crd2";
        final String cnfReleaseName = "crd-inst-CNF";

        final String lifecycleOperationId = "operCrd-4cf4-477c-aab3-123456789101";
        setOperationTimeouts(lifecycleOperationId);

        final HelmReleaseLifecycleMessage crdFailedInstantiationMessage = new HelmReleaseLifecycleMessage();
        getHelmReleaseMessage(crdFailedInstantiationMessage, HelmReleaseState.FAILED, cnfReleaseName, INSTANTIATE);
        crdFailedInstantiationMessage.setLifecycleOperationId(lifecycleOperationId);

        whenWfsResourcesRespondsWithAccepted(HttpMethod.PUT);

        instantiateOperation.failed(crdFailedInstantiationMessage);
        verify(workflowRoutingService, times(0))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(crd1ReleaseName));
        verify(workflowRoutingService, times(0))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(crd2ReleaseName));
        verify(workflowRoutingService, times(1))
                .routeTerminateRequest(any(VnfInstance.class), any(LifecycleOperation.class), anyMap(), eq(cnfReleaseName));
    }

    @Test
    public void operationRollingBackWithFailedAddNodeAndCleanUpResources() {
        final HelmReleaseLifecycleMessage completed = new HelmReleaseLifecycleMessage();
        completed.setState(HelmReleaseState.COMPLETED);
        completed.setOperationType(HelmReleaseOperationType.INSTANTIATE);
        final String lifecycleOperationId = "fgdrsgfnb673-91ee-761fd83641e6";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        when(enmTopologyService.generateAddNodeScript(anyMap())).thenReturn(null);
        doThrow(new FileExecutionException("Authorization failed")).when(ossNodeService).addNode(any());
        instanceService.addCommandResultToInstance(priorToMessage.getVnfInstance(), ADD_NODE);
        instantiateOperation.completed(completed);

        verify(ossNodeService, times(1)).addNode(any());

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(ROLLING_BACK);
        assertThat(operation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Adding node to "
                                                           + "OSS for vnf-test-failure-add-node failed with the following reason: Authorization "
                                                           + "failed.\\nInstantiate failed and cleanUpResources is true, now proceeding to "
                                                           + "terminate and delete all pvcs. \",\"instance\":\"about:blank\"}");
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(NOT_INSTANTIATED);
        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());
    }

    private static void getHelmReleaseMessage(final HelmReleaseLifecycleMessage message, final HelmReleaseState state,
                                              final String releaseName, final HelmReleaseOperationType type) {
        message.setState(state);
        message.setReleaseName(releaseName);
        message.setOperationType(type);
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithAccepted(final HttpMethod method) {
        when(restTemplate.exchange(contains("resources"), eq(method), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsResourcesRespondsWithError(final HttpMethod method) {
        final var resourceResponse = new ResourceResponse();
        resourceResponse.setErrorDetails("{\"errorDetails\":[{"
                                                 + "\"parameterName\":\"releaseName\","
                                                 + "\"message\":\"releaseName must consist of lower "
                                                 + "case alphanumeric characters or -. It must start with an alphabetic character, and end with an "
                                                 + "alphanumeric character\"}]}");

        when(restTemplate.exchange(contains("resources"), eq(method), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(resourceResponse, HttpStatus.BAD_REQUEST));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsPvcsRespondsWithAccepted() {
        when(restTemplate.exchange(contains("pvcs"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    @SuppressWarnings("unchecked")
    private void whenWfsSecretsRespondsWithUnsealKey() {
        final var secret = new WorkflowSecretAttribute();
        secret.setType("Opaque");
        secret.setData(Map.of("replica-user", "replica",
                              "replica-pwd", "postgres",
                              "metrics-user", "exporter",
                              "custom-pwd", "postgres",
                              "metrics-pwd", "postgres",
                              "custom-user", "eo_user",
                              "super-user", "postgres",
                              "super-pwd", "postgres"));
        final var secretsResponse = new WorkflowSecretResponse();
        secretsResponse.setAllSecrets(Map.of("eric-eo-database-pg-secret.eric-sec-key-management-unseal-key", secret));

        when(restTemplate.exchange(contains("secrets"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>(secretsResponse, HttpStatus.OK));
    }
}
