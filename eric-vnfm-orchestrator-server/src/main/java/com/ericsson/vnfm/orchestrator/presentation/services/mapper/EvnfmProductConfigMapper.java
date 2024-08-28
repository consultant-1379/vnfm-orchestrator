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
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.CommonProductsInfoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EvnfmProductInfoConfig;
import com.ericsson.vnfm.orchestrator.model.EvnfmProductConfiguration;
import com.google.common.base.Strings;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvnfmProductConfigMapper {

    EvnfmProductConfiguration toInternalModel(EvnfmProductInfoConfig productInfoConfig);

    EvnfmProductConfiguration toInternalModel(CommonProductsInfoConfig commonProductsInfoConfig);

    @AfterMapping
    default void toInternalModel(EvnfmProductInfoConfig evnfmProductInfoConfig, @MappingTarget EvnfmProductConfiguration evnfmProductConfiguration) {
        evnfmProductConfiguration.setVersion(Strings.emptyToNull(evnfmProductInfoConfig.getVersion()));
    }

}
