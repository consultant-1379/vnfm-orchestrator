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

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;

@Service
public class MappingFileServiceImpl implements MappingFileService {

    @Autowired
    private PackageService packageService;

    @Override
    public Map<String, ScaleMapping> getMappingFile(final String scaleMappingFilePath, final VnfInstance vnfInstance) {
        return packageService.getScalingMapping(vnfInstance.getVnfPackageId(), scaleMappingFilePath);
    }

    @Override
    public Optional<String> getScaleMapFilePathFromDescriptorModel(final String descriptorModel) {
        return Optional.ofNullable(VnfdUtility.getScalingMappingFileArtifactPathFromVNFD(new JSONObject(descriptorModel)));
    }

    @Override
    public boolean isMappingFilePresent(String packageId) {
        String descriptorModel = String.valueOf(packageService.getVnfd(packageId));
        return getScaleMapFilePathFromDescriptorModel(descriptorModel).isPresent();
    }
}
