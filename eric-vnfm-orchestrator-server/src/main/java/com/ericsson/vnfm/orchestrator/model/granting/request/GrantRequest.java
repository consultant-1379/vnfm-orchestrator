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
package com.ericsson.vnfm.orchestrator.model.granting.request;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.granting.StructureLinks;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrantRequest {

    @NotBlank
    private String vnfInstanceId;
    @NotBlank
    private String vnfLcmOpOccId;
    @NotBlank
    private String vnfdId;
    private String dstVnfdId;
    private String flavourId;
    @NotNull
    private GrantedLcmOperationType operation;
    @JsonProperty(value = "isAutomaticInvocation")
    private boolean isAutomaticInvocation;
    private String instantiationLevelId;
    private List<ScaleInfo> targetScaleLevelInfo;
    private List<ResourceDefinition> addResources;
    private List<ResourceDefinition> tempResources;
    private List<ResourceDefinition> removeResources;
    private List<ResourceDefinition> updateResources;
    private List<PlacementConstraint> placementConstraints;
    private List<VimConstraint> vimConstraints;
    private Map<String, Object> additionalParams;
    @JsonProperty("_links")
    @NotNull
    private StructureLinks links;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
