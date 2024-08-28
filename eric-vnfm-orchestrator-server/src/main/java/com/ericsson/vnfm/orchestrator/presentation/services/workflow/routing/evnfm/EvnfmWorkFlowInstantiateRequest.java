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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.OVERRIDE_GLOBAL_REGISTRY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EvnfmWorkFlowInstantiateRequest {
    private String namespace;
    private String releaseName;
    private String chartUrl;
    @Setter
    private Map<String, Object> additionalParams;
    @Setter
    private Object day0Configuration;
    private String applicationTimeOut;
    private String clusterName;
    private boolean skipVerification;
    private boolean overrideGlobalRegistry;
    @Setter
    private String lifecycleOperationId;
    @Setter
    private String state;
    private boolean skipJobVerification;
    private String chartVersion;
    private HelmChartType chartType;

    private String helmClientVersion;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    static class EvnfmWorkFlowInstantiateBuilder {
        private String chartUrl;
        private Map<String, Object> additionalParams;
        private Map<String, Object> day0Configuration;
        private String clusterName;
        private String lifecycleOperationId;
        private String state;
        private boolean overrideGlobalRegistry;
        private String chartVersion;
        private HelmChartType chartType;
        private String namespace;
        private String helmClientVersion;

        EvnfmWorkFlowInstantiateBuilder(String chartUrl, final Map<String, Object> additionalParams) {
            this.chartUrl = chartUrl;
            this.additionalParams = additionalParams;
        }

        public EvnfmWorkFlowInstantiateRequest build() {
            final EvnfmWorkFlowInstantiateRequest requestBody = new EvnfmWorkFlowInstantiateRequest();
            requestBody.chartUrl = this.chartUrl;
            requestBody.lifecycleOperationId = this.lifecycleOperationId;
            requestBody.state = this.state;
            requestBody.overrideGlobalRegistry = this.overrideGlobalRegistry;
            requestBody.chartType = this.chartType;
            requestBody.chartVersion = this.chartVersion;
            requestBody.helmClientVersion = this.helmClientVersion;

            setAllRequestParams(requestBody);
            return requestBody;
        }

        public EvnfmWorkFlowInstantiateBuilder withChartVersion(String chartVersion) {
            if (this.chartType == HelmChartType.CRD) {
                this.chartVersion = chartVersion;
            }
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withChartType(HelmChartType chartType) {
            this.chartType = chartType;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withLifecycleOperationState(String lifeCycleOperationState) {
            this.state = lifeCycleOperationState;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withOverrideGlobalRegistry(boolean overrideGlobalRegistry) {
            this.overrideGlobalRegistry = overrideGlobalRegistry;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withDay0Configuration(Map<String, Object> day0Configuration) {
            this.day0Configuration = day0Configuration;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder withHelmClientVersion(String helmClientVersion) {
            this.helmClientVersion = helmClientVersion;
            return this;
        }

        public EvnfmWorkFlowInstantiateBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        private void setAllRequestParams(EvnfmWorkFlowInstantiateRequest requestBody) {
            requestBody.clusterName = this.clusterName;

            if (this.additionalParams.containsKey(SKIP_VERIFICATION)) {
                requestBody.skipVerification = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(SKIP_VERIFICATION));
            }
            if (this.additionalParams.containsKey(OVERRIDE_GLOBAL_REGISTRY)) {
                requestBody.overrideGlobalRegistry = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(OVERRIDE_GLOBAL_REGISTRY));
            }
            this.additionalParams.keySet()
                    .removeIf(key -> key.startsWith("ossTopology") || key.equals(ADD_NODE_TO_OSS));
            if (this.additionalParams.containsKey(SKIP_JOB_VERIFICATION)) {
                requestBody.skipJobVerification = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(SKIP_JOB_VERIFICATION));
            }

            setAllTimeOutRequestParams(requestBody);

            EVNFM_PARAMS.stream()
                            .filter(param -> !NAMESPACE.equals(param))
                                    .forEach(this.additionalParams::remove);

            if ((this.chartType == HelmChartType.CNF)) {
                if (this.additionalParams.containsKey(NAMESPACE)) {
                    requestBody.namespace = String.valueOf(this.additionalParams.remove(NAMESPACE));
                } else {
                    requestBody.namespace = this.namespace;
                }
            } else if (this.chartType == HelmChartType.CRD) {
                requestBody.namespace = this.namespace;
            }

            requestBody.additionalParams = this.additionalParams;
            requestBody.day0Configuration = this.day0Configuration;
        }

        private void setAllTimeOutRequestParams(EvnfmWorkFlowInstantiateRequest requestBody) {
            if (this.additionalParams.containsKey(APPLICATION_TIME_OUT)) {
                requestBody.applicationTimeOut = String.valueOf(this.additionalParams.remove(APPLICATION_TIME_OUT));
            }
        }
    }
}
