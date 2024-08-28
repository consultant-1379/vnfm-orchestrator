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
package com.ericsson.vnfm.orchestrator.model.license;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Permission {
    @JsonProperty("onboarding")
    ONBOARDING,
    @JsonProperty("lcm_operations")
    LCM_OPERATIONS,
    @JsonProperty("enm_integration")
    ENM_INTEGRATION,
    @JsonProperty("cluster_management")
    CLUSTER_MANAGEMENT
}
