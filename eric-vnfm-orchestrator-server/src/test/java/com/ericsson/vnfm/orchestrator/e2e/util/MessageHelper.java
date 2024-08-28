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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static java.util.Comparator.comparing;

import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestComponent
@Import(TestingMessageSender.class)
public class MessageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TestingMessageSender testingMessageSender;

    @Autowired
    private AwaitHelper awaitHelper;

    public void sendMessageForChart(final HelmReleaseLifecycleMessage message,
                                    final String releaseName,
                                    final String vnfInstanceId,
                                    final boolean checkTempInstance,
                                    final HelmReleaseState expectedState) {

        message.setReleaseName(releaseName);
        testingMessageSender.sendMessage(message);
        await().until(awaitHelper.helmChartReachesState(releaseName, vnfInstanceId, expectedState, checkTempInstance));
    }

    public void sendMessageForDeletedChart(final HelmReleaseLifecycleMessage message,
                                           final String releaseName,
                                           final String vnfInstanceId,
                                           final boolean checkTempInstance) {

        message.setReleaseName(releaseName);
        testingMessageSender.sendMessage(message);
        await().until(awaitHelper.helmChartDeleted(releaseName, vnfInstanceId, checkTempInstance));
    }

    public void sendMessageForChart(final WorkflowServiceEventMessage message,
                                    final String releaseName,
                                    final String vnfInstanceId,
                                    final boolean checkTempInstance,
                                    final HelmReleaseState expectedState) {

        message.setReleaseName(releaseName);
        testingMessageSender.sendMessage(message);
        await().until(awaitHelper.helmChartReachesState(releaseName, vnfInstanceId, expectedState, checkTempInstance));
    }

    public void sendInternalApiMessageForAllCrdCharts(final WorkflowServiceEventMessage message,
                                                      final String vnfInstanceId,
                                                      final HelmReleaseOperationType helmReleaseOperationType,
                                                      final LifecycleOperationState expectedOperationsState,
                                                      final boolean isChartEnabled, final boolean checkTempInstance) throws JsonProcessingException {
        VnfInstance vnfInstance;
        if (helmReleaseOperationType.equals(HelmReleaseOperationType.CHANGE_VNFPKG)) {
            VnfInstance sourceVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            vnfInstance = mapper.readValue(sourceVnfInstance.getTempInstance(), VnfInstance.class);
        } else {
            vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        }
        List<HelmChart> crdCharts = vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartType() == HelmChartType.CRD && chart.isChartEnabled() == isChartEnabled)
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toUnmodifiableList());
        for (HelmChart helmChart : crdCharts) {
            message.setReleaseName(helmChart.getReleaseName());
            testingMessageSender.sendMessage(message);
            try {
                await().atMost(60, TimeUnit.SECONDS)
                        .until(awaitHelper.helmChartReachesState(helmChart.getReleaseName(), vnfInstanceId,
                                                                 HelmReleaseState.COMPLETED, checkTempInstance));
            } catch (ConditionTimeoutException cte) {
                LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
                LOGGER.error("Failed waiting for operation to reach state {}. It was still {}",
                             expectedOperationsState,
                             operation.getOperationState());
                throw cte;
            }
        }
    }

    public void sendInternalApiFailedMessageForCRDChart(final WorkflowServiceEventMessage message,
                                                        final String vnfInstanceId,
                                                        final HelmReleaseState expectedHelmState) {

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        Optional<HelmChart> helmChart = vnfInstance.getHelmCharts().stream()
                .filter(chart -> chart.getHelmChartName().equals(message.getReleaseName()))
                .findFirst();

        if (helmChart.isPresent()) {
            testingMessageSender.sendMessage(message);
            try {
                await().atMost(50, TimeUnit.SECONDS)
                        .until(awaitHelper.helmChartReachesState(message.getReleaseName(),
                                                                 vnfInstanceId,
                                                                 expectedHelmState,
                                                                 false));
            } catch (ConditionTimeoutException cte) {
                LOGGER.error("Failed waiting for helmChart to reach state {}. It was still {}",
                             expectedHelmState,
                             helmChart.get().getState());
                throw cte;
            }
        } else {
            fail("Helm chart with release Name :" + message.getReleaseName() + "was not found");
        }
    }

    public void sendDownsizeMessageForAllCharts(final WorkflowServiceEventMessage message,
                                                final String vnfInstanceId,
                                                final boolean checkTempInstance) {

        final VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        final List<HelmChart> helmCharts = vnfInstance.getHelmCharts().stream()
                .sorted(comparing(HelmChartBaseEntity::getPriority).reversed())
                .toList();
        for (final HelmChart helmChart : helmCharts) {
            message.setReleaseName(helmChart.getReleaseName());
            testingMessageSender.sendMessage(message);
            await().until(awaitHelper.helmChartDownSizeReachesState(helmChart.getReleaseName(),
                                                                    vnfInstanceId,
                                                                    HelmReleaseState.COMPLETED,
                                                                    checkTempInstance));
        }
    }

    public void sendHealCompleteTerminateMessageForAllCnfCharts(final HelmReleaseLifecycleMessage message,
                                                                final String vnfInstanceId,
                                                                final boolean usesTempInstance) {

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        List<HelmChart> cnfCharts = helmChartRepository.findByVnfInstance(instance)
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());
        int lastChartNumber = cnfCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            HelmChart currentChart = cnfCharts.get(i);
            sendMessageForChart(message, currentChart.getReleaseName(), vnfInstanceId, usesTempInstance, HelmReleaseState.COMPLETED);
        }
        message.setReleaseName(cnfCharts.get(lastChartNumber).getReleaseName());
        testingMessageSender.sendMessage(message);
        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationHasCombinedAdditionalParams(message.getLifecycleOperationId()));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
            LOGGER.error("Failed waiting for operation {} to trigger instantiate.", operation.getOperationOccurrenceId());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }

    public void sendCompleteTerminateMessageForAllCnfCharts(final HelmReleaseLifecycleMessage message,
                                                            final String vnfInstanceId,
                                                            final LifecycleOperationState expectedOperationState) {

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        List<HelmChart> cnfCharts = helmChartRepository.findByVnfInstance(instance)
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());
        int lastChartNumber = cnfCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            HelmChart currentChart = cnfCharts.get(i);
            sendMessageForChart(message, currentChart.getReleaseName(), vnfInstanceId, false, HelmReleaseState.COMPLETED);
        }
        message.setReleaseName(cnfCharts.get(lastChartNumber).getReleaseName());
        testingMessageSender.sendMessage(message);
        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationReachesState(message.getLifecycleOperationId(), expectedOperationState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }

    public void sendCompleteMessageForAllCnfCharts(final HelmReleaseLifecycleMessage message,
                                                   final String vnfInstanceId,
                                                   final boolean usesTempInstance,
                                                   final LifecycleOperationState expectedOperationsState) throws JsonProcessingException {

        VnfInstance instance;
        if (message.getOperationType().equals(HelmReleaseOperationType.CHANGE_VNFPKG)) {
            VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
            instance = mapper.readValue(vnfInstance.getTempInstance(), VnfInstance.class);
        } else {
            instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        }
        List<HelmChart> cnfCharts = instance.getHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());
        int lastChartNumber = cnfCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            HelmChart currentChart = cnfCharts.get(i);
            sendMessageForChart(message, currentChart.getReleaseName(), vnfInstanceId, usesTempInstance, HelmReleaseState.COMPLETED);
        }
        message.setReleaseName(cnfCharts.get(lastChartNumber).getReleaseName());
        testingMessageSender.sendMessage(message);
        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationReachesState(message.getLifecycleOperationId(), expectedOperationsState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationsState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }

    public void sendCompleteTerminateMessagesForUpgradeCnfCharts(final HelmReleaseLifecycleMessage terminateMessage,
                                                                 final HelmReleaseLifecycleMessage deletePvcMessage,
                                                                 final String vnfInstanceId,
                                                                 final LifecycleOperationState expectedOperationsState) throws JsonProcessingException {

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        instance = mapper.readValue(instance.getTempInstance(), VnfInstance.class);

        final String lifecycleOperationId = terminateMessage.getLifecycleOperationId();
        List<TerminatedHelmChart> terminatedCnfCharts = instance.getTerminatedHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .filter(chart -> Objects.equals(chart.getOperationOccurrenceId(), lifecycleOperationId))
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());

        int lastChartNumber = terminatedCnfCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            final String currentReleaseName = terminatedCnfCharts.get(i).getReleaseName();

            sendMessageForChart(terminateMessage, currentReleaseName, vnfInstanceId, true, HelmReleaseState.COMPLETED);

            deletePvcMessage.setReleaseName(currentReleaseName);
            testingMessageSender.sendMessage(deletePvcMessage);
            await().until(awaitHelper.helmChartDeletePvcReachesState(currentReleaseName, vnfInstanceId, HelmReleaseState.COMPLETED, true));
        }

        final String lastReleaseName = terminatedCnfCharts.get(lastChartNumber).getReleaseName();
        sendMessageForChart(terminateMessage, lastReleaseName, vnfInstanceId, true, HelmReleaseState.COMPLETED);

        deletePvcMessage.setReleaseName(lastReleaseName);
        testingMessageSender.sendMessage(deletePvcMessage);

        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationReachesState(lifecycleOperationId, expectedOperationsState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationsState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }

    public void sendCompleteUpgradeMessageForUpgradeCnfCharts(final HelmReleaseLifecycleMessage message, final String vnfInstanceId,
                                                              final LifecycleOperationState expectedOperationsState) throws JsonProcessingException {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        instance = mapper.readValue(instance.getTempInstance(), VnfInstance.class);
        List<HelmChart> upgradedHelmCharts = instance.getHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD && chart.isChartEnabled())
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());
        int lastChartNumber = upgradedHelmCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            HelmChart currentChart = upgradedHelmCharts.get(i);
            sendMessageForChart(message, currentChart.getReleaseName(), vnfInstanceId, true, HelmReleaseState.COMPLETED);
        }
        message.setReleaseName(upgradedHelmCharts.get(lastChartNumber).getReleaseName());
        testingMessageSender.sendMessage(message);
        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationReachesState(message.getLifecycleOperationId(), expectedOperationsState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationsState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }

    public void sendCompleteInstantiateMessageForCnfCharts(final HelmReleaseLifecycleMessage message, final String vnfInstanceId,
                                                           final LifecycleOperationState expectedOperationsState, final boolean isChartEnabled) {

        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        List<HelmChart> terminatedCnfCharts = instance.getHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD && chart.isChartEnabled() == isChartEnabled)
                .sorted(Comparator.comparingInt(HelmChartBaseEntity::getPriority))
                .collect(Collectors.toList());
        int lastChartNumber = terminatedCnfCharts.size() - 1;
        for (int i = 0; i < lastChartNumber; i++) {
            HelmChart currentChart = terminatedCnfCharts.get(i);
            sendMessageForChart(message, currentChart.getReleaseName(), vnfInstanceId, false, HelmReleaseState.COMPLETED);
        }
        message.setReleaseName(terminatedCnfCharts.get(lastChartNumber).getReleaseName());
        testingMessageSender.sendMessage(message);
        try {
            await().timeout(60, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(awaitHelper.operationReachesState(message.getLifecycleOperationId(), expectedOperationsState));
        } catch (ConditionTimeoutException cte) {
            LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(message.getLifecycleOperationId());
            LOGGER.error("Failed waiting for operation {} to reach state {}. It was still {}",
                         operation.getOperationOccurrenceId(),
                         expectedOperationsState,
                         operation.getOperationState());
            LOGGER.error("Operation error message : {}", operation.getError());
            throw cte;
        }
    }
}
