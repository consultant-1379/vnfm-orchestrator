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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import java.util.Comparator;
import java.util.Optional;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.ericsson.vnfm.orchestrator.utils.StringsConvertUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = VnfLifecycleMapper.class)
public abstract class VnfInstanceResourceResponseMapper {

    @Autowired
    private ObjectMapper mapper;

    @Mapping(source = "allOperations", target = "lcmOperationDetails")
    @Mapping(source = "vnfInstanceId", target = "instanceId")
    @Mapping(source = "vnfDescriptorId", target = "vnfdId")
    @Mapping(source = "vnfProviderName", target = "vnfProvider")
    @Mapping(source = "instantiationLevel", target = "instantiationLevelId")
    @Mapping(source = "vnfPackageId", target = "vnfPkgId")
    public abstract ResourceResponse toInternalModel(VnfInstance vnfInstance);

    @AfterMapping
    protected void toInternalModel(VnfInstance vnfInstance, @MappingTarget ResourceResponse vnfResource) {
        vnfResource.setInstantiateOssTopology(
                StringsConvertUtils.mapIfNotEmpty(vnfInstance.getInstantiateOssTopology(), Utility::convertStringToJSONObj));
        vnfResource.setInstantiatedVnfInfo(ScalingUtils.mapInstantiatedVnfInfo(vnfInstance.getScaleInfoEntity()));
        vnfResource.setScalingInfo(
                StringsConvertUtils.mapIfNotEmpty(vnfInstance.getPolicies(), s -> ScalingUtils.getScalingDetails(vnfInstance.getPolicies(), mapper)));
        vnfResource.setExtensions(
                StringsConvertUtils.mapIfNotEmpty(vnfInstance.getVnfInfoModifiableAttributesExtensions(), Utility::convertStringToJSONObj));
        Optional.ofNullable(vnfInstance.getAllOperations())
                .flatMap(ops -> ops.stream().max(Comparator.comparing(LifecycleOperation::getStateEnteredTime)))
                .map(LifecycleOperation::getOperationOccurrenceId)
                .flatMap(opId -> vnfResource.getLcmOperationDetails().stream()
                        .filter(opDto -> opId.equals(opDto.getOperationOccurrenceId())).findFirst())
                .ifPresent(opDto -> {
                    opDto.setCurrentLifecycleOperation(true);
                    vnfResource.setLastLifecycleOperation(opDto);
                    vnfResource.setLastStateChanged(opDto.getStateEnteredTime());
                });
        if (vnfResource.getLcmOperationDetails() != null && !vnfResource.getLcmOperationDetails().isEmpty()) {
            final String lastOperationId = vnfResource.getLastLifecycleOperation().getOperationOccurrenceId();
            vnfResource.getLcmOperationDetails()
                    .forEach(op -> op.setCurrentLifecycleOperation(op.getOperationOccurrenceId().equals(lastOperationId)));
        }
    }
}
