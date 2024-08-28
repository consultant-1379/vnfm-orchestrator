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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;

@Repository
public interface ClusterConfigFileRepository extends JpaRepository<ClusterConfigFile, String>, JpaSpecificationExecutor<ClusterConfigFile> {

    Optional<ClusterConfigFile> findByName(String fileName);

    void deleteByName(String fileName);

    @Query("SELECT cluster.name FROM ClusterConfigFile cluster")
    List<String> getAllClusterConfigNames();

    List<ClusterConfigFile> getAllByVerificationNamespaceUidIsNull();


    Optional<ClusterConfigFile> findByIsDefaultTrue();

    @Query("SELECT COUNT(cluster.name) FROM ClusterConfigFile cluster")
    Integer getClustersCount();

    @Query("SELECT COUNT(cluster.name) FROM ClusterConfigFile cluster WHERE cluster.status = 'IN_USE'")
    Integer getClustersInUseCount();

    @Query("SELECT cluster.name FROM ClusterConfigFile cluster WHERE cluster.isDefault = true")
    Optional<String> getDefaultClusterConfigName();

    @Query("SELECT cluster.clusterServer FROM ClusterConfigFile cluster WHERE cluster.name = :clusterName")
    Optional<String> findClusterServerByClusterName(@Param("clusterName") String clusterName);

    @Query("SELECT cluster.crdNamespace FROM ClusterConfigFile cluster WHERE cluster.name = :clusterName")
    Optional<String> findCrdNamespaceByClusterName(@Param("clusterName") String clusterName);

    @Query(value = "SELECT cast(pg_advisory_xact_lock (123) as text)", nativeQuery = true)
    void advisoryLock();
}
