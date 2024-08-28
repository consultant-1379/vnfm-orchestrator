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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public final class TupleToVnfInstanceMapper {
    private TupleToVnfInstanceMapper() {

    }

    public static <T> VnfInstance map(Tuple tuple, Join<T, VnfInstance> join) {
        return VnfInstance.builder()
                .vnfInstanceId(tuple.get(join.get("vnfInstanceId")))
                .vnfInstanceName(tuple.get(join.get("vnfInstanceName")))
                .vnfInstanceDescription(tuple.get(join.get("vnfInstanceDescription")))
                .vnfDescriptorId(tuple.get(join.get("vnfDescriptorId")))
                .vnfProviderName(tuple.get(join.get("vnfProviderName")))
                .vnfProductName(tuple.get(join.get("vnfProductName")))
                .vnfSoftwareVersion(tuple.get(join.get("vnfSoftwareVersion")))
                .vnfdVersion(tuple.get(join.get("vnfdVersion")))
                .vnfPackageId(tuple.get(join.get("vnfPackageId")))
                .instantiationState(tuple.get(join.get("instantiationState")))
                .clusterName(tuple.get(join.get("clusterName")))
                .namespace(tuple.get(join.get("namespace")))
                .operationOccurrenceId(tuple.get(join.get("operationOccurrenceId")))
                .supportedOperations(tuple.get(join.get("supportedOperations")))
                .addedToOss(tuple.get(join.get("addedToOss")))
                .combinedAdditionalParams(tuple.get(join.get("combinedAdditionalParams")))
                .policies(tuple.get(join.get("policies")))
                .resourceDetails(tuple.get(join.get("resourceDetails")))
                .manoControlledScaling(tuple.get(join.get("manoControlledScaling")))
                .overrideGlobalRegistry(tuple.get(join.get("overrideGlobalRegistry")))
                .metadata(tuple.get(join.get("metadata")))
                .alarmSupervisionStatus(tuple.get(join.get("alarmSupervisionStatus")))
                .cleanUpResources(tuple.get(join.get("cleanUpResources")))
                .isHealSupported(tuple.get(join.get("isHealSupported")))
                .broEndpointUrl(tuple.get(join.get("broEndpointUrl")))
                .vnfInfoModifiableAttributesExtensions(tuple.get(join.get("vnfInfoModifiableAttributesExtensions")))
                .instantiationLevel(tuple.get(join.get("instantiationLevel")))
                .crdNamespace(tuple.get(join.get("crdNamespace")))
                .isRel4(tuple.get(join.get("isRel4")))
                .helmClientVersion(tuple.get(join.get("helmClientVersion")))
                .build();
    }

    public static VnfInstance map(Tuple tuple, Root<VnfInstance> root) {
        return VnfInstance.builder()
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
                .operationOccurrenceId(tuple.get(root.get("operationOccurrenceId")))
                .supportedOperations(tuple.get(root.get("supportedOperations")))
                .addedToOss(tuple.get(root.get("addedToOss")))
                .combinedAdditionalParams(tuple.get(root.get("combinedAdditionalParams")))
                .policies(tuple.get(root.get("policies")))
                .resourceDetails(tuple.get(root.get("resourceDetails")))
                .manoControlledScaling(tuple.get(root.get("manoControlledScaling")))
                .overrideGlobalRegistry(tuple.get(root.get("overrideGlobalRegistry")))
                .metadata(tuple.get(root.get("metadata")))
                .alarmSupervisionStatus(tuple.get(root.get("alarmSupervisionStatus")))
                .cleanUpResources(tuple.get(root.get("cleanUpResources")))
                .isHealSupported(tuple.get(root.get("isHealSupported")))
                .broEndpointUrl(tuple.get(root.get("broEndpointUrl")))
                .vnfInfoModifiableAttributesExtensions(tuple.get(root.get("vnfInfoModifiableAttributesExtensions")))
                .instantiationLevel(tuple.get(root.get("instantiationLevel")))
                .crdNamespace(tuple.get(root.get("crdNamespace")))
                .isRel4(tuple.get(root.get("isRel4")))
                .helmClientVersion(tuple.get(root.get("helmClientVersion")))
                .helmCharts(new ArrayList<>())
                .terminatedHelmCharts(new ArrayList<>())
                .scaleInfoEntity(new ArrayList<>())
                .allOperations(new ArrayList<>())
                .build();
    }
}
