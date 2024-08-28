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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import jakarta.persistence.Tuple;

public interface CustomLifecycleOperationRepository<T extends LifecycleOperation, K> {
    List<T> findAll();

    Map<String, Tuple> selectFields(List<T> entities, List<String> fields);

    List<T> findAllById(Iterable<K> ids);

    LifecycleOperation findByOperationOccurrenceId(K id);

    LifecycleOperation findByOperationOccurrenceIdPartial(K id);

    Optional<LifecycleOperation> findById(K operationOccurrenceId);

    Page<T> findAll(Pageable pageable);

    Page<T> findAll(Specification<T> specification, Pageable pageable);

    List<T> findAll(Specification<T> specification);

    List<T> findAll(Specification<T> specification, Sort sort);

    Iterable<T> findAll(Sort sort);

    List<T> findByVnfInstanceAndTypesAndStates(VnfInstance vnfInstance, List<LifecycleOperationType> types, List<LifecycleOperationState> states);

    List<T> findByVnfInstanceOrderByStateEnteredTimeDesc(VnfInstance vnfInstance);

    List<T> findCompletedByVnfInstanceAndStateEnteredTimeBeforeOrderByStateEnteredTimeDesc(VnfInstance vnfInstance, LocalDateTime stateEnteredTime);

    List<T> findByVnfInstanceAndStates(VnfInstance vnfInstance, List<LifecycleOperationState> states);
}