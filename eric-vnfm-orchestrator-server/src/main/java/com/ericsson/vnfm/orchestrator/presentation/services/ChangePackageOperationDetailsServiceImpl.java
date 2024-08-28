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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;

@Service
public class ChangePackageOperationDetailsServiceImpl implements ChangePackageOperationDetailsService {

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Override
    public List<ChangePackageOperationDetails> findAllByVnfInstances(final List<VnfInstance> vnfInstances) {
        final List<String> lifecycleOperationIds = vnfInstances.stream()
                .flatMap(vnfInstance -> vnfInstance.getAllOperations().stream())
                .map(LifecycleOperation::getOperationOccurrenceId)
                .collect(Collectors.toList());
        return changePackageOperationDetailsRepository.findAllById(lifecycleOperationIds);
    }

    @Override
    public List<ChangePackageOperationDetails> findAllByVnfResources(final List<VnfResource> vnfResources) {
        final List<String> lifecycleOperationIds = vnfResources.stream()
                .flatMap(vnfResource -> vnfResource.getLcmOperationDetails().stream())
                .map(VnfResourceLifecycleOperation::getOperationOccurrenceId)
                .collect(Collectors.toList());
        return changePackageOperationDetailsRepository.findAllById(lifecycleOperationIds);
    }

    @Override
    public List<ChangePackageOperationDetails> findAllByVnfInstance(final VnfInstance vnfInstance) {
        final List<VnfInstance> vnfInstances = List.of(vnfInstance);
        return findAllByVnfInstances(vnfInstances);
    }

    @Override
    public List<ChangePackageOperationDetails> findAllByVnfResourceViews(final List<VnfResourceView> vnfResourceViews) {
        final List<String> lifecycleOperationIds = vnfResourceViews.stream()
                .flatMap(vnfResourceView -> vnfResourceView.getAllOperations().stream())
                .map(LifecycleOperation::getOperationOccurrenceId)
                .collect(Collectors.toList());
        return changePackageOperationDetailsRepository.findAllById(lifecycleOperationIds);
    }
}
