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

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.ericsson.vnfm.orchestrator.utils.StringsConvertUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = VnfLifecycleMapper.class)
@Slf4j
public abstract class ResourceViewResponseMapper {

    @Autowired
    private ObjectMapper mapper;

    @Mapping(source = "allOperations", target = "lcmOperationDetails")
    @Mapping(source = "vnfInstanceId", target = "instanceId")
    @Mapping(source = "vnfDescriptorId", target = "vnfdId")
    @Mapping(source = "vnfProviderName", target = "vnfProvider")
    @Mapping(source = "instantiationLevel", target = "instantiationLevelId")
    @Mapping(source = "vnfPackageId", target = "vnfPkgId")
    public abstract ResourceResponse toInternalModel(VnfResourceView vnfResourceView);

    @AfterMapping
    protected void toInternalModel(VnfResourceView vnfResourceView, @MappingTarget ResourceResponse resourceResponse) {
        resourceResponse.setInstantiateOssTopology(
                StringsConvertUtils.mapIfNotEmpty(vnfResourceView.getInstantiateOssTopology(), Utility::convertStringToJSONObj));
        resourceResponse.setInstantiatedVnfInfo(ScalingUtils.mapInstantiatedVnfInfo(vnfResourceView.getScaleInfoEntity()));
        resourceResponse.setScalingInfo(
                StringsConvertUtils.mapIfNotEmpty(vnfResourceView.getPolicies(), s ->
                        ScalingUtils.getScalingDetails(vnfResourceView.getPolicies(), mapper)));
        resourceResponse.setExtensions(
                StringsConvertUtils.mapIfNotEmpty(vnfResourceView.getVnfInfoModifiableAttributesExtensions(), Utility::convertStringToJSONObj));
        if (vnfResourceView.getLastLifecycleOperation() != null) {
            String latestOperationId = vnfResourceView.getLastLifecycleOperation().getOperationOccurrenceId();
            String operationWithLatestStateTimeId = resourceResponse.getLcmOperationDetails().stream()
                    .max(Comparator.comparing(VnfResourceLifecycleOperation::getStateEnteredTime))
                    .map(VnfResourceLifecycleOperation::getOperationOccurrenceId)
                    .orElse(null);

            if (operationWithLatestStateTimeId != null && !operationWithLatestStateTimeId.equals(latestOperationId)) {
                LOGGER.info("Real latest operation (from DB) is not the same with latest in VNF instance ({}). Actual: {}, Expected: {}",
                            vnfResourceView.getVnfInstanceId(), latestOperationId, operationWithLatestStateTimeId);
                latestOperationId = operationWithLatestStateTimeId;
            }

            final String finalLatestOperationId = latestOperationId;
            resourceResponse.getLcmOperationDetails().stream()
                            .filter(op -> op.getOperationOccurrenceId().equals(finalLatestOperationId))
                            .findFirst().ifPresent(resourceResponse::setLastLifecycleOperation);
            resourceResponse.getLcmOperationDetails().forEach(op -> op.setCurrentLifecycleOperation(
                    op.getOperationOccurrenceId().equals(finalLatestOperationId)));
            resourceResponse.getLastLifecycleOperation().setCurrentLifecycleOperation(true);
        }
    }
}
