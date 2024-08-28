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
import java.util.Objects;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.model.granting.ResourceHandle;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceDefinition {

    @NotBlank
    private String id;
    @NotNull
    private ResourceDefinitionType type;
    private String vduId;
    private String vnfdId;
    @NotEmpty
    private List<String> resourceTemplateId;
    private String secondaryResourceTemplateId;
    private ResourceHandle resource;
    private SnapshotResourceDefinition snapshotResDef;

    public static ResourceDefinition of(final ResourceDefinition origin) {
        return new ResourceDefinition()
                .setId(UUID.randomUUID().toString())
                .setType(origin.getType())
                .setVduId(origin.getVduId())
                .setVnfdId(origin.getVnfdId())
                .setResourceTemplateId(origin.getResourceTemplateId())
                .setSecondaryResourceTemplateId(origin.getSecondaryResourceTemplateId())
                .setResource(origin.getResource() == null
                                     ? null
                                     : new ResourceHandle()
                        .setVimConnectionId(origin.getResource().getVimConnectionId())
                        .setResourceProviderId(origin.getResource().getResourceProviderId())
                        .setResourceId(origin.getResource().getResourceId())
                        .setVimLevelResourceType(origin.getResource().getVimLevelResourceType()))
                .setSnapshotResDef(origin.getSnapshotResDef() == null
                                           ? null
                                           : new SnapshotResourceDefinition()
                        .setVnfSnapshotId(origin.getSnapshotResDef().getVnfSnapshotId())
                        .setVnfcSnapshotId(origin.getSnapshotResDef().getVnfcSnapshotId())
                        .setStorageSnapshotId(origin.getSnapshotResDef().getStorageSnapshotId())
                        .setSnapshotResource(origin.getSnapshotResDef().getSnapshotResource()));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ResourceDefinition that = (ResourceDefinition) o;
        return type == that.type && Objects.equals(vduId, that.vduId) && Objects.equals(vnfdId, that.vnfdId)
                && Objects.equals(resourceTemplateId, that.resourceTemplateId) && Objects.equals(secondaryResourceTemplateId,
                                                                                                 that.secondaryResourceTemplateId)
                && Objects.equals(resource, that.resource) && Objects.equals(snapshotResDef, that.snapshotResDef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, vduId, vnfdId, resourceTemplateId, secondaryResourceTemplateId, resource, snapshotResDef);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
