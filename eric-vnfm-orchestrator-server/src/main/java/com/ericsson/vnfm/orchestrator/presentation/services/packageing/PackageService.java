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
package com.ericsson.vnfm.orchestrator.presentation.services.packageing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;

public interface PackageService {

    /**
     * Retrieve the VNF descriptor model for the supplied VNF package Id
     *
     * @param vnfPkgId the id of the vnf package
     * @return The VNF descriptor in json format
     */
    JSONObject getVnfd(String vnfPkgId);

    /**
     * Retrieve the scale mapping file for the supplied VNF package Id
     *
     * @param vnfPkgId             the id of the vnf package
     * @param scaleMappingFilePath the path of the scale mapping file
     * @return The ScaleMapping map
     */
    Map<String, ScaleMapping> getScalingMapping(String vnfPkgId, String scaleMappingFilePath);

    /**
     * Retrieve the PackageInfo for the supplied VNFD Id
     *
     * @param vnfdId the id of the vnf package
     * @return The PackageResponse
     */
    PackageResponse getPackageInfo(String vnfdId);

    /**
     * Retrieve the PackageInfo with DescriptorModel for the supplied VNFD Id
     *
     * @param vnfdId the id of the vnf package
     * @return The PackageResponse
     */
    PackageResponse getPackageInfoWithDescriptorModel(String vnfdId);

    /**
     * Retrieve the content of an artifact within a VNF package
     *
     * @param vnfPkgId     the id of the vnf package
     * @param artifactPath the path of the artifact
     */
    Optional<String> getPackageArtifacts(String vnfPkgId, String artifactPath);

    /**
     * This method responsible for retrieving all OperationDetail
     * from am-shared-utils based on vnfd in fullstack or from
     * local onboarding in smallstack
     *
     * @param vnfPackageId
     * @return List of OperationDetail
     */
    List<OperationDetail> getSupportedOperations(String vnfPackageId);

    /**
     * This method responsible for updating package usage state
     * @param vnfPkgId
     * @param vnfInstanceId
     * @param isInUse
     */
    void updateUsageState(String vnfPkgId, String vnfInstanceId, boolean isInUse);
}
