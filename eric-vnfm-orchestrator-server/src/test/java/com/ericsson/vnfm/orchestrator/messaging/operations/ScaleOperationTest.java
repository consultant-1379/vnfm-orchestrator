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
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;
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

import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ScaleOperationTest extends AbstractDbSetupTest {

    @Autowired
    private ScaleOperation scaleOperation;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private RollbackOperation rollbackOperation;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private HelmChartHistoryServiceImpl helmChartHistoryService;

    @SpyBean
    private WorkflowRoutingServicePassThrough workflowRoutingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScaleService scaleService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setupDefaultPackageService() {
        Mockito.when(packageService.getPackageInfo(Mockito.any())).thenReturn(new PackageResponse());
    }

    @Test
    public void operationInProcessingWhichCompletesSingleChart() throws JsonProcessingException {
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, "multiple-sca1e-1");
        final String lifecycleOperationId = "sca1e-6a0e-443c-9b0c-4da9d9c2ab71";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToOperation = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);

        whenOnboardingRespondsWith("scale/descriptor-model.json");

        // Scale levels initial values
        checkScaleLevel(priorToOperation.getVnfInstance(), 3);
        sendCompletedScaleMessage(completed, priorToOperation, ScaleVnfRequest.TypeEnum.OUT);

        //first scale out
        verifyScaleOperation(lifecycleOperationId, "16", 4);

        final String operationOccId = "sca7e-6a0e-443c-9b0c-4da9d9c2ab71";
        completed.setLifecycleOperationId(operationOccId);
        LifecycleOperation priorToOperationOcc = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);

        // Scale levels initial values
        checkScaleLevel(priorToOperation.getVnfInstance(), 4);
        sendCompletedScaleMessage(completed, priorToOperationOcc, ScaleVnfRequest.TypeEnum.IN);

        //first scale in
        verifyScaleOperation(operationOccId, "9", 3);

        final String operationOccId1 = "sca8e-6a0e-443c-9b0c-4da9d9c2ab71";
        completed.setLifecycleOperationId(operationOccId1);
        LifecycleOperation priorToOperationOcc1 = lifecycleOperationRepository
                .findByOperationOccurrenceId(operationOccId1);
        // Scale levels initial values
        checkScaleLevel(priorToOperationOcc1.getVnfInstance(), 3);
        sendCompletedScaleMessage(completed, priorToOperationOcc1, ScaleVnfRequest.TypeEnum.OUT);

        //second scale out
        verifyScaleOperation(operationOccId, "16", 4);
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmCharts() throws JsonProcessingException {
        final String firstReleaseName = "multiple-sca2e-1";
        final String secondReleaseName = "multiple-sca2e-2";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "sca2e-2240-4d3b-b840-c9deafa98c83";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        priorToMessage.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        priorToMessage.setApplicationTimeout("80");
        priorToMessage.getVnfInstance().getHelmCharts().forEach(chart -> chart.setState(null));
        priorToMessage.getVnfInstance().setTempInstance(objectMapper.writeValueAsString(priorToMessage.getVnfInstance()));

        whenOnboardingRespondsWith("scale/descriptor-model.json");

        ScaleVnfRequest request = createScaleRequest(ScaleVnfRequest.TypeEnum.OUT, 1, "Payload");
        VnfInstance tempInstance = scaleService.createTempInstance(priorToMessage.getVnfInstance(), request);
        priorToMessage.getVnfInstance().setTempInstance(objectMapper.writeValueAsString(tempInstance));

        vnfInstanceRepository.save(priorToMessage.getVnfInstance());
        lifecycleOperationRepository.save(priorToMessage);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        // Scale levels initial values
        checkScaleLevel(priorToMessage.getVnfInstance(), 5);

        whenWfsScaleRespondsWithAccepted();

        scaleOperation.completed(completed);

        // Operation is still processing
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getStateEnteredTime()).isBefore(LocalDateTime.now());

        // Scale levels should not be increased
        VnfInstance instance = operation.getVnfInstance();
        checkScaleLevel(instance, 5);
        List<ScaleInfoEntity> scaleInfoEntities;

        // Chart State has been set to completed;
        String tempInstanceString = instance.getTempInstance();
        VnfInstance scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName,HelmReleaseState.PROCESSING.toString()));

        completed.setReleaseName(secondReleaseName);
        scaleOperation.completed(completed);

        // Operation has been updated
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isBefore(operation.getStateEnteredTime());
        assertThat(operation.getTargetVnfdId()).isEqualTo(operation.getVnfInstance().getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(operation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(operation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());

        // Scale levels should not be increased
        instance = operation.getVnfInstance();
        scaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(instance);
        assertThat(scaleInfoEntities).extracting("scaleLevel").containsExactly(6);

        // Chart state has been set to completed.
        helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()));
    }

    private ScaleVnfRequest createScaleRequest(ScaleVnfRequest.TypeEnum type, int numberOfSteps, String aspectId) {
        ScaleVnfRequest request = new ScaleVnfRequest();
        request.setType(type);
        request.setNumberOfSteps(numberOfSteps);
        request.setAspectId(aspectId);
        return request;
    }

    private HelmReleaseLifecycleMessage createMessage(final HelmReleaseState state, final String releaseName) {
        final HelmReleaseLifecycleMessage message = new HelmReleaseLifecycleMessage();
        message.setReleaseName(releaseName);
        message.setState(state);
        message.setOperationType(HelmReleaseOperationType.SCALE);
        message.setRevisionNumber("1");
        return message;
    }

    @Test
    public void operationInProcessingWhichFailsSingleHelmChart() {
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, "multiple-sca4e-1");
        failed.setMessage("Failed to scale");
        final String lifecycleOperationId = "sca4e-6a0e-443c-9b0c-4da9d9c2ab71";
        setOperationTimeouts(lifecycleOperationId);
        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        checkScaleLevel(priorToMessage.getVnfInstance(), 3);

        whenWfsRollbackRespondsWithAccepted();

        scaleOperation.failed(failed);

        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to scale");

        // Chart has been updated
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts.get(0).getState()).isEqualTo(HelmReleaseState.ROLLING_BACK.toString());

        rollbackOperation.completed(failed);

        // Operation has been updated
        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to scale");

        // Chart has been updated
        scaledInstance = operationOccurrenceId.getVnfInstance();
        List<HelmChart> charts = scaledInstance.getHelmCharts();
        assertThat(charts.get(0).getState()).isEqualTo(HelmReleaseState.ROLLED_BACK.toString());
        charts = operationOccurrenceId.getVnfInstance().getHelmCharts();
        assertThat(charts.get(0).getState()).isEqualTo(HelmReleaseState.ROLLED_BACK.toString());

        // Scale level should not change
        checkScaleLevel(operationOccurrenceId.getVnfInstance(), 3);
    }

    @Test
    public void operationInProcessingWhichFailsFirstOfMultipleHelmCharts() {
        final String firstReleaseName = "multiple-sca5e-1";
        final String secondReleaseName = "multiple-sca5e-2";
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, firstReleaseName);
        failed.setMessage("Failed to scale");
        final String lifecycleOperationId = "sca5e-6a0e-443c-9b0c-4da9d9c2ab71";
        setOperationTimeouts(lifecycleOperationId);

        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        checkScaleLevel(priorToMessage.getVnfInstance(), 3);

        whenWfsRollbackRespondsWithAccepted();

        scaleOperation.failed(failed);

        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to scale");

        // Chart has been updated
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.ROLLING_BACK.toString()),
                        tuple(secondReleaseName, null));

        rollbackOperation.completed(failed);

        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to scale");

        // Chart has been updated
        scaledInstance = operationOccurrenceId.getVnfInstance();
        List<HelmChart> charts = scaledInstance.getHelmCharts();
        assertThat(charts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.ROLLED_BACK.toString()),
                        tuple(secondReleaseName, null));
        //Scale level should not be updated
        checkScaleLevel(operationOccurrenceId.getVnfInstance(), 3);
    }

    @Test
    public void operationInProcessingWhichFailsSecondOfMultipleHelmCharts() {
        final String firstReleaseName = "multiple-sca6e-1";
        final String secondReleaseName = "multiple-sca6e-2";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, firstReleaseName);
        final String lifecycleOperationId = "sca6e-6a0e-443c-9b0c-4da9d9c2ab71";
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        byOperationOccurrenceId.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        byOperationOccurrenceId.setApplicationTimeout("80");
        lifecycleOperationRepository.save(byOperationOccurrenceId);
        completed.setLifecycleOperationId(lifecycleOperationId);

        whenWfsScaleRespondsWithAccepted();

        scaleOperation.completed(completed);

        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, secondReleaseName);
        failed.setMessage("Failed to scale");

        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        checkScaleLevel(priorToMessage.getVnfInstance(), 3);

        whenWfsRollbackRespondsWithAccepted();
        whenWfsTerminatedRespondsWithAccepted();

        scaleOperation.failed(failed);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.PROCESSING.toString()));

        scaleOperation.failed(failed);
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        tempInstanceString = operation.getVnfInstance().getTempInstance();
        scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.COMPLETED.toString()),
                        tuple(secondReleaseName, HelmReleaseState.PROCESSING.toString()));

        scaleOperation.failed(failed);
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operation.getStateEnteredTime())).isTrue();
        assertThat(operation.getError()).contains("Failed to scale");

        // Chart has been updated
        tempInstanceString = operation.getVnfInstance().getTempInstance();
        scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.PROCESSING.toString()),
                        tuple(secondReleaseName, HelmReleaseState.ROLLING_BACK.toString()));

        rollbackOperation.completed(failed);

        LifecycleOperation operationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operationOccurrenceId.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(prior.isBefore(operationOccurrenceId.getStateEnteredTime())).isTrue();
        assertThat(operationOccurrenceId.getError()).contains("Failed to scale");

        // Chart has been updated
        tempInstanceString = operationOccurrenceId.getVnfInstance().getTempInstance();
        scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> charts = scaledInstance.getHelmCharts();
        assertThat(charts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, HelmReleaseState.ROLLING_BACK.toString()),
                        tuple(secondReleaseName, HelmReleaseState.ROLLED_BACK.toString()));
        checkScaleLevel(operationOccurrenceId.getVnfInstance(), 3);
    }

    private void setTempInstance(final String lifecycleOperationId) {
        LifecycleOperation priorToTest = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        VnfInstance vnfInstance = priorToTest.getVnfInstance();
        vnfInstance.setTempInstance(convertObjToJsonString(vnfInstance));
        vnfInstanceRepository.save(vnfInstance);
    }

    @Test
    public void operationInProcessingWhichCompletesMultipleHelmChartsShouldNotScaleCrdCharts() throws JsonProcessingException {
        final String firstReleaseName = "multiple-sca9e-1";
        final String secondReleaseName = "multiple-sca9e-2";
        final String thirdReleaseName = "multiple-sca9e-3";
        final HelmReleaseLifecycleMessage completed = createMessage(HelmReleaseState.COMPLETED, secondReleaseName);
        final String lifecycleOperationId = "sca9e-6a0e-443c-9b0c-4da9d9c2ab71";
        completed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        priorToMessage.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        priorToMessage.setApplicationTimeout("80");
        priorToMessage.getVnfInstance().getHelmCharts().forEach(chart -> chart.setState(null));

        whenOnboardingRespondsWith("scale/descriptor-model.json");

        ScaleVnfRequest request = createScaleRequest(ScaleVnfRequest.TypeEnum.OUT, 1, "Payload");
        VnfInstance tempInstance = scaleService.createTempInstance(priorToMessage.getVnfInstance(), request);
        priorToMessage.getVnfInstance().setTempInstance(objectMapper.writeValueAsString(tempInstance));

        vnfInstanceRepository.save(priorToMessage.getVnfInstance());
        lifecycleOperationRepository.save(priorToMessage);
        LocalDateTime prior = priorToMessage.getStateEnteredTime();
        // Scale levels initial values
        checkScaleLevel(priorToMessage.getVnfInstance(), 3);

        whenWfsScaleRespondsWithAccepted();

        scaleOperation.completed(completed);

        // Operation is still processing
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.PROCESSING);
        assertThat(operation.getStateEnteredTime()).isBefore(LocalDateTime.now());

        // Scale levels should not be increased
        VnfInstance instance = operation.getVnfInstance();
        checkScaleLevel(instance, 3);

        // Chart State has been set to completed except crd chart;
        String tempInstanceString = instance.getTempInstance();
        VnfInstance scaledInstance = parseJson(tempInstanceString, VnfInstance.class);
        List<HelmChart> helmCharts = scaledInstance.getHelmCharts();
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, null),
                          tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()),
                          tuple(thirdReleaseName,HelmReleaseState.PROCESSING.toString()));

        completed.setReleaseName(thirdReleaseName);
        scaleOperation.completed(completed);

        // Operation has been updated
        operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(prior).isBefore(operation.getStateEnteredTime());
        assertThat(operation.getTargetVnfdId()).isEqualTo(operation.getVnfInstance().getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(operation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(operation.getVnfInstance().getCombinedValuesFile());

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());

        // Scale levels should be increased
        instance = operation.getVnfInstance();
        checkScaleLevel(instance, 4);

        // Chart state has been set to completed except crd chart.
        helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts).extracting("releaseName", "state")
                .contains(tuple(firstReleaseName, null),
                          tuple(secondReleaseName, HelmReleaseState.COMPLETED.toString()),
                          tuple(thirdReleaseName, HelmReleaseState.COMPLETED.toString()));

        verify(workflowRoutingService, times(0)).routeScaleRequest(any(VnfInstance.class), any(LifecycleOperation.class),
                                                                   any(ScaleVnfRequest.class));
        verify(workflowRoutingService, times(0)).routeScaleRequest(eq(1), any(LifecycleOperation.class), any(VnfInstance.class));
        verify(workflowRoutingService, times(0)).routeScaleRequest(eq(2), any(LifecycleOperation.class), any(VnfInstance.class));
        verify(workflowRoutingService, times(1)).routeScaleRequest(eq(3), any(LifecycleOperation.class), any(VnfInstance.class));
    }

    @Test
    public void operationInProcessingWhichFailsWithNoErrorMessage() {
        final HelmReleaseLifecycleMessage failed = createMessage(HelmReleaseState.FAILED, "multiple-sca3e-1");
        final String lifecycleOperationId = "sca3e-8084-4975-9d33-0577aedc61b7";
        setOperationTimeouts(lifecycleOperationId);
        failed.setLifecycleOperationId(lifecycleOperationId);
        LifecycleOperation priorToMessage = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        checkScaleLevel(priorToMessage.getVnfInstance(), 6);

        whenWfsRollbackRespondsWithAccepted();

        scaleOperation.failed(failed);
        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLING_BACK);
        assertThat(operation.getError()).contains("Failure event");
        checkScaleLevel(priorToMessage.getVnfInstance(), 6);
    }

    private void checkScaleLevel(final VnfInstance vnfInstance, final int expectedScaleLevel) {
        List<ScaleInfoEntity> preScaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(vnfInstance);
        assertThat(preScaleInfoEntities).extracting("scaleLevel").containsExactly(expectedScaleLevel);
    }

    private VnfInstance verifyScaleOperation(String lifecycleOperationId, String targetReplicas, int targetScaleLevel) {
        // Operation has been updated
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);

        // Instance has been updated
        VnfInstance instance = validateInstanceScaleParams(operation, targetReplicas, targetReplicas);

        // HelmChartHistoryRecords was stored
        assertThat(helmChartHistoryService.getHelmChartHistoryRecordsByOperationId(lifecycleOperationId).size())
                .isEqualTo(instance.getHelmCharts().size());

        // Chart state has been set to completed.
        List<HelmChart> helmCharts = helmChartRepository.findByVnfInstance(instance);
        assertThat(helmCharts.get(0).getState()).isEqualTo(HelmReleaseState.COMPLETED.toString());

        // Scale levels have been set to new levels
        checkScaleLevel(instance, targetScaleLevel);
        return instance;
    }

    private VnfInstance validateInstanceScaleParams(LifecycleOperation operation, String target1Replicas, String target2Replicas) {
        VnfInstance instance = operation.getVnfInstance();
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isNotNull();

        // Check for the correct Replica
        JSONObject currentScaleLevel = new JSONObject(instance.getResourceDetails());
        assertThat(currentScaleLevel.get("PL__scaled_vm").toString()).isEqualTo(target1Replicas);
        assertThat(currentScaleLevel.get("TL_scaled_vm").toString()).isEqualTo(target2Replicas);

        assertThat(operation.getTargetVnfdId()).isEqualTo(operation.getVnfInstance().getVnfDescriptorId());
        assertThat(operation.getCombinedAdditionalParams()).isEqualTo(operation.getVnfInstance().getCombinedAdditionalParams());
        assertThat(operation.getCombinedValuesFile()).isEqualTo(operation.getVnfInstance().getCombinedValuesFile());
        assertThat(operation.getStateEnteredTime()).isBefore(LocalDateTime.now());
        return instance;
    }

    private void sendCompletedScaleMessage(HelmReleaseLifecycleMessage completed, LifecycleOperation priorToOperation,
                                           ScaleVnfRequest.TypeEnum type) throws JsonProcessingException {

        ScaleVnfRequest request = createScaleRequest(type, 1, "Payload");
        VnfInstance tempInstance = scaleService.createTempInstance(priorToOperation.getVnfInstance(), request);
        priorToOperation.getVnfInstance().setTempInstance(objectMapper.writeValueAsString(tempInstance));

        vnfInstanceRepository.save(priorToOperation.getVnfInstance());
        scaleOperation.completed(completed);
    }

    private void whenWfsTerminatedRespondsWithAccepted() {
        when(restTemplate.exchange(contains("terminate"), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private void whenWfsRollbackRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("rollback");
    }

    private void whenWfsScaleRespondsWithAccepted() {
        whenWfsRespondsWithAccepted("scale");
    }

    @SuppressWarnings("unchecked")
    private void whenWfsRespondsWithAccepted(final String urlEnd) {
        when(restTemplate.exchange(endsWith(urlEnd), eq(HttpMethod.POST), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<ResourceResponse>(HttpStatus.ACCEPTED));
    }

    private void whenOnboardingRespondsWith(final String fileName) {
        when(packageService.getVnfd(anyString())).thenReturn(new JSONObject(readDataFromFile(getClass(), fileName)));
    }

    private void setOperationTimeouts(final String lifecycleOperationId) {
        LifecycleOperation byOperationOccurrenceId = lifecycleOperationRepository
                .findByOperationOccurrenceId(lifecycleOperationId);
        byOperationOccurrenceId.setApplicationTimeout("80");
        byOperationOccurrenceId.setExpiredApplicationTime(LocalDateTime.now().plusSeconds(200));
        lifecycleOperationRepository.save(byOperationOccurrenceId);
    }
}
