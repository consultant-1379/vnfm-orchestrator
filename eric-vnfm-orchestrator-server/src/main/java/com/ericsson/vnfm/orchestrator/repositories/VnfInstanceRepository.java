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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

@Repository
public interface VnfInstanceRepository extends JpaRepository<VnfInstance, String>,
        JpaSpecificationExecutor<VnfInstance>, CustomVnfInstanceRepository<VnfInstance, String> {

    @Query("SELECT DISTINCT vi.vnfProductName FROM VnfInstance vi WHERE vi.vnfProductName LIKE concat('%',?1,'%')")
    List<String> findDistinctVnfProductName(String vnfProductName, Pageable pageable);

    @Query("SELECT DISTINCT vi.vnfSoftwareVersion FROM VnfInstance vi WHERE vi.vnfSoftwareVersion LIKE " +
            "concat('%',?1,'%')")
    List<String> findDistinctVnfSoftwareVersion(String vnfSoftwareVersion, Pageable pageable);

    @Query("SELECT DISTINCT vi.vnfdVersion FROM VnfInstance vi WHERE vi.vnfdVersion LIKE concat('%',?1,'%')")
    List<String> findDistinctVnfdVersion(String vnfdVersion, Pageable pageable);

    @Query("SELECT DISTINCT vi.clusterName FROM VnfInstance vi WHERE vi.clusterName LIKE concat('%',?1,'%')")
    List<String> findDistinctClusterName(String clusterName, Pageable pageable);

    void deleteByVnfInstanceId(String vnfInstanceId);

    @Query("SELECT vi.vnfInstanceName FROM VnfInstance vi WHERE vi.clusterName = ?1 OR vi.clusterName = ?2 AND vi.namespace = ?3")
    List<String> findDuplicateClusterNamespace(String clusterWithConfig, String clusterWithoutConfig, String namespace);

    @Query(value = "SELECT COUNT(c) FROM ClusterConfigFile c " +
            "JOIN VnfInstance v ON c.name=v.clusterName OR c.name=concat(v.clusterName,'.config') " +
            "WHERE v.vnfInstanceName = :vnfInstanceName " +
            "AND v.namespace = :namespace " +
            "AND c.clusterServer = (SELECT c2.clusterServer FROM ClusterConfigFile c2 WHERE c2.name= :clusterName )")
    int findInstanceDuplicates(@Param("vnfInstanceName") String vnfInstanceName,
                               @Param("namespace") String namespace,
                               @Param("clusterName") String clusterName);

    @Query("SELECT vnf.namespace FROM VnfInstance vnf " +
            "WHERE vnf.clusterName = :clusterName OR concat(vnf.clusterName,'.config') = :clusterName")
    List<String> getNamespacesAssociatedWithCluster(@Param("clusterName") String clusterName);

    @Modifying
    @Query("UPDATE VnfInstance vi SET vi.operationOccurrenceId = :operationOccurrenceId WHERE vi.vnfInstanceId = :vnfInstanceId")
    void updateOperationOccurrenceId(@Param("vnfInstanceId") String vnfInstanceId,
                                     @Param("operationOccurrenceId") String operationOccurrenceId);

    @Query(value = "SELECT CAST(vi.supported_operations as varchar) supported_operations from app_vnf_instance vi WHERE vi.vnf_id = :vnfInstanceId",
            nativeQuery = true)
    String findSupportedOperationsByVnfInstanceId(@Param("vnfInstanceId") String vnfInstanceId);

    List<VnfInstance> findByVnfInstanceNameAndInstantiationState(String vnfInstanceName, InstantiationState instantiationState);
}
