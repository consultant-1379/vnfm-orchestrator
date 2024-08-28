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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

@Component
public class EvnfmAutoRollback implements RollbackOperation {

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private MessageUtility utility;

    @Autowired
    private HelmChartHistoryService helmChartHistoryService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private CcvpPatternCommandFactory ccvpPatternCommandFactory;

    @Override
    public RollbackType getType() {
        return RollbackType.AUTO_ROLLBACK;
    }

    @Override
    public void execute(LifecycleOperation operation) {
        LOGGER.info("Starting Auto Rollback command");
        VnfInstance actualInstance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);

        Optional<ImmutablePair<HelmChart, String>> chartWithCommand = getChartAndRollbackPattern(
                actualInstance, tempInstance, operation
        );
        chartWithCommand.ifPresentOrElse(chartCommandPair -> {
            final Command service = getService(chartCommandPair.getRight());
            service.execute(operation, chartCommandPair.getLeft(), false);
            databaseInteractionService.persistVnfInstanceAndOperation(actualInstance, operation);
        }, () -> {
                throw new NotFoundException("Unable to identify the chart to rollback.");
            }
        );
    }

    private Optional<ImmutablePair<HelmChart, String>> getChartAndRollbackPattern(final VnfInstance actualInstance,
                                                                                  final VnfInstance tempInstance,
                                                                                  final LifecycleOperation operation) {
        Optional<HelmChart> failedHelmChart = OperationsUtils.retrieveFailingUpgradeHelmChart(actualInstance, tempInstance);

        if (failedHelmChart.isPresent()) {
            HelmChart helmChart = failedHelmChart.get();
            LOGGER.info("Starting Rollback operation for chart name: {} ", helmChart.getHelmChartName());
            helmChart.setState(LifecycleOperationState.ROLLING_BACK.toString());
            return Optional.of(ImmutablePair.of(helmChart, CommandType.ROLLBACK.toString()));
        }

        Optional<HelmChart> terminatedHelmChart = HelmChartUtils.getFirstProcessingTerminatedCnfChart(tempInstance, operation);
        Optional<HelmChart> sourceHelmChart = HelmChartUtils.getFirstProcessingCnfChart(actualInstance);
        Optional<HelmChart> targetHelmChart = HelmChartUtils.getFirstProcessingCnfChart(tempInstance);
        if (terminatedHelmChart.isPresent()) {
            HelmChart helmChart = terminatedHelmChart.get();
            LOGGER.info("Starting Install operation for chart name: {} ", helmChart.getHelmChartName());
            helmChart.setState(LifecycleOperationState.PROCESSING.toString());
            return Optional.of(ImmutablePair.of(helmChart, CommandType.INSTANTIATE.toString()));

        } else if (sourceHelmChart.isEmpty() && targetHelmChart.isPresent()) {
            HelmChart helmChart = targetHelmChart.get();
            LOGGER.info("Starting Terminate operation for chart name: {} ", helmChart.getHelmChartName());
            helmChart.setState(LifecycleOperationState.PROCESSING.toString());
            return Optional.of(ImmutablePair.of(helmChart, CommandType.TERMINATE.toString()));

        } else {
            return Optional.empty();
        }
    }

    @Override
    public void triggerNextStage(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        triggerNextStage(context, false, utility, instanceService, helmChartHistoryService);
    }

    @Override
    public Command getService(String service) {
        return ccvpPatternCommandFactory.getService(service);
    }
}
