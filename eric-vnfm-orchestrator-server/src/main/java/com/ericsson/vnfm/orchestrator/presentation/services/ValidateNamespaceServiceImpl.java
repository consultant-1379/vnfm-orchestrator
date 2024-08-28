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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.INSTANTIATE_OPERATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidateNamespaceServiceImpl implements ValidateNamespaceService {

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Override
    public void validateNamespace(String namespace, String clusterName) {
        lifeCycleManagementHelper.verifyNamespaceForKubeNamespaces(INSTANTIATE_OPERATION, namespace);
        lifeCycleManagementHelper.verifyNamespaceForCrdNamespacesByClusterName(namespace, clusterName);
    }
}
