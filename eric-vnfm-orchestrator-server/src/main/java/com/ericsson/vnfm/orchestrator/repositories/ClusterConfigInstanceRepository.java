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

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigInstance;

@Repository
public interface ClusterConfigInstanceRepository extends JpaRepository<ClusterConfigInstance, String> {

    Optional<ClusterConfigInstance> findByClusterConfigFileAndInstanceId(ClusterConfigFile clusterConfigFile, String vnfInstanceId);

    void deleteByClusterConfigFileAndInstanceId(ClusterConfigFile clusterConfigFile, String vnfInstanceId);

    void deleteAllByInstanceId(String instanceId);
}
