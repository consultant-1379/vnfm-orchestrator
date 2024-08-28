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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.repositories.CustomVnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.projections.VnfInstanceIdProjection;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.FullSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;
import lombok.extern.slf4j.Slf4j;

@Repository
@Transactional(readOnly = true)
@Slf4j
public class CustomVnfInstanceRepositoryImpl implements CustomVnfInstanceRepository<VnfInstance, String> {
    @Autowired
    private PagingAndSpecificationVnfInstanceRepository pagingAndSpecificationVnfInstanceRepository;

    @Autowired
    private PartialSelectionQueryExecutor partialSelectionQueryExecutor;

    @Autowired
    private FullSelectionQueryExecutor fullSelectionQueryExecutor;

    @Override
    public List<VnfInstance> findAll() {
        final Map<String, VnfInstance> vnfInstances = findAllPartial();
        return new ArrayList<>(vnfInstances.values());
    }

    @Override
    public List<VnfInstance> findAllById(Iterable<String> ids) {
        return findAllFull(ids);
    }

    @Override
    public Map<String, Tuple> selectFields(final List<VnfInstance> vnfInstances, final List<String> fields) {
        final List<String> ids = getVnfInstanceIdsFromEntities(vnfInstances);
        return partialSelectionQueryExecutor.selectEntityFields(VnfInstance.class, VnfInstance_.vnfInstanceId, ids, fields);
    }

    @Override
    public <A, T extends Attribute<VnfInstance, ?>> void fetchAssociation(final List<VnfInstance> vnfInstances, final Class<A> associationClass,
                                                                          final T associationName) {
        final Map<String, VnfInstance> vnfInstancesMap = vnfInstances.stream()
                .collect(Collectors.toMap(VnfInstance::getVnfInstanceId, Function.identity()));
        final List<String> vnfInstanceIds = vnfInstances.stream()
                .map(VnfInstance::getVnfInstanceId)
                .collect(Collectors.toList());
        partialSelectionQueryExecutor.fetchAssociation(VnfInstance.class, associationClass, associationName, VnfInstance_.vnfInstanceId,
                                                       vnfInstanceIds, vnfInstancesMap);
    }

    @Override
    public VnfInstance findByVnfInstanceId(final String vnfInstanceId) {
        final List<String> ids = List.of(vnfInstanceId);
        final List<VnfInstance> vnfInstances = findAllFull(ids);
        return vnfInstances.isEmpty() ? null : vnfInstances.get(0);
    }

    @Override
    public Optional<VnfInstance> findById(final String id) {
        final VnfInstance vnfInstance = findByVnfInstanceId(id);
        return Optional.ofNullable(vnfInstance);
    }

    @Override
    public Page<VnfInstance> findAll(final Pageable pageable) {
        final Page<VnfInstanceIdProjection> pagedVnfInstanceIdProjections = pagingAndSpecificationVnfInstanceRepository.findAllBy(pageable);
        final List<VnfInstanceIdProjection> vnfInstanceIdProjections = pagedVnfInstanceIdProjections.getContent();
        final List<String> ids = getVnfInstanceIdsFromProjections(vnfInstanceIdProjections);
        final Map<String, VnfInstance> vnfInstances = findAllPartial(ids);
        return pagedVnfInstanceIdProjections.map(vnfInstanceIdProjection -> vnfInstances.get(vnfInstanceIdProjection.getVnfInstanceId()));
    }

    @Override
    public Page<VnfInstance> findAll(final Specification<VnfInstance> specification, final Pageable pageable) {
        final Page<VnfInstance> pagedVnfInstances = pagingAndSpecificationVnfInstanceRepository.findAll(specification, pageable);
        List<VnfInstance> vnfInstances = pagedVnfInstances.getContent();
        resetVnfInstanceAssociations(vnfInstances);
        return pagedVnfInstances;
    }

    @Override
    public List<VnfInstance> findAll(final Specification<VnfInstance> specification) {
        List<VnfInstance> vnfInstances = pagingAndSpecificationVnfInstanceRepository.findAll(specification);
        resetVnfInstanceAssociations(vnfInstances);
        return vnfInstances;
    }

    @Override
    public List<VnfInstance> findAll(final Specification<VnfInstance> specification, final Sort sort) {
        List<VnfInstance> vnfInstances = pagingAndSpecificationVnfInstanceRepository.findAll(specification, sort);
        resetVnfInstanceAssociations(vnfInstances);
        return vnfInstances;
    }

    @Override
    public List<VnfInstance> findAll(final Sort sort) {
        final Iterable<VnfInstanceIdProjection> vnfInstanceIdProjections = pagingAndSpecificationVnfInstanceRepository.findAllBy(sort);
        final List<String> ids = getVnfInstanceIdsFromProjections(vnfInstanceIdProjections);
        final Map<String, VnfInstance> vnfInstances = findAllPartial(ids);
        return new ArrayList<>(vnfInstances.values());
    }

    private List<VnfInstance> findAllFull(Iterable<String> ids) {
        if (IterableUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        List<VnfInstance> vnfInstances = fullSelectionQueryExecutor.execute(VnfInstance.class,
                                                                            VnfInstance_.allOperations, VnfInstance_.vnfInstanceId, ids);
        if (!CollectionUtils.isEmpty(vnfInstances)) {
            vnfInstances = fullSelectionQueryExecutor.execute(VnfInstance.class, VnfInstance_.helmCharts, VnfInstance_.vnfInstanceId, ids);
        }
        if (!CollectionUtils.isEmpty(vnfInstances)) {
            fullSelectionQueryExecutor.execute(VnfInstance.class, VnfInstance_.scaleInfoEntity, VnfInstance_.vnfInstanceId, ids);
        }
        if (!CollectionUtils.isEmpty(vnfInstances)) {
            fullSelectionQueryExecutor.execute(VnfInstance.class, VnfInstance_.terminatedHelmCharts, VnfInstance_.vnfInstanceId, ids);
        }
        return vnfInstances;
    }

    private Map<String, VnfInstance> findAllPartial(Iterable<String> ids) {
        if (IterableUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        final Map<String, VnfInstance> vnfInstances = partialSelectionQueryExecutor.fetchEntity(VnfInstance.class, VnfInstance_.vnfInstanceId, ids);
        return reorderVnfInstances(vnfInstances, ids);
    }

    public Optional<VnfInstance> findById(String vnfInstanceId, List<String> excludedFields) {
        final Map<String, VnfInstance> vnfInstances = partialSelectionQueryExecutor.fetchEntity(
                VnfInstance.class, VnfInstance_.vnfInstanceId, List.of(vnfInstanceId), excludedFields);
        return Optional.ofNullable(vnfInstances)
                .map(map -> map.get(vnfInstanceId));
    }

    private Map<String, VnfInstance> findAllPartial() {
        List<String> ids = Collections.emptyList();
        final Map<String, VnfInstance> vnfInstances = partialSelectionQueryExecutor.fetchEntity(VnfInstance.class, VnfInstance_.vnfInstanceId, ids);
        return reorderVnfInstances(vnfInstances, ids);
    }

    private Map<String, VnfInstance> reorderVnfInstances(Map<String, VnfInstance> vnfInstances, Iterable<String> ids) {
        if (IterableUtils.isEmpty(ids)) {
            return vnfInstances;
        }
        return StreamSupport.stream(ids.spliterator(), false)
                .map(vnfInstances::get)
                .collect(Collectors.toMap(VnfInstance::getVnfInstanceId, Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
    }

    private List<String> getVnfInstanceIdsFromProjections(Iterable<VnfInstanceIdProjection> vnfInstanceIdProjections) {
        return StreamSupport.stream(vnfInstanceIdProjections.spliterator(), false)
                .map(VnfInstanceIdProjection::getVnfInstanceId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getVnfInstanceIdsFromEntities(Iterable<VnfInstance> vnfInstances) {
        return StreamSupport.stream(vnfInstances.spliterator(), false)
                .map(VnfInstance::getVnfInstanceId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void resetVnfInstanceAssociations(List<VnfInstance> vnfInstances) {
        vnfInstances.forEach(this::resetVnfInstanceAssociations);
    }

    private void resetVnfInstanceAssociations(VnfInstance vnfInstance) {
        vnfInstance.setAllOperations(new ArrayList<>());
        vnfInstance.setHelmCharts(new ArrayList<>());
        vnfInstance.setTerminatedHelmCharts(new ArrayList<>());
        vnfInstance.setScaleInfoEntity(new ArrayList<>());
    }
}
