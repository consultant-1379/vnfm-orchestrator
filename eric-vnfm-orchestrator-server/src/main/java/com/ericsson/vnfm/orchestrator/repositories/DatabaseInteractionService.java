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

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.infrastructure.annotations.SendStateChangedNotification;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.TaskName;
import com.ericsson.vnfm.orchestrator.model.converter.OperationDetailListConverter;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LifecycleInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RunningLcmOperationsAmountExceededException;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.OPERATION_IN_PROGRESS_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.TERMINAL_OPERATION_STATES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.addConfigExtension;
import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class DatabaseInteractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInteractionService.class);

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @Autowired
    private ClusterConfigInstanceRepository clusterConfigInstanceRepository;

    @Autowired
    private VnfInstanceNamespaceDetailsRepository vnfInstanceNamespaceDetailsRepository;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private ChangedInfoRepository changedInfoRepository;

    @Autowired
    private VnfResourceViewRepository vnfResourceViewRepository;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PartialSelectionQueryExecutor partialSelectionQueryExecutor;

    @Autowired
    private LcmOperationsConfig lcmOperationsConfig;

    // Vnf Instance Repository Interactions

    public VnfInstance getVnfInstance(final String vnfInstanceId) {
        return vnfInstanceRepository.findById(vnfInstanceId)
                .orElseThrow(() -> new NotFoundException("Vnf instance with id " + vnfInstanceId + " does not exist"));
    }

    public VnfInstance getVnfInstanceWithoutTransitiveRelations(final String vnfInstanceId) {
        final VnfInstance vnfInstance = vnfInstanceRepository.findById(
                        vnfInstanceId,
                        List.of(VnfInstance_.HELM_CHARTS, VnfInstance_.TERMINATED_HELM_CHARTS, VnfInstance_.SCALE_INFO_ENTITY))
                .orElseThrow(() -> new NotFoundException("Vnf instance with id " + vnfInstanceId + " does not exist"));
        // Queries below loads only corresponding entities without their relations, thus we avoid transitive relations fetching from DB
        fetchAssociationForVnfInstances(List.of(vnfInstance), LifecycleOperation.class, VnfInstance_.allOperations);
        fetchAssociationForVnfInstances(List.of(vnfInstance), HelmChart.class, VnfInstance_.helmCharts);
        fetchAssociationForVnfInstances(List.of(vnfInstance), TerminatedHelmChart.class, VnfInstance_.terminatedHelmCharts);
        fetchAssociationForVnfInstances(List.of(vnfInstance), ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);
        return vnfInstance;
    }

    public VnfInstance getVnfInstanceWithoutOperations(String vnfInstanceId) {
        final VnfInstance vnfInstance = vnfInstanceRepository.findById(
                        vnfInstanceId,
                        List.of(VnfInstance_.ALL_OPERATIONS, VnfInstance_.HELM_CHARTS, VnfInstance_.TERMINATED_HELM_CHARTS,
                                VnfInstance_.SCALE_INFO_ENTITY))
                .orElseThrow(() -> new NotFoundException("Vnf instance with id " + vnfInstanceId + " does not exist"));
        // Queries below loads only corresponding entities without their relations, thus we avoid transitive relations fetching from DB
        fetchAssociationForVnfInstances(List.of(vnfInstance), HelmChart.class, VnfInstance_.helmCharts);
        fetchAssociationForVnfInstances(List.of(vnfInstance), TerminatedHelmChart.class, VnfInstance_.terminatedHelmCharts);
        fetchAssociationForVnfInstances(List.of(vnfInstance), ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);
        return vnfInstance;
    }

    public List<VnfInstance> getAllVnfInstances() {
        return vnfInstanceRepository.findAll();
    }

    public List<VnfInstance> getVnfInstancesByNameAndInstantiationState(String vnfInstanceName, InstantiationState instantiationState) {
        return vnfInstanceRepository.findByVnfInstanceNameAndInstantiationState(vnfInstanceName, instantiationState);
    }

    public void initializeInstantiateOssTopologyFieldInVnfInstances(List<VnfInstance> vnfInstances) {
        List<String> vnfInstanceFields = List.of("instantiateOssTopology");
        final Map<String, Tuple> vnfInstanceFieldTuples = vnfInstanceRepository.selectFields(vnfInstances, vnfInstanceFields);
        for (VnfInstance vnfInstance : vnfInstances) {
            final String vnfInstanceId = vnfInstance.getVnfInstanceId();
            final Tuple tuple = vnfInstanceFieldTuples.get(vnfInstanceId);
            final String instantiateOssTopology = (String) tuple.get(1);
            vnfInstance.setInstantiateOssTopology(instantiateOssTopology);
        }
    }

    public void initializeInstantiateOssTopologyFieldInVnfResourceViews(List<VnfResourceView> vnfResourceViews) {
        List<String> vnfResourceViewFields = List.of("instantiateOssTopology");
        final Map<String, Tuple> vnfResourceViewTuples = vnfResourceViewRepository.selectFields(vnfResourceViews, vnfResourceViewFields);
        for (VnfResourceView vnfResourceView : vnfResourceViews) {
            final String vnfInstanceId = vnfResourceView.getVnfInstanceId();
            final Tuple tuple = vnfResourceViewTuples.get(vnfInstanceId);
            final String instantiateOssTopology = (String) tuple.get(1);
            vnfResourceView.setInstantiateOssTopology(instantiateOssTopology);
        }
    }

    public void initializeOperationParamsFieldInLifecycleOperations(List<LifecycleOperation> lifecycleOperations) {
        List<String> operationFields = List.of("operationParams");
        final Map<String, Tuple> operationFieldTuples = lifecycleOperationRepository.selectFields(lifecycleOperations, operationFields);
        for (LifecycleOperation lifecycleOperation : lifecycleOperations) {
            final String operationOccurrenceId = lifecycleOperation.getOperationOccurrenceId();
            final Tuple tuple = operationFieldTuples.get(operationOccurrenceId);
            final String operationParams = (String) tuple.get(1);
            lifecycleOperation.setOperationParams(operationParams);
        }
    }

    public <A, M extends Attribute<VnfInstance, ?>> void fetchAssociationForVnfInstances(List<VnfInstance> vnfInstances, Class<A> associationClass,
                                                                               M associationName) {
        vnfInstanceRepository.fetchAssociation(vnfInstances, associationClass, associationName);
    }

    public <A, M extends Attribute<VnfResourceView, ?>> void fetchAssociationForVnfResourceViews(List<VnfResourceView> vnfResourceViews,
                                                                                                 Class<A> associationClass, M associationName) {
        vnfResourceViewRepository.fetchAssociation(vnfResourceViews, associationClass, associationName);
    }

    @Transactional(readOnly = true)
    public List<OperationDetail> getSupportedOperationsByVnfInstanceId(final String vnfInstanceId) {
        OperationDetailListConverter operationDetailListConverter = new OperationDetailListConverter();
        final String supportedOperations = vnfInstanceRepository.findSupportedOperationsByVnfInstanceId(vnfInstanceId);
        return operationDetailListConverter.convertToEntityAttribute(supportedOperations);
    }

    public Page<VnfInstance> getAllVnfInstances(Pageable pageable) {
        return vnfInstanceRepository.findAll(pageable);
    }

    public VnfInstance saveVnfInstanceToDB(final VnfInstance vnfInstance) {
        return vnfInstanceRepository.save(vnfInstance);
    }

    public void persistVnfInstances(final List<VnfInstance> vnfInstances) {
        vnfInstanceRepository.saveAll(vnfInstances);
    }

    public void deleteVnfInstance(final VnfInstance vnfInstance) {
        deleteByVnfInstanceId(vnfInstance.getVnfInstanceId());
    }

    public void deleteByVnfInstanceId(final String vnfInstanceId) {
        vnfInstanceRepository.deleteByVnfInstanceId(vnfInstanceId);
    }

    public List<String> getDuplicateClusterNamespace(String clusterWithConfig, String clusterWithoutConfig,
                                                     String namespace) {
        return vnfInstanceRepository.findDuplicateClusterNamespace(clusterWithConfig, clusterWithoutConfig, namespace);
    }

    // Life Cycle Operation Repository Interactions

    @SendStateChangedNotification
    public LifecycleOperation persistLifecycleOperation(final LifecycleOperation operation) {
        LOGGER.info("Persisting LifecycleOperation with state {}", operation.getOperationState());
        return lifecycleOperationRepository.save(operation);
    }

    @SendStateChangedNotification
    public void persistLifecycleOperationInProgress(LifecycleOperation operation, VnfInstance vnfInstance,
                                                                  LifecycleOperationType lifecycleOperationType) {
        failIfRunningLcmOperationsAmountExceeded();

        addToOperationsInProgressTable(vnfInstance.getVnfInstanceId(), lifecycleOperationType);
        persistLifecycleOperation(operation);
        vnfInstanceRepository.updateOperationOccurrenceId(vnfInstance.getVnfInstanceId(), operation.getOperationOccurrenceId());
    }

    @VisibleForTesting
    public void failIfRunningLcmOperationsAmountExceeded() {
        final Integer runningLcmOperationsAmount = getAllOperationsNotInTerminalStates();

        final int limit = lcmOperationsConfig.getLcmOperationsLimit();

        if (runningLcmOperationsAmount >= limit) {
            throw new RunningLcmOperationsAmountExceededException(limit);
        }
    }

    public void persistChangedInfo(ChangedInfo changedInfo, String operationOccurrenceId) {
        final LifecycleOperation lifecycleOperation = lifecycleOperationRepository.getReferenceById(operationOccurrenceId);
        changedInfo.setLifecycleOperation(lifecycleOperation);
        changedInfoRepository.save(changedInfo);
    }

    public void addToOperationsInProgressTable(String vnfInstanceId, LifecycleOperationType type) {
        OperationInProgress operation = new OperationInProgress();
        operation.setVnfId(vnfInstanceId);
        operation.setLifecycleOperationType(type);
        checkLifecycleInProgress(vnfInstanceId);
        operationsInProgressRepository.save(operation);
    }

    public void checkLifecycleInProgress(final String vnfInstanceId) {
        Optional<OperationInProgress> operationInProgress = operationsInProgressRepository.findByVnfId(vnfInstanceId);
        if (operationInProgress.isPresent()) {
            throw new LifecycleInProgressException(String.format(OPERATION_IN_PROGRESS_ERROR_MESSAGE,
                                                                 operationInProgress.get().getLifecycleOperationType(),
                                                                 vnfInstanceId));
        }
    }

    public LifecycleOperation getLifecycleOperation(String operationId) {
        return lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
    }

    public LifecycleOperation getLifecycleOperationPartial(String operationId) {
        return lifecycleOperationRepository.findByOperationOccurrenceIdPartial(operationId);
    }

    public LifecycleOperation getCommittedLifecycleOperation(String operationId) {
        return lifecycleOperationRepository.committedFindByOperationOccurrenceId(operationId);
    }

    public List<LifecycleOperation> getLifecycleOperationsByVnfInstance(VnfInstance vnfInstance) {
        return lifecycleOperationRepository.findByVnfInstance(vnfInstance);
    }

    public int getDuplicateInstances(String instanceName, String namespace, String clusterName) {
        return vnfInstanceRepository.findInstanceDuplicates(instanceName, namespace, clusterName);
    }

    public List<LifecycleOperation> getAllOperations() {
        return lifecycleOperationRepository.findAll();
    }

    public Page<LifecycleOperation> getAllOperations(final Pageable pageable) {
        return lifecycleOperationRepository.findAll(pageable);
    }

    // Cluster Config File Repository Interactions

    public List<ClusterConfigFile> getAllClusterConfigs() {
        return clusterConfigFileRepository.findAll();
    }

    public Page<ClusterConfigFile> getClusterConfigs(Pageable pageable) {
        return clusterConfigFileRepository.findAll(pageable);
    }

    public Optional<ClusterConfigFile> getClusterConfigByName(String clusterConfigFileName) {
        return clusterConfigFileRepository.findByName(clusterConfigFileName);
    }

    public String getClusterConfigServerByClusterName(String clusterConfigFileName) {
        String clusterName = addConfigExtension(clusterConfigFileName);
        return clusterConfigFileRepository.findClusterServerByClusterName(clusterName)
                .orElseThrow(() -> new NotFoundException(String.format("Cluster server with cluster name %s  does not exist", clusterName)));
    }

    public String getClusterConfigCrdNamespaceByClusterName(String clusterConfigFileName) {
        String clusterName = addConfigExtension(clusterConfigFileName);
        return clusterConfigFileRepository.findCrdNamespaceByClusterName(clusterName)
                .orElseThrow(() -> new NotFoundException(String.format("Cluster config file %s not found", clusterConfigFileName)));
    }

    public List<String> getAllClusterConfigNames() {
        List<String> searchResults = clusterConfigFileRepository.getAllClusterConfigNames();
        return searchResults.stream().
                map(name -> name.replace(".config", ""))
                .sorted(Utility::compareClusterConfigNames)
                .collect(toList());
    }

    public ClusterConfigFile saveClusterConfig(ClusterConfigFile clusterConfigFile) {
        return clusterConfigFileRepository.save(clusterConfigFile);
    }

    public void deleteClusterConfigByName(String clusterConfigFileName) {
        clusterConfigFileRepository.deleteByName(clusterConfigFileName);
    }

    public void deleteAllClusterConfigInstancesByInstanceId(String instanceId) {
        clusterConfigInstanceRepository.deleteAllByInstanceId(instanceId);
    }

    // VnfInstance namespace details repository interactions
    public Optional<VnfInstanceNamespaceDetails> getNamespaceDetails(String vnfInstanceId) {
        return vnfInstanceNamespaceDetailsRepository.findByVnfId(vnfInstanceId);
    }

    public List<VnfInstanceNamespaceDetails> getNamespaceDetailsPresent(String namespace, String clusterServer) {
        return vnfInstanceNamespaceDetailsRepository
                .findByNamespaceAndClusterServer(namespace, clusterServer);
    }

    public boolean isNamespaceSetForDeletion(String instanceId) {
        return vnfInstanceNamespaceDetailsRepository.findByVnfId(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance " + instanceId + " does not exist"))
                .isDeletionInProgress();
    }

    public VnfInstanceNamespaceDetails persistNamespaceDetails(VnfInstanceNamespaceDetails details) {
        return vnfInstanceNamespaceDetailsRepository.save(details);
    }

    public void deleteInstanceDetailsByVnfInstanceId(final String vnfInstanceId) {
        vnfInstanceNamespaceDetailsRepository.deleteByVnfId(vnfInstanceId);
    }

    // Common Repository Interactions
    @SendStateChangedNotification
    public void persistVnfInstanceAndOperation(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        persistLifecycleOperation(operation);
        saveVnfInstanceToDB(vnfInstance);
    }

    public List<ScaleInfoEntity> saveScaleInfo(final List<ScaleInfoEntity> scaleInfo) {
        return scaleInfoRepository.saveAll(scaleInfo);
    }

    public List<String> getNamespacesAssociatedWithCluster(String clusterName) {
        return vnfInstanceRepository.getNamespacesAssociatedWithCluster(clusterName);
    }

    public List<ClusterConfigFile> getClusterConfigFilesWhereUuidIsNull() {
        return clusterConfigFileRepository.getAllByVerificationNamespaceUidIsNull();
    }

    public Optional<ClusterConfigFile> getDefaultCluster() {
        return clusterConfigFileRepository.findByIsDefaultTrue();
    }

    public void lockClusterConfigTable() {
        clusterConfigFileRepository.advisoryLock();
    }

    public void releaseNamespaceDeletion(final LifecycleOperation operation) {
        Optional<VnfInstanceNamespaceDetails> namespaceDetails = getNamespaceDetails(operation.getVnfInstance()
                                                                                             .getVnfInstanceId());
        namespaceDetails.ifPresent(vnfInstanceNamespaceDetails -> {
            LOGGER.info("Unmark namespace {} deletion", namespaceDetails.get().getNamespace());
            vnfInstanceNamespaceDetails.setDeletionInProgress(false);
            persistNamespaceDetails(vnfInstanceNamespaceDetails);
        });
    }

    public Optional<String> getDefaultClusterName() {
        return clusterConfigFileRepository.getDefaultClusterConfigName();
    }

    public List<HelmChart> findHelmChartsByVnfInstance(VnfInstance vnfInstance) {
        return helmChartRepository.findByVnfInstance(vnfInstance);
    }

    public void saveTasksInExistingTransaction(List<Task> tasks) {
        taskRepository.saveAll(tasks);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTasksInNewTransaction(List<Task> tasks) {
        taskRepository.saveAll(tasks);
    }

    public void deleteTasks(List<Task> tasks) {
        final List<Integer> taskIds = tasks.stream().map(Task::getId).toList();
        taskRepository.deleteAllById(taskIds);
    }

    public void deleteTask(Task task) {
        taskRepository.deleteById(task.getId());
    }

    public void deleteTasksByVnfInstanceAndTaskName(String vnfInstanceId, TaskName taskName) {
        taskRepository.deleteByVnfInstanceIdAndTaskName(vnfInstanceId, taskName);
    }

    public List<Task> getAvailableTasksForExecution(LocalDateTime time) {
        return taskRepository.findAvailableTasksForExecution(time, time.minusSeconds(5));
    }

    public Integer getAllOperationsNotInTerminalStates() {
        return lifecycleOperationRepository.countByOperationStateNotIn(TERMINAL_OPERATION_STATES);
    }

    public Integer getOperationsCountNotInTerminalStatesByType(LifecycleOperationType type) {
        return lifecycleOperationRepository.countByLifecycleOperationTypeAndOperationStateNotIn(type, TERMINAL_OPERATION_STATES);
    }

    public Integer getOperationsCountNotInTerminalStatesByVnfInstance(VnfInstance vnfInstance) {
        return lifecycleOperationRepository.countByVnfInstanceAndOperationStateNotIn(vnfInstance, TERMINAL_OPERATION_STATES);
    }
}
