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

import java.util.List;

import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;

public interface ChangePackageOperationDetailsService {
    List<ChangePackageOperationDetails> findAllByVnfInstances(List<VnfInstance> vnfInstances);

    List<ChangePackageOperationDetails> findAllByVnfResources(List<VnfResource> vnfInstances);

    List<ChangePackageOperationDetails> findAllByVnfInstance(VnfInstance vnfInstance);

    List<ChangePackageOperationDetails> findAllByVnfResourceViews(List<VnfResourceView> vnfResourceViews);
}
