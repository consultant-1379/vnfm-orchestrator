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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.impl.projections.VnfInstanceIdProjection;

@Repository
interface PagingAndSpecificationVnfResourceViewRepository extends
        PagingAndSortingRepository<VnfResourceView, String>, JpaSpecificationExecutor<VnfResourceView> {
    Page<VnfInstanceIdProjection> findAllBy(Pageable pageable);

    Iterable<VnfInstanceIdProjection> findAllBy(Sort sort);
}
