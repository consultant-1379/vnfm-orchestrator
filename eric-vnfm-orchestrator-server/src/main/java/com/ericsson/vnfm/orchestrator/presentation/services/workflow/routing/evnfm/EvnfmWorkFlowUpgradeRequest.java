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
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
public final class EvnfmWorkFlowUpgradeRequest {
    private String chartUrl;
    @Setter
    private Map<String, Object> additionalParams;
    private String applicationTimeOut;
    private String clusterName;
    private boolean skipVerification;
    private boolean overrideGlobalRegistry;
    @Setter
    private String lifecycleOperationId;
    @Setter
    private String state;
    private boolean skipJobVerification;
    private String namespace;
    @Setter
    private HelmChartType chartType;
    @Setter
    private String chartVersion;
    private String helmClientVersion;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class EvnfmWorkFlowUpgradeBuilder {
        private String chartUrl;
        private Map<String, Object> additionalParams;
        private String clusterName;
        private String lifecycleOperationId;
        private String state;
        private String namespace;
        private boolean overrideGlobalRegistry;
        private HelmChartType chartType;
        private String chartVersion;
        private String helmClientVersion;

        EvnfmWorkFlowUpgradeBuilder(String chartUrl, final Map<String, Object> additionalParams) {
            this.chartUrl = chartUrl;
            this.additionalParams = additionalParams;
        }

        public EvnfmWorkFlowUpgradeRequest build() {
            final EvnfmWorkFlowUpgradeRequest requestBody = new EvnfmWorkFlowUpgradeRequest();
            requestBody.chartUrl = this.chartUrl;
            requestBody.namespace = namespace;
            requestBody.lifecycleOperationId = this.lifecycleOperationId;
            requestBody.state = this.state;
            requestBody.overrideGlobalRegistry = this.overrideGlobalRegistry;
            setAllRequestParams(requestBody);
            requestBody.chartType = this.chartType;
            requestBody.chartVersion = this.chartVersion;
            requestBody.helmClientVersion = this.helmClientVersion;
            return requestBody;
        }

        public EvnfmWorkFlowUpgradeBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withLifecycleOperationState(String lifeCycleOperationState) {
            this.state = lifeCycleOperationState;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withOverrideGlobalRegistry(boolean overrideGlobalRegistry) {
            this.overrideGlobalRegistry = overrideGlobalRegistry;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withChartType(HelmChartType chartType) {
            this.chartType = chartType;
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withChartVersion(String chartVersion) {
            if (this.chartType == HelmChartType.CRD) {
                this.chartVersion = chartVersion;
            }
            return this;
        }

        public EvnfmWorkFlowUpgradeBuilder withHelmClientVersion(String helmClientVersion) {
            this.helmClientVersion = helmClientVersion;
            return this;
        }


        private void setAllRequestParams(EvnfmWorkFlowUpgradeRequest requestBody) {
            requestBody.clusterName = this.clusterName;

            if (this.additionalParams.containsKey(SKIP_VERIFICATION)) {
                requestBody.skipVerification = BooleanUtils
                    .getBooleanValue(this.additionalParams.remove(SKIP_VERIFICATION));
            }
            if (this.additionalParams.containsKey(APPLICATION_TIME_OUT)) {
                requestBody.applicationTimeOut = String.valueOf(this.additionalParams.remove(APPLICATION_TIME_OUT));
            }
            if (this.additionalParams.containsKey(OVERRIDE_GLOBAL_REGISTRY)) {
                requestBody.overrideGlobalRegistry = BooleanUtils
                    .getBooleanValue(this.additionalParams.remove(OVERRIDE_GLOBAL_REGISTRY));
            }
            this.additionalParams.keySet().removeIf(key -> key.startsWith("ossTopology"));
            if (this.additionalParams.containsKey(SKIP_JOB_VERIFICATION)) {
                requestBody.skipJobVerification = BooleanUtils
                    .getBooleanValue(this.additionalParams.remove(SKIP_JOB_VERIFICATION));
            }

            EVNFM_PARAMS.forEach(this.additionalParams::remove);

            requestBody.additionalParams = this.additionalParams;
        }
    }
}
