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
package com.ericsson.vnfm.orchestrator.messaging.handlers;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpdateTempInstanceChartState extends MessageHandler<HelmReleaseLifecycleMessage> {

    private LifecycleOperationState state;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling updating temp instance");
        HelmReleaseLifecycleMessage message = context.getMessage();
        VnfInstance instance = context.getVnfInstance();
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        tempInstance.getHelmCharts().stream()
                .filter(chart -> StringUtils.equals(chart.getReleaseName(), message.getReleaseName()))
                .findFirst()
                .ifPresent(chart -> {
                    chart.setState(state.toString());
                    chart.setRevisionNumber(message.getRevisionNumber());
                });
        instance.setTempInstance(convertObjToJsonString(tempInstance));
        passToSuccessor(getSuccessor(), context);
    }
}
