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
package com.ericsson.vnfm.orchestrator.filters.core;

import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import lombok.Getter;

@Getter
public class SpecificationContext<T> {
    private final CriteriaQuery<?> criteriaQuery;
    private final Root<T> root;
    private final Map<String, Join<?, T>> joins;
    private final CriteriaBuilder criteriaBuilder;

    SpecificationContext(final CriteriaQuery<?> criteriaQuery,
                                final Root<T> root, final CriteriaBuilder criteriaBuilder,
                                final Map<String, Join<?, T>> joins) {
        this.criteriaQuery = criteriaQuery;
        this.root = root;
        this.criteriaBuilder = criteriaBuilder;
        this.joins = Map.copyOf(joins);
    }
}
