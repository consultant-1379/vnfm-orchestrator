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
package com.ericsson.vnfm.orchestrator.presentation.services.license;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_CLUSTERS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CLUSTER_MANAGEMENT_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.LCM_OPERATIONS_LICENSE_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

@Component
public class OrchestratorLimitsCalculator {

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private ClusterConfigFileRepository clusterConfigFileRepository;

    public void checkOrchestratorLimitsForInstances(Object attribute) {
        if (attribute != null) {
            boolean isOperationWithoutLicense = (boolean) attribute;
            if (isOperationWithoutLicense && (int) vnfInstanceRepository.count() >= ALLOWED_NUMBER_OF_INSTANCES) {
                throw new MissingLicensePermissionException(
                        String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, LCM_OPERATIONS_LICENSE_TYPE));
            }
        }
    }

    public void checkOrchestratorLimitsForClusters(Object attribute) {
        if (attribute != null) {
            boolean isOperationWithoutLicense = (boolean) attribute;
            if (isOperationWithoutLicense && (int) clusterConfigFileRepository.count() >= ALLOWED_NUMBER_OF_CLUSTERS) {
                throw new MissingLicensePermissionException(
                        String.format(ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE, CLUSTER_MANAGEMENT_LICENSE_TYPE));
            }
        }
    }
}
