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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_CLIENT_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LIFECYCLE_OPERATION_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.STATE;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EvnfmWorkFlowTerminateRequest {
    private Map<String, Object> additionalParams;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class EvnfmWorkFlowTerminateBuilder {

        private Map<String, Object> additionalParams;
        private String clusterName;
        private String lifecycleOperationId;
        private String state;
        private String namespace;
        private String helmClientVersion;

        EvnfmWorkFlowTerminateBuilder(final Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
        }

        public EvnfmWorkFlowTerminateRequest build() {
            final EvnfmWorkFlowTerminateRequest requestBody = new EvnfmWorkFlowTerminateRequest();
            if (additionalParams == null) {
                requestBody.additionalParams = new HashMap<>();
                setRequestParameters(requestBody.additionalParams);
            } else {
                setAllRequestParams(requestBody);
            }
            requestBody.additionalParams.put(NAMESPACE, namespace);
            if (StringUtils.isNotEmpty(helmClientVersion)) {
                requestBody.additionalParams.put(HELM_CLIENT_VERSION, helmClientVersion);
            }
            return requestBody;
        }

        public EvnfmWorkFlowTerminateBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowTerminateBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public EvnfmWorkFlowTerminateBuilder withState(String state) {
            this.state = state;
            return this;
        }

        public EvnfmWorkFlowTerminateBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowTerminateBuilder withHelmClientVersion(String helmClientVersion) {
            this.helmClientVersion = helmClientVersion;
            return this;
        }

        private void setAllRequestParams(EvnfmWorkFlowTerminateRequest requestBody) {
            HashMap<String, Object> map = new HashMap<>();
            setRequestParameters(map);
            if (additionalParams.containsKey(SKIP_VERIFICATION)) {
                map.put(SKIP_VERIFICATION, additionalParams.get(SKIP_VERIFICATION));
            }
            if (additionalParams.containsKey(SKIP_JOB_VERIFICATION)) {
                map.put(SKIP_JOB_VERIFICATION, additionalParams.get(SKIP_JOB_VERIFICATION));
            }

            setAllTimeOutRequestParams(map);
            requestBody.additionalParams = map;
        }

        private void setRequestParameters(Map<String, Object> map) {
            map.put(CLUSTER_NAME, this.clusterName);
            if (!Strings.isNullOrEmpty(this.state)) {
                map.put(STATE, this.state);
            }
            if (!Strings.isNullOrEmpty(this.lifecycleOperationId)) {
                map.put(LIFECYCLE_OPERATION_ID, this.lifecycleOperationId);
            }
        }

        private void setAllTimeOutRequestParams(final HashMap<String, Object> map) {
            if (additionalParams.containsKey(APPLICATION_TIME_OUT)) {
                map.put(APPLICATION_TIME_OUT, additionalParams.get(APPLICATION_TIME_OUT));
            }
        }
    }
}
