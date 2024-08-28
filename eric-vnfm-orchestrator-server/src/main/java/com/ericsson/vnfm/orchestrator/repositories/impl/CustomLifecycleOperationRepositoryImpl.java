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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.VNF_INSTANCE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.IterableUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation_;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.CustomLifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.projections.LifecycleOperationIdProjection;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.FullSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
@Transactional(readOnly = true)
public class CustomLifecycleOperationRepositoryImpl implements CustomLifecycleOperationRepository<LifecycleOperation, String> {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private PartialSelectionQueryExecutor partialSelectionQueryExecutor;

    @Autowired
    private FullSelectionQueryExecutor fullSelectionQueryExecutor;

    @Autowired
    private PagingAndSpecificationLifecycleOperationRepository pagingAndSpecificationLifecycleOperationRepository;

    @Override
    public List<LifecycleOperation> findAll() {
        final Map<String, LifecycleOperation> lifecycleOperationsMap = findAllWithoutEncryptedFields(Collections.emptyList());
        return new ArrayList<>(lifecycleOperationsMap.values());
    }

    @Override
    public Map<String, Tuple> selectFields(final List<LifecycleOperation> lifecycleOperations, final List<String> fields) {
        final List<String> ids = getOperationOccurrenceIdsFromEntities(lifecycleOperations);
        return partialSelectionQueryExecutor.selectEntityFields(LifecycleOperation.class, LifecycleOperation_.operationOccurrenceId, ids, fields);
    }

    @Override
    public Page<LifecycleOperation> findAll(final Pageable pageable) {
        final Page<LifecycleOperationIdProjection> pagedLifecycleOperationIdProjections = pagingAndSpecificationLifecycleOperationRepository
                .findAllBy(pageable);
        final List<LifecycleOperationIdProjection> lifecycleOperationIdProjections = pagedLifecycleOperationIdProjections.getContent();
        final List<String> operationOccurrenceIds = getOperationOccurrenceIdsFromProjections(lifecycleOperationIdProjections);
        final Map<String, LifecycleOperation> lifecycleOperations = findAllByIdIfPresent(operationOccurrenceIds);
        return pagedLifecycleOperationIdProjections
                .map(pagedLifecycleOperationIdProjection -> lifecycleOperations.get(pagedLifecycleOperationIdProjection.getOperationOccurrenceId()));
    }

    @Override
    public Page<LifecycleOperation> findAll(final Specification<LifecycleOperation> specification, final Pageable pageable) {
        final Page<LifecycleOperation> pagedLifecycleOperations = pagingAndSpecificationLifecycleOperationRepository.findAll(specification, pageable);
        List<LifecycleOperation> lifecycleOperations = pagedLifecycleOperations.getContent();
        final List<String> ids = getOperationOccurrenceIdsFromEntities(lifecycleOperations);
        final Map<String, LifecycleOperation> lifecycleOperationsMap = findAllWithoutEncryptedFields(ids);
        return pagedLifecycleOperations.map(lifecycleOperation -> lifecycleOperationsMap.get(lifecycleOperation.getOperationOccurrenceId()));
    }

    @Override
    public List<LifecycleOperation> findAll(final Specification<LifecycleOperation> specification) {
        final List<LifecycleOperation> lifecycleOperations = pagingAndSpecificationLifecycleOperationRepository.findAll(specification);
        final List<String> ids = getOperationOccurrenceIdsFromEntities(lifecycleOperations);
        final Map<String, LifecycleOperation> lifecycleOperationsMap = findAllWithoutEncryptedFields(ids);
        return new ArrayList<>(lifecycleOperationsMap.values());
    }

    @Override
    public List<LifecycleOperation> findAll(final Specification<LifecycleOperation> specification, final Sort sort) {
        final List<LifecycleOperation> lifecycleOperations = pagingAndSpecificationLifecycleOperationRepository.findAll(specification, sort);
        final List<String> ids = getOperationOccurrenceIdsFromEntities(lifecycleOperations);
        final Map<String, LifecycleOperation> lifecycleOperationsMap = findAllWithoutEncryptedFields(ids);
        return new ArrayList<>(lifecycleOperationsMap.values());
    }

    @Override
    public Iterable<LifecycleOperation> findAll(final Sort sort) {
        final Iterable<LifecycleOperationIdProjection> lifecycleOperationIdProjections =
                pagingAndSpecificationLifecycleOperationRepository.findAllBy(sort);
        final List<String> operationOccurrenceIds = getOperationOccurrenceIdsFromProjections(lifecycleOperationIdProjections);
        final Map<String, LifecycleOperation> lifecycleOperations = findAllByIdIfPresent(operationOccurrenceIds);
        return lifecycleOperations.values();
    }

    @Override
    public List<LifecycleOperation> findAllById(final Iterable<String> operationOccurrenceIds) {
        return findAllByIdWithAllFields(operationOccurrenceIds);
    }

    @Override
    public Optional<LifecycleOperation> findById(final String operationOccurrenceId) {
        final LifecycleOperation lifecycleOperation = findByOperationOccurrenceId(operationOccurrenceId);
        return Optional.ofNullable(lifecycleOperation);
    }

    @Override
    public LifecycleOperation findByOperationOccurrenceId(final String id) {
        List<String> operationOccurrenceIds = List.of(id);
        final List<LifecycleOperation> lifecycleOperations = findAllByIdWithAllFields(operationOccurrenceIds);

        return lifecycleOperations.isEmpty() ? null : lifecycleOperations.get(0);
    }

    @Override
    public LifecycleOperation findByOperationOccurrenceIdPartial(final String id) {
        if (Strings.isBlank(id)) {
            return null;
        }
        Map<String, LifecycleOperation> lifecycleOperations = partialSelectionQueryExecutor
                .fetchEntity(LifecycleOperation.class, LifecycleOperation_.operationOccurrenceId, List.of(id));
        return lifecycleOperations.get(id);
    }

    private Map<String, LifecycleOperation> findAllWithoutEncryptedFields(Iterable<String> ids) {
        Map<String, LifecycleOperation> lifecycleOperations = partialSelectionQueryExecutor
                .fetchEntity(LifecycleOperation.class, LifecycleOperation_.operationOccurrenceId, ids);
        partialSelectionQueryExecutor.fetchAssociation(LifecycleOperation.class, VnfInstance.class, LifecycleOperation_.vnfInstance,
                                                       LifecycleOperation_.operationOccurrenceId, ids, lifecycleOperations);
        return reorderLifecycleOperations(lifecycleOperations, ids);
    }

    private Map<String, LifecycleOperation> findAllByIdIfPresent(Iterable<String> ids) {
        return IterableUtils.isEmpty(ids) ? Collections.emptyMap() : findAllWithoutEncryptedFields(ids);
    }

    private Map<String, LifecycleOperation> reorderLifecycleOperations(Map<String, LifecycleOperation> lifecycleOperations, Iterable<String> ids) {
        if (IterableUtils.isEmpty(ids)) {
            return lifecycleOperations;
        }
        return StreamSupport.stream(ids.spliterator(), false)
                .map(lifecycleOperations::get)
                .collect(Collectors.toMap(LifecycleOperation::getOperationOccurrenceId, Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
    }

    private List<LifecycleOperation> findAllByIdWithAllFields(Iterable<String> operationOccurrenceIds) {
        if (IterableUtils.isEmpty(operationOccurrenceIds)) {
            return new ArrayList<>();
        }
        List<LifecycleOperation> lifecycleOperations = fullSelectionQueryExecutor.execute(LifecycleOperation.class, LifecycleOperation_.vnfInstance,
                                                                                          LifecycleOperation_.operationOccurrenceId,
                                                                                          operationOccurrenceIds);

        final List<String> vnfInstanceIds = lifecycleOperations.stream()
                .map(operation -> operation.getVnfInstance().getVnfInstanceId())
                .distinct()
                .collect(Collectors.toList());
        vnfInstanceRepository.findAllById(vnfInstanceIds);

        return lifecycleOperations;
    }

    private List<String> getOperationOccurrenceIdsFromProjections(Iterable<LifecycleOperationIdProjection> lifecycleOperationIdProjections) {
        return StreamSupport.stream(lifecycleOperationIdProjections.spliterator(), false)
                .map(LifecycleOperationIdProjection::getOperationOccurrenceId)
                .collect(Collectors.toList());
    }

    private List<String> getOperationOccurrenceIdsFromEntities(Iterable<LifecycleOperation> lifecycleOperationIdProjections) {
        return StreamSupport.stream(lifecycleOperationIdProjections.spliterator(), false)
                .map(LifecycleOperation::getOperationOccurrenceId)
                .collect(Collectors.toList());
    }

    @Override
    public List<LifecycleOperation> findByVnfInstanceAndTypesAndStates(final VnfInstance vnfInstance,
                                                                       final List<LifecycleOperationType> types,
                                                                       final List<LifecycleOperationState> states) {
        return new SearchBuilder()
                .withVnfInstance(vnfInstance)
                .withStates(states)
                .withTypes(types)
                .sortByStateEnteredTimeDesc()
                .search();
    }

    @Override
    public List<LifecycleOperation> findByVnfInstanceOrderByStateEnteredTimeDesc(final VnfInstance vnfInstance) {
        return new SearchBuilder()
                .withVnfInstance(vnfInstance)
                .sortByStateEnteredTimeDesc()
                .search();
    }

    @Override
    public List<LifecycleOperation> findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(
            final VnfInstance vnfInstance, final LocalDateTime stateEnteredTime) {
        return new SearchBuilder()
                .withVnfInstance(vnfInstance)
                .withStates(List.of(LifecycleOperationState.COMPLETED))
                .isBeforeStateEnteredTime(stateEnteredTime)
                .sortByStateEnteredTimeDesc()
                .search();
    }

    @Override
    public List<LifecycleOperation> findByVnfInstanceAndStates(final VnfInstance vnfInstance, final List<LifecycleOperationState> states) {
        return new SearchBuilder()
                .withVnfInstance(vnfInstance)
                .withStates(states)
                .search();
    }

    private class SearchBuilder {

        private final CriteriaBuilder criteriaBuilder;
        private final CriteriaQuery<LifecycleOperation> criteriaQuery;
        private final Root<LifecycleOperation> lifecycleOperationRoot;
        private final List<Predicate> predicates = new ArrayList<>();
        private Order order;

        SearchBuilder() {
            criteriaBuilder = entityManager.getCriteriaBuilder();
            criteriaQuery = criteriaBuilder.createQuery(LifecycleOperation.class);
            lifecycleOperationRoot = criteriaQuery.from(LifecycleOperation.class);
        }

        SearchBuilder withVnfInstance(VnfInstance vnfInstance) {
            final Predicate predicate = criteriaBuilder.equal(lifecycleOperationRoot.get(VNF_INSTANCE), vnfInstance);
            predicates.add(predicate);
            return this;
        }

        SearchBuilder withTypes(List<LifecycleOperationType> types) {
            final Predicate predicate = lifecycleOperationRoot.get("lifecycleOperationType").in(types);
            predicates.add(predicate);
            return this;
        }

        SearchBuilder withStates(List<LifecycleOperationState> states) {
            final Predicate predicate = lifecycleOperationRoot.get("operationState").in(states);
            predicates.add(predicate);
            return this;
        }

        SearchBuilder sortByStateEnteredTimeDesc() {
            order = criteriaBuilder.desc(lifecycleOperationRoot.get("stateEnteredTime"));
            return this;
        }

        SearchBuilder isBeforeStateEnteredTime(LocalDateTime stateEnteredTime) {
            final Predicate predicate = criteriaBuilder.lessThanOrEqualTo(lifecycleOperationRoot.get("stateEnteredTime"), stateEnteredTime);
            predicates.add(predicate);
            return this;
        }

        List<LifecycleOperation> search() {
            try {
                final Predicate composedPredicates = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                criteriaQuery.select(lifecycleOperationRoot)
                        .where(composedPredicates);
                if (!Objects.isNull(order)) {
                    criteriaQuery.orderBy(order);
                }
                final TypedQuery<LifecycleOperation> typedQuery = entityManager.createQuery(criteriaQuery);
                return typedQuery.getResultList();
            } finally {
                if (entityManager != null && entityManager.isOpen()) {
                    entityManager.close();
                }
            }
        }
    }
}
