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

import com.ericsson.vnfm.orchestrator.model.ClusterConfigData;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CismClusterConfigMapper {

    @Mapping(source = "content", target = "clusterData")
    ClusterConfigData toInternalModel(ClusterConfigFile configFile);

    @AfterMapping
    default void toInternalModel(ClusterConfigFile clusterConfigFile, @MappingTarget ClusterConfigData clusterConfigData) {
        clusterConfigData.setIsDefault(clusterConfigFile.isDefault());
    }

}
