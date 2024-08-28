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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.OVERRIDE_GLOBAL_REGISTRY;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EvnfmWorkflowScaleRequest {

    private Map<String, Object> additionalParams;
    private String applicationTimeOut;
    private String clusterName;
    private String chartUrl;
    private Map<String, Map<String, Integer>> scaleResources;
    private boolean overrideGlobalRegistry;
    private String lifecycleOperationId;
    private String state;
    @Setter(AccessLevel.NONE)
    private String namespace;
    private String helmClientVersion;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class EvnfmWorkFlowScaleBuilder {
        private Map<String, Object> additionalParams;
        private String clusterName;
        private Map<String, Map<String, Integer>> scaleResources;
        private String helmChartUrl;
        private String lifecycleOperationId;
        private String state;
        private String namespace;
        private boolean overrideGlobalRegistry;
        private String helmClientVersion;

        EvnfmWorkFlowScaleBuilder(final Map<String, Object> additionalParams, String helmChartUrl) {
            this.additionalParams = additionalParams;
            this.helmChartUrl = helmChartUrl;
        }

        public EvnfmWorkflowScaleRequest build() {
            final EvnfmWorkflowScaleRequest requestBody = new EvnfmWorkflowScaleRequest();
            requestBody.lifecycleOperationId = this.lifecycleOperationId;
            requestBody.state = this.state;
            requestBody.namespace = namespace;
            requestBody.helmClientVersion = helmClientVersion;
            requestBody.overrideGlobalRegistry = this.overrideGlobalRegistry;
            setAllRequestParams(requestBody);
            return requestBody;
        }

        public EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowScaleBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public EvnfmWorkFlowScaleBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowScaleBuilder withOverrideGlobalRegistry(boolean overrideGlobalRegistry) {
            this.overrideGlobalRegistry = overrideGlobalRegistry;
            return this;
        }

        public EvnfmWorkFlowScaleBuilder withLifecycleOperationState(String lifeCycleOperationState) {
            this.state = lifeCycleOperationState;
            return this;
        }

        public EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder withScaleResources(
                Map<String, Map<String, Integer>> scaleResources) {
            this.scaleResources = scaleResources;
            return this;
        }

        public EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder withHelmClientVersion(
                String helmClientVersion) {
            this.helmClientVersion = helmClientVersion;
            return this;
        }

        private void setAllRequestParams(EvnfmWorkflowScaleRequest requestBody) {
            if (this.scaleResources == null) {
                throw new IllegalArgumentException("Mandatory input Scale Resources missing from input");
            } else {
                requestBody.scaleResources = scaleResources;
            }
            requestBody.clusterName = this.clusterName;

            if (this.additionalParams.containsKey(APPLICATION_TIME_OUT)) {
                requestBody.applicationTimeOut = String.valueOf(this.additionalParams.remove(APPLICATION_TIME_OUT));
            }
            if (this.additionalParams.containsKey(OVERRIDE_GLOBAL_REGISTRY)) {
                requestBody.overrideGlobalRegistry = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(OVERRIDE_GLOBAL_REGISTRY));
            }

            EVNFM_PARAMS.forEach(this.additionalParams::remove);

            requestBody.additionalParams = this.additionalParams;
            requestBody.chartUrl = helmChartUrl;
        }
    }
}
