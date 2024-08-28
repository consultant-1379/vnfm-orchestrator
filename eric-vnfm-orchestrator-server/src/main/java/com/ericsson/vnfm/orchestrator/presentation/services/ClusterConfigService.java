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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.vnfm.orchestrator.model.ClusterConfigPatchRequest;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Service for operations connected with cluster config file.
 */
public interface ClusterConfigService {

    /**
     * Validates cluster config registration information, build cluster config entity for repository saving.
     * {@link com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException} - if
     * cluster config file is not valid.
     * @param configFile  - cluster config file content that will be saved.
     * @param description - meta information connected to cluster config file that will be saved.
     * @param crdNamespace - namespace where the CRD charts will be installed
     * @return - new cluster config file entity which is based on the information provided
     * and ready to be stored in repository.
     */
    ClusterConfigFile prepareRegistrationClusterConfig(MultipartFile configFile, String description,
                                                       String crdNamespace, Boolean isDefault);

    /**
     * Saves cluster config file. Can throw next exceptions:
     * @param newClusterConfigFile  - cluster config file for save.
     */
    void registerClusterConfig(ClusterConfigFile newClusterConfigFile);

    /**
     * @param name          cluster name as it previously been registered
     * @param configFile    cluster config file content
     * @param description   cluster description
     * @param skipSameClusterVerification  flag that allow to bypass verification tht config belongs to the same cluster
     * @param isDefault     flag that allow to set cluster as default
     * @return updated cluster config file entity
     */
    ClusterConfigFile updateClusterConfig(String name, MultipartFile configFile, String description,
                                          boolean skipSameClusterVerification, boolean isDefault);

    /**
     * @param clusterName     cluster name as it previously been registered
     * @param updateFields    fields that need to be changed
     * @param isSkipSameClusterVerification  flag that allow to bypass verification tht config belongs to the same cluster
     * @return modified cluster config file entity
     */
    ClusterConfigFile modifyClusterConfig(String clusterName, ClusterConfigPatchRequest updateFields,
                                          boolean isSkipSameClusterVerification);

    /**
     * Removes cluster config file fro repository. Can throw next exceptions:
     * {@link com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException} - if
     * cluster config file is not valid.
     *
     * @param clusterConfigFileName - name of cluster config entity that will be removed.
     */
    void deregisterClusterConfig(String clusterConfigFileName);

    /**
     * Retrieves cluster config file with particular name from repository.
     * Returns {@code null} in case there is no file with specified name.
     *
     * @param clusterConfigFileName - name of file entity that will be retrieved.
     * @return - entity that has parameter specified name.
     */
    ClusterConfigFile getConfigFileByName(String clusterConfigFileName);

    ClusterConfigFile getOrDefaultConfigFileByName(String clusterConfigFileName);

    /**
     * Retrieves one page of cluster configs matching filter, sorted accordingly.
     * @param filter filtering expression according to ETSI SOL003 v2.5.1. May be null.
     * @param pageable pagination and sorting parameters
     * @return
     */
    Page<ClusterConfigFile> getClusterConfigs(String filter, Pageable pageable);

    /**
     * Updates status of cluster config file by specified file name.
     *
     * @param clusterConfigFileName - name of cluster config to update.
     * @param newConfigFileStatus   - status that will be established.
     * @param vnfInstance           - the instance thats using the config file
     */
    void changeClusterConfigFileStatus(String clusterConfigFileName, VnfInstance vnfInstance, ConfigFileStatus newConfigFileStatus);

    /**
     * Retrieves all cluster config file names from repository. Returns empty list if no files in system.
     * @return - list of file names that exist in repository.
     */
    List<String> getAllClusterConfigNames();

    /**
     * Retrieves one page of cism cluster configs.
     * @param pageable pagination parameter
     * @return
     */
    Page<ClusterConfigFile> getCismClusterConfigs(Pageable pageable);
}
