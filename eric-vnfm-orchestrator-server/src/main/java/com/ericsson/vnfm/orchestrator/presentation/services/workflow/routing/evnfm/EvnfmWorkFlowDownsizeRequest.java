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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor (access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
final class EvnfmWorkFlowDownsizeRequest {
    private String clusterName;
    private String namespace;
    private String releaseName;
    private String applicationTimeOut;
    private String lifecycleOperationId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class EvnfmWorkFlowDownsizeBuilder {
        private String clusterName;
        private String lifecycleOperationId;
        private String namespace;
        private String releaseName;
        private String applicationTimeOut;

        EvnfmWorkFlowDownsizeBuilder() {
        }

        public EvnfmWorkFlowDownsizeRequest build() {
            final EvnfmWorkFlowDownsizeRequest requestBody = new EvnfmWorkFlowDownsizeRequest();
            requestBody.clusterName = clusterName;
            requestBody.lifecycleOperationId = lifecycleOperationId;
            requestBody.applicationTimeOut = applicationTimeOut;
            requestBody.namespace = namespace;
            requestBody.releaseName = releaseName;
            return requestBody;
        }

        EvnfmWorkFlowDownsizeBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        EvnfmWorkFlowDownsizeBuilder withLifecycleOperationId(String lifecycleOperationId) {
            this.lifecycleOperationId = lifecycleOperationId;
            return this;
        }

        EvnfmWorkFlowDownsizeBuilder withReleaseName(String releaseName) {
            this.releaseName = releaseName;
            return this;
        }

        EvnfmWorkFlowDownsizeBuilder inNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        EvnfmWorkFlowDownsizeBuilder withApplicationTimeout(String timeOut) {
            this.applicationTimeOut = timeOut;
            return this;
        }
    }
}
