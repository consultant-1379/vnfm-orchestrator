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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;

public class FilterSpecification<T> implements Specification<T> {
    private static final long serialVersionUID = 1;

    public static final char EXPRESSION_PATH_SEPARATOR = '.';
    private final boolean distinct;
    private final transient List<FilterExpressionOneValue<? extends Comparable>> oneValueFilters;
    private final transient List<FilterExpressionMultiValue<? extends Comparable>> multiValueFilters;
    private final Map<String, String> joinKeyMappings;

    public FilterSpecification(final List<FilterExpressionOneValue<?>> oneValueFilters,
                               final List<FilterExpressionMultiValue<?>> multiValueFilters) {
        this.oneValueFilters = oneValueFilters;
        this.multiValueFilters = multiValueFilters;
        this.distinct = false;
        this.joinKeyMappings = Collections.emptyMap();
    }

    public FilterSpecification(final List<FilterExpressionOneValue<?>> oneValueFilters,
                               final List<FilterExpressionMultiValue<?>> multiValueFilters,
                               boolean distinct) {
        this.oneValueFilters = oneValueFilters;
        this.multiValueFilters = multiValueFilters;
        this.distinct = distinct;
        this.joinKeyMappings = Collections.emptyMap();
    }

    public FilterSpecification(final List<FilterExpressionOneValue<? extends Comparable>> oneValueFilters,
                               final List<FilterExpressionMultiValue<? extends Comparable>> multiValueFilters,
                               final Map<String, String> joinKeyMappings, final boolean distinct) {
        this.distinct = distinct;
        this.oneValueFilters = oneValueFilters;
        this.multiValueFilters = multiValueFilters;
        this.joinKeyMappings = joinKeyMappings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder criteriaBuilder) {
        query.distinct(distinct);
        Map<String, Join<?, T>> joins = createJoins(root);
        SpecificationContext<T> context = new SpecificationContext<>(query, root, criteriaBuilder, joins);
        final List<Predicate> predicates = oneValueFilters.stream().map(f -> createPredicate(f, context)).collect(Collectors.toList());
        predicates.addAll(multiValueFilters.stream().map(f -> createPredicate(f, context)).collect(Collectors.toList()));
        predicates.addAll(createExtraPredicates(context));
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    @SuppressWarnings("java:51172")
    protected Collection<? extends Predicate> createExtraPredicates(final SpecificationContext<T> context) {
        return Collections.emptyList();
    }

    protected Map<String, Join<?, T>> createJoins(final Root<T> root) {
        Map<String, Join<?, T>> joins = new HashMap<>();
        for (FilterExpressionOneValue<?> filter : oneValueFilters) {
            if (filter.getJoinType() == null) {
                continue;
            }
            String[] keys = StringUtils.split(filter.getKey(), EXPRESSION_PATH_SEPARATOR);
            joins.computeIfAbsent(
                    keys[0], key -> root.join(Optional.ofNullable(joinKeyMappings.get(key)).orElse(key), filter.getJoinType()));
        }
        for (FilterExpressionMultiValue<?> filter : multiValueFilters) {
            if (filter.getJoinType() == null) {
                continue;
            }
            String[] keys = StringUtils.split(filter.getKey(), EXPRESSION_PATH_SEPARATOR);
            joins.computeIfAbsent(
                    keys[0], key -> root.join(Optional.ofNullable(joinKeyMappings.get(key)).orElse(key), filter.getJoinType()));
        }
        return joins;
    }

    protected static <T, V extends Comparable<V>> Predicate createPredicate(FilterExpressionOneValue<V> filterExpression,
                                                                            SpecificationContext<T> context) {
        Expression<V> expression = getExpression(filterExpression.getJoinType(), filterExpression.getKey(), context);
        switch (filterExpression.getOperation()) {
            case EQUAL:
                return context.getCriteriaBuilder().equal(expression, filterExpression.getValue());
            case NOT_EQUAL:
                return context.getCriteriaBuilder().notEqual(expression, filterExpression.getValue());
            case GREATER_THAN:
                return context.getCriteriaBuilder().greaterThan(expression, filterExpression.getValue());
            case GREATER_THAN_EQUAL:
                return context.getCriteriaBuilder().greaterThanOrEqualTo(expression, filterExpression.getValue());
            case LESS_THAN:
                return context.getCriteriaBuilder().lessThan(expression, filterExpression.getValue());
            case LESS_THAN_EQUAL:
                return context.getCriteriaBuilder().lessThanOrEqualTo(expression, filterExpression.getValue());
            default:
                throw new IllegalArgumentException("Invalid operation provided");
        }
    }

    protected static <T, V extends Comparable<V>> Expression<V> getExpression(final JoinType joinType, final String key,
                                                                              final SpecificationContext<T> context) {
        if (joinType == null) {
            return context.getRoot().get(key);
        } else {
            String[] keys = StringUtils.split(key, EXPRESSION_PATH_SEPARATOR);
            return context.getJoins().get(keys[0]).get(keys[1]);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T, V extends Comparable<V>> Predicate createPredicate(FilterExpressionMultiValue<V> filterExpression,
                                                                            SpecificationContext<T> context) {
        Expression<V> expression = getExpression(filterExpression.getJoinType(), filterExpression.getKey(), context);
        final CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();
        switch (filterExpression.getOperation()) {
            case IN:
                return expression.in(filterExpression.getValues());
            case NOT_IN:
                return expression.in(filterExpression.getValues()).not();
            case CONTAINS:
                return filterExpression.getValues().stream().map(value -> "%" + value + "%")
                        .map(s -> criteriaBuilder.like((Expression<String>) expression, s))
                        .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            predicates -> criteriaBuilder.or(predicates.toArray(new Predicate[0]))));
            case NOT_CONTAINS:
                return filterExpression.getValues().stream().map(value -> "%" + value + "%")
                        .map(s -> criteriaBuilder.like((Expression<String>) expression, s))
                        .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            predicates -> criteriaBuilder.or(predicates.toArray(new Predicate[0])).not()));
            default:
                throw new IllegalArgumentException("Invalid operation provided");
        }
    }
}
