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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationStage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LifecycleOperationStageRepository extends JpaRepository<LifecycleOperationStage, String>,
        JpaSpecificationExecutor<LifecycleOperationStage> {

    @Query("SELECT l FROM LifecycleOperationStage l WHERE l.validUntil < :date " +
            "ORDER BY l.ownedSince ASC")
    List<LifecycleOperationStage> findExpiredLifecycleOperationStage(@Param("date") LocalDateTime expiredApplicationTime);
}
