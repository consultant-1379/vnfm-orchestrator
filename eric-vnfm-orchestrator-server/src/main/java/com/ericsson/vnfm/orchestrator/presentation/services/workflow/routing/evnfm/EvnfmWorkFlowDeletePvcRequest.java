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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LABELS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LIFECYCLE_OPERATION_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.STATE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
final class EvnfmWorkFlowDeletePvcRequest {
    private Map<String, Object> additionalParams;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    static class EvnfmWorkFlowDeletePvcBuilder {
        private String clusterName;
        private String lifecycleOperationId;
        private String state;
        private String namespace;
        private final String applicationTimeOut;
        private String[] labels = new String[20];

        EvnfmWorkFlowDeletePvcBuilder(String applicationTimeOut) {
            this.applicationTimeOut = applicationTimeOut;
        }

        public EvnfmWorkFlowDeletePvcRequest build() {
            final EvnfmWorkFlowDeletePvcRequest requestBody = new EvnfmWorkFlowDeletePvcRequest();
            requestBody.setAdditionalParams(setRequestParameters());
            return requestBody;
        }

        public EvnfmWorkFlowDeletePvcBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowDeletePvcBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowDeletePvcBuilder withState(String lifeCycleOperationState) {
            this.state = lifeCycleOperationState;
            return this;
        }

        public EvnfmWorkFlowDeletePvcBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public EvnfmWorkFlowDeletePvcBuilder withLabels(String... labels) {
            this.labels = labels;
            return this;
        }

        private HashMap<String, Object> setRequestParameters() {
            HashMap<String, Object> map = new HashMap<>();
            map.put(CLUSTER_NAME, this.clusterName);
            map.put(APPLICATION_TIME_OUT, this.applicationTimeOut);
            if (!Strings.isNullOrEmpty(this.state)) {
                map.put(STATE, this.state);
            }
            if (!Strings.isNullOrEmpty(this.lifecycleOperationId)) {
                map.put(LIFECYCLE_OPERATION_ID, this.lifecycleOperationId);
            }
            if (!Strings.isNullOrEmpty(this.namespace)) {
                map.put(NAMESPACE, this.namespace);
            }
            if (this.labels.length != 0) {
                map.put(LABELS, Arrays.asList(this.labels));
            }
            return map;
        }
    }
}
