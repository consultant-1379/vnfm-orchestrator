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
package com.ericsson.vnfm.orchestrator.e2e.instantiate;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.OperationalState;
import com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import static com.ericsson.vnfm.orchestrator.TestUtils.BRO_ENDPOINT_URL;
import static com.ericsson.vnfm.orchestrator.TestUtils.DEFAULT_CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CLEAN_UP_RESOURCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class InstantiateNegativeTest extends AbstractEndToEndTest {

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Test
    public void singleHelmChartInstantiateFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "single-chart-instantiate-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        verifyVnfInstanceDetails(instance, releaseName, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);

        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(failedOperation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("FAILED", firstHelmReleaseNameFor(releaseName)));

        // verify VnfInstanceNamespaceDetails exist but delete namespace set to false
        final Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartInstantiateFailsFirstChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "multi-chart-instantiate-first-chart-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart-1");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate,1, INSTANTIATE_URL_ENDING);
        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(failedOperation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").contains(tuple("FAILED", firstHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartInstantiateFailsSecondChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "multi-chart-instantiate-second-chart-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart-2");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // verify VnfInstanceNamespaceDetails do not exist
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId.isPresent()).isFalse();

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        // verify VnfInstanceNamespaceDetails exist delete namespace false
        byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompletedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                 HelmReleaseState.COMPLETED,
                                                                                                 lifeCycleOperationId,
                                                                                                 HelmReleaseOperationType.INSTANTIATE,
                                                                                                 "1");
        instantiateCompletedMessage.setMessage("Instantiate completed");
        testingMessageSender.sendMessage(instantiateCompletedMessage);

        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation).isNotNull();
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(operation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");

        // verify VnfInstanceNamespaceDetails exist and delete namespace is set to false
        byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName)), tuple(
                "FAILED", secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void instantiateFailsForInvalidBroUrl() throws Exception {
        // create identifier
        final String releaseName = "instantiate-invalid-bro";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "instantiate-invalid-bro");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // verify VnfInstanceNamespaceDetails do not exist
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId.isPresent()).isFalse();

        // make instantiate request
        String instantiateRequest = createInstantiateVnfRequestWithInvalidBroUrl(releaseName, "invalid-bro-url");
        MvcResult result = requestHelper.makePostRequest(instantiateRequest,
                                                         vnfInstanceResponse.getId(),
                                                         VnfInstancesControllerImplIntegrationTest.INSTANTIATE);

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Invalid Input Exception\","
                                   + "\"status\":400,\"detail\":\"The Url : invalid-bro-url "
                                   + "is invalid due to no protocol: invalid-bro-url. Please provide a valid URL.\","
                                   + "\"instance\":\"" + createVnfInstanceUri(vnfInstanceResponse.getId()) + "\"}");
        // verify VnfInstanceNamespaceDetails have been deleted on error thrown above
        byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId.isPresent()).isFalse();
    }

    @Test
//    @Ignore("Ignoring due to a bug in merging values - https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/SM-81896")
    public void instantiateFailsForNullValueInAdditionalParams() throws Exception {
        // create identifier
        final String releaseName = "instantiate-null-bro";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "instantiate-null-bro");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        String instantiateRequest = createInstantiateVnfRequestWithInvalidBroUrl(releaseName, null);
        MvcResult result = requestHelper.makePostRequest(instantiateRequest,
                                                         vnfInstanceResponse.getId(),
                                                         VnfInstancesControllerImplIntegrationTest.INSTANTIATE);

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Malformed Request\","
                                   + "\"status\":400,\"detail\":"
                                   + "\"You cannot merge yaml where value is null for bro_endpoint_url\","
                                   + "\"instance\":\"" + createVnfInstanceUri(vnfInstanceResponse.getId()) + "\"}");
    }

    @Test
    public void createVnfInstanceFailsForPackageInDisableState() throws Exception {
        final String vnfdId = "multi-helm-chart-disabled";
        String response = requestHelper.executeCreateVnfRequest(vnfdId);

        assertThat(response).isEqualTo("{\"type\":\"about:blank\",\"title\":\"VNF package is unprocessable with provided VNFD ID\"," +
                "\"status\":422,\"detail\":\"Package " + vnfdId + " rejected due to its " + OperationalState.DISABLED + " state\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void instantiateFailsForEmptyBroUrl() throws Exception {
        // create identifier
        final String releaseName = "instantiate-empty-bro";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "instantiate-empty-bro");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        String instantiateRequest = createInstantiateVnfRequestWithInvalidBroUrl(releaseName, " ");
        MvcResult result = requestHelper.makePostRequest(instantiateRequest,
                                                         vnfInstanceResponse.getId(),
                                                         VnfInstancesControllerImplIntegrationTest.INSTANTIATE);

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Invalid Input Exception\","
                                   + "\"status\":400,\"detail\":\"The Url :  is invalid "
                                   + "due to no protocol: . Please provide a valid URL.\","
                                   + "\"instance\":\"" + createVnfInstanceUri(vnfInstanceResponse.getId()) + "\"}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void multiHelmChartWithLevelsInstantiateFailsSecondChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "multi-chart-levels-second-chart-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "levels-no-vdu");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        //create extensions
        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            releaseName,
                                                                                            INST_LEVEL_2,
                                                                                            extensions,
                                                                                            false);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompletedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                 HelmReleaseState.COMPLETED,
                                                                                                 lifeCycleOperationId,
                                                                                                 HelmReleaseOperationType.INSTANTIATE,
                                                                                                 "1");
        instantiateCompletedMessage.setMessage("Instantiate completed");
        testingMessageSender.sendMessage(instantiateCompletedMessage);

        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation).isNotNull();
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(operation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        //verify that extensions and new instantiation level has not been saved
        assertThat(vnfInstance.getInstantiationLevel()).isNull();
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");
        Map<String, Object> defaultExtensions =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(defaultExtensions.get(PAYLOAD)).isEqualTo(MANUAL_CONTROLLED); //PL & CL
        assertThat(defaultExtensions.get(PAYLOAD_2)).isEqualTo(CISM_CONTROLLED); //TL
        assertThat(defaultExtensions.get(PAYLOAD_3)).isEqualTo(MANUAL_CONTROLLED); //JL

        // verify VnfInstanceNamespaceDetails exist and delete namespace is set to false
        final Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // verify state of chart and ensure replicaDetails are empty
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName", "replicaDetails")
                .containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName), null), tuple(
                        "FAILED", secondHelmReleaseNameFor(releaseName), null));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartInstantiateFailsWFSRequestSecondChart() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "second-chart-wfs-request-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart-3");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // invalidate request to fail WFS request on second chart
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        instance.setClusterName("test+");
        vnfInstanceRepository.save(instance);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompletedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                 HelmReleaseState.COMPLETED,
                                                                                                 lifeCycleOperationId,
                                                                                                 HelmReleaseOperationType.INSTANTIATE,
                                                                                                 "1");
        instantiateCompletedMessage.setMessage("Instantiate completed");
        testingMessageSender.sendMessage(instantiateCompletedMessage);

        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation).isNotNull();
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(operation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName)), tuple(
                "FAILED", secondHelmReleaseNameFor(releaseName)));

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void multiHelmChartInstantiateFailedMessageSecondChartTearDownFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        // 5 TERMINATE STARTING
        // 6 TERMINATE PROCESSING
        // 7 TERMINATE FAILED
        int expectedCountOfCallsMessagingService = 7;

        // create identifier
        final String releaseName = "second-chart-teardown-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "multi-helm-chart-4");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        // send instantiate completed message
        HelmReleaseLifecycleMessage instantiateCompletedMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                 HelmReleaseState.COMPLETED,
                                                                                                 lifeCycleOperationId,
                                                                                                 HelmReleaseOperationType.INSTANTIATE,
                                                                                                 "1");
        instantiateCompletedMessage.setMessage("Instantiate completed");
        testingMessageSender.sendMessage(instantiateCompletedMessage);

        // send instantiate failed message
        HelmReleaseLifecycleMessage instantiateFailedMessage = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                                                                                              HelmReleaseState.FAILED,
                                                                                              lifeCycleOperationId,
                                                                                              HelmReleaseOperationType.INSTANTIATE,
                                                                                              "1");
        instantiateFailedMessage.setMessage("Instantiate failed");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation).isNotNull();
        assertThat(operation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(operation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");

        // verify VnfInstanceNamespaceDetails exist and delete namespace is set to false
        final Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstanceResponse.getId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("COMPLETED", firstHelmReleaseNameFor(releaseName)), tuple(
                "FAILED", secondHelmReleaseNameFor(releaseName)));

        // send teardown request
        MvcResult cleanUpResult = requestHelper.getMvcResultCleanUpRequest(vnfInstanceResponse.getId());
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String cleanUpOperation = getLifeCycleOperationId(cleanUpResult);

        await().until(awaitHelper.helmChartReachesState(firstHelmReleaseNameFor(releaseName),
                                                        vnfInstanceResponse.getId(),
                                                        HelmReleaseState.PROCESSING,
                                                        false));

        // send teardown failed messages
        HelmReleaseLifecycleMessage cleanUpFailedFirstChartMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                                    HelmReleaseState.FAILED,
                                                                                                    cleanUpOperation,
                                                                                                    HelmReleaseOperationType.TERMINATE,
                                                                                                    "1");
        cleanUpFailedFirstChartMessage.setMessage("Teardown failed");
        testingMessageSender.sendMessage(cleanUpFailedFirstChartMessage);

        await().until(awaitHelper.operationReachesState(cleanUpOperation, LifecycleOperationState.FAILED));

        // verify state of teardown
        LifecycleOperation cleanUp = lifecycleOperationRepository.findByOperationOccurrenceId(cleanUpOperation);
        assertThat(cleanUp).isNotNull();
        assertThat(cleanUp.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.TERMINATE);
        assertThat(cleanUp.getError()).contains("Teardown failed");

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    @Test
    public void singleHelmChartInstantiateCRDFails() throws Exception {
        // Set value of expectedCountOfCallsMessagingService
        // 1 IDENTIFIER CREATION
        // 2 INSTANTIATE STARTING
        // 3 INSTANTIATE PROCESSING
        // 4 INSTANTIATE FAILED
        int expectedCountOfCallsMessagingService = 4;

        // create identifier
        final String releaseName = "single-crd-instantiate-fails";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "single-helm-chart");
        assertThat(vnfInstanceResponse.getInstantiationState()).isEqualTo(VnfInstanceResponse.InstantiationStateEnum.NOT_INSTANTIATED);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        // make instantiate request
        MvcResult result = requestHelper.getMvcResultInstantiateRequest(vnfInstanceResponse, releaseName, DEFAULT_CLUSTER_NAME, true);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        verifyVnfInstanceDetails(instance, releaseName, true);

        // send instantiate failed message
        WorkflowServiceEventMessage instantiateFailedMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                                  WorkflowServiceEventType.CRD,
                                                                                  WorkflowServiceEventStatus.FAILED,
                                                                                  "Instantiated CRD chart.");
        instantiateFailedMessage.setMessage("Instantiate failed");
        instantiateFailedMessage.setReleaseName("single-crd-instantiate-fails-1");
        testingMessageSender.sendMessage(instantiateFailedMessage);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.FAILED));

        // verify state of operation
        LifecycleOperation failedOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(failedOperation).isNotNull();
        assertThat(failedOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.INSTANTIATE);
        assertThat(failedOperation.getError()).contains("Instantiate failed");

        // verify state of instance
        VnfInstance vnfInstance = failedOperation.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(vnfInstance.getClusterName()).isNotNull();
        assertThat(vnfInstance.getNamespace()).isNotNull().matches(releaseName);
        assertThat(vnfInstance.getTempInstance()).isNotNull();
        assertThat(vnfInstance.getCombinedValuesFile()).isNotNull().containsIgnoringCase("tags").containsIgnoringCase("pm");
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.8");

        // verify state of chart
        List<HelmChart> charts = vnfInstance.getHelmCharts();
        assertThat(charts).extracting("state", "releaseName").containsOnly(tuple("FAILED", firstHelmReleaseNameFor(releaseName)));

        // verify VnfInstanceNamespaceDetails exist but delete namespace set to false
        final Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isPresent();
        assertThat(byVnfId.get().isDeletionInProgress()).isFalse();

        // Verify that the message has been sent the correct number of times
        verify(messagingService, times(expectedCountOfCallsMessagingService)).sendMessage(any());
    }

    private String createInstantiateVnfRequestWithInvalidBroUrl(final String namespace, final String broEndpointUrl) throws JsonProcessingException {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.clusterName(EXISTING_CLUSTER_CONFIG_NAME);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, namespace);
        additionalParams.put("tags.pm", "true");
        additionalParams.put(CLEAN_UP_RESOURCES, true);
        additionalParams.put(HELM_NO_HOOKS, true);
        additionalParams.putAll(TestUtils.getComplexTypeAdditionalParams());
        additionalParams.put(BRO_ENDPOINT_URL, broEndpointUrl);
        request.setAdditionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private void verifyVnfInstanceDetails(final VnfInstance instance, String namespace, boolean isDeletionInProgress) {
        ClusterConfigFile configFileByName = clusterConfigService.getConfigFileByName(DEFAULT_CLUSTER_NAME);
        String clusterServer = configFileByName.getClusterServer();
        Optional<VnfInstanceNamespaceDetails> byVnfId = vnfInstanceNamespaceDetailsRepository
                .findByVnfId(instance.getVnfInstanceId());
        assertThat(byVnfId).isPresent();
        VnfInstanceNamespaceDetails details = byVnfId.get();
        assertThat(details.isDeletionInProgress()).isEqualTo(isDeletionInProgress);
        assertThat(details.getNamespace()).isEqualTo(namespace);
        assertThat(details.getClusterServer()).contains(clusterServer);
    }

    private String createVnfInstanceUri(String vnfInstanceId) {
        return HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + vnfInstanceId;
    }
}
