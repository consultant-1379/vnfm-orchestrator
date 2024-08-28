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

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EvnfmDowngrade implements RollbackOperation {

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
        return RollbackType.DOWNGRADE_ROLLBACK;
    }

    @Override
    public void execute(LifecycleOperation operation) {
        LOGGER.info("Starting Downgrade command");
        VnfInstance actualInstance = operation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        List<HelmChart> actualHelmCharts = actualInstance.getHelmCharts();
        List<HelmChart> tempHelmCharts = tempInstance.getHelmCharts();

        List<LinkedHashMap<String, String>> rollbackPatternList = Utility.parseJsonToGenericType(
                operation.getRollbackPattern(), new TypeReference<>() { });
        LOGGER.info("for instance {}, executing the following pattern {} for operation {}", actualInstance.getVnfInstanceId(),
                rollbackPatternList, operation);
        ImmutablePair<HelmChart, String> firstHelmChartCommandPair = OperationsUtils.getFirstHelmCommandPair(
                rollbackPatternList, actualHelmCharts, tempHelmCharts, false);
        firstHelmChartCommandPair.getLeft().setState(LifecycleOperationState.PROCESSING.toString());
        actualInstance.setTempInstance(Utility.convertObjToJsonString(tempInstance));

        LOGGER.info("Executing {} on the helm helm chart {}, release name {}", firstHelmChartCommandPair.getRight(),
                    firstHelmChartCommandPair.getLeft().getHelmChartUrl(), firstHelmChartCommandPair.getLeft().getReleaseName());
        Command command = getService(firstHelmChartCommandPair.getRight().toUpperCase());
        command.execute(operation, firstHelmChartCommandPair.getLeft(), true);
    }

    @Override
    public void triggerNextStage(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        triggerNextStage(context, true, utility, instanceService, helmChartHistoryService);
    }

    @Override
    public Command getService(String service) {
        return ccvpPatternCommandFactory.getService(service);
    }
}
