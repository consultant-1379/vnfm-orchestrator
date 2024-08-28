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

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.utils.StringsConvertUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = LocalDateMapper.class)
public interface ResourceOperationsMapper {

    @Mapping(source = "vnfInstance.vnfInstanceName", target = "resourceInstanceName")
    @Mapping(source = "vnfInstance.vnfInstanceId", target = "resourceID")
    @Mapping(source = "lifecycleOperationType", target = "operation")
    @Mapping(source = "operationState", target = "event")
    @Mapping(source = "stateEnteredTime", target = "timestamp")
    @Mapping(target = "error", ignore = true)
    OperationDetails toInternalModel(LifecycleOperation lifecycleOperation);

    @AfterMapping
    default void toInternalModel(LifecycleOperation lifecycleOperation, @MappingTarget OperationDetails operationDetails) {
        String error = lifecycleOperation.getError();
        operationDetails.setError(StringsConvertUtils.mapIfNotEmpty(error, Utility::getProblemDetails));
    }


}
