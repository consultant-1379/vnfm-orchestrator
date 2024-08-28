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

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.getOperationParamsWithoutDay0Configuration;

import java.util.Map;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.utils.StringsConvertUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VnfLifecycleMapper {

    @Mapping(target = "error", ignore = true)
    VnfResourceLifecycleOperation toInternalModel(LifecycleOperation lifecycleOperation);

    @AfterMapping
    default void toInternalModel(LifecycleOperation lifecycleOperation, @MappingTarget VnfResourceLifecycleOperation vnfResourceLifecycleOperation) {
        String error = lifecycleOperation.getError();
        vnfResourceLifecycleOperation.setError(StringsConvertUtils.mapIfNotEmpty(error, Utility::getProblemDetails));
        Map<String, Object> operationParams = getOperationParamsWithoutDay0Configuration(lifecycleOperation.getOperationParams());
        vnfResourceLifecycleOperation.setOperationParams(convertObjToJsonString(operationParams));
    }

}