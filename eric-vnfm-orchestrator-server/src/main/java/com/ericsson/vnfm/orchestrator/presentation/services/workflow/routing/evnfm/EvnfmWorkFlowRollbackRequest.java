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
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_JOB_VERIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor (access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EvnfmWorkFlowRollbackRequest {
    private String revisionNumber;
    private String applicationTimeOut;
    private String clusterName;
    private boolean skipVerification;
    private String lifecycleOperationId;
    private String state;
    private boolean skipJobVerification;
    private String namespace;
    private String helmClientVersion;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class EvnfmWorkFlowRollbackRequestBuilder {
        private Map<String, Object> additionalParams;
        private String revisionNumber;
        private String clusterName;
        private String lifecycleOperationId;
        private String state;
        private String namespace;
        private String helmClientVersion;

        EvnfmWorkFlowRollbackRequestBuilder(final Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
        }

        public EvnfmWorkFlowRollbackRequest build() {
            final EvnfmWorkFlowRollbackRequest evnfmRollbackRequest = new EvnfmWorkFlowRollbackRequest();
            evnfmRollbackRequest.revisionNumber = revisionNumber;
            evnfmRollbackRequest.clusterName = clusterName;
            evnfmRollbackRequest.lifecycleOperationId = lifecycleOperationId;
            evnfmRollbackRequest.state = state;
            evnfmRollbackRequest.namespace = namespace;
            evnfmRollbackRequest.helmClientVersion = helmClientVersion;
            setAllRequestParams(evnfmRollbackRequest);
            return evnfmRollbackRequest;
        }

        public EvnfmWorkFlowRollbackRequestBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public EvnfmWorkFlowRollbackRequestBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        public EvnfmWorkFlowRollbackRequestBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public EvnfmWorkFlowRollbackRequestBuilder withLifecycleOperationState(String lifeCycleOperationState) {
            this.state = lifeCycleOperationState;
            return this;
        }

        public EvnfmWorkFlowRollbackRequestBuilder withHelmClientVersion(String helmClientVersion) {
            this.helmClientVersion = helmClientVersion;
            return this;
        }

        public EvnfmWorkFlowRollbackRequestBuilder resetToRevision(String revisionNumber) {
            this.revisionNumber = revisionNumber;
            return this;
        }

        private void setAllRequestParams(final EvnfmWorkFlowRollbackRequest requestBody) {
            if (additionalParams == null || additionalParams.isEmpty()) {
                return;
            }
            if (this.additionalParams.containsKey(APPLICATION_TIME_OUT)) {
                requestBody.applicationTimeOut = String.valueOf(this.additionalParams.remove(APPLICATION_TIME_OUT));
            }
            if (this.additionalParams.containsKey(SKIP_VERIFICATION)) {
                requestBody.skipVerification = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(SKIP_VERIFICATION));
            }
            if (this.additionalParams.containsKey(SKIP_JOB_VERIFICATION)) {
                requestBody.skipJobVerification = BooleanUtils
                        .getBooleanValue(this.additionalParams.remove(SKIP_JOB_VERIFICATION));
            }
        }
    }
}
