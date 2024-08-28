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
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;

public interface CustomVnfInstanceRepository<T extends VnfInstance, K> {
    List<T> findAll();

    List<T> findAllById(Iterable<K> ids);

    Map<String, Tuple> selectFields(List<T> vnfInstances, List<String> fields);

    <A, M extends Attribute<T, ?>> void fetchAssociation(List<T> vnfInstances, Class<A> associationClass, M associationName);

    T findByVnfInstanceId(K vnfInstanceId);

    Optional<T> findById(K id);

    Optional<VnfInstance> findById(String vnfInstanceId, List<String> excludedFields);

    Page<T> findAll(Pageable pageable);

    Page<T> findAll(Specification<T> specification, Pageable pageable);

    List<T> findAll(Specification<T> specification);

    List<T> findAll(Specification<T> specification, Sort sort);

    List<T> findAll(Sort sort);
}
