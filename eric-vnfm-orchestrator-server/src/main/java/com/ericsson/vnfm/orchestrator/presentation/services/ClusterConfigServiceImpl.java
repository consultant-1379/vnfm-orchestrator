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

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DEFAULT_CRD_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_CONFIG_FILE_NOT_DEFAULT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_CONFIG_UPDATE_VALIDATION_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_CONFIG_UPDATE_VALIDATION_TITLE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NAMESPASES_MISSING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NAMESPASES_MISSING_MSG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NOT_FOUND;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NOT_FOUND_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.INVALID_CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.NOT_SAME_CLUSTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.NO_VERIFICATION_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_BACK_OFF_PERIOD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_KEY_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_LOCK_DURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Lock.CLUSTER_CONFIG_MAX_LOCK_ATTEMPTS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.VERIFICATION_NAMESPACE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.addConfigExtension;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertClusterConfigToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.multipartFileToString;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertMapToYamlFormat;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoResource;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.locks.Lock;
import com.ericsson.am.shared.locks.LockManager;
import com.ericsson.am.shared.locks.LockMode;
import com.ericsson.vnfm.orchestrator.filters.ClusterConfigQuery;
import com.ericsson.vnfm.orchestrator.model.ClusterConfigPatchRequest;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.validator.RespondentValidator;
import com.ericsson.vnfm.orchestrator.validator.Validator;
import com.ericsson.vnfm.orchestrator.validator.context.ClusterRegistrationValidationContext;
import com.ericsson.vnfm.orchestrator.validator.impl.ClusterRegistrationRespondentValidatorImpl;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;
import com.ericsson.workflow.orchestration.mgmt.model.v3.Namespace;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterConfigServiceImpl implements ClusterConfigService {

    private final Validator<String> clusterDeregistrationValidatorImpl;

    private final RespondentValidator<ClusterRegistrationValidationContext, ClusterServerDetailsResponse> clusterRegistrationRespondentValidatorImpl;

    private final DatabaseInteractionService databaseInteractionService;

    private final ClusterConfigInstanceRepository clusterConfigInstanceRepository;

    private final ClusterConfigQuery clusterConfigQuery;

    private final LockManager lockManager;

    private WorkflowService workflowService;

    public ClusterConfigServiceImpl(RespondentValidator<ClusterRegistrationValidationContext,
            ClusterServerDetailsResponse> clusterRegistrationRespondentValidatorImpl,
                                    Validator<String> clusterDeregistrationValidatorImpl,
                                    ClusterConfigInstanceRepository clusterConfigInstanceRepository,
                                    DatabaseInteractionService databaseInteractionService,
                                    final ClusterConfigQuery clusterConfigQuery,
                                    LockManager lockManager) {
        this.clusterRegistrationRespondentValidatorImpl = clusterRegistrationRespondentValidatorImpl;
        this.clusterDeregistrationValidatorImpl = clusterDeregistrationValidatorImpl;
        this.clusterConfigInstanceRepository = clusterConfigInstanceRepository;
        this.databaseInteractionService = databaseInteractionService;
        this.clusterConfigQuery = clusterConfigQuery;
        this.lockManager = lockManager;
    }

    @Override
    public ClusterConfigFile prepareRegistrationClusterConfig(MultipartFile configFile, String description, String crdNamespace, Boolean isDefault) {
        ClusterRegistrationValidationContext context = new ClusterRegistrationValidationContext(configFile, description, crdNamespace);
        ClusterServerDetailsResponse validationResponse = clusterRegistrationRespondentValidatorImpl.validate(context);
        String verificationNamespaceUid = extractVerificationNamespaceUid(validationResponse);
        String newCrdNamespace = StringUtils.defaultIfEmpty(crdNamespace, DEFAULT_CRD_NAMESPACE);
        boolean isDefaultValue = Boolean.TRUE.equals(isDefault);

        return mapToEntity(configFile, description, newCrdNamespace,
                           validationResponse.getHostUrl(), verificationNamespaceUid, isDefaultValue);
    }

    @Override
    @Transactional
    public void registerClusterConfig(ClusterConfigFile newClusterConfigFile) {
        databaseInteractionService.lockClusterConfigTable();
        setDefaultFlag(newClusterConfigFile);

        LOGGER.info("Save new cluster config file with name {}", newClusterConfigFile.getName());
        databaseInteractionService.saveClusterConfig(newClusterConfigFile);
    }

    @Override
    @Transactional
    public ClusterConfigFile updateClusterConfig(String clusterName, MultipartFile configFile, String description,
                                                 boolean skipSameClusterVerification, boolean isDefault) {

        ClusterRegistrationRespondentValidatorImpl.validateClusterDescription(description);
        ClusterConfigFile clusterConfigFile = databaseInteractionService.getClusterConfigByName(clusterName).orElseThrow(() -> {
            String message = String.format(CLUSTER_NOT_FOUND_MESSAGE, clusterName);
            return new ValidationException(message, CLUSTER_NOT_FOUND, HttpStatus.NOT_FOUND);
        });
        validateCannotUnmarkCurrentCluster(clusterConfigFile, isDefault);

        ClusterServerDetailsResponse clusterServerDetails = workflowService.validateClusterConfigFile(configFile.getResource());
        if (!skipSameClusterVerification) {
            verifySameClusterByUid(clusterConfigFile, clusterServerDetails);
            verifySameClusterByNamespaces(clusterName, clusterServerDetails);
        }

        clusterConfigFile.setDescription(description);
        clusterConfigFile.setVerificationNamespaceUid(extractVerificationNamespaceUid(clusterServerDetails));
        clusterConfigFile.setContent(multipartFileToString(configFile));

        if (isDefault) {
            databaseInteractionService.lockClusterConfigTable();
            updateDefaultFlag(clusterConfigFile, clusterName);
        } else {
            clusterConfigFile.setDefault(false);
            LOGGER.info(CLUSTER_CONFIG_FILE_NOT_DEFAULT, clusterConfigFile.getName());
        }
        return databaseInteractionService.saveClusterConfig(clusterConfigFile);
    }

    @Override
    @Transactional
    public ClusterConfigFile modifyClusterConfig(String name,
                                                 ClusterConfigPatchRequest updateRequest,
                                                 boolean isSkipSameClusterVerification) {
        ClusterConfigFile clusterConfigFile = databaseInteractionService.getClusterConfigByName(name).orElseThrow(() -> {
            String message = String.format(CLUSTER_NOT_FOUND_MESSAGE, name);
            return new ValidationException(message, CLUSTER_NOT_FOUND, HttpStatus.NOT_FOUND);
        });
        Boolean setDefault = null;
        if (updateRequest.getIsDefault() != null) {
            setDefault = updateRequest.getIsDefault();
            validateCannotUnmarkCurrentCluster(clusterConfigFile, setDefault);
        }
        if (updateRequest.getDescription().isPresent()) {
            clusterConfigFile.setDescription(updateRequest.getDescription().get());
        }
        if (updateRequest.getClusterConfig() != null) {
            JSONObject clusterConfigSource =
                    convertYamlStringIntoJson(clusterConfigFile.getContent());
            JSONObject clusterConfigPatch = new JSONObject(convertClusterConfigToMap(updateRequest.getClusterConfig()));
            JSONObject result = mergePatchJson(clusterConfigSource, clusterConfigPatch);
            String resultYaml = convertMapToYamlFormat(result.toMap());
            ClusterServerDetailsResponse clusterServerDetails =
                    workflowService.validateClusterConfigFile(convertYamlStringIntoResource(resultYaml, name));
            if (!isSkipSameClusterVerification) {
                verifySameClusterByUid(clusterConfigFile, clusterServerDetails);
                verifySameClusterByNamespaces(name, clusterServerDetails);
            }
            clusterConfigFile.setContent(resultYaml);
        }

        if (Boolean.TRUE.equals(setDefault)) {
            databaseInteractionService.lockClusterConfigTable();
            updateDefaultFlag(clusterConfigFile, clusterConfigFile.getName());
        }

        return databaseInteractionService.saveClusterConfig(clusterConfigFile);
    }

    @Override
    @Transactional
    public void deregisterClusterConfig(String clusterConfigFileName) {
        Lock lock = lockManager.getLock(LockMode.EXCLUSIVE, CLUSTER_CONFIG_KEY_PREFIX + addConfigExtension(clusterConfigFileName))
                .withAcquireRetries(CLUSTER_CONFIG_MAX_LOCK_ATTEMPTS, CLUSTER_CONFIG_BACK_OFF_PERIOD);
        if (!lock.lock(CLUSTER_CONFIG_LOCK_DURATION, TimeUnit.SECONDS)) {
            String clusterConfigInUseMessage =
                    String.format("Cluster config file %s is in use and not available for deletion.", clusterConfigFileName);
            throw new ValidationException(clusterConfigInUseMessage, "Cluster config file is in use and cannot be removed", HttpStatus.CONFLICT);
        }

        try {
            clusterDeregistrationValidatorImpl.validate(clusterConfigFileName);
            LOGGER.info("Delete cluster config file with name {}", clusterConfigFileName);
            databaseInteractionService.deleteClusterConfigByName(clusterConfigFileName);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public ClusterConfigFile getConfigFileByName(final String clusterConfigFileName) {
        String clusterName = addConfigExtension(clusterConfigFileName);
        return databaseInteractionService.getClusterConfigByName(clusterName)
                .orElseThrow(() -> new NotFoundException(String.format("Cluster config file %s not found", clusterConfigFileName)));
    }

    @Override
    @Transactional
    public ClusterConfigFile getOrDefaultConfigFileByName(String clusterConfigFileName) {
        try {
            return getConfigFileByName(clusterConfigFileName);
        } catch (NotFoundException ex) {
            LOGGER.warn("Cluster config with file name {} not found, getting default", clusterConfigFileName, ex);
            return databaseInteractionService.getDefaultCluster().get();
        }
    }

    @Override
    public List<String> getAllClusterConfigNames() {
        return databaseInteractionService.getAllClusterConfigNames();
    }

    @Override
    public Page<ClusterConfigFile> getCismClusterConfigs(Pageable pageable) {
        return databaseInteractionService.getClusterConfigs(pageable);
    }

    @Override
    public Page<ClusterConfigFile> getClusterConfigs(final String filter, final Pageable pageable) {
        if (filter != null && filter.length() > 0) {
            return clusterConfigQuery.getPageWithFilter(filter, pageable);
        } else {
            return databaseInteractionService.getClusterConfigs(pageable);
        }
    }

    @Override
    @Transactional
    public void changeClusterConfigFileStatus(String configFileName, VnfInstance vnfInstance, ConfigFileStatus newConfigFileStatus) {
        String clusterName = addConfigExtension(configFileName);
        ClusterConfigFile clusterConfigFile = getConfigFileByName(clusterName);
        changeExistingConfigFileStatus(clusterName, vnfInstance, newConfigFileStatus, clusterConfigFile);
    }

    private void changeExistingConfigFileStatus(String clusterName, VnfInstance vnfInstance,
                                                ConfigFileStatus newConfigFileStatus, ClusterConfigFile clusterConfigFile) {
        String vnfInstanceId = vnfInstance.getVnfInstanceId();
        if (ConfigFileStatus.IN_USE.equals(newConfigFileStatus)) {
            Optional<ClusterConfigInstance> clusterConfigInstance = clusterConfigInstanceRepository
                    .findByClusterConfigFileAndInstanceId(clusterConfigFile, vnfInstanceId);
            if (clusterConfigInstance.isEmpty()) {
                ClusterConfigInstance configInstance = new ClusterConfigInstance();
                configInstance.setInstanceId(vnfInstanceId);
                configInstance.setClusterConfigFile(clusterConfigFile);
                clusterConfigInstanceRepository.save(configInstance);
            }
        } else if (ConfigFileStatus.NOT_IN_USE.equals(newConfigFileStatus)) {
            LOGGER.info("Delete the vnfInstanceId : {} for clusterConfig name  : {} ", vnfInstanceId, clusterName);
            clusterConfigInstanceRepository.deleteByClusterConfigFileAndInstanceId(clusterConfigFile, vnfInstanceId);
        }
        LOGGER.info("Cluster config file status changed to {}. Cluster name - {}", newConfigFileStatus, clusterConfigFile.getName());
    }

    JSONObject mergePatchJson(JSONObject source, JSONObject patch) {
        Set<String> keys = patch.keySet();
        for (String key : keys) {
            if (Objects.equals(patch.get(key), null)) {
                if (source.has(key)) {
                    source.remove(key);
                }
            } else if (source.has(key) && patch.get(key) instanceof JSONObject) {
                source.put(key, mergePatchJson(source.getJSONObject(key), patch.getJSONObject(key)));
            } else {
                source.put(key, patch.get(key));
            }
        }
        return source;
    }

    void verifySameClusterByUid(ClusterConfigFile clusterConfigFile, ClusterServerDetailsResponse clusterServerDetails) {
        if (clusterConfigFile.getVerificationNamespaceUid() == null) {
            return;
        }
        String updatedClusterUid = extractVerificationNamespaceUid(clusterServerDetails);
        if (!updatedClusterUid.equals(clusterConfigFile.getVerificationNamespaceUid())) {
            throw new ValidationException(NOT_SAME_CLUSTER, INVALID_CLUSTER_CONFIG, BAD_REQUEST);
        }
    }

    void verifySameClusterByNamespaces(String clusterName, ClusterServerDetailsResponse clusterServerDetails) {
        Set<String> namespaces = clusterServerDetails.getNamespaces().stream()
                .map(Namespace::getName).collect(Collectors.toSet());
        List<String> namespacesAssociatedWithCluster = databaseInteractionService.getNamespacesAssociatedWithCluster(clusterName);

        if (!namespaces.containsAll(namespacesAssociatedWithCluster)) {
            Set<String> missingNamespaces = new HashSet<>(namespacesAssociatedWithCluster);
            missingNamespaces.removeAll(namespaces);
            String message = String.format(CLUSTER_NAMESPASES_MISSING_MSG, String.join(",", missingNamespaces));
            throw new ValidationException(message, CLUSTER_NAMESPASES_MISSING, BAD_REQUEST);
        }
    }

    @Autowired
    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    private void setDefaultFlag(final ClusterConfigFile newClusterConfigFile) {
        Optional<ClusterConfigFile> currentDefaultCluster = databaseInteractionService.getDefaultCluster();
        if (currentDefaultCluster.isEmpty()) {
            LOGGER.info("New cluster config file with name {} is marked as default", newClusterConfigFile.getName());
            newClusterConfigFile.setDefault(true);
        } else if (newClusterConfigFile.isDefault()) {
            ClusterConfigFile defaultCluster = currentDefaultCluster.orElseThrow();
            defaultCluster.setDefault(false);
            databaseInteractionService.saveClusterConfig(defaultCluster);
            LOGGER.info(CLUSTER_CONFIG_FILE_NOT_DEFAULT, defaultCluster.getName());
        }
    }

    private void updateDefaultFlag(final ClusterConfigFile currentClusterConfigFile,
                                   String clusterName) {
        Optional<ClusterConfigFile> optionalDefaultClusterConfig = databaseInteractionService.getDefaultCluster();

        if (optionalDefaultClusterConfig.isPresent()
                && !optionalDefaultClusterConfig.get().getName().equals(clusterName)) {
            ClusterConfigFile defaultClusterConfig = optionalDefaultClusterConfig.get();
            defaultClusterConfig.setDefault(false);
            databaseInteractionService.saveClusterConfig(defaultClusterConfig);
            LOGGER.info(CLUSTER_CONFIG_FILE_NOT_DEFAULT, defaultClusterConfig.getName());
        }
        currentClusterConfigFile.setDefault(true);
        LOGGER.info("Cluster config file with name {} sets as default", currentClusterConfigFile.getName());
    }

    private static ClusterConfigFile mapToEntity(MultipartFile configMultipartFile, String description,
                                                 String crdNamespace, String clusterServer, String verificationNamespaceUid,
                                                 Boolean isDefault) {
        String configFileDescription = ObjectUtils.defaultIfNull(description, Strings.EMPTY);
        String configFileAsString = multipartFileToString(configMultipartFile);

        return ClusterConfigFile.builder()
                .name(configMultipartFile.getOriginalFilename())
                .content(configFileAsString)
                .status(ConfigFileStatus.NOT_IN_USE)
                .description(configFileDescription)
                .crdNamespace(crdNamespace)
                .clusterServer(clusterServer)
                .verificationNamespaceUid(verificationNamespaceUid)
                .isDefault(isDefault)
                .build();
    }

    private static String extractVerificationNamespaceUid(ClusterServerDetailsResponse clusterServerDetailsResponse) {
        return clusterServerDetailsResponse.getNamespaces().stream()
                .filter(ns -> VERIFICATION_NAMESPACE.equals(ns.getName()))
                .map(Namespace::getUid).findFirst()
                .orElseThrow(() -> new ValidationException(NO_VERIFICATION_NAMESPACE, INVALID_CLUSTER_CONFIG, HttpStatus.BAD_REQUEST));
    }

    private static void validateCannotUnmarkCurrentCluster(ClusterConfigFile clusterConfig, boolean isDefault) {
        if (!isDefault && clusterConfig.isDefault()) {
            throw new ValidationException(CLUSTER_CONFIG_UPDATE_VALIDATION_ERROR, CLUSTER_CONFIG_UPDATE_VALIDATION_TITLE, BAD_REQUEST);
        }
    }
}
