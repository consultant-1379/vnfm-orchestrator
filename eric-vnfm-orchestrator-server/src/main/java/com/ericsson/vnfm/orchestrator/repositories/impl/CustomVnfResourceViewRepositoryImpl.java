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
package com.ericsson.vnfm.orchestrator.repositories.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView_;
import com.ericsson.vnfm.orchestrator.repositories.CustomVnfResourceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.projections.VnfInstanceIdProjection;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.FullSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CustomVnfResourceViewRepositoryImpl implements CustomVnfResourceViewRepository<VnfResourceView> {

    @Autowired
    private PagingAndSpecificationVnfResourceViewRepository pagingAndSpecificationVnfResourceViewRepository;

    @Autowired
    private PartialSelectionQueryExecutor partialSelectionQueryExecutor;

    @Autowired
    private FullSelectionQueryExecutor fullSelectionQueryExecutor;

    @Override
    public Page<VnfResourceView> findAll(final Pageable pageable) {
        final Page<VnfInstanceIdProjection> pagedVnfInstanceIdProjections = pagingAndSpecificationVnfResourceViewRepository.findAllBy(pageable);
        final List<VnfInstanceIdProjection> vnfInstanceIdProjections = pagedVnfInstanceIdProjections.getContent();
        final List<String> ids = getVnfInstanceIdsFromProjections(vnfInstanceIdProjections);
        final Map<String, VnfResourceView> vnfResourceViews = findAllPartial(ids);
        return pagedVnfInstanceIdProjections.map(projection -> vnfResourceViews.get(projection.getVnfInstanceId()));
    }

    @Override
    public Map<String, Tuple> selectFields(final List<VnfResourceView> vnfResourceViews, final List<String> fields) {
        final List<String> ids = getVnfInstanceIdsFromEntities(vnfResourceViews);
        return partialSelectionQueryExecutor.selectEntityFields(VnfResourceView.class, VnfResourceView_.vnfInstanceId, ids, fields);
    }

    @Override
    public <A, M extends Attribute<VnfResourceView, ?>> void fetchAssociation(final List<VnfResourceView> vnfResourceViews,
                                                                              final Class<A> associationClass, final M associationName) {
        final Map<String, VnfResourceView> vnfResourceViewMap = vnfResourceViews.stream()
                .collect(Collectors.toMap(VnfResourceView::getVnfInstanceId, Function.identity()));
        final List<String> vnfResourceViewIds = vnfResourceViews.stream()
                .map(VnfResourceView::getVnfInstanceId)
                .collect(Collectors.toList());
        partialSelectionQueryExecutor.fetchAssociation(VnfResourceView.class, associationClass, associationName, VnfResourceView_.vnfInstanceId,
                                                       vnfResourceViewIds, vnfResourceViewMap);
    }

    @Override
    public Page<VnfResourceView> findAll(final Specification<VnfResourceView> specification, final Pageable pageable) {
        final Page<VnfResourceView> pagedVnfResourceViews = pagingAndSpecificationVnfResourceViewRepository.findAll(specification, pageable);
        List<VnfResourceView> vnfResourceViews = pagedVnfResourceViews.getContent();
        resetVnfResourceViewAssociations(vnfResourceViews);
        return pagedVnfResourceViews;
    }

    @Override
    public List<VnfResourceView> findAll(final Specification<VnfResourceView> specification) {
        List<VnfResourceView> vnfResourceViews = pagingAndSpecificationVnfResourceViewRepository.findAll(specification);
        resetVnfResourceViewAssociations(vnfResourceViews);
        return vnfResourceViews;
    }

    @Override
    public List<VnfResourceView> findAll(final Specification<VnfResourceView> specification, final Sort sort) {
        List<VnfResourceView> vnfResourceViews = pagingAndSpecificationVnfResourceViewRepository.findAll(specification, sort);
        resetVnfResourceViewAssociations(vnfResourceViews);
        return vnfResourceViews;
    }

    @Override
    public Iterable<VnfResourceView> findAll(final Sort sort) {
        final Iterable<VnfInstanceIdProjection> vnfInstanceIdProjections = pagingAndSpecificationVnfResourceViewRepository.findAllBy(sort);
        final List<String> ids = getVnfInstanceIdsFromProjections(vnfInstanceIdProjections);
        final Map<String, VnfResourceView> vnfResourceMap = findAllPartial(ids);
        return new ArrayList<>(vnfResourceMap.values());
    }

    private Map<String, VnfResourceView> findAllPartial(Iterable<String> ids) {
        Map<String, VnfResourceView> vnfResourceViews = partialSelectionQueryExecutor.fetchEntity(VnfResourceView.class,
                                                                                                  VnfResourceView_.vnfInstanceId, ids);
        return reorderVnfResourceViews(vnfResourceViews, ids);
    }

    private void resetVnfResourceViewAssociations(List<VnfResourceView> vnfResourceViews) {
        vnfResourceViews.forEach(this::resetVnfResourceViewAssociations);
    }

    private void resetVnfResourceViewAssociations(VnfResourceView vnfResourceView) {
        vnfResourceView.setAllOperations(new ArrayList<>());
        vnfResourceView.setScaleInfoEntity(new ArrayList<>());
        vnfResourceView.setLastLifecycleOperation(null);
    }

    private Map<String, VnfResourceView> reorderVnfResourceViews(Map<String, VnfResourceView> vnfInstanceMap, Iterable<String> ids) {
        if (IterableUtils.isEmpty(ids)) {
            return vnfInstanceMap;
        }
        return StreamSupport.stream(ids.spliterator(), false)
                .map(vnfInstanceMap::get)
                .collect(Collectors.toMap(VnfResourceView::getVnfInstanceId, Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
    }

    private List<String> getVnfInstanceIdsFromProjections(Iterable<VnfInstanceIdProjection> vnfInstanceIdProjections) {
        return StreamSupport.stream(vnfInstanceIdProjections.spliterator(), false)
                .map(VnfInstanceIdProjection::getVnfInstanceId)
                .collect(Collectors.toList());
    }

    private List<String> getVnfInstanceIdsFromEntities(Iterable<VnfResourceView> vnfInstances) {
        return StreamSupport.stream(vnfInstances.spliterator(), false)
                .map(VnfResourceView::getVnfInstanceId)
                .collect(Collectors.toList());
    }
}
