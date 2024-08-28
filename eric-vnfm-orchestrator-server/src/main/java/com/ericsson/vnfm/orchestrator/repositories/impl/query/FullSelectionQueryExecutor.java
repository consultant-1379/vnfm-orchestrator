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

import java.util.List;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.extern.slf4j.Slf4j;

@Repository
@Transactional(readOnly = true)
@Slf4j
public class FullSelectionQueryExecutor {

    @PersistenceContext
    private EntityManager entityManager;

    public <R, T extends Attribute<R, ?>> List<R> execute(Class<R> rootClass, T association, SingularAttribute<R, ?> idField, Iterable<String> ids) {
        try {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final CriteriaQuery<R> criteriaQuery = criteriaBuilder.createQuery(rootClass);
            final Root<R> root = criteriaQuery.from(rootClass);
            root.fetch(association.getName(), JoinType.LEFT);
            criteriaQuery.select(root)
                    .distinct(true);

            if (!IterableUtils.isEmpty(ids)) {
                criteriaQuery.where(root.get(idField).in(Lists.newArrayList(ids)));
            }

            final TypedQuery<R> query = entityManager.createQuery(criteriaQuery);
            return query.getResultList();
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
