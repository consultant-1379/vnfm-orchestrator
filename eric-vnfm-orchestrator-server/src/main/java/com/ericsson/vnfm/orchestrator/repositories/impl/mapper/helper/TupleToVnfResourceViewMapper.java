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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper;

import java.util.ArrayList;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Root;

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;

public final class TupleToVnfResourceViewMapper {

    private TupleToVnfResourceViewMapper() {

    }

    public static VnfResourceView map(Tuple tuple, Root<VnfResourceView> root) {
        return VnfResourceView.builder()
                .vnfInstanceId(tuple.get(root.get("vnfInstanceId")))
                .vnfInstanceName(tuple.get(root.get("vnfInstanceName")))
                .vnfInstanceDescription(tuple.get(root.get("vnfInstanceDescription")))
                .vnfDescriptorId(tuple.get(root.get("vnfDescriptorId")))
                .vnfProviderName(tuple.get(root.get("vnfProviderName")))
                .vnfProductName(tuple.get(root.get("vnfProductName")))
                .vnfSoftwareVersion(tuple.get(root.get("vnfSoftwareVersion")))
                .vnfdVersion(tuple.get(root.get("vnfdVersion")))
                .vnfPackageId(tuple.get(root.get("vnfPackageId")))
                .instantiationState(tuple.get(root.get("instantiationState")))
                .clusterName(tuple.get(root.get("clusterName")))
                .namespace(tuple.get(root.get("namespace")))
                .addedToOss(tuple.get(root.get("addedToOss")))
                .policies(tuple.get(root.get("policies")))
                .resourceDetails(tuple.get(root.get("resourceDetails")))
                .manoControlledScaling(tuple.get(root.get("manoControlledScaling")))
                .overrideGlobalRegistry(tuple.get(root.get("overrideGlobalRegistry")))
                .metadata(tuple.get(root.get("metadata")))
                .alarmSupervisionStatus(tuple.get(root.get("alarmSupervisionStatus")))
                .cleanUpResources(tuple.get(root.get("cleanUpResources")))
                .broEndpointUrl(tuple.get(root.get("broEndpointUrl")))
                .vnfInfoModifiableAttributesExtensions(tuple.get(root.get("vnfInfoModifiableAttributesExtensions")))
                .instantiationLevel(tuple.get(root.get("instantiationLevel")))
                .crdNamespace(tuple.get(root.get("crdNamespace")))
                .healSupported(tuple.get(root.get("isHealSupported")))
                .allOperations(new ArrayList<>())
                .scaleInfoEntity(new ArrayList<>())
                .build();
    }
}
