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
package com.ericsson.vnfm.orchestrator.repositories;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;

public interface CustomVnfResourceViewRepository<T extends VnfResourceView> {
    Page<T> findAll(Pageable pageable);

    Map<String, Tuple> selectFields(List<T> entities, List<String> fields);

    <A, M extends Attribute<T, ?>> void fetchAssociation(List<T> vnfResourceViews, Class<A> associationClass, M associationName);

    Page<T> findAll(Specification<T> specification, Pageable pageable);

    List<T> findAll(Specification<T> specification);

    List<T> findAll(Specification<T> specification, Sort sort);

    Iterable<T> findAll(Sort sort);
}
