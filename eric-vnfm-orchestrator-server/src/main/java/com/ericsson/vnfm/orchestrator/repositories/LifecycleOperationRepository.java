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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

@Repository
public interface LifecycleOperationRepository extends JpaRepository<LifecycleOperation, String>,
        JpaSpecificationExecutor<LifecycleOperation>, CustomLifecycleOperationRepository<LifecycleOperation, String> {

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Query("SELECT l FROM LifecycleOperation l WHERE l.operationOccurrenceId = :id ")
    LifecycleOperation committedFindByOperationOccurrenceId(@Param("id") String id);

    List<LifecycleOperation> findByVnfInstance(VnfInstance instanceId);

    @Query("SELECT l.operationOccurrenceId FROM LifecycleOperation l WHERE (l.operationState = 'STARTING' OR l.operationState = " + "'PROCESSING'" +
            " OR l.operationState = 'ROLLING_BACK') AND l.expiredApplicationTime != null AND l.expiredApplicationTime < :date")
    List<String> findProgressExpiredOperation(@Param("date") LocalDateTime expiredApplicationTime, Pageable pageable);

    @Query("SELECT l.operationOccurrenceId FROM LifecycleOperation l WHERE (l.operationState = 'STARTING' OR l.operationState = " + "'PROCESSING'" +
            " OR l.operationState = 'ROLLING_BACK')")
    List<String> findAllInProgress();

    @Query("SELECT l FROM LifecycleOperation l WHERE l.vnfInstance = :vnfInstance " +
            "AND l.lifecycleOperationType NOT IN (com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE) " +
            "AND l.sourceVnfdId <> l.targetVnfdId " +
            "AND l.sourceVnfdId IS NOT NULL " +
            "AND l.operationState = com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED " +
            "ORDER BY l.startTime DESC")
    List<LifecycleOperation> findVnfdChangingOperationsByInstanceDesc(@Param("vnfInstance") VnfInstance vnfInstance);

    Integer countByOperationStateNotIn(List<LifecycleOperationState> operationStates);

    Integer countByLifecycleOperationTypeAndOperationStateNotIn(LifecycleOperationType lifecycleOperationType,
                                                                List<LifecycleOperationState> operationStates);

    Integer countByVnfInstanceAndOperationStateNotIn(VnfInstance vnfInstance, List<LifecycleOperationState> operationStates);

}