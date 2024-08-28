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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;


import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CLOSE_SSH_TO_ENM_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.COMPLETED_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CONNECTIONS_TO_ENM_METRIC_TAGS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.ENM_METRIC_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.INSTANTIATE_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.INSTANTIATE_ENROLLMENT_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPEN_SSH_TO_ENM_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.STARTED_ADD_NODE_METRIC_TAG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;

@Service
public final class EnmMetricsExposers {
    private final ConnectionsToEnm connectionsToEnm;
    private final AddNodeOperations addNodeOperations;
    private final InstantiateAddNodeToOSS instantiateAddNodeToOSS;
    private final InstantiateCMPv2Enrollment instantiateCMPv2Enrollment;

    EnmMetricsExposers(MeterRegistry meterRegistry) {
        this.connectionsToEnm = new ConnectionsToEnm(meterRegistry);
        this.addNodeOperations = new AddNodeOperations(meterRegistry);
        this.instantiateAddNodeToOSS = new InstantiateAddNodeToOSS(meterRegistry);
        this.instantiateCMPv2Enrollment = new InstantiateCMPv2Enrollment(meterRegistry);

    }

    public ConnectionsToEnm getConnToEnmMetric() {
        return connectionsToEnm;
    }

    public AddNodeOperations getAddNodeMetric() {
        return addNodeOperations;
    }

    public void incrementInstantiateMetrics(LifecycleOperationType type, Map<String, Object> additionalParams) {
        if (!type.equals(INSTANTIATE) || additionalParams == null) {
            return;
        }

        boolean addNodeToOss = BooleanUtils.getBooleanValue(additionalParams.get(ADD_NODE_TO_OSS), false);
        boolean cmpV2Enrollment = BooleanUtils.getBooleanValue(additionalParams.get(CMP_V2_ENROLLMENT), false);

        if (addNodeToOss) {
            instantiateAddNodeToOSS.increment();
        }

        if (addNodeToOss && cmpV2Enrollment) {
            instantiateCMPv2Enrollment.increment();
        }
    }

    public static final class ConnectionsToEnm {

        private final Counter openSessions;
        private final Counter closeSessions;

        private ConnectionsToEnm(MeterRegistry meterRegistry) {
            this.openSessions = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, OPEN_SSH_TO_ENM_METRIC_TAG)
                    .register(meterRegistry);
            this.closeSessions = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, CLOSE_SSH_TO_ENM_METRIC_TAG)
                    .register(meterRegistry);
        }

        public void increment() {
            openSessions.increment();
        }

        public void decrement() {
            closeSessions.increment();
        }
    }

    public static final class AddNodeOperations {
        private final Counter startAddNode;
        private final Counter finishAddNode;

        public AddNodeOperations(MeterRegistry meterRegistry) {
            this.startAddNode = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, STARTED_ADD_NODE_METRIC_TAG)
                    .register(meterRegistry);
            this.finishAddNode = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, COMPLETED_ADD_NODE_METRIC_TAG)
                    .register(meterRegistry);
        }

        public void increment() {
            startAddNode.increment();
        }

        public void decrement() {
            finishAddNode.increment();

        }
    }

    public static final class InstantiateAddNodeToOSS {
        private final Counter instantiates;

        public InstantiateAddNodeToOSS(MeterRegistry meterRegistry) {
            this.instantiates = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, INSTANTIATE_ADD_NODE_METRIC_TAG)
                    .register(meterRegistry);
        }

        public void increment() {
            instantiates.increment();
        }
    }

    public static final class InstantiateCMPv2Enrollment {
        private final Counter instantiates;

        public InstantiateCMPv2Enrollment(MeterRegistry meterRegistry) {
            this.instantiates = Counter.builder(CONNECTIONS_TO_ENM_METRIC_TAGS)
                    .description(CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION)
                    .tag(ENM_METRIC_NAME, INSTANTIATE_ENROLLMENT_METRIC_TAG)
                    .register(meterRegistry);
        }

        public void increment() {
            instantiates.increment();
        }
    }
}

