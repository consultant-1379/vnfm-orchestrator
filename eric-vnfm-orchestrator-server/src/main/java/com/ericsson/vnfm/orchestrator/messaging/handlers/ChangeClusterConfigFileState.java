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

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ChangeClusterConfigFileState<T> extends MessageHandler<T> {

    private final ClusterConfigService clusterConfigService;
    private final ConfigFileStatus configFileStatus;

    @Override
    public void handle(final MessageHandlingContext<T> context) {
        VnfInstance vnfInstance = context.getVnfInstance();
        final String clusterName = vnfInstance.getClusterName();
        LOGGER.info("Handling Change cluster config file state. Cluster name - {}, Status - {}", clusterName, configFileStatus);
        clusterConfigService.changeClusterConfigFileStatus(clusterName, vnfInstance, configFileStatus);
        passToSuccessor(getSuccessor(), context);
    }
}
