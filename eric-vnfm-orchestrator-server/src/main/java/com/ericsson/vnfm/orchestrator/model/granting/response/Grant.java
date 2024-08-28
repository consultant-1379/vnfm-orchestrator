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
package com.ericsson.vnfm.orchestrator.model.granting.response;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.model.granting.ResponseStructureLinks;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Grant {

    @NotBlank
    private String id;
    @NotBlank
    private String vnfInstanceId;
    @NotBlank
    private String vnfLcmOpOccId;
    private List<VimConnectionInfo> vimConnectionInfo;
    private List<VimConnectionInfo> cirConnectionInfo;
    private List<VimConnectionInfo> mciopRepositoryInfo;
    private List<ZoneInfo> zones;
    private List<ZoneGroupInfo> zoneGroups;
    private List<GrantInfo> addResources;
    private List<GrantInfo> tempResources;
    private List<GrantInfo> removeResources;
    private List<GrantInfo> updateResources;
    private VimAssetInfo vimAssets;
    private List<ExtVirtualLinkData> extVirtualLinks;
    private List<ExtManagedVirtualLinkData> extManagedVirtualLinks;
    private Map<String, Object> additionalParams;
    @NotNull
    @JsonProperty("_links")
    private ResponseStructureLinks links;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
