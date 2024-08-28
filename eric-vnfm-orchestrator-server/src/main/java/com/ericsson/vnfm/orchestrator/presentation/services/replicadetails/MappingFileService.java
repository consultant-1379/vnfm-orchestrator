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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import java.util.Map;
import java.util.Optional;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Service for getting a mappingFile from Onboarding service.
 */
public interface MappingFileService {

    /**
     * Fetching a mapping file from Onboarding service.
     *
     * @param vnfInstance - created vnfInstance.
     * @param scaleMappingFilePath - path to scaling mapping file from descriptor model.
     * @return - map of scaleMapping files.
     */
    Map<String, ScaleMapping> getMappingFile(String scaleMappingFilePath, VnfInstance vnfInstance);

    /**
     * Fetching a mapping file from vnfd.
     *
     * @param descriptorModel - information about vnfInstance we are creating.
     * @return - map of scaleMapping files.
     */
    Optional<String> getScaleMapFilePathFromDescriptorModel(String descriptorModel);

    boolean isMappingFilePresent(String packageId);
}
