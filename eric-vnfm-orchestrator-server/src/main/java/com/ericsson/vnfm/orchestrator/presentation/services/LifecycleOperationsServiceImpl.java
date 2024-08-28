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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationsViewQuery;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationView;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.LifecycleOperationsMapper;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationViewRepository;

@Service
public class LifecycleOperationsServiceImpl implements LifecycleOperationsService {

    @Autowired
    private LifecycleOperationViewRepository lifecycleOperationViewRepository;

    @Autowired
    private LifecycleOperationsViewQuery lifecycleOperationsViewQuery;

    @Autowired
    private LifecycleOperationsMapper lifecycleOperationsMapper;

    @Override
    public Page<VnfResourceLifecycleOperation> getLifecycleOperationsPage(final String filters, final Pageable pageable) {
        Page<LifecycleOperationView> lifecycleOperationsViewPage = StringUtils.isEmpty(filters)
                ? lifecycleOperationViewRepository.findAll(pageable)
                : lifecycleOperationsViewQuery.getPageWithFilter(filters, pageable);

        return lifecycleOperationsViewPage.map(lifecycleOperationView -> lifecycleOperationsMapper.toInternalModel(lifecycleOperationView));
    }
}
