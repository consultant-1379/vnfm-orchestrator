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

import static com.ericsson.vnfm.orchestrator.utils.Utility.getProblemDetails;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationView;
import com.google.common.base.Strings;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = LocalDateMapper.class)
public interface LifecycleOperationsMapper {

    @Mapping(target = "error", ignore = true)
    VnfResourceLifecycleOperation toInternalModel(LifecycleOperationView lifecycleOperationView);

    @AfterMapping
    default void toInternalModel(LifecycleOperationView lifecycleOperationView,
                                 @MappingTarget VnfResourceLifecycleOperation vnfResourceLifecycleOperation) {
        String error = lifecycleOperationView.getError();
        if (!Strings.isNullOrEmpty(error)) {
            vnfResourceLifecycleOperation.setError(getProblemDetails(error));
        }
        vnfResourceLifecycleOperation.setLifecycleOperationType(lifecycleOperationView.getLifecycleOperationType().name());
    }

}
