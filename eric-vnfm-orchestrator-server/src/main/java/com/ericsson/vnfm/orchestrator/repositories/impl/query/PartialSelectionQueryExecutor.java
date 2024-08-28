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
package com.ericsson.vnfm.orchestrator.repositories.impl.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.TupleMapperFactory;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleAssociationMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity.TupleEntityMapper;
import com.google.common.collect.Lists;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.extern.slf4j.Slf4j;

@Repository
@Transactional(readOnly = true)
@Slf4j
public class PartialSelectionQueryExecutor {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TupleMapperFactory tupleMapperFactory;

    public <R, A, T extends Attribute<R, ?>> void fetchAssociation(Class<R> rootClass, Class<A> associationClass, T association,
                                                                   SingularAttribute<R, ?> idField, Iterable<String> ids, Map<String, R> result) {
        try {
            if (MapUtils.isEmpty(result)) {
                return;
            }
            tryFetchAssociation(rootClass, associationClass, association, idField, ids, result);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    public <R> Map<String, Tuple> selectEntityFields(Class<R> rootClass, SingularAttribute<R, String> idField, Iterable<String> ids,
                                                     List<String> selectFields) {
        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();

            final Root<R> root = criteriaQuery.from(rootClass);

            List<Selection<?>> selectColumns = new ArrayList<>();
            selectColumns.add(root.get(idField));
            final List<Selection<?>> customFieldSelections = getSelectColumnsByFields(root, rootClass, selectFields);
            selectColumns.addAll(customFieldSelections);

            if (!IterableUtils.isEmpty(ids)) {
                criteriaQuery.where(root.get(idField).in(Lists.newArrayList(ids)));
            }

            criteriaQuery.multiselect(selectColumns)
                    .distinct(true);

            return entityManager.createQuery(criteriaQuery).getResultList()
                    .stream()
                    .collect(Collectors.toMap(tuple -> tuple.get(root.get(idField)), Function.identity()));
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    public <R> Map<String, R> fetchEntity(Class<R> rootClass, SingularAttribute<R, String> idField, Iterable<String> ids) {
        return fetchEntity(rootClass, idField, ids, DefaultExcludedFieldsFactory.getEntityExcludedFields(rootClass));
    }

    public <R> Map<String, R> fetchEntity(Class<R> rootClass, SingularAttribute<R, String> idField, Iterable<String> ids,
                                          List<String> excludedFields) {
        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();

            final Root<R> root = criteriaQuery.from(rootClass);

            final ArrayList<String> excluded = new ArrayList<>(excludedFields);
            excluded.add("serialVersionUID");
            final List<Selection<?>> selectColumns = getSelectColumns(root, rootClass, excluded);

            if (!IterableUtils.isEmpty(ids)) {
                criteriaQuery.where(root.get(idField).in(Lists.newArrayList(ids)));
            }

            criteriaQuery.multiselect(selectColumns)
                .distinct(true);

            final List<Tuple> tuples = entityManager.createQuery(criteriaQuery).getResultList();
            final TupleEntityMapper<R> mapper = tupleMapperFactory.getEntityMapper(rootClass);
            return mapper.map(tuples, root);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private <R, A, T extends Attribute<R, ?>> void tryFetchAssociation(Class<R> rootClass, Class<A> associationClass, T association,
                                            SingularAttribute<R, ?> idField, Iterable<String> ids, Map<String, R> result) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();

        final Root<R> root = criteriaQuery.from(rootClass);
        final Join<R, A> associationJoin = root.join(association.getName(), JoinType.LEFT);

        final List<String> associationExcludedFields = DefaultExcludedFieldsFactory.getEntityExcludedFields(associationClass);
        final List<Selection<?>> selectColumns = getSelectColumns(associationJoin, associationClass, associationExcludedFields);
        final Selection<?> entityId = getEntityId(root, rootClass);
        selectColumns.add(entityId);

        if (!IterableUtils.isEmpty(ids)) {
            criteriaQuery.where(root.get(idField).in(Lists.newArrayList(ids)));
        }

        criteriaQuery.multiselect(selectColumns)
                .distinct(true);

        final TupleAssociationMapper<R, A> mapper = tupleMapperFactory.getAssociationMapper(association.getName(), rootClass, associationClass);
        try (Stream<Tuple> tuples = entityManager.createQuery(criteriaQuery).getResultStream()) {
            tuples.forEach(tuple -> mapper.map(tuple, root, associationJoin, result));
        }
    }

    private <R> List<Selection<?>> getSelectColumns(Root<R> root, Class<R> rootClass, List<String> excludedFields) {
        return Arrays.stream(rootClass.getDeclaredFields())
                .map(Field::getName)
                .filter(fieldName -> !excludedFields.contains(fieldName))
                .map(root::get)
                .collect(Collectors.toList());
    }

    private <R, A> List<Selection<?>> getSelectColumns(Join<R, A> associationJoin, Class<A> associationClass, List<String> excludedFields) {
        return Stream.concat(Arrays.stream(associationClass.getDeclaredFields()), Arrays.stream(associationClass.getSuperclass().getDeclaredFields()))
                .map(Field::getName)
                .filter(fieldName -> !excludedFields.contains(fieldName))
                .map(associationJoin::get)
                .collect(Collectors.toList());
    }

    private <R> List<Selection<?>> getSelectColumnsByFields(Root<R> root, Class<R> rootClass, List<String> fields) {
        return Arrays.stream(rootClass.getDeclaredFields())
                .map(Field::getName)
                .filter(fields::contains)
                .map(root::get)
                .collect(Collectors.toList());
    }

    private <R> Selection<?> getEntityId(Root<R> root, Class<R> rootClass) {
        return Arrays.stream(rootClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .map(Field::getName)
                .map(root::get)
                .findFirst()
                .orElseThrow();
    }
}
