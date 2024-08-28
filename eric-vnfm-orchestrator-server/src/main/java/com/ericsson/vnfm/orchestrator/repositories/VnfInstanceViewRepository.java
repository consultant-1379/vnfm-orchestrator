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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceView;

@Repository
public interface VnfInstanceViewRepository extends JpaRepository<VnfInstanceView, String>,
        JpaSpecificationExecutor<VnfInstanceView> {

    @Query("SELECT DISTINCT iv.softwarePackage FROM VnfInstanceView iv WHERE iv.softwarePackage LIKE " +
            "concat('%',?1,'%')")
    List<String> findDistinctSoftwarePackage(String softwarePackage, Pageable pageable);
}
