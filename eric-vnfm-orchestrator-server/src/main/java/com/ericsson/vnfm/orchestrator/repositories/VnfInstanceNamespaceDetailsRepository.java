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
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;

@Repository
public interface VnfInstanceNamespaceDetailsRepository extends JpaRepository<VnfInstanceNamespaceDetails, String>,
                                                               JpaSpecificationExecutor<VnfInstanceNamespaceDetails> {

    Optional<VnfInstanceNamespaceDetails> findByVnfId(String instanceId);

    List<VnfInstanceNamespaceDetails> findByNamespaceAndClusterServer(String namespace, String clusterServer);

    @Transactional
    void deleteByVnfId(String vnfInstanceId);
}
